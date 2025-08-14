package solid.humank.genaidemo.interfaces.web.inventory;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.interfaces.web.inventory.dto.AdjustInventoryRequest;

/** 庫存控制器 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryManagementUseCase inventoryService;

    public InventoryController(InventoryManagementUseCase inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 獲取產品庫存信息 */
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getInventory(@PathVariable String productId) {
        Map<String, Object> response = new HashMap<>();

        try {
            InventoryResponse inventory = inventoryService.getInventory(productId);

            response.put("success", true);
            response.put("data", inventory);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取庫存信息時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /** 調整庫存 */
    @PostMapping("/{productId}/adjust")
    public ResponseEntity<Map<String, Object>> adjustInventory(
            @PathVariable String productId, @RequestBody AdjustInventoryRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            AdjustInventoryCommand.AdjustmentType type =
                    AdjustInventoryCommand.AdjustmentType.valueOf(request.getType().toUpperCase());

            AdjustInventoryCommand command =
                    AdjustInventoryCommand.of(
                            productId, request.getQuantity(), request.getReason(), type);

            InventoryResponse adjustedInventory = inventoryService.adjustInventory(command);

            response.put("success", true);
            response.put("data", adjustedInventory);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "調整庫存時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
