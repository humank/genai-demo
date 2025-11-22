package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test DataSource configuration
 * 
 * This configuration is intentionally minimal for test profile.
 * The test profile uses application-test.yml configuration which
 * already sets up H2 in-memory database.
 * 
 * We don't need to override DataSource here as Spring Boot's
 * auto-configuration will handle it based on application-test.yml.
 */
@TestConfiguration
@Profile("test")
public class TestDataSourceConfiguration {
    // Intentionally empty - rely on application-test.yml configuration
}
