package solid.humank.genaidemo.application.inventory.dto;

/**
 * 庫存預留結果
 */
public class ReservationResult {
    private final String productId;
    private final int quantity;
    private final boolean success;
    private final String reservationId;
    private final String message;

    private ReservationResult(String productId, int quantity, boolean success, String reservationId, String message) {
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.reservationId = reservationId;
        this.message = message;
    }

    public static ReservationResult success(String productId, int quantity, String reservationId) {
        return new ReservationResult(
                productId,
                quantity,
                true,
                reservationId,
                "庫存預留成功"
        );
    }

    public static ReservationResult failure(String productId, int quantity, String message) {
        return new ReservationResult(
                productId,
                quantity,
                false,
                null,
                message
        );
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