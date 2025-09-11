package solid.humank.genaidemo.infrastructure.security;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Secure Kafka configuration with TLS encryption
 * Implements requirement 11.2: WHEN metrics are transmitted THEN the system
 * SHALL use encrypted connections (TLS)
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.kafka.security")
@Profile("production")
public class SecureKafkaConfiguration {

    private String bootstrapServers;
    private String securityProtocol = "SSL";
    private String sslTruststoreLocation;
    private String sslTruststorePassword;
    private String sslKeystoreLocation;
    private String sslKeystorePassword;
    private String sslKeyPassword;
    private String sslEndpointIdentificationAlgorithm = "https";

    @Bean
    public ProducerFactory<String, DomainEvent> secureProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Security configuration
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        // SSL configuration
        if ("SSL".equals(securityProtocol) || "SASL_SSL".equals(securityProtocol)) {
            configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);

            if (sslKeystoreLocation != null) {
                configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
                configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
                configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
            }

            configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                    sslEndpointIdentificationAlgorithm);
        }

        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DomainEvent> secureKafkaTemplate() {
        return new KafkaTemplate<>(secureProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, DomainEvent> secureConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "genai-demo-consumer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Security configuration
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        // SSL configuration
        if ("SSL".equals(securityProtocol) || "SASL_SSL".equals(securityProtocol)) {
            configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);

            if (sslKeystoreLocation != null) {
                configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
                configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
                configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
            }

            configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                    sslEndpointIdentificationAlgorithm);
        }

        // Consumer settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "solid.humank.genaidemo.domain.*");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> secureKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DomainEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(secureConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }

    // Getters and setters
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getSslTruststoreLocation() {
        return sslTruststoreLocation;
    }

    public void setSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    public String getSslKeystoreLocation() {
        return sslKeystoreLocation;
    }

    public void setSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public String getSslKeyPassword() {
        return sslKeyPassword;
    }

    public void setSslKeyPassword(String sslKeyPassword) {
        this.sslKeyPassword = sslKeyPassword;
    }

    public String getSslEndpointIdentificationAlgorithm() {
        return sslEndpointIdentificationAlgorithm;
    }

    public void setSslEndpointIdentificationAlgorithm(String sslEndpointIdentificationAlgorithm) {
        this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
    }
}