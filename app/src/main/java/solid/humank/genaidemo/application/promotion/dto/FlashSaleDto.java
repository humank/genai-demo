package solid.humank.genaidemo.application.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 閃購DTO */
public record FlashSaleDto(
        String promotionId,
        String productId,
        String productName,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int quantityLimit,
        int remainingQuantity,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean isActive) {}
