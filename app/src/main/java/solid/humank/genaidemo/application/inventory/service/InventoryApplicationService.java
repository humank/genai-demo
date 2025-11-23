package solid.humank.genaidemo.application.inventory.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.common.service.DomainEventApplicationService;
import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.exceptions.InsufficientInventoryException;

/** 庫存應用服務 */
@Service
@Transactional
public class InventoryApplicationService implements InventoryManagementUseCase {

    private static final String PRODUCT_NOT_FOUND_MSG = "Product not found with ID: ";

    private final InventoryPersistencePort inventoryPersistencePort;
    private final DomainEventApplicationService domainEventApplicationService;

    public InventoryApplicationService(
            InventoryPersistencePort inventoryPersistencePort,
            DomainEventApplicationService domainEventApplicationService) {
        this.inventoryPersistencePort = inventoryPersistencePort;
        this.domainEventApplicationService = domainEventApplicationService;
    }

    @Override
    public InventoryResponse adjustInventory(AdjustInventoryCommand command) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(command.getProductId());

        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException(PRODUCT_NOT_FOUND_MSG + command.getProductId());
        }

        Inventory inventory = inventoryOpt.get();

        // 根據調整類型更新庫存
        switch (command.getType()) {
            case INCREASE:
                inventory.addStock(command.getQuantity());
                break;
            case DECREASE:
                // 對於減少庫存，我們需要在領域模型中添加相應的方法
                // 這裡暫時使用同步方法來實現
                int newTotal = Math.max(0, inventory.getTotalQuantity() - command.getQuantity());
                inventory.synchronize(newTotal);
                break;
            case SET:
                // 對於設置庫存，使用同步方法
                inventory.synchronize(command.getQuantity());
                break;
        }

        Inventory savedInventory = inventoryPersistencePort.save(inventory);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(savedInventory);

        return toResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventory(String productId) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException(PRODUCT_NOT_FOUND_MSG + productId);
        }

        return toResponse(inventoryOpt.get());
    }

    /**
     * 預留庫存
     *
     * @param productId 商品ID
     * @param quantity  預留數量
     * @param orderId   訂單ID
     */
    public void reserveInventory(String productId, int quantity, String orderId) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException(PRODUCT_NOT_FOUND_MSG + productId);
        }

        Inventory inventory = inventoryOpt.get();

        // 檢查是否有足夠的可用庫存
        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientInventoryException(String.format("庫存不足 - 商品ID: %s, 需要: %d, 可用: %d",
                    productId, quantity, inventory.getAvailableQuantity()));
        }

        // 預留庫存 - 這裡需要在領域模型中添加預留方法
        // 暫時使用現有的方法來模擬預留操作
        // 實際實現中應該在 Inventory 聚合根中添加 reserve 方法

        // 保存庫存變更
        Inventory savedInventory = inventoryPersistencePort.save(inventory);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(savedInventory);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId().getId().toString(),
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getThreshold(),
                inventory.getStatus().name(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt());
    }
}
