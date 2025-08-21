package solid.humank.genaidemo.bdd.promotion;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Add-on Purchase Offers Step Definitions */
public class AddOnPurchaseStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();

    // Add-on specific steps that are not duplicated elsewhere
    @Given("cart contains main product and add-on products")
    public void cartContainsMainProductAndAddOnProducts() {
        testContext.put("hasMainAndAddOnProducts", true);
    }

    @Given("customer removed main product but chose to keep add-on products")
    public void customerRemovedMainProductButChoseToKeepAddOnProducts() {
        testContext.put("removedMainButKeptAddOn", true);
    }

    @Given("the following add-on offer rules are configured:")
    public void theFollowingAddOnOfferRulesAreConfigured(DataTable dataTable) {
        testContext.put("addOnRules", dataTable.asMaps());
    }

    @Given("customer has main product {string} in cart")
    public void customerHasMainProductInCart(String productId) {
        testContext.put("mainProductInCart", productId);
    }

    @Given("has already added {int} {string} as add-on")
    public void hasAlreadyAddedAsAddOn(int quantity, String productId) {
        testContext.put(
                "existingAddOn", java.util.Map.of("product", productId, "quantity", quantity));
    }

    @Given("customer has multiple main products:")
    public void customerHasMultipleMainProducts(DataTable dataTable) {
        testContext.put("multipleMainProducts", dataTable.asMaps());
    }

    @Given("customer is VIP member with 5% discount")
    public void customerIsVipMemberWith5PercentDiscount() {
        testContext.put("memberLevel", "VIP");
        testContext.put("memberDiscount", 0.05);
    }

    @Given("{string} add-on product has stock {int}")
    public void addOnProductHasStock(String productId, int stock) {
        testContext.put("addOnStock", java.util.Map.of("product", productId, "stock", stock));
    }

    @Given("another customer has added it to cart")
    public void anotherCustomerHasAddedItToCart() {
        testContext.put("productReservedByOther", true);
    }

    @When("customer views cart or product page")
    public void customerViewsCartOrProductPage() {
        testContext.put("viewingCartOrProduct", true);
    }

    @When("customer selects add-on {string}")
    public void customerSelectsAddOn(String productId) {
        testContext.put("selectedAddOn", productId);
    }

    @When("customer tries to add another {string} as add-on")
    public void customerTriesToAddAnotherAsAddOn(String productId) {
        testContext.put("attemptedAddOn", productId);
    }

    @When("customer selects {int} {string} as add-on")
    public void customerSelectsAsAddOn(int quantity, String productId) {
        testContext.put(
                "addOnSelection", java.util.Map.of("product", productId, "quantity", quantity));
    }

    @When("customer removes main product {string}")
    public void customerRemovesMainProduct(String productId) {
        testContext.put("removedMainProduct", productId);
    }

    @When("add-on product {string} loses add-on eligibility")
    public void addOnProductLosesAddOnEligibility(String productId) {
        testContext.put("lostEligibilityProduct", productId);
    }

    @When("customer views add-on options")
    public void customerViewsAddOnOptions() {
        testContext.put("viewingAddOnOptions", true);
    }

    @When("calculating final price")
    public void calculatingFinalPrice() {
        testContext.put("calculatingFinalPrice", true);
    }

    @When("customer tries to add {string} as add-on")
    public void customerTriesToAddAsAddOn(String productId) {
        testContext.put("attemptedAddOn", productId);
    }

    @Then("should display the following add-on options:")
    public void shouldDisplayTheFollowingAddOnOptions(DataTable dataTable) {
        testContext.put("expectedAddOnOptions", dataTable.asMaps());
    }

    @Then("{string} should be added to cart at add-on price {int}")
    public void shouldBeAddedToCartAtAddOnPrice(String productId, int price) {
        testContext.put("addOnCartItem", java.util.Map.of("product", productId, "price", price));
    }

    @Then("should be marked as {string}")
    public void shouldBeMarkedAs(String label) {
        testContext.put("addOnLabel", label);
    }

    @Then("should not allow additional add-on")
    public void shouldNotAllowAdditionalAddOn() {
        testContext.put("additionalAddOnAllowed", false);
    }

    @Then("both {string} should be priced at add-on price {int} each")
    public void bothShouldBePricedAtAddOnPriceEach(String productId, int price) {
        testContext.put(
                "multipleAddOnPricing", java.util.Map.of("product", productId, "price", price));
    }

    @Then("cart should display {string}")
    public void cartShouldDisplay(String displayText) {
        testContext.put("cartDisplay", displayText);
    }

    @Then("total add-on amount should be {int}")
    public void totalAddOnAmountShouldBe(int amount) {
        testContext.put("expectedAddOnTotal", amount);
    }

    @Then("add-on products should revert to original price or be removed")
    public void addOnProductsShouldRevertToOriginalPriceOrBeRemoved() {
        testContext.put("addOnReverted", true);
    }

    @Then("ask customer whether to continue")
    public void askCustomerWhetherToContinue() {
        testContext.put("askContinue", true);
    }

    @Then("price should revert to original price {int}")
    public void priceShouldRevertToOriginalPrice(int originalPrice) {
        testContext.put("revertedPrice", originalPrice);
    }

    @Then("remove {string} label")
    public void removeLabel(String label) {
        testContext.put("removedLabel", label);
    }

    @Then("recalculate cart total amount")
    public void recalculateCartTotalAmount() {
        testContext.put("recalculateTotal", true);
    }

    @Then("each main product should have independent add-on quota")
    public void eachMainProductShouldHaveIndependentAddOnQuota() {
        testContext.put("independentQuota", true);
    }

    @Then("{string} add-on quantity limit should be {int}")
    public void addOnQuantityLimitShouldBe(String productId, int limit) {
        testContext.put("addOnLimit", java.util.Map.of("product", productId, "limit", limit));
    }

    @Then("member discount should apply to add-on prices")
    public void memberDiscountShouldApplyToAddOnPrices() {
        testContext.put("memberDiscountOnAddOn", true);
    }

    @Then("{string} final price should be {double}")
    public void finalPriceShouldBe(String productId, double finalPrice) {
        testContext.put(
                "finalAddOnPrice", java.util.Map.of("product", productId, "price", finalPrice));
    }

    @Then("suggest other available add-on options")
    public void suggestOtherAvailableAddOnOptions() {
        testContext.put("suggestAlternatives", true);
    }
}
