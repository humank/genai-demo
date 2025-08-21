package solid.humank.genaidemo.application.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 產品分頁數據傳輸對象 */
@Schema(description = "產品分頁資訊")
public record ProductPageDto(
        @Schema(description = "產品列表", required = true) List<ProductDto> content,
        @Schema(description = "總記錄數", example = "100", minimum = "0", required = true)
                int totalElements,
        @Schema(description = "總頁數", example = "5", minimum = "0", required = true) int totalPages,
        @Schema(description = "每頁大小", example = "20", minimum = "1", required = true) int size,
        @Schema(description = "當前頁碼（從0開始）", example = "0", minimum = "0", required = true)
                int number,
        @Schema(description = "是否為第一頁", example = "true", required = true) boolean first,
        @Schema(description = "是否為最後一頁", example = "false", required = true) boolean last) {}
