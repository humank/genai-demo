package solid.humank.genaidemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Profile validation configuration
 * Provides utilities to validate and check active profiles
 */
@Configuration
public class ProfileValidationConfiguration {

    private final Environment environment;
    private final ProfileConfigurationProperties profileProperties;

    public ProfileValidationConfiguration(Environment environment, ProfileConfigurationProperties profileProperties) {
        this.environment = environment;
        this.profileProperties = profileProperties;
    }

    /**
     * Exception thrown when profile configuration is invalid
     */
    public static class ProfileConfigurationException extends RuntimeException {
        private final String[] invalidProfiles;
        private final String[] validProfiles;
        private final String validationContext;

        public ProfileConfigurationException(String message, String[] invalidProfiles, String[] validProfiles, String validationContext) {
            super(message);
            this.invalidProfiles = invalidProfiles != null ? invalidProfiles.clone() : new String[0];
            this.validProfiles = validProfiles != null ? validProfiles.clone() : new String[0];
            this.validationContext = validationContext;
        }

        public String[] getInvalidProfiles() {
            return invalidProfiles.clone();
        }

        public String[] getValidProfiles() {
            return validProfiles.clone();
        }

        public String getValidationContext() {
            return validationContext;
        }
    }

    /**
     * Check if test profile is active
     * @return true if test profile is active
     */
    public boolean isTestProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if development profile is active
     * @return true if development profile is active
     */
    public boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "development".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if production profile is active
     * @return true if production profile is active
     */
    public boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("production".equals(profile) || "prod".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all active profiles
     * @return array of active profile names
     */
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    /**
     * Validate profiles on startup
     * @throws ProfileConfigurationException if profiles are invalid
     */
    public void validateProfilesOnStartup() throws ProfileConfigurationException {
        String[] activeProfiles = environment.getActiveProfiles();
        
        // Skip validation in test environment unless strict mode is enabled
        if (isTestEnvironment() && !isStrictModeEnabled()) {
            return;
        }
        
        // Check for invalid individual profiles in strict mode
        if (isStrictModeEnabled()) {
            validateIndividualProfiles(activeProfiles);
        }
        
        // Check for invalid profile combinations
        validateProfileCombinations(activeProfiles);
    }

    /**
     * Check if current environment is test environment
     * @return true if test environment
     */
    protected boolean isTestEnvironment() {
        return isTestProfile();
    }

    /**
     * Check if strict mode is enabled
     */
    private boolean isStrictModeEnabled() {
        return "true".equals(environment.getProperty("app.profile.validation.strict"));
    }
    
    /**
     * Validate individual profiles for validity
     */
    private void validateIndividualProfiles(String[] profiles) throws ProfileConfigurationException {
        String[] validProfiles = {"dev", "development", "test", "production", "prod", "openapi", "default"};
        java.util.Set<String> validProfileSet = java.util.Set.of(validProfiles);
        
        for (String profile : profiles) {
            if (!validProfileSet.contains(profile)) {
                throw new ProfileConfigurationException(
                    String.format("Invalid profile '%s' detected. Valid application profiles are: %s", 
                        profile, String.join(", ", validProfiles)),
                    new String[]{profile},
                    validProfiles,
                    "Individual profile validation"
                );
            }
        }
    }
    
    /**
     * Validate profile combinations for conflicts
     */
    private void validateProfileCombinations(String[] profiles) throws ProfileConfigurationException {
        java.util.Set<String> profileSet = java.util.Set.of(profiles);
        
        // test + production is not allowed - SECURITY VIOLATION
        if (profileSet.contains("test") && profileSet.contains("production")) {
            throw new ProfileConfigurationException(
                "SECURITY VIOLATION: Test and production profiles cannot be active simultaneously. " +
                "This combination poses serious security risks.",
                profiles,
                new String[]{"test", "production"},
                "Security validation"
            );
        }
        
        // test + development is not allowed - CONFIGURATION CONFLICT
        if (profileSet.contains("test") && (profileSet.contains("dev") || profileSet.contains("development"))) {
            throw new ProfileConfigurationException(
                "CONFIGURATION CONFLICT: Test and development profiles cannot be active simultaneously. " +
                "This combination creates configuration conflicts.",
                profiles,
                new String[]{"test", "dev", "development"},
                "Configuration validation"
            );
        }
        
        // dev + production is not allowed - ENVIRONMENT CONFLICT
        if ((profileSet.contains("dev") || profileSet.contains("development")) && 
            (profileSet.contains("production") || profileSet.contains("prod"))) {
            throw new ProfileConfigurationException(
                "ENVIRONMENT CONFLICT: Development and production profiles cannot be active simultaneously. " +
                "This combination creates environment conflicts.",
                profiles,
                new String[]{"dev", "development", "production", "prod"},
                "Environment validation"
            );
        }
    }
    
    /**
     * Check for invalid profile combinations (legacy method)
     */
    private boolean hasInvalidProfileCombination(String[] profiles) {
        java.util.Set<String> profileSet = java.util.Set.of(profiles);
        
        // test + production is not allowed
        if (profileSet.contains("test") && profileSet.contains("production")) {
            return true;
        }
        
        // dev + production is not allowed
        if (profileSet.contains("dev") && profileSet.contains("production")) {
            return true;
        }
        
        // test + dev is not allowed
        if (profileSet.contains("test") && profileSet.contains("dev")) {
            return true;
        }
        
        return false;
    }
}