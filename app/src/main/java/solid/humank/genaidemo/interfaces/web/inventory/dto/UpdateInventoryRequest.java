package solid.humank.genaidemo.interfaces.web.inventory.dto;

import jakarta.validation.constraints.Min;

/** 更新庫存請求對象 */
public class UpdateInventoryRequest {

    @Min(value = 0, message = "庫存數量不能小於0")
    private Integer quantity;

    @Min(value = 0, message = "庫存閾值不能小於0")
    private Integer threshold;

    // 默認構造函數
    public UpdateInventoryRequest() {}

    // 帶參數的構造函數
    public UpdateInventoryRequest(Integer quantity, Integer threshold) {
        this.quantity = quantity;
        this.threshold = threshold;
    }

    // Getters and Setters
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
}
