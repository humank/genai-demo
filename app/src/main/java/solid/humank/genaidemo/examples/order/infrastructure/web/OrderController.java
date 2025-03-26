package solid.humank.genaidemo.examples.order.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.examples.order.application.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.examples.order.controller.dto.AddOrderItemRequest;
import solid.humank.genaidemo.examples.order.controller.dto.CreateOrderRequest;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;

/**
 * 訂單控制器 - Primary Adapter
 * 處理HTTP請求並轉發給應用服務
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderManagementUseCase orderManagementUseCase;

    public OrderController(OrderManagementUseCase orderManagementUseCase) {
        this.orderManagementUseCase = orderManagementUseCase;
    }

    /**
     * 創建新訂單
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse response = orderManagementUseCase.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 添加訂單項
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
            @PathVariable String orderId,
            @RequestBody AddOrderItemRequest request) {
        // 確保請求中包含訂單ID
        if (request.getOrderId() == null) {
            // 創建一個新的請求對象，包含訂單ID
            request = AddOrderItemRequest.of(
                    orderId,
                    request.getProductId(),
                    request.getProductName(),
                    request.getQuantity(),
                    request.getPrice().getAmount()
            );
        }
        
        OrderResponse response = orderManagementUseCase.addOrderItem(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 提交訂單
     */
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String orderId) {
        OrderResponse response = orderManagementUseCase.submitOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        OrderResponse response = orderManagementUseCase.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取訂單
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse response = orderManagementUseCase.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 處理訂單不存在異常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        if (ex.getMessage().contains("Order not found")) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
