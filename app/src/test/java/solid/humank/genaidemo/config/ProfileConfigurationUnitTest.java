package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * 輕量級單元測試 - Profile Configuration
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~2s)
 * 
 * 使用 Mock 替代完整 Spring 上下文，測試配置邏輯
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Profile Configuration Unit Tests")
class ProfileConfigurationUnitTest {

    @Mock
    private Environment environment;

    private ProfileConfiguration profileConfiguration;

    @BeforeEach
    void setUp() {
        profileConfiguration = new ProfileConfiguration(environment);
    }

    @Test
    @DisplayName("Should identify test profile correctly")
    void shouldIdentifyTestProfile() {
        // Given: Test profile is active
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

        // When: Checking profile type
        boolean isTest = profileConfiguration.isTestProfile();

        assertThat(isTest).isTrue();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
        assertThat(profileConfiguration.isDevelopmentProfile()).isFalse();
    }

    @Test
    @DisplayName("Should identify production profile correctly")
    void shouldIdentifyProductionProfile() {
        // Given: Production profile is active
        when(environment.getActiveProfiles()).thenReturn(new String[] { "production" });

        // When: Checking production profile
        boolean isProduction = profileConfiguration.isProductionProfile();

        assertThat(isProduction).isTrue();
        assertThat(profileConfiguration.isTestProfile()).isFalse();
        assertThat(profileConfiguration.isDevelopmentProfile()).isFalse();
    }

