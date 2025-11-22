package solid.humank.genaidemo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import solid.humank.genaidemo.config.TestApplicationConfiguration;

/**
 * Base class for integration tests
 * 
 * This class provides common configuration for all integration tests,
 * including test-specific bean configurations and profile settings.
 * 
 * Extend this class for tests that need full Spring context.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
public abstract class BaseIntegrationTest {
    // Common test setup can be added here
}
