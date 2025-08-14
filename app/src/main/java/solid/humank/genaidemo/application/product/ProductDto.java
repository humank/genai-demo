package solid.humank.genaidemo.application.product;

/**
 * 產品數據傳輸對象
 */
public record ProductDto(
        String id,
        String name,
        String description,
        PriceDto price,
        String category,
        boolean inStock,
        int stockQuantity
) {
}