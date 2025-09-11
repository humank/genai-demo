package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.api.OpenTelemetry;

/**
 * 輕量級單元測試 - TracingConfiguration
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~2s)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tracing Configuration Unit Tests")
class TracingConfigurationUnitTest {

        private TracingConfiguration tracingConfiguration;

        @BeforeEach
        void setUp() {
                tracingConfiguration = new TracingConfiguration();
        }

        @Test
        @DisplayName("Should create OpenTelemetry bean for test environment")
        void shouldCreateOpenTelemetryBeanForTestEnvironment() {
                // When
                OpenTelemetry openTelemetry = tracingConfiguration.testOpenTelemetry();

                // Then
                assertThat(openTelemetry).isNotNull();
                assertThat(openTelemetry.getTracer("test-tracer")).isNotNull();
        }

        @Test
        @DisplayName("Should create OpenTelemetry bean for development environment")
        void shouldCreateOpenTelemetryBeanForDevelopmentEnvironment() {
                // When
                OpenTelemetry openTelemetry = tracingConfiguration.developmentOpenTelemetry();

                // Then
                assertThat(openTelemetry).isNotNull();
                assertThat(openTelemetry.getTracer("test-tracer")).isNotNull();
        }

        @Test
        @DisplayName("Should create OpenTelemetry bean for production environment")
        void shouldCreateOpenTelemetryBeanForProductionEnvironment() {
                // When
                OpenTelemetry openTelemetry = tracingConfiguration.productionOpenTelemetry();

                // Then
                assertThat(openTelemetry).isNotNull();
                assertThat(openTelemetry.getTracer("test-tracer")).isNotNull();
        }

        @Test
        @DisplayName("Should create disabled OpenTelemetry when tracing is disabled")
        void shouldCreateDisabledOpenTelemetryWhenTracingDisabled() {
                // When
                OpenTelemetry openTelemetry = tracingConfiguration.disabledOpenTelemetry();

                // Then
                assertThat(openTelemetry).isNotNull();
                // No-op OpenTelemetry still provides tracer but doesn't actually trace
                assertThat(openTelemetry.getTracer("test-tracer")).isNotNull();
        }

        @Test
        @DisplayName("Should create tracer bean with OpenTelemetry instance")
        void shouldCreateTracerBeanWithOpenTelemetryInstance() {
                // Given
                OpenTelemetry openTelemetry = tracingConfiguration.testOpenTelemetry();

                // When
                io.opentelemetry.api.trace.Tracer tracer = tracingConfiguration.tracer(openTelemetry);

                // Then
                assertThat(tracer).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple OpenTelemetry instances gracefully")
        void shouldHandleMultipleOpenTelemetryInstancesGracefully() {
                // When - 創建多個實例
                OpenTelemetry openTelemetry1 = tracingConfiguration.testOpenTelemetry();
                OpenTelemetry openTelemetry2 = tracingConfiguration.disabledOpenTelemetry();

                // Then
                assertThat(openTelemetry1).isNotNull();
                assertThat(openTelemetry2).isNotNull();

                // 兩個實例都應該能創建 tracer
                assertThat(openTelemetry1.getTracer("test-tracer-1")).isNotNull();
                assertThat(openTelemetry2.getTracer("test-tracer-2")).isNotNull();
        }

        @Test
        @DisplayName("Should verify configuration class structure")
        void shouldVerifyConfigurationClassStructure() {
                // Then - 驗證配置類的基本結構
                assertThat(tracingConfiguration).isNotNull();
                assertThat(tracingConfiguration.getClass().getAnnotations()).isNotEmpty();

                // 驗證類有必要的方法
                assertThat(tracingConfiguration.getClass().getDeclaredMethods())
                                .extracting("name")
                                .contains("testOpenTelemetry", "developmentOpenTelemetry", "productionOpenTelemetry",
                                                "disabledOpenTelemetry", "tracer");
        }
}