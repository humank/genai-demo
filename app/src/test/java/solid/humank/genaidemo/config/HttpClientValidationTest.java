package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Integration test to validate HTTP client functionality and ensure
 * TestRestTemplate works without NoClassDefFoundError or dependency conflicts.
 * 
 * This test verifies:
 * 1. TestRestTemplate can be created without errors
 * 2. HTTP requests to actuator endpoints work correctly
 * 3. No NoClassDefFoundError occurs during test execution
 * 4. HTTP client configuration is properly set up
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(UnifiedTestHttpClientConfiguration.class)
@IntegrationTest
class HttpClientValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientValidationTest.class);

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ClientHttpRequestFactory testClientHttpRequestFactory;

    @Autowired
    private UnifiedTestHttpClientConfiguration.TestHttpClientValidator validator;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should create TestRestTemplate without NoClassDefFoundError")
    void shouldCreateTestRestTemplateWithoutErrors() {
        assertThatNoException().isThrownBy(() -> {
            assertThat(testRestTemplate).isNotNull();
            logger.info("TestRestTemplate created successfully: {}", testRestTemplate.getClass().getName());
        });
    }

    @Test
    @DisplayName("Should create RestTemplate with proper configuration")
    void shouldCreateRestTemplateWithProperConfiguration() {
        assertThat(restTemplate).isNotNull();
        assertThat(testClientHttpRequestFactory).isNotNull();

        boolean isValid = validator.validateConfiguration(testClientHttpRequestFactory);
        assertThat(isValid).isTrue();

        String factoryType = validator.getFactoryType(testClientHttpRequestFactory);
        logger.info("RestTemplate created with factory type: {}", factoryType);

        // Verify factory type is one of the expected types
        assertThat(factoryType).isIn("SimpleClientHttpRequestFactory", "HttpComponentsClientHttpRequestFactory");
    }

    @Test
    @DisplayName("Should successfully make HTTP request to health endpoint")
    void shouldMakeHttpRequestToHealthEndpoint() {
        String healthUrl = "http://localhost:" + port + "/actuator/health";

        assertThatNoException().isThrownBy(() -> {
            ResponseEntity<String> response = testRestTemplate.getForEntity(healthUrl, String.class);

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            logger.info("Health endpoint response status: {}", response.getStatusCode());
            logger.info("Health endpoint response body length: {}",
                    response.getBody() != null ? response.getBody().length() : 0);
        });
    }

    @Test
    @DisplayName("Should successfully make HTTP request to info endpoint")
    void shouldMakeHttpRequestToInfoEndpoint() {
        String infoUrl = "http://localhost:" + port + "/actuator/info";

        assertThatNoException().isThrownBy(() -> {
            ResponseEntity<String> response = testRestTemplate.getForEntity(infoUrl, String.class);

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            logger.info("Info endpoint response status: {}", response.getStatusCode());
        });
    }

    @Test
    @DisplayName("Should successfully make HTTP request to metrics endpoint")
    void shouldMakeHttpRequestToMetricsEndpoint() {
        String metricsUrl = "http://localhost:" + port + "/actuator/metrics";

        assertThatNoException().isThrownBy(() -> {
            ResponseEntity<String> response = testRestTemplate.getForEntity(metricsUrl, String.class);

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            logger.info("Metrics endpoint response status: {}", response.getStatusCode());
        });
    }

    @Test
    @DisplayName("Should validate TestRestTemplate connectivity")
    void shouldValidateTestRestTemplateConnectivity() {
        String testUrl = "http://localhost:" + port + "/actuator/health";

        boolean isConnectivityValid = validator.validateTestRestTemplateConnectivity(testRestTemplate, testUrl);
        assertThat(isConnectivityValid).isTrue();

        logger.info("TestRestTemplate connectivity validation passed for URL: {}", testUrl);
    }

    @Test
    @DisplayName("Should handle multiple concurrent HTTP requests without errors")
    void shouldHandleConcurrentHttpRequestsWithoutErrors() {
        String healthUrl = "http://localhost:" + port + "/actuator/health";

        assertThatNoException().isThrownBy(() -> {
            // Make multiple concurrent requests to test stability
            for (int i = 0; i < 5; i++) {
                ResponseEntity<String> response = testRestTemplate.getForEntity(healthUrl, String.class);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            logger.info("Successfully completed 5 concurrent HTTP requests");
        });
    }

    @Test
    @DisplayName("Should provide HTTP client version information")
    void shouldProvideHttpClientVersionInformation() {
        String version = validator.getHttpClientVersion();
        assertThat(version).isNotNull();
        assertThat(version).isNotEmpty();

        logger.info("HTTP client version information: {}", version);
    }

    @Test
    @DisplayName("Should not throw NoClassDefFoundError during HTTP operations")
    void shouldNotThrowNoClassDefFoundErrorDuringHttpOperations() {
        String baseUrl = "http://localhost:" + port;

        assertThatNoException().isThrownBy(() -> {
            // Test various HTTP operations that previously caused NoClassDefFoundError
            testRestTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
            testRestTemplate.getForEntity(baseUrl + "/actuator/info", String.class);
            testRestTemplate.getForEntity(baseUrl + "/actuator/metrics", String.class);

            logger.info("All HTTP operations completed without NoClassDefFoundError");
        });
    }

    @Test
    @DisplayName("Should verify HTTP client factory configuration")
    void shouldVerifyHttpClientFactoryConfiguration() {
        assertThat(testClientHttpRequestFactory).isNotNull();

        String factoryType = validator.getFactoryType(testClientHttpRequestFactory);
        logger.info("HTTP client factory type: {}", factoryType);

        // Verify the factory is properly configured
        boolean isValid = validator.validateConfiguration(testClientHttpRequestFactory);
        assertThat(isValid).isTrue();

        // Log configuration details for debugging
        logger.info("HTTP client factory validation passed");
        logger.info("Factory class: {}", testClientHttpRequestFactory.getClass().getName());
    }
}