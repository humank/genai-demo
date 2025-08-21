package solid.humank.genaidemo.application.promotion.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.application.promotion.service.CartSummaryConverter;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.CartSummary;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

@DisplayName("CartSummaryConverter 測試")
class CartSummaryConverterTest {

    private final CartSummaryConverter converter = new CartSummaryConverter();

    @Test
    @DisplayName("應該能夠將空購物車轉換為 CartSummary")
    void shouldConvertEmptyShoppingCartToCartSummary() {
        ShoppingCart emptyCart =
                new ShoppingCart(ShoppingCartId.generate(), new CustomerId("CUSTOMER-001"));

        CartSummary cartSummary = converter.toCartSummary(emptyCart);

        assertThat(cartSummary.totalAmount()).isEqualTo(Money.twd(0));
        assertThat(cartSummary.totalQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("應該能夠將有商品的購物車轉換為 CartSummary")
    void shouldConvertShoppingCartWithItemsToCartSummary() {
        ShoppingCart cart =
                new ShoppingCart(ShoppingCartId.generate(), new CustomerId("CUSTOMER-001"));

        cart.addItem(new ProductId("PROD-001"), 2, Money.twd(100));
        cart.addItem(new ProductId("PROD-002"), 1, Money.twd(300));

        CartSummary cartSummary = converter.toCartSummary(cart);

        assertThat(cartSummary.totalAmount()).isEqualTo(Money.twd(500));
        assertThat(cartSummary.totalQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("應該能夠處理單一商品的購物車")
    void shouldConvertSingleItemShoppingCart() {
        ShoppingCart cart =
                new ShoppingCart(ShoppingCartId.generate(), new CustomerId("CUSTOMER-001"));

        cart.addItem(new ProductId("PROD-001"), 5, Money.twd(50));

        CartSummary cartSummary = converter.toCartSummary(cart);

        assertThat(cartSummary.totalAmount()).isEqualTo(Money.twd(250));
        assertThat(cartSummary.totalQuantity()).isEqualTo(5);
    }
}
