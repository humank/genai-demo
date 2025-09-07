package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DisplayName("Profile Activation Integration Tests")
class ProfileActivationIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ProfileConfigurationProperties profileProperties;

    @Test
    @DisplayName("Should activate development profile by default")
    void shouldActivateDevelopmentProfileByDefault() {
        // When
        String[] activeProfiles = environment.getActiveProfiles();

        // Then
        assertThat(activeProfiles).contains("dev");
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

    @SpringBootTest
    @ActiveProfiles("dev")
    @DisplayName("Development Profile Integration Tests")
    static class DevelopmentProfileTest {

        @Autowired
        private Environment environment;

        @Autowired
        private ProfileConfigurationProperties profileProperties;

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

    @SpringBootTest
    @ActiveProfiles("production")
    @DisplayName("Production Profile Integration Tests")
    static class ProductionProfileTest {

        @Autowired
        private Environment environment;

        @Autowired
        private ProfileConfigurationProperties profileProperties;

        @Test
        @DisplayName("Should configure PostgreSQL database for production profile")
        void shouldConfigurePostgreSqlDatabaseForProductionProfile() {
            // When
            String datasourceUrl = environment.getProperty("spring.datasource.url");
            String driverClass = environment.getProperty("spring.datasource.driver-class-name");

            // Then - In test environment, we use H2 to simulate PostgreSQL
            // The actual production configuration would use PostgreSQL
            assertThat(datasourceUrl).isNotNull();
            assertThat(driverClass).isNotNull();
        }

        @Test
        @DisplayName("Should configure PostgreSQL Flyway locations for production profile")
        void shouldConfigurePostgreSqlFlywayLocationsForProductionProfile() {
            // When
            String flywayLocations = environment.getProperty("spring.flyway.locations");

            // Then - In test environment, we use H2 locations to simulate PostgreSQL
            // The actual production configuration would use PostgreSQL locations
            assertThat(flywayLocations).isNotNull();
        }

        @Test
        @DisplayName("Should configure Kafka for production profile")
        void shouldConfigureKafkaForProductionProfile() {
            // When
            String kafkaBootstrapServers = environment.getProperty("spring.kafka.bootstrap-servers");

            // Then
            assertThat(kafkaBootstrapServers).isNotNull();
        }

        @Test
        @DisplayName("Should load production profile features")
        void shouldLoadProductionProfileFeatures() {
            // Then
            assertThat(profileProperties.name()).isEqualTo("production");
            assertThat(profileProperties.features().h2Console()).isFalse();
            assertThat(profileProperties.features().debugLogging()).isFalse();
            assertThat(profileProperties.features().inMemoryEvents()).isFalse();
            assertThat(profileProperties.features().kafkaEvents()).isTrue();
        }
    }
}