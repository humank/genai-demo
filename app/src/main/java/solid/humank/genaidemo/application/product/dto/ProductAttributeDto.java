package solid.humank.genaidemo.application.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 產品屬性資料傳輸物件 */
@Schema(description = "產品屬性資訊")
public record ProductAttributeDto(
        @Schema(description = "屬性名稱", example = "顏色", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50) @NotBlank(message = "屬性名稱不能為空") @Size(max = 50, message = "屬性名稱不能超過50個字元") String name,
        @Schema(description = "屬性值", example = "太空黑", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100) @NotBlank(message = "屬性值不能為空") @Size(max = 100, message = "屬性值不能超過100個字元") String value,
        @Schema(description = "屬性類型", allowableValues = {
                "TEXT",
                "NUMBER",
                "BOOLEAN",
                "COLOR",
                "SIZE",
                "MATERIAL"
        }, example = "COLOR", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "屬性類型不能為空") String type,
        @Schema(description = "屬性單位", example = "公分", maxLength = 20) @Size(max = 20, message = "屬性單位不能超過20個字元") String unit,
        @Schema(description = "屬性顯示順序", example = "1", minimum = "0") int displayOrder,
        @Schema(description = "是否為關鍵屬性", example = "true") boolean isKey,
        @Schema(description = "是否可搜尋", example = "true") boolean searchable,
        @Schema(description = "屬性描述", example = "產品的主要顏色", maxLength = 200) @Size(max = 200, message = "屬性描述不能超過200個字元") String description){

    /** 屬性類型列舉 */
    @Schema(description = "產品屬性類型", enumAsRef = true)
    public enum AttributeType {
        @Schema(description = "文字類型 - 一般文字屬性")
        TEXT("TEXT", "文字"),

        @Schema(description = "數字類型 - 數值屬性")
        NUMBER("NUMBER", "數字"),

        @Schema(description = "布林類型 - 是/否屬性")
        BOOLEAN("BOOLEAN", "布林"),

        @Schema(description = "顏色類型 - 顏色屬性")
        COLOR("COLOR", "顏色"),

        @Schema(description = "尺寸類型 - 尺寸屬性")
        SIZE("SIZE", "尺寸"),

        @Schema(description = "材質類型 - 材質屬性")
        MATERIAL("MATERIAL", "材質");

        private final String code;
        private final String displayName;

        AttributeType(String code, String displayName) {
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
