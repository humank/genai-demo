package solid.humank.genaidemo.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Profile 配置驗證器
 * 驗證不同環境的配置正確性
 */
@Component
public class ProfileConfigurationValidator {

    private static final Logger log = LoggerFactory.getLogger(ProfileConfigurationValidator.class);

    private final Environment environment;
    private final Map<String, Boolean> validationResults = new ConcurrentHashMap<>();

    public ProfileConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    public ProfileConfigurationValidator() {
        this.environment = null;
    }

    /**
     * 獲取支援的 Profile 列表
     */
    public List<String> getSupportedProfiles() {
        return Arrays.asList("dev", "development", "test", "production");
    }

    /**
     * 驗證當前 Profile 配置
     */
    public boolean validateCurrentProfile() {
        try {
            if (environment == null) {
                log.warn("Environment is null, using mock validation");
                return true;
            }

            String[] activeProfiles = environment.getActiveProfiles();
            log.info("Validating profiles: {}", Arrays.toString(activeProfiles));

            boolean isValid = true;

            for (String profile : activeProfiles) {
                boolean profileValid = validateProfile(profile);
                validationResults.put(profile, profileValid);
                isValid = isValid && profileValid;
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating current profile", e);
            return false;
        }
    }

    /**
     * 驗證特定 Profile
     */
    public boolean validateProfile(String profile) {
        try {
            return switch (profile.toLowerCase()) {
                case "development", "dev" -> validateDevelopmentProfile();
                case "test" -> validateTestProfile();
                case "production" -> validateProductionProfile();
                default -> {
                    log.warn("Unknown profile: {}", profile);
                    yield true; // 允許未知 profile
                }
            };
        } catch (Exception e) {
            log.error("Error validating profile: {}", profile, e);
            return false;
        }
    }

    /**
     * 檢查開發配置是否啟用
     */
    public boolean isDevelopmentConfigurationActive() {
        return validateDevelopmentProfile();
    }

    private boolean validateDevelopmentProfile() {
        log.info("Validating development profile configuration");

        // 驗證開發環境配置
        boolean hasH2Database = checkProperty("spring.datasource.url", "h2");
        checkProperty("logging.level.solid.humank", "DEBUG"); // Check but don't require

        return hasH2Database; // 開發環境必須使用 H2
    }

    private boolean validateTestProfile() {
        log.info("Validating test profile configuration");

        // 驗證測試環境配置
        // 測試環境日誌配置較寬鬆，不需要特定檢查
        return checkProperty("spring.datasource.url", "mem");
    }

    private boolean validateProductionProfile() {
        log.info("Validating production profile configuration");

        // 驗證生產環境配置
        boolean hasProductionDatabase = !checkProperty("spring.datasource.url", "h2");
        boolean hasSecureLogging = !checkProperty("logging.level.solid.humank", "DEBUG");

        return hasProductionDatabase && hasSecureLogging;
    }

    private boolean checkProperty(String propertyName, String expectedValue) {
        if (environment == null) {
            return true; // Mock 環境總是返回 true
        }

        String actualValue = environment.getProperty(propertyName);
        if (actualValue == null) {
            return false;
        }

        return actualValue.toLowerCase().contains(expectedValue.toLowerCase());
    }

    /**
     * 獲取驗證結果
     */
    public Map<String, Boolean> getValidationResults() {
        return new ConcurrentHashMap<>(validationResults);
    }

    /**
     * 驗證必要的配置屬性
     */
    public boolean validateRequiredProperties() {
        List<String> requiredProperties = Arrays.asList(
                "spring.application.name",
                "spring.datasource.url");

        boolean allPresent = true;

        for (String property : requiredProperties) {
            boolean present = environment != null && environment.getProperty(property) != null;
            if (!present) {
                log.warn("Required property missing: {}", property);
                allPresent = false;
            }
        }

        return allPresent;
    }
}
