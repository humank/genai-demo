package solid.humank.genaidemo.application.monitoring.dto;

/**
 * 重試統計DTO
 * 應用層的重試統計數據傳輸對象
 */
public class RetryStatisticsDto {
    private final int activeRetries;
    private final long totalRetryAttempts;

    public RetryStatisticsDto(int activeRetries, long totalRetryAttempts) {
        this.activeRetries = activeRetries;
        this.totalRetryAttempts = totalRetryAttempts;
    }

    public int getActiveRetries() {
        return activeRetries;
    }

    public long getTotalRetryAttempts() {
        return totalRetryAttempts;
    }
}