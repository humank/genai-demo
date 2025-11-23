package solid.humank.genaidemo.interfaces.web.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 創建庫存請求對象 */
@Schema(description = "創建庫存請求資料，用於為新產品建立庫存記錄")
public class CreateInventoryRequest {

    @Schema(description = "產品唯一識別碼", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "產品ID不能為空")
    private String productId;

    @Schema(description = "產品名稱", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "產品名稱不能為空")
    private String productName;

    @Schema(description = "初始庫存數量，必須為非負整數", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "初始庫存數量不能為空")
    @Min(value = 0, message = "初始庫存數量不能小於0")
    private Integer initialQuantity;

    @Schema(description = "庫存警戒閾值，當可用庫存低於此值時需要補貨", example = "10", minimum = "0")
    @Min(value = 0, message = "庫存閾值不能小於0")
    private Integer threshold = 0;

    // 默認構造函數
    public CreateInventoryRequest() {}

    // 帶參數的構造函數
    public CreateInventoryRequest(
            String productId, String productName, Integer initialQuantity, Integer threshold) {
        this.productId = productId;
        this.productName = productName;
        this.initialQuantity = initialQuantity;
        this.threshold = threshold;
    }

    // Getters and Setters
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

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
}
