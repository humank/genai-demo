package solid.humank.genaidemo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business Continuity Metrics Configuration
 * 
 * Provides comprehensive metrics for monitoring business continuity:
 * - RTO (Recovery Time Objective) tracking
 * - RPO (Recovery Point Objective) tracking
 * - Business transaction metrics
 * - Service availability metrics
 * - Data consistency metrics
 * 
 * @see <a href="https://micrometer.io/">Micrometer Documentation</a>
 */
@Configuration
public class BusinessContinuityMetricsConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessContinuityMetricsConfiguration.class);
    
    /**
     * Business Continuity Metrics Binder
     * 
     * Registers custom metrics for business continuity monitoring
     */
    @Bean
    public MeterBinder businessContinuityMetrics(MeterRegistry registry) {
        logger.info("Initializing Business Continuity Metrics");
        
        return (meterRegistry) -> {
            // RTO Metrics - Recovery Time Objective
            meterRegistry.gauge("business.continuity.rto.target.seconds", 
                Tags.of("type", "target"), 
                120.0); // Target RTO: 2 minutes
            
            // RPO Metrics - Recovery Point Objective
            meterRegistry.gauge("business.continuity.rpo.target.seconds", 
                Tags.of("type", "target"), 
                1.0); // Target RPO: 1 second
            
            // Service Availability Target
            meterRegistry.gauge("business.continuity.availability.target.percent", 
                Tags.of("type", "target"), 
                99.9); // Target: 99.9% availability
            
            logger.info("Business Continuity Metrics initialized: RTO=120s, RPO=1s, Availability=99.9%");
        };
    }
    
    /**
     * Recovery Metrics Tracker
     * 
     * Tracks actual recovery times and data loss during incidents
     */
    @Bean
    public RecoveryMetricsTracker recoveryMetricsTracker(MeterRegistry registry) {
        return new RecoveryMetricsTracker(registry);
    }
    
    /**
     * Business Transaction Metrics Tracker
     * 
     * Tracks business-level transaction metrics for continuity monitoring
     */
    @Bean
    public BusinessTransactionMetricsTracker businessTransactionMetricsTracker(MeterRegistry registry) {
        return new BusinessTransactionMetricsTracker(registry);
    }
    
    /**
     * Recovery Metrics Tracker Implementation
     */
    public static class RecoveryMetricsTracker {
        private final MeterRegistry registry;
        private final ConcurrentHashMap<String, Instant> incidentStartTimes = new ConcurrentHashMap<>();
        private final AtomicLong totalIncidents = new AtomicLong(0);
        private final AtomicLong successfulRecoveries = new AtomicLong(0);
        
        private static final Logger logger = LoggerFactory.getLogger(RecoveryMetricsTracker.class);
        
        public RecoveryMetricsTracker(MeterRegistry registry) {
            this.registry = registry;
            
            // Register gauges for recovery statistics
            registry.gauge("business.continuity.incidents.total", totalIncidents);
            registry.gauge("business.continuity.recoveries.successful", successfulRecoveries);
            registry.gauge("business.continuity.recovery.success.rate", this, 
                tracker -> tracker.getRecoverySuccessRate());
        }
        
        /**
         * Record the start of an incident
         */
        public void recordIncidentStart(String incidentId, String incidentType) {
            incidentStartTimes.put(incidentId, Instant.now());
            totalIncidents.incrementAndGet();
            
            registry.counter("business.continuity.incidents.started",
                Tags.of("type", incidentType))
                .increment();
            
            logger.warn("Incident started: {} (type: {})", incidentId, incidentType);
        }
        
        /**
         * Record successful recovery and calculate RTO
         */
        public void recordRecoverySuccess(String incidentId, String incidentType) {
            Instant startTime = incidentStartTimes.remove(incidentId);
            if (startTime != null) {
                Duration recoveryTime = Duration.between(startTime, Instant.now());
                
                // Record actual RTO
                registry.timer("business.continuity.rto.actual",
                    Tags.of("type", incidentType))
                    .record(recoveryTime);
                
                successfulRecoveries.incrementAndGet();
                
                logger.info("Recovery successful: {} (type: {}, RTO: {}s)", 
                    incidentId, incidentType, recoveryTime.getSeconds());
                
                // Check if RTO target was met
                if (recoveryTime.getSeconds() <= 120) {
                    registry.counter("business.continuity.rto.target.met",
                        Tags.of("type", incidentType))
                        .increment();
                } else {
                    registry.counter("business.continuity.rto.target.missed",
                        Tags.of("type", incidentType))
                        .increment();
                    
                    logger.warn("RTO target missed: {} (actual: {}s, target: 120s)", 
                        incidentId, recoveryTime.getSeconds());
                }
            }
        }
        
        /**
         * Record data loss and calculate RPO
         */
        public void recordDataLoss(String incidentId, long recordsLost, Duration timeLost) {
            registry.counter("business.continuity.data.loss.records",
                Tags.of("incident", incidentId))
                .increment(recordsLost);
            
            registry.timer("business.continuity.rpo.actual",
                Tags.of("incident", incidentId))
                .record(timeLost);
            
            if (timeLost.getSeconds() <= 1) {
                registry.counter("business.continuity.rpo.target.met").increment();
            } else {
                registry.counter("business.continuity.rpo.target.missed").increment();
                logger.warn("RPO target missed: {} (actual: {}s, target: 1s, records lost: {})", 
                    incidentId, timeLost.getSeconds(), recordsLost);
            }
        }
        
        private double getRecoverySuccessRate() {
            long total = totalIncidents.get();
            if (total == 0) return 100.0;
            return (successfulRecoveries.get() * 100.0) / total;
        }
    }
    
    /**
     * Business Transaction Metrics Tracker Implementation
     */
    public static class BusinessTransactionMetricsTracker {
        private final MeterRegistry registry;
        private static final Logger logger = LoggerFactory.getLogger(BusinessTransactionMetricsTracker.class);
        
        public BusinessTransactionMetricsTracker(MeterRegistry registry) {
            this.registry = registry;
            logger.info("Business Transaction Metrics Tracker initialized");
        }
        
        /**
         * Record a business transaction
         */
        public void recordTransaction(String transactionType, boolean success, Duration duration) {
            // Record transaction count
            registry.counter("business.transactions.total",
                Tags.of("type", transactionType, "status", success ? "success" : "failure"))
                .increment();
            
            // Record transaction duration
            registry.timer("business.transactions.duration",
                Tags.of("type", transactionType))
                .record(duration);
            
            // Record success rate
            if (success) {
                registry.counter("business.transactions.success",
                    Tags.of("type", transactionType))
                    .increment();
            } else {
                registry.counter("business.transactions.failure",
                    Tags.of("type", transactionType))
                    .increment();
            }
        }
        
        /**
         * Record business value metrics (e.g., order value, revenue)
         */
        public void recordBusinessValue(String metricName, double value, Tag... tags) {
            registry.counter("business.value." + metricName, Tags.of(tags))
                .increment(value);
        }
        
        /**
         * Record service availability
         */
        public void recordServiceAvailability(String serviceName, boolean available) {
            registry.gauge("business.service.availability",
                Tags.of("service", serviceName),
                available ? 1.0 : 0.0);
        }
    }
}
