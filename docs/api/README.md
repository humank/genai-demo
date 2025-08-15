# GenAI Demo - DDD 電商平台 API 文檔

歡迎使用 GenAI Demo DDD 電商平台的 API 文檔！本文檔提供完整的 API 使用指南，幫助開發者快速上手並有效使用我們的 RESTful API。

## 快速開始

### 1. 啟動應用程式

```bash
# 啟動後端服務
./gradlew bootRun

# 或使用全棧啟動腳本
./start-fullstack.sh
```

### 2. 訪問 Swagger UI

應用程式啟動後，可以通過以下 URL 訪問互動式 API 文檔：

- **Swagger UI**: <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON**: <http://localhost:8080/v3/api-docs>
- **OpenAPI YAML**: <http://localhost:8080/v3/api-docs.yaml>

### 3. 第一個 API 呼叫

```bash
# 獲取所有訂單
curl -X GET "http://localhost:8080/api/orders" \
     -H "accept: application/json"

# 創建新訂單
curl -X POST "http://localhost:8080/api/orders" \
     -H "accept: application/json" \
     -H "Content-Type: application/json" \
     -d '{
       "customerId": "customer-123",
       "items": [
         {
           "productId": "product-456",
           "quantity": 2,
           "unitPrice": 99.99
         }
       ]
     }'
```

## API 概覽

我們的 API 採用 RESTful 設計原則，提供以下核心功能模組：

### 核心業務模組

| 模組 | 基礎路徑 | 描述 |
|------|----------|------|
| 訂單管理 | `/api/orders` | 訂單的創建、查詢、更新和狀態管理 |
| 支付處理 | `/api/payments` | 支付流程、退款和支付狀態查詢 |
| 庫存管理 | `/api/inventory` | 庫存查詢、預留和調整 |
| 產品管理 | `/api/products` | 產品資訊的 CRUD 操作 |
| 客戶管理 | `/api/customers` | 客戶資料管理和查詢 |

### 支援服務模組

| 模組 | 基礎路徑 | 描述 |
|------|----------|------|
| 定價策略 | `/api/pricing` | 動態定價和佣金計算 |
| 活動記錄 | `/api/activities` | 系統活動和事件記錄 |
| 統計報表 | `/api/stats` | 業務統計和分析數據 |

## 模組詳細說明

### 訂單管理 (Orders)

訂單模組是電商平台的核心，提供完整的訂單生命週期管理。

**主要功能:**

- 創建新訂單
- 查詢訂單詳情
- 更新訂單狀態
- 添加/移除訂單項目
- 訂單歷史追蹤

**關鍵端點:**

- `POST /api/orders` - 創建訂單
- `GET /api/orders/{orderId}` - 獲取訂單詳情
- `PUT /api/orders/{orderId}/status` - 更新訂單狀態
- `POST /api/orders/{orderId}/items` - 添加訂單項目

### 支付處理 (Payments)

支付模組處理所有與金錢相關的交易。

**主要功能:**

- 處理支付請求
- 支付狀態查詢
- 退款處理
- 支付方式管理

**關鍵端點:**

- `POST /api/payments` - 創建支付
- `GET /api/payments/{paymentId}` - 查詢支付狀態
- `POST /api/payments/{paymentId}/refund` - 處理退款

### 庫存管理 (Inventory)

庫存模組確保產品可用性和庫存準確性。

**主要功能:**

- 庫存查詢
- 庫存預留
- 庫存調整
- 庫存警報

**關鍵端點:**

- `GET /api/inventory/{productId}` - 查詢庫存
- `POST /api/inventory/reserve` - 預留庫存
- `PUT /api/inventory/{productId}/adjust` - 調整庫存

## 開發指南

### 認證和授權

目前 API 支援 Bearer Token 認證：

