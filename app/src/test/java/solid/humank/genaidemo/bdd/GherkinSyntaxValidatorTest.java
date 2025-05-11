package solid.humank.genaidemo.bdd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Gherkin 語法驗證器
 * 用於檢查 feature 文件的語法是否正確
 */
public class GherkinSyntaxValidatorTest {

    @Test
    @DisplayName("驗證所有 Feature 文件的 Gherkin 語法")
    public void validateAllFeatureFilesGherkinSyntax() throws IOException {
        Path featuresDir = Paths.get("src/test/resources/features");
        assertTrue(Files.exists(featuresDir), "Features directory does not exist: " + featuresDir);
        
        try (Stream<Path> paths = Files.walk(featuresDir)) {
            List<Path> featureFiles = paths
                .filter(path -> path.toString().endsWith(".feature"))
                .collect(Collectors.toList());
            
            System.out.println("Found " + featureFiles.size() + " feature files to validate:");
            
            for (Path file : featureFiles) {
                validateGherkinSyntax(file);
            }
        }
    }
    
    private void validateGherkinSyntax(Path featureFile) throws IOException {
        System.out.println("Validating: " + featureFile);
        String content = Files.readString(featureFile);
        
        // 檢查基本 Gherkin 關鍵字
        assertTrue(content.contains("Feature:"), "Feature file does not contain 'Feature:' keyword: " + featureFile);
        
        // 檢查至少有一個場景
        boolean hasScenario = Pattern.compile("\\s*Scenario:").matcher(content).find() || 
                             Pattern.compile("\\s*Scenario Outline:").matcher(content).find();
        assertTrue(hasScenario, "Feature file does not contain any scenarios: " + featureFile);
        
        // 檢查 Given/When/Then 步驟
        boolean hasSteps = Pattern.compile("\\s*Given ").matcher(content).find() || 
                          Pattern.compile("\\s*When ").matcher(content).find() || 
                          Pattern.compile("\\s*Then ").matcher(content).find();
        assertTrue(hasSteps, "Feature file does not contain any Given/When/Then steps: " + featureFile);
        
        // 檢查語法錯誤 (簡單檢查)
        checkForCommonSyntaxErrors(content, featureFile);
        
        System.out.println("✓ " + featureFile.getFileName() + " syntax is valid");
    }
    
    private void checkForCommonSyntaxErrors(String content, Path file) {
        // 檢查未閉合的引號
        long doubleQuotes = content.chars().filter(ch -> ch == '"').count();
        if (doubleQuotes % 2 != 0) {
            fail("Feature file has unclosed quotes: " + file);
        }
        
        // 檢查 Examples 表格格式
        if (content.contains("Examples:")) {
            assertTrue(content.contains("|"), "Examples section does not contain table format with '|': " + file);
        }
        
        // 檢查 Scenario Outline 中的參數
        if (content.contains("Scenario Outline:")) {
            assertTrue(content.contains("<") && content.contains(">"), 
                    "Scenario Outline does not contain parameters in <> format: " + file);
            assertTrue(content.contains("Examples:"), 
                    "Scenario Outline does not have Examples section: " + file);
        }
    }
}