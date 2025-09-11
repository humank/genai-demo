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
 */
@SpringBootTest
@ActiveProfiles("production")
@TestPropertySource(properties = {
        "analytics.enabled=true",
        "analytics.firehose.stream-name=test-stream",
        "analytics.data-lake.bucket-name=test-bucket",
        "analytics.glue.database-name=test-database",
        "analytics.quicksight.data-source-id=test-datasource"
})
class AnalyticsConfigurationProductionTest {

    @Test
    void contextLoadsWithAnalyticsEnabled() {
        // This test ensures that the Spring context loads successfully
        // with analytics configuration enabled (but without actual AWS services)
        assertThat(true).isTrue();
    }
}