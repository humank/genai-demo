package solid.humank.genaidemo.application.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import solid.humank.genaidemo.application.common.dto.PagedResult;
import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.application.order.dto.response.OrderItemResponse;
import solid.humank.genaidemo.application.order.dto.response.OrderResponse;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.factory.OrderFactory;

/** 訂單應用服務 實現訂單管理用例接口 */
@Service
public class OrderApplicationService implements OrderManagementUseCase {

    private static final String ORDER_NOT_FOUND = "Order not found: ";
    private final OrderPersistencePort orderPersistencePort;
    private final PaymentServicePort paymentServicePort;
    private final OrderFactory orderFactory;

    public OrderApplicationService(
            OrderPersistencePort orderPersistencePort,
            PaymentServicePort paymentServicePort,
            OrderFactory orderFactory) {
        this.orderPersistencePort = orderPersistencePort;
        this.paymentServicePort = paymentServicePort;
        this.orderFactory = orderFactory;
    }

    @Override
    public OrderResponse createOrder(CreateOrderCommand command) {
        // 創建訂單參數
        OrderFactory.OrderCreationParams params =
                new OrderFactory.OrderCreationParams(
                        command.getCustomerId(), command.getShippingAddress());

        // 創建訂單
        Order order = orderFactory.create(params);

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse addOrderItem(AddOrderItemCommand command) {
        // 查找訂單 - 使用領域值對象
        OrderId orderId = OrderId.of(command.getOrderId());
        Optional<Order> orderOpt = orderPersistencePort.findById(orderId);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND + orderId));

        // 添加訂單項目
        order.addItem(
                command.getProductId(),
                command.getProductName(),
                command.getQuantity(),
                command.getPrice());

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse submitOrder(String orderId) {
        // 查找訂單 - 使用領域值對象
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND + id));

        // 提交訂單
        order.submit();

        // 處理支付 - 轉換為UUID給外部服務
        paymentServicePort.processPayment(id.getId(), order.getTotalAmount());

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse cancelOrder(String orderId) {
        // 查找訂單 - 使用領域值對象
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND + id));

        // 取消訂單
        order.cancel();

        // 取消支付 - 轉換為UUID給外部服務
        paymentServicePort.cancelPayment(id.getId());

        // 保存訂單
        orderPersistencePort.save(order);

        // 返回響應
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        // 查找訂單 - 使用領域值對象
        OrderId id = OrderId.of(orderId);
        Optional<Order> orderOpt = orderPersistencePort.findById(id);
        Order order = orderOpt.orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND + id));

        // 返回響應
        return mapToOrderResponse(order);
    }

    @Override
    public PagedResult<OrderResponse> getOrders(int page, int size) {
        // 獲取分頁數據
        List<Order> orders = orderPersistencePort.findAll(page, size);
        long totalElements = orderPersistencePort.count();

        // 轉換為響應DTO
        List<OrderResponse> orderResponses =
                orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());

        // 返回分頁結果
        return PagedResult.of(orderResponses, (int) totalElements, page, size);
    }

    /** 將領域模型轉換為應用層響應DTO */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> items =
                order.getItems().stream()
                        .map(
                                item ->
                                        new OrderItemResponse(
                                                UUID.randomUUID()
                                                        .toString(), // 生成一個臨時ID，因為OrderItem沒有ID屬性
                                                item.getProductId(),
                                                item.getProductName(),
                                                item.getQuantity(),
                                                item.getPrice().getAmount(),
                                                item.getSubtotal().getAmount()))
                        .collect(Collectors.toList());

        return new OrderResponse(
                order.getId().toString(),
                order.getCustomerId().toString(),
                order.getShippingAddress(),
                order.getStatus().toString(),
                order.getTotalAmount().getAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
