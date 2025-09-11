package solid.humank.genaidemo.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DataRetentionConfiguration
 * Tests requirement 11.4: WHEN data is stored THEN the system SHALL implement
 * proper retention policies
 */
class DataRetentionConfigurationTest {

    private DataRetentionConfiguration retentionConfig;

    @BeforeEach
    void setUp() {
        retentionConfig = new DataRetentionConfiguration();
    }

    @Test
    void shouldHaveDefaultRetentionPolicies() {
        // When
        DataRetentionConfiguration.RetentionPolicy applicationLogsPolicy = retentionConfig
                .getPolicy("application-logs");
        DataRetentionConfiguration.RetentionPolicy securityLogsPolicy = retentionConfig.getPolicy("security-logs");
        DataRetentionConfiguration.RetentionPolicy auditLogsPolicy = retentionConfig.getPolicy("audit-logs");

        // Then
        assertThat(applicationLogsPolicy).isNotNull();
        assertThat(applicationLogsPolicy.getHotStorageDuration()).isEqualTo(Duration.ofDays(30));
        assertThat(applicationLogsPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(90));
        assertThat(applicationLogsPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(2555));
        assertThat(applicationLogsPolicy.isAutoCleanupEnabled()).isTrue();

        assertThat(securityLogsPolicy).isNotNull();
        assertThat(securityLogsPolicy.getHotStorageDuration()).isEqualTo(Duration.ofDays(90));
        assertThat(securityLogsPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(365));
        assertThat(securityLogsPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(2555));

        assertThat(auditLogsPolicy).isNotNull();
        assertThat(auditLogsPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(3650)); // 10 years
    }

    @Test
    void shouldReturnDefaultPolicyForUnknownDataType() {
        // When
        DataRetentionConfiguration.RetentionPolicy unknownPolicy = retentionConfig.getPolicy("unknown-data-type");

        // Then
        assertThat(unknownPolicy).isNotNull();
        assertThat(unknownPolicy.getHotStorageDuration()).isEqualTo(Duration.ofDays(30));
        assertThat(unknownPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(90));
        assertThat(unknownPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(365));
        assertThat(unknownPolicy.isAutoCleanupEnabled()).isTrue();
    }

    @Test
    void shouldAllowCustomRetentionPolicies() {
        // Given
        DataRetentionConfiguration.RetentionPolicy customPolicy = new DataRetentionConfiguration.RetentionPolicy(
                Duration.ofDays(7),
                Duration.ofDays(30),
                Duration.ofDays(90),
                true);

        // When
        retentionConfig.setPolicy("custom-data", customPolicy);
        DataRetentionConfiguration.RetentionPolicy retrievedPolicy = retentionConfig.getPolicy("custom-data");

        // Then
        assertThat(retrievedPolicy).isEqualTo(customPolicy);
        assertThat(retrievedPolicy.getHotStorageDuration()).isEqualTo(Duration.ofDays(7));
        assertThat(retrievedPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(30));
        assertThat(retrievedPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(90));
    }

    @Test
    void shouldCalculateTotalRetentionDuration() {
        // Given
        DataRetentionConfiguration.RetentionPolicy policy = new DataRetentionConfiguration.RetentionPolicy(
                Duration.ofDays(30),
                Duration.ofDays(90),
                Duration.ofDays(365),
                true);

        // When
        Duration totalDuration = policy.getTotalRetentionDuration();

        // Then
        assertThat(totalDuration).isEqualTo(Duration.ofDays(485)); // 30 + 90 + 365
    }

    @Test
    void shouldCheckIfDataIsExpired() {
        // Given
        DataRetentionConfiguration.RetentionPolicy policy = new DataRetentionConfiguration.RetentionPolicy(
                Duration.ofDays(1),
                Duration.ofDays(7),
                Duration.ofDays(30),
                true);

        LocalDateTime recentTimestamp = LocalDateTime.now().minusDays(20);
        LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(50);

        // When & Then
        assertThat(policy.isExpired(recentTimestamp)).isFalse();
        assertThat(policy.isExpired(oldTimestamp)).isTrue();
    }

    @Test
    void shouldHaveShortRetentionForTemporaryData() {
        // When
        DataRetentionConfiguration.RetentionPolicy tempDataPolicy = retentionConfig.getPolicy("temp-data");
        DataRetentionConfiguration.RetentionPolicy sessionDataPolicy = retentionConfig.getPolicy("session-data");

        // Then
        assertThat(tempDataPolicy.getHotStorageDuration()).isEqualTo(Duration.ofHours(1));
        assertThat(tempDataPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofHours(24));
        assertThat(tempDataPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(7));

        assertThat(sessionDataPolicy.getHotStorageDuration()).isEqualTo(Duration.ofHours(24));
        assertThat(sessionDataPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(7));
        assertThat(sessionDataPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void shouldHaveLongRetentionForBusinessData() {
        // When
        DataRetentionConfiguration.RetentionPolicy domainEventsPolicy = retentionConfig.getPolicy("domain-events");
        DataRetentionConfiguration.RetentionPolicy personalDataPolicy = retentionConfig.getPolicy("personal-data");

        // Then
        assertThat(domainEventsPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(2555)); // 7 years
        assertThat(personalDataPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(2555)); // 7 years
    }

    @Test
    void shouldHaveShortRetentionForMetricsAndTraces() {
        // When
        DataRetentionConfiguration.RetentionPolicy metricsPolicy = retentionConfig.getPolicy("metrics");
        DataRetentionConfiguration.RetentionPolicy tracesPolicy = retentionConfig.getPolicy("traces");

        // Then
        assertThat(metricsPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(365)); // 1 year
        assertThat(tracesPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(90)); // 3 months
    }
}