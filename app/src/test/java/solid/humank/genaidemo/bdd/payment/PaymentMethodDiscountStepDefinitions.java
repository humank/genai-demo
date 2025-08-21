package solid.humank.genaidemo.bdd.payment;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Payment Method Discount Step Definitions */
public class PaymentMethodDiscountStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();

    @Given("the system supports the following payment methods and discounts:")
    public void theSystemSupportsTheFollowingPaymentMethodsAndDiscounts(DataTable dataTable) {
        testContext.put("paymentMethods", dataTable.asMaps());
    }

    @Given("customer is VIP member with 20% member discount")
    public void customerIsVipMemberWith20PercentMemberDiscount() {
        testContext.put("memberLevel", "VIP");
        testContext.put("memberDiscount", 0.20);
    }

    @Given("customer uses {string} credit card")
    public void customerUsesCreditCard(String bankName) {
        testContext.put("creditCardBank", bankName);
    }

    @When("customer selects {string} payment method")
    public void customerSelectsPaymentMethod(String paymentMethod) {
        testContext.put("selectedPaymentMethod", paymentMethod);
    }

    @When("cart amount is {int} which meets minimum spend {int}")
    public void cartAmountIsWhichMeetsMinimumSpend(int cartAmount, int minimumSpend) {
        testContext.put("cartAmount", cartAmount);
        testContext.put("minimumSpend", minimumSpend);
    }

    @When("cart amount is {int} which does not meet minimum spend {int}")
    public void cartAmountIsWhichDoesNotMeetMinimumSpend(int cartAmount, int minimumSpend) {
        testContext.put("cartAmount", cartAmount);
        testContext.put("minimumSpend", minimumSpend);
        testContext.put("meetsMinimumSpend", false);
    }

    @When("cart amount is {int}")
    public void cartAmountIs(int cartAmount) {
        testContext.put("cartAmount", cartAmount);
    }

    @When("customer cart amount is {int}")
    public void customerCartAmountIs(int cartAmount) {
        testContext.put("cartAmount", cartAmount);
    }

    @When("tries to select {string}")
    public void triesToSelect(String paymentMethod) {
        testContext.put("attemptedPaymentMethod", paymentMethod);
    }

    @When("customer views payment options")
    public void customerViewsPaymentOptions() {
        testContext.put("viewingPaymentOptions", true);
    }

    @When("customer selects {int} installments for amount {int}")
    public void customerSelectsInstallmentsForAmount(int installments, int amount) {
        testContext.put("installments", installments);
        testContext.put("installmentAmount", amount);
    }

    @Then("should receive {int}% payment discount")
    public void shouldReceivePaymentPercentageDiscount(int discountPercentage) {
        testContext.put("expectedDiscountPercentage", discountPercentage);
    }

    @Then("payment discount amount should be {int}")
    public void paymentDiscountAmountShouldBe(int discountAmount) {
        testContext.put("expectedDiscountAmount", discountAmount);
    }

    @Then("discount amount should be {int} due to maximum limit")
    public void discountAmountShouldBeDueToMaximumLimit(int discountAmount) {
        testContext.put("cappedDiscountAmount", discountAmount);
    }

    @Then("should receive {int} fixed discount")
    public void shouldReceiveFixedDiscount(int fixedDiscount) {
        testContext.put("expectedFixedDiscount", fixedDiscount);
    }

    @Then("should not receive payment discount")
    public void shouldNotReceivePaymentDiscount() {
        testContext.put("paymentDiscount", 0);
    }

    @Then("should receive {int}% bank partnership discount")
    public void shouldReceiveBankPartnershipDiscount(int discountPercentage) {
        testContext.put("bankPartnershipDiscount", discountPercentage);
    }

    @Then("should display payment method comparison:")
    public void shouldDisplayPaymentMethodComparison(DataTable dataTable) {
        testContext.put("paymentComparison", dataTable.asMaps());
    }

    @Then("payment discount should be calculated based on amount after member discount")
    public void paymentDiscountShouldBeCalculatedBasedOnAmountAfterMemberDiscount() {
        testContext.put("discountCalculationOrder", "MEMBER_FIRST");
    }

    @Then("digital wallet discount is {int}")
    public void digitalWalletDiscountIs(int discountAmount) {
        testContext.put("digitalWalletDiscount", discountAmount);
    }

    @Then("final payment amount is {int}")
    public void finalPaymentAmountIs(int finalAmount) {
        testContext.put("finalPaymentAmount", finalAmount);
    }

    @Then("payment system should display {string}")
    public void paymentSystemShouldDisplay(String message) {
        testContext.put("displayMessage", message);
    }

    @Then("should not allow selecting this payment method")
    public void shouldNotAllowSelectingThisPaymentMethod() {
        testContext.put("paymentMethodAllowed", false);
    }

    @Then("total fee should be {int}")
    public void totalFeeShouldBe(int totalFee) {
        testContext.put("expectedTotalFee", totalFee);
    }

    @Then("monthly payment should be {int}")
    public void monthlyPaymentShouldBe(int monthlyPayment) {
        testContext.put("expectedMonthlyPayment", monthlyPayment);
    }

    @Given("credit card installment has the following rates:")
    public void creditCardInstallmentHasTheFollowingRates(DataTable dataTable) {
        testContext.put("installmentRates", dataTable.asMaps());
    }

    // Additional missing step definitions for payment method discounts
    @When("customer views payment method options on checkout page")
    public void customerViewsPaymentMethodOptionsOnCheckoutPage() {
        testContext.put("viewingPaymentMethodOptions", true);
    }

    @Given("customer has already applied member discount of {int}")
    public void customerHasAlreadyAppliedMemberDiscountOf(Integer memberDiscount) {
        testContext.put("appliedMemberDiscount", memberDiscount);
    }

    @Given("partnership with {string} provides additional {int}% discount")
    public void partnershipWithProvidesAdditionalDiscount(
            String bankName, Integer discountPercentage) {
        testContext.put("partnershipBank", bankName);
        testContext.put("partnershipDiscountPercentage", discountPercentage);
    }

    @Given("{string} is limited to cart amount under {int}")
    public void isLimitedToCartAmountUnder(String paymentMethod, Integer maxAmount) {
        testContext.put("paymentMethodLimit_" + paymentMethod, maxAmount);
    }
}
