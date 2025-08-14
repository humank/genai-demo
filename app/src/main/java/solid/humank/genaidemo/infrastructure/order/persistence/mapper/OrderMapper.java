package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import java.lang.reflect.Constructor;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderItemEntity;

/** 訂單映射器 負責在領域模型和持久化模型之間進行轉換 */
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
        jpaEntity.setItems(
                order.getItems().stream()
                        .map(
                                item -> {
                                    JpaOrderItemEntity itemEntity = new JpaOrderItemEntity();
                                    itemEntity.setOrderId(order.getId().toString());
                                    itemEntity.setProductId(item.getProductId());
                                    itemEntity.setProductName(item.getProductName());
                                    itemEntity.setQuantity(item.getQuantity());
                                    itemEntity.setPrice(item.getPrice().getAmount());
                                    itemEntity.setCurrency(
                                            item.getPrice().getCurrency().getCurrencyCode());
                                    return itemEntity;
                                })
                        .collect(Collectors.toList()));

        return jpaEntity;
    }

    /**
     * 將持久化模型轉換為領域模型 使用專用的重建建構子來正確重建訂單聚合根
     *
     * @param jpaEntity 訂單持久化模型
     * @return 訂單領域模型
     */
    public static Order toDomainEntity(JpaOrderEntity jpaEntity) {
        try {
            // 創建必要的值對象
            OrderId orderId = OrderId.of(jpaEntity.getId());
            CustomerId customerId = CustomerId.fromString(jpaEntity.getCustomerId());

            // 轉換訂單項
            List<OrderItem> orderItems =
                    jpaEntity.getItems().stream()
                            .map(
                                    item -> {
                                        Money price =
                                                Money.of(
                                                        item.getPrice(),
                                                        Currency.getInstance(item.getCurrency()));
                                        return new OrderItem(
                                                item.getProductId(),
                                                item.getProductName(),
                                                item.getQuantity(),
                                                price);
                                    })
                            .collect(Collectors.toList());

            // 創建金額值對象
            Money totalAmount =
                    Money.of(
                            jpaEntity.getTotalAmount(),
                            Currency.getInstance(jpaEntity.getCurrency()));

            Money effectiveAmount =
                    Money.of(
                            jpaEntity.getEffectiveAmount(),
                            Currency.getInstance(jpaEntity.getCurrency()));

            // 使用反射獲取重建聚合根的建構子
            Constructor<Order> constructor =
                    Order.class.getDeclaredConstructor(
                            OrderId.class,
                            CustomerId.class,
                            String.class,
                            List.class,
                            OrderStatus.class,
                            Money.class,
                            Money.class,
                            java.time.LocalDateTime.class,
                            java.time.LocalDateTime.class);
            constructor.setAccessible(true);

            // 使用建構子重建聚合根
            return constructor.newInstance(
                    orderId,
                    customerId,
                    jpaEntity.getShippingAddress(),
                    orderItems,
                    jpaEntity.getStatus(),
                    totalAmount,
                    effectiveAmount,
                    jpaEntity.getCreatedAt(),
                    jpaEntity.getUpdatedAt());
        } catch (Exception e) {
            // 如果反射失敗，使用備用方法
            return createOrderAlternative(jpaEntity);
        }
    }

    /** 備用方法：當反射方法失敗時使用 */
    private static Order createOrderAlternative(JpaOrderEntity jpaEntity) {
        // 創建基本訂單
        Order order =
                new Order(
                        OrderId.of(jpaEntity.getId()),
                        CustomerId.fromString(jpaEntity.getCustomerId()),
                        jpaEntity.getShippingAddress());

        // 添加訂單項
        jpaEntity
                .getItems()
                .forEach(
                        item -> {
                            Money price =
                                    Money.of(
                                            item.getPrice(),
                                            Currency.getInstance(item.getCurrency()));
                            order.addItem(
                                    item.getProductId(),
                                    item.getProductName(),
                                    item.getQuantity(),
                                    price);
                        });

        // 嘗試使用反射設置其他屬性
        try {
            java.lang.reflect.Field statusField = Order.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(order, jpaEntity.getStatus());

            java.lang.reflect.Field updatedAtField = Order.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(order, jpaEntity.getUpdatedAt());
        } catch (Exception e) {
            // 忽略反射錯誤
        }

        return order;
    }
}
