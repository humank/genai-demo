package solid.humank.genaidemo.application.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 庫存預留結果 */
@Schema(description = "庫存預留操作結果，包含預留狀態和相關資訊")
public class ReservationResult {
    @Schema(description = "產品唯一識別碼", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String productId;

    @Schema(description = "預留數量", example = "5", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int quantity;

    @Schema(description = "預留操作是否成功", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "預留記錄唯一識別碼，成功時返回", example = "RES-001")
    private final String reservationId;

    @Schema(description = "操作結果訊息，包含成功或失敗的詳細說明", example = "庫存預留成功", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String message;

    private ReservationResult(
            String productId, int quantity, boolean success, String reservationId, String message) {
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.reservationId = reservationId;
        this.message = message;
    }

    public static ReservationResult success(String productId, int quantity, String reservationId) {
        return new ReservationResult(productId, quantity, true, reservationId, "庫存預留成功");
    }

    public static ReservationResult failure(String productId, int quantity, String message) {
        return new ReservationResult(productId, quantity, false, null, message);
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getMessage() {
        return message;
    }
}
