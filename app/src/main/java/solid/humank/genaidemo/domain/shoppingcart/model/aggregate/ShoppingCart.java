package solid.humank.genaidemo.domain.shoppingcart.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.exception.CartItemNotFoundException;
import solid.humank.genaidemo.domain.shoppingcart.exception.InvalidQuantityException;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;

/** 購物車聚合根 */
@AggregateRoot(name = "ShoppingCart", description = "購物車聚合根，管理消費者的購物車狀態和商品項目")
public class ShoppingCart {

    private final ShoppingCartId id;
    private final CustomerId consumerId;
    private List<CartItem> items;
    private ShoppingCartStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ShoppingCart(ShoppingCartId id, CustomerId consumerId) {
        this.id = id;
        this.consumerId = consumerId;
        this.items = new ArrayList<>();
        this.status = ShoppingCartStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public ShoppingCartId getId() {
        return id;
    }

    public CustomerId getConsumerId() {
        return consumerId;
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public ShoppingCartStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 業務方法

    /** 添加商品到購物車 */
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("商品數量必須大於 0");
        }

        Optional<CartItem> existingItem = findItemOptional(productId);
        if (existingItem.isPresent()) {
            // 更新現有項目數量
            CartItem updatedItem = existingItem.get().increaseQuantity(quantity);
            replaceItem(existingItem.get(), updatedItem);
        } else {
            // 添加新項目
            CartItem newItem = new CartItem(productId, quantity, unitPrice);
            items.add(newItem);
        }

        updateTimestamp();
        // TODO: 發布領域事件
        // registerEvent(new CartItemAddedEvent(this.id, productId, quantity));
    }

    /** 更新商品數量 */
    public void updateItemQuantity(ProductId productId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new InvalidQuantityException("商品數量必須大於 0");
        }

        CartItem existingItem =
                findItemOptional(productId)
                        .orElseThrow(
                                () -> new CartItemNotFoundException("購物車中找不到商品: " + productId));

        CartItem updatedItem = existingItem.updateQuantity(newQuantity);
        replaceItem(existingItem, updatedItem);
        updateTimestamp();
    }

    /** 移除商品 */
    public void removeItem(ProductId productId) {
        CartItem itemToRemove =
                findItemOptional(productId)
                        .orElseThrow(
                                () -> new CartItemNotFoundException("購物車中找不到商品: " + productId));

        items.remove(itemToRemove);
        updateTimestamp();
        // TODO: 發布領域事件
        // registerEvent(new CartItemRemovedEvent(this.id, productId));
    }

    /** 清空購物車 */
    public void clear() {
        items.clear();
        updateTimestamp();
    }

    /** 計算總金額 */
    public Money calculateTotal() {
        if (items.isEmpty()) {
            return Money.twd(0);
        }

        return items.stream().map(CartItem::totalPrice).reduce(Money::add).orElse(Money.twd(0));
    }

    /** 獲取商品總數量 */
    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }

    /** 檢查是否為空 */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** 檢查是否包含特定商品 */
    public boolean containsProduct(ProductId productId) {
        return findItemOptional(productId).isPresent();
    }

    /** 更新購物車狀態 */
    public void updateStatus(ShoppingCartStatus newStatus) {
        this.status = newStatus;
        updateTimestamp();
    }

    // 測試需要的額外方法

    /** 獲取總金額（測試兼容方法） */
    public Money getTotalAmount() {
        return calculateTotal();
    }

    /** 獲取商品總數（測試兼容方法） */
    public int getTotalItemCount() {
        return getTotalQuantity();
    }

    /** 檢查是否達到最小訂單金額 */
    public boolean meetsMinimumAmount(Money minimumAmount) {
        return calculateTotal().getAmount().compareTo(minimumAmount.getAmount()) >= 0;
    }

    /** 獲取商品種類數 */
    public int getUniqueProductCount() {
        return items.size();
    }

    /** 查找商品項目（測試兼容方法） */
    public CartItem findItem(ProductId productId) {
        return findItemOptional(productId).orElse(null);
    }

    // 私有輔助方法

    private Optional<CartItem> findItemOptional(ProductId productId) {
        return items.stream().filter(item -> item.productId().equals(productId)).findFirst();
    }

    private void replaceItem(CartItem oldItem, CartItem newItem) {
        int index = items.indexOf(oldItem);
        if (index >= 0) {
            items.set(index, newItem);
        }
    }

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ShoppingCart that = (ShoppingCart) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
