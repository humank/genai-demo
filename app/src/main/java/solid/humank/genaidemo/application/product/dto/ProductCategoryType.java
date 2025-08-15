package solid.humank.genaidemo.application.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 產品分類類型列舉 */
@Schema(description = "產品分類類型", enumAsRef = true)
public enum ProductCategoryType {
    @Schema(description = "電子產品 - 包含手機、電腦、家電等")
    ELECTRONICS("電子產品", "包含手機、電腦、家電等電子設備"),

    @Schema(description = "服裝配件 - 包含衣服、鞋子、包包等")
    FASHION("服裝配件", "包含衣服、鞋子、包包等時尚用品"),

    @Schema(description = "家居用品 - 包含家具、裝飾品、生活用品等")
    HOME_LIVING("家居用品", "包含家具、裝飾品、生活用品等"),

    @Schema(description = "運動健身 - 包含運動器材、健身用品等")
    SPORTS_FITNESS("運動健身", "包含運動器材、健身用品、戶外用品等"),

    @Schema(description = "美妝保養 - 包含化妝品、保養品、香水等")
    BEAUTY_CARE("美妝保養", "包含化妝品、保養品、香水等美容用品"),

    @Schema(description = "食品飲料 - 包含食物、飲品、保健品等")
    FOOD_BEVERAGE("食品飲料", "包含食物、飲品、保健品等消費品"),

    @Schema(description = "書籍文具 - 包含書籍、文具、辦公用品等")
    BOOKS_STATIONERY("書籍文具", "包含書籍、文具、辦公用品等學習用品"),

    @Schema(description = "玩具遊戲 - 包含玩具、遊戲、娛樂用品等")
    TOYS_GAMES("玩具遊戲", "包含玩具、遊戲、娛樂用品等休閒商品"),

    @Schema(description = "汽車用品 - 包含汽車配件、維修用品等")
    AUTOMOTIVE("汽車用品", "包含汽車配件、維修用品、車載設備等"),

    @Schema(description = "其他分類 - 不屬於以上分類的商品")
    OTHER("其他", "不屬於以上分類的其他商品");

    private final String displayName;
    private final String description;

    ProductCategoryType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Schema(description = "分類顯示名稱", example = "電子產品")
    public String getDisplayName() {
        return displayName;
    }

    @Schema(description = "分類詳細描述", example = "包含手機、電腦、家電等電子設備")
    public String getDescription() {
        return description;
    }
}
