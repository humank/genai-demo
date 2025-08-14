package solid.humank.genaidemo.application.inventory.port.outgoing;

import java.util.Map;

/** 外部倉庫端口 - 次要輸出端口 定義系統與外部倉庫系統的交互方式 */
public interface ExternalWarehousePort {
    /**
     * 獲取產品庫存數量
     *
     * @param productId 產品ID
     * @return 庫存數量
     */
    int getProductStock(String productId);

    /**
     * 獲取多個產品的庫存數量
     *
     * @param productIds 產品ID列表
     * @return 產品ID到庫存數量的映射
     */
    Map<String, Integer> getProductsStock(Iterable<String> productIds);

    /**
     * 同步庫存數據
     *
     * @return 是否同步成功
     */
    boolean synchronizeInventory();

    /**
     * 通知庫存低於閾值
     *
     * @param productId 產品ID
     * @param currentQuantity 當前數量
     * @param threshold 閾值
     * @return 是否通知成功
     */
    boolean notifyLowStock(String productId, int currentQuantity, int threshold);
}
