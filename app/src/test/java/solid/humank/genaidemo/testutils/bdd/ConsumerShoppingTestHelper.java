package solid.humank.genaidemo.testutils.bdd;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import solid.humank.genaidemo.bdd.common.TestContext;

/** 消費者購物測試輔助類別 */
public class ConsumerShoppingTestHelper {

    private static ConsumerShoppingTestHelper instance;
    private final TestContext testContext;
    private final Map<String, Map<String, Object>> shoppingCarts = new HashMap<>();
    private final Map<String, Map<String, Object>> customers = new HashMap<>();
    private final Map<String, Map<String, Object>> promotions = new HashMap<>();
    private final Map<String, Map<String, Object>> coupons = new HashMap<>();

    private ConsumerShoppingTestHelper() {
        this.testContext = TestContext.getInstance();
    }

    public static ConsumerShoppingTestHelper getInstance() {
        if (instance == null) {
            instance = new ConsumerShoppingTestHelper();
        }
        return instance;
    }

    /** 獲取購物車 */
    public Map<String, Object> getShoppingCart(String customerId) {
        return shoppingCarts.computeIfAbsent(
                customerId,
                k -> {
                    Map<String, Object> cart = new HashMap<>();
                    cart.put("items", new ArrayList<Map<String, Object>>());
                    cart.put("total", BigDecimal.ZERO);
                    return cart;
                });
    }

    /** 添加商品到購物車 */
    public void addItemToCart(String customerId, String productId, int quantity) {
        try {
            Map<String, Object> cart = getShoppingCart(customerId);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

            // 檢查數量限制
            if (quantity > 99) {
                testContext.setLastErrorMessage(
                        "quantity limit exceeded: maximum 99 units allowed");
                return;
            }

            BigDecimal productPrice = getProductPrice(productId);

            // 查找是否已存在該商品
            Map<String, Object> existingItem = null;
            for (Map<String, Object> item : items) {
                if (productId.equals(item.get("productId"))) {
                    existingItem = item;
                    break;
                }
            }

            if (existingItem != null) {
                int currentQuantity = (Integer) existingItem.get("quantity");
                int newQuantity = currentQuantity + quantity;

                if (newQuantity > 99) {
                    testContext.setLastErrorMessage(
                            "quantity limit exceeded: maximum 99 units allowed");
                    return;
                }

                existingItem.put("quantity", newQuantity);
                existingItem.put("unitPrice", productPrice);
                existingItem.put("totalPrice", productPrice.multiply(new BigDecimal(newQuantity)));
            } else {
                Map<String, Object> newItem = new HashMap<>();
                newItem.put("productId", productId);
                newItem.put("quantity", quantity);
                newItem.put("price", productPrice);
                newItem.put("unitPrice", productPrice);
                newItem.put("totalPrice", productPrice.multiply(new BigDecimal(quantity)));
                items.add(newItem);
            }

            // 重新計算購物車總額
            recalculateCartTotal(cart);

            // 清除錯誤訊息
            testContext.setLastErrorMessage(null);

        } catch (Exception e) {
            testContext.setLastErrorMessage("Error adding item to cart: " + e.getMessage());
        }
    }

    /** 獲取商品價格 */
    private BigDecimal getProductPrice(String productId) {
        // 模擬商品價格
        switch (productId) {
            case "PROD-001":
                return new BigDecimal("35900");
            case "PROD-002":
                return new BigDecimal("28900");
            case "PROD-003":
                return new BigDecimal("58000");
            case "PROD-004":
                return new BigDecimal("590");
            case "PROD-005":
                return new BigDecimal("8990");
            default:
                return new BigDecimal("1000");
        }
    }

    /** 重新計算購物車總額 */
    private void recalculateCartTotal(Map<String, Object> cart) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal itemPrice = (BigDecimal) item.get("price");
            int quantity = (Integer) item.get("quantity");
            BigDecimal itemTotal = itemPrice.multiply(new BigDecimal(quantity));
            total = total.add(itemTotal);

