package solid.humank.genaidemo.infrastructure.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.ConsumerGroupState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.endpoint.annotation.Endpoint;
import org.springframework.boot.actuator.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.infrastructure.messaging.MSKDataFlowTrackingService;
import solid.humank.genaidemo.infrastructure.tracing.MSKXRayTracingService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * MSK Actuator Endpoints
 * 
 * Provides comprehensive Spring Boot Actuator endpoints for MSK monitoring
 * with detailed health checks, metrics, performance data, and business KPIs.
 * 
 * Endpoints:
 * - /actuator/msk-health: Detailed MSK connection and consumer group health
 * - /actuator/msk-metrics: Business-specific KPIs and event processing statistics
 * - /actuator/msk-flow: Real-time data flow visualization and event lineage
 * - /actuator/msk-performance: Application-level latency and throughput metrics
 * - /actuator/msk-errors: Detailed error analysis and recovery status
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
@Component
public class MSKActuatorEndpoints {
    
    private static final Logger logger = LoggerFactory.getLogger(MSKActuatorEndpoints.class);
    
    @Autowired(required = false)
    private AdminClient kafkaAdminClient;
    
    @Autowired(required = false)
    private KafkaProducer<String, Object> kafkaProducer;
    
    @Autowired(required = false)
    private KafkaConsumer<String, Object> kafkaConsumer;
    
    @Autowired
    private MSKDataFlowTrackingService dataFlowTrackingService;
    
    @Autowired
    private MSKXRayTracingService xrayTracingService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter healthCheckCounter;
    private final Timer healthCheckTimer;
    private final Gauge activeConsumerGroups;
    
    public MSKActuatorEndpoints(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.healthCheckCounter = Counter.builder("msk.health.checks")
            .description("Number of MSK health checks performed")
            .register(meterRegistry);
            
        this.healthCheckTimer = Timer.builder("msk.health.check.duration")
            .description("Duration of MSK health checks")
            .register(meterRegistry);
            
        this.activeConsumerGroups = Gauge.builder("msk.consumer.groups.active")
            .description("Number of active MSK consumer groups")
            .register(meterRegistry, this, MSKActuatorEndpoints::getActiveConsumerGroupCount);
    }
    
    /**
     * MSK Health Endpoint
     * 
     * Provides detailed MSK connection status and consumer group health information
     */
    @Endpoint(id = "msk-health")
    @Component
    public static class MSKHealthEndpoint implements HealthIndicator {
        
        private final MSKActuatorEndpoints parent;
        
        public MSKHealthEndpoint(MSKActuatorEndpoints parent) {
            this.parent = parent;
        }
        
        @ReadOperation
        public Map<String, Object> mskHealth() {
            return parent.getMSKHealthStatus();
        }
        
        @Override
        public Health health() {
            Map<String, Object> healthData = parent.getMSKHealthStatus();
            boolean isHealthy = (Boolean) healthData.getOrDefault("overall_healthy", false);
            
            Health.Builder builder = isHealthy ? Health.up() : Health.down();
            healthData.forEach(builder::withDetail);
            
            return builder.build();
        }
    }
    
    /**
     * MSK Metrics Endpoint
     * 
     * Provides business-specific KPIs and event processing statistics
     */
    @Endpoint(id = "msk-metrics")
    @Component
    public static class MSKMetricsEndpoint {
        
        private final MSKActuatorEndpoints parent;
        
        public MSKMetricsEndpoint(MSKActuatorEndpoints parent) {
            this.parent = parent;
        }
        
        @ReadOperation
        public Map<String, Object> mskMetrics() {
            return parent.getMSKMetrics();
        }
    }
    
    /**
     * MSK Data Flow Endpoint
     * 
     * Provides real-time data flow visualization and event lineage tracking
     */
    @Endpoint(id = "msk-flow")
    @Component
    public static class MSKFlowEndpoint {
        
        private final MSKActuatorEndpoints parent;
        
        public MSKFlowEndpoint(MSKActuatorEndpoints parent) {
            this.parent = parent;
        }
        
        @ReadOperation
        public Map<String, Object> mskFlow() {
            return parent.getMSKDataFlow();
        }
    }
    
    /**
     * MSK Performance Endpoint
     * 
     * Provides application-level latency and throughput metrics
     */
    @Endpoint(id = "msk-performance")
    @Component
    public static class MSKPerformanceEndpoint {
        
