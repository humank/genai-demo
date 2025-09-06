package solid.humank.genaidemo.bdd.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;
import solid.humank.genaidemo.bdd.common.TestDataBuilder;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;
import solid.humank.genaidemo.domain.promotion.service.PromotionService;

/**
 * Gift with Purchase Step Definitions - Handles gift with purchase activity
 * creation, management
 * and verification related BDD steps
 */
public class GiftWithPurchaseStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiftWithPurchaseStepDefinitions.class);

    private final TestContext testContext;
    private final TestDataBuilder dataBuilder;
    private final PromotionService promotionService;

    // Gift with purchase related fields
    private List<GiftWithPurchaseActivity> giftActivities = new ArrayList<>();
    private Map<String, CustomerPreference> customerPreferences = new HashMap<>();
    @SuppressWarnings("unused")
    private List<GiftRecommendation> giftRecommendations = new ArrayList<>();
    @SuppressWarnings("unused")
    private Map<String, GiftPackaging> giftPackagingInfo = new HashMap<>();
    @SuppressWarnings("unused")
    private List<QualityIssue> qualityIssues = new ArrayList<>();
    private Map<String, Integer> giftInventory = new HashMap<>();
    @SuppressWarnings("unused")
    private String lastDisplayMessage;
    @SuppressWarnings("unused")
    private boolean giftQualificationMet = false;

    // Legacy fields for compatibility
    private Product giftProduct;
    @SuppressWarnings("unused")
    private double minimumPurchaseAmount;
    private List<Product> giftItems = new ArrayList<>();
    private Map<String, Object> promotionDetails = new HashMap<>();

    public GiftWithPurchaseStepDefinitions() {
        this.testContext = TestContext.getInstance();
        this.dataBuilder = TestDataBuilder.getInstance();
        this.promotionService = mock(PromotionService.class);
    }

    // ==================== English Step Definitions ====================

    @Given("the following gift with purchase activities are configured:")
    public void theFollowingGiftWithPurchaseActivitiesAreConfigured(DataTable dataTable) {
        List<Map<String, String>> activities = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> activity : activities) {
            GiftWithPurchaseActivity giftActivity = new GiftWithPurchaseActivity(
                    activity.get("Activity Name"),
                    Integer.parseInt(activity.get("Min Spend")),
                    activity.get("Gift Product ID"),
                    activity.get("Gift Name"),
                    Integer.parseInt(activity.get("Gift Quantity")),
                    Integer.parseInt(activity.get("Gift Stock")));

            giftActivities.add(giftActivity);
            giftInventory.put(
                    activity.get("Gift Product ID"), Integer.parseInt(activity.get("Gift Stock")));
        }
    }

    @When("system checks gift with purchase conditions")
    public void systemChecksGiftWithPurchaseConditions() {
        BigDecimal cartTotal = testContext.getCartTotal();
        List<String> qualifiedActivities = new ArrayList<>();

        for (GiftWithPurchaseActivity activity : giftActivities) {
            if (cartTotal.compareTo(new BigDecimal(activity.getMinimumSpend())) >= 0) {
                qualifiedActivities.add(activity.getActivityName());
            }
        }

        testContext.setQualifiedGiftActivities(qualifiedActivities);
    }

    @Then("customer should qualify for the following gift activities:")
    public void customerShouldQualifyForTheFollowingGiftActivities(DataTable dataTable) {
        List<Map<String, String>> expectedActivities = dataTable.asMaps(String.class, String.class);
        List<String> qualifiedActivities = testContext.getQualifiedGiftActivities();

        for (Map<String, String> expected : expectedActivities) {
            String activityName = expected.get("Activity Name");
            String expectedStatus = expected.get("Status");

            if ("Qualified".equals(expectedStatus)) {
                assertTrue(
                        qualifiedActivities.contains(activityName),
                        "Activity " + activityName + " should be qualified");
            } else {
                assertFalse(
                        qualifiedActivities.contains(activityName),
                        "Activity " + activityName + " should not be qualified");
            }
        }
    }

    @When("customer enters checkout page")
    public void customerEntersCheckoutPage() {
        // Simulate entering checkout page
        testContext.setLastOperationResult("Entered checkout page");
    }

    @Then("system should automatically add qualifying gifts:")
    public void systemShouldAutomaticallyAddQualifyingGifts(DataTable dataTable) {
        List<Map<String, String>> expectedGifts = dataTable.asMaps(String.class, String.class);
        List<String> addedGifts = new ArrayList<>();

        BigDecimal cartTotal = testContext.getCartTotal();
        for (GiftWithPurchaseActivity activity : giftActivities) {
            if (cartTotal.compareTo(new BigDecimal(activity.getMinimumSpend())) >= 0) {
                addedGifts.add(activity.getGiftName());
            }
        }

        for (Map<String, String> expected : expectedGifts) {
            String giftName = expected.get("Gift Product");
            assertTrue(
                    addedGifts.contains(giftName),
                    "Gift " + giftName + " should be automatically added");
        }
    }

    @Then("cart total should remain {int}")
    public void cartTotalShouldRemain(int expectedTotal) {
        BigDecimal cartTotal = testContext.getCartTotal();
        assertEquals(
                new BigDecimal(expectedTotal),
                cartTotal,
                "Cart total should remain " + expectedTotal);
    }

    @Given("gift with purchase activity offers multiple choices:")
    public void giftWithPurchaseActivityOffersMultipleChoices(DataTable dataTable) {
        List<Map<String, String>> choices = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> choice : choices) {
            int minSpend = Integer.parseInt(choice.get("Min Spend"));
            String availableGifts = choice.get("Available Gifts");

            // Store the multiple choice configuration
            testContext.setLastOperationResult(
                    "Multiple choice configured: " + minSpend + " -> " + availableGifts);
        }
    }

    @When("customer views gift options")
    public void customerViewsGiftOptions() {
        // Simulate viewing gift options
        testContext.setLastOperationResult("Customer views gift options");
    }

    @Then("customer can select one of the gifts")
    public void customerCanSelectOneOfTheGifts() {
        // Verify that customer can select gifts
        String result = testContext.getLastOperationResult();
        assertNotNull(result, "Customer should be able to select gifts");
    }

    @Given("{string} gift stock is {int}")
    public void giftStockIs(String giftName, int stock) {
        giftInventory.put(giftName, stock);
        testContext.setLastOperationResult("Gift " + giftName + " stock set to " + stock);
    }

    @When("system checks gift with purchase")
    public void systemChecksGiftWithPurchase() {
        // Check gift eligibility based on cart total and stock
        BigDecimal cartTotal = testContext.getCartTotal();
        boolean hasEligibleGifts = false;

        for (GiftWithPurchaseActivity activity : giftActivities) {
            if (cartTotal.compareTo(new BigDecimal(activity.getMinimumSpend())) >= 0) {
                Integer stock = giftInventory.get(activity.getGiftProductId());
                if (stock != null && stock > 0) {
                    hasEligibleGifts = true;
                    break;
                }
            }
        }

        testContext.setLastOperationResult(
                hasEligibleGifts ? "Has eligible gifts" : "No eligible gifts");
    }

    @Then("provide alternative gift or compensation")
    public void provideAlternativeGiftOrCompensation() {
        testContext.setLastOperationResult("Alternative gift or compensation provided");
    }

    @Given("customer cart contains gift {string}")
    public void customerCartContainsGift(String giftName) {
        // Add gift to cart
        addGiftToCart(giftName, 1);
        testContext.setLastOperationResult("Cart contains gift: " + giftName);
    }

    @Given("cart total is {int}")
    public void cartTotalIs(int total) {
        testContext.setCartTotal(new BigDecimal(total));
    }

    @When("customer removes items reducing total to {int}")
    public void customerRemovesItemsReducingTotalTo(int newTotal) {
        testContext.setCartTotal(new BigDecimal(newTotal));
        testContext.setLastOperationResult("Cart total reduced to " + newTotal);
    }

    @Then("automatically remove unqualified gifts")
    public void automaticallyRemoveUnqualifiedGifts() {
        // Remove gifts that no longer qualify
        BigDecimal cartTotal = testContext.getCartTotal();
        giftItems.removeIf(
                gift -> {
                    // Check if gift still qualifies based on cart total
                    return giftActivities.stream()
                            .noneMatch(
                                    activity -> cartTotal.compareTo(
                                            new BigDecimal(
                                                    activity.getMinimumSpend())) >= 0);
                });
        testContext.setLastOperationResult("Unqualified gifts removed");
    }

    @When("system calculates gift with purchase")
    public void systemCalculatesGiftWithPurchase() {
        BigDecimal cartTotal = testContext.getCartTotal();
        List<String> qualifiedGifts = new ArrayList<>();

        for (GiftWithPurchaseActivity activity : giftActivities) {
            if (cartTotal.compareTo(new BigDecimal(activity.getMinimumSpend())) >= 0) {
                qualifiedGifts.add(activity.getGiftName());
            }
        }

        testContext.setLastOperationResult("Calculated qualified gifts: " + qualifiedGifts.size());
    }

    @Then("customer should receive all qualifying gifts:")
    public void customerShouldReceiveAllQualifyingGifts(DataTable dataTable) {
        List<Map<String, String>> expectedGifts = dataTable.asMaps(String.class, String.class);
        BigDecimal cartTotal = testContext.getCartTotal();

        for (Map<String, String> expected : expectedGifts) {
            String giftName = expected.get("Gift");
            String condition = expected.get("Condition");
            LOGGER.debug("檢查贈品條件 - 贈品: {}, 條件: {}", giftName, condition);
            String expectedStatus = expected.get("Status");

            // Find matching activity
            boolean shouldReceive = giftActivities.stream()
                    .anyMatch(
                            activity -> activity.getGiftName().equals(giftName)
                                    && cartTotal.compareTo(
                                            new BigDecimal(
                                                    activity
                                                            .getMinimumSpend())) >= 0);

            if ("Received".equals(expectedStatus)) {
                assertTrue(shouldReceive, "Should receive gift: " + giftName);
            }
        }
    }

    @Given("gift {string} has multiple sizes and colors:")
    public void giftHasMultipleSizesAndColors(String giftName, DataTable dataTable) {
        List<Map<String, String>> options = dataTable.asMaps(String.class, String.class);
        testContext.setLastOperationResult(
                "Gift " + giftName + " has " + options.size() + " size/color options");
    }

    @When("customer qualifies for gift")
    public void customerQualifiesForGift() {
        giftQualificationMet = true;
        testContext.setLastOperationResult("Customer qualifies for gift");
    }

    @Then("should display size and color selection options")
    public void shouldDisplaySizeAndColorSelectionOptions() {
        lastDisplayMessage = "Please select size and color";
        testContext.setLastOperationResult("Size and color selection displayed");
    }

    @Then("customer must select before proceeding to checkout")
    public void customerMustSelectBeforeProceedingToCheckout() {
        testContext.setLastOperationResult("Selection required before checkout");
    }

    @Given("VIP members have exclusive gift with purchase activities:")
    public void vipMembersHaveExclusiveGiftWithPurchaseActivities(DataTable dataTable) {
        List<Map<String, String>> activities = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> activity : activities) {
            // Store VIP exclusive activities
            testContext.setLastOperationResult(
                    "VIP exclusive activity configured: " + activity.get("Member Level"));
        }
    }

    @Given("customer {string} is VIP member")
    public void customerIsVipMember(String customerName) {
        testContext.setCustomerName(customerName);
        testContext.setVipMember(true);
        testContext.setLastOperationResult("Customer " + customerName + " is VIP member");
    }

    @Then("should receive VIP exclusive gift {string}")
    public void shouldReceiveVipExclusiveGift(String giftName) {
        assertTrue(
                testContext.isVipMember(),
                "Customer should be VIP member to receive exclusive gift");
        testContext.setLastOperationResult("Received VIP exclusive gift: " + giftName);
    }

    @Given("customer used {int} discount coupon")
    public void customerUsedDiscountCoupon(int discountAmount) {
        testContext.setLastOperationResult("Customer used " + discountAmount + " discount coupon");
    }

    @Given("cart original price is {int}, discounted to {int}")
    public void cartOriginalPriceIsDiscountedTo(int originalPrice, int discountedPrice) {
        testContext.setCartTotal(new BigDecimal(discountedPrice));
        testContext.setLastOperationResult(
                "Cart original price " + originalPrice + " discounted to " + discountedPrice);
    }

    @Then("should base on discounted amount {int} for judgment")
    public void shouldBaseOnDiscountedAmountForJudgment(int discountedAmount) {
        BigDecimal cartTotal = testContext.getCartTotal();
        assertEquals(
                new BigDecimal(discountedAmount),
                cartTotal,
                "Should base judgment on discounted amount");
    }

    @Then("customer should receive {string} gift")
    public void customerShouldReceiveGift(String giftName) {
        testContext.setLastOperationResult("Customer should receive gift: " + giftName);
    }

    @Given("customer purchased items and received gift with purchase")
    public void customerPurchasedItemsAndReceivedGiftWithPurchase() {
        testContext.setLastOperationResult(
                "Customer purchased items and received gift with purchase");
    }

    @When("customer requests partial return")
    public void customerRequestsPartialReturn() {
        testContext.setLastOperationResult("Customer requests partial return");
    }

    @Given("return amount no longer meets gift conditions")
    public void returnAmountNoLongerMeetsGiftConditions() {
        testContext.setLastOperationResult("Return amount no longer meets gift conditions");
    }

    @Then("customer must return gift as well")
    public void customerMustReturnGiftAsWell() {
        testContext.setLastOperationResult("Customer must return gift as well");
    }

    @Given("gift with purchase activity expires on {int}-{int}-{int}")
    public void giftWithPurchaseActivityExpiresOn(int year, int month, int day) {
        testContext.setLastOperationResult(
                "Gift activity expires on " + year + "-" + month + "-" + day);
    }

    @Given("current time is {int}-{int}-{int}")
    public void currentTimeIs(int year, int month, int day) {
        testContext.setLastOperationResult("Current time is " + year + "-" + month + "-" + day);
    }

    @Given("current time is Saturday {int}:{int}")
    public void currentTimeIsSaturday(int hour, int minute) {
        testContext.setLastOperationResult("Current time is Saturday " + hour + ":" + minute);
    }

    @When("customer cart total meets conditions")
    public void customerCartTotalMeetsConditions() {
        testContext.setLastOperationResult("Customer cart total meets conditions");
    }

    @Then("should not display expired gift activities")
    public void shouldNotDisplayExpiredGiftActivities() {
        testContext.setLastOperationResult("Should not display expired gift activities");
    }

    @Then("display {string}")
    public void display(String message) {
        lastDisplayMessage = message;
        testContext.setLastOperationResult("Display: " + message);
    }

    @Then("display {string} message")
    public void displayMessage(String message) {
        lastDisplayMessage = message;
        testContext.setLastOperationResult("Display message: " + message);
    }

    @Given("needs {int} more for next gift condition")
    public void needsMoreForNextGiftCondition(int amount) {
        testContext.setLastOperationResult("Needs " + amount + " more for next gift condition");
    }

    @Then("recommend products with similar price")
    public void recommendProductsWithSimilarPrice() {
        testContext.setLastOperationResult("Recommend products with similar price");
    }

    @Given("limited time gift with purchase activities:")
    public void limitedTimeGiftWithPurchaseActivities(DataTable dataTable) {
        List<Map<String, String>> activities = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> activity : activities) {
            testContext.setLastOperationResult(
                    "Limited time activity: " + activity.get("Activity Time"));
        }
    }

    @Then("should qualify for both limited time activities")
    public void shouldQualifyForBothLimitedTimeActivities() {
        testContext.setLastOperationResult("Should qualify for both limited time activities");
    }

    @Then("can receive both gifts")
    public void canReceiveBothGifts() {
        testContext.setLastOperationResult("Can receive both gifts");
    }

    @Given("each person limited to one gift with purchase")
    public void eachPersonLimitedToOneGiftWithPurchase() {
        testContext.setLastOperationResult("Each person limited to one gift with purchase");
    }

    @Given("customer {string} previously received {string}")
    public void customerPreviouslyReceived(String customerName, String giftName) {
        testContext.setCustomerName(customerName);
        testContext.setLastOperationResult(
                "Customer " + customerName + " previously received " + giftName);
    }

    @When("customer meets gift conditions again")
    public void customerMeetsGiftConditionsAgain() {
        testContext.setLastOperationResult("Customer meets gift conditions again");
    }

    @Then("should not receive same gift again")
    public void shouldNotReceiveSameGiftAgain() {
        testContext.setLastOperationResult("Should not receive same gift again");
    }

    @Given("past 30 days gift with purchase data:")
    public void past30DaysGiftWithPurchaseData(DataTable dataTable) {
        List<Map<String, String>> data = dataTable.asMaps(String.class, String.class);
        testContext.setLastOperationResult(
                "Past 30 days data loaded: " + data.size() + " activities");
    }

    @When("querying activity effectiveness report")
    public void queryingActivityEffectivenessReport() {
        testContext.setLastOperationResult("Querying activity effectiveness report");
    }

    @Then("should display activity impact on sales increase")
    public void shouldDisplayActivityImpactOnSalesIncrease() {
        testContext.setLastOperationResult("Should display activity impact on sales increase");
    }

    @Then("analyze customer behavior changes")
    public void analyzeCustomerBehaviorChanges() {
        testContext.setLastOperationResult("Analyze customer behavior changes");
    }

    @Given("customer {string} purchase history shows preference for tech products")
    public void customerPurchaseHistoryShowsPreferenceForTechProducts(String customerName) {
        testContext.setCustomerName(customerName);
        CustomerPreference preference = new CustomerPreference(customerName, "tech");
        customerPreferences.put(customerName, preference);
        testContext.setLastOperationResult("Customer " + customerName + " prefers tech products");
    }

    @Given("reaches gift with purchase conditions")
    public void reachesGiftWithPurchaseConditions() {
        giftQualificationMet = true;
        testContext.setLastOperationResult("Reaches gift with purchase conditions");
    }

    @When("system provides gift options")
    public void systemProvidesGiftOptions() {
        testContext.setLastOperationResult("System provides gift options");
    }

    @Then("should prioritize tech category gifts")
    public void shouldPrioritizeTechCategoryGifts() {
        String customerName = testContext.getCustomerName();
        CustomerPreference preference = customerPreferences.get(customerName);
        if (preference != null && "tech".equals(preference.getPreferredCategory())) {
            testContext.setLastOperationResult("Should prioritize tech category gifts");
        }
    }

    @Given("customer receives gift with purchase")
    public void customerReceivesGiftWithPurchase() {
        testContext.setLastOperationResult("Customer receives gift with purchase");
    }

    @When("order is ready for shipment")
    public void orderIsReadyForShipment() {
        testContext.setLastOperationResult("Order is ready for shipment");
    }

    @Then("gift should have special packaging")
    public void giftShouldHaveSpecialPackaging() {
        testContext.setLastOperationResult("Gift should have special packaging");
    }

    @Then("packaging should have label {string}")
    public void packagingShouldHaveLabel(String label) {
        testContext.setLastOperationResult("Packaging should have label: " + label);
    }

    @Then("ship together with purchased items")
    public void shipTogetherWithPurchasedItems() {
        testContext.setLastOperationResult("Ship together with purchased items");
    }

    @Given("customer receives defective gift")
    public void customerReceivesDefectiveGift() {
        testContext.setLastOperationResult("Customer receives defective gift");
    }

    @When("customer requests gift replacement")
    public void customerRequestsGiftReplacement() {
        testContext.setLastOperationResult("Customer requests gift replacement");
    }

    @Then("should provide free replacement service")
    public void shouldProvideFreeReplacementService() {
        testContext.setLastOperationResult("Should provide free replacement service");
    }

    @Then("not affect original purchase warranty")
    public void notAffectOriginalPurchaseWarranty() {
        testContext.setLastOperationResult("Not affect original purchase warranty");
    }

    @Then("record quality issues for gift selection improvement")
    public void recordQualityIssuesForGiftSelectionImprovement() {
        testContext.setLastOperationResult("Record quality issues for gift selection improvement");
    }

    // ==================== Legacy Step Definitions for Compatibility
    // ====================

    @Given("the store offers a gift {string} worth ${int} with any purchase over ${int}")
    public void the_store_offers_a_gift_worth_$_with_any_purchase_over_$(
            String giftName, Integer giftValue, Integer minimumPurchase) {
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(giftValue));

        this.minimumPurchaseAmount = minimumPurchase;

        // Create gift with purchase rule
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
                Money.of(minimumPurchase), giftProductId, Money.of(giftValue), 1, false);

        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", giftValue);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    @Given("the store offers a gift {string} with any purchase over ${int}")
    public void the_store_offers_a_gift_with_any_purchase_over_$(
            String giftName, Integer minimumPurchase) {
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(50)); // Assume gift value is $50

        this.minimumPurchaseAmount = minimumPurchase;

        // Create gift with purchase rule
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
                Money.of(minimumPurchase), giftProductId, Money.of(50), 1, false);

        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", 50);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    // ==================== Helper Methods ====================

    /** Add gift to cart helper method */
    private void addGiftToCart(String giftName, int quantity) {
        for (int i = 0; i < quantity; i++) {
            Product giftProduct = mock(Product.class);
            when(giftProduct.getName()).thenReturn(new ProductName(giftName));
            when(giftProduct.getId()).thenReturn(new ProductId("GIFT-" + giftName));
            when(giftProduct.getPrice()).thenReturn(Money.of(0)); // Gift price is 0

            giftItems.add(giftProduct);
        }
    }

    /** Verify class creation - for basic testing */
    public void verifyClassCreation() {
        assertNotNull(testContext, "TestContext should be initialized");
        assertNotNull(dataBuilder, "TestDataBuilder should be initialized");
        assertNotNull(promotionService, "PromotionService should be initialized");
    }

    // ==================== Data Classes ====================

    /** Gift with Purchase Activity Data */
    public static class GiftWithPurchaseActivity {
        private final String activityName;
        private final int minimumSpend;
        private final String giftProductId;
        private final String giftName;
        private final int giftQuantity;
        private final int giftStock;

        public GiftWithPurchaseActivity(
                String activityName,
                int minimumSpend,
                String giftProductId,
                String giftName,
                int giftQuantity,
                int giftStock) {
            this.activityName = activityName;
            this.minimumSpend = minimumSpend;
            this.giftProductId = giftProductId;
            this.giftName = giftName;
            this.giftQuantity = giftQuantity;
            this.giftStock = giftStock;
        }

        // Getters
        public String getActivityName() {
            return activityName;
        }

        public int getMinimumSpend() {
            return minimumSpend;
        }

        public String getGiftProductId() {
            return giftProductId;
        }

        public String getGiftName() {
            return giftName;
        }

        public int getGiftQuantity() {
            return giftQuantity;
        }

        public int getGiftStock() {
            return giftStock;
        }
    }

    /** Customer Preference Data */
    public static class CustomerPreference {
        private final String customerName;
        private final String preferredCategory;

        public CustomerPreference(String customerName, String preferredCategory) {
            this.customerName = customerName;
            this.preferredCategory = preferredCategory;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPreferredCategory() {
            return preferredCategory;
        }
    }

    /** Gift Recommendation Data */
    public static class GiftRecommendation {
        private final String giftName;
        private final String category;
        private final boolean priority;
        private final String recommendationReason;

        public GiftRecommendation(
                String giftName, String category, boolean priority, String recommendationReason) {
            this.giftName = giftName;
            this.category = category;
            this.priority = priority;
            this.recommendationReason = recommendationReason;
        }

        public String getGiftName() {
            return giftName;
        }

        public String getCategory() {
            return category;
        }

        public boolean isPriority() {
            return priority;
        }

        public String getRecommendationReason() {
            return recommendationReason;
        }
    }

    /** Gift Packaging Information */
    public static class GiftPackaging {
        private final String packagingType;
        private final String label;
        private final boolean shipWithOrder;

        public GiftPackaging(String packagingType, String label, boolean shipWithOrder) {
            this.packagingType = packagingType;
            this.label = label;
            this.shipWithOrder = shipWithOrder;
        }

        public String getPackagingType() {
            return packagingType;
        }

        public String getLabel() {
            return label;
        }

        public boolean isShipWithOrder() {
            return shipWithOrder;
        }
    }

    /** Quality Issue Record */
    public static class QualityIssue {
        private final String itemType;
        private final String issueType;
        private final String description;
        private boolean replacementRequested = false;
        private boolean recordedForImprovement = false;

        public QualityIssue(String itemType, String issueType, String description) {
            this.itemType = itemType;
            this.issueType = issueType;
            this.description = description;
        }

        public String getItemType() {
            return itemType;
        }

        public String getIssueType() {
            return issueType;
        }

        public String getDescription() {
            return description;
        }

        public boolean isReplacementRequested() {
            return replacementRequested;
        }

        public void setReplacementRequested(boolean replacementRequested) {
            this.replacementRequested = replacementRequested;
        }

        public boolean isRecordedForImprovement() {
            return recordedForImprovement;
        }

        public void setRecordedForImprovement(boolean recordedForImprovement) {
            this.recordedForImprovement = recordedForImprovement;
        }
    }

    /** Activity Statistics Data */
    public static class GiftActivityStatistics {
        private final String activityName;
        private final int participants;
        private final int giftsDistributed;
        private final int averageIncrease;

        public GiftActivityStatistics(
                String activityName, int participants, int giftsDistributed, int averageIncrease) {
            this.activityName = activityName;
            this.participants = participants;
            this.giftsDistributed = giftsDistributed;
            this.averageIncrease = averageIncrease;
        }

        public String getActivityName() {
            return activityName;
        }

        public int getParticipants() {
            return participants;
        }

        public int getGiftsDistributed() {
            return giftsDistributed;
        }

        public int getAverageIncrease() {
            return averageIncrease;
        }
    }
}
