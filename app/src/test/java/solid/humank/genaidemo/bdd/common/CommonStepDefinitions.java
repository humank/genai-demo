package solid.humank.genaidemo.bdd.common;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import solid.humank.genaidemo.testutils.bdd.ConsumerShoppingTestHelper;

/**
 * Common Step Definitions - Shared across multiple test scenarios This class consolidates commonly
 * used step definitions to avoid duplication
 */
public class CommonStepDefinitions {

    private final TestContext testContext;
    private final ConsumerShoppingTestHelper shoppingHelper;

    public CommonStepDefinitions() {
        this.testContext = TestContext.getInstance();
        this.shoppingHelper = ConsumerShoppingTestHelper.getInstance();
    }

    // ========== Product Management Steps ==========

    @Given("the following products exist in the system:")
    public void theFollowingProductsExistInTheSystem(DataTable dataTable) {
        List<Map<String, String>> products = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> product : products) {
            shoppingHelper.createProduct(
                    product.get("Product ID"),
                    product.get("Product Name"),
                    new java.math.BigDecimal(product.get("Price")),
                    Integer.parseInt(product.get("Stock")));
        }
    }

    @Given("the following products exist for promotion testing:")
    public void theFollowingProductsExistForPromotionTesting(DataTable dataTable) {
        theFollowingProductsExistInTheSystem(dataTable);
    }

    @Given("the following products exist in the search system:")
    public void theFollowingProductsExistInTheSearchSystem(DataTable dataTable) {
        testContext.put("searchProducts", dataTable.asMaps());
    }

    // ========== Customer Cart Steps ==========

    @Given("customer adds {string} to cart")
    public void customerAddsToCart(String productId) {
        testContext.put("customerAddedToCart", productId);
    }

    @Given("customer cart contains {string}")
    public void customerCartContains(String productId) {
        testContext.put("cartContains", productId);
    }

    @Given("customer cart contains {int} {string}")
    public void customerCartContains(Integer quantity, String productId) {
        testContext.put(
                "cartContains", java.util.Map.of("product", productId, "quantity", quantity));
    }

    @Given("customer cart contains:")
    public void customerCartContains(DataTable dataTable) {
        testContext.put("cartItems", dataTable.asMaps());
    }

    // ========== System Display Steps ==========

    @Then("system should display {string}")
    public void systemShouldDisplay(String message) {
        testContext.put("systemMessage", message);
    }

    @Then("should display {string}")
    public void shouldDisplay(String message) {
        testContext.put("displayMessage", message);
    }

    @Then("system should prompt {string}")
    public void systemShouldPrompt(String message) {
        testContext.put("systemPrompt", message);
    }

    // ========== Customer Actions Steps ==========

    @When("customer views cart")
    public void customerViewsCart() {
        String customerId = testContext.getCustomerId();
        Map<String, Object> cart = shoppingHelper.getShoppingCart(customerId);
        testContext.put("currentCart", cart);
    }

    @When("customer selects {string}")
    public void customerSelects(String item) {
        testContext.put("selectedItem", item);
    }

    @When("customer views {string}")
    public void customerViews(String item) {
        testContext.put("viewedItem", item);
    }

    // ========== Stock Management Steps ==========

    @Given("{string} has only {int} unit in stock")
    public void hasOnlyUnitInStock(String productId, Integer stock) {
        testContext.put("productStock", java.util.Map.of("product", productId, "stock", stock));
        shoppingHelper.updateProductStock(productId, stock);
    }

    @Given("{string} has only {int} units in stock")
    public void hasOnlyUnitsInStock(String productId, Integer stock) {
        hasOnlyUnitInStock(productId, stock);
    }

    @Given("product {string} has only {int} stock")
    public void productHasOnlyStock(String productId, int stock) {
        shoppingHelper.updateProductStock(productId, stock);
    }

    // ========== Price and Amount Steps ==========

    @Then("total amount should be {int}")
    public void totalAmountShouldBe(int expectedAmount) {
        testContext.put("expectedTotal", expectedAmount);
    }

    @Then("final amount should be {int}")
    public void finalAmountShouldBe(int expectedAmount) {
        testContext.put("expectedFinalAmount", expectedAmount);
    }

    @Then("final payment amount should be {int}")
    public void finalPaymentAmountShouldBe(int finalAmount) {
        testContext.put("expectedFinalAmount", finalAmount);
    }

    @Then("discount amount should be {int}")
    public void discountAmountShouldBe(int discountAmount) {
        testContext.put("expectedDiscountAmount", discountAmount);
    }

    // ========== Customer Management Steps ==========

    @Given("customer {string} exists with ID {string}")
    public void customerExistsWithId(String customerName, String customerId) {
        shoppingHelper.createCustomer(customerId, customerName);
        testContext.setCustomerId(customerId);
        testContext.setCustomerName(customerName);
    }

    @Given("I am customer {string}")
    public void iAmCustomer(String customerName) {
        testContext.setCustomerName(customerName);
        // 如果客戶已經存在於 testContext 中，使用現有的 ID
        String existingCustomerId = testContext.getCustomerId();
        if (existingCustomerId == null) {
            // 如果沒有現有的客戶 ID，根據客戶名稱生成一個
            String customerId = customerName.toLowerCase().replace(" ", "-");
            testContext.setCustomerId(customerId);
        }
    }

    @Given("I am logged in as customer {string}")
    public void iAmLoggedInAsCustomer(String customerName) {
        String customerId = customerName.toLowerCase().replace(" ", "-");
        testContext.setCustomerId(customerId);
        testContext.setCustomerName(customerName);
        shoppingHelper.createCustomer(customerId, customerName);
    }

    // ========== Generic Validation Steps ==========

    @Then("should be successful")
    public void shouldBeSuccessful() {
        testContext.put("operationSuccessful", true);
    }

    @Then("should fail")
    public void shouldFail() {
        testContext.put("operationFailed", true);
    }

    @Then("should show error message {string}")
    public void shouldShowErrorMessage(String errorMessage) {
        testContext.put("errorMessage", errorMessage);
    }
}
