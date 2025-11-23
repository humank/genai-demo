package solid.humank.genaidemo.application.tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Cross-Region Tracing Service
 *
 * This service provides utilities for making cross-region calls with proper
 * trace correlation, performance tracking, and business context propagation.
 */
@Service
public class CrossRegionTracingService {
    private static final Logger logger = LoggerFactory.getLogger(CrossRegionTracingService.class);

    private static final String METADATA_OPERATION = "operation";
    private static final String METADATA_CORRELATION_ID = "correlation.id";
    private static final String METADATA_CROSS_REGION = "cross-region";
    private static final String METADATA_RESPONSE = "response";
    private static final String METADATA_DURATION_MS = "duration-ms";
    private static final String METADATA_ERROR = "error";
    private static final String METADATA_ERROR_TYPE = "error.type";
    private static final String METADATA_DATABASE = "database";
    private static final String METADATA_REGION = "region";
    private static final String METADATA_SUCCESS = "success";
    private static final String METADATA_CACHE = "cache";

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    // Metrics
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

    @Value("${app.tracing.cross-region.correlation.user-context-header:X-User-Context}")
    private String userContextHeader;

    @Value("${app.tracing.cross-region.correlation.session-id-header:X-Session-ID}")
    private String sessionIdHeader;

    public CrossRegionTracingService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
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
            @Nullable Object requestBody,
            @NonNull Class<T> responseType,
            String businessContext) {

        String correlationId = getCurrentCorrelationId();
        String operationName = method.name() + " " + url;

        return AWSXRay.createSubsegment("cross-region-call", subsegment -> {
            try {
                configureSubsegment(subsegment, operationName, targetRegion, correlationId, businessContext);
                recordCallMetrics(targetRegion, method, businessContext);

                TracingContext context = new TracingContext(correlationId, businessContext, targetRegion);
                return executeTracedCall(url, method, requestBody, responseType, context, subsegment);

            } catch (Exception e) {
                subsegment.addException(e);
                throw new CrossRegionCallException("Cross-region call failed: " + e.getMessage(), e);
            }
        });
    }

    private <T> ResponseEntity<T> executeTracedCall(
            String url,
            HttpMethod method,
            @Nullable Object requestBody,
            @NonNull Class<T> responseType,
            TracingContext context,
            com.amazonaws.xray.entities.Subsegment subsegment) {

        HttpHeaders headers = createTracingHeaders(context.correlationId, context.businessContext);
        HttpEntity<?> entity = requestBody != null
                ? new HttpEntity<>(requestBody, Objects.requireNonNull(headers, "Headers cannot be null"))
                : new HttpEntity<>(Objects.requireNonNull(headers, "Headers cannot be null"));

        Instant startTime = Instant.now();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    Objects.requireNonNull(url, "URL cannot be null"),
                    Objects.requireNonNull(method, "HTTP method cannot be null"),
                    entity,
                    Objects.requireNonNull(responseType, "Response type cannot be null"));

            handleSuccessfulResponse(response, subsegment, sample, startTime,
                    context.businessContext, context.targetRegion);
            return response;

        } catch (Exception e) {
            handleFailedResponse(e, subsegment, sample, startTime,
                    context.businessContext, context.targetRegion);
            throw new CrossRegionCallException("Failed to execute cross-region call to " + url, e);
        }
    }

    private void configureSubsegment(
            com.amazonaws.xray.entities.Subsegment subsegment,
            String operationName,
            String targetRegion,
            String correlationId,
            String businessContext) {

        subsegment.putAnnotation(METADATA_OPERATION, operationName);
        subsegment.putAnnotation("target.region", targetRegion);
        subsegment.putAnnotation("source.region", currentRegion);
        subsegment.putAnnotation(METADATA_CORRELATION_ID, correlationId);
        subsegment.putAnnotation("business.context", businessContext);
        subsegment.putAnnotation("is.cross.region", !currentRegion.equals(targetRegion));

        subsegment.putMetadata(METADATA_CROSS_REGION, "target-region", targetRegion);
        subsegment.putMetadata(METADATA_CROSS_REGION, "source-region", currentRegion);
        subsegment.putMetadata(METADATA_CROSS_REGION, "business-context", businessContext);
    }

    private void recordCallMetrics(String targetRegion, HttpMethod method, String businessContext) {
        Counter.builder("cross.region.calls")
                .tag("source_region", currentRegion)
                .tag("target_region", targetRegion)
                .tag("method", method.name())
                .tag("business_context", businessContext)
                .register(meterRegistry)
                .increment();
    }

    private <T> void handleSuccessfulResponse(
            ResponseEntity<T> response,
            com.amazonaws.xray.entities.Subsegment subsegment,
            Timer.Sample sample,
            Instant startTime,
            String businessContext,
            String targetRegion) {

        Duration duration = Duration.between(startTime, Instant.now());
        sample.stop(crossRegionLatencyTimer);

        subsegment.putAnnotation("response.status", response.getStatusCode().value());
        subsegment.putAnnotation("response.success", response.getStatusCode().is2xxSuccessful());
        subsegment.putAnnotation("response.duration.ms", duration.toMillis());

        subsegment.putMetadata(METADATA_RESPONSE, "status-code", response.getStatusCode().value());
        subsegment.putMetadata(METADATA_RESPONSE, METADATA_DURATION_MS, duration.toMillis());
        subsegment.putMetadata(METADATA_RESPONSE, "headers", response.getHeaders().toSingleValueMap());

        recordBusinessMetrics(businessContext, true, duration.toMillis());

        logger.debug("Cross-region call successful: {} -> {} ({}ms)",
                currentRegion, targetRegion, duration.toMillis());
    }

    private void handleFailedResponse(
            Exception e,
            com.amazonaws.xray.entities.Subsegment subsegment,
            Timer.Sample sample,
            Instant startTime,
            String businessContext,
            String targetRegion) {

        sample.stop(crossRegionLatencyTimer);

        Counter.builder("cross.region.errors")
                .tag("source_region", currentRegion)
                .tag("target_region", targetRegion)
                .tag("error_type", e.getClass().getSimpleName())
                .tag("business_context", businessContext)
                .register(meterRegistry)
                .increment();

        subsegment.addException(e);
        subsegment.putAnnotation(METADATA_ERROR_TYPE, e.getClass().getSimpleName());
        subsegment.putAnnotation("error.message", e.getMessage());

        subsegment.putMetadata(METADATA_ERROR, "type", e.getClass().getSimpleName());
        subsegment.putMetadata(METADATA_ERROR, "message", e.getMessage());
        subsegment.putMetadata(METADATA_ERROR, "stack-trace", getStackTrace(e));

        Duration duration = Duration.between(startTime, Instant.now());
        recordBusinessMetrics(businessContext, false, duration.toMillis());

        logger.error("Cross-region call failed: {} -> {}", currentRegion, targetRegion, e);
    }

    /**
     * Track database operation with cross-region context
     */
    public <T> T trackDatabaseOperation(String operation, String database, Supplier<T> databaseCall) {
        String correlationId = getCurrentCorrelationId();

        return AWSXRay.createSubsegment("database-operation", subsegment -> {
            subsegment.putAnnotation(METADATA_OPERATION, operation);
            subsegment.putAnnotation(METADATA_DATABASE, database);
            subsegment.putAnnotation(METADATA_CORRELATION_ID, correlationId);
            subsegment.putAnnotation(METADATA_REGION, currentRegion);

            subsegment.putMetadata(METADATA_DATABASE, METADATA_OPERATION, operation);
            subsegment.putMetadata(METADATA_DATABASE, METADATA_DATABASE, database);
            subsegment.putMetadata(METADATA_DATABASE, METADATA_REGION, currentRegion);

            Timer.Sample sample = Timer.start(meterRegistry);
            Instant startTime = Instant.now();

            try {
                T result = databaseCall.get();

                Duration duration = Duration.between(startTime, Instant.now());
                sample.stop(crossRegionDatabaseTimer);

                subsegment.putAnnotation(METADATA_DURATION_MS, duration.toMillis());
                subsegment.putAnnotation(METADATA_SUCCESS, true);

                subsegment.putMetadata(METADATA_DATABASE, METADATA_DURATION_MS, duration.toMillis());
                subsegment.putMetadata(METADATA_DATABASE, METADATA_SUCCESS, true);

                return result;

            } catch (RuntimeException e) {
                sample.stop(crossRegionDatabaseTimer);

                subsegment.addException(e);
                subsegment.putAnnotation(METADATA_ERROR_TYPE, e.getClass().getSimpleName());
                subsegment.putAnnotation(METADATA_SUCCESS, false);

                Duration duration = Duration.between(startTime, Instant.now());
                subsegment.putMetadata(METADATA_DATABASE, METADATA_DURATION_MS, duration.toMillis());
                subsegment.putMetadata(METADATA_DATABASE, METADATA_SUCCESS, false);
                subsegment.putMetadata(METADATA_DATABASE, METADATA_ERROR, e.getMessage());

                throw new DatabaseOperationException("Database operation failed: " + operation, e);
            }
        });
    }

    /**
     * Track cache operation with cross-region context
     */
    public <T> T trackCacheOperation(String operation, String cacheKey, Supplier<T> cacheCall) {
        String correlationId = getCurrentCorrelationId();

        return AWSXRay.createSubsegment("cache-operation", subsegment -> {
            subsegment.putAnnotation(METADATA_OPERATION, operation);
            subsegment.putAnnotation("cache.key", cacheKey);
            subsegment.putAnnotation(METADATA_CORRELATION_ID, correlationId);
            subsegment.putAnnotation(METADATA_REGION, currentRegion);

            subsegment.putMetadata(METADATA_CACHE, METADATA_OPERATION, operation);
            subsegment.putMetadata(METADATA_CACHE, "key", cacheKey);
            subsegment.putMetadata(METADATA_CACHE, METADATA_REGION, currentRegion);

            Timer.Sample sample = Timer.start(meterRegistry);
            Instant startTime = Instant.now();

            try {
                T result = cacheCall.get();

                Duration duration = Duration.between(startTime, Instant.now());
                sample.stop(crossRegionCacheTimer);

                subsegment.putAnnotation(METADATA_DURATION_MS, duration.toMillis());
                subsegment.putAnnotation("cache.hit", result != null);
                subsegment.putAnnotation(METADATA_SUCCESS, true);

                subsegment.putMetadata(METADATA_CACHE, METADATA_DURATION_MS, duration.toMillis());
                subsegment.putMetadata(METADATA_CACHE, "hit", result != null);
                subsegment.putMetadata(METADATA_CACHE, METADATA_SUCCESS, true);

                return result;

            } catch (RuntimeException e) {
                sample.stop(crossRegionCacheTimer);

                subsegment.addException(e);
                subsegment.putAnnotation(METADATA_ERROR_TYPE, e.getClass().getSimpleName());
                subsegment.putAnnotation(METADATA_SUCCESS, false);

                Duration duration = Duration.between(startTime, Instant.now());
                subsegment.putMetadata(METADATA_CACHE, METADATA_DURATION_MS, duration.toMillis());
                subsegment.putMetadata(METADATA_CACHE, METADATA_SUCCESS, false);
                subsegment.putMetadata(METADATA_CACHE, METADATA_ERROR, e.getMessage());

                throw new CacheOperationException("Cache operation failed: " + operation, e);
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
            @Nullable Object requestBody,
            @NonNull Class<T> responseType,
            String businessContext) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return makeTracedCrossRegionCall(targetRegion, url, method, requestBody, responseType, businessContext);
            } finally {
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
                Object correlationId = segment.getAnnotations().get(METADATA_CORRELATION_ID);
                if (correlationId != null) {
                    return correlationId.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get correlation ID from X-Ray segment", e);
        }

        return currentRegion + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create HTTP headers with tracing information
     */
    private HttpHeaders createTracingHeaders(String correlationId, String businessContext) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Objects.requireNonNull(correlationIdHeader, "Correlation ID header cannot be null"), correlationId);
        headers.set(Objects.requireNonNull(sourceRegionHeader, "Source region header cannot be null"), currentRegion);
        headers.set("X-Business-Context", businessContext);

        try {
            Segment segment = AWSXRay.getCurrentSegment();
            if (segment != null) {
                headers.set("X-Amzn-Trace-Id", segment.getTraceId().toString());

                Object userContext = segment.getAnnotations().get("user.context");
                if (userContext != null) {
                    headers.set(Objects.requireNonNull(userContextHeader, "User context header cannot be null"),
                            userContext.toString());
                }

                Object sessionId = segment.getAnnotations().get("session.id");
                if (sessionId != null) {
                    headers.set(Objects.requireNonNull(sessionIdHeader, "Session ID header cannot be null"),
                            sessionId.toString());
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
                .tag(METADATA_SUCCESS, String.valueOf(success))
                .tag(METADATA_REGION, currentRegion)
                .register(meterRegistry)
                .increment();

        Timer.builder("business.operations.duration")
                .description("Business operation duration")
                .tag("context", businessContext)
                .tag(METADATA_SUCCESS, String.valueOf(success))
                .tag(METADATA_REGION, currentRegion)
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

        public String getCorrelationId() {
            return correlationId;
        }

        public String getSourceRegion() {
            return sourceRegion;
        }

        public String getUserContext() {
            return userContext;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getBusinessContext() {
            return businessContext;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
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
     * Custom exception for database operation failures
     */
    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Custom exception for cache operation failures
     */
    public static class CacheOperationException extends RuntimeException {
        public CacheOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Internal context holder for tracing information
     */
    private static class TracingContext {
        final String correlationId;
        final String businessContext;
        final String targetRegion;

        TracingContext(String correlationId, String businessContext, String targetRegion) {
            this.correlationId = correlationId;
            this.businessContext = businessContext;
            this.targetRegion = targetRegion;
        }
    }
}
