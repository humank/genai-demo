package solid.humank.genaidemo.examples.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.application.OrderApplicationService;
import solid.humank.genaidemo.examples.order.controller.dto.AddOrderItemRequest;
import solid.humank.genaidemo.examples.order.controller.dto.CreateOrderRequest;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 前置條件檢查：防禦性編程
        Objects.requireNonNull(request, "請求內容不能為空");
        
        Order order = new Order(request.customerId());
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
        @PathVariable String orderId,
        @RequestBody AddOrderItemRequest request
    ) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        Objects.requireNonNull(request, "請求內容不能為空");
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        
        Order order = new Order(orderId);
        order.addItem(
            request.productId(),
            request.productName(),
            request.quantity(),
            Money.twd(request.unitPrice().doubleValue())
        );
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }

    @PostMapping("/{orderId}/process")
    public ResponseEntity<Object> processOrder(@PathVariable String orderId) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        
        Order order = new Order(orderId);
        OrderProcessingResult result = orderApplicationService.processOrder(order);
        return OrderApplicationService.createResponse(result, order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        
        Order order = new Order(orderId);
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }
}
