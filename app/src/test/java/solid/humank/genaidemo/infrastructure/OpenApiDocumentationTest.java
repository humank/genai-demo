package solid.humank.genaidemo.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OpenAPI 文檔生成測試類別
 *
 * <p>此測試類別負責： 1. 生成 JSON 格式的 OpenAPI 規範檔案 2. 生成 YAML 格式的 OpenAPI 規範檔案 3. 確保生成的檔案儲存在 docs/api 目錄 4.
 * 提供檔案格式化和美化功能
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("unit")
public class OpenApiDocumentationTest {

    @Autowired private MockMvc mockMvc;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper =
            new ObjectMapper(
                    new YAMLFactory()
                            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));

    private static final String DOCS_API_DIR = "docs/api";
    private static final String OPENAPI_JSON_FILE = "openapi.json";
    private static final String OPENAPI_YAML_FILE = "openapi.yaml";

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

    /**
     * 生成 JSON 格式的 OpenAPI 規範檔案
     *
     * <p>此測試方法會： 1. 從 /v3/api-docs 端點獲取 OpenAPI 規範 2. 格式化 JSON 內容 3. 將格式化後的內容儲存到
     * docs/api/openapi.json 4. 驗證檔案內容的完整性
     */
    @Test
    void generateOpenApiJson() throws Exception {
        System.out.println("正在生成 OpenAPI JSON 規範檔案...");

        // 從 SpringDoc 端點獲取 OpenAPI 規範
        MvcResult result =
                mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        String openApiJson = result.getResponse().getContentAsString();
        assertNotNull(openApiJson, "OpenAPI JSON 內容不應為空");
        assertFalse(openApiJson.trim().isEmpty(), "OpenAPI JSON 內容不應為空字串");

        // 格式化 JSON 內容
        String formattedJson = formatJson(openApiJson);

        // 儲存到檔案
        Path jsonFilePath = saveToFile(OPENAPI_JSON_FILE, formattedJson);

        // 驗證檔案內容
        validateJsonFile(jsonFilePath);

        System.out.println("✅ OpenAPI JSON 規範檔案生成成功: " + jsonFilePath.toAbsolutePath());
    }

    /**
     * 生成 YAML 格式的 OpenAPI 規範檔案
     *
     * <p>此測試方法會： 1. 從 /v3/api-docs.yaml 端點獲取 YAML 格式的 OpenAPI 規範 2. 格式化 YAML 內容 3. 將格式化後的內容儲存到
     * docs/api/openapi.yaml 4. 驗證檔案內容的完整性
     */
    @Test
    void generateOpenApiYaml() throws Exception {
        System.out.println("正在生成 OpenAPI YAML 規範檔案...");

        // 從 JSON 端點獲取內容並轉換為 YAML
        MvcResult jsonResult =
                mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        String jsonContent = jsonResult.getResponse().getContentAsString();
        String openApiYaml = convertJsonToYaml(jsonContent);

        assertNotNull(openApiYaml, "OpenAPI YAML 內容不應為空");
        assertFalse(openApiYaml.trim().isEmpty(), "OpenAPI YAML 內容不應為空字串");

        // 儲存到檔案
        Path yamlFilePath = saveToFile(OPENAPI_YAML_FILE, openApiYaml);

        // 驗證檔案內容
        validateYamlFile(yamlFilePath);

        System.out.println("✅ OpenAPI YAML 規範檔案生成成功: " + yamlFilePath.toAbsolutePath());
    }

    /** 測試生成的檔案是否存在於正確的目錄 */
    @Test
    void verifyFilesLocation() throws Exception {
        // 先生成檔案
        generateOpenApiJson();
        generateOpenApiYaml();

        // 驗證檔案位置
        Path jsonPath = Paths.get(DOCS_API_DIR, OPENAPI_JSON_FILE);
        Path yamlPath = Paths.get(DOCS_API_DIR, OPENAPI_YAML_FILE);

        assertTrue(Files.exists(jsonPath), "JSON 檔案應該存在於 " + jsonPath.toAbsolutePath());
        assertTrue(Files.exists(yamlPath), "YAML 檔案應該存在於 " + yamlPath.toAbsolutePath());

        // 驗證檔案大小
        assertTrue(Files.size(jsonPath) > 0, "JSON 檔案不應為空");
        assertTrue(Files.size(yamlPath) > 0, "YAML 檔案不應為空");

        System.out.println("✅ 檔案位置驗證通過");
        System.out.println(
                "  JSON 檔案: "
                        + jsonPath.toAbsolutePath()
                        + " ("
                        + Files.size(jsonPath)
                        + " bytes)");
        System.out.println(
                "  YAML 檔案: "
                        + yamlPath.toAbsolutePath()
                        + " ("
                        + Files.size(yamlPath)
                        + " bytes)");
    }

    /** 確保目錄存在 */
    private void ensureDirectoryExists() throws IOException {
        Path docsApiPath = Paths.get(DOCS_API_DIR);
        if (!Files.exists(docsApiPath)) {
            Files.createDirectories(docsApiPath);
            System.out.println("已創建目錄: " + docsApiPath.toAbsolutePath());
        }
    }

    /** 格式化 JSON 內容 */
    private String formatJson(String jsonContent) throws IOException {
        JsonNode jsonNode = jsonMapper.readTree(jsonContent);
        return jsonMapper.writeValueAsString(jsonNode);
    }

    /** 格式化 YAML 內容 */
    private String formatYaml(String yamlContent) throws IOException {
        try {
            // 如果輸入已經是 YAML，直接返回格式化版本
            if (yamlContent.trim().startsWith("openapi:") || yamlContent.trim().startsWith("---")) {
                JsonNode jsonNode = yamlMapper.readTree(yamlContent);
                return yamlMapper.writeValueAsString(jsonNode);
            }

            // 如果是 JSON，先轉換為 JsonNode 再轉為 YAML
            JsonNode jsonNode = jsonMapper.readTree(yamlContent);
            return yamlMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            // 如果解析失敗，嘗試清理特殊字符後再處理
            String cleanedContent = yamlContent.replaceAll("[\\x00-\\x1F\\x7F-\\x9F]", "");
            JsonNode jsonNode = jsonMapper.readTree(cleanedContent);
            return yamlMapper.writeValueAsString(jsonNode);
        }
    }

    /** 將 JSON 轉換為 YAML */
    private String convertJsonToYaml(String jsonContent) throws IOException {
        JsonNode jsonNode = jsonMapper.readTree(jsonContent);
        return yamlMapper.writeValueAsString(jsonNode);
    }

    /** 儲存內容到檔案 */
    private Path saveToFile(String fileName, String content) throws IOException {
        Path filePath = Paths.get(DOCS_API_DIR, fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }

        return filePath;
    }

    /** 驗證 JSON 檔案內容 */
    private void validateJsonFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        JsonNode jsonNode = jsonMapper.readTree(content);

        // 驗證基本的 OpenAPI 結構
        assertTrue(jsonNode.has("openapi"), "JSON 檔案應包含 openapi 版本資訊");
        assertTrue(jsonNode.has("info"), "JSON 檔案應包含 info 區塊");
        assertTrue(jsonNode.has("paths"), "JSON 檔案應包含 paths 區塊");

        // 驗證 info 區塊
        JsonNode info = jsonNode.get("info");
        assertTrue(info.has("title"), "info 區塊應包含 title");
        assertTrue(info.has("version"), "info 區塊應包含 version");

        System.out.println("  ✓ JSON 檔案結構驗證通過");
        System.out.println("  ✓ OpenAPI 版本: " + jsonNode.get("openapi").asText());
        System.out.println("  ✓ API 標題: " + info.get("title").asText());
        System.out.println("  ✓ API 版本: " + info.get("version").asText());
        System.out.println("  ✓ 端點數量: " + jsonNode.get("paths").size());
    }

    /** 驗證 YAML 檔案內容 */
    private void validateYamlFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        JsonNode yamlNode = yamlMapper.readTree(content);

        // 驗證基本的 OpenAPI 結構
        assertTrue(yamlNode.has("openapi"), "YAML 檔案應包含 openapi 版本資訊");
        assertTrue(yamlNode.has("info"), "YAML 檔案應包含 info 區塊");
        assertTrue(yamlNode.has("paths"), "YAML 檔案應包含 paths 區塊");

        // 驗證 info 區塊
        JsonNode info = yamlNode.get("info");
        assertTrue(info.has("title"), "info 區塊應包含 title");
        assertTrue(info.has("version"), "info 區塊應包含 version");

        System.out.println("  ✓ YAML 檔案結構驗證通過");
        System.out.println("  ✓ OpenAPI 版本: " + yamlNode.get("openapi").asText());
        System.out.println("  ✓ API 標題: " + info.get("title").asText());
        System.out.println("  ✓ API 版本: " + info.get("version").asText());
        System.out.println("  ✓ 端點數量: " + yamlNode.get("paths").size());
    }
}
