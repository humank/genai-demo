package solid.humank.genaidemo.infrastructure.session.dynamodb;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/**
 * DynamoDB Global Tables Service for Cross-Region Management
 *
 * Provides operations for:
 * - Global Tables creation and management
 * - Cross-region replication monitoring
 * - Conflict resolution tracking
 * - Regional health checks
 *
 * Requirements: 4.1.4 - Cross-region data synchronization
 */
@Service
public class DynamoDBGlobalTablesService {

    private final DynamoDbClient dynamoDbClient;
    private final List<String> replicationRegions;

    public DynamoDBGlobalTablesService(DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.global-tables.regions:us-east-1,us-west-2,eu-west-1}") List<String> replicationRegions) {
        this.dynamoDbClient = dynamoDbClient;
        this.replicationRegions = replicationRegions;
    }

    /**
     * Create Global Table for the specified table
     */
    public void createGlobalTable(String tableName) {
        try {
            // Check if Global Table already exists
            if (isGlobalTableExists(tableName)) {
                return;
            }

            // Create replicas for each region
            List<Replica> replicas = replicationRegions.stream()
                    .map(region -> Replica.builder().regionName(region).build())
                    .collect(Collectors.toList());

            CreateGlobalTableRequest request = CreateGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .replicationGroup(replicas)
                    .build();

            dynamoDbClient.createGlobalTable(request);

        } catch (GlobalTableAlreadyExistsException e) {
            // Global Table already exists, continue
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Global Table: " + tableName, e);
        }
    }

    /**
     * Check if Global Table exists
     */
    public boolean isGlobalTableExists(String tableName) {
        try {
            DescribeGlobalTableRequest request = DescribeGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .build();

            DescribeGlobalTableResponse response = dynamoDbClient.describeGlobalTable(request);
            return response.globalTableDescription() != null;

        } catch (GlobalTableNotFoundException e) {
            return false;
        }
    }

    /**
     * Get Global Table replication status
     */
    public Map<String, String> getGlobalTableStatus(String tableName) {
        try {
            DescribeGlobalTableRequest request = DescribeGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .build();

            DescribeGlobalTableResponse response = dynamoDbClient.describeGlobalTable(request);
            GlobalTableDescription description = response.globalTableDescription();

            return description.replicationGroup().stream()
                    .collect(Collectors.toMap(
                            ReplicaDescription::regionName,
                            replica -> replica.replicaStatus().toString()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to get Global Table status: " + tableName, e);
        }
    }

    /**
     * Get replication metrics for monitoring
     */
    public ReplicationMetrics getReplicationMetrics(String tableName) {
        try {
            DescribeGlobalTableRequest request = DescribeGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .build();

            DescribeGlobalTableResponse response = dynamoDbClient.describeGlobalTable(request);
            GlobalTableDescription description = response.globalTableDescription();

            long activeReplicas = description.replicationGroup().stream()
                    .filter(replica -> replica.replicaStatus() == ReplicaStatus.ACTIVE)
                    .count();

            long totalReplicas = description.replicationGroup().size();

            return ReplicationMetrics.builder()
                    .tableName(tableName)
                    .totalReplicas(totalReplicas)
                    .activeReplicas(activeReplicas)
                    .replicationHealth(activeReplicas == totalReplicas ? "HEALTHY" : "DEGRADED")
                    .regions(description.replicationGroup().stream()
                            .map(ReplicaDescription::regionName)
                            .collect(Collectors.toList()))
                    .build();

        } catch (Exception e) {
            return ReplicationMetrics.builder()
                    .tableName(tableName)
                    .totalReplicas(0)
                    .activeReplicas(0)
                    .replicationHealth("ERROR")
                    .regions(List.of())
                    .build();
        }
    }

    /**
     * Update Global Table settings
     */
    public void updateGlobalTableSettings(String tableName, List<ReplicaUpdate> updates) {
        try {
            UpdateGlobalTableRequest request = UpdateGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .replicaUpdates(updates)
                    .build();

            dynamoDbClient.updateGlobalTable(request);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update Global Table: " + tableName, e);
        }
    }

    /**
     * Add replica to Global Table
     */
    public void addReplica(String tableName, String regionName) {
        try {
            ReplicaUpdate replicaUpdate = ReplicaUpdate.builder()
                    .create(CreateReplicaAction.builder()
                            .regionName(regionName)
                            .build())
                    .build();

            UpdateGlobalTableRequest request = UpdateGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .replicaUpdates(replicaUpdate)
                    .build();

            dynamoDbClient.updateGlobalTable(request);

        } catch (Exception e) {
            throw new RuntimeException("Failed to add replica to Global Table: " + tableName, e);
        }
    }

    /**
     * Remove replica from Global Table
     */
    public void removeReplica(String tableName, String regionName) {
        try {
            ReplicaUpdate replicaUpdate = ReplicaUpdate.builder()
                    .delete(DeleteReplicaAction.builder()
                            .regionName(regionName)
                            .build())
                    .build();

            UpdateGlobalTableRequest request = UpdateGlobalTableRequest.builder()
                    .globalTableName(tableName)
                    .replicaUpdates(replicaUpdate)
                    .build();

            dynamoDbClient.updateGlobalTable(request);

        } catch (Exception e) {
            throw new RuntimeException("Failed to remove replica from Global Table: " + tableName, e);
        }
    }

    /**
     * Replication Metrics Data Class
     */
    public static class ReplicationMetrics {
        private final String tableName;
        private final long totalReplicas;
        private final long activeReplicas;
        private final String replicationHealth;
        private final List<String> regions;

        private ReplicationMetrics(Builder builder) {
            this.tableName = builder.tableName;
            this.totalReplicas = builder.totalReplicas;
            this.activeReplicas = builder.activeReplicas;
            this.replicationHealth = builder.replicationHealth;
            this.regions = builder.regions;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getTableName() {
            return tableName;
        }

        public long getTotalReplicas() {
            return totalReplicas;
        }

        public long getActiveReplicas() {
            return activeReplicas;
        }

        public String getReplicationHealth() {
            return replicationHealth;
        }

        public List<String> getRegions() {
            return regions;
        }

        public static class Builder {
            private String tableName;
            private long totalReplicas;
            private long activeReplicas;
            private String replicationHealth;
            private List<String> regions;

            public Builder tableName(String tableName) {
                this.tableName = tableName;
                return this;
            }

            public Builder totalReplicas(long totalReplicas) {
                this.totalReplicas = totalReplicas;
                return this;
            }

            public Builder activeReplicas(long activeReplicas) {
                this.activeReplicas = activeReplicas;
                return this;
            }

            public Builder replicationHealth(String replicationHealth) {
                this.replicationHealth = replicationHealth;
                return this;
            }

            public Builder regions(List<String> regions) {
                this.regions = regions;
                return this;
            }

            public ReplicationMetrics build() {
                return new ReplicationMetrics(this);
            }
        }
    }
}
