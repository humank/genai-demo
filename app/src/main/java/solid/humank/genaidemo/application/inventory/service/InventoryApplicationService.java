package solid.humank.genaidemo.application.inventory.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;

/** 庫存應用服務 */
@Service
@Transactional
public class InventoryApplicationService implements InventoryManagementUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;

    public InventoryApplicationService(InventoryPersistencePort inventoryPersistencePort) {
        this.inventoryPersistencePort = inventoryPersistencePort;
    }

    @Override
    public InventoryResponse adjustInventory(AdjustInventoryCommand command) {
        Optional<Inventory> inventoryOpt =
                inventoryPersistencePort.findByProductId(command.getProductId());

        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException("找不到產品庫存: " + command.getProductId());
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
        return toResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventory(String productId) {
        Optional<Inventory> inventoryOpt = inventoryPersistencePort.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException("找不到產品庫存: " + productId);
        }

        return toResponse(inventoryOpt.get());
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
