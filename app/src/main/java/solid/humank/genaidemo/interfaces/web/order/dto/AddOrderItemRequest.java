package solid.humank.genaidemo.interfaces.web.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 添加訂單項請求 DTO */
@Schema(
        description = "添加訂單項請求，用於向現有訂單中添加新的商品項目",
        example =
                """
        {
          "orderId": "order-123",
          "productId": "product-456",
          "productName": "iPhone 15 Pro",
          "quantity": 2,
          "price": "500.00 TWD"
        }
        """)
public record AddOrderItemRequest(
        @Schema(
                        description = "目標訂單ID，指定要添加商品項目的訂單",
                        example = "order-123",
                        required = true,
                        minLength = 1,
                        maxLength = 50)
                @NotBlank(message = "訂單ID不能為空")
                @Size(max = 50, message = "訂單ID長度不能超過50個字符")
                String orderId,
        @Schema(
                        description = "商品ID，要添加的商品唯一識別碼",
                        example = "product-456",
                        required = true,
                        minLength = 1,
                        maxLength = 50)
                @NotBlank(message = "商品ID不能為空")
                @Size(max = 50, message = "商品ID長度不能超過50個字符")
                String productId,
        @Schema(
                        description = "商品名稱，用於顯示和確認",
                        example = "iPhone 15 Pro",
                        required = true,
                        minLength = 1,
                        maxLength = 100)
                @NotBlank(message = "商品名稱不能為空")
                @Size(max = 100, message = "商品名稱長度不能超過100個字符")
                String productName,
        @Schema(
                        description = "購買數量，必須為正整數",
                        example = "2",
                        required = true,
                        minimum = "1",
                        maximum = "999")
                @NotNull(message = "數量不能為空")
                @Min(value = 1, message = "數量必須大於0")
                int quantity,
        @Schema(description = "商品單價", example = "500.00", required = true)
                @NotNull(message = "單價不能為空")
                BigDecimal price) {
    public String getOrderId() {
        return orderId;
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

    public BigDecimal unitPrice() {
        return getPrice();
    }

    // 通過BigDecimal創建AddOrderItemRequest的工廠方法
    public static AddOrderItemRequest of(
            String orderId, String productId, String productName, int quantity, BigDecimal price) {
        return new AddOrderItemRequest(orderId, productId, productName, quantity, price);
    }
}
