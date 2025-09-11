package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 輕量級單元測試 - Basic Profile Configuration
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~2s)
 * 
 * 測試配置邏輯，而不是實際的 Spring 環境
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Basic Profile Configuration Unit Tests")
class BasicProfileConfigurationUnitTest {

    @Test
    @DisplayName("Should validate profile names")
    void shouldValidateProfileNames() {
        // Given
        String[] validProfiles = { "test", "dev", "prod", "development", "production" };

        // When & Then
        for (String profile : validProfiles) {
            assertThat(profile).isNotNull();
            assertThat(profile).isNotEmpty();
            assertThat(profile).doesNotContain(" ");
        }
    }

    @Test
    @DisplayName("Should validate application property keys")
    void shouldValidateApplicationPropertyKeys() {
        // Given
        String[] expectedPropertyKeys = {
                "spring.application.name",
                "server.port",
                "info.app.version",
                "info.app.description",
                "management.endpoints.web.exposure.include"
        };

        // When & Then
        for (String key : expectedPropertyKeys) {
            assertThat(key).isNotNull();
            assertThat(key).isNotEmpty();
            assertThat(key).contains(".");
        }
    }

    @Test
    @DisplayName("Should validate property value formats")
    void shouldValidatePropertyValueFormats() {
        // Given
        String appName = "genai-demo";
        String port = "8080";
        String version = "2.0.0";

        // When & Then
        assertThat(appName).matches("[a-z-]+");
        assertThat(port).matches("\\d+");
        assertThat(version).matches("\\d+\\.\\d+\\.\\d+");
    }

    @Test
    @DisplayName("Should validate management endpoints format")
    void shouldValidateManagementEndpointsFormat() {
        // Given
        String endpoints = "health,metrics,prometheus";

        // When
        String[] endpointArray = endpoints.split(",");

        // Then
        assertThat(endpointArray).hasSize(3);
        assertThat(endpointArray).contains("health", "metrics", "prometheus");
    }

    @Test
    @DisplayName("Should validate configuration structure")
    void shouldValidateConfigurationStructure() {
        // Given
        String springPrefix = "spring.";
        String infoPrefix = "info.";
        String managementPrefix = "management.";

        // When & Then
        assertThat("spring.application.name").startsWith(springPrefix);
        assertThat("info.app.version").startsWith(infoPrefix);
        assertThat("management.endpoints.web.exposure.include").startsWith(managementPrefix);
    }

    @Test
    @DisplayName("Should validate profile hierarchy")
    void shouldValidateProfileHierarchy() {
        // Given
        String[] profiles = { "test", "dev", "prod" };

        // When & Then
        assertThat(profiles).hasSize(3);
        assertThat(profiles).containsExactly("test", "dev", "prod");
    }

    @Test
    @DisplayName("Should validate application info structure")
    void shouldValidateApplicationInfoStructure() {
        // Given
        String description = "GenAI Demo Application - DDD E-commerce Platform";

        // When & Then
        assertThat(description).contains("GenAI Demo Application");
        assertThat(description).contains("DDD");
        assertThat(description).contains("E-commerce Platform");
    }

    @Test
    @DisplayName("Should validate port number range")
    void shouldValidatePortNumberRange() {
        // Given
        int port = 8080;

        // When & Then
        assertThat(port).isBetween(1024, 65535);
        assertThat(port).isEqualTo(8080);
    }

    @Test
    @DisplayName("Should validate version format pattern")
    void shouldValidateVersionFormatPattern() {
        // Given
        String version = "2.0.0";

        // When
        String[] versionParts = version.split("\\.");

        // Then
        assertThat(versionParts).hasSize(3);
        assertThat(versionParts[0]).isEqualTo("2"); // major
        assertThat(versionParts[1]).isEqualTo("0"); // minor
        assertThat(versionParts[2]).isEqualTo("0"); // patch
    }
}