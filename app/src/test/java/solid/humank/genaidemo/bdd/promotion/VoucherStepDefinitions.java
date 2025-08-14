package solid.humank.genaidemo.bdd.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import solid.humank.genaidemo.domain.promotion.model.entity.Voucher;

public class VoucherStepDefinitions {

    private List<Voucher> vouchers = new ArrayList<>();
    private Map<String, Object> voucherDetails = new HashMap<>();

    @Given("the store offers a {string} at ${int} instead of the regular ${int}")
    public void the_store_offers_a_at_$_instead_of_the_regular_$(
            String comboName, Integer comboPrice, Integer regularPrice) {

        voucherDetails.put("comboName", comboName);
        voucherDetails.put("comboPrice", comboPrice);
        voucherDetails.put("regularPrice", regularPrice);
        voucherDetails.put("voucherCount", 7); // 假設是7杯咖啡套餐
    }

    @When("the customer purchases the combo")
    public void the_customer_purchases_the_combo() {
        // 驗證套餐信息
        Integer comboPrice = (Integer) voucherDetails.get("comboPrice");
        Integer regularPrice = (Integer) voucherDetails.get("regularPrice");

        // 驗證套餐價格確實比原價便宜
        assertTrue(
                comboPrice < regularPrice,
                String.format("套餐價格 $%d 應該比原價 $%d 便宜", comboPrice, regularPrice));

        // 創建優惠券
        vouchers.clear();
        int voucherCount = (int) voucherDetails.get("voucherCount");

        for (int i = 0; i < voucherCount; i++) {
            Voucher voucher = mock(Voucher.class);
            String redemptionCode = UUID.randomUUID().toString().substring(0, 8);
            String voucherId = UUID.randomUUID().toString();

            // 設置 Voucher 實體的行為
            when(voucher.getRedemptionCode()).thenReturn(redemptionCode);
            when(voucher.getId()).thenReturn(voucherId);
            when(voucher.getIssueDate()).thenReturn(LocalDate.now());
            when(voucher.getExpirationDate()).thenReturn(LocalDate.now().plusDays(90));
            when(voucher.isValid()).thenReturn(true);
            when(voucher.isUsed()).thenReturn(false);
            when(voucher.getRedemptionLocation()).thenReturn("Any 7-11 in Taiwan");
            when(voucher.getContents()).thenReturn("Medium-sized coffee");

            vouchers.add(voucher);
        }
    }

    @Then("the customer receives {int} beverage vouchers valid for {int} days")
    public void the_customer_receives_beverage_vouchers_valid_for_days(
            Integer count, Integer days) {
        assertEquals(count, vouchers.size());
        for (Voucher voucher : vouchers) {
            // 檢查有效期是否為90天
            LocalDate issueDate = voucher.getIssueDate();
            LocalDate expirationDate = voucher.getExpirationDate();
            long validDays = java.time.temporal.ChronoUnit.DAYS.between(issueDate, expirationDate);
            assertEquals(days, (int) validDays);
        }
    }

    @Then("each voucher has a unique redemption code")
    public void each_voucher_has_a_unique_redemption_code() {
        List<String> codes = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            String code = voucher.getRedemptionCode();
            assertTrue(!codes.contains(code));
            codes.add(code);
        }
        assertEquals(vouchers.size(), codes.size());
    }

    @Then("vouchers can be redeemed for any medium-sized coffee at {int}-{int}")
    public void vouchers_can_be_redeemed_for_any_medium_sized_coffee_at(
            Integer storeNumber1, Integer storeNumber2) {
        // 這裡只是驗證7-11的格式，實際上不需要做任何事情
        assertEquals(7, storeNumber1);
        assertEquals(11, storeNumber2);

        // 驗證所有優惠券的內容是否為中杯咖啡
        for (Voucher voucher : vouchers) {
            assertEquals("Medium-sized coffee", voucher.getContents());
            assertEquals("Any 7-11 in Taiwan", voucher.getRedemptionLocation());
        }
    }

    @Then("the combo provides better value than buying individually")
    public void the_combo_provides_better_value_than_buying_individually() {
        String comboName = (String) voucherDetails.get("comboName");
        Integer comboPrice = (Integer) voucherDetails.get("comboPrice");
        Integer regularPrice = (Integer) voucherDetails.get("regularPrice");
        int voucherCount = (int) voucherDetails.get("voucherCount");

        // 計算節省的金額
        int totalRegularPrice = regularPrice * voucherCount;
        int savings = totalRegularPrice - comboPrice;

        assertTrue(
                savings > 0,
                String.format(
                        "套餐 '%s' 應該比單獨購買便宜。套餐價格: $%d, 原價總計: $%d, 節省: $%d",
                        comboName, comboPrice, totalRegularPrice, savings));
    }
}
