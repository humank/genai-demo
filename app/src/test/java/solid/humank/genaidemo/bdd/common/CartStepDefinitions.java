package solid.humank.genaidemo.bdd.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/** 購物車操作步驟定義 - 處理通用的購物車操作 */
public class CartStepDefinitions {

    private TestContext testContext;
    private TestDataBuilder dataBuilder;
    private ShoppingCart shoppingCart;

    public CartStepDefinitions() {
        this.testContext = TestContext.getInstance();
        this.dataBuilder = TestDataBuilder.getInstance();
        // 初始化購物車
        this.shoppingCart =
                new ShoppingCart(
                        new ShoppingCartId(UUID.randomUUID().toString()),
                        new CustomerId(UUID.randomUUID().toString()));
    }

    @Given("empty shopping cart")
    public void empty_shopping_cart() {
        // 重新初始化購物車
        this.shoppingCart =
                new ShoppingCart(
                        new ShoppingCartId(UUID.randomUUID().toString()),
                        new CustomerId(UUID.randomUUID().toString()));
        testContext.getCartItems().clear();
        testContext.setCartTotal(BigDecimal.ZERO);
    }

    @When("customer adds {int} {string} to cart")
    public void customer_adds_quantity_to_cart(int quantity, String productId) {
        Product product = testContext.getProduct(productId);
        assertNotNull(product, String.format("Product %s should exist", productId));

        // 使用 ShoppingCart 聚合根添加商品
        shoppingCart.addItem(product.getId(), quantity, product.getPrice());

        // 同時更新測試上下文以保持兼容性
        CartItem existingItem = testContext.getCartItem(productId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem cartItem =
                    dataBuilder.createCartItem(
                            productId,
                            product.getName().getName(),
                            product.getPrice().getAmount(),
                            quantity);
            testContext.addCartItem(productId, cartItem);
        }
    }

    @When("customer removes {string} from cart")
    public void customer_removes_from_cart(String productId) {
        Product product = testContext.getProduct(productId);
        if (product != null) {
            shoppingCart.removeItem(product.getId());
        }
        testContext.removeCartItem(productId);
    }

    @When("customer updates {string} quantity to {int}")
    public void customer_updates_quantity_to(String productId, int newQuantity) {
        Product product = testContext.getProduct(productId);
        assertNotNull(product, String.format("Product %s should exist", productId));

        CartItem item = testContext.getCartItem(productId);
        assertNotNull(item, String.format("Product %s should be in cart", productId));

        if (newQuantity <= 0) {
            shoppingCart.removeItem(product.getId());
            testContext.removeCartItem(productId);
        } else {
            // 使用 ShoppingCart 聚合根更新數量
            shoppingCart.updateItemQuantity(product.getId(), newQuantity);
            item.setQuantity(newQuantity);
        }
    }

    @When("customer clears cart")
    public void customer_clears_cart() {
        shoppingCart.clear();
        testContext.getCartItems().clear();
        testContext.setCartTotal(BigDecimal.ZERO);
    }

    @Then("cart should be empty")
    public void cart_should_be_empty() {
        assertTrue(shoppingCart.isEmpty(), "ShoppingCart should be empty");
        assertTrue(testContext.getCartItems().isEmpty(), "Cart should be empty");
        assertEquals(BigDecimal.ZERO, testContext.getCartTotal(), "Cart total should be zero");
    }

    @Then("cart should contain {int} items")
    public void cart_should_contain_items(int expectedCount) {
        int actualCount = shoppingCart.getTotalQuantity();
        assertEquals(expectedCount, actualCount, String.format("Cart should contain %d items", expectedCount));

        // 也檢查測試上下文的一致性
        int testContextCount =
                testContext.getCartItems().values().stream().mapToInt(CartItem::getQuantity).sum();
        assertEquals(
                expectedCount,
                testContextCount,
                "Test context should also contain " + expectedCount + " items");
    }

    @Then("cart should contain {string} with quantity {int}")
    public void cart_should_contain_with_quantity(String productId, int expectedQuantity) {
        Product product = testContext.getProduct(productId);
        assertNotNull(product, "Product should exist");

        // 檢查 ShoppingCart 聚合根
        boolean foundInCart =
                shoppingCart.getItems().stream()
                        .anyMatch(
                                item ->
                                        item.productId().equals(product.getId())
                                                && item.quantity() == expectedQuantity);
        assertTrue(
                foundInCart,
                "ShoppingCart should contain " + productId + " with quantity " + expectedQuantity);

        // 檢查測試上下文
        CartItem item = testContext.getCartItem(productId);
        assertNotNull(item, "Cart should contain " + productId);
        assertEquals(
                expectedQuantity,
                item.getQuantity(),
                "Product " + productId + " should have quantity " + expectedQuantity);
    }

    @Then("cart should not contain {string}")
    public void cart_should_not_contain(String productId) {
        Product product = testContext.getProduct(productId);
        if (product != null) {
            boolean foundInCart = shoppingCart.containsProduct(product.getId());
            assertTrue(!foundInCart, "ShoppingCart should not contain " + productId);
        }

        CartItem item = testContext.getCartItem(productId);
        assertNull(item, "Cart should not contain " + productId);
    }

    @Then("cart total should be calculated correctly")
    public void cart_total_should_be_calculated_correctly() {
        // 使用 ShoppingCart 聚合根計算總額
        Money cartTotal = shoppingCart.calculateTotal();

        // 比較測試上下文的計算
        BigDecimal expectedTotal =
                testContext.getCartItems().values().stream()
                        .map(CartItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                expectedTotal, cartTotal.getAmount(), "Cart total should be calculated correctly");
    }

    @Then("cart should show subtotal of {int}")
    public void cart_should_show_subtotal_of(int expectedSubtotal) {
        BigDecimal actualSubtotal =
                testContext.getCartItems().values().stream()
                        .filter(item -> !item.isAddOn())
                        .map(CartItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                new BigDecimal(expectedSubtotal),
                actualSubtotal,
                "Cart subtotal should be " + expectedSubtotal);
    }

    @Then("cart should show add-on total of {int}")
    public void cart_should_show_add_on_total_of(int expectedAddOnTotal) {
        BigDecimal actualAddOnTotal =
                testContext.getCartItems().values().stream()
                        .filter(CartItem::isAddOn)
                        .map(CartItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                new BigDecimal(expectedAddOnTotal),
                actualAddOnTotal,
                "Cart add-on total should be " + expectedAddOnTotal);
    }

    @Then("cart should show total savings of {int}")
    public void cart_should_show_total_savings_of(int expectedSavings) {
        BigDecimal actualSavings =
                testContext.getCartItems().values().stream()
                        .map(CartItem::getSavings)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                new BigDecimal(expectedSavings),
                actualSavings,
                "Cart should show total savings of " + expectedSavings);
    }
}
