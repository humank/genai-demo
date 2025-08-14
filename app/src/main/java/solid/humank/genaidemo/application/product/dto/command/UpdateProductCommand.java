package solid.humank.genaidemo.application.product.dto.command;

import java.math.BigDecimal;

/**
 * 更新產品命令
 */
public class UpdateProductCommand {
    private final String productId;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String currency;
    private final String category;

    public UpdateProductCommand(String productId, String name, String description, 
                               BigDecimal price, String currency, String category) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.category = category;
    }

    public static UpdateProductCommand of(String productId, String name, String description, 
                                        BigDecimal price, String currency, String category) {
        return new UpdateProductCommand(productId, name, description, price, currency, category);
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCategory() {
        return category;
    }
}