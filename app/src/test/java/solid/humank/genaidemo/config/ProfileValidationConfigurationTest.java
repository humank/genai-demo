package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Profile Validation Configuration Tests")
class ProfileValidationConfigurationTest {

    @Mock
    private Environment environment;

    private ProfileConfigurationProperties profileProperties;
    private ProfileValidationConfiguration validationConfiguration;

    private boolean simulateProductionEnvironment = false;

    @BeforeEach
    void setUp() {
        profileProperties = new ProfileConfigurationProperties(
                "test",
                "Test profile",
                new ProfileConfigurationProperties.ProfileFeatures(true, true, true, false));
        validationConfiguration = new ProfileValidationConfiguration(environment, profileProperties) {
            @Override
            protected boolean isTestEnvironment() {
                return !simulateProductionEnvironment && super.isTestEnvironment();
            }
        };
    }

    @Test
    @DisplayName("Should validate development profile successfully")
    void shouldValidateDevelopmentProfileSuccessfully() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:genaidemo");
        when(environment.getProperty("spring.h2.console.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/h2");

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should fail validation when development profile uses wrong database")
    void shouldFailValidationWhenDevelopmentProfileUsesWrongDatabase() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:postgresql://localhost:5432/test");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessage("Development profile must use H2 in-memory database");
    }

    @Test
    @DisplayName("Should fail validation when development profile uses wrong Flyway locations")
    void shouldFailValidationWhenDevelopmentProfileUsesWrongFlywayLocations() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:genaidemo");
        when(environment.getProperty("spring.h2.console.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/postgresql");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessage("Development profile must use H2-specific Flyway migrations");
    }

