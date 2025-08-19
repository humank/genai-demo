package solid.humank.genaidemo.infrastructure.external.monitoring;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.application.monitoring.dto.BackpressureStatusDto;
import solid.humank.genaidemo.application.monitoring.dto.ProcessingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.RetryStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SequenceTrackingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SystemHealthStatusDto;
import solid.humank.genaidemo.application.monitoring.port.EventMonitoringPort;
import solid.humank.genaidemo.infrastructure.event.backpressure.BackpressureManager;
import solid.humank.genaidemo.infrastructure.event.handler.ResilientEventHandler;
import solid.humank.genaidemo.infrastructure.event.monitoring.EventProcessingMonitor;
import solid.humank.genaidemo.infrastructure.event.retry.EventRetryManager;
import solid.humank.genaidemo.infrastructure.event.sequence.EventSequenceTracker;

/**
 * 事件監控適配器
 * 實現事件監控端口介面，將基礎設施層的監控功能適配到應用層
 * 
 * 需求 3.1: 移除應用層對基礎設施層的直接依賴
 */
@Component
public class EventMonitoringAdapter implements EventMonitoringPort {

    private final ResilientEventHandler resilientEventHandler;
    private final EventProcessingMonitor processingMonitor;
    private final EventRetryManager retryManager;
    private final BackpressureManager backpressureManager;
    private final EventSequenceTracker sequenceTracker;

    public EventMonitoringAdapter(ResilientEventHandler resilientEventHandler,
            EventProcessingMonitor processingMonitor,
            EventRetryManager retryManager,
            BackpressureManager backpressureManager,
            EventSequenceTracker sequenceTracker) {
        this.resilientEventHandler = resilientEventHandler;
        this.processingMonitor = processingMonitor;
        this.retryManager = retryManager;
        this.backpressureManager = backpressureManager;
        this.sequenceTracker = sequenceTracker;
    }

    @Override
    public SystemHealthStatusDto getSystemHealth() {
        ResilientEventHandler.SystemHealthStatus health = resilientEventHandler.getSystemHealth();
        return mapToSystemHealthStatusDto(health);
    }

    @Override
    public ProcessingStatisticsDto getProcessingStatistics() {
        EventProcessingMonitor.ProcessingStatistics stats = processingMonitor.getStatistics();
        return mapToProcessingStatisticsDto(stats);
    }

    @Override
    public RetryStatisticsDto getRetryStatistics() {
        EventRetryManager.RetryStatistics stats = retryManager.getRetryStatistics();
        return mapToRetryStatisticsDto(stats);
    }

    @Override
    public BackpressureStatusDto getBackpressureStatus() {
        BackpressureManager.BackpressureStatus status = backpressureManager.getStatus();
        return mapToBackpressureStatusDto(status);
    }

    @Override
    public SequenceTrackingStatisticsDto getSequenceStatistics() {
        EventSequenceTracker.SequenceTrackingStatistics stats = sequenceTracker.getStatistics();
        return mapToSequenceTrackingStatisticsDto(stats);
    }

    @Override
    public void resetProcessingStatistics() {
        processingMonitor.resetStatistics();
    }

    @Override
    public void resetAggregateSequence(String aggregateId) {
        sequenceTracker.resetSequence(aggregateId);
    }

    @Override
    public void forceUpdateSequence(String aggregateId, long sequence) {
        sequenceTracker.forceUpdateSequence(aggregateId, sequence);
    }

    @Override
    public long getCurrentSequence(String aggregateId) {
        return sequenceTracker.getCurrentSequence(aggregateId);
    }

    // Mapping methods
    private SystemHealthStatusDto mapToSystemHealthStatusDto(ResilientEventHandler.SystemHealthStatus status) {
        return new SystemHealthStatusDto(
                mapToProcessingStatisticsDto(status.getProcessingStats()),
                mapToRetryStatisticsDto(status.getRetryStats()),
                mapToBackpressureStatusDto(status.getBackpressureStatus()),
                mapToSequenceTrackingStatisticsDto(status.getSequenceStats()),
                status.getHealthScore(),
                status.isHealthy());
    }

    private ProcessingStatisticsDto mapToProcessingStatisticsDto(EventProcessingMonitor.ProcessingStatistics stats) {
        return new ProcessingStatisticsDto(
                stats.getTotalProcessed(),
                stats.getTotalSucceeded(),
                stats.getTotalFailed(),
                stats.getTotalTimedOut(),
                stats.getActiveProcessing(),
                stats.getSuccessRate(),
                stats.getFailureRate(),
                stats.getTimeoutRate());
    }

    private RetryStatisticsDto mapToRetryStatisticsDto(EventRetryManager.RetryStatistics stats) {
        return new RetryStatisticsDto(
                stats.getActiveRetries(),
                stats.getTotalRetryAttempts());
    }

    private BackpressureStatusDto mapToBackpressureStatusDto(BackpressureManager.BackpressureStatus status) {
        return new BackpressureStatusDto(
                status.getLevel().name(),
                status.getCurrentConcurrentEvents(),
                status.getCurrentQueueSize(),
                status.getEventsInCurrentWindow(),
                status.getLastLevelChangeTime());
    }

    private SequenceTrackingStatisticsDto mapToSequenceTrackingStatisticsDto(
            EventSequenceTracker.SequenceTrackingStatistics stats) {
        return new SequenceTrackingStatisticsDto(
                stats.getTotalAggregates(),
                stats.getTotalRecords(),
                stats.getValidEvents(),
                stats.getDuplicateEvents(),
                stats.getOutOfOrderEvents(),
                stats.getValidRate(),
                stats.getDuplicateRate(),
                stats.getOutOfOrderRate());
    }
}