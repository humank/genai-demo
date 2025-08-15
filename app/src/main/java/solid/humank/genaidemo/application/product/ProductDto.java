package solid.humank.genaidemo.application.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import solid.humank.genaidemo.application.product.dto.ProductAttributeDto;
import solid.humank.genaidemo.application.product.dto.ProductImageDto;
import solid.humank.genaidemo.application.product.dto.ProductStatus;

/** 產品數據傳輸對象 */
@Schema(
        description = "產品完整資訊",
        example =
                """
                {
                  "id": "PROD-001",
                  "name": "iPhone 15 Pro",
                  "description": "最新款iPhone，配備A17 Pro晶片，支援USB-C接口",
                  "price": {
                    "amount": 35900.00,
                    "currency": "TWD"
                  },
                  "category": "ELECTRONICS",
                  "status": "ACTIVE",
                  "inStock": true,
                  "stockQuantity": 50,
                  "sku": "IPHONE15PRO-128GB-BLACK",
                  "brand": "Apple",
                  "model": "iPhone 15 Pro",
                  "weight": 187.0,
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
                  "createdAt": "2024-01-15T10:30:00",
                  "updatedAt": "2024-01-15T10:30:00"
                }
                """)
public record ProductDto(
        @Schema(
                        description = "產品唯一識別碼",
                        example = "PROD-001",
                        required = true,
                        pattern = "^PROD-[A-Z0-9]+$")
                String id,
        @Schema(description = "產品名稱", example = "iPhone 15 Pro", required = true, maxLength = 100)
                String name,
        @Schema(
                        description = "產品詳細描述",
                        example = "最新款iPhone，配備A17 Pro晶片，支援USB-C接口，具備專業級攝影功能",
                        maxLength = 2000)
                String description,
        @Schema(description = "產品價格資訊", required = true) PriceDto price,
        @Schema(
                        description = "產品分類",
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
                        })
                String category,
        @Schema(description = "產品狀態", example = "ACTIVE", required = true) ProductStatus status,
        @Schema(description = "是否有庫存", example = "true", required = true) boolean inStock,
        @Schema(description = "庫存數量", example = "50", minimum = "0", required = true)
                int stockQuantity,
        @Schema(description = "產品SKU（庫存單位）", example = "IPHONE15PRO-128GB-BLACK", maxLength = 50)
                String sku,
        @Schema(description = "產品品牌", example = "Apple", maxLength = 50) String brand,
        @Schema(description = "產品型號", example = "iPhone 15 Pro", maxLength = 100) String model,
        @Schema(description = "產品重量（公克）", example = "187.0", minimum = "0") Double weight,
        @Schema(description = "產品條碼", example = "1234567890123", pattern = "^\\d{12,13}$")
                String barcode,
        @Schema(description = "產品標籤列表", example = "[\"smartphone\", \"premium\", \"5G\"]")
                List<String> tags,
        @Schema(description = "產品圖片列表") List<ProductImageDto> images,
        @Schema(description = "產品屬性列表") List<ProductAttributeDto> attributes,
        @Schema(description = "產品保固期（月）", example = "12", minimum = "0", maximum = "120")
                Integer warrantyMonths,
        @Schema(description = "產品製造商", example = "Apple Inc.", maxLength = 100) String manufacturer,
        @Schema(description = "產品原產地", example = "中國", maxLength = 50) String countryOfOrigin,
        @Schema(
                        description = "產品創建時間",
                        example = "2024-01-15T10:30:00",
                        type = "string",
                        format = "date-time")
                LocalDateTime createdAt,
        @Schema(
                        description = "產品最後更新時間",
                        example = "2024-01-15T10:30:00",
                        type = "string",
                        format = "date-time")
                LocalDateTime updatedAt) {}
