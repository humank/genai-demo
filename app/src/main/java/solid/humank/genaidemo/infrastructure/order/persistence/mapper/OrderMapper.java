package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 訂單映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
public class OrderMapper {

    /**
     * 將領域模型轉換為持久化模型
     */
    public static JpaOrderEntity toJpaEntity(Order order) {
        if (order == null) {
            return null;
        }
        
        JpaOrderEntity jpaEntity = new JpaOrderEntity();
        jpaEntity.setId(order.getId().toString());
        jpaEntity.setCustomerId(order.getCustomerId());
        jpaEntity.setShippingAddress(order.getShippingAddress());
        jpaEntity.setStatus(order.getStatus());
        jpaEntity.setTotalAmount(order.getTotalAmount().getAmount());
        jpaEntity.setCurrency(order.getTotalAmount().getCurrency());
        jpaEntity.setEffectiveAmount(order.getEffectiveAmount().getAmount());
        jpaEntity.setCreatedAt(order.getCreatedAt());
        jpaEntity.setUpdatedAt(order.getUpdatedAt());
        
        // 轉換訂單項
        List<JpaOrderItemEntity> jpaItems = order.getItems().stream()
                .map(item -> {
                    JpaOrderItemEntity jpaItem = new JpaOrderItemEntity();
                    jpaItem.setOrderId(order.getId().toString());
                    jpaItem.setProductId(item.getProductId());
                    jpaItem.setProductName(item.getProductName());
                    jpaItem.setQuantity(item.getQuantity());
                    jpaItem.setPrice(item.getPrice().getAmount());
                    jpaItem.setCurrency(item.getPrice().getCurrency());
                    return jpaItem;
                })
                .collect(Collectors.toList());
        
        jpaEntity.setItems(jpaItems);
        
        return jpaEntity;
    }

    /**
     * 將持久化模型轉換為領域模型
     */
    public static Order toDomainEntity(JpaOrderEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        // 創建訂單
        Order order = new Order(
                OrderId.fromString(jpaEntity.getId()),
                jpaEntity.getCustomerId(),
                jpaEntity.getShippingAddress()
        );
        
        // 添加訂單項
        for (JpaOrderItemEntity jpaItem : jpaEntity.getItems()) {
            order.addItem(
                    jpaItem.getProductId(),
                    jpaItem.getProductName(),
                    jpaItem.getQuantity(),
                    Money.of(jpaItem.getPrice(), jpaItem.getCurrency())
            );
        }
        
        // 根據狀態設置訂單狀態
        // 注意：這裡需要根據實際的訂單狀態流程進行調整
        switch (jpaEntity.getStatus()) {
            case PENDING:
                order.submit();
                break;
            case CONFIRMED:
                order.submit();
                order.confirm();
                break;
            case PAID:
                order.submit();
                order.confirm();
                order.markAsPaid();
                break;
            case SHIPPING:
                order.submit();
                order.confirm();
                order.markAsPaid();
                order.ship();
                break;
            case DELIVERED:
                order.submit();
                order.confirm();
                order.markAsPaid();
                order.ship();
                order.deliver();
                break;
            case CANCELLED:
                order.cancel();
                break;
            default:
                // CREATED 狀態不需要額外處理
                break;
        }
        
        // 如果有折扣，應用折扣
        Money totalAmount = Money.of(jpaEntity.getTotalAmount(), jpaEntity.getCurrency());
        Money effectiveAmount = Money.of(jpaEntity.getEffectiveAmount(), jpaEntity.getCurrency());
        
        if (!totalAmount.equals(effectiveAmount)) {
            Money discountAmount = totalAmount.subtract(effectiveAmount);
            order.applyDiscount(discountAmount);
        }
        
        return order;
    }
}