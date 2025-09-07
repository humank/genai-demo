package solid.humank.genaidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Profile Configuration Properties
 * Defines configuration properties for different profiles
 */
@ConfigurationProperties(prefix = "genai-demo.profile")
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
     * Profile Features Configuration
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
    }
}