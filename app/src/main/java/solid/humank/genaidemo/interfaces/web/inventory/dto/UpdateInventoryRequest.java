package solid.humank.genaidemo.interfaces.web.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/** 更新庫存請求對象 */
@Schema(description = "更新庫存請求資料，用於修改現有庫存的數量和閾值設定")
public class UpdateInventoryRequest {

    @Schema(description = "更新後的庫存數量，必須為非負整數", example = "150", minimum = "0")
    @Min(value = 0, message = "庫存數量不能小於0")
    private Integer quantity;

    @Schema(description = "更新後的庫存警戒閾值，當可用庫存低於此值時需要補貨", example = "15", minimum = "0")
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
