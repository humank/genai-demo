package solid.humank.genaidemo.interfaces.web.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 訂單響應 DTO */
@Schema(
        description = "訂單響應資料，包含完整的訂單資訊",
        example =
                """
        {
          "orderId": "order-123",
          "customerId": "customer-456",
          "shippingAddress": "台北市信義區信義路五段7號",
          "items": [
            {
              "productId": "product-789",
              "productName": "iPhone 15 Pro",
              "quantity": 2,
              "price": "500.00 TWD",
              "subtotal": "1000.00 TWD"
            }
          ],
          "totalAmount": "1000.00 TWD",
          "status": "CREATED",
          "createdAt": "2024-01-15T10:30:00",
          "updatedAt": "2024-01-15T10:35:00"
        }
        """)
public class OrderResponse {
    @Schema(description = "訂單唯一識別碼", example = "order-123", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String orderId;

    @Schema(description = "下訂單的客戶ID", example = "customer-456", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String customerId;

    @Schema(description = "訂單配送地址", example = "台北市信義區信義路五段7號", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String shippingAddress;

    @Schema(description = "訂單項目列表，包含所有購買的商品", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<WebOrderItemResponse> items;

    @Schema(description = "訂單總金額，包含所有商品的總價", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final BigDecimal totalAmount;

    @Schema(
            description = "訂單當前狀態",
            example = "CREATED",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
                "CREATED",
                "SUBMITTED",
                "PAID",
                "PROCESSING",
                "SHIPPED",
                "DELIVERED",
                "COMPLETED",
                "CANCELLED",
                "REJECTED",
                "PAYMENT_FAILED"
            })
    private final String status;

    @Schema(description = "訂單創建時間", example = "2024-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final LocalDateTime createdAt;

    @Schema(description = "訂單最後更新時間", example = "2024-01-15T10:35:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final LocalDateTime updatedAt;

    public OrderResponse(
            String orderId,
            String customerId,
            String shippingAddress,
            List<WebOrderItemResponse> items,
            BigDecimal totalAmount,
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

    public static OrderResponse fromApplicationResponse(
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse) {
        List<WebOrderItemResponse> items =
                appResponse.getItems().stream()
                        .map(WebOrderItemResponse::fromOrderItemResponse)
                        .toList();

        return new OrderResponse(
                appResponse.getId(),
                appResponse.getCustomerId(),
                appResponse.getShippingAddress(),
                items,
                appResponse.getTotalAmount(),
                appResponse.getStatus(),
                appResponse.getCreatedAt(),
                appResponse.getUpdatedAt());
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

    public BigDecimal getTotalAmount() {
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

    /** 介面層訂單項目響應DTO */
    @Schema(
            description = "訂單項目響應資料，代表訂單中的單一商品項目",
            example =
                    """
            {
              "productId": "product-789",
              "productName": "iPhone 15 Pro",
              "quantity": 2,
              "price": "500.00 TWD",
              "subtotal": "1000.00 TWD"
            }
            """)
    public static class WebOrderItemResponse {
        @Schema(description = "商品唯一識別碼", example = "product-789", requiredMode = Schema.RequiredMode.REQUIRED)
        private final String productId;

        @Schema(description = "商品名稱", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
        private final String productName;

        @Schema(description = "購買數量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        private final int quantity;

        @Schema(description = "商品單價", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        private final BigDecimal price;

        @Schema(description = "該項目小計金額（單價 × 數量）", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        private final BigDecimal subtotal;

        public WebOrderItemResponse(
                String productId,
                String productName,
                int quantity,
                BigDecimal price,
                BigDecimal subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        public static WebOrderItemResponse fromOrderItemResponse(
                solid.humank.genaidemo.application.order.dto.response.OrderItemResponse item) {
            return new WebOrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal());
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

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }
}
