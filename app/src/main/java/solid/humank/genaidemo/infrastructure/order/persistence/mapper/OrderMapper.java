package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderItemEntity;

import java.util.Currency;
import java.util.stream.Collectors;

/**
 * 訂單映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
public class OrderMapper {
    
    /**
     * 將領域模型轉換為持久化模型
     * 
     * @param order 訂單領域模型
     * @return 訂單持久化模型
     */
    public static JpaOrderEntity toJpaEntity(Order order) {
        JpaOrderEntity jpaEntity = new JpaOrderEntity();
        jpaEntity.setId(order.getId().toString());
        jpaEntity.setCustomerId(order.getCustomerIdAsString());
        jpaEntity.setShippingAddress(order.getShippingAddress());
        jpaEntity.setStatus(order.getStatus());
        jpaEntity.setTotalAmount(order.getTotalAmount().getAmount());
        jpaEntity.setEffectiveAmount(order.getEffectiveAmount().getAmount());
        jpaEntity.setCurrency(order.getTotalAmount().getCurrency().getCurrencyCode());
        jpaEntity.setCreatedAt(order.getCreatedAt());
        jpaEntity.setUpdatedAt(order.getUpdatedAt());
        
        // 轉換訂單項
        jpaEntity.setItems(order.getItems().stream()
                .map(item -> {
                    JpaOrderItemEntity itemEntity = new JpaOrderItemEntity();
                    itemEntity.setOrderId(order.getId().toString());
                    itemEntity.setProductId(item.getProductId());
                    itemEntity.setProductName(item.getProductName());
                    itemEntity.setQuantity(item.getQuantity());
                    itemEntity.setPrice(item.getPrice().getAmount());
                    itemEntity.setCurrency(item.getPrice().getCurrency().getCurrencyCode());
                    return itemEntity;
                })
                .collect(Collectors.toList()));
        
        return jpaEntity;
    }
    
    /**
     * 將持久化模型轉換為領域模型
     * 注意：這裡使用了一個簡化的方法來重建訂單聚合根
     * 在實際應用中，可能需要更複雜的重建邏輯
     * 
     * @param jpaEntity 訂單持久化模型
     * @return 訂單領域模型
     */
    public static Order toDomainEntity(JpaOrderEntity jpaEntity) {
        // 創建訂單聚合根
        Order order = new Order(
                OrderId.of(jpaEntity.getId()),
                CustomerId.fromString(jpaEntity.getCustomerId()),
                jpaEntity.getShippingAddress()
        );
        
        // 添加訂單項
        jpaEntity.getItems().forEach(item -> {
            Money price = Money.of(
                    item.getPrice(),
                    Currency.getInstance(item.getCurrency())
            );
            order.addItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    price
            );
        });
        
        // 注意：這裡我們沒有處理訂單狀態和其他屬性的重建
        // 在實際應用中，可能需要更複雜的重建邏輯，例如：
        // - 設置訂單狀態
        // - 處理訂單歷史記錄
        // - 處理訂單事件
        
        return order;
    }
}