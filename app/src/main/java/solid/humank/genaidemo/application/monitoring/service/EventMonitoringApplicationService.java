package solid.humank.genaidemo.application.monitoring.service;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.application.monitoring.dto.BackpressureStatusDto;
import solid.humank.genaidemo.application.monitoring.dto.ProcessingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.RetryStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SequenceTrackingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SystemHealthStatusDto;
import solid.humank.genaidemo.application.monitoring.port.EventMonitoringPort;

/**
 * 事件監控應用服務
 * 提供事件處理系統監控功能的應用層抽象
 * 
 * 需求 3.1: 移除Controller對Infrastructure層的直接依賴
 */
@Service
public class EventMonitoringApplicationService {

    private final EventMonitoringPort eventMonitoringPort;

    public EventMonitoringApplicationService(EventMonitoringPort eventMonitoringPort) {
        this.eventMonitoringPort = eventMonitoringPort;
    }

    /**
     * 獲取系統健康狀態
     */
    public SystemHealthStatusDto getSystemHealth() {
        return eventMonitoringPort.getSystemHealth();
    }

    /**
     * 獲取事件處理統計
     */
    public ProcessingStatisticsDto getProcessingStatistics() {
        return eventMonitoringPort.getProcessingStatistics();
    }

    /**
     * 獲取重試統計
     */
    public RetryStatisticsDto getRetryStatistics() {
        return eventMonitoringPort.getRetryStatistics();
    }

    /**
     * 獲取背壓狀態
     */
    public BackpressureStatusDto getBackpressureStatus() {
        return eventMonitoringPort.getBackpressureStatus();
    }

    /**
     * 獲取序列追蹤統計
     */
    public SequenceTrackingStatisticsDto getSequenceStatistics() {
        return eventMonitoringPort.getSequenceStatistics();
    }

    /**
     * 重置處理統計
     */
    public void resetProcessingStatistics() {
        eventMonitoringPort.resetProcessingStatistics();
    }

    /**
     * 重置聚合根序列
     */
    public void resetAggregateSequence(String aggregateId) {
        eventMonitoringPort.resetAggregateSequence(aggregateId);
    }

    /**
     * 強制更新聚合根序列
     */
    public void forceUpdateSequence(String aggregateId, long sequence) {
        eventMonitoringPort.forceUpdateSequence(aggregateId, sequence);
    }

    /**
     * 獲取聚合根當前序列
     */
    public long getCurrentSequence(String aggregateId) {
        return eventMonitoringPort.getCurrentSequence(aggregateId);
    }
}