package solid.humank.genaidemo.examples.order.application.dto;

import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.OrderStatus;
import solid.humank.genaidemo.examples.order.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 訂單響應 DTO
 */
public class OrderResponse {
    private final String orderId;
    private final String customerId;
    private final String shippingAddress;
    private final List<OrderItemResponse> items;
    private final Money totalAmount;
    private final OrderStatus status;

    private OrderResponse(String orderId, String customerId, String shippingAddress,
                        List<OrderItemResponse> items, Money totalAmount, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public static OrderResponse fromOrder(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromOrderItem)
                .collect(Collectors.toList());

        return new OrderResponse(
            order.getId().toString(),
            order.getCustomerId(),
            order.getShippingAddress(),
            items,
            order.getTotalAmount(),
            order.getStatus()
        );
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * 訂單項響應 DTO
     */
    public static class OrderItemResponse {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final Money price;
        private final Money subtotal;

        private OrderItemResponse(String productId, String productName, int quantity, Money price, Money subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        public static OrderItemResponse fromOrderItem(OrderItem item) {
            return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getSubtotal()
            );
        }

        public String getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public Money getPrice() {
            return price;
        }

        public Money getSubtotal() {
            return subtotal;
        }
    }
}
