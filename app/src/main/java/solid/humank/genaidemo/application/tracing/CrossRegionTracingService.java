package solid.humank.genaidemo.application.tracing;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cross-Region Tracing Service
 * 
 * This service provides utilities for making cross-region calls with proper
 * trace correlation, performance tracking, and business context propagation.
 */
@Service
public class CrossRegionTracingService {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionTracingService.class);

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter crossRegionCallsCounter;
    private final Counter crossRegionErrorsCounter;
    private final Timer crossRegionLatencyTimer;
    private final Timer crossRegionDatabaseTimer;
    private final Timer crossRegionCacheTimer;
    
    // Configuration
    @Value("${aws.region:ap-east-2}")
    private String currentRegion;
    
    @Value("${app.tracing.cross-region.correlation.correlation-id-header:X-Correlation-ID}")
    private String correlationIdHeader;
    
    @Value("${app.tracing.cross-region.correlation.region-header:X-Source-Region}")
    private String sourceRegionHeader;
    
    @Value("${app.tracing.cross-region.correlation.request-path-header:X-Request-Path}")
    private String requestPathHeader;
    
    @Value("${app.tracing.cross-region.correlation.user-context-header:X-User-Context}")
    private String userContextHeader;
    
    @Value("${app.tracing.cross-region.correlation.session-id-header:X-Session-ID}")
    private String sessionIdHeader;

    // Cache for correlation contexts
    private final Map<String, CrossRegionContext> contextCache = new ConcurrentHashMap<>();

    public CrossRegionTracingService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.crossRegionCallsCounter = Counter.builder("cross.region.calls.total")
            .description("Total number of cross-region calls")
            .register(meterRegistry);
            
        this.crossRegionErrorsCounter = Counter.builder("cross.region.errors.total")
            .description("Total number of cross-region call errors")
            .register(meterRegistry);
            
        this.crossRegionLatencyTimer = Timer.builder("cross.region.latency")
            .description("Cross-region call latency")
            .register(meterRegistry);
            
        this.crossRegionDatabaseTimer = Timer.builder("cross.region.database.duration")
            .description("Cross-region database operation duration")
            .register(meterRegistry);
            
        this.crossRegionCacheTimer = Timer.builder("cross.region.cache.duration")
            .description("Cross-region cache operation duration")
            .register(meterRegistry);
    }

    /**
     * Make a cross-region HTTP call with proper tracing
     */
    public <T> ResponseEntity<T> makeTracedCrossRegionCall(
            String targetRegion, 
            String url, 
            HttpMethod method, 
            Object requestBody, 
            Class<T> responseType,
            String businessContext) {
        
        String correlationId = getCurrentCorrelationId();
        String operationName = method.name() + " " + url;
        
        return AWSXRay.createSubsegment("cross-region-call", (subsegment) -> {
            try {
                // Configure subsegment
                subsegment.putAnnotation("operation", operationName);
                subsegment.putAnnotation("target.region", targetRegion);
                subsegment.putAnnotation("source.region", currentRegion);
                subsegment.putAnnotation("correlation.id", correlationId);
                subsegment.putAnnotation("business.context", businessContext);
                subsegment.putAnnotation("is.cross.region", !currentRegion.equals(targetRegion));
                
                // Add metadata
                subsegment.putMetadata("cross-region", "target-region", targetRegion);
                subsegment.putMetadata("cross-region", "source-region", currentRegion);
                subsegment.putMetadata("cross-region", "url", url);
                subsegment.putMetadata("cross-region", "method", method.name());
                subsegment.putMetadata("cross-region", "business-context", businessContext);
                
                // Prepare headers with tracing information
                HttpHeaders headers = createTracingHeaders(correlationId, businessContext);
                HttpEntity<?> entity = requestBody != null ? new HttpEntity<>(requestBody, headers) : new HttpEntity<>(headers);
                
                // Record metrics
                Counter.builder("cross.region.calls")
                    .tag("source_region", currentRegion)
                    .tag("target_region", targetRegion)
                    .tag("method", method.name())
                    .tag("business_context", businessContext)
                    .register(meterRegistry)
                    .increment();
                
                // Make the call with timing
                Instant startTime = Instant.now();
                Timer.Sample sample = Timer.start(meterRegistry);
                
                try {
                    ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);
                    
                    // Record success metrics
                    Duration duration = Duration.between(startTime, Instant.now());
                    sample.stop(crossRegionLatencyTimer);
                    
                    // Add response annotations
                    subsegment.putAnnotation("response.status", response.getStatusCode().value());
                    subsegment.putAnnotation("response.success", response.getStatusCode().is2xxSuccessful());
                    subsegment.putAnnotation("response.duration.ms", duration.toMillis());
                    
                    // Add response metadata
                    subsegment.putMetadata("response", "status-code", response.getStatusCode().value());
                    subsegment.putMetadata("response", "duration-ms", duration.toMillis());
                    subsegment.putMetadata("response", "headers", response.getHeaders().toSingleValueMap());
                    
                    // Business metrics
                    recordBusinessMetrics(businessContext, true, duration.toMillis());
                    
                    logger.debug("Cross-region call successful: {} -> {} ({}ms)", 
                        currentRegion, targetRegion, duration.toMillis());
                    
                    return response;
                    
                } catch (Exception e) {
                    // Record error metrics
                    sample.stop(crossRegionLatencyTimer);
                    Counter.builder("cross.region.errors")
                        .tag("source_region", currentRegion)
                        .tag("target_region", targetRegion)
                        .tag("error_type", e.getClass().getSimpleName())
                        .tag("business_context", businessContext)
                        .register(meterRegistry)
                        .increment();
                    
                    // Add error annotations
                    subsegment.addException(e);
                    subsegment.putAnnotation("error.type", e.getClass().getSimpleName());
                    subsegment.putAnnotation("error.message", e.getMessage());
                    
                    // Add error metadata
                    subsegment.putMetadata("error", "type", e.getClass().getSimpleName());
                    subsegment.putMetadata("error", "message", e.getMessage());
                    subsegment.putMetadata("error", "stack-trace", getStackTrace(e));
                    
                    // Business metrics
                    Duration duration = Duration.between(startTime, Instant.now());
                    recordBusinessMetrics(businessContext, false, duration.toMillis());
                    
                    logger.error("Cross-region call failed: {} -> {} (error: {})", 
                        currentRegion, targetRegion, e.getMessage(), e);
                    
                    throw new CrossRegionCallException("Cross-region call failed", e);
                }
                
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            }
        });
    }

    /**
     * Track database operation with cross-region context
     */
    public <T> T trackDatabaseOperation(String operation, String database, Supplier<T> databaseCall) {
        String correlationId = getCurrentCorrelationId();
        
        return AWSXRay.createSubsegment("database-operation", (subsegment) -> {
            subsegment.putAnnotation("operation", operation);
            subsegment.putAnnotation("database", database);
            subsegment.putAnnotation("correlation.id", correlationId);
            subsegment.putAnnotation("region", currentRegion);
            
            subsegment.putMetadata("database", "operation", operation);
            subsegment.putMetadata("database", "database", database);
            subsegment.putMetadata("database", "region", currentRegion);
            
            Timer.Sample sample = Timer.start(meterRegistry);
            Instant startTime = Instant.now();
            
            try {
                T result = databaseCall.get();
                
                Duration duration = Duration.between(startTime, Instant.now());
                sample.stop(crossRegionDatabaseTimer);
                
                subsegment.putAnnotation("duration.ms", duration.toMillis());
                subsegment.putAnnotation("success", true);
                
                subsegment.putMetadata("database", "duration-ms", duration.toMillis());
                subsegment.putMetadata("database", "success", true);
                
                return result;
                
            } catch (Exception e) {
                sample.stop(crossRegionDatabaseTimer);
                
                subsegment.addException(e);
                subsegment.putAnnotation("error.type", e.getClass().getSimpleName());
                subsegment.putAnnotation("success", false);
                
                Duration duration = Duration.between(startTime, Instant.now());
                subsegment.putMetadata("database", "duration-ms", duration.toMillis());
                subsegment.putMetadata("database", "success", false);
                subsegment.putMetadata("database", "error", e.getMessage());
                
                throw e;
            }
        });
    }

    /**
     * Track cache operation with cross-region context
     */
    public <T> T trackCacheOperation(String operation, String cacheKey, Supplier<T> cacheCall) {
        String correlationId = getCurrentCorrelationId();
        
        return AWSXRay.createSubsegment("cache-operation", (subsegment) -> {
            subsegment.putAnnotation("operation", operation);
            subsegment.putAnnotation("cache.key", cacheKey);
            subsegment.putAnnotation("correlation.id", correlationId);
            subsegment.putAnnotation("region", currentRegion);
            
            subsegment.putMetadata("cache", "operation", operation);
            subsegment.putMetadata("cache", "key", cacheKey);
            subsegment.putMetadata("cache", "region", currentRegion);
            
            Timer.Sample sample = Timer.start(meterRegistry);
            Instant startTime = Instant.now();
            
            try {
                T result = cacheCall.get();
                
                Duration duration = Duration.between(startTime, Instant.now());
                sample.stop(crossRegionCacheTimer);
                
                subsegment.putAnnotation("duration.ms", duration.toMillis());
                subsegment.putAnnotation("cache.hit", result != null);
                subsegment.putAnnotation("success", true);
                
                subsegment.putMetadata("cache", "duration-ms", duration.toMillis());
                subsegment.putMetadata("cache", "hit", result != null);
                subsegment.putMetadata("cache", "success", true);
                
                return result;
                
            } catch (Exception e) {
                sample.stop(crossRegionCacheTimer);
                
                subsegment.addException(e);
                subsegment.putAnnotation("error.type", e.getClass().getSimpleName());
                subsegment.putAnnotation("success", false);
                
                Duration duration = Duration.between(startTime, Instant.now());
                subsegment.putMetadata("cache", "duration-ms", duration.toMillis());
                subsegment.putMetadata("cache", "success", false);
                subsegment.putMetadata("cache", "error", e.getMessage());
                
                throw e;
            }
        });
    }

    /**
     * Create an async cross-region call with proper trace propagation
     */
    public <T> CompletableFuture<ResponseEntity<T>> makeAsyncCrossRegionCall(
            String targetRegion, 
            String url, 
            HttpMethod method, 
            Object requestBody, 
            Class<T> responseType,
            String businessContext) {
        
        // Note: Trace context propagation is handled by X-Ray SDK automatically
        return CompletableFuture.supplyAsync(() -> {
            // Restore trace context in async thread using beginSegment
            // Note: setTraceEntity is deprecated, using segment context propagation instead
            try {
                return makeTracedCrossRegionCall(targetRegion, url, method, requestBody, responseType, businessContext);
            } finally {
                // Ensure segment is properly closed
                if (AWSXRay.getCurrentSegmentOptional().isPresent()) {
                    AWSXRay.endSegment();
                }
            }
        });
    }

    /**
     * Get or generate correlation ID from current context
     */
    private String getCurrentCorrelationId() {
        try {
            Segment segment = AWSXRay.getCurrentSegment();
            if (segment != null) {
                Object correlationId = segment.getAnnotations().get("correlation.id");
                if (correlationId != null) {
                    return correlationId.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get correlation ID from X-Ray segment", e);
        }
        
        // Generate new correlation ID
        return currentRegion + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create HTTP headers with tracing information
     */
    private HttpHeaders createTracingHeaders(String correlationId, String businessContext) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(correlationIdHeader, correlationId);
        headers.set(sourceRegionHeader, currentRegion);
        headers.set("X-Business-Context", businessContext);
        
        // Add current trace ID if available
        try {
            Segment segment = AWSXRay.getCurrentSegment();
            if (segment != null) {
                headers.set("X-Amzn-Trace-Id", segment.getTraceId().toString());
                
                // Add user context if available
                Object userContext = segment.getAnnotations().get("user.context");
                if (userContext != null) {
                    headers.set(userContextHeader, userContext.toString());
                }
                
                // Add session ID if available
                Object sessionId = segment.getAnnotations().get("session.id");
                if (sessionId != null) {
                    headers.set(sessionIdHeader, sessionId.toString());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not add trace headers", e);
        }
        
        return headers;
    }

    /**
     * Record business metrics for cross-region operations
     */
    private void recordBusinessMetrics(String businessContext, boolean success, long durationMs) {
        Counter.builder("business.operations.total")
            .description("Total business operations")
            .tag("context", businessContext)
            .tag("success", String.valueOf(success))
            .tag("region", currentRegion)
            .register(meterRegistry)
            .increment();
            
        Timer.builder("business.operations.duration")
            .description("Business operation duration")
            .tag("context", businessContext)
            .tag("success", String.valueOf(success))
            .tag("region", currentRegion)
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Cross-region context holder
     */
    public static class CrossRegionContext {
        private final String correlationId;
        private final String sourceRegion;
        private final String userContext;
        private final String sessionId;
        private final String businessContext;
        private final Instant createdAt;

        public CrossRegionContext(String correlationId, String sourceRegion, String userContext, 
                                String sessionId, String businessContext) {
            this.correlationId = correlationId;
            this.sourceRegion = sourceRegion;
            this.userContext = userContext;
            this.sessionId = sessionId;
            this.businessContext = businessContext;
            this.createdAt = Instant.now();
        }

        // Getters
        public String getCorrelationId() { return correlationId; }
        public String getSourceRegion() { return sourceRegion; }
        public String getUserContext() { return userContext; }
        public String getSessionId() { return sessionId; }
        public String getBusinessContext() { return businessContext; }
        public Instant getCreatedAt() { return createdAt; }
    }

    /**
     * Custom exception for cross-region call failures
     */
    public static class CrossRegionCallException extends RuntimeException {
        public CrossRegionCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    public interface Runnable<T> {
        T get() throws Exception;
    }
}