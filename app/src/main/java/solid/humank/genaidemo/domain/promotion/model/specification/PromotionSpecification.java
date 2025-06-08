package solid.humank.genaidemo.domain.promotion.model.specification;

import solid.humank.genaidemo.domain.common.specification.Specification;

/**
 * 促銷規格接口
 * 用於檢查促銷上下文是否滿足促銷條件
 */
public interface PromotionSpecification extends Specification<PromotionContext> {
    
    /**
     * 檢查促銷上下文是否滿足規格
     * 
     * @param context 促銷上下文，包含訂單和客戶信息
     * @return 是否滿足規格
     */
    @Override
    boolean isSatisfiedBy(PromotionContext context);
}