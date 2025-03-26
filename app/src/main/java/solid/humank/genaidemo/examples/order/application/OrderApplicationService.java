package solid.humank.genaidemo.examples.order.application;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;
import solid.humank.genaidemo.utils.Preconditions;

/**
 * 訂單應用服務
 * 負責協調領域層和基礎設施層，不涉及 HTTP 響應和外部通信格式
 */
@Service
public class OrderApplicationService {
    private final OrderProcessingService orderProcessingService;

    public OrderApplicationService(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * 處理訂單
     * 應用服務只返回領域相關的結果，不涉及 HTTP 響應格式
     * 
     * @param order 要處理的訂單
     * @return 處理結果
     * @throws IllegalArgumentException 如果訂單為空
     */
    public OrderProcessingResult processOrder(Order order) {
        // 前置條件檢查
        Preconditions.requireNonNull(order, "訂單不能為空");
        
        return orderProcessingService.process(order);
    }
}
