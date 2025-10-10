package solid.humank.genaidemo.infrastructure.session.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import solid.humank.genaidemo.infrastructure.common.dynamodb.DynamoDBEntity;
import java.time.Instant;
import java.util.Map;

/**
 * DynamoDB Session Entity for Cross-Region Session Management
 * 
 * Stores user session data in DynamoDB Global Tables for:
 * - Cross-region session sharing
 * - High availability session storage
 * - Automatic session replication
 * 
 * Requirements: 4.1.4 - Cross-region data synchronization
 */
@DynamoDbBean
public class DynamoDBSessionEntity extends DynamoDBEntity {

    private String sessionId;
    private String userId;
    private Map<String, String> sessionData;
    private Instant expiresAt;
    private Boolean isActive;
    private String clientIp;
    private String userAgent;
    private String loginRegion;

    // Default constructor for DynamoDB
    public DynamoDBSessionEntity() {
        super();
    }

    public DynamoDBSessionEntity(String sessionId, String userId, String region) {
        super(region);
        this.sessionId = sessionId;
        this.userId = userId;
        this.isActive = true;
        this.loginRegion = region;
        this.expiresAt = Instant.now().plusSeconds(3600); // 1 hour default
    }

    // Getters and Setters
    @DynamoDbPartitionKey
    @DynamoDbAttribute("session_id")
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("session_data")
    public Map<String, String> getSessionData() {
        return sessionData;
    }

    public void setSessionData(Map<String, String> sessionData) {
        this.sessionData = sessionData;
    }

    @DynamoDbAttribute("expires_at")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @DynamoDbAttribute("is_active")
    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @DynamoDbAttribute("client_ip")
    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @DynamoDbAttribute("user_agent")
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @DynamoDbAttribute("login_region")
    public String getLoginRegion() {
        return loginRegion;
    }

    public void setLoginRegion(String loginRegion) {
        this.loginRegion = loginRegion;
    }

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Extend session expiration
     */
    public void extendSession(long seconds) {
        this.expiresAt = Instant.now().plusSeconds(seconds);
        markModifiedInRegion(getRegion());
    }

    /**
     * Invalidate session
     */
    public void invalidate() {
        this.isActive = false;
        markModifiedInRegion(getRegion());
    }
}