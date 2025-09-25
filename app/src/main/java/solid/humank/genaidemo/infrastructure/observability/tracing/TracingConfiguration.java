package solid.humank.genaidemo.infrastructure.observability.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * OpenTelemetry tracing configuration for distributed tracing
 * Supports both development (Jaeger) and production (OTLP) environments
 */
@Configuration
public class TracingConfiguration {

        private static final Logger log = LoggerFactory.getLogger(TracingConfiguration.class);

        /**
         * Development profile tracing configuration using Jaeger
         */
        @Bean
        @Profile("local")
        @ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
        public OpenTelemetry developmentOpenTelemetry() {
                log.info("Configuring OpenTelemetry for development environment");

                try {
                        return OpenTelemetrySdk.builder()
                                        .buildAndRegisterGlobal();
                } catch (IllegalStateException e) {
                        log.warn("GlobalOpenTelemetry already set, returning existing instance: {}", e.getMessage());
                        return GlobalOpenTelemetry.get();
                }
        }

        /**
         * Production profile tracing configuration using OTLP
         */
        @Bean
        @Profile({ "production", "staging" })
        @ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
        public OpenTelemetry productionOpenTelemetry() {
                log.info("Configuring OpenTelemetry for production environment");

                try {
                        return OpenTelemetrySdk.builder()
                                        .buildAndRegisterGlobal();
                } catch (IllegalStateException e) {
                        log.warn("GlobalOpenTelemetry already set, returning existing instance: {}", e.getMessage());
                        return GlobalOpenTelemetry.get();
                }
        }

        /**
         * Test profile tracing configuration - uses real SDK for testing
         */
        @Bean
        @Profile("test")
        @ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
        public OpenTelemetry testOpenTelemetry() {
                log.info("Configuring OpenTelemetry for test environment");

                try {
                        return OpenTelemetrySdk.builder()
                                        .buildAndRegisterGlobal();
                } catch (IllegalStateException e) {
                        log.warn("GlobalOpenTelemetry already set, returning existing instance: {}", e.getMessage());
                        return GlobalOpenTelemetry.get();
                }
        }

        /**
         * Tracer bean for all profiles
         */
        @Bean
        public io.opentelemetry.api.trace.Tracer tracer(OpenTelemetry openTelemetry) {
                return openTelemetry.getTracer("genai-demo", "1.0.0");
        }

        /**
         * Disabled tracing configuration for testing
         */
        @Bean
        @ConditionalOnProperty(name = "tracing.enabled", havingValue = "false")
        public OpenTelemetry disabledOpenTelemetry() {
                log.info("OpenTelemetry tracing is disabled");
                return OpenTelemetry.noop();
        }
}