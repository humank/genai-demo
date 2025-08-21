package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 客戶消費記錄更新事件 當客戶消費記錄發生變更時發布此事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record CustomerSpendingUpdatedEvent(
        CustomerId customerId,
        Money spendingAmount,
        Money totalSpending,
        String orderId,
        String spendingType, // "PURCHASE", "REFUND", "ADJUSTMENT"
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerSpendingUpdatedEvent create(
            CustomerId customerId,
            Money spendingAmount,
            Money totalSpending,
            String orderId,
            String spendingType) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerSpendingUpdatedEvent(customerId, spendingAmount, totalSpending, orderId, spendingType,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return customerId.getId();
    }
}
