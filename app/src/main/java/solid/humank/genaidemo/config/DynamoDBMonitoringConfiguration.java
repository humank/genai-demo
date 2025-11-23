package solid.humank.genaidemo.config;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import solid.humank.genaidemo.infrastructure.session.dynamodb.DynamoDBSessionRepository;

/**
 * DynamoDB Monitoring Configuration for Cross-Region Metrics
 *
 * Provides monitoring and metrics for:
 * - Cross-region replication latency
 * - Conflict resolution statistics
 * - Session distribution across regions
 * - DynamoDB operation performance
 *
 * Requirements: 4.1.4 - Cross-region data synchronization monitoring
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "aws.dynamodb.monitoring.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean(DynamoDbEnhancedClient.class)
public class DynamoDBMonitoringConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDBMonitoringConfiguration.class);
    private static final String REGION_TAG = "region";

    private final MeterRegistry meterRegistry;
    private final DynamoDBSessionRepository sessionRepository;
    private final String currentRegion;

    // Metrics
    private final Timer dynamodbOperationTimer;
    private final Counter dynamodbConflictCounter;
    private final Counter dynamodbReplicationErrorCounter;
    private final AtomicLong activeSessions = new AtomicLong(0);
    private final AtomicLong crossRegionSessions = new AtomicLong(0);

    public DynamoDBMonitoringConfiguration(MeterRegistry meterRegistry,
            DynamoDBSessionRepository sessionRepository,
            @Value("${aws.region:us-east-1}") String currentRegion) {
        this.meterRegistry = meterRegistry;
        this.sessionRepository = sessionRepository;
        this.currentRegion = currentRegion;

        // Initialize metrics
        this.dynamodbOperationTimer = Timer.builder("dynamodb.operation.duration")
                .description("DynamoDB operation duration")
                .register(meterRegistry);

        this.dynamodbConflictCounter = Counter.builder("dynamodb.conflicts.total")
                .description("Total number of DynamoDB conflicts")
                .register(meterRegistry);

        this.dynamodbReplicationErrorCounter = Counter.builder("dynamodb.replication.errors.total")
                .description("Total number of DynamoDB replication errors")
                .register(meterRegistry);

        // Register gauges
        Gauge.builder("dynamodb.sessions.active", activeSessions, AtomicLong::get)
                .description("Number of active sessions in DynamoDB")
                .register(meterRegistry);

        Gauge.builder("dynamodb.sessions.cross_region", crossRegionSessions, AtomicLong::get)
                .description("Number of cross-region sessions")
                .register(meterRegistry);
    }

    /**
     * DynamoDB Operation Timer Bean
     */
    @Bean
    public Timer dynamodbOperationTimer() {
        return dynamodbOperationTimer;
    }

    /**
     * DynamoDB Conflict Counter Bean
     */
    @Bean
    public Counter dynamodbConflictCounter() {
        return dynamodbConflictCounter;
    }

    /**
     * DynamoDB Replication Error Counter Bean
     */
    @Bean
    public Counter dynamodbReplicationErrorCounter() {
        return dynamodbReplicationErrorCounter;
    }

    /**
     * Scheduled task to update session metrics
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateSessionMetrics() {
        try {
            Map<String, Long> sessionStats = sessionRepository.getSessionStatsByRegion();

            // Update active sessions count
            long totalActiveSessions = sessionStats.values().stream().mapToLong(Long::longValue).sum();
            activeSessions.set(totalActiveSessions);

            // Update cross-region sessions count
            long currentRegionSessions = sessionStats.getOrDefault(currentRegion, 0L);
            long otherRegionSessions = totalActiveSessions - currentRegionSessions;
            crossRegionSessions.set(otherRegionSessions);

            // Create region-specific gauges
            for (Map.Entry<String, Long> entry : sessionStats.entrySet()) {
                String region = entry.getKey();
                Long count = entry.getValue();

                Gauge.builder("dynamodb.sessions.by_region", count::doubleValue)
                        .description("Number of sessions by region")
                        .tag(REGION_TAG, region)
                        .register(meterRegistry);
            }

        } catch (Exception e) {
            // Log error but don't fail the application
            logger.error("Error updating DynamoDB session metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to clean up expired sessions
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredSessions() {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            sessionRepository.cleanupExpiredSessions();
            sample.stop(Timer.builder("dynamodb.cleanup.duration")
                    .description("DynamoDB cleanup operation duration")
                    .register(meterRegistry));
        } catch (Exception e) {
            dynamodbReplicationErrorCounter.increment();
            logger.error("Error cleaning up expired sessions: {}", e.getMessage(), e);
        }
    }

    /**
     * Record DynamoDB operation metrics
     */
    public void recordOperation(String operation, long durationMs, boolean success) {
        Timer.builder("dynamodb.operation.duration")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .tag(REGION_TAG, currentRegion)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record DynamoDB conflict
     */
    public void recordConflict(String table, String operation) {
        Counter.builder("dynamodb.conflicts.total")
                .tag("table", table)
                .tag("operation", operation)
                .tag(REGION_TAG, currentRegion)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record DynamoDB replication error
     */
    public void recordReplicationError(String table, String errorType) {
        Counter.builder("dynamodb.replication.errors.total")
                .tag("table", table)
                .tag("error_type", errorType)
                .tag(REGION_TAG, currentRegion)
                .register(meterRegistry)
                .increment();
    }
}
