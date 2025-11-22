package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for AWS Secrets Manager
 * 
 * This configuration is intentionally minimal for test profile.
 * Tests should not depend on AWS Secrets Manager.
 * 
 * If specific tests need secrets, they should mock them directly.
 */
@TestConfiguration
@Profile("test")
public class TestSecretsManagerConfiguration {
    // Intentionally empty - tests should not depend on Secrets Manager
    // If needed, individual tests can provide their own mocks
}
