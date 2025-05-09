package solid.humank.genaidemo.examples.order.application;

import java.util.List;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.examples.order.controller.dto.CreateOrderRequest;
import solid.humank.genaidemo.examples.order.controller.dto.OrderResponse;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderId;

/**
 * 訂單應用服務
 * 協調領域服務和外部系統
 */
@Service
public class OrderApplicationService {
    
    /**
     * 創建訂單
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 實際應用中會從請求中獲取數據並創建訂單
        // 這裡只是模擬返回一個訂單響應
        return new OrderResponse(
            "order-123",
            request.getCustomerId(),
            request.getShippingAddress(),
            List.of(),
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * 獲取訂單
     */
    public OrderResponse getOrder(String orderId) {
        // 實際應用中會從數據庫中獲取訂單
        // 這裡只是模擬返回一個訂單響應
        return new OrderResponse(
            orderId,
            "customer-123",
            "123 Main St",
            List.of(),
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * 獲取所有訂單
     */
    public List<OrderResponse> getAllOrders() {
        // 實際應用中會從數據庫中獲取所有訂單
        // 這裡只是模擬返回一個訂單列表
        return List.of(
            new OrderResponse("order-123", "customer-123", "123 Main St", List.of(), null, null, null, null),
            new OrderResponse("order-456", "customer-456", "456 Oak St", List.of(), null, null, null, null)
        );
    }
}