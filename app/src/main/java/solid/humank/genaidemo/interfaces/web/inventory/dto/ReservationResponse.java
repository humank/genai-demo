package solid.humank.genaidemo.interfaces.web.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import solid.humank.genaidemo.application.inventory.dto.ReservationResult;

/** 庫存預留響應對象 */
@Schema(description = "庫存預留操作回應資料，包含預留結果和相關資訊")
public class ReservationResponse {
    @Schema(description = "產品唯一識別碼", example = "PROD-001", required = true)
    private String productId;

    @Schema(description = "預留數量", example = "5", minimum = "1", required = true)
    private int quantity;

    @Schema(description = "預留操作是否成功", example = "true", required = true)
    private boolean success;

    @Schema(description = "預留記錄唯一識別碼，成功時返回", example = "RES-001")
    private String reservationId;

    @Schema(description = "操作結果訊息，包含成功或失敗的詳細說明", example = "庫存預留成功", required = true)
    private String message;

    // 默認構造函數
    public ReservationResponse() {}

    // 帶參數的構造函數
    public ReservationResponse(
            String productId, int quantity, boolean success, String reservationId, String message) {
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.reservationId = reservationId;
        this.message = message;
    }

    /** 從應用層結果創建響應對象 */
    public static ReservationResponse fromResult(ReservationResult result) {
        return new ReservationResponse(
                result.getProductId(),
                result.getQuantity(),
                result.isSuccess(),
                result.getReservationId(),
                result.getMessage());
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
