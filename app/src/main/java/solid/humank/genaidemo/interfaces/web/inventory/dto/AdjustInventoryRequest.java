package solid.humank.genaidemo.interfaces.web.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 調整庫存請求 DTO */
@Schema(description = "庫存調整請求資料，用於指定庫存調整的相關參數")
public class AdjustInventoryRequest {
    @Schema(
            description = "調整數量，必須為正整數。對於INCREASE和DECREASE類型，表示增加或減少的數量；對於SET類型，表示設定的目標數量",
            example = "10",
            minimum = "1",
            maximum = "999999",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "調整數量不能為空")
    @Min(value = 1, message = "調整數量必須大於0")
    private int quantity;

    @Schema(
            description = "調整原因，用於記錄庫存調整的業務原因，便於後續追蹤和審計",
            example = "補貨入庫",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "調整原因不能為空")
    private String reason;

    @Schema(
            description = "調整類型，指定庫存調整的操作方式。INCREASE: 增加庫存，DECREASE: 減少庫存，SET: 設定庫存為指定數量",
            allowableValues = {"INCREASE", "DECREASE", "SET"},
            example = "INCREASE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "調整類型不能為空")
    @Pattern(regexp = "^(INCREASE|DECREASE|SET)$", message = "調整類型必須為 INCREASE、DECREASE 或 SET")
    private String type;

    public AdjustInventoryRequest() {}

    public AdjustInventoryRequest(int quantity, String reason, String type) {
        this.quantity = quantity;
        this.reason = reason;
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
