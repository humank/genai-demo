package solid.humank.genaidemo.application.stats.service;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.application.stats.StatsDto;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import solid.humank.genaidemo.domain.stats.repository.StatsRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * 統計應用服務 - 重構為使用 JPA
 * 符合六角形架構的應用層服務，依賴於領域層的儲存庫接口
 */
@Service
public class StatsApplicationService {
    
    private final StatsRepository statsRepository;
    
    public StatsApplicationService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }
    
    /**
     * 獲取數據庫統計信息
     * 使用 JPA Repository 方法替代原生 SQL
     */
    public StatsDto getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 獲取基本統計數據
            Map<String, Object> basicStats = statsRepository.getBasicStats();
            stats.putAll(basicStats);
            
            // 獲取業務統計數據
            Map<String, Object> businessStats = statsRepository.getBusinessStats();
            stats.putAll(businessStats);
            
            return new StatsDto(stats, "success", "數據統計獲取成功");
            
        } catch (Exception e) {
            Map<String, Object> errorStats = new HashMap<>();
            return new StatsDto(errorStats, "error", "獲取統計數據時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取訂單狀態分布
     * 使用 JPA 查詢方法替代原生 SQL
     */
    public OrderStatusStatsDto getOrderStatusStats() {
        try {
            Map<String, Long> statusCounts = statsRepository.getOrderStatusDistribution();
            
            // 轉換 Long 為 Integer (保持與原有 DTO 的兼容性)
            Map<String, Integer> integerStatusCounts = new HashMap<>();
            statusCounts.forEach((status, count) -> 
                integerStatusCounts.put(status, count.intValue()));
            
            return new OrderStatusStatsDto(integerStatusCounts, "success", null);
            
        } catch (Exception e) {
            return new OrderStatusStatsDto(new HashMap<>(), "error", 
                "獲取訂單狀態統計時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取支付方式分布
     * 使用 JPA 查詢方法替代原生 SQL
     */
    public PaymentMethodStatsDto getPaymentMethodStats() {
        try {
            Map<String, Long> methodCounts = statsRepository.getPaymentMethodDistribution();
            
            // 轉換 Long 為 Integer (保持與原有 DTO 的兼容性)
            Map<String, Integer> integerMethodCounts = new HashMap<>();
            methodCounts.forEach((method, count) -> 
                integerMethodCounts.put(method, count.intValue()));
            
            return new PaymentMethodStatsDto(integerMethodCounts, "success", null);
            
        } catch (Exception e) {
            return new PaymentMethodStatsDto(new HashMap<>(), "error", 
                "獲取支付方式統計時發生錯誤: " + e.getMessage());
        }
    }
}