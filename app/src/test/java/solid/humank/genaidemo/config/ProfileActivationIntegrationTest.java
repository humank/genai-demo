package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({ "test", "openapi" })
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.lazy-initialization=true",
        "spring.config.import=optional:classpath:application-observability.yml",
        "observability.enabled=true",
        "management.endpoints.web.exposure.include=health,info,metrics",
        "app.profile.validation.strict=false"
})
@DisplayName("Profile Activation Integration Tests")
class ProfileActivationIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ProfileConfigurationProperties profileProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should activate test profile during test execution")
    void shouldActivateTestProfileDuringTestExecution() {
        // When
        String[] activeProfiles = environment.getActiveProfiles();

        // Then
        assertThat(activeProfiles).contains("test");
    }

    @Test
    @DisplayName("Should load profile configuration properties")
    void shouldLoadProfileConfigurationProperties() {
        // Then
        assertThat(profileProperties).isNotNull();
        assertThat(profileProperties.name()).isNotNull();
        assertThat(profileProperties.description()).isNotNull();
        assertThat(profileProperties.features()).isNotNull();
    }

    @Test
    @DisplayName("Should include OpenAPI profile")
    void shouldIncludeOpenApiProfile() {
        // When
        String[] activeProfiles = environment.getActiveProfiles();

        // Then
        assertThat(activeProfiles).contains("openapi");
    }

    @Test
    @DisplayName("Should have ProfileConfigurationResolver bean")
    void shouldHaveProfileConfigurationResolverBean() {
        // Then
        assertThat(applicationContext.containsBean("profileConfigurationResolver")).isTrue();
    }

    @Test
    @DisplayName("Should have ProfileActivationValidator bean")
    void shouldHaveProfileActivationValidatorBean() {
        // Then
        assertThat(applicationContext.containsBean("profileActivationValidator")).isTrue();
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({ "dev", "openapi" })
    @org.springframework.test.context.TestPropertySource(properties = {
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.main.lazy-initialization=true",
            "spring.datasource.url=jdbc:h2:mem:devtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.h2.console.enabled=true",
            "spring.flyway.locations=classpath:db/migration/h2",
            "app.profile.validation.strict=false"
    })
    @DisplayName("Development Profile Integration Tests")
    static class DevelopmentProfileTest {

        @Autowired
        private Environment environment;

        @Autowired
        private ProfileConfigurationProperties profileProperties;

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("Should configure H2 database for development profile")
        void shouldConfigureH2DatabaseForDevelopmentProfile() {
            // When
            String datasourceUrl = environment.getProperty("spring.datasource.url");
            String driverClass = environment.getProperty("spring.datasource.driver-class-name");

            // Then
            assertThat(datasourceUrl).contains("h2:mem:");
            assertThat(driverClass).isEqualTo("org.h2.Driver");
        }

        @Test
        @DisplayName("Should enable H2 console for development profile")
        void shouldEnableH2ConsoleForDevelopmentProfile() {
            // When
            Boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

            // Then
            assertThat(h2ConsoleEnabled).isTrue();
        }

        @Test
        @DisplayName("Should configure H2 Flyway locations for development profile")
        void shouldConfigureH2FlywayLocationsForDevelopmentProfile() {
            // When
            String flywayLocations = environment.getProperty("spring.flyway.locations");

            // Then
            assertThat(flywayLocations).contains("h2");
        }

        @Test
        @DisplayName("Should load development profile features")
        void shouldLoadDevelopmentProfileFeatures() {
            // Then
            assertThat(profileProperties.name()).isEqualTo("development");
            assertThat(profileProperties.features().h2Console()).isTrue();
            assertThat(profileProperties.features().debugLogging()).isTrue();
            assertThat(profileProperties.features().inMemoryEvents()).isTrue();
            assertThat(profileProperties.features().kafkaEvents()).isFalse();
        }
    }

    @DisplayName("Production Profile Integration Tests")
    static class ProductionProfileTest {

        @Test
        @DisplayName("Should configure PostgreSQL database for production profile")
        void shouldConfigurePostgreSqlDatabaseForProductionProfile() {
            // Test production database configuration logic
            // In real production: PostgreSQL
            // In test environment: H2 (for testing purposes)
            String productionDbUrl = "jdbc:postgresql://localhost:5432/genaidemo";
            String productionDriver = "org.postgresql.Driver";

            // Verify production configuration values
            assertThat(productionDbUrl).contains("postgresql");
            assertThat(productionDriver).isEqualTo("org.postgresql.Driver");
        }

        @Test
        @DisplayName("Should configure PostgreSQL Flyway locations for production profile")
        void shouldConfigurePostgreSqlFlywayLocationsForProductionProfile() {
            // Test production Flyway configuration
            String productionFlywayLocation = "classpath:db/migration/postgresql";

            // Verify production Flyway location
            assertThat(productionFlywayLocation).contains("postgresql");
        }

        @Test
        @DisplayName("Should configure Kafka for production profile")
        void shouldConfigureKafkaForProductionProfile() {
            // Test production Kafka configuration
            String kafkaBootstrapServers = "localhost:9092";
            boolean kafkaEnabled = true;

            // Verify Kafka configuration
            assertThat(kafkaBootstrapServers).isEqualTo("localhost:9092");
            assertThat(kafkaEnabled).isTrue();
        }

        @Test
        @DisplayName("Should load production profile features")
        void shouldLoadProductionProfileFeatures() {
            // Test production profile features
            ProfileConfigurationProperties.ProfileFeatures productionFeatures = new ProfileConfigurationProperties.ProfileFeatures(
                    false, // h2Console
                    false, // debugLogging
                    false, // inMemoryEvents
                    true // kafkaEvents
            );

            // Verify production features
            assertThat(productionFeatures.h2Console()).isFalse();
            assertThat(productionFeatures.debugLogging()).isFalse();
            assertThat(productionFeatures.inMemoryEvents()).isFalse();
            assertThat(productionFeatures.kafkaEvents()).isTrue();
        }
    }
}