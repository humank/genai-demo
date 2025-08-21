package solid.humank.genaidemo.application.monitoring.dto;

/**
 * 序列追蹤統計DTO
 * 應用層的序列追蹤統計數據傳輸對象
 */
public class SequenceTrackingStatisticsDto {
    private final int totalAggregates;
    private final int totalRecords;
    private final long validEvents;
    private final long duplicateEvents;
    private final long outOfOrderEvents;
    private final double validRate;
    private final double duplicateRate;
    private final double outOfOrderRate;

    public SequenceTrackingStatisticsDto(int totalAggregates, int totalRecords,
            long validEvents, long duplicateEvents, long outOfOrderEvents,
            double validRate, double duplicateRate, double outOfOrderRate) {
        this.totalAggregates = totalAggregates;
        this.totalRecords = totalRecords;
        this.validEvents = validEvents;
        this.duplicateEvents = duplicateEvents;
        this.outOfOrderEvents = outOfOrderEvents;
        this.validRate = validRate;
        this.duplicateRate = duplicateRate;
        this.outOfOrderRate = outOfOrderRate;
    }

    public int getTotalAggregates() {
        return totalAggregates;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public long getValidEvents() {
        return validEvents;
    }

    public long getDuplicateEvents() {
        return duplicateEvents;
    }

    public long getOutOfOrderEvents() {
        return outOfOrderEvents;
    }

    public double getValidRate() {
        return validRate;
    }

    public double getDuplicateRate() {
        return duplicateRate;
    }

    public double getOutOfOrderRate() {
        return outOfOrderRate;
    }
}