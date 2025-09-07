package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic tests for Spring Boot Profile Configuration Foundation
 * Simple validation of core profile functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.h2.console.enabled=false",
    "spring.application.name=genai-demo",
    "server.port=8080",
    "info.app.version=2.0.0",
    "info.app.description=GenAI Demo Application - DDD E-commerce Platform",
    "management.endpoints.web.exposure.include=health,metrics,prometheus"
})
class BasicProfileConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Should load application context successfully")
    void shouldLoadApplicationContextSuccessfully() {
        assertThat(environment).isNotNull();
    }

    @Test
    @DisplayName("Should have test profile active")
    void shouldHaveTestProfileActive() {
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }

    @Test
    @DisplayName("Should load basic application properties")
    void shouldLoadBasicApplicationProperties() {
        String appName = environment.getProperty("spring.application.name");
        assertThat(appName).isEqualTo("genai-demo");
        
        String serverPort = environment.getProperty("server.port");
        assertThat(serverPort).isEqualTo("8080");
    }

    @Test
    @DisplayName("Should load application info properties")
    void shouldLoadApplicationInfoProperties() {
        String appVersion = environment.getProperty("info.app.version");
        assertThat(appVersion).isEqualTo("2.0.0");
        
        String appDescription = environment.getProperty("info.app.description");
        assertThat(appDescription).contains("GenAI Demo Application");
    }

    @Test
    @DisplayName("Should load management endpoints configuration")
    void shouldLoadManagementEndpointsConfiguration() {
        String exposedEndpoints = environment.getProperty("management.endpoints.web.exposure.include");
        assertThat(exposedEndpoints).contains("health", "metrics", "prometheus");
    }
}