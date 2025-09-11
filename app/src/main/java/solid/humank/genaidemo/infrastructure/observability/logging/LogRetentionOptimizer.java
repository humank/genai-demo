package solid.humank.genaidemo.infrastructure.observability.logging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optimizes log levels and retention policies to reduce storage costs and
 * improve performance.
 * Implements requirement 12.2: WHEN storing logs THEN the system SHALL use
 * appropriate log levels and retention periods
 */
@Component
public class LogRetentionOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(LogRetentionOptimizer.class);

    private final LogRetentionProperties properties;
    private final Map<String, LogLevelStats> logLevelStats = new ConcurrentHashMap<>();

    public LogRetentionOptimizer(LogRetentionProperties properties) {
        this.properties = properties;
    }

    /**
     * Analyzes log patterns and suggests optimizations
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void analyzeLogPatterns() {
        if (!properties.isOptimizationEnabled()) {
            return;
        }

        logger.debug("Analyzing log patterns for optimization opportunities");

        // Analyze current log volume and patterns
        analyzeLogVolume();

        // Suggest log level adjustments
        suggestLogLevelOptimizations();

        // Check retention policy compliance
        checkRetentionCompliance();
    }

    private void analyzeLogVolume() {
        // Track log volume by level and logger
        logLevelStats.forEach((logger, stats) -> {
            if (stats.getVolumePerMinute() > properties.getHighVolumeThreshold()) {
                this.logger.warn("High log volume detected for logger: {} - {} logs/minute",
                        logger, stats.getVolumePerMinute());

                // Suggest sampling for high-volume loggers
                if (stats.getLevel().equals("DEBUG") || stats.getLevel().equals("TRACE")) {
                    this.logger.info("Suggestion: Consider reducing log level for {} to INFO to reduce costs", logger);
                }
            }
        });
    }

    private void suggestLogLevelOptimizations() {
        // Production environment optimizations
        if (isProductionEnvironment()) {
            suggestProductionOptimizations();
        }

        // Development environment optimizations
        if (isDevelopmentEnvironment()) {
            suggestDevelopmentOptimizations();
        }
    }

    private void suggestProductionOptimizations() {
        logger.info("Production log optimization suggestions:");
        logger.info("- Set root log level to WARN to reduce volume");
        logger.info("- Enable structured logging for better searchability");
        logger.info("- Use sampling for high-frequency debug logs");
        logger.info("- Configure log rotation based on size and time");
    }

    private void suggestDevelopmentOptimizations() {
        logger.debug("Development log optimization suggestions:");
        logger.debug("- Use DEBUG level for application packages");
        logger.debug("- Set third-party libraries to WARN level");
        logger.debug("- Enable SQL logging for debugging");
    }

    private void checkRetentionCompliance() {
        LocalDateTime now = LocalDateTime.now();

        properties.getRetentionPolicies().forEach((logType, policy) -> {
            logger.debug("Checking retention compliance for {}: hot={}, warm={}, cold={}",
                    logType, policy.getHotStorageDuration(), policy.getWarmStorageDuration(),
                    policy.getColdStorageDuration());
        });
    }

    public void recordLogEvent(String loggerName, String level) {
        logLevelStats.computeIfAbsent(loggerName, k -> new LogLevelStats(loggerName, level))
                .incrementCount();
    }

    private boolean isProductionEnvironment() {
        return "production".equals(System.getProperty("spring.profiles.active"));
    }

    private boolean isDevelopmentEnvironment() {
        return "dev".equals(System.getProperty("spring.profiles.active"));
    }

    /**
     * Statistics for log levels and volumes
     */
    public static class LogLevelStats {
        private final String loggerName;
        private final String level;
        private long count = 0;
        private LocalDateTime lastUpdated = LocalDateTime.now();

        public LogLevelStats(String loggerName, String level) {
            this.loggerName = loggerName;
            this.level = level;
        }

        public void incrementCount() {
            count++;
            lastUpdated = LocalDateTime.now();
        }

        public double getVolumePerMinute() {
            Duration elapsed = Duration.between(lastUpdated.minusMinutes(1), LocalDateTime.now());
            return elapsed.toMinutes() > 0 ? (double) count / elapsed.toMinutes() : count;
        }

        // Getters
        public String getLoggerName() {
            return loggerName;
        }

        public String getLevel() {
            return level;
        }

        public long getCount() {
            return count;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}