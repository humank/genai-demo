package solid.humank.genaidemo.application.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 價格數據傳輸對象 */
@Schema(description = "價格資訊")
public record PriceDto(
        @Schema(description = "價格金額", example = "35900.00", minimum = "0", required = true)
                BigDecimal amount,
        @Schema(
                        description = "貨幣代碼",
                        example = "TWD",
                        allowableValues = {"TWD", "USD", "EUR", "JPY"},
                        required = true)
                String currency) {}
