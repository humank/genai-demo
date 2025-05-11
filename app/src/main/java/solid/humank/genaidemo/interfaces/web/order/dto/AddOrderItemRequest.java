package solid.humank.genaidemo.interfaces.web.order.dto;

import java.math.BigDecimal;

import solid.humank.genaidemo.domain.common.valueobject.Money;

/**
 * 添加訂單項請求 DTO
 */
public record AddOrderItemRequest(
    String orderId,
    String productId,
    String productName,
    int quantity,
    Money price
) {
    public String getOrderId() {
        return orderId;
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
    
    public Money unitPrice() {
        return getPrice();
    }
    
    // 通過BigDecimal創建AddOrderItemRequest的工廠方法
    public static AddOrderItemRequest of(String orderId, String productId, String productName, int quantity, BigDecimal price) {
        return new AddOrderItemRequest(orderId, productId, productName, quantity, Money.of(price));
    }
}