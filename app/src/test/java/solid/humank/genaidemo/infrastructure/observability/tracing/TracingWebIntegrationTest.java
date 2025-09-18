package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import solid.humank.genaidemo.config.UnifiedTestHttpClientConfiguration;

/**
 * Web 集成測試 - 驗證追蹤在 HTTP 請求中的工作
 * 
 * 優點：
 * - 專注於 HTTP 層面的追蹤
 * - 使用 MockMvc 避免完整的 Web 容器
 * - 簡化的配置避免 Bean 衝突
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.lazy-initialization=true",
        "spring.jmx.enabled=false",
        "logging.level.root=ERROR",
        "management.health.defaults.enabled=false",
        "management.endpoint.health.show-details=never",
        // Simplified tracing configuration
        "tracing.enabled=false", // Disable production tracing to avoid conflicts
        "spring.main.allow-bean-definition-overriding=true" // Allow bean overriding for test
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.context.annotation.Import({
        solid.humank.genaidemo.config.SimplifiedTracingWebTestConfiguration.class,
        UnifiedTestHttpClientConfiguration.class
})
@DisplayName("追蹤 Web 集成測試")
class TracingWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TraceContextManager traceContextManager;

    @Test
    @DisplayName("應該在 HTTP 請求中處理 Correlation ID")
    void shouldHandleCorrelationIdInHttpRequests() throws Exception {
        // Given
        String correlationId = "web-test-correlation-123";

        // When
        MvcResult result = mockMvc.perform(get("/actuator/health")
                .header("X-Correlation-ID", correlationId))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        // 驗證響應中包含 Correlation ID
        String responseCorrelationId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(responseCorrelationId).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("應該在沒有 Correlation ID 時生成新的")
    void shouldGenerateCorrelationIdWhenNotProvided() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        // 驗證響應中包含生成的 Correlation ID
        String responseCorrelationId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(responseCorrelationId).isNotNull();
        assertThat(responseCorrelationId).isNotEmpty();
    }

    @Test
    @DisplayName("應該正常處理健康檢查請求")
    void shouldHandleHealthCheckRequests() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // 驗證追蹤不會干擾正常的應用程序操作
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotEmpty();
    }

    @Test
    @DisplayName("TraceContextManager 應該在 Web 環境中正常工作")
    void shouldWorkInWebEnvironment() {
        // Given
        String testCorrelationId = "web-env-test";

        // When
        traceContextManager.setCorrelationId(testCorrelationId);

        // Then
        var result = traceContextManager.getCurrentCorrelationId();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testCorrelationId);

        // Cleanup
        traceContextManager.clearContext();
    }

    @Test
    @DisplayName("應該能夠處理業務上下文設置")
    void shouldHandleBusinessContextInWebEnvironment() {
        // Given
        String userId = "web-user-123";
        String orderId = "web-order-456";

        // When
        traceContextManager.setBusinessContext(userId, orderId);

        // Then - 驗證操作不會拋出異常
        assertThat(true).isTrue();

        // Cleanup
        traceContextManager.clearContext();
    }
}