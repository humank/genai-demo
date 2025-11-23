package solid.humank.genaidemo.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import jakarta.annotation.PostConstruct;

/**
 * Profile Configuration Resolver
 *
 * Handles profile activation conflicts and ensures proper test environment
 * setup.
 * This resolver detects and resolves conflicts between different Spring
 * profiles
 * to prevent Bean definition conflicts and configuration issues.
 */
@Configuration
public class ProfileConfigurationResolver implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ProfileConfigurationResolver.class);

    private final Environment environment;
    private ProfileActivationValidator profileValidator;

    public ProfileConfigurationResolver(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initializeValidator() {
        this.profileValidator = new ProfileActivationValidator(environment);
    }

    /**
     * Test profile configuration properties with conflict resolution
     */
    @Bean
    @Profile("test")
    @Conditional(TestProfileCondition.class)
    public ProfileConfigurationProperties testProfileProperties() {
        logger.info("Creating test profile configuration properties");
        return ProfileConfigurationProperties.testDefaults();
    }

    /**
     * Development profile configuration properties with conflict resolution
     */
    @Bean
    @Profile({ "dev", "development" })
    @Conditional(DevelopmentProfileCondition.class)
    public ProfileConfigurationProperties developmentProfileProperties() {
        logger.info("Creating development profile configuration properties");
        return ProfileConfigurationProperties.developmentDefaults();
    }

    /**
     * Production profile configuration properties with conflict resolution
     */
    @Bean
    @Profile({ "prod", "production" })
    @Conditional(ProductionProfileCondition.class)
    public ProfileConfigurationProperties productionProfileProperties() {
        logger.info("Creating production profile configuration properties");
        return ProfileConfigurationProperties.productionDefaults();
    }

    /**
     * Default profile configuration properties (fallback)
     */
    @Bean
    @Profile("default")
    public ProfileConfigurationProperties defaultProfileProperties() {
        logger.info("Creating default profile configuration properties");
        return ProfileConfigurationProperties.developmentDefaults();
    }

    /**
     * Profile activation validator bean
     */
    @Bean
    public ProfileActivationValidator profileActivationValidator() {
        return new ProfileActivationValidator(environment);
    }

    /**
     * Handle application environment prepared event to validate profiles early
     */
    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();

        if (logger.isInfoEnabled()) {
            logger.info("Profile activation event received. Active profiles: {}", Arrays.toString(activeProfiles));
        }

        try {
            // Validate profile combinations early in the startup process
            profileValidator.validateProfileActivation(activeProfiles);

            // Resolve any profile conflicts
            resolveProfileConflicts(activeProfiles);

        } catch (Exception e) {
            logger.error("Profile validation failed during application startup", e);
            // In test environment, we can be more lenient
            if (isTestEnvironment(activeProfiles)) {
                logger.warn("Profile validation failed in test environment, continuing with warnings: {}",
                        e.getMessage());
            } else {
                throw new ProfileConfigurationException("Profile validation failed", e);
            }
        }
    }

    /**
     * Resolve profile conflicts by adjusting active profiles
     */
    private void resolveProfileConflicts(String[] activeProfiles) {
        Set<String> profileSet = new HashSet<>(Arrays.asList(activeProfiles));

        // Handle test + openapi combination (common in tests)
        if (profileSet.contains("test") && profileSet.contains("openapi")) {
            logger.info("Test and OpenAPI profiles detected - this is a valid combination for testing");
            return;
        }

        // Handle conflicting profile combinations
        if (profileSet.contains("test") && (profileSet.contains("dev") || profileSet.contains("development"))) {
            logger.warn("Test and development profiles are both active - prioritizing test profile");
            // In this case, test profile takes precedence
            return;
        }

        if (profileSet.contains("test") && (profileSet.contains("prod") || profileSet.contains("production"))) {
            logger.error("SECURITY VIOLATION: Test and production profiles cannot be active simultaneously");
            throw new ProfileConfigurationException("Security violation: test and production profiles conflict");
        }
    }

    /**
     * Check if current environment is test environment
     */
    private boolean isTestEnvironment(String[] activeProfiles) {
        return Arrays.asList(activeProfiles).contains("test");
    }

    /**
     * Custom exception for profile configuration issues
     */
    public static class ProfileConfigurationException extends RuntimeException {
        public ProfileConfigurationException(String message) {
            super(message);
        }

        public ProfileConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
