package solid.humank.genaidemo.application.promotion.service;

import java.util.List;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.promotion.model.valueobject.CartSummary;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 購物車摘要轉換器 - 將 ShoppingCart 轉換為 CartSummary 用於促銷規則評估 */
@Component
public class CartSummaryConverter {

    /** 將 ShoppingCart 轉換為 CartSummary */
    public CartSummary toCartSummary(ShoppingCart shoppingCart) {
        List<CartSummary.CartItemSummary> itemSummaries =
                shoppingCart.getItems().stream()
                        .map(
                                item ->
                                        new CartSummary.CartItemSummary(
                                                item.productId().getId(),
                                                "Product "
                                                        + item.productId()
                                                                .getId(), // 商品名稱 - 在實際應用中應該從商品服務獲取
                                                "Unknown", // 商品分類 - 在實際應用中應該從商品服務獲取
                                                item.unitPrice(),
                                                item.quantity(),
                                                item.totalPrice()))
                        .toList();

        return new CartSummary(
                shoppingCart.getConsumerId(),
                shoppingCart.calculateTotal(),
                shoppingCart.getTotalQuantity(),
                itemSummaries);
    }
}
