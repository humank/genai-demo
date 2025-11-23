package solid.humank.genaidemo.config;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Profile Activation Validator
 *
 * Validates Spring profile activation and detects conflicts between profiles.
 * This validator ensures that incompatible profiles are not activated
 * simultaneously
 * and provides clear error messages when conflicts are detected.
 */
@Component
public class ProfileActivationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProfileActivationValidator.class);

    // Profile name constants
    private static final String PROFILE_DEVELOPMENT = "development";
    private static final String PROFILE_PRODUCTION = "production";
    private static final String PROFILE_OPENAPI = "openapi";

    private final Environment environment;

    // Valid profiles that can be activated
    private static final Set<String> VALID_PROFILES = Set.of(
            "test", "dev", PROFILE_DEVELOPMENT, "prod", PROFILE_PRODUCTION,
            PROFILE_OPENAPI, "default", "k8s", "msk");

    // Profile combinations that are explicitly allowed
    private static final Set<Set<String>> ALLOWED_COMBINATIONS = Set.of(
            Set.of("test", PROFILE_OPENAPI), // Test with OpenAPI is allowed
            Set.of("dev", PROFILE_OPENAPI), // Development with OpenAPI is allowed
            Set.of(PROFILE_DEVELOPMENT, PROFILE_OPENAPI), // Development with OpenAPI is allowed
            Set.of("prod", "k8s"), // Production with Kubernetes is allowed
            Set.of(PROFILE_PRODUCTION, "k8s"), // Production with Kubernetes is allowed
            Set.of("prod", "msk"), // Production with MSK is allowed
            Set.of(PROFILE_PRODUCTION, "msk") // Production with MSK is allowed
    );

    public ProfileActivationValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validate profile activation for conflicts and security issues
     *
     * @param activeProfiles Array of active profile names
     * @throws ProfileValidationException if validation fails
     */
    public void validateProfileActivation(String[] activeProfiles) throws ProfileValidationException {
        if (activeProfiles == null || activeProfiles.length == 0) {
            logger.info("No active profiles detected, using default profile");
            return;
        }

        logger.debug("Validating profile activation: {}", Arrays.toString(activeProfiles));

        // Validate individual profiles
        validateIndividualProfiles(activeProfiles);

        // Validate profile combinations
        validateProfileCombinations(activeProfiles);

        // Validate security constraints
        validateSecurityConstraints(activeProfiles);

        logger.info("Profile validation completed successfully for profiles: {}", Arrays.toString(activeProfiles));
    }

    /**
     * Validate that all active profiles are recognized
     */
    private void validateIndividualProfiles(String[] activeProfiles) throws ProfileValidationException {
        for (String profile : activeProfiles) {
            if (!VALID_PROFILES.contains(profile)) {
                String message = String.format(
                        "Invalid profile '%s' detected. Valid profiles are: %s",
                        profile, VALID_PROFILES);
                logger.error(message);
                throw new ProfileValidationException(message, profile, VALID_PROFILES.toArray(new String[0]));
            }
        }
    }

    /**
     * Validate profile combinations for conflicts
     */
    private void validateProfileCombinations(String[] activeProfiles) throws ProfileValidationException {
        Set<String> profileSet = Set.of(activeProfiles);

        // Check for explicitly forbidden combinations
        validateForbiddenCombinations(profileSet, activeProfiles);

        // Check if combination is explicitly allowed
        if (isExplicitlyAllowedCombination(profileSet)) {
            logger.debug("Profile combination is explicitly allowed: {}", profileSet);
            return;
        }

        // For combinations not explicitly allowed, check for potential conflicts
        validatePotentialConflicts(profileSet, activeProfiles);
    }

    /**
     * Validate forbidden profile combinations
     */
    private void validateForbiddenCombinations(Set<String> profileSet, String[] activeProfiles)
            throws ProfileValidationException {
        // Test + Production is forbidden (security risk)
        if (profileSet.contains("test") && (profileSet.contains("prod") || profileSet.contains("production"))) {
            String message = "SECURITY VIOLATION: Test and production profiles cannot be active simultaneously. " +
                    "This combination poses serious security risks.";
            logger.error(message);
            throw new ProfileValidationException(message, activeProfiles, new String[] { "test", "production" });
        }

        // Development + Production is forbidden (environment conflict)
        if ((profileSet.contains("dev") || profileSet.contains("development")) &&
                (profileSet.contains("prod") || profileSet.contains("production"))) {
            String message = "ENVIRONMENT CONFLICT: Development and production profiles cannot be active simultaneously. "
                    +
                    "This combination creates environment conflicts.";
            logger.error(message);
            throw new ProfileValidationException(message, activeProfiles, new String[] { "dev", "production" });
        }
    }

    /**
     * Check if profile combination is explicitly allowed
     */
    private boolean isExplicitlyAllowedCombination(Set<String> profileSet) {
        return ALLOWED_COMBINATIONS.stream()
                .anyMatch(allowedCombo -> profileSet.equals(allowedCombo) || profileSet.containsAll(allowedCombo));
    }

    /**
     * Validate potential conflicts for non-explicitly-allowed combinations
     */
    private void validatePotentialConflicts(Set<String> profileSet, String[] activeProfiles)
            throws ProfileValidationException {
        // Test + Development is potentially problematic but allowed with warning
        if (profileSet.contains("test") && (profileSet.contains("dev") || profileSet.contains("development"))) {
            logger.warn("WARNING: Test and development profiles are both active. " +
                    "This may cause configuration conflicts. Test profile will take precedence.");
        }

        // Multiple environment profiles (more than 2) might be problematic
        long environmentProfileCount = profileSet.stream()
                .filter(profile -> Set.of("test", "dev", "development", "prod", "production").contains(profile))
                .count();

        if (environmentProfileCount > 2) {
            String message = String.format(
                    "CONFIGURATION WARNING: Multiple environment profiles detected (%d). " +
                            "This may cause unexpected behavior: %s",
                    environmentProfileCount, profileSet);
            logger.warn(message);
            // This is a warning, not an error, so we don't throw an exception
        }
    }

    /**
     * Validate security constraints
     */
    private void validateSecurityConstraints(String[] activeProfiles) throws ProfileValidationException {
        Set<String> profileSet = Set.of(activeProfiles);

        // Ensure production profile has proper security configurations
        if (profileSet.contains("prod") || profileSet.contains("production")) {
            validateProductionSecurityConstraints(profileSet);
        }

        // Ensure test profile doesn't leak into production
        if (profileSet.contains("test")) {
            validateTestSecurityConstraints(profileSet);
        }
    }

    /**
     * Validate production security constraints
     */
    private void validateProductionSecurityConstraints(Set<String> profileSet) throws ProfileValidationException {
        // Production should not have debug features enabled
        if (profileSet.contains("test") || profileSet.contains("dev") || profileSet.contains("development")) {
            String message = "SECURITY CONSTRAINT: Production profile cannot be combined with debug profiles";
            logger.error(message);
            throw new ProfileValidationException(message, profileSet.toArray(new String[0]),
                    new String[] { "production" });
        }
    }

    /**
     * Validate test security constraints
     */
    private void validateTestSecurityConstraints(Set<String> profileSet) throws ProfileValidationException {
        // Test profile should not access production resources
        if (profileSet.contains("prod") || profileSet.contains("production")) {
            String message = "SECURITY CONSTRAINT: Test profile cannot access production resources";
            logger.error(message);
            throw new ProfileValidationException(message, profileSet.toArray(new String[0]), new String[] { "test" });
        }
    }

    /**
     * Check if test profile is active
     */
    public boolean isTestProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    /**
     * Check if development profile is active
     */
    public boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(profile -> "dev".equals(profile) || "development".equals(profile));
    }

    /**
     * Check if production profile is active
     */
    public boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }

    /**
     * Get active profiles
     */
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    /**
     * Exception thrown when profile validation fails
     */
    public static class ProfileValidationException extends Exception {
        private final String[] invalidProfiles;
        private final String[] validProfiles;

        public ProfileValidationException(String message, String invalidProfile, String[] validProfiles) {
            super(message);
            this.invalidProfiles = new String[] { invalidProfile };
            this.validProfiles = validProfiles != null ? validProfiles.clone() : new String[0];
        }

        public ProfileValidationException(String message, String[] invalidProfiles, String[] validProfiles) {
            super(message);
            this.invalidProfiles = invalidProfiles != null ? invalidProfiles.clone() : new String[0];
            this.validProfiles = validProfiles != null ? validProfiles.clone() : new String[0];
        }

        public String[] getInvalidProfiles() {
            return invalidProfiles.clone();
        }

        public String[] getValidProfiles() {
            return validProfiles.clone();
        }
    }
}
