package solid.humank.genaidemo.application.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 客戶分頁數據傳輸對象 */
@Schema(description = "客戶分頁查詢結果，包含客戶列表和分頁資訊")
public record CustomerPageDto(
        @Schema(description = "客戶列表", required = true) List<CustomerDto> content,
        @Schema(description = "總客戶數量", example = "150", required = true) int totalElements,
        @Schema(description = "總頁數", example = "8", required = true) int totalPages,
        @Schema(description = "每頁大小", example = "20", required = true) int size,
        @Schema(description = "當前頁碼（從0開始）", example = "0", required = true) int number,
        @Schema(description = "是否為第一頁", example = "true", required = true) boolean first,
        @Schema(description = "是否為最後一頁", example = "false", required = true) boolean last) {}
