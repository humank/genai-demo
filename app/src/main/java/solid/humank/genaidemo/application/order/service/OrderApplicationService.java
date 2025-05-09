package solid.humank.genaidemo.application.order.service;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.application.order.port.outgoing.LogisticsServicePort;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;
import solid.humank.genaidemo.application.order.dto.OrderResponse;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.factory.OrderFactory;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

/**
 * 訂單應用服務
 * 實現訂單管理用例，協調各個端口的交互
 */
@Service
public class OrderApplicationService implements OrderManagementUseCase {
    private final OrderPersistencePort orderPersistencePort;
    private final PaymentServicePort paymentServicePort;
    private final LogisticsServicePort logisticsServicePort;
    private final OrderFactory orderFactory;

    public OrderApplicationService(
            OrderPersistencePort orderPersistencePort,
            PaymentServicePort paymentServicePort,
            LogisticsServicePort logisticsServicePort,
            OrderFactory orderFactory) {
        this.orderPersistencePort = orderPersistencePort;
        this.paymentServicePort = paymentServicePort;
        this.logisticsServicePort = logisticsServicePort;
        this.orderFactory = orderFactory;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 創建訂單
        OrderFactory.OrderCreationParams params = new OrderFactory.OrderCreationParams(
                request.getCustomerId(), 
                request.getShippingAddress()
        );
        
        Order order = orderFactory.create(params);
        
        // 持久化訂單
        orderPersistencePort.save(order);
        
        // 返回響應
        return OrderResponse.fromDomain(order);
    }

    @Override
    public OrderResponse addOrderItem(AddOrderItemRequest request) {
        // 查找訂單
        OrderId orderId = new OrderId(request.getOrderId());
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // 添加訂單項
        order.addItem(
                request.getProductId(), 
                request.getProductName(), 
                request.getQuantity(), 
                request.getPrice()
        );
        
        // 更新訂單
        orderPersistencePort.update(order);
        
        // 返回響應
        return OrderResponse.fromDomain(order);
    }

    @Override
    public OrderResponse submitOrder(String orderId) {
        // 查找訂單
        OrderId id = new OrderId(orderId);
        Order order = orderPersistencePort.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // 處理支付
        paymentServicePort.processPayment(order.getId(), order.getTotalAmount());
        
        // 創建物流訂單
        logisticsServicePort.createDeliveryOrder(order.getId());
        
        // 更新訂單狀態
        order.submit();
        orderPersistencePort.update(order);
        
        // 返回響應
        return OrderResponse.fromDomain(order);
    }

    @Override
    public OrderResponse cancelOrder(String orderId) {
        // 查找訂單
        OrderId id = new OrderId(orderId);
        Order order = orderPersistencePort.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // 取消支付
        paymentServicePort.cancelPayment(order.getId());
        
        // 取消物流
        logisticsServicePort.cancelDeliveryOrder(order.getId());
        
        // 更新訂單狀態
        order.cancel();
        orderPersistencePort.update(order);
        
        // 返回響應
        return OrderResponse.fromDomain(order);
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        // 查找訂單
        OrderId id = new OrderId(orderId);
        Order order = orderPersistencePort.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // 返回響應
        return OrderResponse.fromDomain(order);
    }
}