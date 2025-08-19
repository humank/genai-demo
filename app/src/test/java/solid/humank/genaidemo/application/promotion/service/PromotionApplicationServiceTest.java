package solid.humank.genaidemo.application.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.exception.PromotionNotFoundException;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.CartSummary;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionApplicationService 測試")
class PromotionApplicationServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private CartSummaryConverter cartSummaryConverter;

    @InjectMocks private PromotionApplicationService promotionApplicationService;

    @Test
    @DisplayName("應該能夠計算購物車的促銷折扣")
    void shouldCalculatePromotionDiscount() {
        // Given
        String promotionId = "PROMO-001";
        ShoppingCart shoppingCart = createTestShoppingCart();
        CartSummary cartSummary = createTestCartSummary();
        Promotion promotion = createTestPromotion();
        Money expectedDiscount = Money.twd(100);

        when(cartSummaryConverter.toCartSummary(shoppingCart)).thenReturn(cartSummary);
        when(promotionRepository.findById(PromotionId.of(promotionId)))
                .thenReturn(Optional.of(promotion));
        when(promotion.calculateDiscount(cartSummary)).thenReturn(expectedDiscount);

        // When
        Money actualDiscount =
                promotionApplicationService.calculatePromotionDiscount(shoppingCart, promotionId);

        // Then
        assertThat(actualDiscount).isEqualTo(expectedDiscount);
    }

    @Test
    @DisplayName("當促銷不存在時應該拋出異常")
    void shouldThrowExceptionWhenPromotionNotFound() {
        // Given
        String promotionId = "NONEXISTENT-PROMO";
        ShoppingCart shoppingCart = createTestShoppingCart();
        CartSummary cartSummary = createTestCartSummary();

        when(cartSummaryConverter.toCartSummary(shoppingCart)).thenReturn(cartSummary);
        when(promotionRepository.findById(PromotionId.of(promotionId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(
                        () ->
                                promotionApplicationService.calculatePromotionDiscount(
                                        shoppingCart, promotionId))
                .isInstanceOf(PromotionNotFoundException.class)
                .hasMessageContaining("促銷活動不存在: " + promotionId);
    }

    private ShoppingCart createTestShoppingCart() {
        return new ShoppingCart(ShoppingCartId.generate(), new CustomerId("CUSTOMER-001"));
    }

    private CartSummary createTestCartSummary() {
        return new CartSummary(
                new CustomerId("CUSTOMER-001"), Money.twd(1000), 2, java.util.List.of());
    }

    private Promotion createTestPromotion() {
        // 這裡需要根據實際的 Promotion 構造函數來創建
        // 由於我們使用 mock，這個方法可能不會被實際調用
        return org.mockito.Mockito.mock(Promotion.class);
    }
}
