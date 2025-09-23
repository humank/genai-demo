package solid.humank.genaidemo.application.observability.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析統計 DTO
 * 
 * 用於傳輸綜合分析統計數據，包含會話、事件、轉換率等關鍵指標。
 * 僅在生產環境中使用，支援統計資料檢索。
 * 
 * 設計原則：
 * - 不可變 DTO，確保數據一致性
 * - 包含完整的統計維度
 * - 支援 JSON 序列化
 * - 建構者模式，便於創建
 * 
 * 需求: 2.3, 3.3
 */
public record AnalyticsStatsDto(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalSessions,
        Long anonymousSessions,
        Long registeredUserSessions,
        Double averageSessionDuration,
        Long totalPageViews,
        Long totalUserActions,
        Long totalBusinessEvents,
        Long totalEvents,
        Double actionConversionRate,
        Double businessConversionRate,
        List<Map<String, Object>> pageViewStats,
        List<Map<String, Object>> performanceStats) {

    /**
     * 建構者類別
     */
    public static class Builder {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long totalSessions;
        private Long anonymousSessions;
        private Long registeredUserSessions;
        private Double averageSessionDuration;
        private Long totalPageViews;
        private Long totalUserActions;
        private Long totalBusinessEvents;
        private Long totalEvents;
        private Double actionConversionRate;
        private Double businessConversionRate;
        private List<Map<String, Object>> pageViewStats;
        private List<Map<String, Object>> performanceStats;

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder totalSessions(Long totalSessions) {
            this.totalSessions = totalSessions;
            return this;
        }

        public Builder anonymousSessions(Long anonymousSessions) {
            this.anonymousSessions = anonymousSessions;
            return this;
        }

        public Builder registeredUserSessions(Long registeredUserSessions) {
            this.registeredUserSessions = registeredUserSessions;
            return this;
        }

        public Builder averageSessionDuration(Double averageSessionDuration) {
            this.averageSessionDuration = averageSessionDuration;
            return this;
        }

        public Builder totalPageViews(Long totalPageViews) {
            this.totalPageViews = totalPageViews;
            return this;
        }

        public Builder totalUserActions(Long totalUserActions) {
            this.totalUserActions = totalUserActions;
            return this;
        }

        public Builder totalBusinessEvents(Long totalBusinessEvents) {
            this.totalBusinessEvents = totalBusinessEvents;
            return this;
        }

        public Builder totalEvents(Long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder actionConversionRate(Double actionConversionRate) {
            this.actionConversionRate = actionConversionRate;
            return this;
        }

        public Builder businessConversionRate(Double businessConversionRate) {
            this.businessConversionRate = businessConversionRate;
            return this;
        }

        public Builder pageViewStats(List<Map<String, Object>> pageViewStats) {
            this.pageViewStats = pageViewStats;
            return this;
        }

        public Builder performanceStats(List<Map<String, Object>> performanceStats) {
            this.performanceStats = performanceStats;
            return this;
        }

        public AnalyticsStatsDto build() {
            return new AnalyticsStatsDto(
                    startTime, endTime, totalSessions, anonymousSessions, registeredUserSessions,
                    averageSessionDuration, totalPageViews, totalUserActions, totalBusinessEvents,
                    totalEvents, actionConversionRate, businessConversionRate,
                    pageViewStats, performanceStats);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 計算用戶參與率
     */
    public Double getUserEngagementRate() {
        if (totalSessions == null || totalSessions == 0) {
            return 0.0;
        }
        return registeredUserSessions != null ? (double) registeredUserSessions / totalSessions : 0.0;
    }

    /**
     * 計算平均每會話頁面瀏覽量
     */
    public Double getAveragePageViewsPerSession() {
        if (totalSessions == null || totalSessions == 0) {
            return 0.0;
        }
        return totalPageViews != null ? (double) totalPageViews / totalSessions : 0.0;
    }

    /**
     * 計算平均每會話用戶操作數
     */
    public Double getAverageActionsPerSession() {
        if (totalSessions == null || totalSessions == 0) {
            return 0.0;
        }
        return totalUserActions != null ? (double) totalUserActions / totalSessions : 0.0;
    }

    /**
     * 檢查是否有效能問題
     */
    public boolean hasPerformanceIssues() {
        if (performanceStats == null || performanceStats.isEmpty()) {
            return false;
        }

        return performanceStats.stream()
                .anyMatch(stat -> {
                    Object avgValue = stat.get("averageValue");
                    if (avgValue instanceof Number) {
                        double value = ((Number) avgValue).doubleValue();
                        String metricType = (String) stat.get("metricType");

                        // 檢查是否超過建議閾值
                        return switch (metricType) {
                            case "lcp" -> value > 2500; // LCP > 2.5s
                            case "fid" -> value > 100; // FID > 100ms
                            case "cls" -> value > 0.1; // CLS > 0.1
                            case "ttfb" -> value > 600; // TTFB > 600ms
                            default -> false;
                        };
                    }
                    return false;
                });
    }
}