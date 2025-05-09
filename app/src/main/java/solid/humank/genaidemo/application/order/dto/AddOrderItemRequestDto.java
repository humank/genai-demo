package solid.humank.genaidemo.application.order.dto;

import solid.humank.genaidemo.domain.order.model.valueobject.Money;

/**
 * 添加訂單項目請求DTO
 * 應用層使用的數據傳輸對象，避免直接依賴介面層
 */
public class AddOrderItemRequestDto {
    private String orderId;
    private String productId;
    private String productName;
    private int quantity;
    private Money price;

    public AddOrderItemRequestDto() {
    }

    public AddOrderItemRequestDto(String orderId, String productId, String productName, int quantity, Money price) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    /**
     * 從通用參數創建應用層DTO
     */
    public static AddOrderItemRequestDto from(String orderId, String productId, String productName, int quantity, Money price) {
        return new AddOrderItemRequestDto(orderId, productId, productName, quantity, price);
    }
}