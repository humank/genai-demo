package solid.humank.genaidemo.infrastructure.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * 輕量級單元測試 - Health Check
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~2s)
 * 
 * 測試健康檢查邏輯，而不是實際的 Web 端點
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Health Check Unit Tests")
class HealthCheckUnitTest {

    @Test
    @DisplayName("Should create UP health status")
    void shouldCreateUpHealthStatus() {
        // When
        Health health = Health.up().build();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    @DisplayName("Should create DOWN health status")
    void shouldCreateDownHealthStatus() {
        // When
        Health health = Health.down()
                .withDetail("error", "Connection refused")
                .build();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("Connection refused");
    }

    @Test
    @DisplayName("Should include detailed information in health response")
    void shouldIncludeDetailedInformationInHealthResponse() {
        // When
        Health health = Health.up()
                .withDetail("database", "H2")
                .withDetail("status", "Connected")
                .withDetail("connectionPool", "Active")
                .build();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).hasSize(3);
        assertThat(health.getDetails().get("database")).isEqualTo("H2");
        assertThat(health.getDetails().get("status")).isEqualTo("Connected");
        assertThat(health.getDetails().get("connectionPool")).isEqualTo("Active");
    }

    @Test
    @DisplayName("Should handle custom status")
    void shouldHandleCustomStatus() {
        // Given
        Status customStatus = new Status("DEGRADED", "Service is running but with reduced performance");

        // When
        Health health = Health.status(customStatus)
                .withDetail("performance", "Reduced")
                .withDetail("reason", "High load")
                .build();

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getStatus().getDescription()).isEqualTo("Service is running but with reduced performance");
        assertThat(health.getDetails().get("performance")).isEqualTo("Reduced");
        assertThat(health.getDetails().get("reason")).isEqualTo("High load");
    }

    @Test
    @DisplayName("Should create health with multiple details")
    void shouldCreateHealthWithMultipleDetails() {
        // When
        Health health = Health.up()
                .withDetail("service", "test-service")
                .withDetail("version", "1.0.0")
                .withDetail("environment", "test")
                .withDetail("uptime", "5 minutes")
                .build();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).hasSize(4);
        assertThat(health.getDetails().get("service")).isEqualTo("test-service");
        assertThat(health.getDetails().get("version")).isEqualTo("1.0.0");
        assertThat(health.getDetails().get("environment")).isEqualTo("test");
        assertThat(health.getDetails().get("uptime")).isEqualTo("5 minutes");
    }

    @Test
    @DisplayName("Should validate health status codes")
    void shouldValidateHealthStatusCodes() {
        // When & Then
        assertThat(Status.UP.getCode()).isEqualTo("UP");
        assertThat(Status.DOWN.getCode()).isEqualTo("DOWN");
        assertThat(Status.OUT_OF_SERVICE.getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(Status.UNKNOWN.getCode()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle empty health details")
    void shouldHandleEmptyHealthDetails() {
        // When
        Health upHealth = Health.up().build();
        Health downHealth = Health.down().build();

        // Then
        assertThat(upHealth.getDetails()).isEmpty();
        assertThat(downHealth.getDetails()).isEmpty();
        assertThat(upHealth.getStatus()).isEqualTo(Status.UP);
        assertThat(downHealth.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("Should create health with exception details")
    void shouldCreateHealthWithExceptionDetails() {
        // Given
        Exception testException = new RuntimeException("Database connection failed");

        // When
        Health health = Health.down(testException)
                .withDetail("database", "PostgreSQL")
                .withDetail("host", "localhost:5432")
                .build();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails().get("host")).isEqualTo("localhost:5432");
    }

    @Test
    @DisplayName("Should validate health builder pattern")
    void shouldValidateHealthBuilderPattern() {
        // When
        Health health = Health.status("MAINTENANCE")
                .withDetail("reason", "Scheduled maintenance")
                .withDetail("duration", "30 minutes")
                .withDetail("contact", "admin@example.com")
                .build();

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("MAINTENANCE");
        assertThat(health.getDetails()).hasSize(3);
        assertThat(health.getDetails().get("reason")).isEqualTo("Scheduled maintenance");
        assertThat(health.getDetails().get("duration")).isEqualTo("30 minutes");
        assertThat(health.getDetails().get("contact")).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("Should handle health status comparison")
    void shouldHandleHealthStatusComparison() {
        // Given
        Health upHealth = Health.up().build();
        Health downHealth = Health.down().build();
        Health unknownHealth = Health.unknown().build();

        // Then
        assertThat(upHealth.getStatus()).isEqualTo(Status.UP);
        assertThat(downHealth.getStatus()).isEqualTo(Status.DOWN);
        assertThat(unknownHealth.getStatus()).isEqualTo(Status.UNKNOWN);

        assertThat(upHealth.getStatus()).isNotEqualTo(downHealth.getStatus());
        assertThat(downHealth.getStatus()).isNotEqualTo(unknownHealth.getStatus());
    }
}