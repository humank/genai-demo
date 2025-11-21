package solid.humank.genaidemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.net.URI;

/**
 * DynamoDB Configuration for Cross-Region Global Tables
 * 
 * This configuration supports:
 * - DynamoDB Global Tables for multi-region active-active setup
 * - Cross-region replication with conflict resolution
 * - Regional endpoint configuration for optimal performance
 * - Monitoring and metrics integration
 * 
 * Requirements: 4.1.4 - Cross-region data synchronization
 */
@Configuration
@Profile({"production", "staging"})
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true", matchIfMissing = false)
public class DynamoDBConfiguration {

    @Value("${aws.region:us-east-1}")
    private String primaryRegion;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamodbEndpoint;

    @Value("${aws.dynamodb.table-prefix:genai-demo}")
    private String tablePrefix;

    @Value("${aws.dynamodb.global-tables.enabled:true}")
    private boolean globalTablesEnabled;

    /**
     * Primary DynamoDB client for the current region
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .region(Region.of(primaryRegion));

        // Use custom endpoint if specified (for local development)
        if (dynamodbEndpoint != null && !dynamodbEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(dynamodbEndpoint));
        }

        return builder.build();
    }

    /**
     * DynamoDB Enhanced Client with Global Tables configuration
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * DynamoDB Global Tables Properties
     */
    @Bean
    public DynamoDBGlobalTablesProperties dynamoDBGlobalTablesProperties() {
        return DynamoDBGlobalTablesProperties.builder()
                .enabled(globalTablesEnabled)
                .primaryRegion(primaryRegion)
                .tablePrefix(tablePrefix)
                .conflictResolution(ConflictResolutionStrategy.LAST_WRITER_WINS)
                .replicationRegions(java.util.List.of("us-east-1", "us-west-2", "eu-west-1"))
                .build();
    }

    /**
     * DynamoDB Global Tables Properties Configuration
     */
    public static class DynamoDBGlobalTablesProperties {
        private final boolean enabled;
        private final String primaryRegion;
        private final String tablePrefix;
        private final ConflictResolutionStrategy conflictResolution;
        private final java.util.List<String> replicationRegions;

        private DynamoDBGlobalTablesProperties(Builder builder) {
            this.enabled = builder.enabled;
            this.primaryRegion = builder.primaryRegion;
            this.tablePrefix = builder.tablePrefix;
            this.conflictResolution = builder.conflictResolution;
            this.replicationRegions = builder.replicationRegions;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public String getPrimaryRegion() { return primaryRegion; }
        public String getTablePrefix() { return tablePrefix; }
        public ConflictResolutionStrategy getConflictResolution() { return conflictResolution; }
        public java.util.List<String> getReplicationRegions() { return replicationRegions; }

        public static class Builder {
            private boolean enabled = false;
            private String primaryRegion = "us-east-1";
            private String tablePrefix = "genai-demo";
            private ConflictResolutionStrategy conflictResolution = ConflictResolutionStrategy.LAST_WRITER_WINS;
            private java.util.List<String> replicationRegions = java.util.List.of();

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder primaryRegion(String primaryRegion) {
                this.primaryRegion = primaryRegion;
                return this;
            }

            public Builder tablePrefix(String tablePrefix) {
                this.tablePrefix = tablePrefix;
                return this;
            }

            public Builder conflictResolution(ConflictResolutionStrategy conflictResolution) {
                this.conflictResolution = conflictResolution;
                return this;
            }

            public Builder replicationRegions(java.util.List<String> replicationRegions) {
                this.replicationRegions = replicationRegions;
                return this;
            }

            public DynamoDBGlobalTablesProperties build() {
                return new DynamoDBGlobalTablesProperties(this);
            }
        }
    }

    /**
     * Conflict Resolution Strategy for Global Tables
     */
    public enum ConflictResolutionStrategy {
        LAST_WRITER_WINS,
        FIRST_WRITER_WINS,
        CUSTOM_RESOLUTION
    }
}