package solid.humank.genaidemo.domain.shoppingcart.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.exception.CartItemNotFoundException;
import solid.humank.genaidemo.domain.shoppingcart.exception.InvalidQuantityException;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartCheckedOutEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartClearedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartCreatedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartItemAddedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartItemQuantityUpdatedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartItemRemovedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartStatusChangedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;

/** 購物車聚合根 */
@AggregateRoot(name = "ShoppingCart", description = "購物車聚合根，管理消費者的購物車狀態和商品項目", boundedContext = "ShoppingCart", version = "1.0")
public class ShoppingCart extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

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

        // 發布購物車創建事件
        collectEvent(CartCreatedEvent.create(id, consumerId));
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

        // 發布購物車商品添加事件
        collectEvent(
                CartItemAddedEvent.create(this.id, this.consumerId, productId, quantity, unitPrice));
    }

    /** 更新商品數量 */
    public void updateItemQuantity(ProductId productId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new InvalidQuantityException("商品數量必須大於 0");
        }

        CartItem existingItem = findItemOptional(productId)
                .orElseThrow(
                        () -> new CartItemNotFoundException("購物車中找不到商品: " + productId));

        int oldQuantity = existingItem.quantity();
        CartItem updatedItem = existingItem.updateQuantity(newQuantity);
        replaceItem(existingItem, updatedItem);
        updateTimestamp();

        // 發布購物車商品數量更新事件
        collectEvent(
                CartItemQuantityUpdatedEvent.create(
                        this.id,
                        this.consumerId,
                        productId,
                        oldQuantity,
                        newQuantity,
                        existingItem.unitPrice()));
    }

    /** 移除商品 */
    public void removeItem(ProductId productId) {
        CartItem itemToRemove = findItemOptional(productId)
                .orElseThrow(
                        () -> new CartItemNotFoundException("購物車中找不到商品: " + productId));

        items.remove(itemToRemove);
        updateTimestamp();

        // 發布購物車商品移除事件
        collectEvent(
                CartItemRemovedEvent.create(
                        this.id,
                        this.consumerId,
                        productId,
                        itemToRemove.quantity(),
                        itemToRemove.unitPrice()));
    }

    /** 清空購物車 */
    public void clear() {
        List<CartItem> itemsToRemove = new ArrayList<>(items);
        Money clearedAmount = calculateTotal();

        items.clear();
        updateTimestamp();

        // 發布購物車清空事件
        collectEvent(CartClearedEvent.create(this.id, this.consumerId, itemsToRemove, clearedAmount));
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
        ShoppingCartStatus oldStatus = this.status;
        this.status = newStatus;
        updateTimestamp();

        // 發布購物車狀態變更事件
        collectEvent(CartStatusChangedEvent.create(this.id, this.consumerId, oldStatus, newStatus));
    }

    /** 結帳購物車 */
    public void checkout() {
        if (isEmpty()) {
            throw new IllegalStateException("無法結帳空的購物車");
        }

        Money totalAmount = calculateTotal();
        int totalQuantity = getTotalQuantity();
        List<CartItem> itemsSnapshot = new ArrayList<>(items);

        // 更新狀態為已結帳
        updateStatus(ShoppingCartStatus.CHECKED_OUT);

        // 發布購物車結帳事件
        collectEvent(
                CartCheckedOutEvent.create(
                        this.id, this.consumerId, itemsSnapshot, totalAmount, totalQuantity));
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

    public Optional<CartItem> findItemOptional(ProductId productId) {
        return items.stream().filter(item -> item.productId().equals(productId)).findFirst();
    }

    private void replaceItem(CartItem oldItem, CartItem newItem) {
        int index = items.indexOf(oldItem);
        if (index >= 0) {
            items.set(index, newItem);
        }
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ShoppingCart that = (ShoppingCart) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
