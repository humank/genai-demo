package solid.humank.genaidemo.application.monitoring.dto;

/**
 * 系統健康狀態DTO
 * 應用層的系統健康狀態數據傳輸對象
 */
public class SystemHealthStatusDto {
    private final ProcessingStatisticsDto processingStats;
    private final RetryStatisticsDto retryStats;
    private final BackpressureStatusDto backpressureStatus;
    private final SequenceTrackingStatisticsDto sequenceStats;
    private final int healthScore;
    private final boolean healthy;

    public SystemHealthStatusDto(ProcessingStatisticsDto processingStats,
            RetryStatisticsDto retryStats,
            BackpressureStatusDto backpressureStatus,
            SequenceTrackingStatisticsDto sequenceStats,
            int healthScore,
            boolean healthy) {
        this.processingStats = processingStats;
        this.retryStats = retryStats;
        this.backpressureStatus = backpressureStatus;
        this.sequenceStats = sequenceStats;
        this.healthScore = healthScore;
        this.healthy = healthy;
    }

    public ProcessingStatisticsDto getProcessingStats() {
        return processingStats;
    }

    public RetryStatisticsDto getRetryStats() {
        return retryStats;
    }

    public BackpressureStatusDto getBackpressureStatus() {
        return backpressureStatus;
    }

    public SequenceTrackingStatisticsDto getSequenceStats() {
        return sequenceStats;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public boolean isHealthy() {
        return healthy;
    }
}