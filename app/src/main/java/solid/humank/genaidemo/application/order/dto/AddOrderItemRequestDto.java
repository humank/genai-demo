package solid.humank.genaidemo.application.order.dto;

import java.math.BigDecimal;

/**
 * 添加訂單項請求 DTO
 */
public class AddOrderItemRequestDto {
    private String orderId;
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private String currency;

    public AddOrderItemRequestDto() {
        this.currency = "TWD"; // 默認貨幣為新台幣
    }

    /**
     * 從參數創建 DTO
     * 
     * @param orderId 訂單ID
     * @param productId 產品ID
     * @param productName 產品名稱
     * @param quantity 數量
     * @param unitPrice 單價
     * @return 添加訂單項請求 DTO
     */
    public static AddOrderItemRequestDto from(
            String orderId, 
            String productId, 
            String productName, 
            int quantity, 
            BigDecimal unitPrice) {
        AddOrderItemRequestDto dto = new AddOrderItemRequestDto();
        dto.setOrderId(orderId);
        dto.setProductId(productId);
        dto.setProductName(productName);
        dto.setQuantity(quantity);
        dto.setUnitPrice(unitPrice);
        return dto;
    }

    /**
     * 從參數創建 DTO
     * 
     * @param orderId 訂單ID
     * @param productId 產品ID
     * @param productName 產品名稱
     * @param quantity 數量
     * @param unitPrice 單價
     * @param currency 貨幣
     * @return 添加訂單項請求 DTO
     */
    public static AddOrderItemRequestDto from(
            String orderId, 
            String productId, 
            String productName, 
            int quantity, 
            BigDecimal unitPrice,
            String currency) {
        AddOrderItemRequestDto dto = from(orderId, productId, productName, quantity, unitPrice);
        dto.setCurrency(currency);
        return dto;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getPrice() {
        return unitPrice;
    }

    public void setPrice(BigDecimal price) {
        this.unitPrice = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}