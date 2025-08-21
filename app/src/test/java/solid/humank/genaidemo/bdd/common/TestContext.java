package solid.humank.genaidemo.bdd.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule;

/** 測試上下文 - 管理 BDD 測試期間的共享狀態 使用單例模式，不依賴 Spring */
public class TestContext {

    private static TestContext instance;

    // 產品數據
    private Map<String, Product> products = new HashMap<>();

    // 購物車狀態
    private Map<String, CartItem> cartItems = new HashMap<>();
    private BigDecimal cartTotal = BigDecimal.ZERO;

    // 加購規則
    private List<AddOnPurchaseRule> addOnRules = new ArrayList<>();

    // 客戶信息
    private String customerId;
    private String customerName;

    // 錯誤狀態
    private String lastErrorMessage;
    private boolean hasError = false;

    // 顯示的加購選項
    private List<AddOnOption> displayedAddOnOptions = new ArrayList<>();

    // 會員折扣
    private BigDecimal memberDiscount = BigDecimal.ZERO;
    private boolean isVipMember = false;

    // 時間管理
    private java.time.LocalDateTime currentTime;

    private TestContext() {
        // 私有構造函數
    }

    public static TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }

    /** 清理測試數據 - 每個場景結束後調用 */
    public void clear() {
        products.clear();
        cartItems.clear();
        addOnRules.clear();
        displayedAddOnOptions.clear();
        genericData.clear();
        cartTotal = BigDecimal.ZERO;
        customerId = null;
        customerName = null;
        lastErrorMessage = null;
        hasError = false;
        memberDiscount = BigDecimal.ZERO;
        isVipMember = false;
        currentTime = null;
        lastOperationResult = null;
    }

    // Getters and Setters
    public Map<String, Product> getProducts() {
        return products;
    }

    public void addProduct(String productId, Product product) {
        this.products.put(productId, product);
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    public Map<String, CartItem> getCartItems() {
        return cartItems;
    }

    public void addCartItem(String productId, CartItem item) {
        this.cartItems.put(productId, item);
        recalculateCartTotal();
    }

    public CartItem getCartItem(String productId) {
        return cartItems.get(productId);
    }

    public void removeCartItem(String productId) {
        cartItems.remove(productId);
        recalculateCartTotal();
    }

    public BigDecimal getCartTotal() {
        return cartTotal;
    }

    public void setCartTotal(BigDecimal cartTotal) {
        this.cartTotal = cartTotal;
    }

    private void recalculateCartTotal() {
        cartTotal =
                cartItems.values().stream()
                        .map(
                                item ->
                                        item.getPrice()
                                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<AddOnPurchaseRule> getAddOnRules() {
        return addOnRules;
    }

    public void addAddOnRule(AddOnPurchaseRule rule) {
        this.addOnRules.add(rule);
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
        this.hasError = lastErrorMessage != null;
    }

    public boolean hasError() {
        return hasError;
    }

    public List<AddOnOption> getDisplayedAddOnOptions() {
        return displayedAddOnOptions;
    }

    public void setDisplayedAddOnOptions(List<AddOnOption> displayedAddOnOptions) {
        this.displayedAddOnOptions = displayedAddOnOptions;
    }

    public BigDecimal getMemberDiscount() {
        return memberDiscount;
    }

    public void setMemberDiscount(BigDecimal memberDiscount) {
        this.memberDiscount = memberDiscount;
    }

    public boolean isVipMember() {
        return isVipMember;
    }

    public void setVipMember(boolean vipMember) {
        isVipMember = vipMember;
    }

    public java.time.LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(java.time.LocalDateTime currentTime) {
        this.currentTime = currentTime;
    }

    // 操作結果存儲
    private String lastOperationResult;

    public String getLastOperationResult() {
        return lastOperationResult;
    }

    public void setLastOperationResult(String lastOperationResult) {
        this.lastOperationResult = lastOperationResult;
    }

    // Gift with purchase related fields and methods
    private List<String> qualifiedGiftActivities = new ArrayList<>();

    public List<String> getQualifiedGiftActivities() {
        return qualifiedGiftActivities;
    }

    public void setQualifiedGiftActivities(List<String> qualifiedGiftActivities) {
        this.qualifiedGiftActivities = qualifiedGiftActivities;
    }

    // Generic data storage for compatibility
    private Map<String, Object> genericData = new HashMap<>();

    public void put(String key, Object value) {
        genericData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = genericData.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new IllegalArgumentException(
                String.format(
                        "Value for key '%s' is not of expected type %s, but was %s",
                        key, type.getSimpleName(), value.getClass().getSimpleName()));
    }

    public boolean contains(String key) {
        return genericData.containsKey(key);
    }
}
