package solid.humank.genaidemo.domain.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 領域事件接口 所有領域事件都應該實現此接口
 * 
 * 設計理念：
 * 1. Record 優先：推薦使用 record 實作，減少樣板代碼
 * 2. 工廠方法：提供靜態工廠方法自動設定 eventId 和 occurredOn
 * 3. 類型安全：自動推導事件類型，避免手動指定錯誤
 * 4. 不可變性：所有事件都應該是不可變的
 * 
 * 使用方式：
 * 
 * public record CustomerCreatedEvent(
 * CustomerId customerId,
 * String customerName,
 * UUID eventId,
 * LocalDateTime occurredOn
 * ) implements DomainEvent {
 * 
 * // 工廠方法，自動設定 eventId 和 occurredOn
 * public static CustomerCreatedEvent create(CustomerId customerId, String
 * customerName) {
 * return new CustomerCreatedEvent(customerId, customerName, UUID.randomUUID(),
 * LocalDateTime.now());
 * }
 * 
 * @Override
 *           public String getEventType() {
 *           return DomainEvent.getEventTypeFromClass(this.getClass());
 *           }
 * 
 * @Override
 *           public String getAggregateId() {
 *           return customerId.value();
 *           }
 *           }
 */
public interface DomainEvent extends Serializable {

    /**
     * 獲取事件唯一標識
     *
     * @return 事件唯一標識
     */
    default UUID getEventId() {
        return UUID.randomUUID(); // 臨時默認實現，讓編譯通過
    }

    /**
     * 獲取事件發生時間
     *
     * @return 事件發生時間
     */
    LocalDateTime getOccurredOn();

    /**
     * 獲取事件類型
     *
     * @return 事件類型
     */
    String getEventType();

    /**
     * 獲取聚合根ID
     *
     * @return 聚合根ID
     */
    String getAggregateId();

    /**
     * 從類別名稱自動推導事件類型
     * 移除 "Event" 後綴，保留核心業務名稱
     * 
     * @param eventClass 事件類別
     * @return 事件類型名稱
     */
    static String getEventTypeFromClass(Class<? extends DomainEvent> eventClass) {
        String className = eventClass.getSimpleName();
        if (className.endsWith("Event")) {
            return className.substring(0, className.length() - 5);
        }
        return className;
    }

    /**
     * 創建事件的輔助方法
     * 為 record 事件提供統一的創建方式
     * 
     * @return 包含 eventId 和 occurredOn 的事件數據
     */
    static EventMetadata createEventMetadata() {
        return new EventMetadata(UUID.randomUUID(), LocalDateTime.now());
    }

    /**
     * 事件元數據記錄
     * 包含所有事件共同的元數據
     */
    record EventMetadata(UUID eventId, LocalDateTime occurredOn) {
    }
}
