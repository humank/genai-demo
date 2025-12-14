package solid.humank.genaidemo.config;

/**
 * Profile configuration properties
 * Defines configuration properties for different profiles.
 * 
 * Note: This record is NOT annotated with @ConfigurationProperties because
 * the beans are created manually in ProfileConfigurationResolver with
 * profile-specific defaults. Using @ConfigurationProperties would cause
 * NoUniqueBeanDefinitionException due to @ConfigurationPropertiesScan
 * in the main application class.
 */
public record ProfileConfigurationProperties(
        String name,
        String description,
        ProfileFeatures features) {

    public ProfileConfigurationProperties {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile description cannot be null or empty");
        }
        if (features == null) {
            throw new IllegalArgumentException("Profile features cannot be null");
        }
    }

    /**
     * Feature flags for different profiles
     */
    public record ProfileFeatures(
            boolean h2Console,
            boolean debugLogging,
            boolean inMemoryEvents,
            boolean kafkaEvents) {

        public ProfileFeatures {
            if (inMemoryEvents && kafkaEvents) {
                throw new IllegalArgumentException("Cannot enable both in-memory and kafka events simultaneously");
            }
        }

        /**
         * Default features for test profile
         */
        public static ProfileFeatures testDefaults() {
            return new ProfileFeatures(
                    false, // h2Console - disabled in test
                    false, // debugLogging - disabled in test
                    true, // inMemoryEvents - enabled in test
                    false // kafkaEvents - disabled in test
            );
        }

        /**
         * Default features for development profile
         */
        public static ProfileFeatures developmentDefaults() {
            return new ProfileFeatures(
                    true, // h2Console - enabled in dev
                    true, // debugLogging - enabled in dev
                    true, // inMemoryEvents - enabled in dev
                    false // kafkaEvents - disabled in dev for simplicity
            );
        }

        /**
         * Default features for production profile
         */
        public static ProfileFeatures productionDefaults() {
            return new ProfileFeatures(
                    false, // h2Console - disabled in production
                    false, // debugLogging - disabled in production
                    false, // inMemoryEvents - disabled in production
                    true // kafkaEvents - enabled in production
            );
        }
    }

    /**
     * Default configuration for test profile
     */
    public static ProfileConfigurationProperties testDefaults() {
        return new ProfileConfigurationProperties("test", "Test environment", ProfileFeatures.testDefaults());
    }

    /**
     * Default configuration for development profile
     */
    public static ProfileConfigurationProperties developmentDefaults() {
        return new ProfileConfigurationProperties("development", "Development environment",
                ProfileFeatures.developmentDefaults());
    }

    /**
     * Default configuration for production profile
     */
    public static ProfileConfigurationProperties productionDefaults() {
        return new ProfileConfigurationProperties("production", "Production environment",
                ProfileFeatures.productionDefaults());
    }
}