package solid.humank.genaidemo.interfaces.web.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import solid.humank.genaidemo.application.product.dto.ProductAttributeDto;
import solid.humank.genaidemo.application.product.dto.ProductImageDto;
import solid.humank.genaidemo.application.product.dto.ProductStatus;

/** 產品回應 DTO */
@Schema(
        description = "產品回應資料",
        example =
                """
        {
          "id": "PROD-001",
          "name": "iPhone 15 Pro",
          "description": "最新款iPhone，配備A17 Pro晶片，支援USB-C接口",
          "price": 35900.00,
          "currency": "TWD",
          "category": "ELECTRONICS",
          "categoryDisplayName": "電子產品",
          "status": "ACTIVE",
          "statusDisplayName": "活躍",
          "inStock": true,
          "stockQuantity": 50,
          "sku": "IPHONE15PRO-128GB-BLACK",
          "brand": "Apple",
          "model": "iPhone 15 Pro",
          "weight": 187.0,
          "dimensions": {
            "length": 15.99,
            "width": 7.67,
            "height": 0.825
          },
          "barcode": "1234567890123",
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
          ],
          "warrantyMonths": 12,
          "manufacturer": "Apple Inc.",
          "countryOfOrigin": "中國",
          "createdAt": "2024-01-15T10:30:00",
          "updatedAt": "2024-01-15T10:30:00"
        }
        """)
public record ProductResponse(
        @Schema(description = "產品唯一識別碼", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(description = "產品名稱", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "產品詳細描述", example = "最新款iPhone，配備A17 Pro晶片，支援USB-C接口，具備專業級攝影功能")
                String description,
        @Schema(description = "產品價格", example = "35900.00", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal price,
        @Schema(description = "貨幣代碼", example = "TWD", requiredMode = Schema.RequiredMode.REQUIRED) String currency,
        @Schema(description = "產品分類代碼", example = "ELECTRONICS", requiredMode = Schema.RequiredMode.REQUIRED) String category,
        @Schema(description = "產品分類顯示名稱", example = "電子產品", requiredMode = Schema.RequiredMode.REQUIRED)
                String categoryDisplayName,
        @Schema(description = "產品狀態", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED) ProductStatus status,
        @Schema(description = "產品狀態顯示名稱", example = "活躍", requiredMode = Schema.RequiredMode.REQUIRED) String statusDisplayName,
        @Schema(description = "是否有庫存", example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean inStock,
        @Schema(description = "庫存數量", example = "50", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
                int stockQuantity,
        @Schema(description = "產品SKU", example = "IPHONE15PRO-128GB-BLACK") String sku,
        @Schema(description = "產品品牌", example = "Apple") String brand,
        @Schema(description = "產品型號", example = "iPhone 15 Pro") String model,
        @Schema(description = "產品重量（公克）", example = "187.0", minimum = "0") Double weight,
        @Schema(description = "產品尺寸資訊") ProductDimensions dimensions,
        @Schema(description = "產品條碼", example = "1234567890123") String barcode,
        @Schema(description = "產品標籤列表", example = "[\"smartphone\", \"premium\", \"5G\"]")
                List<String> tags,
        @Schema(description = "產品圖片列表") List<ProductImageDto> images,
        @Schema(description = "產品屬性列表") List<ProductAttributeDto> attributes,
        @Schema(description = "產品保固期（月）", example = "12", minimum = "0") Integer warrantyMonths,
        @Schema(description = "產品製造商", example = "Apple Inc.") String manufacturer,
        @Schema(description = "產品原產地", example = "中國") String countryOfOrigin,
        @Schema(
                        description = "產品創建時間",
                        example = "2024-01-15T10:30:00",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                LocalDateTime createdAt,
        @Schema(
                        description = "產品最後更新時間",
                        example = "2024-01-15T10:30:00",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                LocalDateTime updatedAt) {

    /** 產品尺寸資訊 */
    @Schema(description = "產品尺寸資訊")
    public record ProductDimensions(
            @Schema(description = "長度（公分）", example = "15.99", minimum = "0") Double length,
            @Schema(description = "寬度（公分）", example = "7.67", minimum = "0") Double width,
            @Schema(description = "高度（公分）", example = "0.825", minimum = "0") Double height) {}
}