```bash
curl -X GET "http://localhost:8080/api/orders" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 請求格式

所有 API 請求都應該：

- 使用 `Content-Type: application/json` 標頭
- 發送有效的 JSON 格式資料
- 包含必要的認證資訊

### 回應格式

#### 成功回應

```json
{
  "id": "order-123",
  "customerId": "customer-456",
  "status": "CONFIRMED",
  "totalAmount": 199.98,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### 錯誤回應

```json
{
  "code": "VALIDATION_ERROR",
  "message": "請求參數驗證失敗",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/orders",
  "details": [
    {
      "field": "customerId",
      "message": "客戶ID不能為空",
      "rejectedValue": null
    }
  ]
}
```

### 分頁和排序

支援分頁的端點使用以下參數：

```bash
GET /api/orders?page=0&size=20&sort=createdAt,desc
```

參數說明：

- `page`: 頁碼（從 0 開始）
- `size`: 每頁大小（預設 20）
- `sort`: 排序欄位和方向

### 錯誤處理

API 使用標準 HTTP 狀態碼：

| 狀態碼 | 說明 | 使用場景 |
|--------|------|----------|
| 200 | 成功 | 請求成功處理 |
| 201 | 已創建 | 資源成功創建 |
| 400 | 請求錯誤 | 參數驗證失敗 |
| 404 | 未找到 | 資源不存在 |
| 422 | 無法處理 | 業務規則違反 |
| 500 | 伺服器錯誤 | 系統內部錯誤 |

## 最佳實踐

### 1. API 設計原則

**遵循 RESTful 設計:**

- 使用適當的 HTTP 方法（GET、POST、PUT、DELETE）
- 使用有意義的資源路徑
- 保持 API 的一致性

**版本控制:**

- 在 URL 中包含版本號（如 `/api/v1/orders`）
- 向後相容性考量
- 適當的棄用通知

### 2. 新增 API 端點

當需要新增 API 端點時，請遵循以下步驟：

#### 步驟 1: 創建控制器

```java
@RestController
@RequestMapping("/api/your-module")
@Tag(name = "模組名稱", description = "模組功能描述")
@Validated
public class YourModuleController {
    
    @PostMapping
    @Operation(
        summary = "創建資源",
        description = "詳細的操作描述"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "創建成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = YourResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數無效",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandardErrorResponse.class)
            )
        )
    })
    public ResponseEntity<YourResponseDto> createResource(
        @Parameter(description = "請求參數描述", required = true)
        @Valid @RequestBody YourRequestDto request) {
        // 實作邏輯
    }
}
```

#### 步驟 2: 創建 DTO

```java
@Schema(description = "請求資料傳輸物件")
public class YourRequestDto {
    
    @Schema(
        description = "欄位描述",
        example = "範例值",
        required = true
    )
    @NotBlank(message = "欄位不能為空")
    private String field;
    
    // getter/setter
}
```

#### 步驟 3: 更新文檔

```bash
# 重新生成 API 文檔
./gradlew generateApiDocs

# 驗證文檔格式
./gradlew validateApiDocs
```

### 3. DTO 註解最佳實踐

**完整的 Schema 註解:**

```java
@Schema(
    description = "訂單創建請求",
    example = """
    {
      "customerId": "customer-123",
      "items": [
        {
          "productId": "product-456",
          "quantity": 2,
          "unitPrice": 99.99
        }
      ]
    }
    """
)
public class CreateOrderRequest {
    
    @Schema(
        description = "客戶唯一識別碼",
        example = "customer-123",
        required = true,
        pattern = "^customer-[0-9]+$"
    )
    @NotBlank(message = "客戶ID不能為空")
    @Pattern(regexp = "^customer-[0-9]+$", message = "客戶ID格式不正確")
    private String customerId;
    
    @Schema(
        description = "訂單項目列表",
        required = true,
        minItems = 1
    )
    @NotEmpty(message = "訂單項目不能為空")
    @Valid
    private List<OrderItemRequest> items;
}
```

### 4. 錯誤處理標準化

**統一的錯誤回應格式:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardErrorResponse handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {
        
        List<StandardErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new StandardErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
        return StandardErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("請求參數驗證失敗")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .details(fieldErrors)
            .build();
    }
}
```

## 配置選項和自訂

### OpenAPI 配置

主要配置位於 `OpenApiConfig.java`：

```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GenAI Demo - DDD 電商平台 API")
                .description("基於 DDD 架構的電商平台 RESTful API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("開發團隊")
                    .email("dev-team@example.com")
                    .url("https://github.com/your-org/genai-demo"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(Arrays.asList(
                new Server().url("http://localhost:8080").description("開發環境"),
                new Server().url("https://api-staging.example.com").description("測試環境"),
                new Server().url("https://api.example.com").description("生產環境")
            ));
    }
}
```

### 環境特定配置

#### application-openapi.yml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    try-it-out-enabled: true
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    show-extensions: true
    show-common-extensions: true
  show-actuator: true
  group-configs:
    - group: 'public-api'
      display-name: '公開 API'
      paths-to-match: '/api/**'
      paths-to-exclude: '/api/internal/**'
    - group: 'internal-api'
      display-name: '內部 API'
      paths-to-match: '/api/internal/**'
    - group: 'management'
      display-name: '管理端點'
      paths-to-match: '/actuator/**'
```

### 自訂 Swagger UI

可以通過配置檔案自訂 Swagger UI 的外觀和行為：

```yaml
springdoc:
  swagger-ui:
    # UI 自訂選項
    deep-linking: true
    display-operation-id: false
    default-models-expand-depth: 1
    default-model-expand-depth: 1
    default-model-rendering: example
    display-request-duration: true
    doc-expansion: none
    filter: false
    max-displayed-tags: 50
    show-extensions: false
    show-common-extensions: false
    use-root-path: false
    disable-swagger-default-url: true
```

## 文檔生成和管理

