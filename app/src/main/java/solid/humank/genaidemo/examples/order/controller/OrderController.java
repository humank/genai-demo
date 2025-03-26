package solid.humank.genaidemo.examples.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.application.OrderApplicationService;
import solid.humank.genaidemo.examples.order.controller.dto.AddOrderItemRequest;
import solid.humank.genaidemo.examples.order.controller.dto.CreateOrderRequest;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;
import solid.humank.genaidemo.utils.Preconditions;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    // 常量定義，避免字符串重複
    private static final String ORDER_ID_REQUIRED = "訂單ID不能為空";
    private static final String REQUEST_REQUIRED = "請求內容不能為空";
    private final OrderApplicationService orderApplicationService;
    private final ResponseFactory responseFactory;

    public OrderController(OrderApplicationService orderApplicationService, ResponseFactory responseFactory) {
        this.orderApplicationService = orderApplicationService;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 前置條件檢查：防禦性編程
        Preconditions.requireNonNull(request, REQUEST_REQUIRED);
        
        Order order = new Order(request.customerId(), request.shippingAddress());
        return responseFactory.createOrderResponse(order);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
        @PathVariable String orderId,
        @RequestBody AddOrderItemRequest request
    ) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        Preconditions.requireNonNull(request, REQUEST_REQUIRED);
        Preconditions.requireNonBlank(orderId, ORDER_ID_REQUIRED);
        
        Order order = new Order(orderId);
        order.addItem(
            request.productId(),
            request.productName(),
            request.quantity(),
            request.unitPrice()
        );
        return responseFactory.createOrderResponse(order);
    }

    @PostMapping("/{orderId}/process")
    public ResponseEntity<Object> processOrder(@PathVariable String orderId) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        Preconditions.requireNonBlank(orderId, ORDER_ID_REQUIRED);
        
        Order order = new Order(orderId);
        OrderProcessingResult result = orderApplicationService.processOrder(order);
        return responseFactory.createOrderProcessingResponse(result, order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        // 在實際應用中，這裡應該要從資料庫取得訂單
        // 這裡簡化實作，只建立新訂單
        
        // 前置條件檢查：防禦性編程
        Preconditions.requireNonBlank(orderId, ORDER_ID_REQUIRED);
        
        Order order = new Order(orderId);
        return responseFactory.createOrderResponse(order);
    }
}
