package solid.humank.genaidemo.interfaces.web.product.dto;

import java.math.BigDecimal;

/** 更新產品請求 DTO */
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private String category;

    public UpdateProductRequest() {}

    public UpdateProductRequest(
            String name, String description, BigDecimal price, String currency, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
