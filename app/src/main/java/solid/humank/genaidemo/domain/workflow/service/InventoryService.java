package solid.humank.genaidemo.domain.workflow.service;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.util.List;
import java.util.Map;

/**
 * 庫存服務接口
 * 定義與庫存相關的操作
 */
public interface InventoryService {
    
    /**
     * 檢查庫存
     *
     * @param orderId 訂單ID
     * @param productIds 產品ID列表
     * @return 庫存是否充足
     */
    boolean checkInventory(OrderId orderId, List<String> productIds);
    
    /**
     * 預留庫存
     *
     * @param orderId 訂單ID
     * @param productIds 產品ID列表
     * @return 是否預留成功
     */
    boolean reserveInventory(OrderId orderId, List<String> productIds);
    
    /**
     * 釋放庫存
     *
     * @param orderId 訂單ID
     * @return 是否釋放成功
     */
    boolean releaseInventory(OrderId orderId);
    
    /**
     * 獲取庫存不足的商品
     *
     * @param orderId 訂單ID
     * @return 庫存不足的商品列表
     */
    List<String> getOutOfStockProducts(OrderId orderId);
    
    /**
     * 獲取替代商品建議
     *
     * @param productIds 產品ID列表
     * @return 替代商品建議，鍵為原商品ID，值為替代商品ID
     */
    Map<String, String> getAlternativeProducts(List<String> productIds);
}