package solid.humank.genaidemo.application.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 產品狀態列舉 */
@Schema(description = "產品狀態", enumAsRef = true)
public enum ProductStatus {
    @Schema(description = "活躍狀態 - 產品可正常銷售")
    ACTIVE("ACTIVE", "活躍"),

    @Schema(description = "停用狀態 - 產品暫時停止銷售")
    INACTIVE("INACTIVE", "停用"),

    @Schema(description = "下架狀態 - 產品已從銷售目錄中移除")
    DISCONTINUED("DISCONTINUED", "下架"),

    @Schema(description = "草稿狀態 - 產品尚未發布")
    DRAFT("DRAFT", "草稿"),

    @Schema(description = "缺貨狀態 - 產品暫時無庫存")
    OUT_OF_STOCK("OUT_OF_STOCK", "缺貨");

    private final String code;
    private final String displayName;

    ProductStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @Schema(description = "狀態代碼", example = "ACTIVE")
    public String getCode() {
        return code;
    }

    @Schema(description = "狀態顯示名稱", example = "活躍")
    public String getDisplayName() {
        return displayName;
    }

    /** 根據代碼獲取產品狀態 */
    public static ProductStatus fromCode(String code) {
        for (ProductStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的產品狀態代碼: " + code);
    }
}