            // 更新商品的總價
            item.put("totalPrice", itemTotal);
        }

        cart.put("total", total);
        cart.put("totalAmount", total);
    }

    /** 更新客戶會員等級 */
    public void updateCustomerMembershipLevel(String customerId, String membershipLevel) {
        Map<String, Object> customer = customers.computeIfAbsent(customerId, k -> new HashMap<>());
        customer.put("membershipLevel", membershipLevel);
    }

    /** 更新客戶獎勵積分 */
    public void updateCustomerRewardPoints(String customerId, int rewardPoints) {
        Map<String, Object> customer = customers.computeIfAbsent(customerId, k -> new HashMap<>());
        customer.put("rewardPoints", rewardPoints);
    }

    /** 創建 VIP 促銷活動 */
    public void createVipPromotion(String promotionId, String name, String description) {
        Map<String, Object> promotion = new HashMap<>();
        promotion.put("id", promotionId);
        promotion.put("name", name);
        promotion.put("description", description);
        promotion.put("type", "VIP_EXCLUSIVE");
        promotions.put(promotionId, promotion);
    }

    /** 創建優惠券 */
    public void createCoupon(String couponCode, int discountPercentage) {
        Map<String, Object> coupon = new HashMap<>();
        coupon.put("code", couponCode);
        coupon.put("discountPercentage", discountPercentage);
        coupon.put("type", "PERCENTAGE");
        coupons.put(couponCode, coupon);
    }

    /** 合併匿名購物車到用戶購物車 */
    public void mergeAnonymousCartToUserCart(String anonymousUserId, String userId) {
        Map<String, Object> anonymousCart = shoppingCarts.get(anonymousUserId);
        if (anonymousCart == null) {
            return;
        }

        Map<String, Object> userCart = getShoppingCart(userId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anonymousItems =
                (List<Map<String, Object>>) anonymousCart.get("items");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userItems = (List<Map<String, Object>>) userCart.get("items");

        // 合併商品
        for (Map<String, Object> anonymousItem : anonymousItems) {
            String productId = (String) anonymousItem.get("productId");
            int quantity = (Integer) anonymousItem.get("quantity");

            // 查找用戶購物車中是否已有該商品
            boolean found = false;
            for (Map<String, Object> userItem : userItems) {
                if (productId.equals(userItem.get("productId"))) {
                    int currentQuantity = (Integer) userItem.get("quantity");
                    userItem.put("quantity", currentQuantity + quantity);
                    found = true;
                    break;
                }
            }

            if (!found) {
                userItems.add(new HashMap<>(anonymousItem));
            }
        }

        // 清除匿名購物車
        shoppingCarts.remove(anonymousUserId);
    }

    /** 創建產品 */
    public void createProduct(String productId, String productName, BigDecimal price, int stock) {
        // 在實際實現中，這裡會調用產品服務來創建產品
        // 目前只是模擬存儲
        testContext.put(
                "product_" + productId,
                Map.of(
                        "id", productId,
                        "name", productName,
                        "price", price,
                        "stock", stock));
    }

    /** 更新產品庫存 */
    public void updateProductStock(String productId, int stock) {
        testContext.put("stock_" + productId, stock);
    }

    /** 創建客戶 */
    public void createCustomer(String customerId, String customerName) {
        Map<String, Object> customer = customers.computeIfAbsent(customerId, k -> new HashMap<>());
        customer.put("id", customerId);
        customer.put("name", customerName);
    }

    /** 瀏覽產品 */
    public List<Map<String, Object>> browseProducts() {
        List<Map<String, Object>> products = new ArrayList<>();
        // 返回模擬的產品列表
        products.add(
                Map.of(
                        "id",
                        "PROD-001",
                        "name",
                        "iPhone 15 Pro Max",
                        "price",
                        new BigDecimal("35900")));
        products.add(
                Map.of("id", "PROD-002", "name", "AirPods Pro", "price", new BigDecimal("8990")));
        return products;
    }

    /** 計算結帳 */
    public Map<String, Object> calculateCheckout(String customerId) {
        Map<String, Object> cart = getShoppingCart(customerId);
        Map<String, Object> checkout = new HashMap<>();
        checkout.put("subtotal", cart.get("total"));
        checkout.put("tax", BigDecimal.ZERO);
        checkout.put("total", cart.get("total"));
        return checkout;
    }

    /** 獲取可用促銷活動 */
    public List<Map<String, Object>> getAvailablePromotions(String customerId) {
        return new ArrayList<>(promotions.values());
    }

    /** 清空購物車 */
    public void clearCart(String customerId) {
        Map<String, Object> cart = getShoppingCart(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        items.clear();
        cart.put("total", BigDecimal.ZERO);
        cart.put("totalAmount", BigDecimal.ZERO);
    }

    /** 移除購物車中的商品 */
    public void removeItemFromCart(String customerId, String productId) {
        Map<String, Object> cart = getShoppingCart(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        items.removeIf(item -> productId.equals(item.get("productId")));

        // 重新計算購物車總額
        recalculateCartTotal(cart);
    }

    /** 更新購物車商品數量 */
    public void updateCartItemQuantity(String customerId, String productId, int newQuantity) {
        try {
            // 檢查庫存
            Integer stock = testContext.get("stock_" + productId, Integer.class);
            if (stock != null && newQuantity > stock) {
                testContext.setLastErrorMessage(
                        "insufficient stock: only " + stock + " units available");
                return;
            }

            Map<String, Object> cart = getShoppingCart(customerId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

            for (Map<String, Object> item : items) {
                if (productId.equals(item.get("productId"))) {
                    if (newQuantity <= 0) {
                        items.remove(item);
                    } else {
                        item.put("quantity", newQuantity);
                        BigDecimal unitPrice = (BigDecimal) item.get("price");
                        item.put("totalPrice", unitPrice.multiply(new BigDecimal(newQuantity)));
                    }

                    // 重新計算購物車總額
                    recalculateCartTotal(cart);
                    testContext.setLastErrorMessage(null);
                    return;
                }
            }
        } catch (Exception e) {
            testContext.setLastErrorMessage("Error updating cart item: " + e.getMessage());
        }
    }

    /** 搜索產品 */
    public List<Map<String, Object>> searchProducts(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
        // 模擬搜索結果
        if (keyword.toLowerCase().contains("phone")) {
            results.add(
                    Map.of(
                            "id",
                            "PROD-001",
                            "name",
                            "iPhone 15 Pro Max",
                            "price",
                            new BigDecimal("35900")));
        }
        if (keyword.toLowerCase().contains("airpods")) {
            results.add(
                    Map.of(
                            "id",
                            "PROD-002",
                            "name",
                            "AirPods Pro",
                            "price",
                            new BigDecimal("8990")));
        }
        return results;
    }

    /** 獲取產品詳情 */
    public Map<String, Object> getProductDetail(String productId) {
        Map<String, Object> product = new HashMap<>();
        switch (productId) {
            case "PROD-001":
                product.put("id", productId);
                product.put("name", "iPhone 15 Pro Max");
                product.put("price", new BigDecimal("35900"));
                product.put("description", "Latest iPhone with Pro Max features");
                break;
            case "PROD-002":
                product.put("id", productId);
                product.put("name", "AirPods Pro");
                product.put("price", new BigDecimal("8990"));
                product.put("description", "Wireless earphones with noise cancellation");
                break;
            default:
                product.put("id", productId);
                product.put("name", "Unknown Product");
                product.put("price", new BigDecimal("1000"));
                product.put("description", "Product description not available");
        }
        return product;
    }

    /** 應用促銷活動 */
    public Map<String, Object> applyPromotion(String customerId, String promotionId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> promotion = promotions.get(promotionId);

        if (promotion != null) {
            result.put("promotionApplied", true);
            result.put("promotionId", promotionId);
            result.put("discountAmount", new BigDecimal("500")); // 模擬折扣金額
        } else {
            result.put("promotionApplied", false);
            result.put("error", "Promotion not found");
        }

        return result;
    }

    /** 應用優惠券 */
    public Map<String, Object> applyCoupon(String customerId, String couponCode) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> coupon = coupons.get(couponCode);

        if (coupon != null) {
            int discountPercentage = (Integer) coupon.get("discountPercentage");
            Map<String, Object> cart = getShoppingCart(customerId);
            BigDecimal cartTotal = (BigDecimal) cart.get("total");
            BigDecimal discountAmount =
                    cartTotal
                            .multiply(new BigDecimal(discountPercentage))
                            .divide(new BigDecimal(100));

            result.put("couponApplied", true);
            result.put("couponCode", couponCode);
            result.put("discountAmount", discountAmount);
            result.put("finalAmount", cartTotal.subtract(discountAmount));
        } else {
            result.put("couponApplied", false);
            result.put("error", "Coupon not found");
        }

        return result;
    }

    /** 清理測試數據 */
    public void clear() {
        shoppingCarts.clear();
        customers.clear();
        promotions.clear();
        coupons.clear();
    }
}
