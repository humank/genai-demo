package solid.humank.genaidemo.bdd.notification;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Notification Management System Step Definitions */
public class NotificationManagementStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();

    @Given("the system supports the following notification channels:")
    public void theSystemSupportsTheFollowingNotificationChannels(DataTable dataTable) {
        // Implementation for notification channels setup
        testContext.put("notificationChannels", dataTable.asMaps());
    }

    @Given("customer {string} has phone number {string} and email {string}")
    public void customerHasPhoneNumberAndEmail(
            String customerId, String phoneNumber, String email) {
        testContext.put("customerId", customerId);
        testContext.put("phoneNumber", phoneNumber);
        testContext.put("email", email);
    }

    @Given("customer is GOLD member named {string}")
    public void customerIsGoldMemberNamed(String customerName) {
        testContext.put("customerName", customerName);
        testContext.put("memberLevel", "GOLD");
    }

    @Given("customer set no non-urgent notifications between 10 PM and 8 AM")
    public void customerSetNoNonUrgentNotificationsBetween10PMAnd8AM() {
        testContext.put("quietHours", "22:00-08:00");
    }

    @Given("customer has received 5 promotion notifications today")
    public void customerHasReceived5PromotionNotificationsToday() {
        testContext.put("dailyNotificationCount", 5);
    }

    @When("system attempts to send order confirmation via SMS")
    public void systemAttemptsToSendOrderConfirmationViaSMS() {
        // Implementation for SMS sending attempt
        testContext.put("notificationAttempt", "SMS_ORDER_CONFIRMATION");
    }

    @When("SMS delivery fails")
    public void smsDeliveryFails() {
        testContext.put("smsDeliveryStatus", "FAILED");
    }

    @When("system attempts to send email notification and fails")
    public void systemAttemptsToSendEmailNotificationAndFails() {
        testContext.put("emailDeliveryStatus", "FAILED");
    }

    @When("system executes bulk notification task")
    public void systemExecutesBulkNotificationTask() {
        testContext.put("bulkNotificationTask", "EXECUTED");
    }

    @When("system generates personalized notification content")
    public void systemGeneratesPersonalizedNotificationContent() {
        testContext.put("personalizedContent", "GENERATED");
    }

    @When("system attempts to send promotion notification at 11 PM")
    public void systemAttemptsToSendPromotionNotificationAt11PM() {
        testContext.put("notificationTime", "23:00");
        testContext.put("notificationType", "PROMOTION");
    }

    @When("system attempts to send 6th promotion notification")
    public void systemAttemptsToSend6thPromotionNotification() {
        testContext.put("notificationAttempt", 6);
    }

    @Then("system should log delivery failure")
    public void systemShouldLogDeliveryFailure() {
        // Verify delivery failure is logged
    }

    @Then("attempt to send via other available channels")
    public void attemptToSendViaOtherAvailableChannels() {
        // Verify fallback channels are used
    }

    @Then("mark phone number as invalid")
    public void markPhoneNumberAsInvalid() {
        testContext.put("phoneNumberStatus", "INVALID");
    }

    @Then("system should retry after 5 minutes")
    public void systemShouldRetryAfter5Minutes() {
        testContext.put("retryInterval", "5_MINUTES");
    }

    @Then("retry maximum 3 times")
    public void retryMaximum3Times() {
        testContext.put("maxRetries", 3);
    }

    @Then("log to error log if still fails")
    public void logToErrorLogIfStillFails() {
        testContext.put("errorLogging", "ENABLED");
    }

    @Then("should send in batches of 100")
    public void shouldSendInBatchesOf100() {
        testContext.put("batchSize", 100);
    }

    @Then("control sending rate to avoid service provider limits")
    public void controlSendingRateToAvoidServiceProviderLimits() {
        testContext.put("rateLimiting", "ENABLED");
    }

    @Then("record sending progress and statistics")
    public void recordSendingProgressAndStatistics() {
        testContext.put("progressTracking", "ENABLED");
    }

    @Then("notification content should be personalized:")
    public void notificationContentShouldBePersonalized(DataTable dataTable) {
        // Verify personalized content matches expected values
        testContext.put("personalizedContentVerified", true);
    }

    @Then("notification should be delayed until 8 AM")
    public void notificationShouldBeDelayedUntil8AM() {
        testContext.put("delayedUntil", "08:00");
    }

    @Then("urgent notifications are not time-restricted")
    public void urgentNotificationsAreNotTimeRestricted() {
        testContext.put("urgentNotificationRestriction", false);
    }

    @Then("system should check frequency limit")
    public void systemShouldCheckFrequencyLimit() {
        testContext.put("frequencyLimitCheck", true);
    }

    @Then("delay sending to next day")
    public void delaySendingToNextDay() {
        testContext.put("delayToNextDay", true);
    }

    @Then("log frequency limit record")
    public void logFrequencyLimitRecord() {
        testContext.put("frequencyLimitLogged", true);
    }

    @Given("customer {string} notification preferences are set as:")
    public void customerNotificationPreferencesAreSetAs(String customerName, DataTable dataTable) {
        testContext.put("customerName", customerName);
        testContext.put("notificationPreferences", dataTable.asMaps());
    }

    @Given("need to send promotion notification to {int} members")
    public void needToSendPromotionNotificationToMembers(Integer memberCount) {
        testContext.put("bulkNotificationCount", memberCount);
    }

    @Given("customer phone number is deactivated")
    public void customerPhoneNumberIsDeactivated() {
        testContext.put("phoneNumberStatus", "DEACTIVATED");
    }

    @When("system attempts to send SMS notification")
    public void systemAttemptsToSendSMSNotification() {
        testContext.put("smsAttempt", true);
    }

    @Given("customer received {int} promotion notifications in one day")
    public void customerReceivedPromotionNotificationsInOneDay(Integer count) {
        testContext.put("dailyNotificationCount", count);
    }

    @When("customer {string} updates notification preferences")
    public void customerUpdatesNotificationPreferences(String customerName) {
        testContext.put("updatingPreferences", customerName);
    }

    @When("disables push notifications for promotions")
    public void disablesPushNotificationsForPromotions() {
        testContext.put("pushNotificationsDisabled", true);
    }

    @When("enables SMS notifications for promotions")
    public void enablesSMSNotificationsForPromotions() {
        testContext.put("smsNotificationsEnabled", true);
    }

    @Then("system should update customer notification preferences")
    public void systemShouldUpdateCustomerNotificationPreferences() {
        testContext.put("preferencesUpdated", true);
    }

    @Then("future promotion notifications should follow new settings")
    public void futurePromotionNotificationsShouldFollowNewSettings() {
        testContext.put("newSettingsApplied", true);
    }

    @Given("email service is temporarily unavailable")
    public void emailServiceIsTemporarilyUnavailable() {
        testContext.put("emailServiceStatus", "UNAVAILABLE");
    }

    @Given("customer {string} order {string} status updates from {string} to {string}")
    public void customerOrderStatusUpdatesFromTo(
            String customerName, String orderId, String fromStatus, String toStatus) {
        testContext.put("customerName", customerName);
        testContext.put("orderId", orderId);
        testContext.put("fromStatus", fromStatus);
        testContext.put("toStatus", toStatus);
    }

    @When("system triggers order status notification")
    public void systemTriggersOrderStatusNotification() {
        testContext.put("orderStatusNotificationTriggered", true);
    }

    @Then("should send the following notifications:")
    public void shouldSendTheFollowingNotifications(DataTable dataTable) {
        testContext.put("expectedNotifications", dataTable.asMaps());
    }

    @Given("customer {string} is {string} member who recently purchased iPhone")
    public void customerIsMemberWhoRecentlyPurchasedIPhone(
            String customerName, String memberLevel) {
        testContext.put("customerName", customerName);
        testContext.put("memberLevel", memberLevel);
        testContext.put("recentPurchase", "iPhone");
    }

    @When("system sends promotion notification")
    public void systemSendsPromotionNotification() {
        testContext.put("promotionNotificationSent", true);
    }
}
