package solid.humank.genaidemo.examples.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import solid.humank.genaidemo.examples.order.application.OrderApplicationService;
import solid.humank.genaidemo.examples.order.controller.dto.CreateOrderRequest;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.model.service.OrderProcessingService.OrderProcessingResult;

/**
 * 訂單控制器
 * 處理訂單相關的HTTP請求
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService orderService;
    private final ResponseFactory responseFactory;
    
    public OrderController(
            OrderApplicationService orderService,
            ResponseFactory responseFactory) {
        this.orderService = orderService;
        this.responseFactory = responseFactory;
    }
    
    /**
     * 創建訂單
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return responseFactory.createOrderResponse(order);
    }
    
    /**
     * 處理訂單
     */
    @PostMapping("/{orderId}/process")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable String orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        // 在實際應用中，這裡應該調用 orderService.processOrder(order)
        // 但由於我們重構了代碼，這個方法可能需要重新實現
        return responseFactory.createOrderResponse(order);
    }
    
    /**
     * 獲取訂單
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        return responseFactory.createOrderResponse(order);
    }
    
    /**
     * 獲取所有訂單
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}