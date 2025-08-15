package solid.humank.genaidemo.domain.shoppingcart.model.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;

/** ShoppingCart 聚合根單元測試 測試購物車聚合根的業務邏輯和領域事件 */
@DisplayName("ShoppingCart 聚合根測試")
class ShoppingCartTest {

    private ShoppingCart shoppingCart;
    private ShoppingCartId cartId;
    private CustomerId customerId;
    private ProductId productId1;
    private ProductId productId2;
    private Money unitPrice1;
    private Money unitPrice2;

    @BeforeEach
    void setUp() {
        cartId = new ShoppingCartId("cart-001");
        customerId = new CustomerId("customer-001");
        productId1 = new ProductId("PROD-001");
        productId2 = new ProductId("PROD-002");
        unitPrice1 = Money.twd(1000);
        unitPrice2 = Money.twd(2000);

        shoppingCart = new ShoppingCart(cartId, customerId);
    }

    @Test
    @DisplayName("應該能夠創建購物車")
    void shouldCreateShoppingCart() {
        // Then
        assertThat(shoppingCart.getId()).isEqualTo(cartId);
        assertThat(shoppingCart.getConsumerId()).isEqualTo(customerId);
        assertThat(shoppingCart.getItems()).isEmpty();
        assertThat(shoppingCart.getTotalAmount()).isEqualTo(Money.zero());
        assertThat(shoppingCart.getStatus()).isEqualTo(ShoppingCartStatus.ACTIVE);
    }

    @Test
    @DisplayName("應該能夠添加商品到購物車")
    void shouldAddItemToCart() {
        // Given
        int quantity = 2;

        // When
        shoppingCart.addItem(productId1, quantity, unitPrice1);

        // Then
        List<CartItem> items = shoppingCart.getItems();
        assertThat(items).hasSize(1);

        CartItem item = items.get(0);
        assertThat(item.productId()).isEqualTo(productId1);
        assertThat(item.quantity()).isEqualTo(quantity);
        assertThat(item.unitPrice()).isEqualTo(unitPrice1);
        assertThat(item.totalPrice()).isEqualTo(unitPrice1.multiply(quantity));
    }

    @Test
    @DisplayName("添加相同商品應該增加數量")
    void shouldIncreaseQuantityWhenAddingSameProduct() {
        // Given
        shoppingCart.addItem(productId1, 1, unitPrice1);

        // When
        shoppingCart.addItem(productId1, 2, unitPrice1);

        // Then
        List<CartItem> items = shoppingCart.getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("應該能夠更新商品數量")
    void shouldUpdateItemQuantity() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1);
        int newQuantity = 5;

        // When
        shoppingCart.updateItemQuantity(productId1, newQuantity);

        // Then
        CartItem item = shoppingCart.findItem(productId1);
        assertThat(item).isNotNull();
        assertThat(item.quantity()).isEqualTo(newQuantity);
    }

    @Test
    @DisplayName("應該能夠移除商品")
    void shouldRemoveItem() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1);
        shoppingCart.addItem(productId2, 1, unitPrice2);

        // When
        shoppingCart.removeItem(productId1);

        // Then
        assertThat(shoppingCart.getItems()).hasSize(1);
        assertThat(shoppingCart.findItem(productId1)).isNull();
        assertThat(shoppingCart.findItem(productId2)).isNotNull();
    }

    @Test
    @DisplayName("應該能夠清空購物車")
    void shouldClearCart() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1);
        shoppingCart.addItem(productId2, 1, unitPrice2);

        // When
        shoppingCart.clear();

        // Then
        assertThat(shoppingCart.getItems()).isEmpty();
        assertThat(shoppingCart.getTotalAmount()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("應該能夠計算總金額")
    void shouldCalculateTotalAmount() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1); // 2 * 1000 = 2000
        shoppingCart.addItem(productId2, 1, unitPrice2); // 1 * 2000 = 2000

        // When
        Money totalAmount = shoppingCart.getTotalAmount();

        // Then
        assertThat(totalAmount).isEqualTo(Money.twd(4000));
    }

    @Test
    @DisplayName("應該能夠檢查是否為空")
    void shouldCheckIfEmpty() {
        // When & Then
        assertThat(shoppingCart.isEmpty()).isTrue();

        // Given
        shoppingCart.addItem(productId1, 1, unitPrice1);

        // When & Then
        assertThat(shoppingCart.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("應該能夠獲取商品總數")
    void shouldGetTotalItemCount() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1);
        shoppingCart.addItem(productId2, 3, unitPrice2);

        // When
        int totalCount = shoppingCart.getTotalItemCount();

        // Then
        assertThat(totalCount).isEqualTo(5);
    }

    @Test
    @DisplayName("應該能夠檢查是否包含特定商品")
    void shouldCheckIfContainsProduct() {
        // Given
        shoppingCart.addItem(productId1, 1, unitPrice1);

        // When & Then
        assertThat(shoppingCart.containsProduct(productId1)).isTrue();
        assertThat(shoppingCart.containsProduct(productId2)).isFalse();
    }

    @Test
    @DisplayName("更新不存在的商品數量應該拋出異常")
    void shouldThrowExceptionWhenUpdatingNonExistentItem() {
        // When & Then
        assertThatThrownBy(() -> shoppingCart.updateItemQuantity(productId1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("移除不存在的商品應該拋出異常")
    void shouldThrowExceptionWhenRemovingNonExistentItem() {
        // When & Then
        assertThatThrownBy(() -> shoppingCart.removeItem(productId1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("添加無效數量的商品應該拋出異常")
    void shouldThrowExceptionWhenAddingInvalidQuantity() {
        // When & Then
        assertThatThrownBy(() -> shoppingCart.addItem(productId1, 0, unitPrice1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> shoppingCart.addItem(productId1, -1, unitPrice1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("更新為無效數量應該拋出異常")
    void shouldThrowExceptionWhenUpdatingToInvalidQuantity() {
        // Given
        shoppingCart.addItem(productId1, 1, unitPrice1);

        // When & Then
        assertThatThrownBy(() -> shoppingCart.updateItemQuantity(productId1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> shoppingCart.updateItemQuantity(productId1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("應該能夠檢查購物車是否達到最小訂單金額")
    void shouldCheckMinimumOrderAmount() {
        // Given
        Money minimumAmount = Money.twd(3000);
        shoppingCart.addItem(productId1, 2, unitPrice1); // 2000

        // When & Then
        assertThat(shoppingCart.meetsMinimumAmount(minimumAmount)).isFalse();

        // Given
        shoppingCart.addItem(productId2, 1, unitPrice2); // +2000 = 4000 total

        // When & Then
        assertThat(shoppingCart.meetsMinimumAmount(minimumAmount)).isTrue();
    }

    @Test
    @DisplayName("應該能夠獲取商品種類數")
    void shouldGetUniqueProductCount() {
        // Given
        shoppingCart.addItem(productId1, 2, unitPrice1);
        shoppingCart.addItem(productId2, 3, unitPrice2);

        // When
        int uniqueCount = shoppingCart.getUniqueProductCount();

        // Then
        assertThat(uniqueCount).isEqualTo(2);
    }
}
