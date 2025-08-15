package solid.humank.genaidemo.application.shoppingcart.dto;

import java.math.BigDecimal;

/** 購物車項目DTO */
public record CartItemDto(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {}
