package solid.humank.genaidemo.infrastructure.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import solid.humank.genaidemo.infrastructure.observability.logging.LoggingContextManager;
import solid.humank.genaidemo.infrastructure.observability.tracing.TracingConfiguration;

/**
 * Configuration class for observability features including structured logging,
 * metrics collection, and distributed tracing.
 */
@Configuration
@EnableAspectJAutoProxy
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.context.annotation.ComponentScan(basePackages = "solid.humank.genaidemo.infrastructure.observability")
@Import(TracingConfiguration.class)
public class ObservabilityConfiguration {

    /**
     * Configure logging context manager for all profiles
     */
    @Bean
    public LoggingContextManager loggingContextManager() {
        return new LoggingContextManager();
    }

    // Tracer bean is provided by TracingConfiguration

    /**
     * Configuration properties for observability features
     */
    @Bean
    @ConditionalOnProperty(name = "observability.enabled", havingValue = "true", matchIfMissing = true)
    public ObservabilityProperties observabilityProperties() {
        return new ObservabilityProperties();
    }

    /**
     * Properties class for observability configuration
     */
    public static class ObservabilityProperties {
        private boolean tracingEnabled = true;
        private boolean metricsEnabled = true;
        private boolean structuredLogging = true;
        private MdcProperties mdc = new MdcProperties();
        private TracingProperties tracing = new TracingProperties();

        public boolean isTracingEnabled() {
            return tracingEnabled;
        }

        public void setTracingEnabled(boolean tracingEnabled) {
            this.tracingEnabled = tracingEnabled;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isStructuredLogging() {
            return structuredLogging;
        }

        public void setStructuredLogging(boolean structuredLogging) {
            this.structuredLogging = structuredLogging;
        }

        public MdcProperties getMdc() {
            return mdc;
        }

        public void setMdc(MdcProperties mdc) {
            this.mdc = mdc;
        }

        public TracingProperties getTracing() {
            return tracing;
        }

        public void setTracing(TracingProperties tracing) {
            this.tracing = tracing;
        }

        public static class MdcProperties {
            private boolean correlationId = true;
            private boolean traceId = true;
            private boolean spanId = true;

            public boolean isCorrelationId() {
                return correlationId;
            }

            public void setCorrelationId(boolean correlationId) {
                this.correlationId = correlationId;
            }

            public boolean isTraceId() {
                return traceId;
            }

            public void setTraceId(boolean traceId) {
                this.traceId = traceId;
            }

            public boolean isSpanId() {
                return spanId;
            }

            public void setSpanId(boolean spanId) {
                this.spanId = spanId;
            }
        }

        public static class TracingProperties {
            private String exporter = "jaeger";
            private double samplingRatio = 1.0;
            private String jaegerEndpoint = "http://localhost:14250";
            private String otlpEndpoint = "http://localhost:4317";

            public String getExporter() {
                return exporter;
            }

            public void setExporter(String exporter) {
                this.exporter = exporter;
            }

            public double getSamplingRatio() {
                return samplingRatio;
            }

            public void setSamplingRatio(double samplingRatio) {
                this.samplingRatio = samplingRatio;
            }

            public String getJaegerEndpoint() {
                return jaegerEndpoint;
            }

            public void setJaegerEndpoint(String jaegerEndpoint) {
                this.jaegerEndpoint = jaegerEndpoint;
            }

            public String getOtlpEndpoint() {
                return otlpEndpoint;
            }

            public void setOtlpEndpoint(String otlpEndpoint) {
                this.otlpEndpoint = otlpEndpoint;
            }
        }
    }
}