package solid.humank.genaidemo.application.product;

import java.util.List;

/**
 * 產品分頁數據傳輸對象
 */
public record ProductPageDto(
        List<ProductDto> content,
        int totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {
}