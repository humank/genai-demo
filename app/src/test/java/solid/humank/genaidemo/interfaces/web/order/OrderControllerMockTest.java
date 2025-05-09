package solid.humank.genaidemo.interfaces.web.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.application.order.dto.AddOrderItemRequestDto;
import solid.humank.genaidemo.application.order.dto.CreateOrderRequestDto;
import solid.humank.genaidemo.application.order.dto.OrderResponse;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderStatus;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 訂單控制器單元測試
 * 使用 Mockito 進行單元測試，不啟動 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerMockTest {

    @Mock
    private OrderManagementUseCase orderManagementUseCase;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("創建訂單應返回201狀態碼和訂單詳情")
    void createOrderShouldReturn201AndOrderDetails() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("customer-123", "台北市信義區");
        String orderId = UUID.randomUUID().toString();
        
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                "customer-123",
                "台北市信義區",
                Collections.emptyList(),
                Money.zero(),
                OrderStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(orderManagementUseCase.createOrder(any(CreateOrderRequestDto.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponse> response = orderController.createOrder(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().getOrderId());
        assertEquals("customer-123", response.getBody().getCustomerId());
        assertEquals("台北市信義區", response.getBody().getShippingAddress());
    }

    @Test
    @DisplayName("添加訂單項目應返回200狀態碼和更新後的訂單")
    void addOrderItemShouldReturn200AndUpdatedOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        AddOrderItemRequest request = new AddOrderItemRequest(
                orderId,
                "product-1",
                "iPhone 15",
                1,
                Money.of(new BigDecimal("30000"))
        );
        
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                "customer-123",
                "台北市信義區",
                Collections.singletonList(
                    OrderResponse.OrderItemResponse.fromOrderItem(
                        new solid.humank.genaidemo.domain.order.model.valueobject.OrderItem(
                            "product-1", 
                            "iPhone 15", 
                            1, 
                            Money.of(new BigDecimal("30000"))
                        )
                    )
                ),
                Money.of(new BigDecimal("30000")),
                OrderStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(orderManagementUseCase.addOrderItem(any(AddOrderItemRequestDto.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponse> response = orderController.addOrderItem(orderId, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().getOrderId());
        assertEquals(1, response.getBody().getItems().size());
        assertEquals("iPhone 15", response.getBody().getItems().get(0).getProductName());
    }

    @Test
    @DisplayName("提交訂單應返回200狀態碼和更新後的訂單狀態")
    void submitOrderShouldReturn200AndUpdatedOrderStatus() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                "customer-123",
                "台北市信義區",
                Collections.emptyList(),
                Money.zero(),
                OrderStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(orderManagementUseCase.submitOrder(orderId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponse> response = orderController.submitOrder(orderId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().getOrderId());
        assertEquals(OrderStatus.PENDING, response.getBody().getStatus());
    }

    @Test
    @DisplayName("取消訂單應返回200狀態碼和已取消的訂單狀態")
    void cancelOrderShouldReturn200AndCancelledOrderStatus() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                "customer-123",
                "台北市信義區",
                Collections.emptyList(),
                Money.zero(),
                OrderStatus.CANCELLED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(orderManagementUseCase.cancelOrder(orderId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponse> response = orderController.cancelOrder(orderId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().getOrderId());
        assertEquals(OrderStatus.CANCELLED, response.getBody().getStatus());
    }

    @Test
    @DisplayName("獲取訂單應返回200狀態碼和訂單詳情")
    void getOrderShouldReturn200AndOrderDetails() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        
        OrderResponse mockResponse = new OrderResponse(
                orderId,
                "customer-123",
                "台北市信義區",
                Collections.emptyList(),
                Money.zero(),
                OrderStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(orderManagementUseCase.getOrder(orderId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponse> response = orderController.getOrder(orderId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().getOrderId());
        assertEquals("customer-123", response.getBody().getCustomerId());
    }

    @Test
    @DisplayName("獲取不存在的訂單應返回404狀態碼")
    void getNonExistentOrderShouldReturn404() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String errorMessage = "Order not found: " + orderId;
        RuntimeException exception = new RuntimeException(errorMessage);
        
        // Act
        ResponseEntity<String> response = orderController.handleException(exception);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
    }
}