package solid.humank.genaidemo.domain.product.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 商品價格變更事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record ProductPriceChangedEvent(
        ProductId productId,
        Money oldPrice,
        Money newPrice,
        UUID eventId,
        LocalDateTime occurredOn)
        implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static ProductPriceChangedEvent create(ProductId productId, Money oldPrice, Money newPrice) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ProductPriceChangedEvent(productId, oldPrice, newPrice,
                metadata.eventId(), metadata.occurredOn());
    }

    // 向後兼容的構造函數
    public ProductPriceChangedEvent(ProductId productId, Money oldPrice, Money newPrice) {
        this(productId, oldPrice, newPrice, UUID.randomUUID(), LocalDateTime.now());
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
