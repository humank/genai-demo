package solid.humank.genaidemo.interfaces.web.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import solid.humank.genaidemo.application.product.dto.ProductAttributeDto;
import solid.humank.genaidemo.application.product.dto.ProductImageDto;
import solid.humank.genaidemo.application.product.dto.ProductStatus;

/** 更新產品請求 DTO */
@Schema(
        description = "更新產品請求資料",
        example =
                """
        {
          "name": "iPhone 15 Pro",
          "description": "最新款iPhone，配備A17 Pro晶片，支援USB-C接口",
          "price": 35900.00,
          "currency": "TWD",
          "category": "ELECTRONICS",
          "status": "ACTIVE",
          "stockQuantity": 100,
          "weight": 187.0,
          "dimensions": {
            "length": 15.99,
            "width": 7.67,
            "height": 0.825
          },
          "sku": "IPHONE15PRO-128GB-BLACK",
          "barcode": "1234567890123",
          "brand": "Apple",
          "model": "iPhone 15 Pro",
          "tags": ["smartphone", "premium", "5G"],
          "images": [
            {
              "id": "img-001",
              "url": "https://example.com/images/iphone15pro-front.jpg",
              "altText": "iPhone 15 Pro 正面圖",
              "type": "PRIMARY",
              "sortOrder": 1
            }
          ],
          "attributes": [
            {
              "name": "顏色",
              "value": "太空黑",
              "type": "COLOR",
              "displayOrder": 1,
              "isKey": true
            }
          ]
        }
        """)
public class UpdateProductRequest {

    @Schema(
            description = "產品名稱",
            example = "iPhone 15 Pro",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "產品名稱不能超過100個字元")
    private String name;

    @Schema(
            description = "產品詳細描述，支援Markdown格式",
            example = "最新款iPhone，配備A17 Pro晶片，支援USB-C接口，具備專業級攝影功能",
            maxLength = 2000,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 2000, message = "產品描述不能超過2000個字元")
    private String description;

    @Schema(
            description = "產品價格（必須大於0）",
            example = "35900.00",
            minimum = "0.01",
            type = "number",
            format = "decimal",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DecimalMin(value = "0.01", message = "產品價格必須大於0")
    private BigDecimal price;

    @Schema(
            description = "貨幣代碼（ISO 4217標準）",
            example = "TWD",
            allowableValues = {"TWD", "USD", "EUR", "JPY", "CNY", "KRW", "HKD", "SGD"},
            pattern = "^[A-Z]{3}$",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^[A-Z]{3}$", message = "貨幣代碼必須為3位大寫字母")
    private String currency;

    @Schema(
            description = "產品分類類型",
            example = "ELECTRONICS",
            allowableValues = {
                "ELECTRONICS",
                "FASHION",
                "HOME_LIVING",
                "SPORTS_FITNESS",
                "BEAUTY_CARE",
                "FOOD_BEVERAGE",
                "BOOKS_STATIONERY",
                "TOYS_GAMES",
                "AUTOMOTIVE",
                "OTHER"
            },
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String category;

    @Schema(
            description = "產品狀態",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE", "DISCONTINUED", "DRAFT", "OUT_OF_STOCK"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private ProductStatus status;

    @Schema(
            description = "庫存數量",
            example = "100",
            minimum = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer stockQuantity;

    @Schema(
            description = "產品重量（公克）",
            example = "187.0",
            minimum = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double weight;

    @Schema(description = "產品尺寸資訊", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Valid
    private ProductDimensions dimensions;

    @Schema(
            description = "產品SKU（庫存單位）",
            example = "IPHONE15PRO-128GB-BLACK",
            maxLength = 50,
            pattern = "^[A-Z0-9\\-_]+$",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 50, message = "SKU不能超過50個字元")
    @Pattern(regexp = "^[A-Z0-9\\-_]*$", message = "SKU只能包含大寫字母、數字、連字號和底線")
    private String sku;

    @Schema(
            description = "產品條碼（EAN-13或UPC-A格式）",
            example = "1234567890123",
            pattern = "^\\d{12,13}$",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^\\d{12,13}$", message = "條碼必須為12或13位數字")
    private String barcode;

    @Schema(
            description = "產品品牌",
            example = "Apple",
            maxLength = 50,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 50, message = "品牌名稱不能超過50個字元")
    private String brand;

    @Schema(
            description = "產品型號",
            example = "iPhone 15 Pro",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "型號不能超過100個字元")
    private String model;

    @Schema(
            description = "產品標籤列表（用於搜尋和分類）",
            example = "[\"smartphone\", \"premium\", \"5G\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 20, message = "標籤數量不能超過20個")
    private List<@Size(max = 30, message = "單個標籤不能超過30個字元") String> tags;

    @Schema(description = "產品圖片列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Valid
    @Size(max = 10, message = "圖片數量不能超過10張")
    private List<ProductImageDto> images;

    @Schema(description = "產品屬性列表（如顏色、尺寸、材質等）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Valid
    @Size(max = 50, message = "屬性數量不能超過50個")
    private List<ProductAttributeDto> attributes;

    @Schema(
            description = "產品保固期（月）",
            example = "12",
            minimum = "0",
            maximum = "120",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer warrantyMonths;

    @Schema(
            description = "產品製造商",
            example = "Apple Inc.",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "製造商名稱不能超過100個字元")
    private String manufacturer;

    @Schema(
            description = "產品原產地",
            example = "中國",
            maxLength = 50,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 50, message = "原產地不能超過50個字元")
    private String countryOfOrigin;

    /** 產品尺寸資訊 */
    @Schema(description = "產品尺寸資訊")
    public static class ProductDimensions {
        @Schema(
                description = "長度（公分）",
                example = "15.99",
                minimum = "0",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0", message = "長度必須大於等於0")
        private Double length;

        @Schema(
                description = "寬度（公分）",
                example = "7.67",
                minimum = "0",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0", message = "寬度必須大於等於0")
        private Double width;

        @Schema(
                description = "高度（公分）",
                example = "0.825",
                minimum = "0",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0", message = "高度必須大於等於0")
        private Double height;

        public ProductDimensions() {}

        public ProductDimensions(Double length, Double width, Double height) {
            this.length = length;
            this.width = width;
            this.height = height;
        }

        public Double getLength() {
            return length;
        }

        public void setLength(Double length) {
            this.length = length;
        }

        public Double getWidth() {
            return width;
        }

        public void setWidth(Double width) {
            this.width = width;
        }

        public Double getHeight() {
            return height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }
    }

    // Constructors
    public UpdateProductRequest() {}

    public UpdateProductRequest(
            String name, String description, BigDecimal price, String currency, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.category = category;
    }

    // Getters and Setters
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

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public ProductDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(ProductDimensions dimensions) {
        this.dimensions = dimensions;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<ProductImageDto> getImages() {
        return images;
    }

    public void setImages(List<ProductImageDto> images) {
        this.images = images;
    }

    public List<ProductAttributeDto> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ProductAttributeDto> attributes) {
        this.attributes = attributes;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }
}