    @Test
    @DisplayName("Should validate production profile successfully")
    void shouldValidateProductionProfileSuccessfully() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "production" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:postgresql://localhost:5432/genaidemo");
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/postgresql");
        when(environment.getProperty("DB_HOST")).thenReturn("localhost");
        when(environment.getProperty("DB_NAME")).thenReturn("genaidemo");
        when(environment.getProperty("DB_USERNAME")).thenReturn("genaidemo");
        when(environment.getProperty("spring.kafka.bootstrap-servers")).thenReturn("localhost:9092");
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should fail validation when production profile uses wrong database")
    void shouldFailValidationWhenProductionProfileUsesWrongDatabase() {
        // Given - simulate production environment
        simulateProductionEnvironment = true;
        when(environment.getActiveProfiles()).thenReturn(new String[] { "production" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:mysql://localhost:3306/genaidemo");
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/postgresql");
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);
        // Mock required properties to avoid earlier validation failures
        when(environment.getProperty("DB_HOST")).thenReturn("localhost");
        when(environment.getProperty("DB_NAME")).thenReturn("genaidemo");
        when(environment.getProperty("DB_USERNAME")).thenReturn("genaidemo");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessage("Production profile must use PostgreSQL database");
    }

    @Test
    @DisplayName("Should fail validation when production profile uses wrong Flyway locations")
    void shouldFailValidationWhenProductionProfileUsesWrongFlywayLocations() {
        // Given - simulate production environment
        simulateProductionEnvironment = true;
        when(environment.getActiveProfiles()).thenReturn(new String[] { "production" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:postgresql://localhost:5432/genaidemo");
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/h2");
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);
        // Mock required properties to avoid earlier validation failures
        when(environment.getProperty("DB_HOST")).thenReturn("localhost");
        when(environment.getProperty("DB_NAME")).thenReturn("genaidemo");
        when(environment.getProperty("DB_USERNAME")).thenReturn("genaidemo");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessage("Production profile must use PostgreSQL-specific Flyway migrations");
    }

    @Test
    @DisplayName("Should handle no active profiles gracefully")
    void shouldHandleNoActiveProfilesGracefully() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] {});

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should handle unknown profiles gracefully")
    void shouldHandleUnknownProfilesGracefully() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "unknown-profile" });

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    // ===== Test Profile Isolation and Security Tests =====

    @Test
    @DisplayName("Should validate test profile successfully")
    void shouldValidateTestProfileSuccessfully() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:testdb");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("create-drop");

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should detect test profile correctly")
    void shouldDetectTestProfileCorrectly() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

        // When
        boolean isTestProfile = validationConfiguration.isTestProfile();

        // Then
        assertThat(isTestProfile).isTrue();
    }

    @Test
    @DisplayName("Should not detect test profile when other profiles are active")
    void shouldNotDetectTestProfileWhenOtherProfilesAreActive() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        // When
        boolean isTestProfile = validationConfiguration.isTestProfile();

        // Then
        assertThat(isTestProfile).isFalse();
    }

    @Test
    @DisplayName("Should prevent test profile activation with production profile")
    void shouldPreventTestProfileActivationWithProductionProfile() {
        // Given - simulate production environment
        simulateProductionEnvironment = true;
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "production" });
        when(environment.getProperty("app.profile.validation.strict")).thenReturn("true");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessageContaining("SECURITY VIOLATION")
                .hasMessageContaining("'test' and 'production' profiles must not be active together");
    }

    @Test
    @DisplayName("Should prevent test profile activation with development profile")
    void shouldPreventTestProfileActivationWithDevelopmentProfile() {
        // Given - create a custom validation configuration that simulates non-test
        // environment
        ProfileValidationConfiguration customValidationConfiguration = new ProfileValidationConfiguration(environment,
                profileProperties) {
            @Override
            protected boolean isTestEnvironment() {
                return false; // Simulate non-test environment to enable strict validation
            }
        };

        when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "dev" });
        when(environment.getProperty("app.profile.validation.strict")).thenReturn("true");
        // Mock required properties to avoid earlier validation failures
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:genaidemo");
        when(environment.getProperty("spring.h2.console.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/h2");
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> customValidationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessageContaining("CONFIGURATION CONFLICT")
                .hasMessageContaining("'test' and 'development' profiles should not be active together");
    }

    @Test
    @DisplayName("Should allow test profile with openapi profile")
    void shouldAllowTestProfileWithOpenapiProfile() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "openapi" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:testdb");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("create-drop");

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should validate test profile uses secure database configuration")
    void shouldValidateTestProfileUsesSecureDatabaseConfiguration() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:testdb");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("create-drop");

        // When & Then - should not throw exception and log appropriate warnings
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should warn when test profile uses non-memory database")
    void shouldWarnWhenTestProfileUsesNonMemoryDatabase() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:file:./testdb");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("create-drop");

        // When & Then - should not throw exception but should log warning
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should warn when test profile uses non-create-drop DDL")
    void shouldWarnWhenTestProfileUsesNonCreateDropDDL() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:testdb");
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("update");

        // When & Then - should not throw exception but should log warning
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should not enable strict validation in test environment")
    void shouldNotEnableStrictValidationInTestEnvironment() {
        // Given - test environment (default behavior)
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "production" });
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn("test");
        // Mock required properties for production profile validation
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:testdb");
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/h2");
        when(environment.getProperty("DB_HOST")).thenReturn("localhost");
        when(environment.getProperty("DB_NAME")).thenReturn("testdb");
        when(environment.getProperty("DB_USERNAME")).thenReturn("sa");

        // When & Then - should not throw exception because strict validation is
        // disabled in test environment
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should enable strict validation in production environment")
    void shouldEnableStrictValidationInProductionEnvironment() {
        // Given - simulate production environment
        simulateProductionEnvironment = true;
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test", "production" });
        when(environment.getProperty("spring.boot.test.context.SpringBootTestContextBootstrapper")).thenReturn(null);

        // When & Then - should throw exception because strict validation is enabled in
        // production
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessageContaining("SECURITY VIOLATION");
    }

    @Test
    @DisplayName("Should validate multiple valid profiles")
    void shouldValidateMultipleValidProfiles() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev", "openapi" });
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:genaidemo");
        when(environment.getProperty("spring.h2.console.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("spring.flyway.locations")).thenReturn("classpath:db/migration/h2");

        // When & Then - should not throw exception
        validationConfiguration.validateProfileConfiguration();
    }

    @Test
    @DisplayName("Should reject invalid profile combinations in strict mode")
    void shouldRejectInvalidProfileCombinationsInStrictMode() {
        // Given - simulate production environment with strict validation
        simulateProductionEnvironment = true;
        when(environment.getActiveProfiles()).thenReturn(new String[] { "invalid-profile" });
        when(environment.getProperty("app.profile.validation.strict")).thenReturn("true");

        // When & Then
        assertThatThrownBy(() -> validationConfiguration.validateProfileConfiguration())
                .isInstanceOf(ProfileValidationConfiguration.ProfileConfigurationException.class)
                .hasMessageContaining("Invalid profile 'invalid-profile' detected");
    }
}