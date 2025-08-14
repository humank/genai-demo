package solid.humank.genaidemo.application.inventory.service;

import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;
import solid.humank.genaidemo.application.inventory.port.incoming.InventoryManagementUseCase;
import solid.humank.genaidemo.infrastructure.inventory.persistence.repository.JpaInventoryRepository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 庫存應用服務
 */
@Service
@Transactional
public class InventoryApplicationService implements InventoryManagementUseCase {
    
    private final JpaInventoryRepository inventoryRepository;
    
    public InventoryApplicationService(JpaInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }
    
    @Override
    public InventoryResponse adjustInventory(AdjustInventoryCommand command) {
        Optional<JpaInventoryEntity> inventoryOpt = inventoryRepository.findByProductId(command.getProductId());
        
        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException("找不到產品庫存: " + command.getProductId());
        }
        
        JpaInventoryEntity inventory = inventoryOpt.get();
        
        // 根據調整類型更新庫存
        switch (command.getType()) {
            case INCREASE:
                inventory.setTotalQuantity(inventory.getTotalQuantity() + command.getQuantity());
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + command.getQuantity());
                break;
            case DECREASE:
                int newTotal = Math.max(0, inventory.getTotalQuantity() - command.getQuantity());
                int newAvailable = Math.max(0, inventory.getAvailableQuantity() - command.getQuantity());
                inventory.setTotalQuantity(newTotal);
                inventory.setAvailableQuantity(newAvailable);
                break;
            case SET:
                inventory.setTotalQuantity(command.getQuantity());
                inventory.setAvailableQuantity(command.getQuantity() - inventory.getReservedQuantity());
                break;
        }
        
        inventory.setUpdatedAt(LocalDateTime.now());
        
        JpaInventoryEntity savedInventory = inventoryRepository.save(inventory);
        return toResponse(savedInventory);
    }
    
    @Override
    public InventoryResponse getInventory(String productId) {
        Optional<JpaInventoryEntity> inventoryOpt = inventoryRepository.findByProductId(productId);
        
        if (inventoryOpt.isEmpty()) {
            throw new RuntimeException("找不到產品庫存: " + productId);
        }
        
        return toResponse(inventoryOpt.get());
    }
    
    private InventoryResponse toResponse(JpaInventoryEntity entity) {
        return new InventoryResponse(
            entity.getId().toString(),
            entity.getProductId(),
            entity.getProductName(),
            entity.getTotalQuantity(),
            entity.getAvailableQuantity(),
            entity.getReservedQuantity(),
            entity.getThreshold(),
            entity.getStatus().name(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}