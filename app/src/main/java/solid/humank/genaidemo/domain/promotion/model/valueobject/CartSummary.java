package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.util.List;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 購物車摘要 - 用於促銷規則評估的購物車數據 */
@ValueObject
public record CartSummary(
        CustomerId customerId, Money totalAmount, int totalQuantity, List<CartItemSummary> items) {

    /** 購物車項目摘要 */
    @ValueObject
    public record CartItemSummary(
            String productId,
            String productName,
            String productCategory,
            Money unitPrice,
            int quantity,
            Money totalPrice) {}
}
