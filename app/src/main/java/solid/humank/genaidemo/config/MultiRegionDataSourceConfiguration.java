package solid.humank.genaidemo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import solid.humank.genaidemo.infrastructure.routing.HealthChecker;
import solid.humank.genaidemo.infrastructure.routing.RouteSelector;
import solid.humank.genaidemo.infrastructure.routing.SmartRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * MultiRegionDataSourceConfiguration sets up dual-region database connectivity.
 * 
 * This configuration creates separate DataSource instances for Taiwan and Japan regions,
 * then wraps them in a SmartRoutingDataSource for intelligent routing.
 * 
 * Activation:
 * - Enabled when spring.datasource.multi-region.enabled=true
 * - Requires taiwan and japan endpoint configurations
 * 
 * Configuration example:
 * <pre>
 * spring:
 *   datasource:
 *     multi-region:
 *       enabled: true
 *       taiwan:
 *         jdbc-url: jdbc:postgresql://taiwan-db.example.com:5432/genaidemo
 *         username: ${TAIWAN_DB_USER}
 *         password: ${TAIWAN_DB_PASSWORD}
 *       japan:
 *         jdbc-url: jdbc:postgresql://japan-db.example.com:5432/genaidemo
 *         username: ${JAPAN_DB_USER}
 *         password: ${JAPAN_DB_PASSWORD}
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.multi-region.enabled", havingValue = "true")
public class MultiRegionDataSourceConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiRegionDataSourceConfiguration.class);
    
    /**
     * Creates the Taiwan region DataSource.
     */
    @Bean(name = "taiwanDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.multi-region.taiwan")
    public DataSource taiwanDataSource() {
        logger.info("Initializing Taiwan region DataSource");
        HikariConfig config = new HikariConfig();
        config.setPoolName("TaiwanHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        return new HikariDataSource(config);
    }
    
    /**
     * Creates the Japan region DataSource.
     */
    @Bean(name = "japanDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.multi-region.japan")
    public DataSource japanDataSource() {
        logger.info("Initializing Japan region DataSource");
        HikariConfig config = new HikariConfig();
        config.setPoolName("JapanHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        return new HikariDataSource(config);
    }
    
    /**
     * Creates the primary SmartRoutingDataSource that intelligently routes
     * between Taiwan and Japan DataSources.
     */
    @Bean
    @Primary
    public DataSource dataSource(
            DataSource taiwanDataSource,
            DataSource japanDataSource,
            RouteSelector routeSelector,
            HealthChecker healthChecker) {
        
        logger.info("Initializing SmartRoutingDataSource for multi-region support");
        
        // Register DataSources with HealthChecker
        healthChecker.registerDataSource("taiwan-db", taiwanDataSource);
        healthChecker.registerDataSource("japan-db", japanDataSource);
        
        // Create routing DataSource
        SmartRoutingDataSource routingDataSource = new SmartRoutingDataSource(routeSelector);
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("taiwan-db", taiwanDataSource);
        targetDataSources.put("japan-db", japanDataSource);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(taiwanDataSource);
        
        logger.info("SmartRoutingDataSource initialized with {} regions", targetDataSources.size());
        
        return routingDataSource;
    }
}
