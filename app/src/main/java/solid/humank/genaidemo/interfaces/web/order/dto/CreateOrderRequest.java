package solid.humank.genaidemo.interfaces.web.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 創建訂單請求 DTO */
@Schema(
        description = "創建訂單請求",
        example =
                """
        {
          "customerId": "customer-123",
          "shippingAddress": "台北市信義區信義路五段7號"
        }
        """)
public record CreateOrderRequest(
        @Schema(
                        description = "客戶ID，用於識別下訂單的客戶",
                        example = "customer-123",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        minLength = 1,
                        maxLength = 50)
                @NotBlank(message = "客戶ID不能為空")
                @Size(max = 50, message = "客戶ID長度不能超過50個字符")
                String customerId,
        @Schema(
                        description = "配送地址，訂單商品的配送目的地",
                        example = "台北市信義區信義路五段7號",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        minLength = 1,
                        maxLength = 200)
                @NotBlank(message = "配送地址不能為空")
                @Size(max = 200, message = "配送地址長度不能超過200個字符")
                String shippingAddress) {

    public String getCustomerId() {
        return customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }
}
