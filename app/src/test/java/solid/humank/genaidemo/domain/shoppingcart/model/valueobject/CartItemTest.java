package solid.humank.genaidemo.domain.shoppingcart.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** CartItem 值對象單元測試 測試購物車項目的不變性和業務邏輯 */
@DisplayName("CartItem 值對象測試")
class CartItemTest {

    @Test
    @DisplayName("應該能夠創建購物車項目")
    void shouldCreateCartItem() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        int quantity = 2;
        Money unitPrice = Money.twd(1000);

        // When
        CartItem cartItem = new CartItem(productId, quantity, unitPrice);

        // Then
        assertThat(cartItem.productId()).isEqualTo(productId);
        assertThat(cartItem.quantity()).isEqualTo(quantity);
        assertThat(cartItem.unitPrice()).isEqualTo(unitPrice);
    }

    @Test
    @DisplayName("應該能夠計算總價")
    void shouldCalculateTotalPrice() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        int quantity = 3;
        Money unitPrice = Money.twd(1500);
        CartItem cartItem = new CartItem(productId, quantity, unitPrice);

        // When
        Money totalPrice = cartItem.totalPrice();

        // Then
        assertThat(totalPrice).isEqualTo(Money.twd(4500));
    }

    @Test
    @DisplayName("不應該允許零或負數數量")
    void shouldNotAllowZeroOrNegativeQuantity() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        Money unitPrice = Money.twd(1000);

        // When & Then
        assertThatThrownBy(() -> new CartItem(productId, 0, unitPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品數量必須大於 0");

        assertThatThrownBy(() -> new CartItem(productId, -1, unitPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品數量必須大於 0");
    }

    @Test
    @DisplayName("不應該允許零或負數價格")
    void shouldNotAllowZeroOrNegativePrice() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        int quantity = 1;

        // When & Then
        assertThatThrownBy(() -> new CartItem(productId, quantity, Money.twd(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品價格必須大於 0");

        assertThatThrownBy(() -> new CartItem(productId, quantity, Money.twd(-100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品價格必須大於 0");
    }

    @Test
    @DisplayName("應該能夠增加數量")
    void shouldIncreaseQuantity() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem originalItem = new CartItem(productId, 2, Money.twd(1000));
        int additionalQuantity = 3;

        // When
        CartItem newItem = originalItem.increaseQuantity(additionalQuantity);

        // Then
        assertThat(newItem.quantity()).isEqualTo(5);
        assertThat(newItem.productId()).isEqualTo(productId);
        assertThat(newItem.unitPrice()).isEqualTo(Money.twd(1000));
        assertThat(originalItem.quantity()).isEqualTo(2); // 原對象不變
    }

    @Test
    @DisplayName("應該能夠更新數量")
    void shouldUpdateQuantity() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem originalItem = new CartItem(productId, 2, Money.twd(1000));
        int newQuantity = 5;

        // When
        CartItem newItem = originalItem.updateQuantity(newQuantity);

        // Then
        assertThat(newItem.quantity()).isEqualTo(newQuantity);
        assertThat(newItem.productId()).isEqualTo(productId);
        assertThat(newItem.unitPrice()).isEqualTo(Money.twd(1000));
        assertThat(originalItem.quantity()).isEqualTo(2); // 原對象不變
    }

    @Test
    @DisplayName("應該能夠更新價格")
    void shouldUpdatePrice() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem originalItem = new CartItem(productId, 2, Money.twd(1000));
        Money newPrice = Money.twd(1200);

        // When
        CartItem newItem = originalItem.updatePrice(newPrice);

        // Then
        assertThat(newItem.unitPrice()).isEqualTo(newPrice);
        assertThat(newItem.productId()).isEqualTo(productId);
        assertThat(newItem.quantity()).isEqualTo(2);
        assertThat(originalItem.unitPrice()).isEqualTo(Money.twd(1000)); // 原對象不變
    }

    @Test
    @DisplayName("更新為無效數量應該拋出異常")
    void shouldThrowExceptionWhenUpdatingToInvalidQuantity() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem cartItem = new CartItem(productId, 2, Money.twd(1000));

        // When & Then
        assertThatThrownBy(() -> cartItem.updateQuantity(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> cartItem.updateQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("增加無效數量應該拋出異常")
    void shouldThrowExceptionWhenIncreasingByInvalidQuantity() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem cartItem = new CartItem(productId, 2, Money.twd(1000));

        // When & Then
        assertThatThrownBy(() -> cartItem.increaseQuantity(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> cartItem.increaseQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("值對象應該是不可變的")
    void shouldBeImmutable() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        CartItem originalItem = new CartItem(productId, 2, Money.twd(1000));

        // When
        CartItem newItem = originalItem.updateQuantity(5);

        // Then
        assertThat(originalItem.quantity()).isEqualTo(2);
        assertThat(newItem.quantity()).isEqualTo(5);
        assertThat(originalItem).isNotEqualTo(newItem);
    }

    @Test
    @DisplayName("相同內容的值對象應該相等")
    void shouldBeEqualWithSameContent() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        Money unitPrice = Money.twd(1000);
        CartItem item1 = new CartItem(productId, 2, unitPrice);
        CartItem item2 = new CartItem(productId, 2, unitPrice);

        // Then
        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }

    @Test
    @DisplayName("不同內容的值對象應該不相等")
    void shouldNotBeEqualWithDifferentContent() {
        // Given
        ProductId productId1 = new ProductId("PROD-001");
        ProductId productId2 = new ProductId("PROD-002");
        Money unitPrice = Money.twd(1000);

        CartItem item1 = new CartItem(productId1, 2, unitPrice);
        CartItem item2 = new CartItem(productId2, 2, unitPrice);
        CartItem item3 = new CartItem(productId1, 3, unitPrice);
        CartItem item4 = new CartItem(productId1, 2, Money.twd(1200));

        // Then
        assertThat(item1).isNotEqualTo(item2);
        assertThat(item1).isNotEqualTo(item3);
        assertThat(item1).isNotEqualTo(item4);
    }

    @Test
    @DisplayName("應該能夠處理小數價格")
    void shouldHandleDecimalPrices() {
        // Given
        ProductId productId = new ProductId("PROD-001");
        Money unitPrice = Money.twd(99.99);
        CartItem cartItem = new CartItem(productId, 3, unitPrice);

        // When
        Money totalPrice = cartItem.totalPrice();

        // Then
        assertThat(totalPrice.getAmount()).isEqualByComparingTo(new BigDecimal("299.97"));
    }
}
