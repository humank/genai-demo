package solid.humank.genaidemo.testutils.bdd;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 消費者購物測試輔助類 提供BDD測試所需的業務操作方法 */
@Component
public class ConsumerShoppingTestHelper {

    // 模擬的資料存儲
    private final Map<String, Map<String, Object>> products = new HashMap<>();
    private final Map<String, Map<String, Object>> customers = new HashMap<>();
    private final Map<String, Map<String, Object>> shoppingCarts = new HashMap<>();
    private final Map<String, Map<String, Object>> promotions = new HashMap<>();
    private final Map<String, Map<String, Object>> coupons = new HashMap<>();

    /** 創建商品 */
    public void createProduct(String productId, String name, BigDecimal price, int stock) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", productId);
        product.put("name", name);
        product.put("price", price);
        product.put("stock", stock);
        product.put("category", "ELECTRONICS");
        product.put("description", "測試商品描述");
        product.put("rating", 4.5);
        products.put(productId, product);
    }

    /** 創建客戶 */
    public void createCustomer(String customerId, String name) {
        Map<String, Object> customer = new HashMap<>();
        customer.put("id", customerId);
        customer.put("name", name);
        customer.put("email", name.toLowerCase() + "@example.com");
        customer.put("membershipLevel", "STANDARD");
        customer.put("rewardPoints", 0);
        customers.put(customerId, customer);

        // 同時創建空的購物車
        createEmptyCart(customerId);
    }

    /** 更新客戶會員等級 */
    public void updateCustomerMembershipLevel(String customerId, String membershipLevel) {
        Map<String, Object> customer = customers.get(customerId);
        if (customer != null) {
            customer.put("membershipLevel", membershipLevel);
        }
    }

    /** 更新客戶獎勵積點 */
    public void updateCustomerRewardPoints(String customerId, int rewardPoints) {
        Map<String, Object> customer = customers.get(customerId);
        if (customer != null) {
            customer.put("rewardPoints", rewardPoints);
        }
    }

    /** 創建VIP促銷活動 */
    public void createVipPromotion(String promotionId, String name, String description) {
        Map<String, Object> promotion = new HashMap<>();
        promotion.put("id", promotionId);
        promotion.put("name", name);
        promotion.put("description", description);
        promotion.put("type", "FIXED_AMOUNT_DISCOUNT");
        promotion.put("discountAmount", BigDecimal.valueOf(200));
        promotion.put("minimumAmount", BigDecimal.valueOf(10000));
        promotion.put("applicableMembershipLevels", List.of("VIP"));
        promotions.put(promotionId, promotion);
    }

    /** 創建優惠券 */
    public void createCoupon(String couponCode, int discountPercentage) {
        Map<String, Object> coupon = new HashMap<>();
        coupon.put("code", couponCode);
        coupon.put("discountPercentage", discountPercentage);
        coupon.put("minimumAmount", BigDecimal.valueOf(1000));
        coupon.put("status", "ACTIVE");
        coupons.put(couponCode, coupon);
    }

    /** 瀏覽商品 */
    public List<Map<String, Object>> browseProducts() {
        return new ArrayList<>(products.values());
    }

    /** 搜尋商品 */
    public List<Map<String, Object>> searchProducts(String keyword) {
        return products.values().stream()
                .filter(
                        product ->
                                product.get("name")
                                        .toString()
                                        .toLowerCase()
                                        .contains(keyword.toLowerCase()))
                .toList();
    }

    /** 獲取商品詳情 */
    public Map<String, Object> getProductDetail(String productId) {
        Map<String, Object> product = products.get(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        return new HashMap<>(product);
    }

    /** 添加商品到購物車 */
    public void addItemToCart(String customerId, String productId, int quantity) {
        Map<String, Object> product = products.get(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }

        int stock = (Integer) product.get("stock");
        if (stock < quantity) {
            throw new RuntimeException("庫存不足，可用庫存: " + stock);
        }

        Map<String, Object> cart = shoppingCarts.get(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        // 檢查是否已存在相同商品
        Map<String, Object> existingItem =
                items.stream()
                        .filter(item -> item.get("productId").equals(productId))
                        .findFirst()
                        .orElse(null);

        if (existingItem != null) {
            int currentQuantity = (Integer) existingItem.get("quantity");
            existingItem.put("quantity", currentQuantity + quantity);
            BigDecimal unitPrice = (BigDecimal) existingItem.get("unitPrice");
            existingItem.put(
                    "totalPrice",
                    unitPrice.multiply(BigDecimal.valueOf(currentQuantity + quantity)));
        } else {
            Map<String, Object> newItem = new HashMap<>();
            newItem.put("productId", productId);
            newItem.put("productName", product.get("name"));
            newItem.put("quantity", quantity);
            newItem.put("unitPrice", product.get("price"));
            newItem.put(
                    "totalPrice",
                    ((BigDecimal) product.get("price")).multiply(BigDecimal.valueOf(quantity)));
            items.add(newItem);
        }

        recalculateCartTotal(cart);
    }

    /** 獲取購物車 */
    public Map<String, Object> getShoppingCart(String customerId) {
        Map<String, Object> cart = shoppingCarts.get(customerId);
        if (cart == null) {
            createEmptyCart(customerId);
            cart = shoppingCarts.get(customerId);
        }
        return new HashMap<>(cart);
    }

    /** 計算結帳金額 */
    public Map<String, Object> calculateCheckout(String customerId) {
        Map<String, Object> cart = shoppingCarts.get(customerId);
        BigDecimal subtotal = (BigDecimal) cart.get("totalAmount");
        BigDecimal discount = (BigDecimal) cart.getOrDefault("discountAmount", BigDecimal.ZERO);
        BigDecimal total = subtotal.subtract(discount);

        Map<String, Object> checkoutSummary = new HashMap<>();
        checkoutSummary.put("subtotal", subtotal);
        checkoutSummary.put("discount", discount);
        checkoutSummary.put("total", total);
        checkoutSummary.put("items", cart.get("items"));

        return checkoutSummary;
    }

    /** 獲取可用促銷活動 */
    public List<Map<String, Object>> getAvailablePromotions(String customerId) {
        Map<String, Object> customer = customers.get(customerId);
        String membershipLevel = (String) customer.get("membershipLevel");

        return promotions.values().stream()
                .filter(
                        promotion -> {
                            @SuppressWarnings("unchecked")
                            List<String> applicableLevels =
                                    (List<String>) promotion.get("applicableMembershipLevels");
                            return applicableLevels == null
                                    || applicableLevels.contains(membershipLevel);
                        })
                .toList();
    }

    /** 應用促銷活動 */
    public Map<String, Object> applyPromotion(String customerId, String promotionId) {
        Map<String, Object> promotion = promotions.get(promotionId);
        if (promotion == null) {
            throw new RuntimeException("促銷活動不存在: " + promotionId);
        }

        Map<String, Object> cart = shoppingCarts.get(customerId);
        BigDecimal totalAmount = (BigDecimal) cart.get("totalAmount");
        BigDecimal minimumAmount = (BigDecimal) promotion.get("minimumAmount");

        if (totalAmount.compareTo(minimumAmount) < 0) {
            throw new RuntimeException("未達到最低消費金額");
        }

        BigDecimal discountAmount = (BigDecimal) promotion.get("discountAmount");
        cart.put("discountAmount", discountAmount);
        cart.put("finalAmount", totalAmount.subtract(discountAmount));

        Map<String, Object> result = new HashMap<>();
        result.put("message", "促銷活動已應用");
        result.put("discountAmount", discountAmount);
        return result;
    }

    /** 應用優惠券 */
    public Map<String, Object> applyCoupon(String customerId, String couponCode) {
        Map<String, Object> coupon = coupons.get(couponCode);
        if (coupon == null) {
            throw new RuntimeException("優惠券不存在: " + couponCode);
        }

        Map<String, Object> cart = shoppingCarts.get(customerId);
        BigDecimal totalAmount = (BigDecimal) cart.get("totalAmount");
        BigDecimal minimumAmount = (BigDecimal) coupon.get("minimumAmount");

        if (totalAmount.compareTo(minimumAmount) < 0) {
            throw new RuntimeException("未達到最低消費金額");
        }

        int discountPercentage = (Integer) coupon.get("discountPercentage");
        BigDecimal discountAmount =
                totalAmount
                        .multiply(BigDecimal.valueOf(discountPercentage))
                        .divide(BigDecimal.valueOf(100));

        cart.put("discountAmount", discountAmount);
        cart.put("finalAmount", totalAmount.subtract(discountAmount));

        Map<String, Object> result = new HashMap<>();
        result.put("message", "優惠券已應用");
        result.put("discountAmount", discountAmount);
        return result;
    }

    /** 更新購物車商品數量 */
    public void updateCartItemQuantity(String customerId, String productId, int newQuantity) {
        Map<String, Object> cart = shoppingCarts.get(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        Map<String, Object> item =
                items.stream()
                        .filter(i -> i.get("productId").equals(productId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("商品不存在於購物車中: " + productId));

        item.put("quantity", newQuantity);
        BigDecimal unitPrice = (BigDecimal) item.get("unitPrice");
        item.put("totalPrice", unitPrice.multiply(BigDecimal.valueOf(newQuantity)));

        recalculateCartTotal(cart);
    }

    /** 從購物車移除商品 */
    public void removeItemFromCart(String customerId, String productId) {
        Map<String, Object> cart = shoppingCarts.get(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        boolean removed = items.removeIf(item -> item.get("productId").equals(productId));
        if (!removed) {
            throw new RuntimeException("商品不存在於購物車中: " + productId);
        }

        recalculateCartTotal(cart);
    }

    /** 清空購物車 */
    public void clearCart(String customerId) {
        Map<String, Object> cart = shoppingCarts.get(customerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        items.clear();
        recalculateCartTotal(cart);
    }

    /** 更新商品庫存 */
    public void updateProductStock(String productId, int stock) {
        Map<String, Object> product = products.get(productId);
        if (product != null) {
            product.put("stock", stock);
        }
    }

    /** 創建空購物車 */
    private void createEmptyCart(String customerId) {
        Map<String, Object> cart = new HashMap<>();
        cart.put("customerId", customerId);
        cart.put("items", new ArrayList<Map<String, Object>>());
        cart.put("totalAmount", BigDecimal.ZERO);
        cart.put("discountAmount", BigDecimal.ZERO);
        cart.put("finalAmount", BigDecimal.ZERO);
        cart.put("status", "ACTIVE");
        shoppingCarts.put(customerId, cart);
    }

    /** 重新計算購物車總金額 */
    private void recalculateCartTotal(Map<String, Object> cart) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        BigDecimal totalAmount =
                items.stream()
                        .map(item -> (BigDecimal) item.get("totalPrice"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.put("totalAmount", totalAmount);

        BigDecimal discountAmount =
                (BigDecimal) cart.getOrDefault("discountAmount", BigDecimal.ZERO);
        cart.put("finalAmount", totalAmount.subtract(discountAmount));
    }
}
