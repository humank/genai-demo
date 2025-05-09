package solid.humank.genaidemo.infrastructure.order.acl;

import java.util.Map;

/**
 * 外部物流系統介面
 * 定義與外部物流系統的互動協議
 */
public interface ExternalLogisticsSystem {
    /**
     * 創建配送單
     * @param deliveryData 配送資料
     * @return 配送單號
     */
    String createDelivery(Map<String, String> deliveryData);

    /**
     * 查詢配送狀態
     * @param referenceNo 訂單參考號
     * @return 配送狀態
     */
    String getDeliveryStatus(String referenceNo);
}
