package solid.humank.genaidemo.infrastructure.event.maintenance;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.infrastructure.event.monitoring.EventProcessingMonitor;
import solid.humank.genaidemo.infrastructure.event.retry.EventRetryManager;
import solid.humank.genaidemo.infrastructure.event.sequence.EventSequenceTracker;

/**
 * 事件系統維護任務
 * 定期執行系統清理和監控任務
 */
@Component
public class EventSystemMaintenanceTask {

    private static final Logger logger = LoggerFactory.getLogger(EventSystemMaintenanceTask.class);

    private final EventProcessingMonitor monitor;
    private final EventRetryManager retryManager;
    private final EventSequenceTracker sequenceTracker;

    public EventSystemMaintenanceTask(EventProcessingMonitor monitor,
            EventRetryManager retryManager,
            EventSequenceTracker sequenceTracker) {
        this.monitor = monitor;
        this.retryManager = retryManager;
        this.sequenceTracker = sequenceTracker;
    }

    /**
     * 每分鐘檢查超時的事件處理
     */
    @Scheduled(fixedRate = 60000) // 每分鐘執行一次
    public void checkTimeouts() {
        try {
            Duration timeout = Duration.ofSeconds(30);
            monitor.checkForTimeouts(timeout);
        } catch (Exception e) {
            logger.error("Error during timeout check", e);
        }
    }

    /**
     * 每5分鐘清理過期的重試上下文
     */
    @Scheduled(fixedRate = 300000) // 每5分鐘執行一次
    public void cleanupRetryContexts() {
        try {
            retryManager.cleanupExpiredContexts();
            logger.debug("Cleaned up expired retry contexts");
        } catch (Exception e) {
            logger.error("Error during retry context cleanup", e);
        }
    }

    /**
     * 每小時清理過期的序列記錄
     */
    @Scheduled(fixedRate = 3600000) // 每小時執行一次
    public void cleanupSequenceRecords() {
        try {
            sequenceTracker.cleanupExpiredRecords();
            logger.debug("Cleaned up expired sequence records");
        } catch (Exception e) {
            logger.error("Error during sequence record cleanup", e);
        }
    }

    /**
     * 每10分鐘記錄系統統計信息
     */
    @Scheduled(fixedRate = 600000) // 每10分鐘執行一次
    public void logSystemStatistics() {
        try {
            EventProcessingMonitor.ProcessingStatistics processingStats = monitor.getStatistics();
            EventRetryManager.RetryStatistics retryStats = retryManager.getRetryStatistics();
            EventSequenceTracker.SequenceTrackingStatistics sequenceStats = sequenceTracker.getStatistics();

            logger.info("Event Processing Statistics - Total: {}, Succeeded: {}, Failed: {}, Timed Out: {}, " +
                    "Active: {}, Success Rate: {:.2f}%",
                    processingStats.getTotalProcessed(),
                    processingStats.getTotalSucceeded(),
                    processingStats.getTotalFailed(),
                    processingStats.getTotalTimedOut(),
                    processingStats.getActiveProcessing(),
                    processingStats.getSuccessRate() * 100);

            logger.info("Retry Statistics - Active Retries: {}, Total Retry Attempts: {}",
                    retryStats.getActiveRetries(),
                    retryStats.getTotalRetryAttempts());

            logger.info("Sequence Statistics - Total Aggregates: {}, Valid Events: {}, " +
                    "Duplicate Events: {}, Out-of-Order Events: {}, Valid Rate: {:.2f}%",
                    sequenceStats.getTotalAggregates(),
                    sequenceStats.getValidEvents(),
                    sequenceStats.getDuplicateEvents(),
                    sequenceStats.getOutOfOrderEvents(),
                    sequenceStats.getValidRate() * 100);

        } catch (Exception e) {
            logger.error("Error during statistics logging", e);
        }
    }
}