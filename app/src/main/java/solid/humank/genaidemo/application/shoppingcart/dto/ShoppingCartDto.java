package solid.humank.genaidemo.application.shoppingcart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;

/** 購物車DTO */
public record ShoppingCartDto(
        String id,
        String customerId,
        List<CartItemDto> items,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        ShoppingCartStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
