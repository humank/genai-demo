package solid.humank.genaidemo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import solid.humank.genaidemo.testutils.BaseTest;

/**
 * 簡化的 API 文檔測試
 * 
 * 專注於核心 API 文檔功能驗證，避免複雜的配置依賴
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("API 文檔基本功能測試")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.h2.console.enabled=false",
                "logging.level.org.springframework.web=ERROR",
                "spring.profiles.active=test",
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "spring.autoconfigure.exclude="
})
@org.springframework.context.annotation.Import({
                solid.humank.genaidemo.config.UnifiedTestHttpClientConfiguration.class
})
public class ApiDocumentationTest extends BaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("驗證 Swagger UI 基本可用性")
        public void shouldAccessSwaggerUI() throws Exception {
                // 測試 Swagger UI 重定向
                mockMvc.perform(get("/swagger-ui.html"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("驗證 OpenAPI 文檔端點可訪問")
        public void shouldAccessOpenAPIDocumentation() throws Exception {
                // 測試主要 API 文檔端點
                MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType("application/json"))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                JsonNode apiDoc = objectMapper.readTree(content);

                // 驗證基本結構
                assertThat(apiDoc.get("info")).isNotNull();
                assertThat(apiDoc.get("info").get("title").asText()).contains("GenAI Demo");
                assertThat(apiDoc.get("paths")).isNotNull();
                assertThat(apiDoc.get("paths").size()).isGreaterThan(0);
        }

        @Test
        @DisplayName("驗證 API 端點有基本文檔結構")
        public void shouldHaveBasicApiDocumentation() throws Exception {
                MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                JsonNode apiDoc = objectMapper.readTree(content);

                JsonNode paths = apiDoc.get("paths");
                assertThat(paths).isNotNull();

                // 驗證至少有一些 API 端點被文檔化
                assertThat(paths.size()).isGreaterThan(0);

                // 檢查是否有重要的業務端點
                boolean hasOrderEndpoint = paths.has("/api/orders") ||
                                paths.fieldNames().hasNext() &&
                                                paths.fieldNames().next().contains("order");

                boolean hasCustomerEndpoint = paths.has("/api/customers") ||
                                paths.fieldNames().hasNext() &&
                                                paths.toString().contains("customer");

                // 至少應該有一些業務相關的端點
                assertThat(hasOrderEndpoint || hasCustomerEndpoint).isTrue();
        }

        @Test
        @DisplayName("驗證 OpenAPI 規範基本完整性")
        public void shouldHaveValidOpenAPIStructure() throws Exception {
                MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                JsonNode apiDoc = objectMapper.readTree(content);

                // 驗證 OpenAPI 規範的必需字段
                assertThat(apiDoc.has("openapi")).isTrue();
                assertThat(apiDoc.has("info")).isTrue();
                assertThat(apiDoc.has("paths")).isTrue();

                // 驗證 OpenAPI 版本
                String openApiVersion = apiDoc.get("openapi").asText();
                assertThat(openApiVersion).startsWith("3.");

                // 驗證 info 區塊
                JsonNode info = apiDoc.get("info");
                assertThat(info.has("title")).isTrue();
                assertThat(info.has("version")).isTrue();
        }
}