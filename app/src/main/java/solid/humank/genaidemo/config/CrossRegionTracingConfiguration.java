package solid.humank.genaidemo.config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cross-Region Distributed Tracing Configuration
 *
 * This configuration class sets up enhanced X-Ray tracing with cross-region
 * correlation,
 * performance tracking, and business context propagation.
 */
@Configuration
@ConditionalOnEnabledTracing
@ConditionalOnProperty(name = "app.tracing.cross-region.enabled", havingValue = "true", matchIfMissing = true)
public class CrossRegionTracingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionTracingConfiguration.class);

    private static final String CORRELATION_ID_KEY = "correlation.id";
    private static final String CROSS_REGION_KEY = "cross-region";
    private static final String PERFORMANCE_KEY = "performance";
    private static final String BUSINESS_CONTEXT_KEY = "business.context";
    private static final String BUSINESS_KEY = "business";
    private static final String USER_DEVICE_KEY = "user.device";
    private static final String BUSINESS_METRICS_KEY = "business-metrics";

    @Value("${aws.region:ap-east-2}")
    private String primaryRegion;

    @Value("${app.tracing.cross-region.correlation.correlation-id-header:X-Correlation-ID}")
    private String correlationIdHeader;

    @Value("${app.tracing.cross-region.correlation.region-header:X-Source-Region}")
    private String sourceRegionHeader;

    @Value("${app.tracing.cross-region.correlation.user-context-header:X-User-Context}")
    private String userContextHeader;

    @Value("${app.tracing.cross-region.correlation.session-id-header:X-Session-ID}")
    private String sessionIdHeader;

    @Value("${app.tracing.cross-region.performance.slow-request-threshold:2000}")
    private long slowRequestThreshold;

    /**
     * Configure AWS X-Ray Recorder with cross-region plugins and settings
     */
    @Bean
    @Primary
    public AWSXRayRecorder awsXRayRecorder() {
        logger.info("Configuring AWS X-Ray Recorder for cross-region tracing");

        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                .withPlugin(new EC2Plugin())
                .withPlugin(new ECSPlugin())
                .withPlugin(new EKSPlugin())
                .withContextMissingStrategy(new LogErrorContextMissingStrategy())
                .withSamplingStrategy(new LocalizedSamplingStrategy());

        AWSXRayRecorder recorder = builder.build();
        AWSXRay.setGlobalRecorder(recorder);

        logger.info("AWS X-Ray Recorder configured successfully with cross-region plugins");
        return recorder;
    }

    private final MeterRegistry meterRegistry;

    public CrossRegionTracingConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Cross-Region Tracing Interceptor
     */
    @Bean
    public @NonNull CrossRegionTracingInterceptor crossRegionTracingInterceptor() {
        return new CrossRegionTracingInterceptor(meterRegistry);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(crossRegionTracingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/health", "/favicon.ico");
    }

    /**
     * Cross-Region Tracing Interceptor Implementation
     */
    public class CrossRegionTracingInterceptor implements HandlerInterceptor {

        private final MeterRegistry meterRegistry;
        private final Map<String, Timer> timers = new ConcurrentHashMap<>();

        public CrossRegionTracingInterceptor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                @NonNull Object handler) {
            try {
                // Get or generate correlation ID
                String correlationId = getOrGenerateCorrelationId(request);
                String sourceRegion = request.getHeader(sourceRegionHeader);
                String requestPath = request.getRequestURI();
                String userContext = request.getHeader(userContextHeader);
                String sessionId = request.getHeader(sessionIdHeader);

                // Set response headers for downstream services
                response.setHeader(correlationIdHeader, correlationId);
                response.setHeader(sourceRegionHeader, sourceRegion != null ? sourceRegion : primaryRegion);

                // Add annotations to X-Ray segment
                Segment segment = AWSXRay.getCurrentSegment();
                if (segment != null) {
                    segment.putAnnotation(CORRELATION_ID_KEY, correlationId);
                    segment.putAnnotation("source.region", sourceRegion != null ? sourceRegion : primaryRegion);
                    segment.putAnnotation("current.region", primaryRegion);
                    segment.putAnnotation("request.path", requestPath);
                    segment.putAnnotation("http.method", request.getMethod());

                    if (userContext != null) {
                        segment.putAnnotation("user.context", userContext);
                    }
                    if (sessionId != null) {
                        segment.putAnnotation("session.id", sessionId);
                    }

                    // Add metadata for performance analysis
                    segment.putMetadata(CROSS_REGION_KEY, "correlation-id", correlationId);
                    segment.putMetadata(CROSS_REGION_KEY, "source-region", sourceRegion);
                    segment.putMetadata(CROSS_REGION_KEY, "target-region", primaryRegion);
                    segment.putMetadata(CROSS_REGION_KEY, "is-cross-region", !primaryRegion.equals(sourceRegion));

                    // Add request metadata
                    segment.putMetadata("http", "user-agent", request.getHeader("User-Agent"));
                    segment.putMetadata("http", "remote-addr", request.getRemoteAddr());
                    segment.putMetadata("http", "x-forwarded-for", request.getHeader("X-Forwarded-For"));

                    // Business context tracking
                    addBusinessContext(segment, request);
                }

                // Start performance timer
                String timerKey = request.getMethod() + "_" + requestPath;
                timers.computeIfAbsent(timerKey,
                        k -> Timer.builder("http.request.duration")
                                .description("HTTP request duration")
                                .tag("method", request.getMethod())
                                .tag("path", requestPath)
                                .tag("region", primaryRegion)
                                .tag(CORRELATION_ID_KEY, correlationId)
                                .register(meterRegistry));

                request.setAttribute("timer.sample", Timer.start(meterRegistry));
                request.setAttribute(CORRELATION_ID_KEY, correlationId);
                request.setAttribute("start.time", System.currentTimeMillis());

                logger.debug("Cross-region tracing initialized for request: {} with correlation ID: {}",
                        requestPath, correlationId);

            } catch (Exception e) {
                logger.error("Error in cross-region tracing interceptor preHandle", e);
            }

            return true;
        }

        @Override
        public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                @NonNull Object handler, @Nullable Exception ex) {
            try {
                // Stop performance timer
                Timer.Sample sample = (Timer.Sample) request.getAttribute("timer.sample");
                String correlationId = (String) request.getAttribute(CORRELATION_ID_KEY);
                Long startTime = (Long) request.getAttribute("start.time");

                if (sample != null) {
                    String timerKey = request.getMethod() + "_" + request.getRequestURI();
                    Timer timer = timers.get(timerKey);
                    if (timer != null) {
                        sample.stop(timer);
                    }
                }

                // Calculate request duration
                long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

                // Add performance annotations to X-Ray segment
                Segment segment = AWSXRay.getCurrentSegment();
                if (segment != null) {
                    segment.putAnnotation("response.status", response.getStatus());
                    segment.putAnnotation("request.duration", duration);
                    segment.putAnnotation("is.slow.request", duration > slowRequestThreshold);
                    segment.putAnnotation("is.error", response.getStatus() >= 400);

                    // Add performance metadata
                    segment.putMetadata(PERFORMANCE_KEY, "duration-ms", duration);
                    segment.putMetadata(PERFORMANCE_KEY, "is-slow", duration > slowRequestThreshold);
                    segment.putMetadata(PERFORMANCE_KEY, "threshold-ms", slowRequestThreshold);

                    // Add response metadata
                    segment.putMetadata("http", "status-code", response.getStatus());
                    segment.putMetadata("http", "content-type", response.getContentType());

                    // Business metrics
                    addBusinessMetrics(segment, request, response, duration);

                    if (ex != null) {
                        segment.addException(ex);
                        segment.putAnnotation("error.type", ex.getClass().getSimpleName());
                        segment.putMetadata("error", "message", ex.getMessage());
                        segment.putMetadata("error", "stack-trace", getStackTrace(ex));
                    }
                }

                // Log performance metrics
                if (duration > slowRequestThreshold) {
                    logger.warn("Slow request detected: {} {} took {}ms (correlation ID: {})",
                            request.getMethod(), request.getRequestURI(), duration, correlationId);
                }

                logger.debug("Cross-region tracing completed for request: {} (duration: {}ms, correlation ID: {})",
                        request.getRequestURI(), duration, correlationId);

            } catch (Exception e) {
                logger.error("Error in cross-region tracing interceptor afterCompletion", e);
            }
        }

        private String getOrGenerateCorrelationId(HttpServletRequest request) {
            String correlationId = request.getHeader(correlationIdHeader);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = generateCorrelationId();
            }
            return correlationId;
        }

        private String generateCorrelationId() {
            return primaryRegion + "-" + System.currentTimeMillis() + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
        }

        private void addBusinessContext(Segment segment, HttpServletRequest request) {
            String requestPath = request.getRequestURI();

            // Identify business context based on request path
            if (requestPath.contains("/orders")) {
                segment.putAnnotation(BUSINESS_CONTEXT_KEY, "order-processing");
                segment.putMetadata(BUSINESS_KEY, "flow", "order-processing");
            } else if (requestPath.contains("/payments")) {
                segment.putAnnotation(BUSINESS_CONTEXT_KEY, "payment-processing");
                segment.putMetadata(BUSINESS_KEY, "flow", "payment-processing");
            } else if (requestPath.contains("/customers")) {
                segment.putAnnotation(BUSINESS_CONTEXT_KEY, "customer-management");
                segment.putMetadata(BUSINESS_KEY, "flow", "customer-management");
            } else if (requestPath.contains("/auth")) {
                segment.putAnnotation(BUSINESS_CONTEXT_KEY, "authentication");
                segment.putMetadata(BUSINESS_KEY, "flow", "authentication");
            }

            // Add user journey tracking
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                if (userAgent.contains("Mobile")) {
                    segment.putAnnotation(USER_DEVICE_KEY, "mobile");
                } else if (userAgent.contains("Tablet")) {
                    segment.putAnnotation(USER_DEVICE_KEY, "tablet");
                } else {
                    segment.putAnnotation(USER_DEVICE_KEY, "desktop");
                }
            }
        }

        private void addBusinessMetrics(Segment segment, HttpServletRequest request,
                HttpServletResponse response, long duration) {
            String requestPath = request.getRequestURI();
            int statusCode = response.getStatus();

            // Track conversion metrics
            if (requestPath.contains("/orders") && request.getMethod().equals("POST")) {
                segment.putMetadata(BUSINESS_METRICS_KEY, "order-attempt", true);
                segment.putMetadata(BUSINESS_METRICS_KEY, "order-success", statusCode < 400);
            }

            if (requestPath.contains("/payments") && request.getMethod().equals("POST")) {
                segment.putMetadata(BUSINESS_METRICS_KEY, "payment-attempt", true);
                segment.putMetadata(BUSINESS_METRICS_KEY, "payment-success", statusCode < 400);
            }

            if (requestPath.contains("/customers") && request.getMethod().equals("POST")) {
                segment.putMetadata(BUSINESS_METRICS_KEY, "registration-attempt", true);
                segment.putMetadata(BUSINESS_METRICS_KEY, "registration-success", statusCode < 400);
            }

            // Track performance impact on business
            segment.putMetadata(BUSINESS_METRICS_KEY, "performance-impact",
                    duration > slowRequestThreshold ? "high" : "normal");
        }

        private String getStackTrace(Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            return sw.toString();
        }
    }
}
