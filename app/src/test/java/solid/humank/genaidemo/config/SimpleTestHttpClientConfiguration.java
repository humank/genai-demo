package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 簡化的測試 HTTP 客戶端配置
 * 
 * 使用 Spring 內建的 SimpleClientHttpRequestFactory 避免 HttpComponents 依賴問題
 */
@TestConfiguration
public class SimpleTestHttpClientConfiguration {

    /**
     * 創建簡單的 RestTemplate，使用 SimpleClientHttpRequestFactory
     */
    @Bean
    @Primary
    public RestTemplate simpleTestRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return new RestTemplateBuilder()
                .requestFactory(() -> factory)
                .build();
    }

    /**
     * 創建簡單的 TestRestTemplate，直接使用 SimpleClientHttpRequestFactory
     */
    @Bean
    @Primary
    public TestRestTemplate simpleTestRestTemplateForTesting() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        RestTemplateBuilder builder = new RestTemplateBuilder()
                .requestFactory(() -> factory);

        return new TestRestTemplate(builder);
    }
}