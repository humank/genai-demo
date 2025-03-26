package solid.humank.genaidemo.examples.order.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.OrderItem;

/**
 * 訂單響應 DTO
 */
public class OrderResponse {
    private final String orderId;
    private final String customerId;
    private final String shippingAddress;
    private final List<OrderItemDto> items;
    private final Money totalAmount;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderResponse(
            String orderId,
            String customerId,
            String shippingAddress,
            List<OrderItemDto> items,
            Money totalAmount,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 將領域模型轉換為DTO
     */
    public static OrderResponse fromDomain(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(OrderItemDto::fromOrderItem)
                .toList();

        return new OrderResponse(
                order.getId().toString(),
                order.getCustomerId(),
                order.getShippingAddress(),
                items,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 訂單項 DTO
     */
    public static class OrderItemDto {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final Money price;
        private final Money subtotal;

        public OrderItemDto(String productId, String productName, int quantity, Money price, Money subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        public static OrderItemDto fromOrderItem(OrderItem item) {
            return new OrderItemDto(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getSubtotal()
            );
        }

        // Getters
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
