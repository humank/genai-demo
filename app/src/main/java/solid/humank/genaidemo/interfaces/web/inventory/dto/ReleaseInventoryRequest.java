package solid.humank.genaidemo.interfaces.web.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 釋放庫存請求對象 */
@Schema(description = "釋放庫存請求資料，用於釋放之前預留的庫存，使其重新可用於銷售")
public class ReleaseInventoryRequest {

    @Schema(description = "預留記錄唯一識別碼，用於識別要釋放的庫存預留", example = "RES-001", required = true)
    @NotBlank(message = "預留ID不能為空")
    private String reservationId;

    // 默認構造函數
    public ReleaseInventoryRequest() {}

    // 帶參數的構造函數
    public ReleaseInventoryRequest(String reservationId) {
        this.reservationId = reservationId;
    }

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
}
