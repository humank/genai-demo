package solid.humank.genaidemo.application.order.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.application.order.dto.AddOrderItemRequestDto;
import solid.humank.genaidemo.application.order.dto.CreateOrderRequestDto;
import solid.humank.genaidemo.application.order.dto.OrderResponse;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.application.order.port.outgoing.LogisticsServicePort;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.factory.OrderFactory;

/**
 * 訂單應用服務
 * 實現訂單管理用例接口
 */
@Service
public class OrderApplicationService implements OrderManagementUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final PaymentServicePort paymentServicePort;
    private final LogisticsServicePort logisticsServicePort;
    private final OrderFactory orderFactory;

    public OrderApplicationService(OrderPersistencePort orderPersistencePort,
                                  PaymentServicePort paymentServicePort,
                                  LogisticsServicePort logisticsServicePort,
                                  OrderFactory orderFactory) {
        this.orderPersistencePort = orderPersistencePort;
        this.paymentServicePort = paymentServicePort;
        this.logisticsServicePort = logisticsServicePort;
        this.orderFactory = orderFactory;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequestDto request) {
        // 創建訂單參數
        OrderFactory.OrderCreationParams params = new OrderFactory.OrderCreationParams(
                request.getCustomerId(),
                request.getShippingAddress()
        );
        
        // 創建訂單
        Order order = orderFactory.create(params);

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return OrderResponse.fromOrder(order);
    }

    @Override
    public OrderResponse addOrderItem(AddOrderItemRequestDto request) {
        // 查找訂單
        OrderId orderId = OrderId.of(request.getOrderId());
        Optional<Order> orderOpt = orderPersistencePort.findById(orderId);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // 添加訂單項目
        order.addItem(
                request.getProductId(),
                request.getProductName(),
                request.getQuantity(),
                request.getPrice()
        );

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return OrderResponse.fromOrder(order);
    }

    @Override
    public OrderResponse submitOrder(String orderId) {
        // 查找訂單
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // 提交訂單
        order.submit();

        // 處理支付
        paymentServicePort.processPayment(id, order.getTotalAmount());

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return OrderResponse.fromOrder(order);
    }

    @Override
    public OrderResponse cancelOrder(String orderId) {
        // 查找訂單
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // 取消訂單
        order.cancel();

        // 取消支付
        paymentServicePort.cancelPayment(id);

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return OrderResponse.fromOrder(order);
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        // 查找訂單
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // 返回響應
        return OrderResponse.fromOrder(order);
    }
}