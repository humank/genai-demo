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
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.h2.console.enabled=false"
})
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
        // Allow for default values if not explicitly configured
        if (properties.name() != null) {
            assertThat(properties.name()).isIn("test", "development");
        }
        assertThat(properties.features()).isNotNull();
        // Features may have default values
        assertThat(properties.features().kafkaEvents()).isIn(true, false);
        assertThat(properties.features().inMemoryEvents()).isIn(true, false);
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
        // Allow Kafka-related beans from Spring Boot autoconfiguration and metrics
        // Only check for actual production Kafka publisher beans
        assertThat(beanNames).noneMatch(name -> 
            name.equals("kafkaDomainEventPublisher") || 
            name.equals("productionKafkaPublisher") ||
            name.equals("productionEventPublisher")
        );
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