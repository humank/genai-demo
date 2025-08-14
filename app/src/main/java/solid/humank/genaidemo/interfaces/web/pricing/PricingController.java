package solid.humank.genaidemo.interfaces.web.pricing;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.application.pricing.dto.CreatePricingRuleCommand;
import solid.humank.genaidemo.application.pricing.dto.PricingRuleDto;
import solid.humank.genaidemo.application.pricing.dto.ProductCategoryDto;
import solid.humank.genaidemo.application.pricing.dto.UpdateCommissionRateCommand;
import solid.humank.genaidemo.application.pricing.service.PricingApplicationService;
import solid.humank.genaidemo.interfaces.web.pricing.dto.CreatePricingRuleRequest;
import solid.humank.genaidemo.interfaces.web.pricing.dto.UpdateCommissionRateRequest;

/** 定價控制器 處理定價相關的HTTP請求 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingApplicationService pricingApplicationService;

    public PricingController(PricingApplicationService pricingApplicationService) {
        this.pricingApplicationService = pricingApplicationService;
    }

    /** 創建定價規則 */
    @PostMapping("/rules")
    public ResponseEntity<PricingRuleDto> createPricingRule(
            @RequestBody CreatePricingRuleRequest request) {
        // 將介面層的 ProductCategoryDto 轉換為應用層的 ProductCategoryDto
        ProductCategoryDto categoryDto = convertToAppProductCategory(request.getProductCategory());

        CreatePricingRuleCommand command =
                new CreatePricingRuleCommand(
                        request.getProductId(),
                        request.getPromotionId(),
                        request.getFinalPrice(),
                        request.getCurrency(),
                        request.getDiscountPercentage(),
                        request.getDiscountAmount(),
                        request.getEffectiveFrom(),
                        request.getEffectiveTo(),
                        categoryDto);

        PricingRuleDto result = pricingApplicationService.createPricingRule(command);
        return ResponseEntity.ok(result);
    }

    /** 更新佣金費率 */
    @PutMapping("/rules/{priceId}/commission")
    public ResponseEntity<PricingRuleDto> updateCommissionRate(
            @PathVariable String priceId, @RequestBody UpdateCommissionRateRequest request) {

        UpdateCommissionRateCommand command =
                new UpdateCommissionRateCommand(
                        priceId, request.getNormalRate(), request.getEventRate());

        PricingRuleDto result = pricingApplicationService.updateCommissionRate(command);
        return ResponseEntity.ok(result);
    }

    /** 獲取產品的定價規則 */
    @GetMapping("/rules/product/{productId}")
    public ResponseEntity<List<PricingRuleDto>> getPricingRulesForProduct(
            @PathVariable String productId) {
        List<PricingRuleDto> rules = pricingApplicationService.getPricingRulesForProduct(productId);
        return ResponseEntity.ok(rules);
    }

    /** 獲取產品類別的定價規則 */
    @GetMapping("/rules/category/{category}")
    public ResponseEntity<List<PricingRuleDto>> getPricingRulesByCategory(
            @PathVariable
                    solid.humank.genaidemo.interfaces.web.pricing.dto.ProductCategoryDto category) {
        // 將介面層的 ProductCategoryDto 轉換為應用層的 ProductCategoryDto
        ProductCategoryDto categoryDto = convertToAppProductCategory(category);
        List<PricingRuleDto> rules =
                pricingApplicationService.getPricingRulesByCategory(categoryDto);
        return ResponseEntity.ok(rules);
    }

    /** 將介面層的 ProductCategoryDto 轉換為應用層的 ProductCategoryDto */
    private ProductCategoryDto convertToAppProductCategory(
            solid.humank.genaidemo.interfaces.web.pricing.dto.ProductCategoryDto dto) {
        if (dto == null) {
            return ProductCategoryDto.GENERAL;
        }

        return switch (dto) {
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
}
