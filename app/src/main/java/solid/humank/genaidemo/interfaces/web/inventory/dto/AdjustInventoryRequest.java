package solid.humank.genaidemo.interfaces.web.inventory.dto;

/**
 * 調整庫存請求 DTO
 */
public class AdjustInventoryRequest {
    private int quantity;
    private String reason;
    private String type; // INCREASE, DECREASE, SET

    public AdjustInventoryRequest() {
    }

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