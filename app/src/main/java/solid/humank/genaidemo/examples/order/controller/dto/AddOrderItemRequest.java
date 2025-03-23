package solid.humank.genaidemo.examples.order.controller.dto;

import java.math.BigDecimal;

public record AddOrderItemRequest(
    String productId,
    String productName,
    int quantity,
    BigDecimal unitPrice
) {}
