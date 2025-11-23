package solid.humank.genaidemo.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Business Logic Health Indicator
 *
 * This indicator checks business-specific conditions that are NOT covered
 * by standard Spring Boot health indicators (db, redis, kafka, etc.)
 *
 * Examples of business logic health checks:
 * - Order processing queue is not stuck
 * - Critical business workflows are operational
 * - External payment gateway is reachable
 * - Business rule engine is functioning
 *
 * Note: This is OPTIONAL. Only add business-specific checks here.
 * Infrastructure health (DB, Redis, Kafka) is handled by Spring Boot
 * automatically.
 *
 * Used by Kubernetes readiness probe to determine if pod should receive
 * traffic.
 */
@Component("businessLogic")
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Business-specific health checks can be added here
        // Examples: order processing queue status, critical workflow health, external
        // service connectivity
        // Currently returns UP status as no business-specific checks are required

        return Health.up()
                .withDetail("status", "Business logic is healthy")
                .withDetail("timestamp", System.currentTimeMillis())
                .build();
    }
}
