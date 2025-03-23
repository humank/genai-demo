package solid.humank.genaidemo.examples.order.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.OrderStatus;

public record OrderResponse(
    String orderId,
    String customerId,
    OrderStatus status,
    BigDecimal totalAmount,
    BigDecimal finalAmount,
    List<Map<String, Object>> items
) {
    public static OrderResponse fromDomain(Order order) {
        return new OrderResponse(
            order.getOrderId().toString(),
            order.getCustomerId(),
            order.getStatus(),
            order.getTotalAmount().amount(),
            order.getFinalAmount() != null ? order.getFinalAmount().amount() : null,
            order.getItemsAsMap()
        );
    }
}
