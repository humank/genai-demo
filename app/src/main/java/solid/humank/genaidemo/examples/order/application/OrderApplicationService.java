package solid.humank.genaidemo.examples.order.application;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.controller.dto.ErrorResponse;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;

/**
 * 訂單應用服務
 * 負責協調領域層和基礎設施層，但不直接處理 HTTP 響應格式
 */
@Service
public class OrderApplicationService {
    private final OrderProcessingService orderProcessingService;

    public OrderApplicationService(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * 處理訂單
     * 應用服務應該返回領域相關的結果，而不是直接處理 HTTP 響應格式
     * 
     * @param order 要處理的訂單
     * @return 處理結果
     * @throws IllegalArgumentException 如果訂單為空
     */
    public OrderProcessingResult processOrder(Order order) {
        // 前置條件檢查
        if (order == null) {
            throw new IllegalArgumentException("訂單不能為空");
        }
        
        return orderProcessingService.process(order);
    }
    
    /**
     * 創建 HTTP 響應
     * 這個靜態方法用於轉換領域結果為 HTTP 響應
     * 將這個方法分離出來使得應用服務不直接處理 HTTP 相關邏輯
     * 
     * @param result 訂單處理結果
     * @param order 訂單實體
     * @return HTTP 響應
     * @throws IllegalArgumentException 如果結果或訂單為空
     */
    public static ResponseEntity<Object> createResponse(OrderProcessingResult result, Order order) {
        // 前置條件檢查
        if (result == null) {
            throw new IllegalArgumentException("處理結果不能為空");
        }
        
        if (result.success()) {
            if (order == null) {
                throw new IllegalArgumentException("成功響應需要有效的訂單");
            }
            return ResponseEntity.ok(OrderResponse.fromDomain(order));
        } else {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(result.errors()));
        }
    }
}
