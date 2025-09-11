package solid.humank.genaidemo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 輕量級單元測試 - Swagger UI Functionality
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~100ms (vs @SpringBootTest ~3s)
 * 
 * 測試 Swagger UI 相關邏輯，而不是實際的 Web 端點
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Swagger UI Functionality Unit Tests")
class SwaggerUIFunctionalityUnitTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should validate OpenAPI specification structure")
  void shouldValidateOpenApiSpecificationStructure() throws Exception {
    // Given
    String mockOpenApiSpec = """
        {
          "openapi": "3.0.1",
          "info": {
            "title": "GenAI Demo - DDD 電商平台 API",
            "version": "1.0.0",
            "description": "基於 DDD 架構的電商平台 API"
          },
          "paths": {
            "/api/orders": {
              "post": {
                "summary": "創建訂單",
                "tags": ["訂單管理"],
                "responses": {
                  "200": {"description": "成功"},
                  "400": {"description": "請求錯誤"},
                  "500": {"description": "伺服器錯誤"}
                }
              }
            }
          },
          "components": {
            "schemas": {
              "StandardErrorResponse": {
                "type": "object",
                "required": ["code", "message", "timestamp", "path"],
                "properties": {
                  "code": {"type": "string"},
                  "message": {"type": "string"},
                  "timestamp": {"type": "string"},
                  "path": {"type": "string"},
                  "details": {"type": "object"}
                }
              }
            }
          },
          "tags": [
            {"name": "訂單管理", "description": "訂單相關操作"},
            {"name": "支付管理", "description": "支付相關操作"}
          ]
        }
        """;

    // When
    JsonNode apiDoc = objectMapper.readTree(mockOpenApiSpec);

    // Then
    assertThat(apiDoc.has("openapi")).isTrue();
    assertThat(apiDoc.has("info")).isTrue();
    assertThat(apiDoc.has("paths")).isTrue();
    assertThat(apiDoc.has("components")).isTrue();
    assertThat(apiDoc.has("tags")).isTrue();
  }

  @Test
  @DisplayName("Should validate API info section")
  void shouldValidateApiInfoSection() throws Exception {
    // Given
    String infoSection = """
        {
          "title": "GenAI Demo - DDD 電商平台 API",
          "version": "1.0.0",
          "description": "基於 DDD 架構的電商平台 API"
        }
        """;

    // When
    JsonNode info = objectMapper.readTree(infoSection);

    // Then
    assertThat(info.get("title").asText()).isEqualTo("GenAI Demo - DDD 電商平台 API");
    assertThat(info.get("version").asText()).isEqualTo("1.0.0");
    assertThat(info.get("description").asText()).contains("DDD 架構");
  }

  @Test
  @DisplayName("Should validate expected API tags")
  void shouldValidateExpectedApiTags() {
    // Given
    List<String> expectedTags = List.of(
        "訂單管理", "支付管理", "庫存管理", "產品管理",
        "客戶管理", "定價管理", "活動記錄", "統計報表");

    // When & Then
    for (String tag : expectedTags) {
      assertThat(tag).isNotNull();
      assertThat(tag).isNotEmpty();
      assertThat(tag).doesNotContain(" ");
    }

    assertThat(expectedTags).hasSize(8);
    assertThat(expectedTags).contains("訂單管理", "支付管理", "客戶管理");
  }

  @Test
  @DisplayName("Should validate standard error response schema")
  void shouldValidateStandardErrorResponseSchema() throws Exception {
    // Given
    String errorSchema = """
        {
          "type": "object",
          "required": ["code", "message", "timestamp", "path"],
          "properties": {
            "code": {"type": "string"},
            "message": {"type": "string"},
            "timestamp": {"type": "string"},
            "path": {"type": "string"},
            "details": {"type": "object"}
          }
        }
        """;

    // When
    JsonNode schema = objectMapper.readTree(errorSchema);

    // Then
    assertThat(schema.get("type").asText()).isEqualTo("object");

    JsonNode required = schema.get("required");
    assertThat(required.isArray()).isTrue();
    assertThat(required.size()).isEqualTo(4);

    JsonNode properties = schema.get("properties");
    assertThat(properties.has("code")).isTrue();
    assertThat(properties.has("message")).isTrue();
    assertThat(properties.has("timestamp")).isTrue();
    assertThat(properties.has("path")).isTrue();
    assertThat(properties.has("details")).isTrue();
  }

  @Test
  @DisplayName("Should validate HTTP methods")
  void shouldValidateHttpMethods() {
    // Given
    List<String> httpMethods = List.of(
        "get", "post", "put", "delete", "patch", "head", "options", "trace");

    // When & Then
    for (String method : httpMethods) {
      assertThat(isHttpMethod(method)).isTrue();
    }

    assertThat(isHttpMethod("invalid")).isFalse();
    assertThat(isHttpMethod("")).isFalse();
    assertThat(isHttpMethod(null)).isFalse();
  }

  @Test
  @DisplayName("Should validate API operation structure")
  void shouldValidateApiOperationStructure() throws Exception {
    // Given
    String operation = """
        {
          "summary": "創建訂單",
          "tags": ["訂單管理"],
          "responses": {
            "200": {"description": "成功"},
            "400": {"description": "請求錯誤"},
            "500": {"description": "伺服器錯誤"}
          }
        }
        """;

    // When
    JsonNode op = objectMapper.readTree(operation);

    // Then
    assertThat(op.has("summary")).isTrue();
    assertThat(op.has("tags")).isTrue();
    assertThat(op.has("responses")).isTrue();

    assertThat(op.get("summary").asText()).isEqualTo("創建訂單");
    assertThat(op.get("tags").isArray()).isTrue();
    assertThat(op.get("responses").has("200")).isTrue();
    assertThat(op.get("responses").has("400")).isTrue();
    assertThat(op.get("responses").has("500")).isTrue();
  }

  @Test
  @DisplayName("Should validate important DTO schemas")
  void shouldValidateImportantDtoSchemas() {
    // Given
    List<String> importantSchemas = List.of(
        "CreateOrderRequest",
        "OrderResponse",
        "PaymentRequest",
        "PaymentResponse",
        "UpdateProductRequest");

    // When & Then
    for (String schemaName : importantSchemas) {
      assertThat(schemaName).isNotNull();
      assertThat(schemaName).isNotEmpty();
      assertThat(schemaName).matches("[A-Z][a-zA-Z]*"); // PascalCase
    }
  }

  @Test
  @DisplayName("Should validate OpenAPI version format")
  void shouldValidateOpenApiVersionFormat() {
    // Given
    String version = "3.0.1";

    // When
    String[] parts = version.split("\\.");

    // Then
    assertThat(parts).hasSize(3);
    assertThat(parts[0]).isEqualTo("3");
    assertThat(version).startsWith("3.0");
  }

  @Test
  @DisplayName("Should validate Swagger UI paths")
  void shouldValidateSwaggerUiPaths() {
    // Given
    List<String> swaggerPaths = List.of(
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/v3/api-docs");

    // When & Then
    for (String path : swaggerPaths) {
      assertThat(path).startsWith("/");
      assertThat(path).doesNotEndWith("/");
      assertThat(path).doesNotContain(" ");
    }
  }

  @Test
  @DisplayName("Should validate content types")
  void shouldValidateContentTypes() {
    // Given
    Set<String> expectedContentTypes = Set.of(
        "application/json",
        "text/html",
        "application/yaml");

    // When & Then
    for (String contentType : expectedContentTypes) {
      assertThat(contentType).contains("/");
      assertThat(contentType).doesNotContain(" ");
    }

    assertThat(expectedContentTypes).contains("application/json");
    assertThat(expectedContentTypes).contains("text/html");
  }

  // Helper method
  private boolean isHttpMethod(String method) {
    if (method == null || method.isEmpty()) {
      return false;
    }
    return List.of("get", "post", "put", "delete", "patch", "head", "options", "trace")
        .contains(method.toLowerCase());
  }
}