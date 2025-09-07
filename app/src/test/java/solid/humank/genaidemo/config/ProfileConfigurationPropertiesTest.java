package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Profile Configuration Properties Tests")
class ProfileConfigurationPropertiesTest {

    @Test
    @DisplayName("Should create valid profile configuration")
    void shouldCreateValidProfileConfiguration() {
        // Given
        String name = "development";
        String description = "Development environment";
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                true, true, true, false);

        // When
        ProfileConfigurationProperties config = new ProfileConfigurationProperties(name, description, features);

        // Then
        assertThat(config.name()).isEqualTo(name);
        assertThat(config.description()).isEqualTo(description);
        assertThat(config.features()).isEqualTo(features);
    }

    @Test
    @DisplayName("Should reject null or empty profile name")
    void shouldRejectNullOrEmptyProfileName() {
        // Given
        String description = "Test description";
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                true, true, true, false);

        // When & Then
        assertThatThrownBy(() -> new ProfileConfigurationProperties(null, description, features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile name cannot be null or empty");

        assertThatThrownBy(() -> new ProfileConfigurationProperties("", description, features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile name cannot be null or empty");

        assertThatThrownBy(() -> new ProfileConfigurationProperties("   ", description, features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile name cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject null or empty profile description")
    void shouldRejectNullOrEmptyProfileDescription() {
        // Given
        String name = "development";
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                true, true, true, false);

        // When & Then
        assertThatThrownBy(() -> new ProfileConfigurationProperties(name, null, features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile description cannot be null or empty");

        assertThatThrownBy(() -> new ProfileConfigurationProperties(name, "", features))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile description cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject null features")
    void shouldRejectNullFeatures() {
        // Given
        String name = "development";
        String description = "Test description";

        // When & Then
        assertThatThrownBy(() -> new ProfileConfigurationProperties(name, description, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile features cannot be null");
    }

    @Test
    @DisplayName("Should create valid profile features for development")
    void shouldCreateValidProfileFeaturesForDevelopment() {
        // When
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                true, true, true, false);

        // Then
        assertThat(features.h2Console()).isTrue();
        assertThat(features.debugLogging()).isTrue();
        assertThat(features.inMemoryEvents()).isTrue();
        assertThat(features.kafkaEvents()).isFalse();
    }

    @Test
    @DisplayName("Should create valid profile features for production")
    void shouldCreateValidProfileFeaturesForProduction() {
        // When
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                false, false, false, true);

        // Then
        assertThat(features.h2Console()).isFalse();
        assertThat(features.debugLogging()).isFalse();
        assertThat(features.inMemoryEvents()).isFalse();
        assertThat(features.kafkaEvents()).isTrue();
    }

    @Test
    @DisplayName("Should reject conflicting event configurations")
    void shouldRejectConflictingEventConfigurations() {
        // When & Then
        assertThatThrownBy(() -> new ProfileConfigurationProperties.ProfileFeatures(true, true, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot enable both in-memory and kafka events simultaneously");
    }

    @Test
    @DisplayName("Should allow both event types to be disabled")
    void shouldAllowBothEventTypesToBeDisabled() {
        // When
        ProfileConfigurationProperties.ProfileFeatures features = new ProfileConfigurationProperties.ProfileFeatures(
                false, false, false, false);

        // Then
        assertThat(features.inMemoryEvents()).isFalse();
        assertThat(features.kafkaEvents()).isFalse();
    }
}