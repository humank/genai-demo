package solid.humank.genaidemo.interfaces.web.pricing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
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
@Tag(name = "定價管理", description = "提供定價策略、佣金費率管理和產品定價規則的相關功能，支援複雜的定價邏輯和促銷規則配置")
@Validated
public class PricingController {

    private final PricingApplicationService pricingApplicationService;

    public PricingController(PricingApplicationService pricingApplicationService) {
        this.pricingApplicationService = pricingApplicationService;
    }

    /** 創建定價規則 */
    @PostMapping("/rules")
    @Operation(summary = "創建定價規則", description = "為特定產品或產品類別創建新的定價規則，包含折扣策略、有效期間和佣金設定。支援複雜的定價邏輯配置。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "定價規則創建成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PricingRuleDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效，如必填欄位缺失、日期格式錯誤或定價邏輯衝突",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "業務規則違反，如定價規則衝突、有效期間重疊或折扣設定不合理",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PricingRuleDto> createPricingRule(
            @Parameter(description = "定價規則創建請求，包含產品資訊、定價策略和有效期間", required = true)
                    @Valid
                    @RequestBody
                    CreatePricingRuleRequest request) {
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
    @Operation(summary = "更新佣金費率", description = "更新指定定價規則的佣金費率設定，包含一般費率和活動期間特殊費率。支援差異化佣金策略配置。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "佣金費率更新成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = PricingRuleDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效，如費率值超出合理範圍或格式錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "指定的定價規則不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "業務規則違反，如佣金費率設定不符合商業邏輯或超出允許範圍",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<PricingRuleDto> updateCommissionRate(
            @Parameter(
                            description = "定價規則的唯一識別碼",
                            required = true,
                            example = "price-123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    String priceId,
            @Parameter(description = "佣金費率更新請求，包含一般費率和活動費率", required = true) @Valid @RequestBody
                    UpdateCommissionRateRequest request) {

        UpdateCommissionRateCommand command =
                new UpdateCommissionRateCommand(
                        priceId, request.getNormalRate(), request.getEventRate());

        PricingRuleDto result = pricingApplicationService.updateCommissionRate(command);
        return ResponseEntity.ok(result);
    }

    /** 獲取產品的定價規則 */
    @GetMapping("/rules/product/{productId}")
    @Operation(
            summary = "獲取產品的定價規則",
            description = "查詢指定產品的所有有效定價規則，包含基礎價格、折扣策略、佣金設定和有效期間。結果按優先級和有效期間排序。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取產品定價規則列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = PricingRuleDto.class,
                                                        type = "array"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效，如產品ID格式錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "指定的產品不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<List<PricingRuleDto>> getPricingRulesForProduct(
            @Parameter(
                            description = "產品的唯一識別碼",
                            required = true,
                            example = "prod-123e4567-e89b-12d3-a456-426614174000")
                    @PathVariable
                    String productId) {
        List<PricingRuleDto> rules = pricingApplicationService.getPricingRulesForProduct(productId);
        return ResponseEntity.ok(rules);
    }

    /** 獲取產品類別的定價規則 */
    @GetMapping("/rules/category/{category}")
    @Operation(
            summary = "獲取產品類別的定價規則",
            description = "查詢指定產品類別的所有定價規則，用於類別級別的定價策略管理。支援批量定價規則查詢和類別定價分析。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取產品類別定價規則列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = PricingRuleDto.class,
                                                        type = "array"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效，如產品類別值不在允許範圍內",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "業務規則違反，如查詢的產品類別不支援定價規則",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<List<PricingRuleDto>> getPricingRulesByCategory(
            @Parameter(
                            description = "產品類別，用於篩選特定類別的定價規則",
                            required = true,
                            example = "ELECTRONICS",
                            schema =
                                    @Schema(
                                            implementation =
                                                    solid.humank.genaidemo.interfaces.web.pricing
                                                            .dto.ProductCategoryDto.class,
                                            allowableValues = {
                                                "ELECTRONICS",
                                                "FASHION",
                                                "GROCERIES",
                                                "HOME_APPLIANCES",
                                                "BEAUTY",
                                                "SPORTS",
                                                "BOOKS",
                                                "TOYS",
                                                "AUTOMOTIVE",
                                                "HEALTH",
                                                "GENERAL"
                                            }))
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
