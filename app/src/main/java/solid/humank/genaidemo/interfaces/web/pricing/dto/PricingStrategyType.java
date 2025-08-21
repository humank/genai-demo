package solid.humank.genaidemo.interfaces.web.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 定價策略類型枚舉 定義系統支援的各種定價策略 */
@Schema(description = "定價策略類型", example = "PERCENTAGE_DISCOUNT")
public enum PricingStrategyType {
    @Schema(description = "百分比折扣策略")
    PERCENTAGE_DISCOUNT,

    @Schema(description = "固定金額折扣策略")
    FIXED_AMOUNT_DISCOUNT,

    @Schema(description = "買一送一策略")
    BUY_ONE_GET_ONE,

    @Schema(description = "階梯定價策略")
    TIERED_PRICING,

    @Schema(description = "會員專屬定價策略")
    MEMBER_EXCLUSIVE,

    @Schema(description = "限時促銷策略")
    TIME_LIMITED_PROMOTION,

    @Schema(description = "組合商品定價策略")
    BUNDLE_PRICING,

    @Schema(description = "標準定價策略")
    STANDARD_PRICING
}
