package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 加價購規則 */
@ValueObject
public class AddOnPurchaseRule {
    private final ProductId mainProductId;
    private final ProductId addOnProductId;
    private final Money specialPrice;
    private final Money regularPrice;

    public AddOnPurchaseRule(
            ProductId mainProductId,
            ProductId addOnProductId,
            Money specialPrice,
            Money regularPrice) {
        this.mainProductId = mainProductId;
        this.addOnProductId = addOnProductId;
        this.specialPrice = specialPrice;
        this.regularPrice = regularPrice;
    }

    public ProductId getMainProductId() {
        return mainProductId;
    }

    public ProductId getAddOnProductId() {
        return addOnProductId;
    }

    public Money getSpecialPrice() {
        return specialPrice;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }
}
