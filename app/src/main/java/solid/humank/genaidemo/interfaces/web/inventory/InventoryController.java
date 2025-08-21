package solid.humank.genaidemo.interfaces.web.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.interfaces.web.inventory.dto.AdjustInventoryRequest;

/** 庫存控制器 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "庫存管理", description = "提供庫存查詢、調整等庫存管理相關功能。包含庫存數量查詢、庫存調整操作，支援增加、減少和設定庫存數量。")
public class InventoryController {

    private final InventoryManagementUseCase inventoryService;

    public InventoryController(InventoryManagementUseCase inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 獲取產品庫存信息 */
    @GetMapping("/{productId}")
    @Operation(
            summary = "查詢產品庫存資訊",
            description = "根據產品ID查詢該產品的詳細庫存資訊，包含總庫存、可用庫存、預留庫存、庫存閾值和庫存狀態等資訊。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取庫存資訊",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(implementation = InventoryResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "產品不存在或庫存資訊不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> getInventory(
            @Parameter(description = "產品唯一識別碼，用於查詢該產品的庫存資訊", required = true, example = "PROD-001")
                    @PathVariable
                    String productId) {
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
    @Operation(
            summary = "調整產品庫存",
            description =
                    "對指定產品進行庫存調整操作，支援增加庫存(INCREASE)、減少庫存(DECREASE)或設定庫存(SET)三種調整類型。調整時需要提供調整數量和調整原因。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "庫存調整成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(implementation = InventoryResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效，如調整數量為負數、調整類型不正確等",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "產品不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "業務規則違反，如庫存不足無法減少、調整後庫存為負數等",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> adjustInventory(
            @Parameter(description = "產品唯一識別碼，指定要調整庫存的產品", required = true, example = "PROD-001")
                    @PathVariable
                    String productId,
            @Parameter(description = "庫存調整請求資料，包含調整數量、調整原因和調整類型", required = true) @RequestBody
                    AdjustInventoryRequest request) {

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
