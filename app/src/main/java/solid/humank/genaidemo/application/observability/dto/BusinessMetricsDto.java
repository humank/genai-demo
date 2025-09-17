package solid.humank.genaidemo.application.observability.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 業務指標 DTO
 * 
 * 用於傳輸業務相關的統計數據，包含轉換率、漏斗分析、熱門頁面等指標。
 * 僅在生產環境中使用，支援業務決策分析。
 * 
 * 設計原則：
 * - 不可變 DTO，確保數據一致性
 * - 專注於業務價值指標
 * - 支援 JSON 序列化
 * - 建構者模式，便於創建
 * 
 * 需求: 2.3, 3.3
 */
public record BusinessMetricsDto(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalEvents,
        Long pageViews,
        Long userActions,
        Long businessEvents,
        Double actionConversionRate,
        Double businessConversionRate,
        List<Map<String, Object>> funnelData,
        List<Map<String, Object>> popularPages) {

    /**
     * 建構者類別
     */
    public static class Builder {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long totalEvents;
        private Long pageViews;
        private Long userActions;
        private Long businessEvents;
        private Double actionConversionRate;
        private Double businessConversionRate;
        private List<Map<String, Object>> funnelData;
        private List<Map<String, Object>> popularPages;

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder totalEvents(Long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder pageViews(Long pageViews) {
            this.pageViews = pageViews;
            return this;
        }

        public Builder userActions(Long userActions) {
            this.userActions = userActions;
            return this;
        }

        public Builder businessEvents(Long businessEvents) {
            this.businessEvents = businessEvents;
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

        public Builder funnelData(List<Map<String, Object>> funnelData) {
            this.funnelData = funnelData;
            return this;
        }

        public Builder popularPages(List<Map<String, Object>> popularPages) {
            this.popularPages = popularPages;
            return this;
        }

        public BusinessMetricsDto build() {
            return new BusinessMetricsDto(
                    startTime, endTime, totalEvents, pageViews, userActions,
                    businessEvents, actionConversionRate, businessConversionRate,
                    funnelData, popularPages);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 計算事件參與度
     */
    public Double getEventEngagementRate() {
        if (pageViews == null || pageViews == 0) {
            return 0.0;
        }
        Long totalInteractions = (userActions != null ? userActions : 0L) +
                (businessEvents != null ? businessEvents : 0L);
        return (double) totalInteractions / pageViews;
    }

    /**
     * 獲取最熱門頁面
     */
    public Map<String, Object> getTopPage() {
        if (popularPages == null || popularPages.isEmpty()) {
            return Map.of();
        }
        return popularPages.get(0);
    }

    /**
     * 計算業務價值分數
     */
    public Double getBusinessValueScore() {
        if (businessEvents == null || pageViews == null || pageViews == 0) {
            return 0.0;
        }

        // 業務價值分數 = 業務事件數 * 轉換率 * 100
        Double conversionRate = businessConversionRate != null ? businessConversionRate : 0.0;
        return businessEvents * conversionRate * 100;
    }

    /**
     * 檢查轉換率是否健康
     */
    public boolean hasHealthyConversionRates() {
        // 一般電商網站的基準：
        // 操作轉換率 > 10%，業務轉換率 > 2%
        boolean actionRateHealthy = actionConversionRate != null && actionConversionRate > 0.1;
        boolean businessRateHealthy = businessConversionRate != null && businessConversionRate > 0.02;

        return actionRateHealthy && businessRateHealthy;
    }
}