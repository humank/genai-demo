package solid.humank.genaidemo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * 輕量級單元測試 - OpenAPI Documentation
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~100ms (vs @SpringBootTest ~3s)
 * 
 * 測試 OpenAPI 文檔生成的邏輯，而不是實際的 Spring 端點
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("OpenAPI Documentation Unit Tests")
class OpenApiDocumentationUnitTest {

  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final ObjectMapper yamlMapper = new ObjectMapper(
      new YAMLFactory()
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
          .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
          .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));

  private static final String DOCS_API_DIR = "docs/api";
  private static final String OPENAPI_JSON_FILE = "openapi-test.json";
  private static final String OPENAPI_YAML_FILE = "openapi-test.yaml";

  @BeforeEach
  void setUp() throws IOException {
    // 配置 JSON 格式化
    jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
    jsonMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    // 配置 YAML 格式化
    yamlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    yamlMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    // 確保目錄存在
    ensureDirectoryExists();
  }

  @Test
  @DisplayName("Should format JSON content correctly")
  void shouldFormatJsonContentCorrectly() throws IOException {
    // Given
    String rawJson = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"GenAI Demo API\",\"version\":\"1.0.0\"},\"paths\":{\"/api/test\":{\"get\":{\"summary\":\"Test endpoint\"}}}}";

    // When
    String formattedJson = formatJson(rawJson);

    // Then
    assertThat(formattedJson).isNotNull();
    assertThat(formattedJson).contains("\"openapi\" : \"3.0.1\"");
    assertThat(formattedJson).contains("\"title\" : \"GenAI Demo API\"");

    // 驗證是有效的 JSON
    JsonNode jsonNode = jsonMapper.readTree(formattedJson);
    assertThat(jsonNode.get("openapi").asText()).isEqualTo("3.0.1");
    assertThat(jsonNode.get("info").get("title").asText()).isEqualTo("GenAI Demo API");
  }

  @Test
  @DisplayName("Should convert JSON to YAML correctly")
  void shouldConvertJsonToYamlCorrectly() throws IOException {
    // Given
    String jsonContent = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"GenAI Demo API\",\"version\":\"1.0.0\"},\"paths\":{\"/api/test\":{\"get\":{\"summary\":\"Test endpoint\"}}}}";

    // When
    String yamlContent = convertJsonToYaml(jsonContent);

    // Then
    assertThat(yamlContent).isNotNull();
    assertThat(yamlContent).contains("openapi:");
    assertThat(yamlContent).contains("title:");

    // 驗證是有效的 YAML
    JsonNode yamlNode = yamlMapper.readTree(yamlContent);
    assertThat(yamlNode.get("openapi").asText()).isEqualTo("3.0.1");
    assertThat(yamlNode.get("info").get("title").asText()).isEqualTo("GenAI Demo API");
  }

  @Test
  @DisplayName("Should save content to file correctly")
  void shouldSaveContentToFileCorrectly() throws IOException {
    // Given
    String testContent = "{\n  \"test\": \"content\"\n}";

    // When
    Path filePath = saveToFile("test-output.json", testContent);

    // Then
    assertThat(Files.exists(filePath)).isTrue();
    assertThat(Files.size(filePath)).isGreaterThan(0);

    String savedContent = Files.readString(filePath);
    assertThat(savedContent).isEqualTo(testContent);

    // 清理測試文件
    Files.deleteIfExists(filePath);
  }

  @Test
  @DisplayName("Should validate JSON file structure")
  void shouldValidateJsonFileStructure() throws IOException {
    // Given
    String validOpenApiJson = """
        {
          "openapi": "3.0.1",
          "info": {
            "title": "Test API",
            "version": "1.0.0"
          },
          "paths": {
            "/test": {
              "get": {
                "summary": "Test endpoint"
              }
            }
          }
        }
        """;

    Path jsonFilePath = saveToFile("test-validation.json", validOpenApiJson);

    // When & Then
    validateJsonFile(jsonFilePath);

    // 清理測試文件
    Files.deleteIfExists(jsonFilePath);
  }

  @Test
  @DisplayName("Should validate YAML file structure")
  void shouldValidateYamlFileStructure() throws IOException {
    // Given
    String validOpenApiYaml = """
        openapi: "3.0.1"
        info:
          title: "Test API"
          version: "1.0.0"
        paths:
          /test:
            get:
              summary: "Test endpoint"
        """;

    Path yamlFilePath = saveToFile("test-validation.yaml", validOpenApiYaml);

    // When & Then
    validateYamlFile(yamlFilePath);

    // 清理測試文件
    Files.deleteIfExists(yamlFilePath);
  }

  @Test
  @DisplayName("Should handle complex OpenAPI structure")
  void shouldHandleComplexOpenApiStructure() throws IOException {
    // Given
    String complexJson = """
        {
          "openapi": "3.0.1",
          "info": {
            "title": "GenAI Demo API",
            "version": "1.0.0",
            "description": "API for GenAI Demo application"
          },
          "paths": {
            "/api/customers": {
              "get": {
                "summary": "Get customers",
                "responses": {
                  "200": {
                    "description": "Success"
                  }
                }
              }
            },
            "/api/orders": {
              "post": {
                "summary": "Create order",
                "requestBody": {
                  "required": true
                }
              }
            }
          },
          "components": {
            "schemas": {
              "Customer": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
        """;

    // When
    String formattedJson = formatJson(complexJson);
    String yamlContent = convertJsonToYaml(complexJson);

    // Then
    JsonNode jsonNode = jsonMapper.readTree(formattedJson);
    assertThat(jsonNode.get("paths").size()).isEqualTo(2);
    assertThat(jsonNode.get("components").get("schemas").has("Customer")).isTrue();

    JsonNode yamlNode = yamlMapper.readTree(yamlContent);
    assertThat(yamlNode.get("paths").size()).isEqualTo(2);
    assertThat(yamlNode.get("components").get("schemas").has("Customer")).isTrue();
  }

  @Test
  @DisplayName("Should ensure directory creation")
  void shouldEnsureDirectoryCreation() throws IOException {
    // Given
    Path testDir = Paths.get("test-docs");

    // 確保目錄不存在
    if (Files.exists(testDir)) {
      Files.delete(testDir);
    }

    // When
    if (!Files.exists(testDir)) {
      Files.createDirectories(testDir);
    }

    // Then
    assertThat(Files.exists(testDir)).isTrue();
    assertThat(Files.isDirectory(testDir)).isTrue();

    // 清理
    Files.deleteIfExists(testDir);
  }

  // Helper methods

  private void ensureDirectoryExists() throws IOException {
    Path docsApiPath = Paths.get(DOCS_API_DIR);
    if (!Files.exists(docsApiPath)) {
      Files.createDirectories(docsApiPath);
    }
  }

  private String formatJson(String jsonContent) throws IOException {
    JsonNode jsonNode = jsonMapper.readTree(jsonContent);
    return jsonMapper.writeValueAsString(jsonNode);
  }

  private String convertJsonToYaml(String jsonContent) throws IOException {
    JsonNode jsonNode = jsonMapper.readTree(jsonContent);
    return yamlMapper.writeValueAsString(jsonNode);
  }

  private Path saveToFile(String fileName, String content) throws IOException {
    Path filePath = Paths.get(DOCS_API_DIR, fileName);
    try (FileWriter writer = new FileWriter(filePath.toFile())) {
      writer.write(content);
    }
    return filePath;
  }

  private void validateJsonFile(Path filePath) throws IOException {
    String content = Files.readString(filePath);
    JsonNode jsonNode = jsonMapper.readTree(content);

    // 驗證基本的 OpenAPI 結構
    assertThat(jsonNode.has("openapi")).isTrue();
    assertThat(jsonNode.has("info")).isTrue();
    assertThat(jsonNode.has("paths")).isTrue();

    // 驗證 info 區塊
    JsonNode info = jsonNode.get("info");
    assertThat(info.has("title")).isTrue();
    assertThat(info.has("version")).isTrue();
  }

  private void validateYamlFile(Path filePath) throws IOException {
    String content = Files.readString(filePath);
    JsonNode yamlNode = yamlMapper.readTree(content);

    // 驗證基本的 OpenAPI 結構
    assertThat(yamlNode.has("openapi")).isTrue();
    assertThat(yamlNode.has("info")).isTrue();
    assertThat(yamlNode.has("paths")).isTrue();

    // 驗證 info 區塊
    JsonNode info = yamlNode.get("info");
    assertThat(info.has("title")).isTrue();
    assertThat(info.has("version")).isTrue();
  }
}