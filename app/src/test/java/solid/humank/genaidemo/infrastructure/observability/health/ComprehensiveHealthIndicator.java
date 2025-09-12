package solid.humank.genaidemo.infrastructure.observability.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 綜合健康指標
 * 提供應用程式各個組件的健康狀態檢查
 */
@Component
public class ComprehensiveHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ComprehensiveHealthIndicator.class);

    private final Map<String, Boolean> componentHealth = new ConcurrentHashMap<>();

    public ComprehensiveHealthIndicator() {
        initializeComponents();
    }

    private void initializeComponents() {
        componentHealth.put("database", true);
        componentHealth.put("messaging", true);
        componentHealth.put("cache", true);
        componentHealth.put("external-api", true);
        componentHealth.put("observability", true);
    }

    @Override
    public Health health() {
        try {
            boolean allHealthy = checkAllComponents();

            Health.Builder builder = allHealthy ? Health.up() : Health.down();

            componentHealth.forEach(builder::withDetail);

            builder.withDetail("timestamp", System.currentTimeMillis());
            builder.withDetail("version", "1.0.0");

            return builder.build();
        } catch (Exception e) {
            log.error("Error checking health", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkAllComponents() {
        return componentHealth.values().stream().allMatch(Boolean::booleanValue);
    }

    public void updateComponentHealth(String component, boolean healthy) {
        componentHealth.put(component, healthy);
        log.info("Updated health for component {}: {}", component, healthy);
    }
}