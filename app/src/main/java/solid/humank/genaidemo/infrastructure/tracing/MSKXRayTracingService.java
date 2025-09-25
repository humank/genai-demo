package solid.humank.genaidemo.infrastructure.tracing;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MSK X-Ray Tracing Service
 * 
 * Provides comprehensive distributed tracing for MSK message flows with
 * X-Ray service map integration, dependency analysis, and performance monitoring.
 * 
 * Features:
 * - Message flow tracing across producer-consumer chains
 * - Cross-service dependency mapping
 * - Error propagation visualization
 * - Performance bottleneck identification
 * - Service dependency discovery
 * - Trace sampling optimization
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
@Service
public class MSKXRayTracingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MSKXRayTracingService.class);
    
    private static final String MSK_SERVICE_NAME = "MSK-Kafka-Cluster";
    private static final String PRODUCER_OPERATION = "kafka-produce";
    private static final String CONSUMER_OPERATION = "kafka-consume";
    private static final String MESSAGE_FLOW_OPERATION = "message-flow";
    
    @Value("${spring.application.name:genai-demo}")
    private String applicationName;
    
    @Value("${aws.xray.tracing.name:genai-demo-msk}")
    private String tracingName;
    
    // Cache for active traces to correlate producer-consumer flows
    private final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    /**
     * Start tracing for Kafka message production
     * 
     * @param record Producer record being sent
     * @param topic Kafka topic name
     * @return Trace context for correlation
     */
    public TraceContext startProducerTrace(ProducerRecord<String, Object> record, String topic) {
        try {
            // Create or get current segment
            Segment segment = AWSXRay.getCurrentSegmentOptional()
                .orElse(AWSXRay.beginSegment(tracingName));
            
            // Create subsegment for Kafka producer operation
            Subsegment producerSubsegment = segment.beginSubsegment(PRODUCER_OPERATION);
            producerSubsegment.setNamespace("remote");
            
            // Add MSK service information
            Map<String, Object> mskService = new HashMap<>();
            mskService.put("name", MSK_SERVICE_NAME);
            mskService.put("type", "kafka");
            mskService.put("operation", "produce");
            producerSubsegment.putService(mskService);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("kafka.topic", topic);
            metadata.put("kafka.partition", record.partition());
            metadata.put("kafka.key", record.key());
            metadata.put("kafka.timestamp", record.timestamp());
            metadata.put("application.name", applicationName);
            metadata.put("message.size", getMessageSize(record.value()));
            metadata.put("producer.timestamp", Instant.now().toString());
            
            producerSubsegment.putAllMetadata("kafka", metadata);
            
            // Add annotations for filtering and searching
            producerSubsegment.putAnnotation("kafka.topic", topic);
            producerSubsegment.putAnnotation("kafka.operation", "produce");
            producerSubsegment.putAnnotation("service.name", applicationName);
            
            // Create trace context
            TraceContext traceContext = new TraceContext(
                segment.getTraceId(),
                producerSubsegment.getId(),
                topic,
                record.key(),
                Instant.now()
            );
            
            // Store in active traces for correlation
            String correlationId = generateCorrelationId(topic, record.key());
            activeTraces.put(correlationId, traceContext);
            
            logger.debug("Started producer trace for topic: {}, key: {}, traceId: {}", 
                topic, record.key(), segment.getTraceId());
            
            return traceContext;
            
        } catch (Exception e) {
            logger.error("Error starting producer trace for topic: {}", topic, e);
            return null;
        }
    }
    
    /**
     * Complete producer tracing with success or error
     * 
     * @param traceContext Trace context from startProducerTrace
     * @param success Whether the operation was successful
     * @param error Error information if failed
     */
    public void completeProducerTrace(TraceContext traceContext, boolean success, Throwable error) {
        if (traceContext == null) {
            return;
        }
        
        try {
            Subsegment subsegment = AWSXRay.getCurrentSubsegmentOptional().orElse(null);
            if (subsegment != null) {
                // Add completion metadata
                Map<String, Object> completionMetadata = new HashMap<>();
                completionMetadata.put("producer.completed_at", Instant.now().toString());
                completionMetadata.put("producer.success", success);
                completionMetadata.put("producer.duration_ms", 
                    Instant.now().toEpochMilli() - traceContext.getStartTime().toEpochMilli());
                
                if (error != null) {
                    completionMetadata.put("producer.error", error.getMessage());
                    completionMetadata.put("producer.error_type", error.getClass().getSimpleName());
                    subsegment.addException(error);
                    subsegment.setError(true);
                }
                
                subsegment.putAllMetadata("kafka", completionMetadata);
                subsegment.putAnnotation("kafka.success", success);
                
                // Close subsegment
                subsegment.close();
                
                logger.debug("Completed producer trace for topic: {}, success: {}", 
                    traceContext.getTopic(), success);
            }
            
        } catch (Exception e) {
            logger.error("Error completing producer trace", e);
        }
    }
    
    /**
     * Start tracing for Kafka message consumption
     * 
     * @param record Consumer record being processed
     * @return Trace context for the consumer operation
     */
    public TraceContext startConsumerTrace(ConsumerRecord<String, Object> record) {
        try {
            // Try to correlate with existing producer trace
            String correlationId = generateCorrelationId(record.topic(), record.key());
            TraceContext producerContext = activeTraces.get(correlationId);
            
            Segment segment;
            if (producerContext != null) {
                // Continue existing trace
                segment = AWSXRay.beginSegment(tracingName, producerContext.getTraceId(), null);
            } else {
                // Start new trace
                segment = AWSXRay.beginSegment(tracingName);
            }
            
            // Create subsegment for Kafka consumer operation
            Subsegment consumerSubsegment = segment.beginSubsegment(CONSUMER_OPERATION);
            consumerSubsegment.setNamespace("remote");
            
            // Add MSK service information
            Map<String, Object> mskService = new HashMap<>();
            mskService.put("name", MSK_SERVICE_NAME);
            mskService.put("type", "kafka");
            mskService.put("operation", "consume");
            consumerSubsegment.putService(mskService);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("kafka.topic", record.topic());
            metadata.put("kafka.partition", record.partition());
            metadata.put("kafka.offset", record.offset());
            metadata.put("kafka.key", record.key());
            metadata.put("kafka.timestamp", record.timestamp());
            metadata.put("application.name", applicationName);
            metadata.put("message.size", getMessageSize(record.value()));
            metadata.put("consumer.timestamp", Instant.now().toString());
            
            // Calculate lag if producer context available
            if (producerContext != null) {
                long lagMs = Instant.now().toEpochMilli() - producerContext.getStartTime().toEpochMilli();
                metadata.put("kafka.lag_ms", lagMs);
                consumerSubsegment.putAnnotation("kafka.lag_ms", lagMs);
            }
            
            consumerSubsegment.putAllMetadata("kafka", metadata);
            
            // Add annotations
            consumerSubsegment.putAnnotation("kafka.topic", record.topic());
            consumerSubsegment.putAnnotation("kafka.operation", "consume");
            consumerSubsegment.putAnnotation("service.name", applicationName);
            
            // Create trace context
            TraceContext traceContext = new TraceContext(
                segment.getTraceId(),
                consumerSubsegment.getId(),
                record.topic(),
                record.key(),
                Instant.now()
            );
            
            logger.debug("Started consumer trace for topic: {}, key: {}, offset: {}, traceId: {}", 
                record.topic(), record.key(), record.offset(), segment.getTraceId());
            
            return traceContext;
            
        } catch (Exception e) {
            logger.error("Error starting consumer trace for topic: {}", record.topic(), e);
            return null;
        }
    }
    
    /**
     * Complete consumer tracing with processing results
     * 
     * @param traceContext Trace context from startConsumerTrace
     * @param success Whether the processing was successful
     * @param error Error information if failed
     * @param processingTimeMs Time taken to process the message
     */
    public void completeConsumerTrace(TraceContext traceContext, boolean success, 
                                    Throwable error, long processingTimeMs) {
        if (traceContext == null) {
            return;
        }
        
        try {
            Subsegment subsegment = AWSXRay.getCurrentSubsegmentOptional().orElse(null);
            if (subsegment != null) {
                // Add completion metadata
                Map<String, Object> completionMetadata = new HashMap<>();
                completionMetadata.put("consumer.completed_at", Instant.now().toString());
                completionMetadata.put("consumer.success", success);
                completionMetadata.put("consumer.processing_time_ms", processingTimeMs);
                completionMetadata.put("consumer.duration_ms", 
                    Instant.now().toEpochMilli() - traceContext.getStartTime().toEpochMilli());
                
                if (error != null) {
                    completionMetadata.put("consumer.error", error.getMessage());
                    completionMetadata.put("consumer.error_type", error.getClass().getSimpleName());
                    subsegment.addException(error);
                    subsegment.setError(true);
                    
                    // Add error propagation information
                    subsegment.putAnnotation("error.propagated", true);
                    subsegment.putAnnotation("error.source", "consumer");
                }
                
                subsegment.putAllMetadata("kafka", completionMetadata);
                subsegment.putAnnotation("kafka.success", success);
                subsegment.putAnnotation("kafka.processing_time_ms", processingTimeMs);
                
                // Close subsegment
                subsegment.close();
                
                logger.debug("Completed consumer trace for topic: {}, success: {}, processingTime: {}ms", 
                    traceContext.getTopic(), success, processingTimeMs);
            }
            
            // Clean up correlation
            String correlationId = generateCorrelationId(traceContext.getTopic(), traceContext.getMessageKey());
            activeTraces.remove(correlationId);
            
        } catch (Exception e) {
            logger.error("Error completing consumer trace", e);
        }
    }
    
    /**
     * Create cross-service dependency trace
     * 
     * @param sourceService Source service name
     * @param targetService Target service name
     * @param operation Operation being performed
     * @param metadata Additional metadata
     */
    public void traceCrossServiceDependency(String sourceService, String targetService, 
                                          String operation, Map<String, Object> metadata) {
        try {
            Segment segment = AWSXRay.getCurrentSegmentOptional()
                .orElse(AWSXRay.beginSegment(tracingName));
            
            Subsegment dependencySubsegment = segment.beginSubsegment(operation);
            dependencySubsegment.setNamespace("remote");
            
            // Add service dependency information
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("source_service", sourceService);
            serviceInfo.put("target_service", targetService);
            serviceInfo.put("operation", operation);
            serviceInfo.put("timestamp", Instant.now().toString());
            
            if (metadata != null) {
                serviceInfo.putAll(metadata);
            }
            
            dependencySubsegment.putAllMetadata("service_dependency", serviceInfo);
            
            // Add annotations for service map
            dependencySubsegment.putAnnotation("source.service", sourceService);
            dependencySubsegment.putAnnotation("target.service", targetService);
            dependencySubsegment.putAnnotation("dependency.operation", operation);
            
            dependencySubsegment.close();
            
            logger.debug("Traced cross-service dependency: {} -> {} ({})", 
                sourceService, targetService, operation);
            
        } catch (Exception e) {
            logger.error("Error tracing cross-service dependency: {} -> {}", 
                sourceService, targetService, e);
        }
    }
    
    /**
     * Optimize trace sampling based on message patterns
     * 
     * @param topic Kafka topic
     * @param messageType Type of message
     * @return Whether this message should be traced
     */
    public boolean shouldTrace(String topic, String messageType) {
        try {
            // High-priority topics always traced
            if (topic.contains("error") || topic.contains("critical") || topic.contains("alert")) {
                return true;
            }
            
            // Business events with higher sampling rate
            if (topic.contains("order") || topic.contains("payment") || topic.contains("customer")) {
                return Math.random() < 0.1; // 10% sampling
            }
            
            // System events with lower sampling rate
            if (topic.contains("system") || topic.contains("infrastructure")) {
                return Math.random() < 0.01; // 1% sampling
            }
            
            // Default sampling rate
            return Math.random() < 0.05; // 5% sampling
            
        } catch (Exception e) {
            logger.error("Error determining trace sampling for topic: {}", topic, e);
            return false;
        }
    }
    
    /**
     * Get current trace statistics for monitoring
     * 
     * @return Map of trace statistics
     */
    public Map<String, Object> getTraceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active_traces", activeTraces.size());
        stats.put("service_name", applicationName);
        stats.put("tracing_name", tracingName);
        stats.put("timestamp", Instant.now().toString());
        
        // Calculate trace age distribution
        long now = Instant.now().toEpochMilli();
        long oldTraces = activeTraces.values().stream()
            .mapToLong(ctx -> now - ctx.getStartTime().toEpochMilli())
            .filter(age -> age > 300000) // 5 minutes
            .count();
        
        stats.put("old_traces_count", oldTraces);
        
        return stats;
    }
    
    /**
     * Clean up old traces to prevent memory leaks
     */
    public void cleanupOldTraces() {
        try {
            long cutoffTime = Instant.now().toEpochMilli() - 600000; // 10 minutes
            
            activeTraces.entrySet().removeIf(entry -> 
                entry.getValue().getStartTime().toEpochMilli() < cutoffTime);
            
            logger.debug("Cleaned up old traces, remaining: {}", activeTraces.size());
            
        } catch (Exception e) {
            logger.error("Error cleaning up old traces", e);
        }
    }
    
    private String generateCorrelationId(String topic, Object key) {
        return topic + ":" + (key != null ? key.toString() : "null");
    }
    
    private int getMessageSize(Object message) {
        if (message == null) {
            return 0;
        }
        
        if (message instanceof String) {
            return ((String) message).length();
        }
        
        if (message instanceof byte[]) {
            return ((byte[]) message).length;
        }
        
        // Estimate size for other objects
        return message.toString().length();
    }
    
    /**
     * Trace context for correlating producer-consumer flows
     */
    public static class TraceContext {
        private final TraceID traceId;
        private final String segmentId;
        private final String topic;
        private final Object messageKey;
        private final Instant startTime;
        
        public TraceContext(TraceID traceId, String segmentId, String topic, 
                          Object messageKey, Instant startTime) {
            this.traceId = traceId;
            this.segmentId = segmentId;
            this.topic = topic;
            this.messageKey = messageKey;
            this.startTime = startTime;
        }
        
        // Getters
        public TraceID getTraceId() { return traceId; }
        public String getSegmentId() { return segmentId; }
        public String getTopic() { return topic; }
        public Object getMessageKey() { return messageKey; }
        public Instant getStartTime() { return startTime; }
    }
}