package solid.humank.genaidemo.interfaces.web.stats;

import solid.humank.genaidemo.application.stats.service.StatsApplicationService;
import solid.humank.genaidemo.application.stats.StatsDto;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 數據統計控制器
 * 用於驗證數據庫中的數據量
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsApplicationService statsApplicationService;
    
    public StatsController(StatsApplicationService statsApplicationService) {
        this.statsApplicationService = statsApplicationService;
    }

    /**
     * 獲取數據庫統計信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        StatsDto statsDto = statsApplicationService.getStats();
        
        Map<String, Object> response = new HashMap<>(statsDto.stats());
        response.put("status", statsDto.status());
        response.put("message", statsDto.message());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 獲取訂單狀態分布
     */
    @GetMapping("/order-status")
    public ResponseEntity<Map<String, Object>> getOrderStatusStats() {
        OrderStatusStatsDto statsDto = statsApplicationService.getOrderStatusStats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("statusDistribution", statsDto.statusDistribution());
        result.put("status", statsDto.status());
        if (statsDto.message() != null) {
            result.put("message", statsDto.message());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 獲取支付方式分布
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethodStats() {
        PaymentMethodStatsDto statsDto = statsApplicationService.getPaymentMethodStats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("paymentMethodDistribution", statsDto.paymentMethodDistribution());
        result.put("status", statsDto.status());
        if (statsDto.message() != null) {
            result.put("message", statsDto.message());
        }
        
        return ResponseEntity.ok(result);
    }
}
