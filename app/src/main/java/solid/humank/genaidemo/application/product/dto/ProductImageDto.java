package solid.humank.genaidemo.application.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 產品圖片資料傳輸物件 */
@Schema(description = "產品圖片資訊")
public record ProductImageDto(
                @Schema(description = "圖片唯一識別碼", example = "img-001", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "圖片ID不能為空") String id,
                @Schema(description = "圖片URL", example = "https://example.com/images/product/iphone15pro.jpg", requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") @NotBlank(message = "圖片URL不能為空") @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|webp)$", message = "圖片URL格式不正確，必須是有效的HTTP(S)連結且以圖片格式結尾") String url,
                @Schema(description = "圖片替代文字", example = "iPhone 15 Pro 太空黑色正面圖", maxLength = 200) @Size(max = 200, message = "圖片替代文字不能超過200個字元") String altText,
                @Schema(description = "圖片標題", example = "iPhone 15 Pro - 正面視圖", maxLength = 100) @Size(max = 100, message = "圖片標題不能超過100個字元") String title,
                @Schema(description = "圖片類型", allowableValues = {
                                "PRIMARY", "GALLERY", "THUMBNAIL",
                                "DETAIL" }, example = "PRIMARY", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "圖片類型不能為空") String type,
                @Schema(description = "圖片排序順序", example = "1", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
                @Schema(description = "圖片寬度（像素）", example = "800", minimum = "1") Integer width,
                @Schema(description = "圖片高度（像素）", example = "600", minimum = "1") Integer height,
                @Schema(description = "圖片檔案大小（位元組）", example = "102400", minimum = "1") Long fileSize,
                @Schema(description = "圖片MIME類型", allowableValues = { "image/jpeg", "image/png", "image/gif",
                                "image/webp" }, example = "image/jpeg") String mimeType){

        /** 圖片類型列舉 */
        @Schema(description = "圖片類型", enumAsRef = true)
        public enum ImageType {
                @Schema(description = "主要圖片 - 產品的主要展示圖片")
                PRIMARY("PRIMARY", "主要圖片"),

                @Schema(description = "圖庫圖片 - 產品的額外展示圖片")
                GALLERY("GALLERY", "圖庫圖片"),

                @Schema(description = "縮圖 - 產品的縮略圖")
                THUMBNAIL("THUMBNAIL", "縮圖"),

                @Schema(description = "詳細圖片 - 產品的細節圖片")
                DETAIL("DETAIL", "詳細圖片");

                private final String code;
                private final String displayName;

                ImageType(String code, String displayName) {
                        this.code = code;
                        this.displayName = displayName;
                }

                public String getCode() {
                        return code;
                }

                public String getDisplayName() {
                        return displayName;
                }
        }
}
