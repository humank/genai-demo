package solid.humank.genaidemo.application.promotion.dto;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/** 促銷DTO */
public record PromotionDto(
        String id,
        String name,
        String description,
        PromotionType type,
        PromotionStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int usageLimit,
        int usageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
