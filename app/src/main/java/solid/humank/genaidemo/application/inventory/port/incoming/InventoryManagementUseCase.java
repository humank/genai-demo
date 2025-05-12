package solid.humank.genaidemo.application.inventory.port.incoming;

/**
 * 庫存管理用例接口 - 主要輸入端口
 * 定義系統對外提供的所有庫存相關操作
 */
public interface InventoryManagementUseCase {
    /**
     * 檢查庫存是否充足
     * 
     * @param productId 產品ID
     * @param quantity 數量
     * @return 庫存是否充足
     */
    boolean checkInventory(String productId, int quantity);
    
    /**
     * 預留庫存
     * 
     * @param productId 產品ID
     * @param quantity 數量
     * @return 是否成功預留
     */
    boolean reserveInventory(String productId, int quantity);
    
    /**
     * 釋放庫存
     * 
     * @param productId 產品ID
     * @param quantity 數量
     * @return 是否成功釋放
     */
    boolean releaseInventory(String productId, int quantity);
}