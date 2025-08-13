package solid.humank.genaidemo.interfaces.web.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 數據統計控制器
 * 用於驗證數據庫中的數據量
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private DataSource dataSource;

    /**
     * 獲取數據庫統計信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 統計訂單數量
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalOrders", rs.getInt(1));
                }
            }
            
            // 統計訂單項目數量
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM order_items")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalOrderItems", rs.getInt(1));
                }
            }
            
            // 統計支付記錄數量
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM payments")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalPayments", rs.getInt(1));
                }
            }
            
            // 統計庫存記錄數量
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM inventories")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalInventories", rs.getInt(1));
                }
            }
            
            // 統計庫存預留記錄數量
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM inventory_reservations")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalReservations", rs.getInt(1));
                }
            }
            
            // 計算總記錄數
            int totalRecords = (Integer) stats.get("totalOrders") + 
                              (Integer) stats.get("totalOrderItems") + 
                              (Integer) stats.get("totalPayments") + 
                              (Integer) stats.get("totalInventories") + 
                              (Integer) stats.get("totalReservations");
            stats.put("totalRecords", totalRecords);
            
            // 添加一些業務統計
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(total_amount) FROM orders WHERE status = 'COMPLETED'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalCompletedOrderValue", rs.getBigDecimal(1));
                }
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(DISTINCT customer_id) FROM orders")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("uniqueCustomers", rs.getInt(1));
                }
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(available_quantity) FROM inventories WHERE status = 'ACTIVE'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.put("totalAvailableInventory", rs.getInt(1));
                }
            }
            
            stats.put("status", "success");
            stats.put("message", "數據統計獲取成功");
            
        } catch (Exception e) {
            stats.put("status", "error");
            stats.put("message", "獲取統計數據時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 獲取訂單狀態分布
     */
    @GetMapping("/order-status")
    public ResponseEntity<Map<String, Object>> getOrderStatusStats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> statusCounts = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT status, COUNT(*) FROM orders GROUP BY status")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    statusCounts.put(rs.getString(1), rs.getInt(2));
                }
            }
            
            result.put("statusDistribution", statusCounts);
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取訂單狀態統計時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 獲取支付方式分布
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethodStats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> methodCounts = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT payment_method, COUNT(*) FROM payments GROUP BY payment_method")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    methodCounts.put(rs.getString(1), rs.getInt(2));
                }
            }
            
            result.put("paymentMethodDistribution", methodCounts);
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取支付方式統計時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
