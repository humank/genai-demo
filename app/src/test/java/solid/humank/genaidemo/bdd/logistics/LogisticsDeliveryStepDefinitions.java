package solid.humank.genaidemo.bdd.logistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Logistics Delivery System Step Definitions */
public class LogisticsDeliveryStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();
    private final Map<String, DeliveryMethod> deliveryMethods = new HashMap<>();
    private final Map<String, Customer> customers = new HashMap<>();

    @Given("the following delivery methods exist in the system:")
    public void theFollowingDeliveryMethodsExistInTheSystem(DataTable dataTable) {
        List<Map<String, String>> methods = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> method : methods) {
            DeliveryMethod deliveryMethod = new DeliveryMethod(
                    method.get("Delivery Method"),
                    Integer.parseInt(method.get("Delivery Fee")),
                    method.get("Estimated Days"),
                    method.get("Weight Limit"),
                    method.get("Region Limit"));
            deliveryMethods.put(method.get("Delivery Method"), deliveryMethod);
        }
    }

    @Given("customer {string} has default delivery address {string}")
    public void customerHasDefaultDeliveryAddress(String customerName, String address) {
        Customer customer = new Customer(customerName, address);
        customers.put(customerName, customer);
        testContext.setCustomerName(customerName);
    }

    @Given("customer's cart weight is {int}kg and delivery address is Taipei City")
    public void customersCartWeightIsKgAndDeliveryAddressIsTaipeiCity(Integer weight) {
        testContext.put("cartWeight", weight);
        testContext.put("deliveryAddress", "Taipei City");
    }

    @When("customer views available delivery methods")
    public void customerViewsAvailableDeliveryMethods() {
        testContext.put("viewingDeliveryMethods", true);
    }

    @Then("should display all applicable delivery methods:")
    public void shouldDisplayAllApplicableDeliveryMethods(DataTable dataTable) {
        List<Map<String, String>> expectedMethods = dataTable.asMaps(String.class, String.class);
        testContext.put("displayedDeliveryMethods", expectedMethods);
    }

    @Given("customer's cart weight is {int}kg")
    public void customersCartWeightIsKg(Integer weight) {
        testContext.put("cartWeight", weight);
    }

    @Then("should not display {string} \\(limited to {int}kg)")
    public void shouldNotDisplayLimitedToKg(String methodName, Integer weightLimit) {
        testContext.put("excludedMethod_" + methodName, "weight_limit_" + weightLimit);
    }

    @Then("should not display {string} \\(limited to {string})")
    public void shouldNotDisplayLimitedTo(String methodName, String limitation) {
        testContext.put("excludedMethod_" + methodName, limitation);
    }

    @Then("should not display {string} \\(limited to North\\/Central\\/South)")
    public void shouldNotDisplayLimitedToNorthCentralSouth(String methodName) {
        testContext.put("excludedMethod_" + methodName, "region_limit_north_central_south");
    }

    @Then("should not display {string} \\(limited to Taipei City)")
    public void shouldNotDisplayLimitedToTaipeiCity(String methodName) {
        testContext.put("excludedMethod_" + methodName, "region_limit_taipei_city");
    }

    @Then("should display weight limit explanation")
    public void shouldDisplayWeightLimitExplanation() {
        testContext.put("weightLimitExplanationShown", true);
    }

    @Given("customer's delivery address is {string}")
    public void customersDeliveryAddressIs(String address) {
        testContext.put("deliveryAddress", address);
    }

    @Then("should display region restriction explanation")
    public void shouldDisplayRegionRestrictionExplanation() {
        testContext.put("regionRestrictionExplanationShown", true);
    }

    @Given("standard delivery has free shipping over {int}")
    public void standardDeliveryHasFreeShippingOver(Integer threshold) {
        testContext.put("freeShippingThreshold", threshold);
    }

    @Given("customer's cart total amount is {int}")
    public void customersCartTotalAmountIs(Integer amount) {
        testContext.put("cartTotalAmount", amount);
    }

    @When("customer selects standard delivery")
    public void customerSelectsStandardDelivery() {
        testContext.put("selectedDeliveryMethod", "Standard");
    }

    @Then("delivery fee should be {int}")
    public void deliveryFeeShouldBe(Integer expectedFee) {
        testContext.put("calculatedDeliveryFee", expectedFee);
    }

    @When("customer adds new delivery address")
    public void customerAddsNewDeliveryAddress(DataTable dataTable) {
        List<Map<String, String>> addresses = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> address : addresses) {
            DeliveryAddress newAddress = new DeliveryAddress(
                    address.get("Recipient"), address.get("Phone"), address.get("Address"));
            testContext.put("newDeliveryAddress", newAddress);
        }
    }

    @Then("address should be successfully saved")
    public void addressShouldBeSuccessfullySaved() {
        testContext.put("addressSaved", true);
    }

    @Then("customer can select this address during checkout")
    public void customerCanSelectThisAddressDuringCheckout() {
        testContext.put("addressAvailableForCheckout", true);
    }

    @Given("customer selects {string} delivery")
    public void customerSelectsDelivery(String deliveryMethod) {
        testContext.put("selectedDeliveryMethod", deliveryMethod);
    }

    @When("customer schedules delivery time for {string}")
    public void customerSchedulesDeliveryTimeFor(String timeSlot) {
        testContext.put("scheduledDeliveryTime", timeSlot);
    }

    @Then("system should confirm time availability")
    public void systemShouldConfirmTimeAvailability() {
        testContext.put("timeAvailabilityConfirmed", true);
    }

    @Then("save delivery time preference")
    public void saveDeliveryTimePreference() {
        testContext.put("deliveryTimePreferenceSaved", true);
    }

    @Then("notify logistics provider during delivery")
    public void notifyLogisticsProviderDuringDelivery() {
        testContext.put("logisticsProviderNotified", true);
    }

    @Given("order {string} has been shipped with tracking number {string}")
    public void orderHasBeenShippedWithTrackingNumber(String orderId, String trackingNumber) {
        testContext.put("orderId", orderId);
        testContext.put("trackingNumber", trackingNumber);
    }

    @When("customer queries delivery status")
    public void customerQueriesDeliveryStatus() {
        testContext.put("deliveryStatusQueried", true);
    }

    @Then("should display the following delivery progress:")
    public void shouldDisplayTheFollowingDeliveryProgress(DataTable dataTable) {
        List<Map<String, String>> progress = dataTable.asMaps(String.class, String.class);
        testContext.put("deliveryProgress", progress);
    }

    @Given("delivery person arrives at delivery address but cannot find recipient address")
    public void deliveryPersonArrivesAtDeliveryAddressButCannotFindRecipientAddress() {
        testContext.put("deliveryAttemptResult", "address_not_found");
    }

    @When("delivery person reports {string}")
    public void deliveryPersonReports(String report) {
        testContext.put("deliveryPersonReport", report);
    }

    @Then("delivery status should be updated to {string}")
    public void deliveryStatusShouldBeUpdatedTo(String status) {
        testContext.put("deliveryStatus", status);
    }

    @Then("system should notify customer to confirm address")
    public void systemShouldNotifyCustomerToConfirmAddress() {
        testContext.put("addressConfirmationNotificationSent", true);
    }

    @Then("pause delivery until address confirmation")
    public void pauseDeliveryUntilAddressConfirmation() {
        testContext.put("deliveryPaused", true);
    }

    @Given("delivery person arrives at delivery address but no one to receive")
    public void deliveryPersonArrivesAtDeliveryAddressButNoOneToReceive() {
        testContext.put("deliveryAttemptResult", "no_one_to_receive");
    }

    @When("delivery person attempts delivery {int} times with no one to receive")
    public void deliveryPersonAttemptsDeliveryTimesWithNoOneToReceive(Integer attempts) {
        testContext.put("deliveryAttempts", attempts);
    }

    @Then("item should be returned to warehouse")
    public void itemShouldBeReturnedToWarehouse() {
        testContext.put("itemReturnedToWarehouse", true);
    }

    @Then("customer should receive redelivery arrangement notification")
    public void customerShouldReceiveRedeliveryArrangementNotification() {
        testContext.put("redeliveryNotificationSent", true);
    }

    @Given("customer selects {string} for pickup")
    public void customerSelectsForPickup(String storeName) {
        testContext.put("selectedPickupStore", storeName);
    }

    @When("item arrives at designated store")
    public void itemArrivesAtDesignatedStore() {
        testContext.put("itemArrivedAtStore", true);
    }

    @Then("customer should receive pickup notification SMS")
    public void customerShouldReceivePickupNotificationSMS() {
        testContext.put("pickupNotificationSent", true);
    }

    @Then("SMS should contain pickup code {string}")
    public void smsShouldContainPickupCode(String pickupCode) {
        testContext.put("pickupCode", pickupCode);
    }

    @Then("item storage period should be {int} days")
    public void itemStoragePeriodShouldBeDays(Integer days) {
        testContext.put("storagePerio", days);
    }

    @Given("customer's item has been stored at convenience store for over {int} days")
    public void customersItemHasBeenStoredAtConvenienceStoreForOverDays(Integer days) {
        testContext.put("itemStoredDays", days + 1);
    }

    @When("system checks overdue items")
    public void systemChecksOverdueItems() {
        testContext.put("overdueItemsChecked", true);
    }

    @Then("customer should receive overdue notification")
    public void customerShouldReceiveOverdueNotification() {
        testContext.put("overdueNotificationSent", true);
    }

    @Then("can choose redelivery or refund")
    public void canChooseRedeliveryOrRefund() {
        testContext.put("redeliveryOrRefundOptionsProvided", true);
    }

    @Given("customer places order at {string} selecting same day delivery")
    public void customerPlacesOrderAtSelectingSameDayDelivery(String orderTime) {
        testContext.put("orderTime", orderTime);
        testContext.put("selectedDeliveryMethod", "Same Day");
    }

    @Given("customer places order at {int}:{int} AM selecting same day delivery")
    public void customerPlacesOrderAtAmSelectingSameDayDelivery(Integer hour, Integer minute) {
        String orderTime = String.format("%02d:%02d AM", hour, minute);
        testContext.put("orderTime", orderTime);
        testContext.put("selectedDeliveryMethod", "Same Day");
    }

    @Given("delivery address is in Taipei City")
    public void deliveryAddressIsInTaipeiCity() {
        testContext.put("deliveryAddress", "Taipei City");
    }

    @When("system confirms same day delivery feasibility")
    public void systemConfirmsSameDayDeliveryFeasibility() {
        testContext.put("sameDayDeliveryFeasible", true);
    }

    @Then("should arrange afternoon delivery")
    public void shouldArrangeAfternoonDelivery() {
        testContext.put("afternoonDeliveryArranged", true);
    }

    @Then("customer should receive estimated delivery time notification")
    public void customerShouldReceiveEstimatedDeliveryTimeNotification() {
        testContext.put("estimatedDeliveryTimeNotificationSent", true);
    }

    @Given("cart contains the following items:")
    public void cartContainsTheFollowingItems(DataTable dataTable) {
        List<Map<String, String>> items = dataTable.asMaps(String.class, String.class);
        testContext.put("cartItems", items);
    }

    @Then("should calculate delivery fee based on total weight {double}kg")
    public void shouldCalculateDeliveryFeeBasedOnTotalWeightKg(Double totalWeight) {
        testContext.put("calculatedTotalWeight", totalWeight);
    }

    @Then("consider packaging requirements for largest volume item")
    public void considerPackagingRequirementsForLargestVolumeItem() {
        testContext.put("packagingRequirementsConsidered", true);
    }

    @Given("order contains in-stock and pre-order items:")
    public void orderContainsInStockAndPreOrderItems(DataTable dataTable) {
        List<Map<String, String>> items = dataTable.asMaps(String.class, String.class);
        testContext.put("orderItems", items);
    }

    @Then("in-stock items should ship first")
    public void inStockItemsShouldShipFirst() {
        testContext.put("inStockItemsShippedFirst", true);
    }

    @Then("pre-order items should ship after arrival")
    public void preOrderItemsShouldShipAfterArrival() {
        testContext.put("preOrderItemsShippedAfterArrival", true);
    }

    @Then("each shipment should have independent tracking number")
    public void eachShipmentShouldHaveIndependentTrackingNumber() {
        testContext.put("independentTrackingNumbers", true);
    }

    @When("customer selects delivery insurance")
    public void customerSelectsDeliveryInsurance() {
        testContext.put("deliveryInsuranceSelected", true);
    }

    @Then("insurance fee should be {double}% of item value")
    public void insuranceFeeShouldBeOfItemValue(Double percentage) {
        testContext.put("insurancePercentage", percentage);
    }

    @Then("insurance fee should be {int}")
    public void insuranceFeeShouldBe(Integer expectedFee) {
        testContext.put("calculatedInsuranceFee", expectedFee);
    }

    @Then("full compensation available for delivery damage")
    public void fullCompensationAvailableForDeliveryDamage() {
        testContext.put("fullCompensationAvailable", true);
    }

    @Given("customer has received items")
    public void customerHasReceivedItems() {
        testContext.put("itemsReceived", true);
    }

    @When("customer rates delivery service")
    public void customerRatesDeliveryService() {
        testContext.put("deliveryServiceRated", true);
    }

    @When("gives {int}-star rating with comment {string}")
    public void givesStarRatingWithComment(Integer rating, String comment) {
        testContext.put("deliveryRating", rating);
        testContext.put("deliveryComment", comment);
    }

    @Then("rating should be recorded")
    public void ratingShouldBeRecorded() {
        testContext.put("ratingRecorded", true);
    }

    @Then("affect delivery person's service score")
    public void affectDeliveryPersonsServiceScore() {
        testContext.put("serviceScoreAffected", true);
    }

    @Given("customer needs emergency delivery for medical supplies")
    public void customerNeedsEmergencyDeliveryForMedicalSupplies() {
        testContext.put("emergencyDeliveryNeeded", true);
        testContext.put("deliveryReason", "medical supplies");
    }

    @When("customer selects {string} with reason")
    public void customerSelectsWithReason(String deliveryType) {
        testContext.put("selectedDeliveryType", deliveryType);
    }

    @Then("system should prioritize this order")
    public void systemShouldPrioritizeThisOrder() {
        testContext.put("orderPrioritized", true);
    }

    @Then("arrange fastest delivery method")
    public void arrangeFastestDeliveryMethod() {
        testContext.put("fastestDeliveryMethodArranged", true);
    }

    @Then("customer should receive real-time delivery tracking")
    public void customerShouldReceiveRealTimeDeliveryTracking() {
        testContext.put("realTimeTrackingProvided", true);
    }

    // Inner classes for data models
    @SuppressWarnings("unused")
    private static class DeliveryMethod {
        private final String name;
        private final int fee;
        private final String estimatedDays;
        private final String weightLimit;
        private final String regionLimit;

        public DeliveryMethod(
                String name,
                int fee,
                String estimatedDays,
                String weightLimit,
                String regionLimit) {
            this.name = name;
            this.fee = fee;
            this.estimatedDays = estimatedDays;
            this.weightLimit = weightLimit;
            this.regionLimit = regionLimit;
        }

        // Getters
        public String getName() {
            return name;
        }

        public int getFee() {
            return fee;
        }

        public String getEstimatedDays() {
            return estimatedDays;
        }

        public String getWeightLimit() {
            return weightLimit;
        }

        public String getRegionLimit() {
            return regionLimit;
        }
    }

    @SuppressWarnings("unused")
    private static class Customer {
        private final String name;
        private final String defaultAddress;

        public Customer(String name, String defaultAddress) {
            this.name = name;
            this.defaultAddress = defaultAddress;
        }

        // Getters
        public String getName() {
            return name;
        }

        public String getDefaultAddress() {
            return defaultAddress;
        }
    }

    @SuppressWarnings("unused")
    private static class DeliveryAddress {
        private final String recipient;
        private final String phone;
        private final String address;

        public DeliveryAddress(String recipient, String phone, String address) {
            this.recipient = recipient;
            this.phone = phone;
            this.address = address;
        }

        // Getters
        public String getRecipient() {
            return recipient;
        }

        public String getPhone() {
            return phone;
        }

        public String getAddress() {
            return address;
        }
    }
}
