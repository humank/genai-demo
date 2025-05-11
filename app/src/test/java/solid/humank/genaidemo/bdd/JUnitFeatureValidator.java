package solid.humank.genaidemo.bdd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用純 JUnit 5 測試框架驗證 feature 文件
 * 不依賴 Cucumber 或 Spring，避免複雜的配置問題
 */
public class JUnitFeatureValidator {

    @Test
    @DisplayName("驗證訂單管理 Feature 文件")
    public void validateOrderManagementFeatureFile() throws IOException {
        Path featureFile = Paths.get("src/test/resources/features/order/order_management.feature");
        assertTrue(Files.exists(featureFile), "Feature file does not exist: " + featureFile);
        
        String content = Files.readString(featureFile);
        System.out.println("Feature file content is valid:\n" + content);
        assertTrue(content.contains("Feature: Order Management"), "Feature file does not contain expected content");
    }
    
    @Test
    @DisplayName("列出所有 Feature 文件")
    public void listAllFeatureFiles() throws IOException {
        Path featuresDir = Paths.get("src/test/resources/features");
        assertTrue(Files.exists(featuresDir), "Features directory does not exist: " + featuresDir);
        
        try (Stream<Path> paths = Files.walk(featuresDir)) {
            List<Path> featureFiles = paths
                .filter(path -> path.toString().endsWith(".feature"))
                .collect(Collectors.toList());
            
            System.out.println("Found " + featureFiles.size() + " feature files:");
            for (Path file : featureFiles) {
                System.out.println("- " + featuresDir.relativize(file));
            }
            
            assertTrue(featureFiles.size() > 0, "No feature files found");
        }
    }
}