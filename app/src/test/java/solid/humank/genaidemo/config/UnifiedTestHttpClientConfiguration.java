package solid.humank.genaidemo.config;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Unified Test HTTP Client Configuration
 * 
 * Provides a single, authoritative HTTP client configuration for all tests
 * using SimpleClientHttpRequestFactory.
 * This is the recommended approach for Spring Boot tests as it:
 * 1. Uses Spring Boot's built-in HTTP client (no external dependencies)
 * 2. Has maximum compatibility with TestRestTemplate
 * 3. Avoids HttpComponents version conflicts
 * 4. Provides reliable performance for integration tests
 */
@TestConfiguration
@Profile("test")
public class UnifiedTestHttpClientConfiguration {

    /**
     * Creates a properly configured ClientHttpRequestFactory using
     * SimpleClientHttpRequestFactory
     * This is Spring Boot's recommended approach for test environments
     */
    @Bean
    @Primary
    public ClientHttpRequestFactory testClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    /**
     * Creates a unified RestTemplate for test use with
     * SimpleClientHttpRequestFactory
     */
    @Bean
    @Primary
    public RestTemplate testRestTemplate(ClientHttpRequestFactory testClientHttpRequestFactory) {
        return new RestTemplateBuilder()
                .requestFactory(() -> testClientHttpRequestFactory)
                .build();
    }

    /**
     * Creates a unified TestRestTemplate for integration tests with
     * SimpleClientHttpRequestFactory
     */
    @Bean
    @Primary
    public TestRestTemplate testRestTemplateForTesting(ClientHttpRequestFactory testClientHttpRequestFactory) {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .requestFactory(() -> testClientHttpRequestFactory);

        return new TestRestTemplate(builder);
    }

    /**
     * Configuration validator to ensure proper setup
     */
    @Bean
    public TestHttpClientValidator testHttpClientValidator() {
        return new TestHttpClientValidator();
    }

    /**
     * Validator class to verify HTTP client configuration
     */
    public static class TestHttpClientValidator {

        public boolean validateConfiguration(ClientHttpRequestFactory factory) {
            return factory instanceof SimpleClientHttpRequestFactory;
        }

        public String getFactoryType(ClientHttpRequestFactory factory) {
            return factory.getClass().getSimpleName();
        }

        public String getHttpClientVersion() {
            return "SimpleClientHttpRequestFactory (Spring Boot Recommended)";
        }
    }
}