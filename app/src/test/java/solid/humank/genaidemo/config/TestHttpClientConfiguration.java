package solid.humank.genaidemo.config;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 統一的測試 HTTP 客戶端配置
 * 
 * 使用簡化的 HTTP 客戶端配置，避免 Apache HttpComponents 依賴問題
 * 確保測試環境的穩定性和一致性。
 */
@TestConfiguration
public class TestHttpClientConfiguration {

        /**
         * 創建統一的 RestTemplate，使用簡單的 HTTP 客戶端工廠
         */
        @Bean
        @Primary
        public RestTemplate unifiedTestRestTemplate() {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

                // 設定連接和讀取超時
                factory.setConnectTimeout(Duration.ofSeconds(5));
                factory.setReadTimeout(Duration.ofSeconds(10));

                return new RestTemplateBuilder()
                                .requestFactory(() -> factory)
                                .build();
        }

        /**
         * 創建統一的 TestRestTemplate，使用統一的 RestTemplateBuilder
         */
        @Bean
        @Primary
        public TestRestTemplate unifiedTestRestTemplateForTesting() {
                // 強制使用 SimpleClientHttpRequestFactory 避免 HttpComponents 依賴問題
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(Duration.ofSeconds(5));
                factory.setReadTimeout(Duration.ofSeconds(10));

                RestTemplateBuilder builder = new RestTemplateBuilder()
                                .requestFactory(() -> factory);

                return new TestRestTemplate(builder);
        }
}