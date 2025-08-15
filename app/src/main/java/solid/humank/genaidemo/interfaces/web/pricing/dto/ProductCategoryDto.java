package solid.humank.genaidemo.interfaces.web.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 產品類別DTO 用於介面層與應用層之間的數據傳輸，定義系統支援的所有產品類別 */
@Schema(description = "產品類別枚舉，用於商品分類管理", example = "ELECTRONICS")
public enum ProductCategoryDto {
    @Schema(description = "電子產品類別，包含手機、電腦、家電等")
    ELECTRONICS,

    @Schema(description = "時尚服飾類別，包含服裝、鞋類、配件等")
    FASHION,

    @Schema(description = "生鮮雜貨類別，包含食品、飲料、日用品等")
    GROCERIES,

    @Schema(description = "家用電器類別，包含大型家電、小型家電等")
    HOME_APPLIANCES,

    @Schema(description = "美容保養類別，包含化妝品、保養品、香水等")
    BEAUTY,

    @Schema(description = "運動用品類別，包含運動器材、運動服飾等")
    SPORTS,

    @Schema(description = "書籍文具類別，包含圖書、文具、辦公用品等")
    BOOKS,

    @Schema(description = "玩具遊戲類別，包含兒童玩具、遊戲產品等")
    TOYS,

    @Schema(description = "汽車用品類別，包含汽車配件、維修用品等")
    AUTOMOTIVE,

    @Schema(description = "健康醫療類別，包含保健品、醫療器材等")
    HEALTH,

    @Schema(description = "一般商品類別，不屬於其他特定類別的商品")
    GENERAL
}
