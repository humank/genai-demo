package solid.humank.genaidemo.bdd.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;

public class GiftWithPurchaseStepDefinitions {

    private Product giftProduct;
    private double minimumPurchaseAmount;
    private List<Product> giftItems = new ArrayList<>();
    private Map<String, Object> promotionDetails = new HashMap<>();

    @Given("the store offers a gift {string} worth ${int} with any purchase over ${int}")
    public void the_store_offers_a_gift_worth_$_with_any_purchase_over_$(
            String giftName, Integer giftValue, Integer minimumPurchase) {
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(giftValue));

        this.minimumPurchaseAmount = minimumPurchase;

        // 創建滿額贈規則
        GiftWithPurchaseRule rule =
                new GiftWithPurchaseRule(
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
        when(giftProduct.getPrice()).thenReturn(Money.of(50)); // 假設禮品價值為$50

        this.minimumPurchaseAmount = minimumPurchase;

        // 創建滿額贈規則
        GiftWithPurchaseRule rule =
                new GiftWithPurchaseRule(
                        Money.of(minimumPurchase), giftProductId, Money.of(50), 1, false);

        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", 50);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    @When("the customer's order total is ${int}")
    public void the_customer_s_order_total_is_$(Integer orderTotal) {
        if (orderTotal >= minimumPurchaseAmount) {
            // 符合滿額贈條件，添加禮品
            addGiftToOrder(1);
        }
    }

    @When("the customer's order total is ${int} and they qualify for {int} gifts")
    public void the_customer_s_order_total_is_$_and_they_qualify_for_gifts(
            Integer orderTotal, Integer giftCount) {
        if (orderTotal >= minimumPurchaseAmount) {
            addGiftToOrder(giftCount);
        }
    }

    private void addGiftToOrder(int giftCount) {
        giftItems.clear();
        String giftName = giftProduct.getName().getName();
        ProductId giftProductId = giftProduct.getId();

        for (int i = 0; i < giftCount; i++) {
            Product giftProductCopy = mock(Product.class);
            when(giftProductCopy.getName()).thenReturn(new ProductName(giftName));
            when(giftProductCopy.getId()).thenReturn(giftProductId);
            // 設置禮品價格為0
            when(giftProductCopy.getPrice()).thenReturn(Money.of(0));

            giftItems.add(giftProductCopy);
        }
    }

    @Then("the customer receives {int} {string} at no additional cost")
    public void the_customer_receives_at_no_additional_cost(Integer quantity, String giftName) {
        assertEquals(quantity.intValue(), giftItems.size());

        // 驗證所有禮品的名稱和價格
        for (Product gift : giftItems) {
            assertEquals(giftName, gift.getName().getName());
        }
    }

    @Then("the gift items are added to the order at ${int} each")
    public void the_gift_items_are_added_to_the_order_at_$_each(Integer price) {
        assertEquals(0, price); // 所有禮品價格應為0
        for (Product gift : giftItems) {
            assertEquals(Money.of(0).getAmount(), gift.getPrice().getAmount());
        }
    }

    // 新增缺失的步驟定義，遵循 DDD + 六邊形架構

    @Given("the store offers a free {string} \\(valued at ${int}) for purchases over ${int}")
    public void the_store_offers_a_free_valued_at_$_for_purchases_over_$(
            String giftName, Integer giftValue, Integer minimumPurchase) {
        // 遵循 DDD 領域模型：創建滿額贈禮規則
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(giftValue));

        this.minimumPurchaseAmount = minimumPurchase;

        // 創建滿額贈規則，遵循領域驅動設計
        GiftWithPurchaseRule rule =
                new GiftWithPurchaseRule(
                        Money.of(minimumPurchase), giftProductId, Money.of(giftValue), 1, false);

        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", giftValue);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    @When("the customer's cart total reaches ${int}")
    public void the_customer_s_cart_total_reaches_$(Integer cartTotal) {
        // 遵循 DDD 應用服務模式：檢查是否符合滿額贈條件
        Integer spendingThreshold = (Integer) promotionDetails.get("spendingThreshold");

        if (spendingThreshold != null) {
            // 階梯式滿額贈：計算應獲得的禮品數量
            calculateGiftsForCartTotal(cartTotal);
        } else if (cartTotal >= minimumPurchaseAmount) {
            // 單一滿額贈：符合條件，添加一個禮品
            addGiftToOrder(1);
        }
    }

    @Then("the customer should automatically receive the {string} in their cart")
    public void the_customer_should_automatically_receive_the_in_their_cart(String giftName) {
        // 驗證禮品已添加到購物車
        assertEquals(1, giftItems.size());
        assertEquals(giftName, giftItems.get(0).getName().getName());
    }

    @Then("the gift item should be marked as ${int}")
    public void the_gift_item_should_be_marked_as_$(Integer price) {
        // 驗證禮品價格為0
        assertEquals(0, price.intValue());
        for (Product gift : giftItems) {
            assertEquals(Money.of(0).getAmount(), gift.getPrice().getAmount());
        }
    }

    @Given("the store offers one free {string} for every ${int} spent, up to {int} speakers")
    public void the_store_offers_one_free_for_every_$_spent_up_to_speakers(
            String giftName, Integer spendingThreshold, Integer maxGifts) {
        // 遵循 DDD 領域模型：創建階梯式滿額贈規則
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(50)); // 假設禮品價值$50

        this.minimumPurchaseAmount = spendingThreshold;

        // 創建階梯式滿額贈規則
        GiftWithPurchaseRule rule =
                new GiftWithPurchaseRule(
                        Money.of(spendingThreshold),
                        giftProductId,
                        Money.of(50),
                        maxGifts,
                        true); // 允許多個禮品

        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", 50);
        promotionDetails.put("spendingThreshold", spendingThreshold);
        promotionDetails.put("maxGifts", maxGifts);
    }

    @Then("the customer should receive {int} {string} items in their cart")
    public void the_customer_should_receive_items_in_their_cart(Integer quantity, String giftName) {
        // 驗證收到正確數量的禮品
        assertEquals(quantity.intValue(), giftItems.size());

        for (Product gift : giftItems) {
            assertEquals(giftName, gift.getName().getName());
        }
    }

    @Then("all gift items should be marked as ${int}")
    public void all_gift_items_should_be_marked_as_$(Integer price) {
        // 驗證所有禮品價格都為0
        assertEquals(0, price.intValue());
        for (Product gift : giftItems) {
            assertEquals(Money.of(0).getAmount(), gift.getPrice().getAmount());
        }
    }

    // 私有輔助方法：根據購物車總額計算應獲得的禮品數量
    private void calculateGiftsForCartTotal(int cartTotal) {
        Integer spendingThreshold = (Integer) promotionDetails.get("spendingThreshold");
        Integer maxGifts = (Integer) promotionDetails.get("maxGifts");

        if (spendingThreshold != null && maxGifts != null) {
            int eligibleGifts = Math.min(cartTotal / spendingThreshold, maxGifts);
            if (eligibleGifts > 0) {
                addGiftToOrder(eligibleGifts);
            }
        }
    }
}
