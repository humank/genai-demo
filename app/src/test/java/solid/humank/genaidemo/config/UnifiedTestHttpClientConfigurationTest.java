package solid.humank.genaidemo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

/**
 * Test to validate the UnifiedTestHttpClientConfiguration works correctly with
 * SimpleClientHttpRequestFactory
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(UnifiedTestHttpClientConfiguration.class)
class UnifiedTestHttpClientConfigurationTest {

    @Autowired
    private RestTemplate testRestTemplate;

    @Autowired
    private TestRestTemplate testRestTemplateForTesting;

    @Autowired
    private ClientHttpRequestFactory testClientHttpRequestFactory;

    @Autowired
    private UnifiedTestHttpClientConfiguration.TestHttpClientValidator validator;

    @Test
    @DisplayName("Should create RestTemplate with SimpleClientHttpRequestFactory")
    void shouldCreateRestTemplateWithSimpleClientHttpRequestFactory() {
        assertThat(testRestTemplate).isNotNull();

        // Verify the RestTemplate is using SimpleClientHttpRequestFactory
        ClientHttpRequestFactory factory = testRestTemplate.getRequestFactory();
        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
    }

    @Test
    @DisplayName("Should create TestRestTemplate with SimpleClientHttpRequestFactory")
    void shouldCreateTestRestTemplateWithSimpleClientHttpRequestFactory() {
        assertThat(testRestTemplateForTesting).isNotNull();

        // Verify the TestRestTemplate is using SimpleClientHttpRequestFactory
        RestTemplate restTemplate = testRestTemplateForTesting.getRestTemplate();
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
    }

    @Test
    @DisplayName("Should create SimpleClientHttpRequestFactory")
    void shouldCreateSimpleClientHttpRequestFactory() {
        assertThat(testClientHttpRequestFactory).isNotNull();
        assertThat(testClientHttpRequestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
    }

    @Test
    @DisplayName("Should validate configuration correctly")
    void shouldValidateConfigurationCorrectly() {
        assertThat(validator).isNotNull();

        boolean isValid = validator.validateConfiguration(testClientHttpRequestFactory);
        assertThat(isValid).isTrue();

        String factoryType = validator.getFactoryType(testClientHttpRequestFactory);
        assertThat(factoryType).isEqualTo("SimpleClientHttpRequestFactory");

        String httpClientVersion = validator.getHttpClientVersion();
        assertThat(httpClientVersion).isEqualTo("SimpleClientHttpRequestFactory (Spring Boot Recommended)");
    }
}