package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests to verify that profile-specific bean configurations work correctly
 * These tests ensure that beans are properly configured based on active
 * profiles
 * and that test profile creates appropriate bean instances
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Test Profile Bean Configuration Tests")
class TestProfileBeanConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ProfileValidationConfiguration profileValidationConfiguration;

    @Test
    @DisplayName("Should create ProfileValidationConfiguration bean")
    void shouldCreateProfileValidationConfigurationBean() {
        // Given & When
        boolean beanExists = applicationContext.containsBean("profileValidationConfiguration");

        // Then
        assertThat(beanExists).isTrue();
        assertThat(profileValidationConfiguration).isNotNull();
    }

    @Test
    @DisplayName("Should create DataSource bean with test configuration")
    void shouldCreateDataSourceBeanWithTestConfiguration() {
        // Given & When
        boolean dataSourceExists = applicationContext.containsBean("dataSource");
        DataSource dataSource = applicationContext.getBean(DataSource.class);

        // Then
        assertThat(dataSourceExists).isTrue();
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("Should have ProfileConfigurationProperties bean")
    void shouldHaveProfileConfigurationPropertiesBean() {
        // Given & When
        ProfileConfigurationProperties properties = applicationContext
                .getBean(ProfileConfigurationProperties.class);

        // Then
        assertThat(properties).isNotNull();
        assertThat(properties.name()).isEqualTo("test");
        assertThat(properties.description()).isEqualTo("Test environment with H2 in-memory database");
        assertThat(properties.features()).isNotNull();
        assertThat(properties.features().h2Console()).isFalse();
        assertThat(properties.features().inMemoryEvents()).isTrue();
        assertThat(properties.features().kafkaEvents()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify test profile in bean configuration")
    void shouldCorrectlyIdentifyTestProfileInBeanConfiguration() {
        // Given & When
        boolean isTestProfile = profileValidationConfiguration.isTestProfile();

        // Then
        assertThat(isTestProfile).isTrue();
    }

    @Test
    @DisplayName("Should not create production-specific beans in test profile")
    void shouldNotCreateProductionSpecificBeansInTestProfile() {
        // Given & When
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // Then - Verify no production-specific beans are created
        assertThat(beanNames).noneMatch(name -> name.contains("production"));
        assertThat(beanNames).noneMatch(name -> name.contains("kafka") && !name.contains("test"));
    }

    @Test
    @DisplayName("Should create test-specific configuration beans")
    void shouldCreateTestSpecificConfigurationBeans() {
        // Given & When
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // Then - Verify test-specific beans exist
        assertThat(beanNames).anyMatch(name -> name.contains("profileValidationConfiguration"));
        assertThat(beanNames).anyMatch(name -> name.contains("dataSource"));
    }

    @Test
    @DisplayName("Should validate bean dependencies in test profile")
    void shouldValidateBeanDependenciesInTestProfile() {
        // Given & When
        ProfileValidationConfiguration config = applicationContext.getBean(ProfileValidationConfiguration.class);

        // Then - Verify dependencies are properly injected
        assertThat(config).isNotNull();
        assertThat(config.isTestProfile()).isTrue();
    }
}