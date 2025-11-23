package solid.humank.genaidemo.domain.promotion.model.specification;

import java.util.List;

import solid.humank.genaidemo.domain.common.annotations.Specification;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule;

/** 加價購規格 檢查訂單是否滿足加價購條件 */
@Specification(name = "AddOnPurchaseSpecification", description = "加價購規格，檢查訂單是否滿足加價購條件")
public class AddOnPurchaseSpecification implements PromotionSpecification {
    private final AddOnPurchaseRule rule;

    public AddOnPurchaseSpecification(AddOnPurchaseRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean isSatisfiedBy(PromotionContext context) {
        Order order = context.getOrder();
        List<OrderItem> items = order.getItems();

        // 檢查訂單中是否包含主要商品
        return items.stream()
                .anyMatch(
                        item -> item.getProductId()
                                .equals(rule.getMainProductId().getId()));
    }
}