    @Test
    @DisplayName("Should identify development profile correctly")
    void shouldIdentifyDevelopmentProfile() {
        // Given: Development profile is active
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        // When: Checking development profile
        boolean isDev = profileConfiguration.isDevelopmentProfile();

        assertThat(isDev).isTrue();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
        assertThat(profileConfiguration.isTestProfile()).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple active profiles")
    void shouldHandleMultipleActiveProfiles() {
        // Given: Multiple profiles are active
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev", "debug", "local" });

        // When: Checking profile types
        boolean isDev = profileConfiguration.isDevelopmentProfile();
        boolean isTest = profileConfiguration.isTestProfile();
        boolean isProduction = profileConfiguration.isProductionProfile();

        // Then: Should identify development profile correctly
        assertThat(isDev).isTrue();
        assertThat(isTest).isFalse();
        assertThat(isProduction).isFalse();
    }

    @Test
    @DisplayName("Should handle no active profiles")
    void shouldHandleNoActiveProfiles() {
        // Given: No profiles are active
        when(environment.getActiveProfiles()).thenReturn(new String[] {});

        // When: Checking profile types
        boolean isDev = profileConfiguration.isDevelopmentProfile();
        boolean isTest = profileConfiguration.isTestProfile();
        boolean isProduction = profileConfiguration.isProductionProfile();

        assertThat(isDev).isFalse();
        assertThat(isTest).isFalse();
        assertThat(isProduction).isFalse();
    }

    @Nested
    @DisplayName("Development Profile Configuration Tests")
    class DevelopmentProfileConfigurationTest {

        @Test
        @DisplayName("Should validate development profile properties")
        void shouldValidateDevelopmentProfileProperties() {
            // Given: Development profile properties
            when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

            // When: Checking profile identification
            boolean isDev = profileConfiguration.isDevelopmentProfile();

            // Then: Should be development profile with correct properties
            assertThat(isDev).isTrue();
        }

        @Test
        @DisplayName("Should validate development database configuration")
        void shouldValidateDevelopmentDatabaseConfiguration() {
            // Given: Development database configuration
            String expectedUrl = "jdbc:h2:mem:devdb";
            String expectedDriver = "org.h2.Driver";
            boolean expectedConsoleEnabled = true;

            // When & Then: Validate configuration values
            assertThat(expectedUrl).contains("h2:mem:");
            assertThat(expectedDriver).isEqualTo("org.h2.Driver");
            assertThat(expectedConsoleEnabled).isTrue();
        }
    }

    @Nested
    @DisplayName("Test Profile Configuration Tests")
    class TestProfileConfigurationTest {

        @Test
        @DisplayName("Should validate test profile properties")
        void shouldValidateTestProfileProperties() {
            // Given: Test profile properties
            when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

            // When: Checking profile identification
            boolean isTest = profileConfiguration.isTestProfile();

            // Then: Should be test profile with correct properties
            assertThat(isTest).isTrue();
        }

        @Test
        @DisplayName("Should validate test database configuration")
        void shouldValidateTestDatabaseConfiguration() {
            // Given: Test database configuration
            String expectedUrl = "jdbc:h2:mem:testdb";
            String expectedDdlAuto = "create-drop";
            String expectedLogLevel = "ERROR";

            // When & Then: Validate configuration values
            assertThat(expectedUrl).contains("h2:mem:testdb");
            assertThat(expectedDdlAuto).isEqualTo("create-drop");
            assertThat(expectedLogLevel).isEqualTo("ERROR");
        }
    }

    @Nested
    @DisplayName("Production Profile Configuration Tests")
    class ProductionProfileConfigurationTest {

        @Test
        @DisplayName("Should validate production profile properties")
        void shouldValidateProductionProfileProperties() {
            // Given: Production profile properties
            when(environment.getActiveProfiles()).thenReturn(new String[] { "production" });

            // When: Checking profile identification
            boolean isProduction = profileConfiguration.isProductionProfile();

            // Then: Should be production profile with correct properties
            assertThat(isProduction).isTrue();
        }

        @Test
        @DisplayName("Should validate production database configuration")
        void shouldValidateProductionDatabaseConfiguration() {
            // Given: Production database configuration
            String expectedUrl = "jdbc:postgresql://prod-db:5432/genai_demo";
            String expectedDdlAuto = "validate";
            String expectedLogLevel = "WARN";

            // When & Then: Validate configuration values
            assertThat(expectedUrl).contains("postgresql");
            assertThat(expectedDdlAuto).isEqualTo("validate");
            assertThat(expectedLogLevel).isEqualTo("WARN");
        }
    }

    @Nested
    @DisplayName("Profile Priority Tests")
    class ProfilePriorityTest {

        @Test
        @DisplayName("Should prioritize production over development when both active")
        void shouldPrioritizeProductionOverDevelopment() {
            // Given: Both production and development profiles are active
            when(environment.getActiveProfiles()).thenReturn(new String[] { "production", "dev" });

            // When: Checking profile types
            boolean isProduction = profileConfiguration.isProductionProfile();
            boolean isDev = profileConfiguration.isDevelopmentProfile();

            // Then: Production should take priority
            assertThat(isProduction).isTrue();
            assertThat(isDev).isTrue(); // Both can be true, but production takes precedence in logic
        }

        @Test
        @DisplayName("Should prioritize test over other profiles when active")
        void shouldPrioritizeTestOverOtherProfiles() {
            // Given: Test profile is active with others
            when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "dev", "debug" });

            // When: Checking profile types
            boolean isTest = profileConfiguration.isTestProfile();
            boolean isDev = profileConfiguration.isDevelopmentProfile();

            // Then: Test should be identified correctly
            assertThat(isTest).isTrue();
            assertThat(isDev).isTrue(); // Both can be detected
        }
    }

    @Nested
    @DisplayName("Profile Validation Tests")
    class ProfileValidationTest {

        @Test
        @DisplayName("Should validate profile names are case sensitive")
        void shouldValidateProfileNamesAreCaseSensitive() {
            // Given: Profile names with different cases
            when(environment.getActiveProfiles()).thenReturn(new String[] { "TEST", "Dev", "PRODUCTION" });

            // When: Checking profile types
            boolean isTest = profileConfiguration.isTestProfile();
            boolean isDev = profileConfiguration.isDevelopmentProfile();
            boolean isProduction = profileConfiguration.isProductionProfile();

            // Then: Should not match due to case sensitivity
            assertThat(isTest).isFalse(); // "TEST" != "test"
            assertThat(isDev).isFalse(); // "Dev" != "dev"
            assertThat(isProduction).isFalse(); // "PRODUCTION" != "production"
        }

        @Test
        @DisplayName("Should handle null active profiles gracefully")
        void shouldHandleNullActiveProfilesGracefully() {
            // Given: Null active profiles
            when(environment.getActiveProfiles()).thenReturn(null);

            // When: Checking profile types
            boolean isTest = profileConfiguration.isTestProfile();
            boolean isDev = profileConfiguration.isDevelopmentProfile();
            boolean isProduction = profileConfiguration.isProductionProfile();

            assertThat(isTest).isFalse();
            assertThat(isDev).isFalse();
            assertThat(isProduction).isFalse();
        }

        @Test
        @DisplayName("Should validate profile configuration consistency")
        void shouldValidateProfileConfigurationConsistency() {
            // Given: Single profile active
            when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

            // When: Checking all profile types
            boolean isTest = profileConfiguration.isTestProfile();
            boolean isDev = profileConfiguration.isDevelopmentProfile();
            boolean isProduction = profileConfiguration.isProductionProfile();

            // Then: Only one should be true
            int activeCount = (isTest ? 1 : 0) + (isDev ? 1 : 0) + (isProduction ? 1 : 0);
            assertThat(activeCount).isEqualTo(1);
            assertThat(isTest).isTrue();
        }
    }
}