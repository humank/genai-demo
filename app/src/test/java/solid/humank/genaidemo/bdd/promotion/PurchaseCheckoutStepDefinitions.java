package solid.humank.genaidemo.bdd.promotion;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.cucumber.java.en.When;
import java.math.BigDecimal;
import solid.humank.genaidemo.bdd.common.CartItem;
import solid.humank.genaidemo.bdd.common.TestContext;
import solid.humank.genaidemo.bdd.common.TestDataBuilder;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;

/** 購買和結算相關步驟定義 處理購買操作、購物車總額管理、折扣計算、促銷效果驗證和促銷提示 */
public class PurchaseCheckoutStepDefinitions {

    private TestContext testContext;
    private TestDataBuilder dataBuilder;

    // 購買和結算相關狀態
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private String lastPromptMessage;
    private int chargedUnits = 0;
    private boolean halfPriceApplied = false;

    public PurchaseCheckoutStepDefinitions() {
        this.testContext = TestContext.getInstance();
        this.dataBuilder = TestDataBuilder.getInstance();
    }

    // ===== 15.1 購買操作步驟定義 =====

    // 移除重複的步驟定義 - 已在 PromotionManagementStepDefinitions 中定義
    // @When("customer purchases {int} units of {string}") - 重複步驟定義已移除

    @When("customer purchases {int} phones")
    public void customer_purchases_phones(int quantity) {
        // 查找手機類別的產品
        Product phoneProduct = null;
        for (Product product : testContext.getProducts().values()) {
            // 假設手機產品ID包含 "PROD-001" 或 "PROD-002" (根據feature文件)
            if (product.getId().getId().equals("PROD-001")
                    || product.getId().getId().equals("PROD-002")) {
                phoneProduct = product;
                break;
            }
        }

        assertNotNull(phoneProduct, "Should have phone products available");

        // 執行購買操作 - 直接實現邏輯
        CartItem existingItem = testContext.getCartItem(phoneProduct.getId().getId());
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem cartItem =
                    dataBuilder.createCartItem(
                            phoneProduct.getId().getId(),
                            phoneProduct.getName().getName(),
                            phoneProduct.getPrice().getAmount(),
                            quantity);
            testContext.addCartItem(phoneProduct.getId().getId(), cartItem);
        }

        this.chargedUnits = quantity;
        testContext.setLastOperationResult("購買 " + quantity + " 個手機");
    }

    // 移除重複的步驟定義 - 已在 PromotionManagementStepDefinitions 中定義
    // @When("customer purchases {int} phones:") - 重複步驟定義已移除

    // ===== 15.2 購物車總額管理步驟定義 =====

    // 移除重複的步驟定義 - 已在 PromotionManagementStepDefinitions 中定義
    // @When("customer cart total is {int}") - 重複步驟定義已移除

    // Cart total verification is now handled by CommonCartStepDefinitions

    // ===== 15.3 折扣計算步驟定義 =====

    // Note: "should receive {int}% discount" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // Note: "discount amount should be {int}" and "final amount should be {int}"
    // step definitions
    // moved to PromotionManagementStepDefinitions to avoid duplicates

    // ===== 15.4 促銷效果驗證步驟定義 =====

    // Note: "should only charge for {int} units" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // Note: "lower priced item should get half price" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // Note: "{string} price should be {int}" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // ===== 15.5 促銷提示和階梯折扣步驟定義 =====

    // Note: "should prompt {string}" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // Note: "should receive {int} discount for {int} tier" step definition moved to
    // PromotionManagementStepDefinitions to avoid duplicates

    // ===== 輔助方法 =====

    /** 計算購物車總額（包含促銷折扣） */
    private BigDecimal calculateCartTotalWithPromotions() {
        BigDecimal total =
                testContext.getCartItems().values().stream()
                        .map(CartItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 應用折扣
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discountAmount);
        }

        return total;
    }

    /** 重置促銷狀態 */
    private void resetPromotionState() {
        this.discountPercentage = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.lastPromptMessage = null;
        this.chargedUnits = 0;
        this.halfPriceApplied = false;
    }
}
