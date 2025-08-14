package solid.humank.genaidemo.application.logistics.port.incoming;

/** 物流管理用例接口 - 主要輸入端口 定義系統對外提供的所有物流相關操作 */
public interface LogisticsManagementUseCase {
    /**
     * 創建配送訂單
     *
     * @param orderId 訂單ID
     * @return 配送訂單ID
     */
    String createDeliveryOrder(String orderId);

    /**
     * 分配配送資源
     *
     * @param deliveryOrderId 配送訂單ID
     * @return 是否成功分配
     */
    boolean allocateResources(String deliveryOrderId);

    /**
     * 執行配送
     *
     * @param deliveryOrderId 配送訂單ID
     * @return 是否成功配送
     */
    boolean executeDelivery(String deliveryOrderId);

    /**
     * 確認送達
     *
     * @param deliveryOrderId 配送訂單ID
     * @return 是否成功送達
     */
    boolean confirmDelivery(String deliveryOrderId);
}
