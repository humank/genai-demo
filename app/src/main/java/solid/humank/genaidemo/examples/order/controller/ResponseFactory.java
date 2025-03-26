package solid.humank.genaidemo.examples.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.controller.dto.ErrorResponse;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;
import solid.humank.genaidemo.utils.Preconditions;

/**
 * 響應工廠
 * 專門負責將領域結果轉換為 HTTP 響應格式
 * 移除應用服務對 HTTP 響應的耦合
 */
@Component
public class ResponseFactory {

    /**
     * 為領域處理結果創建 HTTP 響應
     *
     * @param result 處理結果
     * @param order 訂單實體
     * @return HTTP 響應
     */
    public ResponseEntity<Object> createOrderProcessingResponse(OrderProcessingResult result, Order order) {
        // 前置條件檢查
        Preconditions.requireNonNull(result, "處理結果不能為空");
        
        if (result.success()) {
            Preconditions.requireNonNull(order, "成功響應需要有效的訂單");
            return ResponseEntity.ok(OrderResponse.fromDomain(order));
        } else {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(result.errors()));
        }
    }
    
    /**
     * 為訂單創建成功響應
     *
     * @param order 訂單實體
     * @return HTTP 響應
     */
    public ResponseEntity<OrderResponse> createOrderResponse(Order order) {
        Preconditions.requireNonNull(order, "訂單不能為空");
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }
}
