package solid.humank.genaidemo.infrastructure.common.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import java.time.Instant;

/**
 * Base DynamoDB Entity with Global Tables support
 * 
 * Provides common attributes for all DynamoDB entities:
 * - Optimistic locking with version control
 * - Automatic timestamp management
 * - Cross-region replication metadata
 * 
 * Requirements: 4.1.4 - Cross-region data synchronization
 */
@DynamoDbBean
public abstract class DynamoDBEntity {

    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private String region;
    private String lastModifiedRegion;

    // Default constructor for DynamoDB
    protected DynamoDBEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    protected DynamoDBEntity(String region) {
        this();
        this.region = region;
        this.lastModifiedRegion = region;
    }

    // Getters and Setters with DynamoDB annotations
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @DynamoDbAttribute("created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("region")
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @DynamoDbAttribute("last_modified_region")
    public String getLastModifiedRegion() {
        return lastModifiedRegion;
    }

    public void setLastModifiedRegion(String lastModifiedRegion) {
        this.lastModifiedRegion = lastModifiedRegion;
    }

    /**
     * Update the last modified region when entity is modified
     */
    public void markModifiedInRegion(String region) {
        this.lastModifiedRegion = region;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if this entity was last modified in the specified region
     */
    public boolean wasLastModifiedInRegion(String region) {
        return region != null && region.equals(this.lastModifiedRegion);
    }
}