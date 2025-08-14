package solid.humank.genaidemo.interfaces.web.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import solid.humank.genaidemo.application.order.dto.response.OrderItemResponse;
import solid.humank.genaidemo.application.common.valueobject.Money;
import solid.humank.genaidemo.application.common.valueobject.OrderStatus;

/**
 * 訂單響應 DTO
 */
public class OrderResponse {
    private final String orderId;
    private final String customerId;
    private final String shippingAddress;
    private final List<WebOrderItemResponse> items;
    private final Money totalAmount;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderResponse(String orderId, String customerId, String shippingAddress,
                        List<WebOrderItemResponse> items, Money totalAmount, OrderStatus status,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderResponse fromApplicationResponse(solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse) {
        List<WebOrderItemResponse> items = appResponse.getItems().stream()
                .map(WebOrderItemResponse::fromOrderItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                appResponse.getId(),
                appResponse.getCustomerId(),
                appResponse.getShippingAddress(),
                items,
                Money.of(appResponse.getTotalAmount()),
                OrderStatus.valueOf(appResponse.getStatus()),
                appResponse.getCreatedAt(),
                appResponse.getUpdatedAt()
        );
    }

    public String getOrderId() {
        return orderId;
    }
    
    // 為了與前端兼容，添加 getId() 方法
    public String getId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public List<WebOrderItemResponse> getItems() {
        return items;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 介面層訂單項目響應DTO
     */
    public static class WebOrderItemResponse {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final Money price;
        private final Money subtotal;

        public WebOrderItemResponse(String productId, String productName, int quantity, Money price, Money subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        public static WebOrderItemResponse fromOrderItemResponse(solid.humank.genaidemo.application.order.dto.response.OrderItemResponse item) {
            return new WebOrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    Money.of(item.getUnitPrice()),
                    Money.of(item.getSubtotal())
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