package solid.humank.genaidemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Configuration for AWS Secrets Manager integration
 *
 * This configuration is only active for staging and production profiles
 * where AWS Secrets Manager is available and required.
 */
@Configuration
@ConditionalOnProperty(name = "aws.secretsmanager.enabled", havingValue = "true")
@Profile({ "staging", "production" })
@EnableConfigurationProperties(SecretsManagerProperties.class)
public class SecretsManagerConfiguration {

    private final SecretsManagerProperties properties;

    public SecretsManagerConfiguration(SecretsManagerProperties properties) {
        this.properties = properties;
    }

    /**
     * Create AWS Secrets Manager client
     * Uses default credentials provider chain for AWS authentication
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(properties.getRegion()))
                // Use builder pattern instead of deprecated create()
                .credentialsProvider(
                        software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.builder().build())
                .build();
    }

    /**
     * Create Secrets Manager service
     */
    @Bean
    public SecretsManagerService secretsManagerService(SecretsManagerClient client) {
        return new SecretsManagerService(client, properties);
    }
}
