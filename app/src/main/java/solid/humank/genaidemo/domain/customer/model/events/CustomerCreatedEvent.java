package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 客戶創建事件 當新客戶被創建時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record CustomerCreatedEvent(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerCreatedEvent create(CustomerId customerId, CustomerName customerName, Email email,
            MembershipLevel membershipLevel) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(customerId, customerName, email, membershipLevel,
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
