package solid.humank.genaidemo.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.ArrayList;

/**
 * Configuration properties for Redis connection and distributed locking with high availability support.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {
    
    /**
     * Whether Redis is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Redis deployment mode: SINGLE, CLUSTER, SENTINEL
     */
    private DeploymentMode mode = DeploymentMode.SINGLE;
    
    /**
     * Redis server host (for single server mode).
     */
    @NotBlank
    private String host = "localhost";
    
    /**
     * Redis server port (for single server mode).
     */
    @Min(1)
    @Max(65535)
    private int port = 6379;
    
    /**
     * Redis database index.
     */
    @Min(0)
    @Max(15)
    private int database = 0;
    
    /**
     * Redis password (optional).
     */
    private String password;
    
    /**
     * Connection timeout in milliseconds.
     */
    @Min(1000)
    private int connectTimeout = 10000;
    
    /**
     * Command timeout in milliseconds.
     */
    @Min(1000)
    private int timeout = 3000;
    
    /**
     * Minimum idle connections in the pool.
     */
    @Min(1)
    private int connectionMinimumIdleSize = 10;
    
    /**
     * Maximum connections in the pool.
     */
    @Min(1)
    private int connectionPoolSize = 64;
    
    /**
     * Number of retry attempts for failed operations.
     */
    @Min(0)
    private int retryAttempts = 3;
    
    /**
     * Interval between retry attempts in milliseconds.
     */
    @Min(100)
    private int retryInterval = 1500;
    
    /**
     * Lock watchdog timeout in milliseconds.
     * This is the default lease time for locks if not specified.
     */
    @Min(5000)
    private long lockWatchdogTimeout = 30000;
    
    /**
     * Default lock wait time in milliseconds.
     */
    @Min(0)
    private long defaultLockWaitTime = 5000;
    
    /**
     * Default lock lease time in milliseconds.
     */
    @Min(1000)
    private long defaultLockLeaseTime = 30000;
    
    /**
     * Cluster configuration.
     */
    private Cluster cluster = new Cluster();
    
    /**
     * Sentinel configuration.
     */
    private Sentinel sentinel = new Sentinel();
    
    /**
     * High availability configuration.
     */
    private HighAvailability highAvailability = new HighAvailability();
    
    /**
     * Health check configuration.
     */
    private HealthCheck healthCheck = new HealthCheck();
    
    /**
     * Redis deployment modes.
     */
    public enum DeploymentMode {
        SINGLE, CLUSTER, SENTINEL
    }
    
    /**
     * Cluster configuration properties.
     */
    @Data
    public static class Cluster {
        /**
         * Cluster node addresses in format host:port.
         */
        private List<String> nodes = new ArrayList<>();
        
        /**
         * Maximum number of redirections to follow.
         */
        @Min(1)
        private int maxRedirections = 5;
        
        /**
         * Scan interval for cluster topology updates in milliseconds.
         */
        @Min(1000)
        private int scanInterval = 5000;
        
        /**
         * Whether to read from slave nodes.
         */
        private boolean readFromSlaves = true;
        
        /**
         * Slave connection pool size.
         */
        @Min(1)
        private int slaveConnectionPoolSize = 32;
        
        /**
         * Master connection pool size.
         */
        @Min(1)
        private int masterConnectionPoolSize = 32;
    }
    
    /**
     * Sentinel configuration properties.
     */
    @Data
    public static class Sentinel {
        /**
         * Master name.
         */
        private String masterName = "mymaster";
        
        /**
         * Sentinel node addresses in format host:port.
         */
        private List<String> nodes = new ArrayList<>();
        
        /**
         * Sentinel password (if different from Redis password).
         */
        private String password;
        
        /**
         * Whether to check sentinel nodes list.
         */
        private boolean checkSentinelsList = true;
        
        /**
         * Sentinel scan interval in milliseconds.
         */
        @Min(1000)
        private int scanInterval = 2000;
        
        /**
         * Whether to read from slave nodes.
         */
        private boolean readFromSlaves = true;
    }
    
    /**
     * High availability configuration.
     */
    @Data
    public static class HighAvailability {
        /**
         * Whether to enable automatic failover.
         */
        private boolean enableFailover = true;
        
        /**
         * Failover timeout in milliseconds.
         */
        @Min(1000)
        private int failoverTimeout = 30000;
        
        /**
         * Maximum number of failover attempts.
         */
        @Min(1)
        private int maxFailoverAttempts = 3;
        
        /**
         * Interval between failover attempts in milliseconds.
         */
        @Min(1000)
        private int failoverRetryInterval = 5000;
        
        /**
         * Whether to enable connection recovery.
         */
        private boolean enableConnectionRecovery = true;
        
        /**
         * Connection recovery interval in milliseconds.
         */
        @Min(1000)
        private int connectionRecoveryInterval = 10000;
        
        /**
         * Keep alive interval in milliseconds.
         */
        @Min(1000)
        private int keepAliveInterval = 30000;
        
        /**
         * TCP keep alive enabled.
         */
        private boolean tcpKeepAlive = true;
    }
    
    /**
     * Health check configuration.
     */
    @Data
    public static class HealthCheck {
        /**
         * Whether to enable health checks.
         */
        private boolean enabled = true;
        
        /**
         * Health check interval in milliseconds.
         */
        @Min(1000)
        private int interval = 30000;
        
        /**
         * Health check timeout in milliseconds.
         */
        @Min(1000)
        private int timeout = 5000;
        
        /**
         * Number of consecutive failures before marking as unhealthy.
         */
        @Min(1)
        private int failureThreshold = 3;
        
        /**
         * Number of consecutive successes before marking as healthy.
         */
        @Min(1)
        private int successThreshold = 1;
        
        /**
         * Whether to perform ping health checks.
         */
        private boolean pingEnabled = true;
        
        /**
         * Whether to perform info health checks.
         */
        private boolean infoEnabled = true;
    }
    
    @Override
    public String toString() {
        return String.format("RedisProperties{enabled=%s, host='%s', port=%d, database=%d, " +
                           "connectTimeout=%d, timeout=%d, connectionPoolSize=%d, " +
                           "lockWatchdogTimeout=%d}", 
                           enabled, host, port, database, connectTimeout, timeout, 
                           connectionPoolSize, lockWatchdogTimeout);
    }
}