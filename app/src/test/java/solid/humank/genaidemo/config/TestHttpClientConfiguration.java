package solid.humank.genaidemo.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 統一的測試 HTTP 客戶端配置
 * 
 * 為所有測試提供統一的 Apache HttpComponents Client 5.x 配置，
 * 解決 NoClassDefFoundError 問題並確保測試環境的一致性。
 */
@TestConfiguration
public class TestHttpClientConfiguration {

        /**
         * 創建統一的測試 HTTP 客戶端，使用 Apache HttpComponents Client 5.x
         * 配置寬鬆的 SSL 設定和合理的連接池參數
         */
        @Bean
        @Primary
        public CloseableHttpClient unifiedTestHttpClient()
                        throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

                // 創建信任所有證書的 SSL 上下文（僅用於測試）
                SSLContext sslContext = SSLContextBuilder.create()
                                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                .build();

                // 創建 SSL 連接工廠
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

                // 創建連接管理器，優化測試環境性能
                HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(sslSocketFactory)
                                .setMaxConnTotal(50) // 減少最大連接數以節省資源
                                .setMaxConnPerRoute(10) // 每個路由的最大連接數
                                .build();

                // 創建請求配置，設定合理的超時時間
                RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                                .setResponseTimeout(Timeout.ofSeconds(10))
                                .build();

                // 構建統一的 HTTP 客戶端
                return HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .setDefaultRequestConfig(requestConfig)
                                .build();
        }

        /**
         * 創建統一的 RestTemplate，使用統一的 HTTP 客戶端
         */
        @Bean
        @Primary
        public RestTemplate unifiedTestRestTemplate(CloseableHttpClient unifiedTestHttpClient) {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
                                unifiedTestHttpClient);

                // 設定連接和讀取超時
                factory.setConnectTimeout(Duration.ofSeconds(5));
                factory.setConnectionRequestTimeout(Duration.ofSeconds(5));

                return new RestTemplateBuilder()
                                .requestFactory(() -> factory)
                                .build();
        }

        /**
         * 創建統一的 TestRestTemplate，使用統一的 RestTemplateBuilder
         */
        @Bean
        @Primary
        public TestRestTemplate unifiedTestRestTemplate(RestTemplateBuilder restTemplateBuilder) {
                return new TestRestTemplate(restTemplateBuilder);
        }
}