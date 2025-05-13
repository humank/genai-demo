package solid.humank.genaidemo.interfaces.web.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 預留庫存請求對象
 */
public class ReserveInventoryRequest {
    
    @NotBlank(message = "產品ID不能為空")
    private String productId;
    
    @NotNull(message = "數量不能為空")
    @Min(value = 1, message = "數量必須大於0")
    private Integer quantity;
    
    @NotBlank(message = "訂單ID不能為空")
    private String orderId;

    // 默認構造函數
    public ReserveInventoryRequest() {
    }

    // 帶參數的構造函數
    public ReserveInventoryRequest(String productId, Integer quantity, String orderId) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderId = orderId;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}