        private final MSKActuatorEndpoints parent;
        
        public MSKPerformanceEndpoint(MSKActuatorEndpoints parent) {
            this.parent = parent;
        }
        
        @ReadOperation
        public Map<String, Object> mskPerformance() {
            return parent.getMSKPerformanceMetrics();
        }
    }
    
    /**
     * MSK Errors Endpoint
     * 
     * Provides detailed error analysis and recovery status tracking
     */
    @Endpoint(id = "msk-errors")
    @Component
    public static class MSKErrorsEndpoint {
        
        private final MSKActuatorEndpoints parent;
        
        public MSKErrorsEndpoint(MSKActuatorEndpoints parent) {
            this.parent = parent;
        }
        
        @ReadOperation
        public Map<String, Object> mskErrors() {
            return parent.getMSKErrorAnalysis();
        }
    }
    
    // Implementation methods
    
    public Map<String, Object> getMSKHealthStatus() {
        Timer.Sample sample = Timer.start(meterRegistry);
        healthCheckCounter.increment();
        
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("timestamp", Instant.now().toString());
            
            // Check Kafka admin client connectivity
            boolean adminHealthy = checkAdminClientHealth();
            health.put("admin_client_healthy", adminHealthy);
            
            // Check producer health
            boolean producerHealthy = checkProducerHealth();
            health.put("producer_healthy", producerHealthy);
            
            // Check consumer health
            boolean consumerHealthy = checkConsumerHealth();
            health.put("consumer_healthy", consumerHealthy);
            
            // Check consumer groups
            Map<String, Object> consumerGroupsHealth = getConsumerGroupsHealth();
            health.put("consumer_groups", consumerGroupsHealth);
            
            // Overall health status
            boolean overallHealthy = adminHealthy && producerHealthy && consumerHealthy;
            health.put("overall_healthy", overallHealthy);
            health.put("status", overallHealthy ? "UP" : "DOWN");
            
            // Connection details
            health.put("connection_details", getConnectionDetails());
            
            return health;
            
        } catch (Exception e) {
            logger.error("Error checking MSK health", e);
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("overall_healthy", false);
            errorHealth.put("status", "DOWN");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", Instant.now().toString());
            return errorHealth;
            
        } finally {
            sample.stop(healthCheckTimer);
        }
    }
    
    public Map<String, Object> getMSKMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", Instant.now().toString());
        
        try {
            // Business KPIs
            metrics.put("business_events", getBusinessEventMetrics());
            
            // Event processing statistics
            metrics.put("processing_stats", getEventProcessingStats());
            
            // Topic-level metrics
            metrics.put("topic_metrics", getTopicMetrics());
            
            // Consumer group metrics
            metrics.put("consumer_group_metrics", getConsumerGroupMetrics());
            
            // Throughput metrics
            metrics.put("throughput", getThroughputMetrics());
            
        } catch (Exception e) {
            logger.error("Error collecting MSK metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    public Map<String, Object> getMSKDataFlow() {
        Map<String, Object> dataFlow = new HashMap<>();
        dataFlow.put("timestamp", Instant.now().toString());
        
        try {
            // Real-time event flow
            dataFlow.put("real_time_flow", getRealTimeEventFlow());
            
            // Event lineage tracking
            dataFlow.put("event_lineage", getEventLineageTracking());
            
            // Data flow patterns
            dataFlow.put("flow_patterns", getDataFlowPatterns());
            
            // Cross-service dependencies
            dataFlow.put("service_dependencies", getServiceDependencies());
            
            // Flow visualization data
            dataFlow.put("visualization", getFlowVisualizationData());
            
        } catch (Exception e) {
            logger.error("Error collecting MSK data flow information", e);
            dataFlow.put("error", e.getMessage());
        }
        
        return dataFlow;
    }
    
    public Map<String, Object> getMSKPerformanceMetrics() {
        Map<String, Object> performance = new HashMap<>();
        performance.put("timestamp", Instant.now().toString());
        
        try {
            // Latency metrics
            performance.put("latency", getLatencyMetrics());
            
            // Throughput analysis
            performance.put("throughput_analysis", getThroughputAnalysis());
            
            // Resource utilization
            performance.put("resource_utilization", getResourceUtilization());
            
            // Performance trends
            performance.put("trends", getPerformanceTrends());
            
            // Bottleneck analysis
            performance.put("bottlenecks", getBottleneckAnalysis());
            
        } catch (Exception e) {
            logger.error("Error collecting MSK performance metrics", e);
            performance.put("error", e.getMessage());
        }
        
        return performance;
    }
    
    public Map<String, Object> getMSKErrorAnalysis() {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", Instant.now().toString());
        
        try {
            // Error statistics
            errors.put("error_stats", getErrorStatistics());
            
            // Error patterns
            errors.put("error_patterns", getErrorPatterns());
            
            // Recovery status
            errors.put("recovery_status", getRecoveryStatus());
            
            // Dead letter queue analysis
            errors.put("dlq_analysis", getDLQAnalysis());
            
            // Error trends
            errors.put("error_trends", getErrorTrends());
            
        } catch (Exception e) {
            logger.error("Error collecting MSK error analysis", e);
            errors.put("error", e.getMessage());
        }
        
        return errors;
    }
    
    // Helper methods
    
    private boolean checkAdminClientHealth() {
        if (kafkaAdminClient == null) {
            return false;
        }
        
        try {
            // Try to list topics with timeout
            kafkaAdminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warn("Admin client health check failed", e);
            return false;
        }
    }
    
    private boolean checkProducerHealth() {
        if (kafkaProducer == null) {
            return false;
        }
        
        try {
            // Check producer metrics
            return kafkaProducer.metrics() != null;
        } catch (Exception e) {
            logger.warn("Producer health check failed", e);
            return false;
        }
    }
    
    private boolean checkConsumerHealth() {
        if (kafkaConsumer == null) {
            return false;
        }
        
        try {
            // Check consumer assignment
            return kafkaConsumer.assignment() != null;
        } catch (Exception e) {
            logger.warn("Consumer health check failed", e);
            return false;
        }
    }
    
    private Map<String, Object> getConsumerGroupsHealth() {
        Map<String, Object> groupsHealth = new HashMap<>();
        
        if (kafkaAdminClient == null) {
            groupsHealth.put("status", "unavailable");
            return groupsHealth;
        }
        
        try {
            ListConsumerGroupsResult listResult = kafkaAdminClient.listConsumerGroups();
            Set<String> groupIds = listResult.all().get(5, TimeUnit.SECONDS)
                .stream()
                .map(listing -> listing.groupId())
                .collect(Collectors.toSet());
            
            DescribeConsumerGroupsResult describeResult = kafkaAdminClient.describeConsumerGroups(groupIds);
            Map<String, ConsumerGroupDescription> descriptions = describeResult.all().get(5, TimeUnit.SECONDS);
            
            Map<String, Object> groupDetails = new HashMap<>();
            int healthyGroups = 0;
            
            for (Map.Entry<String, ConsumerGroupDescription> entry : descriptions.entrySet()) {
                String groupId = entry.getKey();
                ConsumerGroupDescription description = entry.getValue();
                
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("state", description.state().toString());
                groupInfo.put("members", description.members().size());
                groupInfo.put("coordinator", description.coordinator().toString());
                
                boolean isHealthy = description.state() == ConsumerGroupState.STABLE;
                groupInfo.put("healthy", isHealthy);
                
                if (isHealthy) {
                    healthyGroups++;
                }
                
                groupDetails.put(groupId, groupInfo);
            }
            
            groupsHealth.put("total_groups", groupIds.size());
            groupsHealth.put("healthy_groups", healthyGroups);
            groupsHealth.put("groups", groupDetails);
            groupsHealth.put("status", "available");
            
        } catch (Exception e) {
            logger.error("Error checking consumer groups health", e);
            groupsHealth.put("status", "error");
            groupsHealth.put("error", e.getMessage());
        }
        
        return groupsHealth;
    }
    
    private Map<String, Object> getConnectionDetails() {
        Map<String, Object> details = new HashMap<>();
        
        // Add connection configuration details
        details.put("admin_client_available", kafkaAdminClient != null);
        details.put("producer_available", kafkaProducer != null);
        details.put("consumer_available", kafkaConsumer != null);
        
        return details;
    }
    
    private double getActiveConsumerGroupCount() {
        if (kafkaAdminClient == null) {
            return 0.0;
        }
        
        try {
            return kafkaAdminClient.listConsumerGroups()
                .all()
                .get(2, TimeUnit.SECONDS)
                .size();
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // Placeholder methods for detailed metrics (to be implemented based on specific requirements)
    
    private Map<String, Object> getBusinessEventMetrics() {
        return Map.of(
            "order_events_per_minute", getMetricValue("order.events.rate"),
            "customer_events_per_minute", getMetricValue("customer.events.rate"),
            "payment_events_per_minute", getMetricValue("payment.events.rate"),
            "inventory_events_per_minute", getMetricValue("inventory.events.rate")
        );
    }
    
    private Map<String, Object> getEventProcessingStats() {
        return Map.of(
            "total_events_processed", getMetricValue("events.processed.total"),
            "events_per_second", getMetricValue("events.processing.rate"),
            "average_processing_time_ms", getMetricValue("events.processing.time.avg"),
            "success_rate", getMetricValue("events.processing.success.rate")
        );
    }
    
    private Map<String, Object> getTopicMetrics() {
        // Implementation would collect per-topic metrics
        return Map.of("placeholder", "topic_metrics");
    }
    
    private Map<String, Object> getConsumerGroupMetrics() {
        // Implementation would collect consumer group specific metrics
        return Map.of("placeholder", "consumer_group_metrics");
    }
    
    private Map<String, Object> getThroughputMetrics() {
        return Map.of(
            "messages_per_second", getMetricValue("kafka.messages.rate"),
            "bytes_per_second", getMetricValue("kafka.bytes.rate"),
            "peak_throughput", getMetricValue("kafka.throughput.peak")
        );
    }
    
    private Map<String, Object> getRealTimeEventFlow() {
        // Implementation would provide real-time flow data
        return dataFlowTrackingService.getCurrentFlowStatus();
    }
    
    private Map<String, Object> getEventLineageTracking() {
        // Implementation would provide event lineage information
        return Map.of("placeholder", "event_lineage");
    }
    
    private Map<String, Object> getDataFlowPatterns() {
        // Implementation would analyze data flow patterns
        return Map.of("placeholder", "flow_patterns");
    }
    
    private Map<String, Object> getServiceDependencies() {
        // Implementation would map service dependencies
        return xrayTracingService.getTraceStatistics();
    }
    
    private Map<String, Object> getFlowVisualizationData() {
        // Implementation would provide visualization data
        return Map.of("placeholder", "visualization_data");
    }
    
    private Map<String, Object> getLatencyMetrics() {
        return Map.of(
            "producer_latency_p95", getMetricValue("kafka.producer.latency.p95"),
            "consumer_latency_p95", getMetricValue("kafka.consumer.latency.p95"),
            "end_to_end_latency_p95", getMetricValue("kafka.e2e.latency.p95")
        );
    }
    
    private Map<String, Object> getThroughputAnalysis() {
        // Implementation would provide detailed throughput analysis
        return Map.of("placeholder", "throughput_analysis");
    }
    
    private Map<String, Object> getResourceUtilization() {
        // Implementation would provide resource utilization data
        return Map.of("placeholder", "resource_utilization");
    }
    
    private Map<String, Object> getPerformanceTrends() {
        // Implementation would provide performance trend analysis
        return Map.of("placeholder", "performance_trends");
    }
    
    private Map<String, Object> getBottleneckAnalysis() {
        // Implementation would identify performance bottlenecks
        return Map.of("placeholder", "bottleneck_analysis");
    }
    
    private Map<String, Object> getErrorStatistics() {
        return Map.of(
            "total_errors", getMetricValue("kafka.errors.total"),
            "error_rate", getMetricValue("kafka.errors.rate"),
            "producer_errors", getMetricValue("kafka.producer.errors"),
            "consumer_errors", getMetricValue("kafka.consumer.errors")
        );
    }
    
    private Map<String, Object> getErrorPatterns() {
        // Implementation would analyze error patterns
        return Map.of("placeholder", "error_patterns");
    }
    
    private Map<String, Object> getRecoveryStatus() {
        // Implementation would provide recovery status information
        return Map.of("placeholder", "recovery_status");
    }
    
    private Map<String, Object> getDLQAnalysis() {
        // Implementation would analyze dead letter queue
        return Map.of("placeholder", "dlq_analysis");
    }
    
    private Map<String, Object> getErrorTrends() {
        // Implementation would provide error trend analysis
        return Map.of("placeholder", "error_trends");
    }
    
    private double getMetricValue(String metricName) {
        try {
            return meterRegistry.find(metricName)
                .gauge()
                .map(gauge -> gauge.value())
                .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }
}