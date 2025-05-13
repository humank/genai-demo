package solid.humank.genaidemo.application.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.inventory.dto.*;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.application.inventory.port.outgoing.ExternalWarehousePort;
import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 庫存應用服務
 * 實現庫存管理用例接口
 */
@Service
public class InventoryApplicationService implements InventoryManagementUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;
    private final ExternalWarehousePort externalWarehousePort;

    public InventoryApplicationService(InventoryPersistencePort inventoryPersistencePort, 
                                      ExternalWarehousePort externalWarehousePort) {
        this.inventoryPersistencePort = inventoryPersistencePort;
        this.externalWarehousePort = externalWarehousePort;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkInventory(String productId, int quantity) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        return inventoryOpt.map(inventory -> inventory.isSufficient(quantity)).orElse(false);
    }

    @Override
    @Transactional
    public boolean reserveInventory(String productId, int quantity) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        if (!inventory.isSufficient(quantity)) {
            return false;
        }

        try {
            inventory.reserve(UUID.randomUUID(), quantity);
            inventoryPersistencePort.save(inventory);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean releaseInventory(String productId, int quantity) {
        // 在實際應用中，我們需要知道具體的預留ID
        // 這裡簡化處理，假設每個產品只有一個預留
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        Map<ReservationId, Integer> reservations = inventory.getReservations();
        
        // 找到第一個匹配數量的預留
        Optional<ReservationId> reservationIdOpt = reservations.entrySet().stream()
                .filter(entry -> entry.getValue() == quantity)
                .map(Map.Entry::getKey)
                .findFirst();
                
        if (reservationIdOpt.isEmpty()) {
            return false;
        }
        
        try {
            inventory.releaseReservation(reservationIdOpt.get());
            inventoryPersistencePort.save(inventory);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 創建新庫存
     */
    @Transactional
    public InventoryDto createInventory(CreateInventoryCommand command) {
        // 檢查是否已存在相同產品ID的庫存
        Optional<Inventory> existingInventory = inventoryPersistencePort.findByProductId(command.getProductId());
        if (existingInventory.isPresent()) {
            throw new IllegalArgumentException("產品ID已存在庫存記錄: " + command.getProductId());
        }

        // 創建新庫存
        Inventory inventory = new Inventory(
                command.getProductId(),
                command.getProductName(),
                command.getInitialQuantity()
        );
        
        // 設置閾值
        if (command.getThreshold() > 0) {
            inventory.setThreshold(command.getThreshold());
        }
        
        // 保存庫存
        inventory = inventoryPersistencePort.save(inventory);
        
        // 返回DTO
        return InventoryDto.fromDomain(inventory);
    }

    /**
     * 獲取所有庫存
     */
    @Transactional(readOnly = true)
    public List<InventoryDto> getAllInventories() {
        return inventoryPersistencePort.findAll().stream()
                .map(InventoryDto::fromDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根據產品ID獲取庫存
     */
    @Transactional(readOnly = true)
    public Optional<InventoryDto> getInventoryByProductId(String productId) {
        return inventoryPersistencePort.findByProductId(productId)
                .map(InventoryDto::fromDomain);
    }

    /**
     * 根據ID獲取庫存
     */
    @Transactional(readOnly = true)
    public Optional<InventoryDto> getInventoryById(String inventoryId) {
        return inventoryPersistencePort.findById(InventoryId.fromString(inventoryId))
                .map(InventoryDto::fromDomain);
    }

    /**
     * 增加庫存
     */
    @Transactional
    public InventoryDto addStock(String productId, int quantity) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("找不到產品庫存: " + productId);
        }

        Inventory inventory = inventoryOpt.get();
        inventory.addStock(quantity);
        inventory = inventoryPersistencePort.save(inventory);
        
        return InventoryDto.fromDomain(inventory);
    }

    /**
     * 設置庫存閾值
     */
    @Transactional
    public InventoryDto setThreshold(String productId, int threshold) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("找不到產品庫存: " + productId);
        }

        Inventory inventory = inventoryOpt.get();
        inventory.setThreshold(threshold);
        inventory = inventoryPersistencePort.save(inventory);
        
        return InventoryDto.fromDomain(inventory);
    }

    /**
     * 檢查庫存是否充足
     */
    @Transactional(readOnly = true)
    public InventoryCheckResult checkInventoryDetailed(String productId, int quantity) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);
        if (inventoryOpt.isEmpty()) {
            return InventoryCheckResult.notFound(productId);
        }

        Inventory inventory = inventoryOpt.get();
        if (inventory.isSufficient(quantity)) {
            return InventoryCheckResult.sufficient(productId, quantity, inventory.getAvailableQuantity());
        } else {
            return InventoryCheckResult.insufficient(productId, quantity, inventory.getAvailableQuantity());
        }
    }

    /**
     * 預留庫存
     */
    @Transactional
    public ReservationResult reserveInventoryDetailed(ReserveInventoryCommand command) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(command.getProductId());
        if (inventoryOpt.isEmpty()) {
            return ReservationResult.failure(command.getProductId(), command.getQuantity(), "找不到產品庫存");
        }

        Inventory inventory = inventoryOpt.get();
        if (!inventory.isSufficient(command.getQuantity())) {
            return ReservationResult.failure(
                    command.getProductId(),
                    command.getQuantity(),
                    String.format("庫存不足，需要 %d，但只有 %d", command.getQuantity(), inventory.getAvailableQuantity())
            );
        }

        try {
            ReservationId reservationId = inventory.reserve(command.getOrderId(), command.getQuantity());
            inventory = inventoryPersistencePort.save(inventory);
            return ReservationResult.success(command.getProductId(), command.getQuantity(), reservationId.toString());
        } catch (Exception e) {
            return ReservationResult.failure(command.getProductId(), command.getQuantity(), e.getMessage());
        }
    }

    /**
     * 釋放庫存
     */
    @Transactional
    public boolean releaseInventoryDetailed(ReleaseInventoryCommand command) {
        ReservationId reservationId = ReservationId.fromString(command.getReservationId());
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByReservationId(reservationId);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        inventory.releaseReservation(reservationId);
        inventoryPersistencePort.save(inventory);
        return true;
    }

    /**
     * 確認庫存預留
     */
    @Transactional
    public boolean confirmReservation(String reservationId) {
        ReservationId resId = ReservationId.fromString(reservationId);
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByReservationId(resId);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        inventory.confirmReservation(resId);
        inventoryPersistencePort.save(inventory);
        return true;
    }

    /**
     * 同步庫存
     */
    @Transactional
    public void synchronizeInventory() {
        // 獲取所有庫存
        List<Inventory> inventories = inventoryPersistencePort.findAll();
        
        // 獲取所有產品ID
        List<String> productIds = inventories.stream()
                .map(Inventory::getProductId)
                .collect(Collectors.toList());
                
        // 從外部倉庫獲取最新庫存數據
        Map<String, Integer> externalStocks = externalWarehousePort.getProductsStock(productIds);
        
        // 更新本地庫存
        for (Inventory inventory : inventories) {
            String productId = inventory.getProductId();
            if (externalStocks.containsKey(productId)) {
                int newQuantity = externalStocks.get(productId);
                try {
                    inventory.synchronize(newQuantity);
                    inventoryPersistencePort.save(inventory);
                } catch (IllegalArgumentException e) {
                    // 如果新數量小於已預留數量，則忽略
                    // 實際應用中可能需要更複雜的處理
                }
            }
        }
    }

    /**
     * 獲取低於閾值的庫存
     */
    @Transactional(readOnly = true)
    public List<InventoryDto> getLowStockInventories() {
        return inventoryPersistencePort.findBelowThreshold().stream()
                .map(InventoryDto::fromDomain)
                .collect(Collectors.toList());
    }

    /**
     * 通知庫存低於閾值
     */
    @Transactional
    public void notifyLowStock() {
        List<Inventory> lowStockInventories = inventoryPersistencePort.findBelowThreshold();
        for (Inventory inventory : lowStockInventories) {
            externalWarehousePort.notifyLowStock(
                    inventory.getProductId(),
                    inventory.getAvailableQuantity(),
                    inventory.getThreshold()
            );
        }
    }
}