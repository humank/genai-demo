package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 配送地址添加事件
 * 當客戶添加新的配送地址時發布此事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record DeliveryAddressAddedEvent(
        CustomerId customerId,
        Address address,
        int totalAddressCount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static DeliveryAddressAddedEvent create(
            CustomerId customerId, Address address, int totalAddressCount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new DeliveryAddressAddedEvent(customerId, address, totalAddressCount,
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
