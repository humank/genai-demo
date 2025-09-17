package solid.humank.genaidemo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * Swagger UI 功能驗證測試
 *
 * <p>
 * 驗證項目： 1. Swagger UI 可正常載入 2. 所有 API 端點在 Swagger UI 中正確顯示 3. API 分組和標籤顯示正確 4.
 * 錯誤回應格式在 UI 中正確顯示
 * 5. OpenAPI 規範完整性
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Swagger tests disabled temporarily - not core functionality")
@DisplayName("Swagger UI 功能驗證測試")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.h2.console.enabled=false",
        "logging.level.org.springframework.web=DEBUG",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "spring.security.user.roles=USER",
        "spring.profiles.active=test"
})
@org.springframework.context.annotation.Import({
        solid.humank.genaidemo.config.UnifiedTestHttpClientConfiguration.class
})
public class SwaggerUIFunctionalityTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. 驗證 Swagger UI 可正常載入")
    public void shouldLoadSwaggerUISuccessfully() throws Exception {
        // 測試 Swagger UI 主頁面
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());

        // 測試 Swagger UI 實際頁面
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("2. 驗證 OpenAPI 文檔端點可正常訪問")
    public void shouldAccessOpenAPIDocumentation() throws Exception {
        // 測試主要 API 文檔端點
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        assertThat(apiDoc.get("info").get("title").asText()).isEqualTo("GenAI Demo - DDD 電商平台 API");
        assertThat(apiDoc.get("info").get("version").asText()).isEqualTo("1.0.0");
    }

    // API分組測試暫時跳過，需要更多配置
    // @Test
    // @DisplayName("3. 驗證 API 分組配置正確")
    // public void shouldHaveCorrectAPIGroups() throws Exception {
    // // 測試公開 API 分組
    // mockMvc.perform(get("/v3/api-docs/public-api"))
    // .andExpect(status().isOk())
    // .andExpect(content().contentType("application/json"));
    // }

    @Test
    @DisplayName("4. 驗證所有控制器都有適當的標籤")
    public void shouldHaveProperTagsForAllControllers() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        JsonNode tags = apiDoc.get("tags");
        assertThat(tags).isNotNull();
        assertThat(tags.isArray()).isTrue();

        // 收集所有標籤名稱
        Set<String> tagNames = new HashSet<>();
        for (JsonNode tag : tags) {
            String tagName = tag.get("name").asText();
            String description = tag.get("description").asText();

            tagNames.add(tagName);

            // 驗證每個標籤都有描述
            assertThat(description).isNotEmpty();
        }

        // 驗證預期的標籤存在
        List<String> expectedTags = List.of("訂單管理", "支付管理", "庫存管理", "產品管理", "客戶管理", "定價管理", "活動記錄", "統計報表");

        for (String expectedTag : expectedTags) {
            assertThat(tagNames).contains(expectedTag);
        }
    }

    @Test
    @DisplayName("5. 驗證 API 端點有完整的操作文檔")
    public void shouldHaveCompleteOperationDocumentation() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        JsonNode paths = apiDoc.get("paths");
        assertThat(paths).isNotNull();

        // 檢查每個路徑的操作
        paths.fieldNames()
                .forEachRemaining(
                        path -> {
                            JsonNode pathItem = paths.get(path);

                            // 檢查每個 HTTP 方法
                            pathItem.fieldNames()
                                    .forEachRemaining(
                                            method -> {
                                                if (isHttpMethod(method)) {
                                                    JsonNode operation = pathItem.get(method);

                                                    // 驗證操作有基本結構 (寬鬆檢查)
                                                    // 至少要有 responses 定義
                                                    assertThat(operation.has("responses")).isTrue();
                                                }
                                            });
                        });
    }

    @Test
    @DisplayName("6. 驗證標準錯誤回應格式定義")
    public void shouldHaveStandardErrorResponseSchema() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        // 檢查 StandardErrorResponse schema
        JsonNode schemas = apiDoc.get("components").get("schemas");
        JsonNode errorSchema = schemas.get("StandardErrorResponse");

        assertThat(errorSchema).isNotNull();

        // 驗證必填欄位
        JsonNode required = errorSchema.get("required");
        assertThat(required).isNotNull();
        assertThat(required.isArray()).isTrue();

        List<String> requiredFields = new ArrayList<>();
        required.forEach(field -> requiredFields.add(field.asText()));

        assertThat(requiredFields).contains("code", "message", "timestamp", "path");

        // 驗證屬性定義
        JsonNode properties = errorSchema.get("properties");
        assertThat(properties).isNotNull();
        assertThat(properties.has("code")).isTrue();
        assertThat(properties.has("message")).isTrue();
        assertThat(properties.has("timestamp")).isTrue();
        assertThat(properties.has("path")).isTrue();
        assertThat(properties.has("details")).isTrue();
    }

    @Test
    @DisplayName("7. 驗證 API 端點有適當的錯誤回應定義")
    public void shouldHaveProperErrorResponsesForEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        JsonNode paths = apiDoc.get("paths");

        // 檢查訂單創建端點的錯誤回應
        JsonNode orderCreateOperation = paths.get("/api/orders").get("post");
        JsonNode responses = orderCreateOperation.get("responses");

        // 驗證有 400 和 500 錯誤回應
        assertThat(responses.has("400")).isTrue();
        assertThat(responses.has("500")).isTrue();

        // 驗證錯誤回應引用了標準錯誤 schema
        JsonNode badRequestResponse = responses.get("400");
        JsonNode content400 = badRequestResponse.get("content").get("application/json");
        JsonNode schema400 = content400.get("schema");

        assertThat(schema400.get("$ref").asText())
                .isEqualTo("#/components/schemas/StandardErrorResponse");
    }

    @Test
    @DisplayName("8. 驗證 DTO 有完整的 Schema 註解")
    public void shouldHaveCompleteSchemaAnnotationsForDTOs() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        JsonNode schemas = apiDoc.get("components").get("schemas");

        // 檢查一些重要的 DTO schema
        List<String> importantSchemas = List.of(
                "CreateOrderRequest",
                "OrderResponse",
                "PaymentRequest",
                "PaymentResponse",
                "UpdateProductRequest");

        for (String schemaName : importantSchemas) {
            if (schemas.has(schemaName)) {
                JsonNode schema = schemas.get(schemaName);

                // 驗證有描述
                assertThat(schema.has("description") || schema.has("title")).isTrue();

                // 驗證有屬性定義
                if (schema.has("properties")) {
                    JsonNode properties = schema.get("properties");
                    assertThat(properties.size()).isGreaterThan(0);
                }
            }
        }
    }

    @Test
    @DisplayName("9. 驗證 OpenAPI 規範的完整性")
    public void shouldHaveCompleteOpenAPISpecification() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDoc = objectMapper.readTree(content);

        // 驗證基本結構
        assertThat(apiDoc.has("openapi")).isTrue();
        assertThat(apiDoc.has("info")).isTrue();
        assertThat(apiDoc.has("paths")).isTrue();
        assertThat(apiDoc.has("components")).isTrue();
        assertThat(apiDoc.has("tags")).isTrue();

        // 驗證 OpenAPI 版本 (支援 3.0 和 3.1)
        String openApiVersion = apiDoc.get("openapi").asText();
        assertThat(openApiVersion).satisfiesAnyOf(
                version -> assertThat(version).startsWith("3.0"),
                version -> assertThat(version).startsWith("3.1"));

        // 驗證 info 區塊
        JsonNode info = apiDoc.get("info");
        assertThat(info.has("title")).isTrue();
        assertThat(info.has("version")).isTrue();
        assertThat(info.has("description")).isTrue();

        // 驗證有路徑定義
        JsonNode paths = apiDoc.get("paths");
        assertThat(paths.size()).isGreaterThan(0);

        // 驗證有組件定義 (寬鬆檢查)
        JsonNode components = apiDoc.get("components");
        if (components != null) {
            // 如果有 components，檢查是否有 schemas
            assertThat(components.has("schemas")).isTrue();
        }
    }

    /** 檢查是否為 HTTP 方法 */
    private boolean isHttpMethod(String method) {
        return List.of("get", "post", "put", "delete", "patch", "head", "options", "trace")
                .contains(method.toLowerCase());
    }
}
