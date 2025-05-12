package solid.humank.genaidemo.interfaces.web.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
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
        // 創建命令對象
        CreateOrderCommand command = CreateOrderCommand.of(
            request.getCustomerId(),
            request.getShippingAddress()
        );
        
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.createOrder(command);
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
        // 創建命令對象
        AddOrderItemCommand command = AddOrderItemCommand.of(
            orderId,
            request.getProductId(),
            request.getProductName(),
            request.getQuantity(),
            request.getPrice().getAmount()
        );
        
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.addOrderItem(command);
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