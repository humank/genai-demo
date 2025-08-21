package solid.humank.genaidemo.application.monitoring.port;

import solid.humank.genaidemo.application.monitoring.dto.BackpressureStatusDto;
import solid.humank.genaidemo.application.monitoring.dto.ProcessingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.RetryStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SequenceTrackingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SystemHealthStatusDto;

/**
 * 事件監控端口介面
 * 定義事件監控系統的抽象介面，遵循端口和適配器模式
 * 
 * 需求 3.1: 移除應用層對基礎設施層的直接依賴
 */
public interface EventMonitoringPort {

    /**
     * 獲取系統健康狀態
     */
    SystemHealthStatusDto getSystemHealth();

    /**
     * 獲取處理統計資訊
     */
    ProcessingStatisticsDto getProcessingStatistics();

    /**
     * 獲取重試統計資訊
     */
    RetryStatisticsDto getRetryStatistics();

    /**
     * 獲取背壓狀態
     */
    BackpressureStatusDto getBackpressureStatus();

    /**
     * 獲取序列追蹤統計資訊
     */
    SequenceTrackingStatisticsDto getSequenceStatistics();

    /**
     * 重置處理統計資訊
     */
    void resetProcessingStatistics();

    /**
     * 重置聚合根序列
     */
    void resetAggregateSequence(String aggregateId);

    /**
     * 強制更新序列
     */
    void forceUpdateSequence(String aggregateId, long sequence);

    /**
     * 獲取當前序列
     */
    long getCurrentSequence(String aggregateId);
}