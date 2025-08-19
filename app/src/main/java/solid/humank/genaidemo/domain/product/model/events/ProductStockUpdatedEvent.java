package solid.humank.genaidemo.domain.product.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity;

/**
 * 商品庫存更新事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record ProductStockUpdatedEvent(
        ProductId productId,
        StockQuantity oldStock,
        StockQuantity newStock,
        UUID eventId,
        LocalDateTime occurredOn)
        implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static ProductStockUpdatedEvent create(
            ProductId productId, StockQuantity oldStock, StockQuantity newStock) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ProductStockUpdatedEvent(productId, oldStock, newStock,
                metadata.eventId(), metadata.occurredOn());
    }

    // 向後兼容的構造函數
    public ProductStockUpdatedEvent(
            ProductId productId, StockQuantity oldStock, StockQuantity newStock) {
        this(productId, oldStock, newStock, UUID.randomUUID(), LocalDateTime.now());
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
