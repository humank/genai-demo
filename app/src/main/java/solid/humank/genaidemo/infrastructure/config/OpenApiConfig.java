package solid.humank.genaidemo.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/** OpenAPI 配置 定義 API 文檔的基本資訊和結構 */
@Configuration
public class OpenApiConfig {

        private final Environment environment;

        @Value("${server.port:8080}")
        private String serverPort;

        @Value("${spring.application.name:genai-demo}")
        private String applicationName;

        public OpenApiConfig(Environment environment) {
                this.environment = environment;
        }

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(createApiInfo())
                                .servers(createServers())
                                .tags(createTags())
                                .components(createComponents())
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }

        /** 創建 API 基本資訊 */
        private Info createApiInfo() {
                return new Info()
                                .title("GenAI Demo - DDD 電商平台 API")
                                .description(
                                                """
                                                                基於領域驅動設計（DDD）的電商平台 API

                                                                ## 架構特色
                                                                - 遵循 DDD 分層架構
                                                                - 六邊形架構（Hexagonal Architecture）
                                                                - CQRS 和事件驅動設計
                                                                - 豐富的領域模型

                                                                ## 主要功能模組
                                                                - 訂單管理：完整的訂單生命週期
                                                                - 支付處理：多種支付方式和狀態管理
                                                                - 庫存管理：即時庫存追蹤和預留
                                                                - 產品管理：產品資訊和分類
                                                                - 促銷活動：多樣化的促銷規則
                                                                - 定價策略：動態定價和佣金計算
                                                                - 客戶管理：會員等級和偏好設定

                                                                ## 錯誤處理
                                                                所有 API 都遵循統一的錯誤回應格式，包含錯誤代碼、訊息、時間戳記和詳細資訊。
                                                                """)
                                .version("1.0.0")
                                .contact(
                                                new Contact()
                                                                .name("GenAI Demo Team")
                                                                .email("contact@genai-demo.com")
                                                                .url("https://github.com/humank/genai-demo"))
                                .license(
                                                new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT"));
        }

        /** 創建環境特定的伺服器配置 */
        private List<Server> createServers() {
                List<Server> servers = new ArrayList<>();

                // 根據當前環境配置不同的伺服器
                String[] activeProfiles = environment.getActiveProfiles();

                if (activeProfiles.length == 0 || containsProfile(activeProfiles, "development")) {
                        // 開發環境
                        servers.add(new Server().url("http://localhost:" + serverPort).description("本地開發環境"));
                }

                if (containsProfile(activeProfiles, "test")) {
                        // 測試環境
                        servers.add(new Server().url("http://test-api.genai-demo.com").description("測試環境"));
                }

                if (containsProfile(activeProfiles, "staging")) {
                        // 預發布環境
                        servers.add(
                                        new Server().url("https://staging-api.genai-demo.com").description("預發布環境"));
                }

                if (containsProfile(activeProfiles, "production")) {
                        // 生產環境
                        servers.add(new Server().url("https://api.genai-demo.com").description("生產環境"));
                        servers.add(
                                        new Server().url("https://api-backup.genai-demo.com").description("生產環境備用"));
                }

                // 如果沒有匹配的環境，添加預設的本地環境
                if (servers.isEmpty()) {
                        servers.add(new Server().url("http://localhost:" + serverPort).description("預設本地環境"));
                }

                return servers;
        }

        /** 檢查是否包含指定的 profile */
        private boolean containsProfile(String[] profiles, String targetProfile) {
                for (String profile : profiles) {
                        if (profile.equals(targetProfile)) {
                                return true;
                        }
                }
                return false;
        }

        /** 創建標籤定義 */
        private List<Tag> createTags() {
                return List.of(
                                new Tag().name("訂單管理").description("訂單相關的 API 操作，包含訂單創建、查詢、更新和狀態管理"),
                                new Tag().name("支付處理").description("支付相關的 API 操作，包含支付創建、查詢和狀態更新"),
                                new Tag().name("庫存管理").description("庫存相關的 API 操作，包含庫存查詢、預留和釋放"),
                                new Tag().name("產品管理").description("產品相關的 API 操作，包含產品資訊管理和分類"),
                                new Tag().name("客戶管理").description("客戶相關的 API 操作，包含客戶資訊和會員管理"),
                                new Tag().name("定價策略").description("定價相關的 API 操作，包含定價規則和佣金計算"),
                                new Tag().name("統計報表").description("統計數據相關的 API 操作，包含各種業務指標查詢"),
                                new Tag().name("活動記錄").description("活動記錄相關的 API 操作，包含系統活動追蹤"));
        }

        /** 創建組件配置 */
        private Components createComponents() {
                return new Components()
                                .addSecuritySchemes("bearerAuth", createBearerAuthScheme())
                                .addSecuritySchemes("apiKey", createApiKeyScheme())
                                .addSchemas("StandardErrorResponse", createStandardErrorResponseSchema())
                                .addSchemas("FieldError", createFieldErrorSchema())
                                .addSchemas("ValidationErrorResponse", createValidationErrorResponseSchema())
                                .addSchemas("BusinessErrorResponse", createBusinessErrorResponseSchema());
        }

        /** 創建 Bearer Token 認證方案 */
        private SecurityScheme createBearerAuthScheme() {
                return new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("請在此處輸入 JWT Token，格式：Bearer {token}");
        }

        /** 創建 API Key 認證方案 */
        private SecurityScheme createApiKeyScheme() {
                return new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("請在此處輸入 API Key");
        }

        /** 創建標準錯誤回應 Schema */
        private Schema<Object> createStandardErrorResponseSchema() {
                Schema<Object> schema = new Schema<>();
                schema.type("object")
                                .description("標準錯誤回應")
                                .addProperty(
                                                "code",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("錯誤代碼")
                                                                .example("VALIDATION_ERROR"))
                                .addProperty(
                                                "message",
                                                new Schema<String>().type("string").description("錯誤訊息")
                                                                .example("請求參數驗證失敗"))
                                .addProperty(
                                                "timestamp",
                                                new Schema<String>()
                                                                .type("string")
                                                                .format("date-time")
                                                                .description("錯誤發生時間")
                                                                .example("2024-01-15T10:30:00"))
                                .addProperty(
                                                "path",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("請求路徑")
                                                                .example("/api/orders"))
                                .addProperty(
                                                "details",
                                                createArraySchema("詳細錯誤資訊", "#/components/schemas/FieldError"))
                                .addProperty(
                                                "traceId",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("追蹤ID，用於問題排查")
                                                                .example("abc123def456"));
                schema.setRequired(List.of("code", "message", "timestamp", "path"));
                return schema;
        }

        /** 創建欄位錯誤 Schema */
        private Schema<Object> createFieldErrorSchema() {
                Schema<Object> schema = new Schema<>();
                schema.type("object")
                                .description("欄位錯誤詳情")
                                .addProperty(
                                                "field",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("欄位名稱")
                                                                .example("customerId"))
                                .addProperty(
                                                "message",
                                                new Schema<String>().type("string").description("錯誤訊息")
                                                                .example("客戶ID不能為空"))
                                .addProperty(
                                                "rejectedValue",
                                                new Schema<Object>().description("拒絕的值").example("null"))
                                .addProperty(
                                                "code",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("錯誤代碼")
                                                                .example("NotBlank"));
                schema.setRequired(List.of("field", "message"));
                return schema;
        }

        /** 創建驗證錯誤回應 Schema */
        private Schema<Object> createValidationErrorResponseSchema() {
                Schema<Object> schema = new Schema<>();
                schema.type("object")
                                .description("驗證錯誤回應，繼承自標準錯誤回應")
                                .addProperty(
                                                "code",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("錯誤代碼")
                                                                .example("VALIDATION_ERROR"))
                                .addProperty(
                                                "message",
                                                new Schema<String>().type("string").description("錯誤訊息")
                                                                .example("請求參數驗證失敗"))
                                .addProperty(
                                                "timestamp",
                                                new Schema<String>()
                                                                .type("string")
                                                                .format("date-time")
                                                                .description("錯誤發生時間")
                                                                .example("2024-01-15T10:30:00"))
                                .addProperty(
                                                "path",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("請求路徑")
                                                                .example("/api/orders"))
                                .addProperty(
                                                "details",
                                                createArraySchema("詳細驗證錯誤資訊", "#/components/schemas/FieldError"))
                                .addProperty(
                                                "traceId",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("追蹤ID")
                                                                .example("abc123def456"))
                                .addProperty(
                                                "validationErrors",
                                                new Schema<Integer>().type("integer").description("驗證錯誤總數").example(3));
                schema.setRequired(List.of("code", "message", "timestamp", "path", "details"));
                return schema;
        }

        /** 創建陣列 Schema 的輔助方法 */
        private Schema<Object> createArraySchema(String description, String itemsRef) {
                Schema<Object> arraySchema = new Schema<>();
                arraySchema.type("array").description(description);

                Schema<Object> itemSchema = new Schema<>();
                itemSchema.$ref(itemsRef);
                arraySchema.setItems(itemSchema);

                return arraySchema;
        }

        /** 創建業務錯誤回應 Schema */
        private Schema<Object> createBusinessErrorResponseSchema() {
                Schema<Object> schema = new Schema<>();
                schema.type("object")
                                .description("業務邏輯錯誤回應")
                                .addProperty(
                                                "code",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("業務錯誤代碼")
                                                                .example("INSUFFICIENT_INVENTORY"))
                                .addProperty(
                                                "message",
                                                new Schema<String>().type("string").description("業務錯誤訊息")
                                                                .example("庫存不足"))
                                .addProperty(
                                                "timestamp",
                                                new Schema<String>()
                                                                .type("string")
                                                                .format("date-time")
                                                                .description("錯誤發生時間")
                                                                .example("2024-01-15T10:30:00"))
                                .addProperty(
                                                "path",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("請求路徑")
                                                                .example("/api/orders"))
                                .addProperty(
                                                "businessContext",
                                                new Schema<Object>()
                                                                .type("object")
                                                                .description("業務上下文資訊")
                                                                .example(
                                                                                "{\"productId\": \"P001\", \"requestedQuantity\":"
                                                                                                + " 10, \"availableQuantity\": 5}"))
                                .addProperty(
                                                "traceId",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("追蹤ID")
                                                                .example("abc123def456"))
                                .addProperty(
                                                "suggestedAction",
                                                new Schema<String>()
                                                                .type("string")
                                                                .description("建議的解決方案")
                                                                .example("請減少訂購數量或選擇其他產品"));
                schema.setRequired(List.of("code", "message", "timestamp", "path"));
                return schema;
        }
}