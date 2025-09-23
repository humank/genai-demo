package solid.humank.genaidemo.domain.observability.events;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 用戶行為分析事件
 * 
 * 當前端收集到用戶行為數據時發布此事件，用於業務分析和用戶體驗優化。
 * 整合現有 DDD 系統，支援環境差異化處理（開發環境記憶體處理，生產環境 MSK 處理）。
 * 
 * 設計原則：
 * - 不可變 Record 實作，符合領域事件最佳實踐
 * - 包含完整的用戶行為上下文資訊
 * - 支援追蹤 ID 傳播，整合現有 MDC 系統
 * - 可選用戶 ID，支援匿名用戶追蹤
 * 
 * 需求: 1.1, 1.2, 2.1, 2.2
 */
public record UserBehaviorAnalyticsEvent(
        String eventId,
        String eventType,
        String sessionId,
        Optional<String> userId,
        String traceId,
        Map<String, Object> eventData,
        LocalDateTime receivedAt,
        UUID domainEventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 創建用戶行為分析事件
     * 
     * @param eventId   前端事件 ID
     * @param eventType 事件類型 (page_view, user_action, business_event)
     * @param sessionId 會話 ID
     * @param userId    用戶 ID (可選)
     * @param traceId   追蹤 ID，用於端到端追蹤
     * @param eventData 事件數據
     * @return 用戶行為分析事件實例
     */
    public static UserBehaviorAnalyticsEvent create(
            String eventId,
            String eventType,
            String sessionId,
            Optional<String> userId,
            String traceId,
            Map<String, Object> eventData) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new UserBehaviorAnalyticsEvent(
                eventId, eventType, sessionId, userId, traceId, eventData,
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
        return "UserBehaviorAnalytics";
    }

    @Override
    public String getAggregateId() {
        return sessionId;
    }

    /**
     * 獲取前端原始事件 ID
     * 
     * @return 前端事件 ID
     */
    public String getFrontendEventId() {
        return eventId;
    }

    /**
     * 獲取前端事件類型
     * 
     * @return 前端事件類型
     */
    public String getFrontendEventType() {
        return eventType;
    }

    /**
     * 檢查是否為匿名用戶事件
     * 
     * @return 如果是匿名用戶返回 true
     */
    public boolean isAnonymousUser() {
        return userId.isEmpty();
    }

    /**
     * 獲取事件數據中的特定值
     * 
     * @param key 數據鍵
     * @return 數據值
     */
    public Optional<Object> getEventDataValue(String key) {
        return Optional.ofNullable(eventData.get(key));
    }

    /**
     * 獲取頁面資訊（如果是頁面相關事件）
     * 
     * @return 頁面路徑
     */
    public Optional<String> getPage() {
        return getEventDataValue("page").map(Object::toString);
    }

    /**
     * 獲取用戶操作資訊（如果是操作事件）
     * 
     * @return 操作類型
     */
    public Optional<String> getAction() {
        return getEventDataValue("action").map(Object::toString);
    }
}