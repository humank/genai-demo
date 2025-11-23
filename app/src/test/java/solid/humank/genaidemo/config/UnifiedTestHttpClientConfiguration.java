package solid.humank.genaidemo.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * Unified HTTP Client Configuration for Tests
 * 
 * Provides a single, authoritative HTTP client configuration for all tests.
 * This configuration supports both SimpleClientHttpRequestFactory (default) and
 * HttpComponents5 (optional) with proper error handling and conflict
 * prevention.
 * 
 * Key features:
 * 1. Uses @ConditionalOnMissingBean to prevent Bean conflicts
 * 2. Provides @Primary TestRestTemplate with proper error handling
 * 3. Supports both simple and HttpComponents5 client factories
 * 4. Includes comprehensive error handling and logging
 * 5. Validates configuration to ensure proper setup
 */
@TestConfiguration
@Profile("test")
@ConditionalOnClass({ TestRestTemplate.class, RestTemplate.class })
public class UnifiedTestHttpClientConfiguration {    private static final Logger logger = LoggerFactory.getLogger(UnifiedTestHttpClientConfiguration.class);

    /**
     * Creates a properly configured ClientHttpRequestFactory.
     * Uses SimpleClientHttpRequestFactory by default for maximum compatibility.
     * Can be configured to use HttpComponents5 via property.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ClientHttpRequestFactory.class)
    public ClientHttpRequestFactory testClientHttpRequestFactory() {
        try {
            // Try to use HttpComponents5 if available and enabled
            if (isHttpComponents5Available() && isHttpComponents5Enabled()) {
                logger.info("Creating HttpComponents5 ClientHttpRequestFactory for tests");
                return createHttpComponents5Factory();
            } else {
                logger.info("Creating SimpleClientHttpRequestFactory for tests (default)");
                return createSimpleClientHttpRequestFactory();
            }
        } catch (Exception e) {
            logger.warn(
                    "Failed to create preferred HTTP client factory, falling back to SimpleClientHttpRequestFactory",
                    e);
            return createSimpleClientHttpRequestFactory();
        }
    }

    /**
     * Creates a unified RestTemplate for test use with proper error handling
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "testRestTemplate")
    public RestTemplate testRestTemplate(ClientHttpRequestFactory testClientHttpRequestFactory) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(testClientHttpRequestFactory);

            logger.debug("Created RestTemplate with factory: {}",
                    testClientHttpRequestFactory.getClass().getSimpleName());

            return restTemplate;
        } catch (Exception e) {
            logger.error("Failed to create RestTemplate", e);
            throw new IllegalStateException("Unable to create RestTemplate for tests", e);
        }
    }

    /**
     * Creates a unified TestRestTemplate for integration tests with proper error
     * handling
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(TestRestTemplate.class)
    public TestRestTemplate testRestTemplateForTesting(ClientHttpRequestFactory testClientHttpRequestFactory) {
        try {
            RestTemplateBuilder builder = new RestTemplateBuilder()
                    .requestFactory(() -> testClientHttpRequestFactory)
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(30));

            TestRestTemplate testRestTemplate = new TestRestTemplate(builder);

            logger.debug("Created TestRestTemplate with factory: {}",
                    testClientHttpRequestFactory.getClass().getSimpleName());

            return testRestTemplate;
        } catch (Exception e) {
            logger.error("Failed to create TestRestTemplate", e);
            throw new IllegalStateException("Unable to create TestRestTemplate for tests", e);
        }
    }

    /**
     * Configuration validator to ensure proper setup
     */
    @Bean
    @ConditionalOnMissingBean(TestHttpClientValidator.class)
    public TestHttpClientValidator testHttpClientValidator() {
        return new TestHttpClientValidator();
    }

    /**
     * Creates SimpleClientHttpRequestFactory with optimized settings for tests
     */
    private SimpleClientHttpRequestFactory createSimpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }

    /**
     * Creates HttpComponents5 ClientHttpRequestFactory if available
     */
    private ClientHttpRequestFactory createHttpComponents5Factory() {
        try {
            Class<?> factoryClass = Class
                    .forName("org.springframework.http.client.HttpComponentsClientHttpRequestFactory");
            Object factory = factoryClass.getDeclaredConstructor().newInstance();

            // Set timeouts using reflection
            factoryClass.getMethod("setConnectTimeout", Duration.class).invoke(factory, Duration.ofSeconds(10));
            factoryClass.getMethod("setReadTimeout", Duration.class).invoke(factory, Duration.ofSeconds(30));

            return (ClientHttpRequestFactory) factory;
        } catch (Exception e) {
            logger.warn("HttpComponents5 not available or failed to configure, using SimpleClientHttpRequestFactory",
                    e);
            return createSimpleClientHttpRequestFactory();
        }
    }

    /**
     * Checks if HttpComponents5 is available on the classpath
     */
    private boolean isHttpComponents5Available() {
        try {
            Class.forName("org.apache.hc.client5.http.impl.classic.CloseableHttpClient");
            Class.forName("org.springframework.http.client.HttpComponentsClientHttpRequestFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if HttpComponents5 is enabled via configuration
     */
    private boolean isHttpComponents5Enabled() {
        String property = System.getProperty("test.http.client.type", "simple");
        return "httpcomponents5".equalsIgnoreCase(property);
    }

    /**
     * Validator class to verify HTTP client configuration
     */
    public static class TestHttpClientValidator {        private static final Logger logger = LoggerFactory.getLogger(TestHttpClientValidator.class);

        public boolean validateConfiguration(ClientHttpRequestFactory factory) {
            if (factory == null) {
                logger.error("ClientHttpRequestFactory is null");
                return false;
            }

            boolean isValid = factory instanceof SimpleClientHttpRequestFactory ||
                    factory.getClass().getName().contains("HttpComponentsClientHttpRequestFactory");

            if (!isValid) {
                logger.warn("Unexpected ClientHttpRequestFactory type: {}", factory.getClass().getName());
            }

            return isValid;
        }

        public String getFactoryType(ClientHttpRequestFactory factory) {
            return factory != null ? factory.getClass().getSimpleName() : "null";
        }

        public String getHttpClientVersion() {
            if (isHttpComponents5Available()) {
                return "HttpComponents5 available, SimpleClientHttpRequestFactory default";
            } else {
                return "SimpleClientHttpRequestFactory only";
            }
        }

        private boolean isHttpComponents5Available() {
            try {
                Class.forName("org.apache.hc.client5.http.impl.classic.CloseableHttpClient");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        /**
         * Validates that TestRestTemplate can make HTTP requests without errors
         */
        public boolean validateTestRestTemplateConnectivity(TestRestTemplate testRestTemplate, String testUrl) {
            try {
                testRestTemplate.getForEntity(testUrl, String.class);
                return true;
            } catch (Exception e) {
                logger.error("TestRestTemplate connectivity validation failed for URL: {}", testUrl, e);
                return false;
            }
        }
    }
}