package solid.humank.genaidemo.examples.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.examples.order.controller.dto.ErrorResponse;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.model.service.OrderProcessingService.OrderProcessingResult;

/**
 * 響應工廠
 * 負責創建HTTP響應
 */
@Component
public class ResponseFactory {
    
    /**
     * 創建錯誤響應
     */
    public ResponseEntity<ErrorResponse> createErrorResponse(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
    
    /**
     * 創建訂單處理響應
     */
    public ResponseEntity<Object> createOrderProcessingResponse(OrderProcessingResult result, OrderResponse order) {
        if (!result.success()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(result.errors()));
        }
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(order);
    }
    
    /**
     * 創建訂單響應
     */
    public ResponseEntity<OrderResponse> createOrderResponse(OrderResponse order) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(order);
    }
    
    /**
     * 創建訂單列表響應
     */
    public ResponseEntity<List<OrderResponse>> createOrderListResponse(List<OrderResponse> orders) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orders);
    }
}