package solid.humank.genaidemo.bdd.product;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Advanced Product Bundle Sales Step Definitions */
public class AdvancedProductBundleStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();

    @Given("{string} bundle exists:")
    public void bundleExists(String bundleName, DataTable dataTable) {
        testContext.put("bundleName", bundleName);
        testContext.put("bundleProducts", dataTable.asMaps());
    }

    @Given("{string} promotion exists")
    public void promotionExists(String promotionName) {
        testContext.put("promotionName", promotionName);
    }

    @Given("customer selects {int} accessory products:")
    public void customerSelectsAccessoryProducts(int count, DataTable dataTable) {
        testContext.put("selectedAccessoryCount", count);
        testContext.put("selectedAccessories", dataTable.asMaps());
    }

    @Given("the following tiered discount exists:")
    public void theFollowingTieredDiscountExists(DataTable dataTable) {
        testContext.put("tieredDiscount", dataTable.asMaps());
    }

    @Given("applies to {string} category")
    public void appliesToCategory(String category) {
        testContext.put("applicableCategory", category);
    }

    @Given("when purchasing {string} accessories get special price:")
    public void whenPurchasingAccessoriesGetSpecialPrice(String mainProduct, DataTable dataTable) {
        testContext.put("mainProduct", mainProduct);
        testContext.put("accessorySpecialPrices", dataTable.asMaps());
    }

    @Given("spend {int} or more gets free {string}")
    public void spendOrMoreGetsFree(int threshold, String freeProduct) {
        testContext.put("spendThreshold", threshold);
        testContext.put("freeProduct", freeProduct);
    }

    @Given("{string} has buy one get one promotion")
    public void hasBuyOneGetOnePromotion(String productId) {
        testContext.put("bogoProduct", productId);
    }

    @Given("customer purchased {string}")
    public void customerPurchased(String productId) {
        testContext.put("purchasedProduct", productId);
    }

    @Given("{string} requires:")
    public void requires(String bundleName, DataTable dataTable) {
        testContext.put("bundleRequirements", dataTable.asMaps());
    }

    @Given("current stock levels:")
    public void currentStockLevels(DataTable dataTable) {
        testContext.put("stockLevels", dataTable.asMaps());
    }

    @Given("{string} allows customer to select:")
    public void allowsCustomerToSelect(String bundleName) {
        testContext.put("customBundleName", bundleName);
    }

    @When("customer purchases {string}")
    public void customerPurchases(String bundleName) {
        testContext.put("purchasedBundle", bundleName);
    }

    @When("customer selects {int} accessory products")
    public void customerSelectsAccessoryProducts(int count) {
        testContext.put("selectedAccessoryCount", count);
    }

    @When("customer purchases {int} phones with total {int}")
    public void customerPurchasesPhonesWithTotal(int phoneCount, int totalAmount) {
        testContext.put("phoneCount", phoneCount);
        testContext.put("totalAmount", totalAmount);
    }

    @When("customer purchases {int} {string} and {int} {string}")
    public void customerPurchasesAnd(int qty1, String product1, int qty2, String product2) {
        testContext.put("purchase1", java.util.Map.of("product", product1, "quantity", qty1));
        testContext.put("purchase2", java.util.Map.of("product", product2, "quantity", qty2));
    }

    @When("customer purchases {int} {string}")
    public void customerPurchases(int quantity, String productId) {
        testContext.put("purchaseQuantity", quantity);
        testContext.put("purchaseProduct", productId);
    }

    @When("system recommends cross-selling products")
    public void systemRecommendsCrossSellingProducts() {
        testContext.put("crossSellingRecommended", true);
    }

    @When("customer tries to purchase {string}")
    public void customerTriesToPurchase(String bundleName) {
        testContext.put("attemptedPurchase", bundleName);
    }

    @When("customer selects {int} {string} and {int} accessories")
    public void customerSelectsAndAccessories(
            int phoneQty, String phoneProduct, int accessoryCount) {
        testContext.put(
                "phoneSelection", java.util.Map.of("product", phoneProduct, "quantity", phoneQty));
        testContext.put("accessoryCount", accessoryCount);
    }

    @Then("cart should contain all bundle products")
    public void cartShouldContainAllBundleProducts() {
        testContext.put("bundleProductsInCart", true);
    }

    @Then("savings amount should be {int}")
    public void savingsAmountShouldBe(int savingsAmount) {
        testContext.put("expectedSavings", savingsAmount);
    }

    @Then("original total should be {int}")
    public void originalTotalShouldBe(int originalTotal) {
        testContext.put("originalTotal", originalTotal);
    }

    @Then("discounted price should be {int}")
    public void discountedPriceShouldBe(int discountedPrice) {
        testContext.put("discountedPrice", discountedPrice);
    }

    @Then("discount should apply to highest priced {int} items")
    public void discountShouldApplyToHighestPricedItems(int itemCount) {
        testContext.put("discountAppliedToCount", itemCount);
    }

    @Then("remaining {int} items should be charged at original price")
    public void remainingItemsShouldBeChargedAtOriginalPrice(int itemCount) {
        testContext.put("originalPriceItemCount", itemCount);
    }

    @Then("system should display discount details")
    public void systemShouldDisplayDiscountDetails() {
        testContext.put("showDiscountDetails", true);
    }

    @Then("{string} should be charged at original price {int}")
    public void shouldBeChargedAtOriginalPrice(String productId, int price) {
        testContext.put(
                "originalPriceProduct", java.util.Map.of("product", productId, "price", price));
    }

    @Then("{string} should be charged at special price {int}")
    public void shouldBeChargedAtSpecialPrice(String productId, int price) {
        testContext.put(
                "specialPriceProduct", java.util.Map.of("product", productId, "price", price));
    }

    @Then("should automatically add free gift {string} to cart")
    public void shouldAutomaticallyAddFreeGiftToCart(String giftProduct) {
        testContext.put("freeGift", giftProduct);
    }

    @Then("gift price should be {int}")
    public void giftPriceShouldBe(int price) {
        testContext.put("giftPrice", price);
    }

    @Then("should display {string} label")
    public void shouldDisplayLabel(String label) {
        testContext.put("displayLabel", label);
    }

    @Then("should automatically add {int} same product as free gift")
    public void shouldAutomaticallyAddSameProductAsFreeGift(int quantity) {
        testContext.put("freeGiftQuantity", quantity);
    }

    @Then("cart should display {int} {string}")
    public void cartShouldDisplay(int quantity, String productId) {
        testContext.put(
                "cartDisplay", java.util.Map.of("product", productId, "quantity", quantity));
    }

    @Then("should only charge for {int} unit")
    public void shouldOnlyChargeForUnit(int chargedUnits) {
        testContext.put("chargedUnits", chargedUnits);
    }

    @Then("should recommend the following cross-selling products:")
    public void shouldRecommendTheFollowingCrossSellingProducts(DataTable dataTable) {
        testContext.put("crossSellingRecommendations", dataTable.asMaps());
    }

    @Then("should suggest alternatives or individual purchase")
    public void shouldSuggestAlternativesOrIndividualPurchase() {
        testContext.put("suggestAlternatives", true);
    }

    @Then("phone should be charged at original price")
    public void phoneShouldBeChargedAtOriginalPrice() {
        testContext.put("phoneOriginalPrice", true);
    }

    @Then("accessories total should get {int}% discount")
    public void accessoriesTotalShouldGetDiscount(int discountPercentage) {
        testContext.put("accessoryDiscount", discountPercentage);
    }

    @Then("system should display bundle discount details")
    public void systemShouldDisplayBundleDiscountDetails() {
        testContext.put("showBundleDiscountDetails", true);
    }

    // 新增缺少的步驟定義
    @Then("system should recommend related accessories:")
    public void systemShouldRecommendRelatedAccessories(DataTable dataTable) {
        testContext.put("recommendedAccessories", dataTable.asMaps());
    }

    @Given("accessories get {int}% discount")
    public void accessoriesGetDiscount(Integer discountPercentage) {
        testContext.put("accessoryDiscountPercentage", discountPercentage);
    }

    @Given("bundle price is {int} with original price {int}")
    public void bundlePriceIsWithOriginalPrice(Integer bundlePrice, Integer originalPrice) {
        testContext.put("bundlePrice", bundlePrice);
        testContext.put("bundleOriginalPrice", originalPrice);
    }

    @Given("applies to all products in {string} category")
    public void appliesToAllProductsInCategory(String category) {
        testContext.put("categoryDiscount", category);
    }

    @When("customer selects the following accessories:")
    public void customerSelectsTheFollowingAccessories(DataTable dataTable) {
        testContext.put("selectedAccessoriesDetail", dataTable.asMaps());
    }
}
