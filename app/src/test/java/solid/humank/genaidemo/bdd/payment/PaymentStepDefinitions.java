package solid.humank.genaidemo.bdd.payment;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付聚合根的 Cucumber 步驟定義
 */
public class PaymentStepDefinitions {

    private UUID orderId;
    private Payment payment;
    private Exception thrownException;

    @Given("建立新的訂單ID")
    public void createNewOrderId() {
        orderId = UUID.randomUUID();
        assertNotNull(orderId);
    }

    @When("建立支付")
    public void createPayment(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        int amount = Integer.parseInt(data.get("金額"));
        payment = new Payment(orderId, Money.twd(amount));
        assertNotNull(payment);
    }

    @When("設定支付方式為 {string}")
    public void setPaymentMethod(String method) {
        payment.setPaymentMethod(PaymentMethod.valueOf(method));
    }

    @When("完成支付處理")
    public void completePayment(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String transactionId = data.get("交易ID");
        try {
            payment.complete(transactionId);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("支付處理失敗")
    public void failPayment(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String reason = data.get("失敗原因");
        payment.fail(reason);
    }

    @When("申請退款")
    public void refundPayment() {
        try {
            payment.refund();
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @When("支付處理超時")
    public void timeoutPayment() {
        payment.timeout();
    }

    @When("重試支付")
    public void retryPayment() {
        payment.retry();
    }

    @Then("支付應該成功建立")
    public void paymentShouldBeCreated() {
        assertNotNull(payment);
        assertNotNull(payment.getId());
    }

    @Then("支付狀態應為 {string}")
    public void paymentStatusShouldBe(String status) {
        assertEquals(PaymentStatus.valueOf(status), payment.getStatus());
    }

    @Then("支付金額應為 {int}")
    public void paymentAmountShouldBe(int amount) {
        assertEquals(BigDecimal.valueOf(amount), payment.getAmount().getAmount());
    }

    @Then("支付方式應為 {string}")
    public void paymentMethodShouldBe(String method) {
        assertEquals(PaymentMethod.valueOf(method), payment.getPaymentMethod());
    }

    @Then("交易ID應為 {string}")
    public void transactionIdShouldBe(String transactionId) {
        assertEquals(transactionId, payment.getTransactionId());
    }

    @Then("失敗原因應為 {string}")
    public void failureReasonShouldBe(String reason) {
        assertEquals(reason, payment.getFailureReason());
    }

    @Then("支付應可重試")
    public void paymentShouldBeRetryable() {
        assertTrue(payment.canRetry());
    }

    @Then("應拋出支付相關異常 {string}")
    public void shouldThrowPaymentExceptionWithMessage(String errorMessage) {
        assertNotNull(thrownException, "Expected exception was not thrown");
        assertTrue(thrownException.getMessage().contains(errorMessage), 
                "Expected error message to contain: " + errorMessage + 
                ", but was: " + thrownException.getMessage());
    }
}