package solid.humank.genaidemo.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.infrastructure.messaging.DataFlowEvent;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * CloudWatch Data Flow Logger
 * 
 * This component provides structured logging for MSK data flow events
 * optimized for CloudWatch Logs Insights analysis and monitoring.
 * 
 * Features:
 * - Structured logging with consistent field names
 * - CloudWatch Logs Insights optimized format
 * - Event lifecycle tracking (publish → consume → process)
 * - Error logging with context and correlation
 * - Performance metrics logging
 * - Business context preservation
 * 
 * Created: 2025年9月24日 下午2:34 (台北時間)
 * Task: 9.2 - CloudWatch Logging Integration
 */
@Component
public class CloudWatchDataFlowLogger {

    // Separate loggers for different types of events
    private static final Logger dataFlowLogger = LoggerFactory.getLogger("DATA_FLOW");
    private static final Logger publishLogger = LoggerFactory.getLogger("DATA_FLOW_PUBLISH");
    private static final Logger consumeLogger = LoggerFactory.getLogger("DATA_FLOW_CONSUME");
    private static final Logger errorLogger = LoggerFactory.getLogger("DATA_FLOW_ERROR");
    private static final Logger metricsLogger = LoggerFactory.getLogger("DATA_FLOW_METRICS");

    /**
     * Log successful event publishing
     */
    public void logSuccessfulPublish(DataFlowEvent event, SendResult<String, Object> result) {
        publishLogger.info("Event published successfully",
            kv("eventId", event.eventId()),
            kv("correlationId", event.correlationId()),
            kv("eventType", event.eventType()),
            kv("topic", event.topic()),
            kv("timestamp", event.timestamp()),
            kv("traceId", event.traceId()),
            kv("spanId", event.spanId()),
            kv("sourceService", event.sourceService()),
            kv("dataSize", event.dataSize()),
            kv("businessContext", event.businessContext()),
            kv("boundedContext", event.boundedContext()),
            kv("kafkaPartition", result.getRecordMetadata().partition()),
            kv("kafkaOffset", result.getRecordMetadata().offset()),
            kv("kafkaTimestamp", result.getRecordMetadata().timestamp()),
            kv("operation", "publish"),
            kv("status", "success")
        );
    }

    /**
     * Log failed event publishing
     */
    public void logFailedPublish(DataFlowEvent event, Throwable throwable) {
        errorLogger.error("Event publishing failed",
            kv("eventId", event.eventId()),
            kv("correlationId", event.correlationId()),
            kv("eventType", event.eventType()),
            kv("topic", event.topic()),
            kv("timestamp", event.timestamp()),
            kv("traceId", event.traceId()),
            kv("sourceService", event.sourceService()),
            kv("dataSize", event.dataSize()),
            kv("businessContext", event.businessContext()),
            kv("boundedContext", event.boundedContext()),
            kv("errorType", throwable.getClass().getSimpleName()),
            kv("errorMessage", throwable.getMessage()),
            kv("operation", "publish"),
            kv("status", "failed"),
            throwable
        );
    }

    /**
     * Log data flow event processing
     */
    public void logDataFlowEvent(DataFlowEvent event, long processingLatency) {
        dataFlowLogger.info("Data flow event processed",
            kv("eventId", event.eventId()),
            kv("correlationId", event.correlationId()),
            kv("eventType", event.eventType()),
            kv("topic", event.topic()),
            kv("timestamp", event.timestamp()),
            kv("consumedAt", event.consumedAt()),
            kv("traceId", event.traceId()),
            kv("spanId", event.spanId()),
            kv("sourceService", event.sourceService()),
            kv("targetService", event.targetService()),
            kv("dataSize", event.dataSize()),
            kv("businessContext", event.businessContext()),
            kv("boundedContext", event.boundedContext()),
            kv("consumerPartition", event.consumerPartition()),
            kv("consumerOffset", event.consumerOffset()),
            kv("processingLatency", processingLatency),
            kv("endToEndLatency", event.getEndToEndLatencyMs()),
            kv("eventAge", event.getEventAgeMs()),
            kv("isBusinessEvent", event.isBusinessEvent()),
            kv("isSystemEvent", event.isSystemEvent()),
            kv("isErrorEvent", event.isErrorEvent()),
            kv("operation", "consume"),
            kv("status", "success")
        );

        // Log additional metrics for business events
        if (event.isBusinessEvent()) {
            logBusinessEventMetrics(event, processingLatency);
        }
    }

    /**
     * Log event processing error
     */
    public void logProcessingError(DataFlowEvent event, Exception exception) {
        errorLogger.error("Event processing failed",
            kv("eventId", event.eventId()),
            kv("correlationId", event.correlationId()),
            kv("eventType", event.eventType()),
            kv("topic", event.topic()),
            kv("timestamp", event.timestamp()),
            kv("consumedAt", event.consumedAt()),
            kv("traceId", event.traceId()),
            kv("sourceService", event.sourceService()),
            kv("targetService", event.targetService()),
            kv("dataSize", event.dataSize()),
            kv("businessContext", event.businessContext()),
            kv("boundedContext", event.boundedContext()),
            kv("consumerPartition", event.consumerPartition()),
            kv("consumerOffset", event.consumerOffset()),
            kv("errorType", exception.getClass().getSimpleName()),
            kv("errorMessage", exception.getMessage()),
            kv("operation", "consume"),
            kv("status", "failed"),
            exception
        );
    }

