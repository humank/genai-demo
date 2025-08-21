package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 客戶個人資料更新事件 當客戶個人資料被更新時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record CustomerProfileUpdatedEvent(
        CustomerId customerId,
        CustomerName newName,
        Email newEmail,
        Phone newPhone,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerProfileUpdatedEvent create(CustomerId customerId, CustomerName newName, Email newEmail,
            Phone newPhone) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerProfileUpdatedEvent(customerId, newName, newEmail, newPhone,
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
        return customerId.getValue();
    }
}
