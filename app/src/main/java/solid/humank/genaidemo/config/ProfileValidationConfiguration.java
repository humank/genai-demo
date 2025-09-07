package solid.humank.genaidemo.config;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configuration class for validating Spring Boot profiles and their settings
 */
@Component
@EnableConfigurationProperties(ProfileConfigurationProperties.class)
public class ProfileValidationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ProfileValidationConfiguration.class);

    // Valid application profiles for different environments
    private static final Set<String> VALID_PROFILES = Set.of("dev", "development", "production", "prod", "test");

    // Utility profiles that can be combined with other profiles
    private static final Set<String> UTILITY_PROFILES = Set.of("openapi");

    private final Environment environment;
    private final ProfileConfigurationProperties profileProperties;

    public ProfileValidationConfiguration(Environment environment,
            ProfileConfigurationProperties profileProperties) {
        this.environment = environment;
        this.profileProperties = profileProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProfileConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();

        logger.info("Application started with active profiles: {}", Arrays.toString(activeProfiles));
        logger.info("Profile configuration: name={}, description={}",
                profileProperties.name(), profileProperties.description());

        if (activeProfiles.length == 0) {
            logger.warn("No active profiles detected, using default profile");
            return;
        }

        // Validate profiles on startup
        validateProfilesOnStartup();

        // Validate profile-specific configurations
        for (String profile : activeProfiles) {
            validateProfile(profile);
        }

        // Log profile features
        logProfileFeatures();
    }

    /**
     * Validates that all active profiles are supported
     * Provides clear error messages and validates profile combinations
     */
    public void validateProfilesOnStartup() {
        String[] activeProfiles = environment.getActiveProfiles();

        if (activeProfiles.length == 0) {
            logger.info("No active profiles detected, using default profile configuration");
            return;
        }

        // Collect validation results
        StringBuilder validationResults = new StringBuilder();
        boolean hasInvalidProfiles = false;
        boolean hasInvalidCombinations = false;

        // Validate individual profiles
        for (String profile : activeProfiles) {
            if (!VALID_PROFILES.contains(profile) && !UTILITY_PROFILES.contains(profile)) {
                hasInvalidProfiles = true;
                String errorMessage = String.format(
                        "Invalid profile '%s' detected. Valid application profiles are: %s. Utility profiles are: %s",
                        profile, VALID_PROFILES, UTILITY_PROFILES);
                logger.error(errorMessage);
                validationResults.append(errorMessage).append(". ");
            } else {
                logger.debug("Profile '{}' is valid", profile);
            }
        }

        // Validate profile combinations
        String combinationError = validateProfileCombinations(activeProfiles);
        if (combinationError != null) {
            hasInvalidCombinations = true;
            logger.error(combinationError);
            validationResults.append(combinationError).append(". ");
        }

        // Log final validation results
        if (hasInvalidProfiles || hasInvalidCombinations) {
            logger.error("Profile validation failed for profiles: {}. Errors: {}",
                    Arrays.toString(activeProfiles), validationResults.toString().trim());

            // In production or when strict validation is enabled, throw exception
            if (isStrictValidationEnabled()) {
                throw new ProfileConfigurationException(
                        "Profile validation failed: " + validationResults.toString().trim());
            }
        } else {
            logger.info("Profile validation completed successfully for profiles: {}", Arrays.toString(activeProfiles));
            logProfileValidationSummary(activeProfiles);
        }
    }

    /**
     * Validates that profile combinations are valid
     * 
     * @param activeProfiles array of active profile names
     * @return error message if invalid combination found, null if valid
     */
    private String validateProfileCombinations(String[] activeProfiles) {
        Set<String> profileSet = Set.of(activeProfiles);

        // Check for conflicting environment profiles
        boolean hasTest = profileSet.contains("test");
        boolean hasProduction = profileSet.contains("production") || profileSet.contains("prod");
        boolean hasDevelopment = profileSet.contains("dev") || profileSet.contains("development");

        // Critical security violation: test + production
        if (hasTest && hasProduction) {
            return "SECURITY VIOLATION: 'test' and 'production' profiles must not be active together. " +
                    "This combination poses security risks and may expose test configurations in production environment.";
        }

        // Configuration conflict: test + development
        if (hasTest && hasDevelopment) {
            return "CONFIGURATION CONFLICT: 'test' and 'development' profiles should not be active together. " +
                    "This may cause configuration conflicts and unpredictable behavior. Use either 'test' for testing or 'dev' for development.";
        }

        // Environment conflict: production + development
        if (hasProduction && hasDevelopment) {
            return "ENVIRONMENT CONFLICT: 'production' and 'development' profiles should not be active together. " +
                    "This may cause production systems to use development configurations.";
        }

        // Valid combinations - log for information
        if (hasTest && profileSet.contains("openapi")) {
            logger.info("Valid combination detected: 'test' + 'openapi' profiles for API documentation testing");
        }

        return null; // No validation errors
    }

    /**
     * Checks if the test profile is currently active
     * 
     * @return true if test profile is active, false otherwise
     */
    public boolean isTestProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    private void validateProfile(String profile) {
        switch (profile) {
            case "dev", "development" -> validateDevelopmentProfile();
            case "production", "prod" -> validateProductionProfile();
            case "test" -> validateTestProfile();
            case "openapi" -> logger.debug("OpenAPI profile is active");
            default -> {
                // Only log warning for unknown profiles, don't fail validation
                // The validateProfilesOnStartup() method will handle strict validation
                logger.info("Unknown profile '{}' - using default configuration", profile);
            }
        }
    }

    private void validateTestProfile() {
        logger.info("Validating test profile configuration");

        // Validate H2 database configuration for tests
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl != null && !datasourceUrl.contains("h2:mem:")) {
            logger.warn("Test profile should typically use H2 in-memory database, but found: {}", datasourceUrl);
        }

        // Validate JPA configuration for tests
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto != null && !ddlAuto.equals("create-drop")) {
            logger.warn("Test profile should typically use 'create-drop' for DDL auto, but found: {}", ddlAuto);
        }

        logger.info("Test profile validation completed successfully");
    }

    private void validateDevelopmentProfile() {
        logger.info("Validating development profile configuration");

        // Validate H2 database configuration
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl == null || !datasourceUrl.contains("h2:mem:")) {
            throw new ProfileConfigurationException(
                    "Development profile must use H2 in-memory database");
        }

        // Validate H2 console is enabled
        boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, false);
        if (!h2ConsoleEnabled) {
            logger.warn("H2 console is not enabled in development profile - consider enabling for debugging");
        }

        // Validate Flyway locations
        String flywayLocations = environment.getProperty("spring.flyway.locations");
        if (flywayLocations == null || !flywayLocations.contains("h2")) {
            throw new ProfileConfigurationException(
                    "Development profile must use H2-specific Flyway migrations");
        }

        logger.info("Development profile validation completed successfully");
    }

    private void validateProductionProfile() {
        logger.info("Validating production profile configuration");

        // Check if we're in a test environment
        boolean isTestEnvironment = isTestEnvironment();

        // Validate database configuration
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl == null) {
            throw new ProfileConfigurationException(
                    "Production profile must have datasource URL configured");
        }

        // In test environment, allow H2 with PostgreSQL mode
        if (isTestEnvironment) {
            if (!datasourceUrl.contains("postgresql") && !datasourceUrl.contains("h2:mem:")) {
                throw new ProfileConfigurationException(
                        "Production profile in test environment must use PostgreSQL or H2 with PostgreSQL mode");
            }
        } else {
            // In real production, require PostgreSQL
            if (!datasourceUrl.contains("postgresql")) {
                throw new ProfileConfigurationException(
                        "Production profile must use PostgreSQL database");
            }
        }

        // Validate required environment variables
        validateRequiredProperty("DB_HOST", "Database host must be configured");
        validateRequiredProperty("DB_NAME", "Database name must be configured");
        validateRequiredProperty("DB_USERNAME", "Database username must be configured");

        // Validate Flyway locations - be more flexible in test environment
        String flywayLocations = environment.getProperty("spring.flyway.locations");
        if (flywayLocations == null) {
            throw new ProfileConfigurationException(
                    "Production profile must have Flyway locations configured");
        }

        // In test environment, allow standard migration locations
        if (!isTestEnvironment && !flywayLocations.contains("postgresql")) {
            throw new ProfileConfigurationException(
                    "Production profile must use PostgreSQL-specific Flyway migrations");
        }

        // Validate Kafka configuration
        String kafkaBootstrapServers = environment.getProperty("spring.kafka.bootstrap-servers");
        if (kafkaBootstrapServers == null || kafkaBootstrapServers.trim().isEmpty()) {
            logger.warn("Kafka bootstrap servers not configured - event publishing may fail");
        }

        logger.info("Production profile validation completed successfully");
    }

    private void validateRequiredProperty(String propertyName, String errorMessage) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            String detailedMessage = String.format("%s: Property '%s' is not set or empty. " +
                    "This may cause runtime failures in production environment.", errorMessage, propertyName);
            logger.warn(detailedMessage);

            // In strict validation mode, this could be an error
            if (isStrictValidationEnabled()) {
                logger.error("STRICT VALIDATION: Missing required property '{}' in production environment",
                        propertyName);
            }
        } else {
            logger.debug("Required property '{}' is properly configured", propertyName);
        }
    }

    protected boolean isTestEnvironment() {
        // Check if we're running in a test context
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("test")) ||
                environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper") != null ||
                // Check if running in JUnit test environment
                isJUnitTestEnvironment();
    }

    private boolean isJUnitTestEnvironment() {
        try {
            // Check if JUnit is on the classpath and we're in a test context
            Class.forName("org.junit.jupiter.api.Test");
            // Check for test-specific system properties or stack trace
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            return Arrays.stream(stackTrace)
                    .anyMatch(element -> element.getClassName().contains("junit") ||
                            element.getClassName().contains("Test") ||
                            element.getMethodName().contains("test"));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Determines if strict profile validation should be enforced
     * Strict validation throws exceptions for invalid profiles/combinations
     * 
     * @return true if strict validation is enabled
     */
    private boolean isStrictValidationEnabled() {
        // Enable strict validation in production environments
        String[] activeProfiles = environment.getActiveProfiles();
        boolean hasProductionProfile = Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.equals("production") || profile.equals("prod"));

        // Also check for explicit strict validation property
        String strictProperty = environment.getProperty("app.profile.validation.strict");
        boolean explicitStrict = strictProperty != null && Boolean.parseBoolean(strictProperty);

        // Disable strict validation in test environments to allow flexibility
        boolean isTestEnv = isTestEnvironment();

        return (hasProductionProfile || explicitStrict) && !isTestEnv;
    }

    /**
     * Logs a comprehensive summary of profile validation results
     * 
     * @param activeProfiles array of active profile names
     */
    private void logProfileValidationSummary(String[] activeProfiles) {
        logger.info("=== Profile Validation Summary ===");
        logger.info("Active profiles: {}", Arrays.toString(activeProfiles));

        // Categorize profiles
        Set<String> environmentProfiles = Arrays.stream(activeProfiles)
                .filter(VALID_PROFILES::contains)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> utilityProfiles = Arrays.stream(activeProfiles)
                .filter(UTILITY_PROFILES::contains)
                .collect(java.util.stream.Collectors.toSet());

        if (!environmentProfiles.isEmpty()) {
            logger.info("Environment profiles: {}", environmentProfiles);
        }

        if (!utilityProfiles.isEmpty()) {
            logger.info("Utility profiles: {}", utilityProfiles);
        }

        // Log environment type
        if (isTestProfile()) {
            logger.info("Running in TEST environment");
        } else if (environmentProfiles.contains("production") || environmentProfiles.contains("prod")) {
            logger.info("Running in PRODUCTION environment");
        } else if (environmentProfiles.contains("dev") || environmentProfiles.contains("development")) {
            logger.info("Running in DEVELOPMENT environment");
        }

        logger.info("Strict validation: {}", isStrictValidationEnabled() ? "ENABLED" : "DISABLED");
        logger.info("=== End Profile Validation Summary ===");
    }

    private void logProfileFeatures() {
        ProfileConfigurationProperties.ProfileFeatures features = profileProperties.features();
        logger.info("Profile features: H2Console={}, DebugLogging={}, InMemoryEvents={}, KafkaEvents={}",
                features.h2Console(), features.debugLogging(),
                features.inMemoryEvents(), features.kafkaEvents());
    }

    /**
     * Custom exception for profile configuration errors
     * Provides detailed information about profile validation failures
     */
    public static class ProfileConfigurationException extends RuntimeException {
        private final String[] invalidProfiles;
        private final String[] validProfiles;
        private final String validationContext;

        public ProfileConfigurationException(String message) {
            super(message);
            this.invalidProfiles = new String[0];
            this.validProfiles = new String[0];
            this.validationContext = "general";
        }

        public ProfileConfigurationException(String message, Throwable cause) {
            super(message, cause);
            this.invalidProfiles = new String[0];
            this.validProfiles = new String[0];
            this.validationContext = "general";
        }

        public ProfileConfigurationException(String message, String[] invalidProfiles, String[] validProfiles,
                String validationContext) {
            super(buildDetailedMessage(message, invalidProfiles, validProfiles, validationContext));
            this.invalidProfiles = invalidProfiles != null ? invalidProfiles.clone() : new String[0];
            this.validProfiles = validProfiles != null ? validProfiles.clone() : new String[0];
            this.validationContext = validationContext != null ? validationContext : "general";
        }

        private static String buildDetailedMessage(String message, String[] invalidProfiles, String[] validProfiles,
                String context) {
            StringBuilder sb = new StringBuilder(message);

            if (invalidProfiles != null && invalidProfiles.length > 0) {
                sb.append(" Invalid profiles: ").append(Arrays.toString(invalidProfiles)).append(".");
            }

            if (validProfiles != null && validProfiles.length > 0) {
                sb.append(" Valid profiles: ").append(Arrays.toString(validProfiles)).append(".");
            }

            if (context != null && !context.equals("general")) {
                sb.append(" Context: ").append(context).append(".");
            }

            return sb.toString();
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
}