package solid.humank.genaidemo.domain.shoppingcart.service;

import java.util.List;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.repository.ShoppingCartRepository;

/**
 * 購物車領域服務
 * 處理購物車相關的複雜業務邏輯和跨聚合根操作
 */
@DomainService(name = "ShoppingCartDomainService", description = "購物車領域服務，處理購物車管理的複雜業務邏輯", boundedContext = "ShoppingCart")
public class ShoppingCartDomainService {

    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCartDomainService(ShoppingCartRepository shoppingCartRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
    }

    /**
     * 合併購物車
     * 當用戶登入時，將匿名購物車與用戶購物車合併
     * 
     * @param anonymousCartId 匿名購物車ID
     * @param userCartId      用戶購物車ID
     * @return 合併後的購物車
     */
    public ShoppingCart mergeCarts(ShoppingCartId anonymousCartId, ShoppingCartId userCartId) {
        ShoppingCart anonymousCart = shoppingCartRepository.findById(anonymousCartId).orElse(null);
        ShoppingCart userCart = shoppingCartRepository.findById(userCartId).orElse(null);

        if (anonymousCart == null) {
            return userCart;
        }

        if (userCart == null) {
            return anonymousCart;
        }

        // 將匿名購物車的商品添加到用戶購物車
        List<CartItem> anonymousItems = anonymousCart.getItems();
        for (CartItem item : anonymousItems) {
            userCart.addItem(item.productId(), item.quantity(), item.unitPrice());
        }

        // 刪除匿名購物車
        shoppingCartRepository.delete(anonymousCart);
        shoppingCartRepository.save(userCart);

        return userCart;
    }

    /**
     * 計算購物車總價（包含折扣）
     * 
     * @param cartId     購物車ID
     * @param customerId 客戶ID
     * @return 總價
     */
    public Money calculateTotalWithDiscounts(ShoppingCartId cartId, CustomerId customerId) {
        ShoppingCart cart = shoppingCartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            return Money.ZERO;
        }

        // 簡化實現 - 計算小計
        Money subtotal = cart.getTotalAmount();

        // 這裡可以應用各種折扣邏輯
        // 例如：會員折扣、促銷折扣、優惠券等
        Money discount = calculateApplicableDiscounts(cart, customerId);

        return subtotal.subtract(discount);
    }

    /**
     * 驗證購物車是否可以結帳
     * 
     * @param cartId 購物車ID
     * @return 驗證結果
     */
    public CartValidationResult validateForCheckout(ShoppingCartId cartId) {
        ShoppingCart cart = shoppingCartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            return CartValidationResult.invalid("購物車不存在");
        }

        if (cart.isEmpty()) {
            return CartValidationResult.invalid("購物車為空");
        }

        // 檢查商品是否仍然有效
        for (CartItem item : cart.getItems()) {
            if (!isProductAvailable(item.productId().getId())) {
                return CartValidationResult.invalid("商品 " + item.productId().getId() + " 已下架");
            }

            if (!hasEnoughStock(item.productId().getId(), item.quantity())) {
                return CartValidationResult.invalid("商品 " + item.productId().getId() + " 庫存不足");
            }
        }

        return CartValidationResult.valid();
    }

    /**
     * 清理過期的購物車
     * 
     * @param customerId 客戶ID
     */
    public void cleanupExpiredCarts(CustomerId customerId) {
        // 簡化實現 - 假設沒有過期購物車需要清理
        // List<ShoppingCart> expiredCarts =
        // shoppingCartRepository.findExpiredByCustomerId(customerId);
        // for (ShoppingCart cart : expiredCarts) {
        // shoppingCartRepository.delete(cart);
    }

    /**
     * 恢復已刪除的購物車項目
     * 
     * @param cartId    購物車ID
     * @param productId 產品ID
     * @return 是否恢復成功
     */
    public boolean restoreDeletedItem(ShoppingCartId cartId, String productId) {
        ShoppingCart cart = shoppingCartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            return false;
        }

        // 簡化實現 - 假設恢復成功
        return true;
    }

    // 私有輔助方法
    private Money calculateApplicableDiscounts(ShoppingCart cart, CustomerId customerId) {
        // 這裡實現折扣計算邏輯
        // 可以與促銷服務、會員服務等協作
        return Money.ZERO;
    }

    private boolean isProductAvailable(String productId) {
        // 檢查產品是否仍然可用
        // 這裡可以與產品服務協作
        return true;
    }

    private boolean hasEnoughStock(String productId, int quantity) {
        // 檢查庫存是否充足
        // 這裡可以與庫存服務協作
        return true;
    }

    /**
     * 購物車驗證結果
     */
    public static class CartValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private CartValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static CartValidationResult valid() {
            return new CartValidationResult(true, null);
        }

        public static CartValidationResult invalid(String errorMessage) {
            return new CartValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}