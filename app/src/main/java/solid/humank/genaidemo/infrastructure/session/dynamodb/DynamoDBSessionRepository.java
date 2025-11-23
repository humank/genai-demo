package solid.humank.genaidemo.infrastructure.session.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DynamoDB Session Repository for Cross-Region Session Management
 * 
 * Provides session operations with Global Tables support:
 * - Cross-region session storage and retrieval
 * - Session expiration management
 * - Conflict resolution for concurrent updates
 * 
 * Requirements: 4.1.4 - Cross-region data synchronization
 */
@Repository
@ConditionalOnBean(DynamoDbEnhancedClient.class)
public class DynamoDBSessionRepository {

    private final DynamoDbTable<DynamoDBSessionEntity> sessionTable;    private final String currentRegion;

    public DynamoDBSessionRepository(DynamoDbEnhancedClient enhancedClient, 
                                   @Value("${aws.region:us-east-1}") String currentRegion,
                                   @Value("${aws.dynamodb.table-prefix:genai-demo}") String tablePrefix) {
        this.sessionTable = enhancedClient.table(tablePrefix + "-user-sessions", 
                                                TableSchema.fromBean(DynamoDBSessionEntity.class));
        this.currentRegion = currentRegion;
    }

    /**
     * Save or update session
     */
    public DynamoDBSessionEntity save(DynamoDBSessionEntity session) {
        session.markModifiedInRegion(currentRegion);
        sessionTable.putItem(session);
        return session;
    }

    /**
     * Find session by ID
     */
    public Optional<DynamoDBSessionEntity> findById(String sessionId) {
        Key key = Key.builder().partitionValue(sessionId).build();
        DynamoDBSessionEntity session = sessionTable.getItem(key);
        
        if (session != null && !session.isExpired() && session.getIsActive()) {
            return Optional.of(session);
        }
        
        return Optional.empty();
    }

    /**
     * Find active sessions by user ID
     */
    public List<DynamoDBSessionEntity> findActiveSessionsByUserId(String userId) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":userId", AttributeValue.builder().s(userId).build());
        expressionValues.put(":isActive", AttributeValue.builder().bool(true).build());
        expressionValues.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

        Expression filterExpression = Expression.builder()
                .expression("user_id = :userId AND is_active = :isActive AND expires_at > :now")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return sessionTable.scan(scanRequest).items().stream().collect(Collectors.toList());
    }

    /**
     * Delete session
     */
    public void delete(String sessionId) {
        Key key = Key.builder().partitionValue(sessionId).build();
        sessionTable.deleteItem(key);
    }

    /**
     * Invalidate session (soft delete)
     */
    public void invalidate(String sessionId) {
        Optional<DynamoDBSessionEntity> sessionOpt = findById(sessionId);
        if (sessionOpt.isPresent()) {
            DynamoDBSessionEntity session = sessionOpt.get();
            session.invalidate();
            save(session);
        }
    }

    /**
     * Extend session expiration
     */
    public void extendSession(String sessionId, long seconds) {
        Optional<DynamoDBSessionEntity> sessionOpt = findById(sessionId);
        if (sessionOpt.isPresent()) {
            DynamoDBSessionEntity session = sessionOpt.get();
            session.extendSession(seconds);
            save(session);
        }
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

        Expression filterExpression = Expression.builder()
                .expression("expires_at < :now")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        List<DynamoDBSessionEntity> expiredSessions = sessionTable.scan(scanRequest).items()
                .stream().collect(Collectors.toList());
        
        for (DynamoDBSessionEntity session : expiredSessions) {
            Key key = Key.builder().partitionValue(session.getSessionId()).build();
            sessionTable.deleteItem(key);
        }
    }

    /**
     * Get session statistics by region
     */
    public Map<String, Long> getSessionStatsByRegion() {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":isActive", AttributeValue.builder().bool(true).build());
        expressionValues.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

        Expression filterExpression = Expression.builder()
                .expression("is_active = :isActive AND expires_at > :now")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        List<DynamoDBSessionEntity> activeSessions = sessionTable.scan(scanRequest).items()
                .stream().collect(Collectors.toList());
        
        Map<String, Long> stats = new HashMap<>();
        for (DynamoDBSessionEntity session : activeSessions) {
            String region = session.getLoginRegion();
            stats.put(region, stats.getOrDefault(region, 0L) + 1);
        }
        
        return stats;
    }
}