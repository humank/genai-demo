package solid.humank.genaidemo.domain.inventory.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.repository.InventoryRepository;

/**
 * 庫存領域服務
 * 處理庫存相關的複雜業務邏輯和跨聚合根操作
 */
@DomainService(name = "InventoryDomainService", description = "庫存領域服務，處理庫存管理的複雜業務邏輯", boundedContext = "Inventory")
public class InventoryDomainService {

    private final InventoryRepository inventoryRepository;

    public InventoryDomainService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 檢查庫存是否充足
     * 
     * @param productId        產品ID
     * @param requiredQuantity 需要的數量
     * @return 是否有足夠庫存
     */
    public boolean isStockSufficient(String productId, int requiredQuantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        if (inventory == null) {
            return false;
        }
        return inventory.getAvailableQuantity() >= requiredQuantity;
    }

    /**
     * 預留庫存
     * 為訂單預留指定數量的庫存
     * 
     * @param productId 產品ID
     * @param quantity  預留數量
     * @param orderId   訂單ID
     * @return 是否預留成功
     */
    public boolean reserveStock(String productId, int quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        if (inventory == null || inventory.getAvailableQuantity() < quantity) {
            return false;
        }

        // 使用預留功能
        inventory.reserve(java.util.UUID.fromString(orderId), quantity);
        inventoryRepository.save(inventory);
        return true;
    }

    /**
     * 釋放預留庫存
     * 當訂單取消時釋放預留的庫存
     * 
     * @param productId 產品ID
     * @param quantity  釋放數量
     * @param orderId   訂單ID
     */
    public void releaseReservedStock(String productId, int quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        if (inventory != null) {
            // 簡化實現 - 增加庫存
            inventory.addStock(quantity);
            inventoryRepository.save(inventory);
        }
    }

    /**
     * 確認庫存消耗
     * 當訂單完成時確認消耗庫存
     * 
     * @param productId 產品ID
     * @param quantity  消耗數量
     * @param orderId   訂單ID
     */
    public void confirmStockConsumption(String productId, int quantity, String orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        if (inventory != null) {
            // 簡化實現 - 庫存已在預留時減少，這裡不需要額外操作
            inventoryRepository.save(inventory);
        }
    }

    /**
     * 補充庫存
     * 
     * @param productId 產品ID
     * @param quantity  補充數量
     * @param reason    補充原因
     */
    public void replenishStock(String productId, int quantity, String reason) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        if (inventory != null) {
            // 簡化實現 - 增加庫存數量
            inventory.addStock(quantity);
            inventoryRepository.save(inventory);
        }
    }

    /**
     * 檢查低庫存警告
     * 
     * @param productId 產品ID
     * @return 是否需要補貨
     */
    public boolean needsReplenishment(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        // 簡化實現 - 當庫存少於 10 時需要補貨
        return inventory != null && inventory.getAvailableQuantity() < 10;
    }
}