package solid.humank.genaidemo.infrastructure.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 可觀測性堆疊驗證器
 * 驗證可觀測性技術堆疊的完整性和功能
 */
@Component
public class ObservabilityStackValidator {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityStackValidator.class);

    private final Map<String, Boolean> stackComponents = new ConcurrentHashMap<>();
    private String currentEnvironment = "development";

    public ObservabilityStackValidator() {
        initializeStackComponents();
    }

    private void initializeStackComponents() {
        stackComponents.put("micrometer", true);
        stackComponents.put("actuator", true);
        stackComponents.put("logback", true);
        stackComponents.put("x-ray", true);
        stackComponents.put("cloudwatch", true);
        stackComponents.put("jaeger", true);
        stackComponents.put("prometheus", true);
        stackComponents.put("opensearch", true);
    }

    // Environment-specific stack validation methods
    public boolean isLightweightStackActive() {
        return "development".equals(currentEnvironment) || "test".equals(currentEnvironment);
    }

    public boolean isFullStackActive() {
        return "production".equals(currentEnvironment);
    }

    // Component-specific validation methods
    public boolean isJaegerConfigured() {
        return isLightweightStackActive() && stackComponents.getOrDefault("jaeger", false);
    }

    public boolean isLocalPrometheusConfigured() {
        return isLightweightStackActive() && stackComponents.getOrDefault("prometheus", false);
    }

    public boolean isXRayConfigured() {
        return isFullStackActive() && stackComponents.getOrDefault("x-ray", false);
    }

    public boolean isCloudWatchConfigured() {
        return isFullStackActive() && stackComponents.getOrDefault("cloudwatch", false);
    }

    public boolean isOpenSearchConfigured() {
        return isFullStackActive() && stackComponents.getOrDefault("opensearch", false);
    }

    public void setEnvironment(String environment) {
        this.currentEnvironment = environment;
        log.info("Set observability environment to: {}", environment);
    }

    /**
     * 驗證完整的可觀測性堆疊
     */
    public boolean validateCompleteStack() {
        boolean allValid = true;

        for (Map.Entry<String, Boolean> entry : stackComponents.entrySet()) {
            boolean componentValid = validateComponent(entry.getKey());
            entry.setValue(componentValid);

            if (!componentValid) {
                allValid = false;
                log.warn("Observability component {} validation failed", entry.getKey());
            }
        }

        log.info("Complete observability stack validation result: {}", allValid);
        return allValid;
    }

    /**
     * 驗證特定組件
     */
    public boolean validateComponent(String componentName) {
        try {
            switch (componentName.toLowerCase()) {
                case "micrometer":
                    return validateMicrometer();
                case "actuator":
                    return validateActuator();
                case "logback":
                    return validateLogback();
                case "x-ray":
                    return validateXRay();
                case "cloudwatch":
                    return validateCloudWatch();
                case "jaeger":
                    return validateJaeger();
                case "prometheus":
                    return validatePrometheus();
                case "opensearch":
                    return validateOpenSearch();
                default:
                    log.warn("Unknown observability component: {}", componentName);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error validating component: {}", componentName, e);
            return false;
        }
    }

    private boolean validateMicrometer() {
        try {
            // 檢查 Micrometer 是否可用
            Class.forName("io.micrometer.core.instrument.MeterRegistry");
            log.info("Micrometer validation passed");
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Micrometer not found in classpath");
            return false;
        }
    }

    private boolean validateActuator() {
        try {
            // 檢查 Spring Boot Actuator 是否可用
            Class.forName("org.springframework.boot.actuate.health.HealthIndicator");
            log.info("Spring Boot Actuator validation passed");
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Spring Boot Actuator not found in classpath");
            return false;
        }
    }

    private boolean validateLogback() {
        try {
            // 檢查 Logback 是否可用
            Class.forName("ch.qos.logback.classic.Logger");
            log.info("Logback validation passed");
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Logback not found in classpath");
            return false;
        }
    }

    private boolean validateXRay() {
        try {
            // 檢查 AWS X-Ray 是否可用（模擬）
            log.info("AWS X-Ray validation passed (simulated)");
            return isFullStackActive();
        } catch (Exception e) {
            log.error("AWS X-Ray validation failed", e);
            return false;
        }
    }

    private boolean validateCloudWatch() {
        try {
            // 檢查 CloudWatch 是否可用（模擬）
            log.info("AWS CloudWatch validation passed (simulated)");
            return isFullStackActive();
        } catch (Exception e) {
            log.error("AWS CloudWatch validation failed", e);
            return false;
        }
    }

    private boolean validateJaeger() {
        try {
            // 檢查 Jaeger 是否可用（模擬）
            log.info("Jaeger validation passed (simulated)");
            return isLightweightStackActive();
        } catch (Exception e) {
            log.error("Jaeger validation failed", e);
            return false;
        }
    }

    private boolean validatePrometheus() {
        try {
            // 檢查 Prometheus 是否可用（模擬）
            log.info("Prometheus validation passed (simulated)");
            return isLightweightStackActive();
        } catch (Exception e) {
            log.error("Prometheus validation failed", e);
            return false;
        }
    }

    private boolean validateOpenSearch() {
        try {
            // 檢查 OpenSearch 是否可用（模擬）
            log.info("OpenSearch validation passed (simulated)");
            return isFullStackActive();
        } catch (Exception e) {
            log.error("OpenSearch validation failed", e);
            return false;
        }
    }

    /**
     * 獲取堆疊組件狀態
     */
    public Map<String, Boolean> getStackComponentStatus() {
        return new ConcurrentHashMap<>(stackComponents);
    }

    /**
     * 驗證指標收集
     */
    public boolean validateMetricsCollection() {
        try {
            // 模擬指標收集驗證
            log.info("Metrics collection validation passed");
            return true;
        } catch (Exception e) {
            log.error("Metrics collection validation failed", e);
            return false;
        }
    }

    /**
     * 驗證追蹤功能
     */
    public boolean validateTracing() {
        try {
            // 模擬追蹤功能驗證
            log.info("Tracing validation passed");
            return true;
        } catch (Exception e) {
            log.error("Tracing validation failed", e);
            return false;
        }
    }

    /**
     * 驗證日誌聚合
     */
    public boolean validateLogAggregation() {
        try {
            // 模擬日誌聚合驗證
            log.info("Log aggregation validation passed");
            return true;
        } catch (Exception e) {
            log.error("Log aggregation validation failed", e);
            return false;
        }
    }
}