    /**
     * Log business event metrics for KPI tracking
     */
    private void logBusinessEventMetrics(DataFlowEvent event, long processingLatency) {
        metricsLogger.info("Business event metrics",
            kv("eventId", event.eventId()),
            kv("correlationId", event.correlationId()),
            kv("eventType", event.eventType()),
            kv("businessContext", event.businessContext()),
            kv("boundedContext", event.boundedContext()),
            kv("processingLatency", processingLatency),
            kv("endToEndLatency", event.getEndToEndLatencyMs()),
            kv("dataSize", event.dataSize()),
            kv("timestamp", event.timestamp()),
            kv("metricType", "business_event"),
            kv("operation", "metrics")
        );
    }

    /**
     * Log cross-context data flow for lineage tracking
     */
    public void logCrossContextDataFlow(
            String sourceContext, 
            String targetContext, 
            String eventType, 
            String correlationId,
            long latency) {
        
        dataFlowLogger.info("Cross-context data flow",
            kv("sourceContext", sourceContext),
            kv("targetContext", targetContext),
            kv("eventType", eventType),
            kv("correlationId", correlationId),
            kv("crossContextLatency", latency),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "cross_context_flow"),
            kv("metricType", "data_lineage")
        );
    }

    /**
     * Log performance anomaly detection
     */
    public void logPerformanceAnomaly(
            String anomalyType, 
            String eventType, 
            String topic,
            double threshold, 
            double actualValue, 
            String description) {
        
        errorLogger.warn("Performance anomaly detected",
            kv("anomalyType", anomalyType),
            kv("eventType", eventType),
            kv("topic", topic),
            kv("threshold", threshold),
            kv("actualValue", actualValue),
            kv("description", description),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "anomaly_detection"),
            kv("severity", "warning")
        );
    }

    /**
     * Log compliance audit event
     */
    public void logComplianceAuditEvent(
            String auditType,
            String eventType,
            String correlationId,
            String businessContext,
            String complianceStatus,
            String details) {
        
        dataFlowLogger.info("Compliance audit event",
            kv("auditType", auditType),
            kv("eventType", eventType),
            kv("correlationId", correlationId),
            kv("businessContext", businessContext),
            kv("complianceStatus", complianceStatus),
            kv("auditDetails", details),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "compliance_audit"),
            kv("metricType", "compliance")
        );
    }

    /**
     * Log data quality metrics
     */
    public void logDataQualityMetrics(
            String eventType,
            String topic,
            int totalEvents,
            int validEvents,
            int invalidEvents,
            double qualityScore) {
        
        metricsLogger.info("Data quality metrics",
            kv("eventType", eventType),
            kv("topic", topic),
            kv("totalEvents", totalEvents),
            kv("validEvents", validEvents),
            kv("invalidEvents", invalidEvents),
            kv("qualityScore", qualityScore),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "data_quality"),
            kv("metricType", "quality_metrics")
        );
    }

    /**
     * Log throughput metrics
     */
    public void logThroughputMetrics(
            String topic,
            String eventType,
            long eventsPerSecond,
            long bytesPerSecond,
            String timeWindow) {
        
        metricsLogger.info("Throughput metrics",
            kv("topic", topic),
            kv("eventType", eventType),
            kv("eventsPerSecond", eventsPerSecond),
            kv("bytesPerSecond", bytesPerSecond),
            kv("timeWindow", timeWindow),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "throughput_metrics"),
            kv("metricType", "performance")
        );
    }

    /**
     * Log consumer lag metrics
     */
    public void logConsumerLagMetrics(
            String topic,
            int partition,
            long currentOffset,
            long highWaterMark,
            long lag,
            String consumerGroup) {
        
        metricsLogger.info("Consumer lag metrics",
            kv("topic", topic),
            kv("partition", partition),
            kv("currentOffset", currentOffset),
            kv("highWaterMark", highWaterMark),
            kv("lag", lag),
            kv("consumerGroup", consumerGroup),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "consumer_lag"),
            kv("metricType", "performance")
        );
    }

    /**
     * Log business impact correlation
     */
    public void logBusinessImpactCorrelation(
            String eventType,
            String businessMetric,
            double technicalValue,
            double businessValue,
            double correlationScore,
            String impactDescription) {
        
        metricsLogger.info("Business impact correlation",
            kv("eventType", eventType),
            kv("businessMetric", businessMetric),
            kv("technicalValue", technicalValue),
            kv("businessValue", businessValue),
            kv("correlationScore", correlationScore),
            kv("impactDescription", impactDescription),
            kv("timestamp", java.time.Instant.now()),
            kv("operation", "business_impact"),
            kv("metricType", "business_correlation")
        );
    }
}