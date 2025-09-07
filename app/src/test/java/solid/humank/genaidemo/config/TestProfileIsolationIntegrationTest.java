package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests to verify test profile isolation and security
 * These tests ensure that test profile configurations are properly isolated
 * and don't expose sensitive data or configurations
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Test Profile Isolation Integration Tests")
class TestProfileIsolationIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProfileValidationConfiguration profileValidationConfiguration;

    @Test
    @DisplayName("Should use test profile configuration")
    void shouldUseTestProfileConfiguration() {
        // Given & When
        String[] activeProfiles = environment.getActiveProfiles();

        // Then
        assertThat(activeProfiles).contains("test");
        assertThat(profileValidationConfiguration.isTestProfile()).isTrue();
    }

    @Test
    @DisplayName("Should use H2 in-memory database for test profile")
    void shouldUseH2InMemoryDatabaseForTestProfile() throws Exception {
        // Given & When
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            String url = metaData.getURL();

            // Then
            assertThat(databaseProductName).isEqualTo("H2");
            assertThat(url).contains("mem:");
            assertThat(url).doesNotContain("file:");
        }
    }

    @Test
    @DisplayName("Should use secure test database configuration")
    void shouldUseSecureTestDatabaseConfiguration() {
        // Given & When
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        // Then
        assertThat(datasourceUrl).contains("h2:mem:");
        assertThat(datasourceUrl).contains("testdb");
        assertThat(username).isEqualTo("sa");
        assertThat(password).isNullOrEmpty(); // No password for test database
    }

    @Test
    @DisplayName("Should disable external services in test profile")
    void shouldDisableExternalServicesInTestProfile() {
        // Given & When
        String flywayEnabled = environment.getProperty("spring.flyway.enabled");
        String kafkaEvents = environment.getProperty("genai-demo.profile.features.kafkaEvents");
        String inMemoryEvents = environment.getProperty("genai-demo.profile.features.inMemoryEvents");

        // Then
        assertThat(flywayEnabled).isEqualTo("false");
        assertThat(kafkaEvents).isEqualTo("false");
        assertThat(inMemoryEvents).isEqualTo("true");
    }

    @Test
    @DisplayName("Should use test-specific JPA configuration")
    void shouldUseTestSpecificJpaConfiguration() {
        // Given & When
        String ddlAuto = environment.getProperty("genai-demo.jpa.hibernate.ddl-auto");
        String showSql = environment.getProperty("genai-demo.jpa.show-sql");
        String useSecondLevelCache = environment
                .getProperty("genai-demo.jpa.properties.hibernate.cache.use_second_level_cache");

        // Then
        assertThat(ddlAuto).isEqualTo("create-drop");
        assertThat(showSql).isEqualTo("false");
        assertThat(useSecondLevelCache).isEqualTo("false");
    }

    @Test
    @DisplayName("Should use appropriate logging configuration for tests")
    void shouldUseAppropriateLoggingConfigurationForTests() {
        // Given & When
        String springLogLevel = environment.getProperty("logging.level.org.springframework");
        String hibernateLogLevel = environment.getProperty("logging.level.org.hibernate");
        String appLogLevel = environment.getProperty("logging.level.solid.humank.genaidemo");

        // Then
        assertThat(springLogLevel).isEqualTo("WARN");
        assertThat(hibernateLogLevel).isEqualTo("WARN");
        assertThat(appLogLevel).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Should disable H2 console in test profile for security")
    void shouldDisableH2ConsoleInTestProfileForSecurity() {
        // Given & When
        String h2ConsoleEnabled = environment.getProperty("genai-demo.profile.features.h2Console");

        // Then
        assertThat(h2ConsoleEnabled).isEqualTo("false");
    }

    @Test
    @DisplayName("Should use minimal management endpoints in test profile")
    void shouldUseMinimalManagementEndpointsInTestProfile() {
        // Given & When
        String exposedEndpoints = environment.getProperty("management.endpoints.web.exposure.include");
        String healthShowDetails = environment.getProperty("management.endpoint.health.show-details");
        String metricsEnabled = environment.getProperty("management.metrics.enable.all");

        // Then
        assertThat(exposedEndpoints).isEqualTo("health");
        assertThat(healthShowDetails).isEqualTo("never");
        assertThat(metricsEnabled).isEqualTo("false");
    }

    @Test
    @DisplayName("Should enable lazy initialization for faster test startup")
    void shouldEnableLazyInitializationForFasterTestStartup() {
        // Given & When
        String lazyInitialization = environment.getProperty("spring.main.lazy-initialization");

        // Then
        assertThat(lazyInitialization).isEqualTo("true");
    }

    @Test
    @DisplayName("Should exclude unnecessary auto-configurations in test profile")
    void shouldExcludeUnnecessaryAutoConfigurationsInTestProfile() {
        // Given & When
        String excludedConfigs = environment.getProperty("spring.autoconfigure.exclude");

        // Then
        assertThat(excludedConfigs).isNotNull();
        assertThat(excludedConfigs).contains("FlywayAutoConfiguration");
        assertThat(excludedConfigs).contains("AopAutoConfiguration");
    }
}