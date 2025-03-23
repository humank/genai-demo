package solid.humank.genaidemo.examples.order.application;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.controller.dto.ErrorResponse;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService;
import solid.humank.genaidemo.examples.order.service.OrderProcessingService.OrderProcessingResult;

@Service
public class OrderApplicationService {
    private final OrderProcessingService orderProcessingService;

    public OrderApplicationService(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * 處理訂單並轉換為 HTTP 回應
     * 這是一個應用服務，負責協調領域層和基礎設施層
     */
    public ResponseEntity<Object> processOrder(Order order) {
        OrderProcessingResult result = orderProcessingService.process(order);
        
        if (result.success()) {
            return ResponseEntity.ok(OrderResponse.fromDomain(order));
        } else {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(result.errors()));
        }
    }
}
