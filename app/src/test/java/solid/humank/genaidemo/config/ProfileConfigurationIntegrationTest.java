package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Profile Configuration Foundation
 * Tests the complete profile configuration loading and validation process
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.flyway.locations=classpath:db/migration/h2",
    "spring.h2.console.enabled=false",
    "spring.application.name=genai-demo",
    "spring.application.version=2.0.0",
    "management.endpoints.web.exposure.include=health,info,metrics",
    "genai-demo.profile.name=test",
    "genai-demo.profile.description=Test environment with isolated configuration",
    "genai-demo.profile.features.h2Console=false",
    "genai-demo.profile.features.debugLogging=true",
    "genai-demo.profile.features.inMemoryEvents=true",
    "genai-demo.profile.features.kafkaEvents=false"
})
class ProfileConfigurationIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ProfileConfiguration profileConfiguration;

    @Autowired    private ProfileConfigurationProperties profileProperties;

    @Autowired
    private ProfileValidationConfiguration profileValidation;

    @Test
    @DisplayName("Should load application context with test profile")
    void shouldLoadApplicationContextWithTestProfile() {
        // Given: Test profile is active
        // When: Application context loads
        // Then: All configuration beans should be available
        assertThat(environment).isNotNull();
        assertThat(profileConfiguration).isNotNull();
        assertThat(profileProperties).isNotNull();
        assertThat(profileValidation).isNotNull();
    }

    @Test
    @DisplayName("Should have correct profile configuration properties")
    void shouldHaveCorrectProfileConfigurationProperties() {
        // Given: Test profile is active
        // When: Reading profile properties
        // Then: Properties should match test profile configuration
        assertThat(profileProperties.name()).isEqualTo("test");
        assertThat(profileProperties.description()).containsIgnoringCase("test");
        assertThat(profileProperties.features().h2Console()).isFalse(); // H2 console disabled in test for security
        assertThat(profileProperties.features().inMemoryEvents()).isTrue();
        assertThat(profileProperties.features().kafkaEvents()).isFalse();
    }

    @Test
    @DisplayName("Should have correct environment properties")
    void shouldHaveCorrectEnvironmentProperties() {
        // Given: Test profile is active
        // When: Reading environment properties
        // Then: Properties should be correctly loaded
        assertThat(environment.getActiveProfiles()).contains("test");
        
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertThat(datasourceUrl).contains("h2:mem:");
        
        String flywayLocations = environment.getProperty("spring.flyway.locations");
        assertThat(flywayLocations).contains("h2");
    }

    @Test
    @DisplayName("Should validate profile configuration successfully")
    void shouldValidateProfileConfigurationSuccessfully() {
        // Given: Test profile is active with valid configuration
        // When: Validation runs
        // Then: Should not throw any exceptions
        assertThat(profileValidation.isTestProfile()).isTrue();
        
        // Validation should complete without exceptions
        profileValidation.validateProfilesOnStartup();
    }

    @Test
    @DisplayName("Should have correct profile identification")
    void shouldHaveCorrectProfileIdentification() {
        // Given: Test profile is active
        // When: Checking profile types
        // Then: Should correctly identify profile types
        assertThat(profileConfiguration.isTestProfile()).isTrue();
        assertThat(profileConfiguration.isDevelopmentProfile()).isFalse();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
    }

    @Test
    @DisplayName("Should have application information configured")
    void shouldHaveApplicationInformationConfigured() {
        // Given: Application is running
        // When: Reading application info
        // Then: Should have correct application information
        String appName = environment.getProperty("spring.application.name");
        assertThat(appName).isEqualTo("genai-demo");
        
        String appVersion = environment.getProperty("spring.application.version");
        assertThat(appVersion).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("Should have management endpoints configured")
    void shouldHaveManagementEndpointsConfigured() {
        // Given: Application is running
        // When: Reading management configuration
        // Then: Should have health and metrics endpoints enabled
        String exposedEndpoints = environment.getProperty("management.endpoints.web.exposure.include");
        assertThat(exposedEndpoints).contains("health");
        assertThat(exposedEndpoints).contains("info");
        assertThat(exposedEndpoints).contains("metrics");
    }
}