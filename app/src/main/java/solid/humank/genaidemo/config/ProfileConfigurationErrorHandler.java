package solid.humank.genaidemo.config;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.config.ProfileValidationConfiguration.ProfileConfigurationException;

/**
 * Error handler for profile configuration issues
 * Provides clear error messages and fallback strategies
 */
@Component
public class ProfileConfigurationErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProfileConfigurationErrorHandler.class);
    private static final Set<String> SAFE_FALLBACK_PROFILES = Set.of("dev", "test");

    private final Environment environment;

    public ProfileConfigurationErrorHandler(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    public void handleApplicationFailure(ApplicationFailedEvent event) {
        Throwable exception = event.getException();

        if (exception instanceof ProfileConfigurationException profileException) {
            handleProfileConfigurationException(profileException);
        } else if (isProfileRelatedError(exception)) {
            handleGenericProfileError(exception);
        }
    }

    private void handleProfileConfigurationException(ProfileConfigurationException exception) {
        logger.error("=== Profile Configuration Error ===");
        logger.error("Error: {}", exception.getMessage());

        if (exception.getInvalidProfiles().length > 0) {
            logger.error("Invalid profiles detected: {}", Arrays.toString(exception.getInvalidProfiles()));
        }

        if (exception.getValidProfiles().length > 0) {
            logger.error("Valid profiles are: {}", Arrays.toString(exception.getValidProfiles()));
        }

        logger.error("Context: {}", exception.getValidationContext());

        provideFallbackSuggestions();
        logger.error("=== End Profile Configuration Error ===");
    }

    private void handleGenericProfileError(Throwable exception) {
        logger.error("=== Profile Related Error ===");
        logger.error("A profile-related error occurred during application startup");
        logger.error("Error details: {}", exception.getMessage());

        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            logger.error("Active profiles: {}", Arrays.toString(activeProfiles));
        } else {
            logger.error("No active profiles detected");
        }

        provideFallbackSuggestions();
        logger.error("=== End Profile Related Error ===");
    }

    private boolean isProfileRelatedError(Throwable exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        return message.toLowerCase().contains("profile") ||
                message.toLowerCase().contains("datasource") ||
                message.toLowerCase().contains("configuration") ||
                message.toLowerCase().contains("property");
    }

    private void provideFallbackSuggestions() {
        logger.error("");
        logger.error("=== Troubleshooting Suggestions ===");

        String[] activeProfiles = environment.getActiveProfiles();

        if (activeProfiles.length == 0) {
            logger.error("1. No profiles are active. Try setting SPRING_PROFILES_ACTIVE environment variable:");
            logger.error("   - For development: SPRING_PROFILES_ACTIVE=dev");
            logger.error("   - For production: SPRING_PROFILES_ACTIVE=prod");
            logger.error("   - For testing: SPRING_PROFILES_ACTIVE=test");
        } else {
            logger.error("1. Current active profiles: {}", Arrays.toString(activeProfiles));

            // Check for invalid combinations
            Set<String> profileSet = Set.of(activeProfiles);
            if (profileSet.contains("test") && profileSet.contains("production")) {
                logger.error("   ERROR: test + production profiles are not allowed together!");
                logger.error("   SOLUTION: Use either 'test' OR 'production', never both");
            }

            if (profileSet.contains("dev") && profileSet.contains("production")) {
                logger.error("   ERROR: dev + production profiles are not allowed together!");
                logger.error("   SOLUTION: Use either 'dev' OR 'production', never both");
            }
        }

        logger.error("2. Verify configuration files exist:");
        logger.error("   - app/src/main/resources/application.yml");
        logger.error("   - app/src/main/resources/application-dev.yml");
        logger.error("   - app/src/main/resources/application-production.yml");

        logger.error("3. For production profile, ensure these environment variables are set:");
        logger.error("   - DB_HOST (database host)");
        logger.error("   - DB_NAME (database name)");
        logger.error("   - DB_USERNAME (database username)");
        logger.error("   - DB_PASSWORD (database password)");
        logger.error("   - KAFKA_BOOTSTRAP_SERVERS (Kafka servers)");

        logger.error("4. For development, ensure H2 database configuration is correct");

        logger.error("5. Check application logs above for specific configuration errors");

        logger.error("6. Safe fallback profiles for testing: {}", SAFE_FALLBACK_PROFILES);

        logger.error("=== End Troubleshooting Suggestions ===");
    }

    /**
     * Provides configuration precedence information for debugging
     */
    public void logConfigurationPrecedence() {
        logger.info("=== Configuration Precedence Rules ===");
        logger.info("1. Environment Variables (highest priority)");
        logger.info("2. Profile-specific configuration files (application-{profile}.yml)");
        logger.info("3. Base configuration file (application.yml)");
        logger.info("4. Default values (lowest priority)");
        logger.info("");
        logger.info("Current active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        logger.info("=== End Configuration Precedence Rules ===");
    }

    /**
     * Validates that required configuration is present for the current profile
     */
    public boolean validateRequiredConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();

        for (String profile : activeProfiles) {
            switch (profile) {
                case "production", "prod" -> {
                    if (!validateProductionConfiguration()) {
                        return false;
                    }
                }
                case "dev", "development" -> {
                    if (!validateDevelopmentConfiguration()) {
                        return false;
                    }
                }
                case "test" -> {
                    if (!validateTestConfiguration()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean validateProductionConfiguration() {
        logger.debug("Validating production configuration...");

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl == null || !datasourceUrl.contains("postgresql")) {
            logger.error("Production profile requires PostgreSQL datasource URL");
            return false;
        }

        String dbHost = environment.getProperty("DB_HOST");
        if (dbHost == null || dbHost.trim().isEmpty()) {
            logger.warn("DB_HOST environment variable is not set for production");
        }

        return true;
    }

    private boolean validateDevelopmentConfiguration() {
        logger.debug("Validating development configuration...");

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl == null || !datasourceUrl.contains("h2:mem:")) {
            logger.error("Development profile requires H2 in-memory database");
            return false;
        }

        return true;
    }

    private boolean validateTestConfiguration() {
        logger.debug("Validating test configuration...");

        // Test configuration is typically more flexible
        // Just ensure basic configuration is present
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl != null) {
            logger.debug("Test datasource URL: {}", datasourceUrl);
        }

        return true;
    }
}