package solid.humank.genaidemo.examples.order;

import java.util.List;

import solid.humank.genaidemo.ddd.annotations.DomainService;
import solid.humank.genaidemo.ddd.factories.DomainFactory;

@DomainService
public class OrderFactory implements DomainFactory<Order, OrderFactory.OrderCreationParams> {
    
    public record OrderCreationParams(
        String customerId,
        List<OrderItemParams> items
    ) {
        public static OrderCreationParams of(String customerId, List<OrderItemParams> items) {
            return new OrderCreationParams(customerId, items);
        }
    }

    public record OrderItemParams(
        String productId,
        String productName,
        int quantity,
        Money unitPrice
    ) {}

    @Override
    public Order create(OrderCreationParams params) {
        Order order = new Order(params.customerId());
        
        params.items().forEach(item -> 
            order.addItem(
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPrice()
            )
        );
        
        return order;
    }

    @Override
    public Order reconstitute(OrderCreationParams params) {
        // 這個方法通常用於從持久化資料重建物件
        // 在這個簡單的例子中，我們先使用與 create 相同的邏輯
        return create(params);
    }

    // 提供便利的建立方法
    public Order createOrder(String customerId, OrderItemParams... items) {
        return create(OrderCreationParams.of(customerId, List.of(items)));
    }
}
