package solid.humank.genaidemo.interfaces.web.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse;

/**
 * 響應工廠
 * 負責創建HTTP響應
 */
@Component
public class ResponseFactory {
    
    /**
     * 創建錯誤響應
     */
    public ResponseEntity<ApiErrorResponse> createErrorResponse(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(message));
    }
    
    /**
     * 創建訂單處理響應
     */
    public ResponseEntity<Object> createOrderProcessingResponse(boolean success, List<String> errors, OrderResponse order) {
        if (!success) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(errors));
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
    
    /**
     * API錯誤響應
     * 在響應工廠中定義，避免跨層依賴
     */
    public static class ApiErrorResponse {
        private final List<String> errors;

        public ApiErrorResponse(String error) {
            this.errors = List.of(error);
        }

        public ApiErrorResponse(List<String> errors) {
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}