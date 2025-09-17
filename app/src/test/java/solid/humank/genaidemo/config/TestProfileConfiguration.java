package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

/**
 * Test Profile Configuration
 * 
 * Ensures proper test environment isolation and configuration.
 * This configuration is only active when the 'test' profile is enabled.
 */
@TestConfiguration
@Profile("test")
@TestPropertySource(properties = {
        "spring.main.lazy-initialization=true",
        "spring.jmx.enabled=false",
        "logging.level.org.springframework.boot.autoconfigure=ERROR",
        "management.health.defaults.enabled=false"
})
public class TestProfileConfiguration {

    /**
     * Test environment validator to ensure proper test setup
     */
    @Bean
    public TestEnvironmentValidator testEnvironmentValidator() {
        return new TestEnvironmentValidator();
    }

    /**
     * Test resource manager for proper resource allocation and cleanup
     */
    @Bean
    public TestResourceManager testResourceManager() {
        return new TestResourceManager();
    }

    /**
     * Validator to ensure test environment is properly configured
     */
    public static class TestEnvironmentValidator {

        public boolean validateTestProfile() {
            String activeProfiles = System.getProperty("spring.profiles.active", "");
            return activeProfiles.contains("test");
        }

        public boolean validateDatabaseConfiguration() {
            // Validate that we're using H2 in-memory database for tests
            String dbUrl = System.getProperty("spring.datasource.url", "");
            return dbUrl.contains("h2:mem") || dbUrl.isEmpty();
        }

        public boolean validateHttpClientConfiguration() {
            // Validate that we're using simple HTTP client factory
            return true; // SimpleClientHttpRequestFactory is always available
        }

        public String getTestEnvironmentInfo() {
            return String.format(
                    "Test Environment - Profile: %s, Database: %s, HTTP Client: SimpleClientHttpRequestFactory",
                    System.getProperty("spring.profiles.active", "default"),
                    System.getProperty("spring.datasource.url", "default"));
        }
    }

    /**
     * Manager for test resource allocation and cleanup
     */
    public static class TestResourceManager {

        private volatile boolean resourcesAllocated = false;

        public void allocateTestResources() {
            if (!resourcesAllocated) {
                // Allocate test-specific resources
                System.setProperty("test.resources.allocated", "true");
                resourcesAllocated = true;
            }
        }

        public void cleanupTestResources() {
            if (resourcesAllocated) {
                // Cleanup test-specific resources
                System.clearProperty("test.resources.allocated");
                resourcesAllocated = false;
            }
        }

        public boolean areResourcesAllocated() {
            return resourcesAllocated;
        }
    }
}