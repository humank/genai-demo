package solid.humank.genaidemo.interfaces.web.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.application.inventory.dto.CreateInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.InventoryCheckResult;
import solid.humank.genaidemo.application.inventory.dto.InventoryDto;
import solid.humank.genaidemo.application.inventory.dto.ReserveInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.ReleaseInventoryCommand;
import solid.humank.genaidemo.application.inventory.service.InventoryApplicationService;
import solid.humank.genaidemo.interfaces.web.inventory.dto.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 庫存控制器
 * 處理與庫存相關的HTTP請求
 */
@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryApplicationService inventoryService;

    public InventoryController(InventoryApplicationService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * 創建庫存
     */
    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        CreateInventoryCommand command = new CreateInventoryCommand(
                request.getProductId(),
                request.getProductName(),
                request.getInitialQuantity(),
                request.getThreshold()
        );
        
        InventoryDto inventoryDto = inventoryService.createInventory(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryResponse.fromDto(inventoryDto));
    }

    /**
     * 獲取所有庫存
     */
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventories() {
        List<InventoryDto> inventories = inventoryService.getAllInventories();
        List<InventoryResponse> responses = inventories.stream()
                .map(InventoryResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 根據ID獲取庫存
     */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable String id) {
        Optional<InventoryDto> inventoryDto = inventoryService.getInventoryById(id);
        return inventoryDto.map(dto -> ResponseEntity.ok(InventoryResponse.fromDto(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根據產品ID獲取庫存
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable String productId) {
        Optional<InventoryDto> inventoryDto = inventoryService.getInventoryByProductId(productId);
        return inventoryDto.map(dto -> ResponseEntity.ok(InventoryResponse.fromDto(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 增加庫存
     */
    @PutMapping("/{productId}/add")
    public ResponseEntity<InventoryResponse> addStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        
        if (request.getQuantity() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        InventoryDto inventoryDto = inventoryService.addStock(productId, request.getQuantity());
        return ResponseEntity.ok(InventoryResponse.fromDto(inventoryDto));
    }

    /**
     * 設置庫存閾值
     */
    @PutMapping("/{productId}/threshold")
    public ResponseEntity<InventoryResponse> setThreshold(
            @PathVariable String productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        
        if (request.getThreshold() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        InventoryDto inventoryDto = inventoryService.setThreshold(productId, request.getThreshold());
        return ResponseEntity.ok(InventoryResponse.fromDto(inventoryDto));
    }

    /**
     * 檢查庫存是否充足
     */
    @GetMapping("/{productId}/check")
    public ResponseEntity<?> checkInventory(
            @PathVariable String productId,
            @RequestParam int quantity) {
        
        InventoryCheckResult result = inventoryService.checkInventoryDetailed(productId, quantity);
        
        if (result.isSufficient()) {
            return ResponseEntity.ok().body(Map.of(
                    "productId", result.getProductId(),
                    "requestedQuantity", result.getRequestedQuantity(),
                    "availableQuantity", result.getAvailableQuantity(),
                    "sufficient", true,
                    "message", result.getMessage()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "productId", result.getProductId(),
                    "requestedQuantity", result.getRequestedQuantity(),
                    "availableQuantity", result.getAvailableQuantity(),
                    "sufficient", false,
                    "message", result.getMessage()
            ));
        }
    }

    /**
     * 預留庫存
     */
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        ReserveInventoryCommand command = new ReserveInventoryCommand(
                request.getProductId(),
                request.getQuantity(),
                UUID.fromString(request.getOrderId())
        );
        
        var result = inventoryService.reserveInventoryDetailed(command);
        ReservationResponse response = ReservationResponse.fromResult(result);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    /**
     * 釋放庫存預留
     */
    @DeleteMapping("/reserve/{reservationId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable String reservationId) {
        boolean released = inventoryService.releaseInventoryDetailed(new ReleaseInventoryCommand(reservationId));
        
        if (released) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 確認庫存預留
     */
    @PostMapping("/reserve/{reservationId}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable String reservationId) {
        boolean confirmed = inventoryService.confirmReservation(reservationId);
        
        if (confirmed) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 獲取低於閾值的庫存
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventories() {
        List<InventoryDto> inventories = inventoryService.getLowStockInventories();
        List<InventoryResponse> responses = inventories.stream()
                .map(InventoryResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 同步庫存
     */
    @PostMapping("/synchronize")
    public ResponseEntity<Void> synchronizeInventory() {
        inventoryService.synchronizeInventory();
        return ResponseEntity.noContent().build();
    }

    /**
     * 通知庫存低於閾值
     */
    @PostMapping("/notify-low-stock")
    public ResponseEntity<Void> notifyLowStock() {
        inventoryService.notifyLowStock();
        return ResponseEntity.noContent().build();
    }
    
    // 用於Map.of方法
    private static class Map {
        public static java.util.Map<String, Object> of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            map.put(k4, v4);
            map.put(k5, v5);
            return map;
        }
    }
}