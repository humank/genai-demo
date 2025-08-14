package solid.humank.genaidemo.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.stats.repository.StatsRepository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;
import solid.humank.genaidemo.infrastructure.payment.persistence.repository.JpaPaymentRepository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.repository.JpaInventoryRepository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.repository.JpaReservationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 統計數據儲存庫適配器 - 使用 JPA 替代原生 SQL
 * 符合六角形架構的基礎設施層實現
 */
@Repository
public class StatsRepositoryAdapter implements StatsRepository {
    
    private final JpaOrderRepository orderRepository;
    private final JpaPaymentRepository paymentRepository;
    private final JpaInventoryRepository inventoryRepository;
    private final JpaReservationRepository reservationRepository;
    
    public StatsRepositoryAdapter(
            JpaOrderRepository orderRepository,
            JpaPaymentRepository paymentRepository,
            JpaInventoryRepository inventoryRepository,
            JpaReservationRepository reservationRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }
    
    /**
     * 獲取基本統計數據
     */
    public Map<String, Object> getBasicStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 使用 JPA Repository 的 count() 方法
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalOrderItems", orderRepository.countAllOrderItems());
        stats.put("totalPayments", paymentRepository.count());
        stats.put("totalInventories", inventoryRepository.count());
        stats.put("totalReservations", reservationRepository.count());
        
        // 計算總記錄數
        long totalRecords = (Long) stats.get("totalOrders") + 
                           (Long) stats.get("totalOrderItems") + 
                           (Long) stats.get("totalPayments") + 
                           (Long) stats.get("totalInventories") + 
                           (Long) stats.get("totalReservations");
        stats.put("totalRecords", totalRecords);
        
        return stats;
    }
    
    /**
     * 獲取業務統計數據
     */
    public Map<String, Object> getBusinessStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 使用 JPA 查詢方法
        stats.put("totalCompletedOrderValue", orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED));
        stats.put("uniqueCustomers", orderRepository.countDistinctCustomers());
        stats.put("totalAvailableInventory", inventoryRepository.sumAvailableQuantityByStatus(JpaInventoryEntity.InventoryStatusEnum.ACTIVE));
        
        return stats;
    }
    
    /**
     * 獲取訂單狀態分布
     */
    public Map<String, Long> getOrderStatusDistribution() {
        List<Object[]> results = orderRepository.countByStatusGrouped();
        Map<String, Long> statusCounts = new HashMap<>();
        
        for (Object[] result : results) {
            // 處理 OrderStatus 枚舉轉換為字符串
            Object statusObj = result[0];
            String status = statusObj != null ? statusObj.toString() : "UNKNOWN";
            Long count = (Long) result[1];
            statusCounts.put(status, count);
        }
        
        return statusCounts;
    }
    
    /**
     * 獲取支付方式分布
     */
    public Map<String, Long> getPaymentMethodDistribution() {
        List<Object[]> results = paymentRepository.countByPaymentMethodGrouped();
        Map<String, Long> methodCounts = new HashMap<>();
        
        for (Object[] result : results) {
            String paymentMethod = (String) result[0];
            Long count = (Long) result[1];
            methodCounts.put(paymentMethod, count);
        }
        
        return methodCounts;
    }
}
