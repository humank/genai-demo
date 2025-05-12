package solid.humank.genaidemo.interfaces.web.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import solid.humank.genaidemo.application.order.dto.AddOrderItemRequestDto;
import solid.humank.genaidemo.application.order.dto.CreateOrderRequestDto;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse;

/**
 * 訂單控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderManagementUseCase orderService;

    public OrderController(OrderManagementUseCase orderService) {
        this.orderService = orderService;
    }

    /**
     * 創建訂單
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 將介面層DTO轉換為應用層DTO
        CreateOrderRequestDto dto = CreateOrderRequestDto.from(
            request.getCustomerId(),
            request.getShippingAddress()
        );
        
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.createOrder(dto);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return new ResponseEntity<>(webResponse, HttpStatus.CREATED);
    }

    /**
     * 添加訂單項目
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
            @PathVariable String orderId,
            @RequestBody AddOrderItemRequest request) {
        // 將介面層DTO轉換為應用層DTO
        AddOrderItemRequestDto dto = AddOrderItemRequestDto.from(
            request.getOrderId(),
            request.getProductId(),
            request.getProductName(),
            request.getQuantity(),
            request.getPrice().getAmount()
        );
        
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.addOrderItem(dto);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 提交訂單
     */
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.submitOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.cancelOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 獲取訂單
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.getOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 處理異常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        if (ex.getMessage().contains("not found")) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}