### Gradle 任務

#### 1. generateApiDocs

生成 OpenAPI 規範檔案（JSON 和 YAML 格式）

```bash
./gradlew generateApiDocs
```

**功能說明:**

- 執行 `OpenApiDocumentationTest` 測試類別
- 自動創建 `docs/api` 目錄
- 生成 `openapi.json` 和 `openapi.yaml` 檔案
- 提供詳細的執行狀態和結果訊息

#### 2. validateApiDocs

驗證生成的 OpenAPI 文檔格式

```bash
./gradlew validateApiDocs
```

#### 3. buildWithDocs

建構專案並生成文檔

```bash
./gradlew buildWithDocs
```

### CI/CD 整合

在 CI/CD 流程中整合文檔生成：

```yaml
# GitHub Actions 範例
name: Build and Generate Docs
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Gradle
        run: ./gradlew build
      
      - name: Generate API Documentation
        run: ./gradlew generateApiDocs
      
      - name: Validate API Documentation
        run: ./gradlew validateApiDocs
      
      - name: Upload API Documentation
        uses: actions/upload-artifact@v2
        with:
          name: api-docs
          path: docs/api/
```

## 故障排除

### 常見問題

#### 1. Swagger UI 無法載入

**症狀:** 訪問 `/swagger-ui.html` 時出現 404 錯誤

**解決方案:**

- 確認應用程式已正確啟動
- 檢查 `application.yml` 中的 SpringDoc 配置
- 確認沒有安全配置阻擋 Swagger UI 路徑

#### 2. API 文檔不完整

**症狀:** 某些端點或參數在文檔中缺失

**解決方案:**

- 檢查控制器是否有 `@Tag` 註解
- 確認所有端點方法都有 `@Operation` 註解
- 驗證 DTO 類別是否有適當的 `@Schema` 註解

#### 3. 文檔生成失敗

**症狀:** `generateApiDocs` 任務執行失敗

**解決方案:**

```bash
# 檢查詳細錯誤訊息
./gradlew generateApiDocs --info --stacktrace

# 確認測試類別存在
ls -la src/test/java/solid/humank/genaidemo/infrastructure/OpenApiDocumentationTest.java

# 清理並重新建構
./gradlew clean build
```

### 除錯技巧

#### 啟用詳細日誌

```yaml
# application-dev.yml
logging:
  level:
    org.springdoc: DEBUG
    org.springframework.web: DEBUG
```

#### 檢查生成的 OpenAPI 規範

```bash
# 檢查 JSON 格式
curl http://localhost:8080/v3/api-docs | jq .

# 檢查 YAML 格式
curl http://localhost:8080/v3/api-docs.yaml
```

## 相關資源

### 官方文檔

- [OpenAPI 規範 3.0](https://swagger.io/specification/)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [Spring Boot 文檔](https://spring.io/projects/spring-boot)

### 工具和擴展

- [Swagger Editor](https://editor.swagger.io/) - 線上 OpenAPI 編輯器
- [Swagger Codegen](https://swagger.io/tools/swagger-codegen/) - 程式碼生成工具
- [Postman](https://www.postman.com/) - API 測試工具
- [Insomnia](https://insomnia.rest/) - REST 客戶端

### 專案相關文檔

- [專案架構概覽](../architecture-overview.md)
- [DDD 設計指南](../DesignGuideline.MD)
- [開發環境設定](../README.md)
- [部署指南](../aws-eks-architecture.md)

### 學習資源

- [RESTful API 設計最佳實踐](https://restfulapi.net/)
- [OpenAPI 3.0 教學](https://swagger.io/docs/specification/about/)
- [Spring Boot REST API 教學](https://spring.io/guides/tutorials/rest/)
- [DDD 實踐指南](https://domainlanguage.com/ddd/)

## 支援和貢獻

### 獲得幫助

如有問題或需要支援，請：

1. **查看本文檔** - 首先檢查相關章節和故障排除指南
2. **檢查 Issue 追蹤** - 查看是否有類似問題已被報告
3. **聯繫開發團隊** - 透過內部溝通管道聯繫
4. **提交 Issue** - 在專案儲存庫中創建新的 Issue

### 貢獻指南

歡迎對 API 文檔進行改進：

1. **文檔更新** - 發現錯誤或需要補充的內容
2. **範例改進** - 提供更好的使用範例
3. **最佳實踐** - 分享開發經驗和技巧
4. **工具整合** - 提供新的工具或腳本

### 版本歷史

- **v1.0.0** (2024-01-15) - 初始版本，包含核心 API 模組
- **v1.1.0** (計劃中) - 增加進階搜尋和篩選功能
- **v2.0.0** (計劃中) - GraphQL 支援和效能最佳化

---

**最後更新:** 2024-01-15  
**文檔版本:** 1.0.0  
**API 版本:** 1.0.0
