package solid.humank.genaidemo.application.customer;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 客戶分頁數據傳輸對象 */
@Schema(description = "客戶分頁查詢結果，包含客戶列表和分頁資訊")
public record CustomerPageDto(
                @Schema(description = "客戶列表", requiredMode = Schema.RequiredMode.REQUIRED) List<CustomerDto> content,
                @Schema(description = "總客戶數量", example = "150", requiredMode = Schema.RequiredMode.REQUIRED) int totalElements,
                @Schema(description = "總頁數", example = "8", requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
                @Schema(description = "每頁大小", example = "20", requiredMode = Schema.RequiredMode.REQUIRED) int size,
                @Schema(description = "當前頁碼（從0開始）", example = "0", requiredMode = Schema.RequiredMode.REQUIRED) int number,
                @Schema(description = "是否為第一頁", example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean first,
                @Schema(description = "是否為最後一頁", example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean last) {
}
