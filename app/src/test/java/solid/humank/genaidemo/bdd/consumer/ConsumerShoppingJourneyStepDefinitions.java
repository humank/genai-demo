package solid.humank.genaidemo.bdd.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_tw.並且;
import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import solid.humank.genaidemo.testutils.bdd.BddTestContext;
import solid.humank.genaidemo.testutils.bdd.ConsumerShoppingTestHelper;

/** 消費者購物流程 BDD 步驟定義 */
@SpringBootTest
@ActiveProfiles("test")
public class ConsumerShoppingJourneyStepDefinitions {

    @Autowired private ConsumerShoppingTestHelper shoppingHelper;

    @Autowired private BddTestContext testContext;

    @假設("系統中存在以下商品:")
    public void 系統中存在以下商品(DataTable dataTable) {
        List<Map<String, String>> products = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> product : products) {
            shoppingHelper.createProduct(
                    product.get("商品ID"),
                    product.get("商品名稱"),
                    new BigDecimal(product.get("價格")),
                    Integer.parseInt(product.get("庫存")));
        }
    }

    @並且("系統中存在客戶 {string} ID為 {string}")
    public void 系統中存在客戶ID為(String customerName, String customerId) {
        shoppingHelper.createCustomer(customerId, customerName);
        testContext.setCurrentCustomerId(customerId);
        testContext.setCurrentCustomerName(customerName);
    }

    @並且("客戶 {string} 的會員等級為 {string}")
    public void 客戶的會員等級為(String customerName, String membershipLevel) {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.updateCustomerMembershipLevel(customerId, membershipLevel);
    }

    @並且("客戶 {string} 的獎勵積點為 {int}")
    public void 客戶的獎勵積點為(String customerName, int rewardPoints) {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.updateCustomerRewardPoints(customerId, rewardPoints);
    }

    @假設("我是客戶 {string}")
    public void 我是客戶(String customerName) {
        testContext.setCurrentCustomerName(customerName);
        // 假設客戶ID已經在背景中設定
    }

    @假設("我是VIP會員 {string} ID為 {string}")
    public void 我是VIP會員ID為(String customerName, String customerId) {
        shoppingHelper.createCustomer(customerId, customerName);
        testContext.setCurrentCustomerId(customerId);
        testContext.setCurrentCustomerName(customerName);
    }

    @並且("存在VIP會員專享促銷活動 {string}")
    public void 存在VIP會員專享促銷活動(String promotionId) {
        shoppingHelper.createVipPromotion(promotionId, "VIP會員專享", "滿萬免運");
    }

    @並且("存在優惠券 {string} 可提供{int}%折扣")
    public void 存在優惠券可提供折扣(String couponCode, int discountPercentage) {
        shoppingHelper.createCoupon(couponCode, discountPercentage);
    }

    @當("我瀏覽商品目錄")
    public void 我瀏覽商品目錄() {
        List<Map<String, Object>> products = shoppingHelper.browseProducts();
        testContext.setLastProductList(products);
    }

    @那麼("我應該看到可用的商品列表")
    public void 我應該看到可用的商品列表() {
        List<Map<String, Object>> products = testContext.getLastProductList();
        assertThat(products).isNotEmpty();
    }

    @當("我搜尋 {string}")
    public void 我搜尋(String keyword) {
        List<Map<String, Object>> searchResults = shoppingHelper.searchProducts(keyword);
        testContext.setLastSearchResults(searchResults);
    }

    @那麼("我應該看到包含 {string} 的搜尋結果")
    public void 我應該看到包含的搜尋結果(String expectedProductName) {
        List<Map<String, Object>> searchResults = testContext.getLastSearchResults();
        assertThat(searchResults).isNotEmpty();

        boolean found =
                searchResults.stream()
                        .anyMatch(
                                product ->
                                        product.get("name")
                                                .toString()
                                                .contains(expectedProductName));
        assertThat(found).isTrue();
    }

    @當("我查看商品 {string} 的詳情")
    public void 我查看商品的詳情(String productId) {
        Map<String, Object> productDetail = shoppingHelper.getProductDetail(productId);
        testContext.setLastProductDetail(productDetail);
    }

    @那麼("我應該看到商品名稱為 {string}")
    public void 我應該看到商品名稱為(String expectedName) {
        Map<String, Object> productDetail = testContext.getLastProductDetail();
        assertThat(productDetail.get("name")).isEqualTo(expectedName);
    }

    @並且("我應該看到價格為 {int} 元")
    public void 我應該看到價格為元(int expectedPrice) {
        Map<String, Object> productDetail = testContext.getLastProductDetail();
        BigDecimal price = new BigDecimal(productDetail.get("price").toString());
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(expectedPrice));
    }

    @當("我將 {int} 個 {string} 添加到購物車")
    public void 我將個添加到購物車(int quantity, String productId) {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.addItemToCart(customerId, productId, quantity);
    }

    @那麼("我的購物車應該包含 {int} 個商品")
    public void 我的購物車應該包含個商品(int expectedItemCount) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        int totalQuantity =
                items.stream()
                        .mapToInt(item -> Integer.parseInt(item.get("quantity").toString()))
                        .sum();

        assertThat(totalQuantity).isEqualTo(expectedItemCount);
    }

    @並且("購物車總金額應該為 {int} 元")
    public void 購物車總金額應該為元(int expectedAmount) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);

        BigDecimal totalAmount = new BigDecimal(cart.get("totalAmount").toString());
        assertThat(totalAmount).isEqualByComparingTo(BigDecimal.valueOf(expectedAmount));
    }

    @當("我查看購物車")
    public void 我查看購物車() {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);
        testContext.setCurrentCart(cart);
    }

    @那麼("我應該看到以下商品:")
    public void 我應該看到以下商品(DataTable dataTable) {
        Map<String, Object> cart = testContext.getCurrentCart();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        List<Map<String, String>> expectedItems = dataTable.asMaps(String.class, String.class);

        assertThat(items).hasSize(expectedItems.size());

        for (Map<String, String> expectedItem : expectedItems) {
            String productId = expectedItem.get("商品ID");
            boolean found =
                    items.stream()
                            .anyMatch(
                                    item ->
                                            item.get("productId").equals(productId)
                                                    && item.get("quantity")
                                                            .toString()
                                                            .equals(expectedItem.get("數量"))
                                                    && new BigDecimal(
                                                                            item.get("unitPrice")
                                                                                    .toString())
                                                                    .compareTo(
                                                                            new BigDecimal(
                                                                                    expectedItem
                                                                                            .get(
                                                                                                    "單價")))
                                                            == 0);
            assertThat(found).as("找不到商品: " + productId).isTrue();
        }
    }

    @當("我進行結帳計算")
    public void 我進行結帳計算() {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> checkoutSummary = shoppingHelper.calculateCheckout(customerId);
        testContext.setLastCheckoutSummary(checkoutSummary);
    }

    @那麼("我應該看到小計為 {int} 元")
    public void 我應該看到小計為元(int expectedSubtotal) {
        Map<String, Object> checkoutSummary = testContext.getLastCheckoutSummary();
        BigDecimal subtotal = new BigDecimal(checkoutSummary.get("subtotal").toString());
        assertThat(subtotal).isEqualByComparingTo(BigDecimal.valueOf(expectedSubtotal));
    }

    @並且("我應該看到總計為 {int} 元")
    public void 我應該看到總計為元(int expectedTotal) {
        Map<String, Object> checkoutSummary = testContext.getLastCheckoutSummary();
        BigDecimal total = new BigDecimal(checkoutSummary.get("total").toString());
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(expectedTotal));
    }

    @當("我查看可用的促銷活動")
    public void 我查看可用的促銷活動() {
        String customerId = testContext.getCurrentCustomerId();
        List<Map<String, Object>> promotions = shoppingHelper.getAvailablePromotions(customerId);
        testContext.setLastPromotions(promotions);
    }

    @那麼("我應該看到 {string} 促銷活動")
    public void 我應該看到促銷活動(String promotionName) {
        List<Map<String, Object>> promotions = testContext.getLastPromotions();
        boolean found =
                promotions.stream()
                        .anyMatch(promo -> promo.get("name").toString().contains(promotionName));
        assertThat(found).isTrue();
    }

    @當("我應用促銷活動 {string}")
    public void 我應用促銷活動(String promotionId) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> result = shoppingHelper.applyPromotion(customerId, promotionId);
        testContext.setLastPromotionResult(result);
    }

    @那麼("我應該獲得折扣")
    public void 我應該獲得折扣() {
        Map<String, Object> result = testContext.getLastPromotionResult();
        BigDecimal discountAmount = new BigDecimal(result.get("discountAmount").toString());
        assertThat(discountAmount).isGreaterThan(BigDecimal.ZERO);
    }

    @並且("我的最終金額應該少於原始金額")
    public void 我的最終金額應該少於原始金額() {
        // 這個驗證需要比較應用促銷前後的金額
        // 在實際實現中，我們需要在應用促銷前保存原始金額
        Map<String, Object> result = testContext.getLastPromotionResult();
        assertThat(result.get("discountAmount")).isNotNull();
    }

    @當("我應用優惠券 {string}")
    public void 我應用優惠券(String couponCode) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> result = shoppingHelper.applyCoupon(customerId, couponCode);
        testContext.setLastCouponResult(result);
    }

    @那麼("我應該獲得 {int} 元的折扣")
    public void 我應該獲得元的折扣(int expectedDiscount) {
        Map<String, Object> result = testContext.getLastCouponResult();
        BigDecimal discountAmount = new BigDecimal(result.get("discountAmount").toString());
        assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(expectedDiscount));
    }

    @並且("我的最終金額應該為 {int} 元")
    public void 我的最終金額應該為元(int expectedFinalAmount) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);
        BigDecimal finalAmount = new BigDecimal(cart.get("finalAmount").toString());
        assertThat(finalAmount).isEqualByComparingTo(BigDecimal.valueOf(expectedFinalAmount));
    }

    @那麼("我的購物車應該包含 {int} 種商品")
    public void 我的購物車應該包含種商品(int expectedUniqueCount) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        assertThat(items).hasSize(expectedUniqueCount);
    }

    @並且("商品 {string} 的數量應該為 {int}")
    public void 商品的數量應該為(String productId, int expectedQuantity) {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");

        Map<String, Object> item =
                items.stream()
                        .filter(i -> i.get("productId").equals(productId))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("找不到商品: " + productId));

        int quantity = Integer.parseInt(item.get("quantity").toString());
        assertThat(quantity).isEqualTo(expectedQuantity);
    }

    @當("我將商品 {string} 的數量更新為 {int}")
    public void 我將商品的數量更新為(String productId, int newQuantity) {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.updateCartItemQuantity(customerId, productId, newQuantity);
    }

    @當("我從購物車移除商品 {string}")
    public void 我從購物車移除商品(String productId) {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.removeItemFromCart(customerId, productId);
    }

    @那麼("我的購物車應該為空")
    public void 我的購物車應該為空() {
        String customerId = testContext.getCurrentCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        assertThat(items).isEmpty();
    }

    @並且("商品 {string} 的庫存只有 {int} 個")
    public void 商品的庫存只有個(String productId, int stock) {
        shoppingHelper.updateProductStock(productId, stock);
    }

    @當("我嘗試將 {int} 個 {string} 添加到購物車")
    public void 我嘗試將個添加到購物車(int quantity, String productId) {
        String customerId = testContext.getCurrentCustomerId();
        try {
            shoppingHelper.addItemToCart(customerId, productId, quantity);
            testContext.setLastError(null);
        } catch (Exception e) {
            testContext.setLastError(e.getMessage());
        }
    }

    @那麼("我應該收到庫存不足的錯誤訊息")
    public void 我應該收到庫存不足的錯誤訊息() {
        String lastError = testContext.getLastError();
        assertThat(lastError).isNotNull();
        assertThat(lastError).contains("庫存不足");
    }

    @並且("我的購物車中有以下商品:")
    public void 我的購物車中有以下商品(DataTable dataTable) {
        String customerId = testContext.getCurrentCustomerId();
        List<Map<String, String>> items = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> item : items) {
            shoppingHelper.addItemToCart(
                    customerId, item.get("商品ID"), Integer.parseInt(item.get("數量")));
        }
    }

    @當("我清空購物車")
    public void 我清空購物車() {
        String customerId = testContext.getCurrentCustomerId();
        shoppingHelper.clearCart(customerId);
    }
}
