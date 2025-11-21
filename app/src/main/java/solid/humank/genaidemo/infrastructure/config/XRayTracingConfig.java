package solid.humank.genaidemo.infrastructure.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.plugins.EC2Plugin;
// ECS Plugin removed - using EKS only
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.util.Map;

/**
 * AWS X-Ray Distributed Tracing Configuration
 * 
 * This configuration enables X-Ray distributed tracing for the GenAI Demo application
 * running in EKS with comprehensive trace collection and analysis.
 * 
 * Features:
 * - Automatic trace collection for HTTP requests
 * - Service method tracing with annotations
 * - Database query tracing
 * - External service call tracing
 * - Custom business logic tracing
 * - Integration with CloudWatch and Grafana
 * 
 * Created: 2025年9月24日 下午6:23 (台北時間)
 */
@Configuration
@Profile({"staging", "production"})
@ConditionalOnProperty(name = "aws.xray.enabled", havingValue = "true")
@EnableAspectJAutoProxy
public class XRayTracingConfig {

    private static final Logger logger = LoggerFactory.getLogger(XRayTracingConfig.class);

    @Value("${aws.xray.tracing-name:genai-demo}")
    private String tracingName;

    @Value("${aws.xray.daemon-address:127.0.0.1:2000}")
    private String daemonAddress;

    @Value("${aws.xray.sampling.rate:0.1}")
    private double samplingRate;

    @Value("${aws.xray.sampling.reservoir:1}")
    private int samplingReservoir;

    @Value("${spring.profiles.active:development}")
    private String environment;

    /**
     * Initialize X-Ray recorder with plugins and configuration
     */
    @PostConstruct
    public void init() {
        try {
            // Configure X-Ray recorder
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                    .withPlugin(new EC2Plugin())
                    .withPlugin(new EKSPlugin());

            // Set context missing strategy
            builder.withContextMissingStrategy(new LogErrorContextMissingStrategy());

            // Configure sampling strategy
            if (samplingRate > 0) {
                URL samplingRulesFile = getClass().getClassLoader().getResource("xray-sampling-rules.json");
                if (samplingRulesFile != null) {
                    builder.withSamplingStrategy(new LocalizedSamplingStrategy(samplingRulesFile));
                }
            }

            // Set daemon address
            if (daemonAddress != null && !daemonAddress.isEmpty()) {
                System.setProperty("com.amazonaws.xray.emitters.daemonAddress", daemonAddress);
            }

            // Build and set global recorder
            AWSXRay.setGlobalRecorder(builder.build());

            logger.info("X-Ray tracing initialized successfully for service: {}, environment: {}", 
                       tracingName, environment);

        } catch (Exception e) {
            logger.error("Failed to initialize X-Ray tracing", e);
        }
    }

    /**
     * X-Ray interceptor for service layer tracing
     */
    @Aspect
    public static class XRayServiceInterceptor extends AbstractXRayInterceptor {
        
        @Override
        protected Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp, Subsegment subsegment) {
            return super.generateMetadata(pjp, subsegment);
        }

        @Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled) && execution(* solid.humank.genaidemo.application..*(..))")
        public void xrayEnabledClasses() {}

        @Pointcut("execution(* solid.humank.genaidemo.application..*Service.*(..))")
        public void serviceLayer() {}

        @Pointcut("execution(* solid.humank.genaidemo.infrastructure..*Repository.*(..))")
        public void repositoryLayer() {}

        @Pointcut("execution(* solid.humank.genaidemo.interfaces..*Controller.*(..))")
        public void controllerLayer() {}

        @Pointcut("xrayEnabledClasses() || serviceLayer() || repositoryLayer()")
        public void xrayTrace() {}
    }

    /**
     * Register X-Ray service interceptor
     */
    @Bean
    public XRayServiceInterceptor xrayServiceInterceptor() {
        return new XRayServiceInterceptor();
    }

    /**
     * Custom X-Ray tracing utilities
     */
    @Bean
    public XRayTracingUtils xrayTracingUtils() {
        return new XRayTracingUtils();
    }

    /**
     * Utility class for custom X-Ray tracing
     */
    public static class XRayTracingUtils {

        private static final Logger logger = LoggerFactory.getLogger(XRayTracingUtils.class);

        /**
         * Create a custom subsegment for business logic tracing
         */
        public void traceBusinessOperation(String operationName, Runnable operation) {
            Subsegment subsegment = AWSXRay.beginSubsegment(operationName);
            try {
                subsegment.putAnnotation("operation_type", "business");
                subsegment.putAnnotation("service", "genai-demo");
                operation.run();
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }
        }

        /**
         * Create a custom subsegment with return value for business logic tracing
         */
        public <T> T traceBusinessOperation(String operationName, java.util.function.Supplier<T> operation) {
            Subsegment subsegment = AWSXRay.beginSubsegment(operationName);
            try {
                subsegment.putAnnotation("operation_type", "business");
                subsegment.putAnnotation("service", "genai-demo");
                T result = operation.get();
                subsegment.putMetadata("result_type", result != null ? result.getClass().getSimpleName() : "null");
                return result;
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }
        }

        /**
         * Add custom annotations to current segment
         */
        public void addAnnotation(String key, String value) {
            try {
                Segment segment = AWSXRay.getCurrentSegment();
                if (segment != null) {
                    segment.putAnnotation(key, value);
                }
            } catch (Exception e) {
                logger.debug("Failed to add X-Ray annotation: {}", e.getMessage());
            }
        }

        /**
         * Add custom metadata to current segment
         */
        public void addMetadata(String namespace, String key, Object value) {
            try {
                Segment segment = AWSXRay.getCurrentSegment();
                if (segment != null) {
                    segment.putMetadata(namespace, key, value);
                }
            } catch (Exception e) {
                logger.debug("Failed to add X-Ray metadata: {}", e.getMessage());
            }
        }

        /**
         * Trace external service calls
         */
        public <T> T traceExternalCall(String serviceName, String operation, java.util.function.Supplier<T> call) {
            Subsegment subsegment = AWSXRay.beginSubsegment(serviceName);
            try {
                subsegment.putAnnotation("service_type", "external");
                subsegment.putAnnotation("operation", operation);
                subsegment.setNamespace("remote");
                return call.get();
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }
        }

        /**
         * Trace database operations
         */
        public <T> T traceDatabaseOperation(String tableName, String operation, java.util.function.Supplier<T> dbCall) {
            Subsegment subsegment = AWSXRay.beginSubsegment("database");
            try {
                subsegment.putAnnotation("table_name", tableName);
                subsegment.putAnnotation("operation", operation);
                subsegment.setNamespace("remote");
                return dbCall.get();
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }
        }
    }
}