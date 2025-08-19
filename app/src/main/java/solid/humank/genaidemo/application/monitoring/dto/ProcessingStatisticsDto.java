package solid.humank.genaidemo.application.monitoring.dto;

/**
 * 事件處理統計DTO
 * 應用層的事件處理統計數據傳輸對象
 */
public class ProcessingStatisticsDto {
    private final long totalProcessed;
    private final long totalSucceeded;
    private final long totalFailed;
    private final long totalTimedOut;
    private final int activeProcessing;
    private final double successRate;
    private final double failureRate;
    private final double timeoutRate;

    public ProcessingStatisticsDto(long totalProcessed, long totalSucceeded, long totalFailed,
            long totalTimedOut, int activeProcessing, double successRate,
            double failureRate, double timeoutRate) {
        this.totalProcessed = totalProcessed;
        this.totalSucceeded = totalSucceeded;
        this.totalFailed = totalFailed;
        this.totalTimedOut = totalTimedOut;
        this.activeProcessing = activeProcessing;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.timeoutRate = timeoutRate;
    }

    public long getTotalProcessed() {
        return totalProcessed;
    }

    public long getTotalSucceeded() {
        return totalSucceeded;
    }

    public long getTotalFailed() {
        return totalFailed;
    }

    public long getTotalTimedOut() {
        return totalTimedOut;
    }

    public int getActiveProcessing() {
        return activeProcessing;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public double getTimeoutRate() {
        return timeoutRate;
    }
}