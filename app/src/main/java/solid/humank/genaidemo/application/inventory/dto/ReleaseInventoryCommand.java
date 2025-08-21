package solid.humank.genaidemo.application.inventory.dto;

/** 釋放庫存命令 */
public class ReleaseInventoryCommand {
    private final String reservationId;

    public ReleaseInventoryCommand(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }
}
