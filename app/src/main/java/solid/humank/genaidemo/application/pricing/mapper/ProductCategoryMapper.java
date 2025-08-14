package solid.humank.genaidemo.application.pricing.mapper;

import solid.humank.genaidemo.application.pricing.dto.ProductCategoryDto;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;

/** 產品類別映射器 用於應用層與領域層之間的數據轉換 */
public class ProductCategoryMapper {

    /** 將領域層的 ProductCategory 轉換為應用層的 ProductCategoryDto */
    public static ProductCategoryDto toDto(ProductCategory domain) {
        if (domain == null) {
            return ProductCategoryDto.GENERAL;
        }

        return switch (domain) {
            case ELECTRONICS -> ProductCategoryDto.ELECTRONICS;
            case FASHION -> ProductCategoryDto.FASHION;
            case GROCERIES -> ProductCategoryDto.GROCERIES;
            case HOME_APPLIANCES -> ProductCategoryDto.HOME_APPLIANCES;
            case BEAUTY -> ProductCategoryDto.BEAUTY;
            case SPORTS -> ProductCategoryDto.SPORTS;
            case BOOKS -> ProductCategoryDto.BOOKS;
            case TOYS -> ProductCategoryDto.TOYS;
            case AUTOMOTIVE -> ProductCategoryDto.AUTOMOTIVE;
            case HEALTH -> ProductCategoryDto.HEALTH;
            case GENERAL -> ProductCategoryDto.GENERAL;
        };
    }

    /** 將應用層的 ProductCategoryDto 轉換為領域層的 ProductCategory */
    public static ProductCategory toDomain(ProductCategoryDto dto) {
        if (dto == null) {
            return ProductCategory.GENERAL;
        }

        return switch (dto) {
            case ELECTRONICS -> ProductCategory.ELECTRONICS;
            case FASHION -> ProductCategory.FASHION;
            case GROCERIES -> ProductCategory.GROCERIES;
            case HOME_APPLIANCES -> ProductCategory.HOME_APPLIANCES;
            case BEAUTY -> ProductCategory.BEAUTY;
            case SPORTS -> ProductCategory.SPORTS;
            case BOOKS -> ProductCategory.BOOKS;
            case TOYS -> ProductCategory.TOYS;
            case AUTOMOTIVE -> ProductCategory.AUTOMOTIVE;
            case HEALTH -> ProductCategory.HEALTH;
            case GENERAL -> ProductCategory.GENERAL;
        };
    }
}
