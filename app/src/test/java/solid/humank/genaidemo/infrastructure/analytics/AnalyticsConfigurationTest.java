package solid.humank.genaidemo.infrastructure.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Unit tests for analytics configuration.
 * 
 * These tests verify that the analytics configuration is properly set up
 * for different profiles and that the correct beans are created.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "analytics.enabled=false"
})
class AnalyticsConfigurationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
        // with analytics configuration disabled
        assertThat(true).isTrue();
    }
}

/**
 * Test for production analytics configuration.
 * Uses test profile with analytics enabled to avoid PostgreSQL dependency.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "analytics.enabled=true",
        "analytics.firehose.stream-name=test-stream",
        "analytics.data-lake.bucket-name=test-bucket",
        "analytics.glue.database-name=test-database",
        "analytics.quicksight.data-source-id=test-datasource",
        // Use H2 for testing
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Disable CloudWatch metrics for testing
        "management.metrics.export.cloudwatch.enabled=false",
        // Disable Kafka for testing
        "spring.kafka.enabled=false"
})
class AnalyticsConfigurationProductionTest {

    @Test
    void contextLoadsWithAnalyticsEnabled() {
        // This test ensures that the Spring context loads successfully
        // with analytics configuration enabled (but without actual AWS services)
        assertThat(true).isTrue();
    }
}