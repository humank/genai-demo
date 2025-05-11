package solid.humank.genaidemo.bdd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 獨立的 feature 文件驗證器，不使用任何測試框架
 * 這個類只是一個普通的 Java 類，可以直接運行
 */
public class FeatureFileValidatorNoSpringNoTest {

    public static void main(String[] args) throws IOException {
        validateOrderManagementFeatureFile();
        listAllFeatureFiles();
    }
    
    public static void validateOrderManagementFeatureFile() throws IOException {
        // 修正路徑，使用絕對路徑
        Path featureFile = Paths.get("app/src/test/resources/features/order/order_management.feature");
        if (!Files.exists(featureFile)) {
            System.err.println("Feature file does not exist: " + featureFile);
            return;
        }
        
        String content = Files.readString(featureFile);
        System.out.println("Feature file content is valid:\n" + content);
        if (!content.contains("Feature: Order Management")) {
            System.err.println("Feature file does not contain expected content");
        }
    }
    
    public static void listAllFeatureFiles() throws IOException {
        // 修正路徑，使用絕對路徑
        Path featuresDir = Paths.get("app/src/test/resources/features");
        if (!Files.exists(featuresDir)) {
            System.err.println("Features directory does not exist: " + featuresDir);
            return;
        }
        
        try (Stream<Path> paths = Files.walk(featuresDir)) {
            List<Path> featureFiles = paths
                .filter(path -> path.toString().endsWith(".feature"))
                .collect(Collectors.toList());
            
            System.out.println("Found " + featureFiles.size() + " feature files:");
            for (Path file : featureFiles) {
                System.out.println("- " + featuresDir.relativize(file));
            }
            
            if (featureFiles.isEmpty()) {
                System.err.println("No feature files found");
            }
        }
    }
}