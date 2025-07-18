package solid.humank.genaidemo.testutils.builders;

import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 訂單測試資料建構器
 * 使用Builder模式來簡化測試中的訂單資料創建
 */
public class OrderTestDataBuilder {
    
    private String customerId = "test-customer-" + UUID.randomUUID().toString().substring(0, 8);
    private String shippingAddress = "台北市信義區測試地址";
    private final List<OrderItem> items = new ArrayList<>();
    
    private static class OrderItem {
        final String productId;
        final String productName;
        final int quantity;
        final BigDecimal price;
        
        OrderItem(String productId, String productName, int quantity, BigDecimal price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
    }
    
    /**
     * 創建新的訂單建構器
     */
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    /**
     * 設置客戶ID
     */
    public OrderTestDataBuilder withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }
    
    /**
     * 設置配送地址
     */
    public OrderTestDataBuilder withShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }
    
    /**
     * 添加訂單項目
     */
    public OrderTestDataBuilder withItem(String productId, String productName, int quantity, BigDecimal price) {
        this.items.add(new OrderItem(productId, productName, quantity, price));
        return this;
    }
    
    /**
     * 添加訂單項目（使用預設產品ID）
     */
    public OrderTestDataBuilder withItem(String productName, int quantity, BigDecimal price) {
        String productId = "product-" + productName.hashCode();
        return withItem(productId, productName, quantity, price);
    }
    
    /**
     * 添加預設的測試項目
     */
    public OrderTestDataBuilder withDefaultItem() {
        return withItem("iPhone 15", 1, new BigDecimal("35000"));
    }
    
    /**
     * 建構Order領域物件
     */
    public Order build() {
        Order order = new Order(customerId, shippingAddress);
        
        for (OrderItem item : items) {
            order.addItem(item.productId, item.productName, item.quantity, Money.of(item.price));
        }
        
        return order;
    }
    
    /**
     * 建構CreateOrderCommand
     */
    public CreateOrderCommand buildCreateCommand() {
        return new CreateOrderCommand(customerId, shippingAddress);
    }
    
    /**
     * 建構AddOrderItemCommand
     */
    public AddOrderItemCommand buildAddItemCommand(String orderId) {
        if (items.isEmpty()) {
            withDefaultItem();
        }
        
        OrderItem firstItem = items.get(0);
        return AddOrderItemCommand.of(
            orderId, 
            firstItem.productId, 
            firstItem.productName, 
            firstItem.quantity, 
            firstItem.price
        );
    }
    
    /**
     * 建構多個AddOrderItemCommand
     */
    public List<AddOrderItemCommand> buildAddItemCommands(String orderId) {
        List<AddOrderItemCommand> commands = new ArrayList<>();
        
        for (OrderItem item : items) {
            commands.add(AddOrderItemCommand.of(
                orderId, 
                item.productId, 
                item.productName, 
                item.quantity, 
                item.price
            ));
        }
        
        return commands;
    }
    
    /**
     * 計算總金額
     */
    public BigDecimal calculateTotalAmount() {
        return items.stream()
            .map(item -> item.price.multiply(BigDecimal.valueOf(item.quantity)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 獲取項目數量
     */
    public int getItemCount() {
        return items.size();
    }
}