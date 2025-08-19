package solid.humank.genaidemo.domain.product.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;

/**
 * 商品創建事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record ProductCreatedEvent(
        ProductId productId,
        ProductName productName,
        Money price,
        ProductCategory category,
        UUID eventId,
        LocalDateTime occurredOn)
        implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static ProductCreatedEvent create(
            ProductId productId, ProductName productName, Money price, ProductCategory category) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ProductCreatedEvent(productId, productName, price, category,
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
        return productId.getId();
    }
}
