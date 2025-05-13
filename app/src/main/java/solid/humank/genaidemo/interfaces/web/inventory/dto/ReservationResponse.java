package solid.humank.genaidemo.interfaces.web.inventory.dto;

import solid.humank.genaidemo.application.inventory.dto.ReservationResult;

/**
 * 庫存預留響應對象
 */
public class ReservationResponse {
    private String productId;
    private int quantity;
    private boolean success;
    private String reservationId;
    private String message;

    // 默認構造函數
    public ReservationResponse() {
    }

    // 帶參數的構造函數
    public ReservationResponse(String productId, int quantity, boolean success, String reservationId, String message) {
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.reservationId = reservationId;
        this.message = message;
    }

    /**
     * 從應用層結果創建響應對象
     */
    public static ReservationResponse fromResult(ReservationResult result) {
        return new ReservationResponse(
                result.getProductId(),
                result.getQuantity(),
                result.isSuccess(),
                result.getReservationId(),
                result.getMessage()
        );
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