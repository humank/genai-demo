package solid.humank.genaidemo.domain.order.model.factory;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.factory.DomainFactory;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 訂單工廠
 * 負責創建訂單聚合根
 */
@Component
public class OrderFactory implements DomainFactory<Order, OrderFactory.OrderCreationParams> {
    
    @Override
    public Order create(OrderCreationParams params) {
        // 創建新訂單
        Order order = new Order(
                OrderId.generate(),
                params.customerId,
                params.shippingAddress
        );
        
        // 如果有初始項目，添加到訂單
        if (params.initialItems != null && !params.initialItems.isEmpty()) {
            for (OrderItemParams item : params.initialItems) {
                order.addItem(
                        item.productId,
                        item.productName,
                        item.quantity,
                        item.price
                );
            }
        }
        
        return order;
    }
    
    @Override
    public Order reconstitute(OrderCreationParams params) {
        // 從現有數據重建訂單
        Order order = new Order(
                OrderId.generate(), // 在實際應用中，這裡應該使用來自持久化數據的已有ID
                params.customerId,
                params.shippingAddress
        );
        
        // 添加所有訂單項
        if (params.initialItems != null) {
            for (OrderItemParams item : params.initialItems) {
                order.addItem(
                        item.productId,
                        item.productName,
                        item.quantity,
                        item.price
                );
            }
        }
        
        return order;
    }
    
    /**
     * 訂單創建參數
     */
    public static class OrderCreationParams {
        private final String customerId;
        private final String shippingAddress;
        private final List<OrderItemParams> initialItems;
        
        public OrderCreationParams(String customerId, String shippingAddress) {
            this(customerId, shippingAddress, Collections.emptyList());
        }
        
        public OrderCreationParams(String customerId, String shippingAddress, List<OrderItemParams> initialItems) {
            this.customerId = customerId;
            this.shippingAddress = shippingAddress;
            this.initialItems = initialItems != null ? 
                    Collections.unmodifiableList(new ArrayList<>(initialItems)) : 
                    Collections.emptyList();
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public String getShippingAddress() {
            return shippingAddress;
        }
        
        public List<OrderItemParams> getInitialItems() {
            return initialItems;
        }
    }
    
    /**
     * 訂單項參數
     */
    public static class OrderItemParams {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final Money price;
        
        public OrderItemParams(String productId, String productName, int quantity, Money price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public Money getPrice() {
            return price;
        }
    }
}