package solid.humank.genaidemo.infrastructure.inventory.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** 庫存異常處理器 處理庫存相關的異常 */
@ControllerAdvice
public class InventoryExceptionHandler {

    /** 處理庫存不足異常 */
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientInventoryException(
            InsufficientInventoryException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "insufficient_inventory");
        response.put("message", ex.getMessage());
        response.put("productId", ex.getProductId());
        response.put("requestedQuantity", ex.getRequestedQuantity());
        response.put("availableQuantity", ex.getAvailableQuantity());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /** 處理庫存不存在異常 */
    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryNotFoundException(
            InventoryNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "inventory_not_found");
        response.put("message", ex.getMessage());
        response.put("productId", ex.getProductId());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** 處理預留不存在異常 */
    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleReservationNotFoundException(
            ReservationNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "reservation_not_found");
        response.put("message", ex.getMessage());
        response.put("reservationId", ex.getReservationId());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** 處理一般庫存異常 */
    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryException(InventoryException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "inventory_error");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
