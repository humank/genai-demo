package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import solid.humank.genaidemo.config.ProfileValidationConfiguration.ProfileConfigurationException;

/**
 * Unit tests for Profile Validation Configuration
 * Tests profile validation logic, error handling, and edge cases
 */
class ProfileValidationConfigurationTest {

    private Environment mockEnvironment;
    private ProfileConfigurationProperties mockProperties;
    private ProfileValidationConfiguration profileValidation;

    @BeforeEach
    void setUp() {
        mockEnvironment = mock(Environment.class);
        mockProperties = createMockProperties("test", "Test environment", 
            new ProfileConfigurationProperties.ProfileFeatures(true, true, true, false));
        
        // Create a testable ProfileValidationConfiguration that allows overriding test environment detection
        profileValidation = new ProfileValidationConfiguration(mockEnvironment, mockProperties) {
            @Override
            protected boolean isTestEnvironment() {
                // Check if we're explicitly setting strict mode for testing
                String strictMode = mockEnvironment.getProperty("app.profile.validation.strict");
                if ("true".equals(strictMode)) {
                    // For strict mode tests, pretend we're not in test environment
                    String bootstrapper = mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper");
                    return bootstrapper != null;
                }
                return super.isTestEnvironment();
            }
        };
    }

    @Nested
    @DisplayName("Profile Validation Tests")
    class ProfileValidationTests {

        @Test
        @DisplayName("Should validate single valid profile")
        void shouldValidateSingleValidProfile() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            // When & Then
            // Should not throw exception
            profileValidation.validateProfilesOnStartup();
        }

        @Test
        @DisplayName("Should validate multiple valid profiles")
        void shouldValidateMultipleValidProfiles() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"dev", "openapi"});

            // When & Then
            // Should not throw exception
            profileValidation.validateProfilesOnStartup();
        }

        @Test
        @DisplayName("Should handle empty profiles gracefully")
        void shouldHandleEmptyProfilesGracefully() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{});

            // When & Then
            // Should not throw exception
            profileValidation.validateProfilesOnStartup();
        }

        @Test
        @DisplayName("Should reject invalid profile in strict mode")
        void shouldRejectInvalidProfileInStrictMode() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"invalid-profile"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("true");
            // Mock test environment detection to return false for strict validation
            when(mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> profileValidation.validateProfilesOnStartup())
                .isInstanceOf(ProfileConfigurationException.class)
                .hasMessageContaining("Invalid profile 'invalid-profile'");
        }
    }

    @Nested
    @DisplayName("Profile Combination Validation Tests")
    class ProfileCombinationValidationTests {

        @Test
        @DisplayName("Should reject test + production combination")
        void shouldRejectTestProductionCombination() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"test", "production"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("true");
            // Mock test environment detection to return false for strict validation
            when(mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> profileValidation.validateProfilesOnStartup())
                .isInstanceOf(ProfileConfigurationException.class)
                .hasMessageContaining("SECURITY VIOLATION");
        }

        @Test
        @DisplayName("Should reject test + development combination")
        void shouldRejectTestDevelopmentCombination() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"test", "dev"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("true");
            // Mock test environment detection to return false for strict validation
            when(mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> profileValidation.validateProfilesOnStartup())
                .isInstanceOf(ProfileConfigurationException.class)
                .hasMessageContaining("CONFIGURATION CONFLICT");
        }

        @Test
        @DisplayName("Should reject production + development combination")
        void shouldRejectProductionDevelopmentCombination() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"production", "dev"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("true");
            // Mock test environment detection to return false for strict validation
            when(mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> profileValidation.validateProfilesOnStartup())
                .isInstanceOf(ProfileConfigurationException.class)
                .hasMessageContaining("ENVIRONMENT CONFLICT");
        }

        @Test
        @DisplayName("Should allow test + openapi combination")
        void shouldAllowTestOpenApiCombination() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"test", "openapi"});

            // When & Then
            // Should not throw exception
            profileValidation.validateProfilesOnStartup();
        }
    }

    @Nested
    @DisplayName("Test Environment Detection Tests")
    class TestEnvironmentDetectionTests {

        @Test
        @DisplayName("Should detect test profile as test environment")
        void shouldDetectTestProfileAsTestEnvironment() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"test"});

            // When
            boolean isTestEnv = profileValidation.isTestProfile();

            // Then
            assertThat(isTestEnv).isTrue();
        }

        @Test
        @DisplayName("Should not detect dev profile as test environment")
        void shouldNotDetectDevProfileAsTestEnvironment() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            // When
            boolean isTestEnv = profileValidation.isTestProfile();

            // Then
            assertThat(isTestEnv).isFalse();
        }
    }

    @Nested
    @DisplayName("Configuration Properties Tests")
    class ConfigurationPropertiesTests {

        @Test
        @DisplayName("Should validate profile configuration properties")
        void shouldValidateProfileConfigurationProperties() {
            // Given
            ProfileConfigurationProperties.ProfileFeatures features = 
                new ProfileConfigurationProperties.ProfileFeatures(true, true, true, false);
            
            // When
            ProfileConfigurationProperties properties = 
                new ProfileConfigurationProperties("test", "Test environment", features);

            // Then
            assertThat(properties.name()).isEqualTo("test");
            assertThat(properties.description()).isEqualTo("Test environment");
            assertThat(properties.features().h2Console()).isTrue();
            assertThat(properties.features().debugLogging()).isTrue();
            assertThat(properties.features().inMemoryEvents()).isTrue();
            assertThat(properties.features().kafkaEvents()).isFalse();
        }

        @Test
        @DisplayName("Should reject null profile name")
        void shouldRejectNullProfileName() {
            // Given
            ProfileConfigurationProperties.ProfileFeatures features = 
                new ProfileConfigurationProperties.ProfileFeatures(true, true, true, false);

            // When & Then
            assertThatThrownBy(() -> 
                new ProfileConfigurationProperties(null, "Test environment", features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile name cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject empty profile name")
        void shouldRejectEmptyProfileName() {
            // Given
            ProfileConfigurationProperties.ProfileFeatures features = 
                new ProfileConfigurationProperties.ProfileFeatures(true, true, true, false);

            // When & Then
            assertThatThrownBy(() -> 
                new ProfileConfigurationProperties("", "Test environment", features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile name cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject conflicting event configurations")
        void shouldRejectConflictingEventConfigurations() {
            // When & Then
            assertThatThrownBy(() -> 
                new ProfileConfigurationProperties.ProfileFeatures(true, true, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot enable both in-memory and kafka events simultaneously");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should provide clear error messages for invalid profiles")
        void shouldProvideClearErrorMessagesForInvalidProfiles() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"invalid-profile"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("true");
            // Mock test environment detection to return false for strict validation
            when(mockEnvironment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> profileValidation.validateProfilesOnStartup())
                .isInstanceOf(ProfileConfigurationException.class)
                .hasMessageContaining("Invalid profile 'invalid-profile'")
                .hasMessageContaining("Valid application profiles are:");
        }

        @Test
        @DisplayName("Should provide fallback behavior for non-strict mode")
        void shouldProvideFallbackBehaviorForNonStrictMode() {
            // Given
            when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"unknown-profile"});
            when(mockEnvironment.getProperty("app.profile.validation.strict")).thenReturn("false");

            // When & Then
            // Should not throw exception in non-strict mode
            profileValidation.validateProfilesOnStartup();
        }
    }

    private ProfileConfigurationProperties createMockProperties(String name, String description, 
            ProfileConfigurationProperties.ProfileFeatures features) {
        return new ProfileConfigurationProperties(name, description, features);
    }
}