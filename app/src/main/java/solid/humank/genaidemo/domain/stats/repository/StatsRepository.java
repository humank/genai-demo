package solid.humank.genaidemo.domain.stats.repository;

import java.util.Map;

/**
 * 統計數據儲存庫接口
 */
public interface StatsRepository {
    
    /**
     * 獲取基本統計數據
     */
    Map<String, Object> getBasicStats();
    
    /**
     * 獲取業務統計數據
     */
    Map<String, Object> getBusinessStats();
    
    /**
     * 獲取訂單狀態分布
     */
    Map<String, Long> getOrderStatusDistribution();
    
    /**
     * 獲取支付方式分布
     */
    Map<String, Long> getPaymentMethodDistribution();
}