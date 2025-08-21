package solid.humank.genaidemo.bdd.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 測試工具類 - 提供測試輔助方法 */
public class TestUtils {

    /** 打印測試上下文狀態 - 用於調試 */
    public static void printTestContextState(TestContext context) {
        System.out.println("=== Test Context State ===");
        System.out.println("Products: " + context.getProducts().size());
        System.out.println("Cart Items: " + context.getCartItems().size());
        System.out.println("Cart Total: " + context.getCartTotal());
        System.out.println("Add-on Rules: " + context.getAddOnRules().size());
        System.out.println("Has Error: " + context.hasError());
        if (context.hasError()) {
            System.out.println("Error Message: " + context.getLastErrorMessage());
        }
        System.out.println("VIP Member: " + context.isVipMember());
        System.out.println("Member Discount: " + context.getMemberDiscount());
        System.out.println("========================");
    }

    /** 打印購物車詳情 */
    public static void printCartDetails(TestContext context) {
        System.out.println("=== Cart Details ===");
        for (Map.Entry<String, CartItem> entry : context.getCartItems().entrySet()) {
            CartItem item = entry.getValue();
            System.out.printf(
                    "Product: %s, Name: %s, Price: %s, Quantity: %d, Type: %s%n",
                    item.getProductId(),
                    item.getProductName(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getType());
        }
        System.out.println("Total: " + context.getCartTotal());
        System.out.println("==================");
    }

    /** 打印加購選項 */
    public static void printAddOnOptions(List<AddOnOption> options) {
        System.out.println("=== Add-on Options ===");
        for (AddOnOption option : options) {
            System.out.printf(
                    "Product: %s, Name: %s, Original: %s, Add-on: %s, Savings: %s, Limit: %d%n",
                    option.getProductId(),
                    option.getProductName(),
                    option.getOriginalPrice(),
                    option.getAddOnPrice(),
                    option.getSavings(),
                    option.getLimitQuantity());
        }
        System.out.println("=====================");
    }

    /** 驗證金額相等（考慮精度問題） */
    public static boolean isAmountEqual(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null && amount2 == null) return true;
        if (amount1 == null || amount2 == null) return false;
        return amount1.compareTo(amount2) == 0;
    }

    /** 格式化金額顯示 */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    /** 計算百分比折扣 */
    public static BigDecimal calculateDiscountAmount(
            BigDecimal originalAmount, BigDecimal discountRate) {
        if (originalAmount == null || discountRate == null) return BigDecimal.ZERO;
        return originalAmount.multiply(discountRate);
    }

    /** 應用折扣後的金額 */
    public static BigDecimal applyDiscount(BigDecimal originalAmount, BigDecimal discountRate) {
        if (originalAmount == null) return BigDecimal.ZERO;
        if (discountRate == null) return originalAmount;
        return originalAmount.multiply(BigDecimal.ONE.subtract(discountRate));
    }

    /** 檢查產品 ID 格式是否正確 */
    public static boolean isValidProductId(String productId) {
        return productId != null && productId.matches("PROD-\\d{3}");
    }

    /** 生成測試用的產品 ID */
    public static String generateTestProductId(int number) {
        return String.format("PROD-%03d", number);
    }

    /** 清理測試環境 */
    public static void cleanupTestEnvironment() {
        TestContext.getInstance().clear();
    }

    /** 設置測試數據 */
    public static void setupBasicTestData() {
        TestContext context = TestContext.getInstance();
        TestDataBuilder builder = TestDataBuilder.getInstance();

        // 清理現有數據
        context.clear();

        // 創建基本測試產品
        context.addProduct(
                "PROD-001",
                builder.createTestProduct(
                        "PROD-001", "iPhone 15 Pro Max", "Phone", new BigDecimal("35900"), 50));
        context.addProduct(
                "PROD-002",
                builder.createTestProduct(
                        "PROD-002", "AirPods Pro", "Headset", new BigDecimal("8990"), 100));
        context.addProduct(
                "PROD-003",
                builder.createTestProduct(
                        "PROD-003", "iPhone Case", "Accessory", new BigDecimal("990"), 200));

        // 創建基本加購規則
        context.addAddOnRule(
                builder.createAddOnRule(
                        "PROD-001", "PROD-002", new BigDecimal("7990"), new BigDecimal("8990"), 1));
        context.addAddOnRule(
                builder.createAddOnRule(
                        "PROD-001", "PROD-003", new BigDecimal("690"), new BigDecimal("990"), 2));
    }
}
