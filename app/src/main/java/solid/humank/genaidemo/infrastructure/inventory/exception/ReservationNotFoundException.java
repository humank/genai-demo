package solid.humank.genaidemo.infrastructure.inventory.exception;

/**
 * 預留不存在異常
 */
public class ReservationNotFoundException extends InventoryException {
    
    private final String reservationId;
    
    public ReservationNotFoundException(String reservationId) {
        super(String.format("找不到預留 %s", reservationId));
        this.reservationId = reservationId;
    }
    
    public String getReservationId() {
        return reservationId;
    }
}