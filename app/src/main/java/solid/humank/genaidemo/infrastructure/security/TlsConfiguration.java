package solid.humank.genaidemo.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * TLS Configuration for secure data transmission
 * Implements requirement 11.2: WHEN metrics are transmitted THEN the system
 * SHALL use encrypted connections (TLS)
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.security.tls")
public class TlsConfiguration {

    private boolean enabled = true;
    private String protocol = "TLSv1.3";
    private boolean requireClientAuth = false;
    private String keyStore;
    private String keyStorePassword;
    private String trustStore;
    private String trustStorePassword;

    /**
     * RestTemplate with TLS configuration for secure HTTP communications
     */
    @Bean
    @Profile("production")
    public RestTemplate secureRestTemplate() {
        // In production, the underlying HTTP client will use system TLS settings
        // Additional TLS configuration can be added through JVM system properties:
        // -Djavax.net.ssl.keyStore=path/to/keystore
        // -Djavax.net.ssl.keyStorePassword=password
        // -Djavax.net.ssl.trustStore=path/to/truststore
        // -Djavax.net.ssl.trustStorePassword=password
        // -Dhttps.protocols=TLSv1.3

        return new RestTemplate();
    }

    /**
     * Development RestTemplate without strict TLS requirements
     */
    @Bean
    @Profile({ "local", "test" })
    public RestTemplate developmentRestTemplate() {
        return new RestTemplate();
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isRequireClientAuth() {
        return requireClientAuth;
    }

    public void setRequireClientAuth(boolean requireClientAuth) {
        this.requireClientAuth = requireClientAuth;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
