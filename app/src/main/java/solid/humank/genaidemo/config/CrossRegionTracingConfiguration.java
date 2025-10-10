package solid.humank.genaidemo.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
// import com.amazonaws.xray.plugins.ElastiCachePlugin;
// import com.amazonaws.xray.plugins.RDSPlugin;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-Region Distributed Tracing Configuration
 * 
 * This configuration class sets up enhanced X-Ray tracing with cross-region correlation,
 * performance tracking, and business context propagation.
 */
@Configuration
@ConditionalOnEnabledTracing
@ConditionalOnProperty(name = "app.tracing.cross-region.enabled", havingValue = "true", matchIfMissing = true)
public class CrossRegionTracingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionTracingConfiguration.class);

    @Value("${aws.region:ap-east-2}")
    private String primaryRegion;

    @Value("${app.tracing.cross-region.regions.secondary:ap-northeast-1}")
    private String secondaryRegion;

    @Value("${app.tracing.cross-region.regions.tertiary:us-west-2}")
    private String tertiaryRegion;

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

    /**
     * Cross-Region Tracing Interceptor
     */
    @Bean
    public CrossRegionTracingInterceptor crossRegionTracingInterceptor(MeterRegistry meterRegistry) {
        return new CrossRegionTracingInterceptor(meterRegistry);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(crossRegionTracingInterceptor(null))
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
        public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
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
                response.setHeader(requestPathHeader, requestPath);

                // Add annotations to X-Ray segment
                Segment segment = AWSXRay.getCurrentSegment();
                if (segment != null) {
                    segment.putAnnotation("correlation.id", correlationId);
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
                    segment.putMetadata("cross-region", "correlation-id", correlationId);
                    segment.putMetadata("cross-region", "source-region", sourceRegion);
                    segment.putMetadata("cross-region", "target-region", primaryRegion);
                    segment.putMetadata("cross-region", "is-cross-region", !primaryRegion.equals(sourceRegion));
                    
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
                        .tag("correlation.id", correlationId)
                        .register(meterRegistry));
                
                request.setAttribute("timer.sample", Timer.start(meterRegistry));
                request.setAttribute("correlation.id", correlationId);
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
                String correlationId = (String) request.getAttribute("correlation.id");
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
                    segment.putMetadata("performance", "duration-ms", duration);
                    segment.putMetadata("performance", "is-slow", duration > slowRequestThreshold);
                    segment.putMetadata("performance", "threshold-ms", slowRequestThreshold);
                    
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
            return primaryRegion + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        private void addBusinessContext(Segment segment, HttpServletRequest request) {
            String requestPath = request.getRequestURI();
            
            // Identify business context based on request path
            if (requestPath.contains("/orders")) {
                segment.putAnnotation("business.context", "order-processing");
                segment.putMetadata("business", "flow", "order-processing");
            } else if (requestPath.contains("/payments")) {
                segment.putAnnotation("business.context", "payment-processing");
                segment.putMetadata("business", "flow", "payment-processing");
            } else if (requestPath.contains("/customers")) {
                segment.putAnnotation("business.context", "customer-management");
                segment.putMetadata("business", "flow", "customer-management");
            } else if (requestPath.contains("/auth")) {
                segment.putAnnotation("business.context", "authentication");
                segment.putMetadata("business", "flow", "authentication");
            }
            
            // Add user journey tracking
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                if (userAgent.contains("Mobile")) {
                    segment.putAnnotation("user.device", "mobile");
                } else if (userAgent.contains("Tablet")) {
                    segment.putAnnotation("user.device", "tablet");
                } else {
                    segment.putAnnotation("user.device", "desktop");
                }
            }
        }

        private void addBusinessMetrics(Segment segment, HttpServletRequest request, 
                                      HttpServletResponse response, long duration) {
            String requestPath = request.getRequestURI();
            int statusCode = response.getStatus();
            
            // Track conversion metrics
            if (requestPath.contains("/orders") && request.getMethod().equals("POST")) {
                segment.putMetadata("business-metrics", "order-attempt", true);
                segment.putMetadata("business-metrics", "order-success", statusCode < 400);
            }
            
            if (requestPath.contains("/payments") && request.getMethod().equals("POST")) {
                segment.putMetadata("business-metrics", "payment-attempt", true);
                segment.putMetadata("business-metrics", "payment-success", statusCode < 400);
            }
            
            if (requestPath.contains("/customers") && request.getMethod().equals("POST")) {
                segment.putMetadata("business-metrics", "registration-attempt", true);
                segment.putMetadata("business-metrics", "registration-success", statusCode < 400);
            }
            
            // Track performance impact on business
            segment.putMetadata("business-metrics", "performance-impact", 
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