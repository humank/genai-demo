package solid.humank.genaidemo.interfaces.web.inventory.dto;

import jakarta.validation.constraints.NotBlank;

/** 釋放庫存請求對象 */
public class ReleaseInventoryRequest {

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
