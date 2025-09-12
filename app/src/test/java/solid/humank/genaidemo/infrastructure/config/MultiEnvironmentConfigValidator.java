package solid.humank.genaidemo.infrastructure.config;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validator for multi-environment configuration integration testing
 */
@Component
public class MultiEnvironmentConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(MultiEnvironmentConfigValidator.class);

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    // Test Environment Validation
    public boolean validateTestEnvironmentConfig() {
        try {
            logger.info("Validating test environment configuration");

            // Check active profiles
            String[] activeProfiles = environment.getActiveProfiles();
            boolean hasTestProfile = List.of(activeProfiles).contains("test");

            if (!hasTestProfile) {
                logger.warn("Test profile not active. Active profiles: {}", List.of(activeProfiles));
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Test environment validation failed", e);
            return false;
        }
    }

    // Profile Configuration Validation
    public boolean validateProfileConfiguration(String profile) {
        try {
            logger.info("Validating profile configuration for: {}", profile);

            switch (profile) {
                case "test":
                    return validateTestProfileConfiguration();
                case "dev":
                    return validateDevProfileConfiguration();
                case "production":
                    return validateProductionProfileConfiguration();
                default:
                    logger.warn("Unknown profile: {}", profile);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Profile configuration validation failed for {}", profile, e);
            return false;
        }
    }

    private boolean validateTestProfileConfiguration() {
        // Validate test-specific configurations
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        return datasourceUrl != null && datasourceUrl.contains("h2:mem:");
    }

    private boolean validateDevProfileConfiguration() {
        // Validate development-specific configurations
        return true; // Placeholder
    }

    private boolean validateProductionProfileConfiguration() {
        // Validate production-specific configurations
        return true; // Placeholder
    }

    // Database Configuration Validation
    public boolean validateDatabaseConfiguration() {
        try {
            logger.info("Validating database configuration");

            if (dataSource == null) {
                logger.error("DataSource is null");
                return false;
            }

            // Test database connectivity
            try (var connection = dataSource.getConnection()) {
                return connection != null && !connection.isClosed();
            }
        } catch (Exception e) {
            logger.error("Database configuration validation failed", e);
            return false;
        }
    }

    // Event Publishing Configuration Validation
    public boolean validateEventPublishingConfiguration() {
        try {
            logger.info("Validating event publishing configuration");

            // Check if event publishing is properly configured for the environment
            // This would check for appropriate event publisher beans
            return true; // Placeholder
        } catch (Exception e) {
            logger.error("Event publishing configuration validation failed", e);
            return false;
        }
    }

    // Observability Configuration Validation
    public boolean validateObservabilityConfiguration(String environment) {
        try {
            logger.info("Validating observability configuration for environment: {}", environment);

            switch (environment) {
                case "test":
                    return validateTestObservabilityConfiguration();
                case "dev":
                    return validateDevObservabilityConfiguration();
                case "production":
                    return validateProductionObservabilityConfiguration();
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("Observability configuration validation failed for {}", environment, e);
            return false;
        }
    }

    private boolean validateTestObservabilityConfiguration() {
        // Validate test observability configuration
        return true; // Placeholder
    }

    private boolean validateDevObservabilityConfiguration() {
        // Validate development observability configuration
        return true; // Placeholder
    }

    private boolean validateProductionObservabilityConfiguration() {
        // Validate production observability configuration
        return true; // Placeholder
    }

    // Comprehensive Configuration Validation
    public boolean validateComprehensiveConfiguration() {
        Map<String, Boolean> validations = Map.of(
                "testEnvironment", validateTestEnvironmentConfig(),
                "profileConfig", validateProfileConfiguration("test"),
                "databaseConfig", validateDatabaseConfiguration(),
                "eventPublishing", validateEventPublishingConfiguration(),
                "observabilityConfig", validateObservabilityConfiguration("test"));

        long passCount = validations.values().stream().mapToLong(result -> result ? 1 : 0).sum();
        double passRate = (double) passCount / validations.size();

        logger.info("Comprehensive configuration validation: {}/{} passed ({}%)",
                passCount, validations.size(), passRate * 100);

        return passRate >= 0.9; // 90% pass rate required
    }

    // Additional helper methods
    public List<String> getSupportedProfiles() {
        return List.of("test", "dev", "production");
    }

    public boolean isDevelopmentConfigurationActive() {
        return List.of(environment.getActiveProfiles()).contains("dev");
    }

    public boolean isProductionConfigurationActive() {
        return List.of(environment.getActiveProfiles()).contains("production");
    }
}