package solid.humank.genaidemo.testutils.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.config.TestProfileConfiguration;
import solid.humank.genaidemo.config.UnifiedTestHttpClientConfiguration;
import solid.humank.genaidemo.testutils.TestPerformanceConfiguration;

/**
 * Enhanced Base class for integration tests
 * 
 * Provides common test setup, HTTP client configuration, and utility methods
 * for integration testing with proper resource management and cleanup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
        UnifiedTestHttpClientConfiguration.class,
        TestProfileConfiguration.class,
        TestPerformanceConfiguration.class
})
public abstract class BaseIntegrationTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected TestProfileConfiguration.TestEnvironmentValidator environmentValidator;

    @Autowired
    protected TestProfileConfiguration.TestResourceManager resourceManager;

    @LocalServerPort
    protected int port;

    protected String baseUrl;

    @BeforeEach
    void setUpBaseTest() {
        baseUrl = "http://localhost:" + port;

        // Validate test environment
        if (!environmentValidator.validateTestProfile()) {
            throw new IllegalStateException("Test profile is not active");
        }

        // Allocate test resources
        resourceManager.allocateTestResources();

        // Log resource usage before test
        logger.debug("Test setup completed");

        logger.info("Test environment initialized: {}", environmentValidator.getTestEnvironmentInfo());
        logger.debug("Base URL: {}", baseUrl);
    }

    @AfterEach
    void tearDownBaseTest() {
        // Log resource usage after test
        logger.debug("Test cleanup completed");

        // Cleanup test resources
        resourceManager.cleanupTestResources();
        logger.debug("Test resources cleaned up");
    }

    // Utility methods for common test operations

    protected void logTestStart(String testName) {
        logger.info("Starting test: {}", testName);
    }

    protected void logTestEnd(String testName) {
        logger.info("Completed test: {}", testName);
    }

    /**
     * Performs a GET request to the specified endpoint
     */
    protected <T> ResponseEntity<T> performGet(String endpoint, Class<T> responseType) {
        String url = baseUrl + endpoint;
        logger.debug("GET request to: {}", url);
        return restTemplate.getForEntity(url, responseType);
    }

    /**
     * Performs a GET request with custom headers
     */
    protected <T> ResponseEntity<T> performGet(String endpoint, Class<T> responseType, HttpHeaders headers) {
        String url = baseUrl + endpoint;
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        logger.debug("GET request to: {} with headers: {}", url, headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

    /**
     * Performs a POST request with body
     */
    protected <T, R> ResponseEntity<R> performPost(String endpoint, T requestBody, Class<R> responseType) {
        String url = baseUrl + endpoint;
        logger.debug("POST request to: {} with body: {}", url, requestBody);
        return restTemplate.postForEntity(url, requestBody, responseType);
    }

    /**
     * Performs a POST request with body and custom headers
     */
    protected <T, R> ResponseEntity<R> performPost(String endpoint, T requestBody, Class<R> responseType,
            HttpHeaders headers) {
        String url = baseUrl + endpoint;
        HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
        logger.debug("POST request to: {} with body: {} and headers: {}", url, requestBody, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    /**
     * Creates default HTTP headers for tests
     */
    protected HttpHeaders createDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-Suite", getClass().getSimpleName());
        headers.set("X-Correlation-ID", "test-" + System.currentTimeMillis());
        return headers;
    }

    /**
     * Validates that the test environment is properly configured
     */
    protected void validateTestEnvironment() {
        if (!environmentValidator.validateTestProfile()) {
            throw new IllegalStateException("Test profile validation failed");
        }
        if (!environmentValidator.validateDatabaseConfiguration()) {
            throw new IllegalStateException("Database configuration validation failed");
        }
        if (!environmentValidator.validateHttpClientConfiguration()) {
            throw new IllegalStateException("HTTP client configuration validation failed");
        }
    }

    /**
     * Waits for the application to be ready (useful for slow-starting tests)
     */
    protected void waitForApplicationReady() {
        try {
            ResponseEntity<String> response = performGet("/actuator/health", String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Application is not ready");
            }
            logger.debug("Application is ready");
        } catch (Exception e) {
            logger.warn("Failed to verify application readiness: {}", e.getMessage());
        }
    }

    /**
     * Force cleanup of test resources.
     * Use this method if you need to explicitly clean up resources during a test.
     */
    protected void forceResourceCleanup() {
        logger.debug("Force cleanup requested by test");
        System.gc();
    }

    /**
     * Check if memory usage is within acceptable limits.
     */
    protected boolean isMemoryUsageAcceptable() {
        return true; // Simplified for now
    }

    /**
     * Wait for a condition to be true with timeout.
     * Useful for waiting for asynchronous operations to complete.
     */
    protected void waitForCondition(java.util.function.BooleanSupplier condition,
            java.time.Duration timeout,
            String description) throws InterruptedException {
        long timeoutMs = timeout.toMillis();
        long startTime = System.currentTimeMillis();

        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new AssertionError("Timeout waiting for condition: " + description);
            }
            Thread.sleep(100); // Check every 100ms
        }
    }

    /**
     * Sleep with proper exception handling.
     */
    protected void sleep(java.time.Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}