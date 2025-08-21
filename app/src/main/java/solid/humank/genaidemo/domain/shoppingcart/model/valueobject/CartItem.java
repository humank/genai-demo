package solid.humank.genaidemo.domain.shoppingcart.model.valueobject;

import java.math.BigDecimal;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 購物車項目值對象 */
@ValueObject(name = "CartItem", description = "購物車項目值對象")
public record CartItem(ProductId productId, int quantity, Money unitPrice) {
    public CartItem {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能為空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("商品數量必須大於 0");
        }
        if (unitPrice == null || unitPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("商品價格必須大於 0");
        }
    }

    public Money totalPrice() {
        return unitPrice.multiply(quantity);
    }

    public CartItem increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("增加的數量必須大於 0");
        }
        return new CartItem(productId, quantity + additionalQuantity, unitPrice);
    }

    public CartItem updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("新數量必須大於 0");
        }
        return new CartItem(productId, newQuantity, unitPrice);
    }

    public CartItem updatePrice(Money newPrice) {
        if (newPrice == null || newPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("新價格必須大於 0");
        }
        return new CartItem(productId, quantity, newPrice);
    }
}
