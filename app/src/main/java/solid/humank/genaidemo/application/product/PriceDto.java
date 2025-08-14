package solid.humank.genaidemo.application.product;

import java.math.BigDecimal;

/** 價格數據傳輸對象 */
public record PriceDto(BigDecimal amount, String currency) {}
