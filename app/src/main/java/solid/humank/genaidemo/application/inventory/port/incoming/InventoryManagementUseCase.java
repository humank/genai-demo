package solid.humank.genaidemo.application.inventory.port.incoming;

import solid.humank.genaidemo.application.inventory.dto.command.AdjustInventoryCommand;
import solid.humank.genaidemo.application.inventory.dto.response.InventoryResponse;

/**
 * 庫存管理用例接口
 */
public interface InventoryManagementUseCase {
    
    /**
     * 調整庫存
     */
    InventoryResponse adjustInventory(AdjustInventoryCommand command);
    
    /**
     * 獲取產品庫存信息
     */
    InventoryResponse getInventory(String productId);
}