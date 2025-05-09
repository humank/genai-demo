package solid.humank.genaidemo.domain.order.model.entity;

import solid.humank.genaidemo.domain.order.model.valueobject.Money;

import java.util.UUID;

/**
 * 訂單項實體
 */
public class OrderItem {
    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    private OrderItem(UUID id, UUID productId, String productName, int quantity, Money unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * 創建訂單項
     */
    public static OrderItem create(UUID id, UUID productId, String productName, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (unitPrice == null || unitPrice.isNegativeOrZero()) {
            throw new IllegalArgumentException("Unit price must be greater than 0");
        }
        return new OrderItem(id, productId, productName, quantity, unitPrice);
    }

    /**
     * 計算小計
     */
    public Money getSubtotal() {
        return unitPrice.multiply(quantity);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
    
    /**
     * 獲取單價 (兼容舊代碼)
     */
    public Money getPrice() {
        return unitPrice;
    }
}