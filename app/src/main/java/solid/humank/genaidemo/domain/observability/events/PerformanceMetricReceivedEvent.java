package solid.humank.genaidemo.domain.observability.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 效能指標接收事件
 * 
 * 當前端收集到效能指標（如 LCP, FID, CLS, TTFB）時發布此事件。
 * 用於監控前端效能和用戶體驗優化。
 * 
 * 設計原則：
 * - 不可變 Record 實作，符合領域事件最佳實踐
 * - 包含完整的效能指標上下文
 * - 支援追蹤 ID 傳播，整合現有 MDC 系統
 * - 支援多種效能指標類型
 * 
 * 需求: 1.3, 2.1
 */
public record PerformanceMetricReceivedEvent(
        String metricId,
        String metricType,
        double value,
        String page,
        String sessionId,
        String traceId,
        LocalDateTime receivedAt,
        UUID domainEventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 創建效能指標接收事件
     * 
     * @param metricId   指標 ID
     * @param metricType 指標類型 (lcp, fid, cls, ttfb, page_load)
     * @param value      指標值
     * @param page       頁面路徑
     * @param sessionId  會話 ID
     * @param traceId    追蹤 ID
     * @return 效能指標接收事件實例
     */
    public static PerformanceMetricReceivedEvent create(
            String metricId,
            String metricType,
            double value,
            String page,
            String sessionId,
            String traceId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PerformanceMetricReceivedEvent(
                metricId, metricType, value, page, sessionId, traceId,
                LocalDateTime.now(), metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return domainEventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return "PerformanceMetricReceived";
    }

    @Override
    public String getAggregateId() {
        return sessionId;
    }

    /**
     * 檢查是否為核心網頁指標 (Core Web Vitals)
     * 
     * @return 如果是核心網頁指標返回 true
     */
    public boolean isCoreWebVital() {
        return "lcp".equals(metricType) ||
                "fid".equals(metricType) ||
                "cls".equals(metricType);
    }

    /**
     * 檢查是否為載入時間指標
     * 
     * @return 如果是載入時間指標返回 true
     */
    public boolean isLoadTimeMetric() {
        return "ttfb".equals(metricType) ||
                "page_load".equals(metricType);
    }

    /**
     * 獲取指標值的毫秒表示（如果適用）
     * 
     * @return 毫秒值
     */
    public long getValueInMilliseconds() {
        // CLS 是比率，不需要轉換
        if ("cls".equals(metricType)) {
            return Math.round(value * 1000); // 轉換為千分之一
        }
        // 其他指標通常已經是毫秒
        return Math.round(value);
    }

    /**
     * 檢查指標是否超過建議閾值
     * 基於 Google Core Web Vitals 建議
     * 
     * @return 如果超過閾值返回 true
     */
    public boolean exceedsRecommendedThreshold() {
        return switch (metricType) {
            case "lcp" -> value > 2500; // LCP > 2.5s
            case "fid" -> value > 100; // FID > 100ms
            case "cls" -> value > 0.1; // CLS > 0.1
            case "ttfb" -> value > 600; // TTFB > 600ms
            case "page_load" -> value > 3000; // Page Load > 3s
            default -> false;
        };
    }
}