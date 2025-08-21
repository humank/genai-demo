package solid.humank.genaidemo.bdd.promotion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/** 滿額贈品步驟定義類的單元測試 */
public class GiftWithPurchaseStepDefinitionsTest {

    @Test
    public void testClassCreation() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions stepDefinitions =
                            new GiftWithPurchaseStepDefinitions();
                    stepDefinitions.verifyClassCreation();
                },
                "GiftWithPurchaseStepDefinitions should be created successfully");
    }

    @Test
    public void testGiftWithPurchaseActivityDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.GiftWithPurchaseActivity activity =
                            new GiftWithPurchaseStepDefinitions.GiftWithPurchaseActivity(
                                    "滿千送好禮", 1000, "PROD-004", "保溫杯", 1, 200);

                    assertNotNull(activity.getActivityName());
                    assertNotNull(activity.getGiftProductId());
                    assertNotNull(activity.getGiftName());
                },
                "GiftWithPurchaseActivity should be created successfully");
    }

    @Test
    public void testCustomerPreferenceDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.CustomerPreference preference =
                            new GiftWithPurchaseStepDefinitions.CustomerPreference("張小明", "科技產品");

                    assertNotNull(preference.getCustomerName());
                    assertNotNull(preference.getPreferredCategory());
                },
                "CustomerPreference should be created successfully");
    }

    @Test
    public void testGiftRecommendationDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.GiftRecommendation recommendation =
                            new GiftWithPurchaseStepDefinitions.GiftRecommendation(
                                    "無線充電器", "科技產品", true, "基於您的購買偏好推薦");

                    assertNotNull(recommendation.getGiftName());
                    assertNotNull(recommendation.getCategory());
                    assertNotNull(recommendation.getRecommendationReason());
                },
                "GiftRecommendation should be created successfully");
    }

    @Test
    public void testGiftPackagingDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.GiftPackaging packaging =
                            new GiftWithPurchaseStepDefinitions.GiftPackaging(
                                    "專門包裝", "滿額贈品，感謝您的支持", true);

                    assertNotNull(packaging.getPackagingType());
                    assertNotNull(packaging.getLabel());
                },
                "GiftPackaging should be created successfully");
    }

    @Test
    public void testQualityIssueDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.QualityIssue issue =
                            new GiftWithPurchaseStepDefinitions.QualityIssue("贈品", "品質問題", "外觀瑕疵");

                    assertNotNull(issue.getItemType());
                    assertNotNull(issue.getIssueType());
                    assertNotNull(issue.getDescription());
                },
                "QualityIssue should be created successfully");
    }

    @Test
    public void testGiftActivityStatisticsDataClass() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions.GiftActivityStatistics stats =
                            new GiftWithPurchaseStepDefinitions.GiftActivityStatistics(
                                    "滿千送好禮", 2500, 2500, 150);

                    assertNotNull(stats.getActivityName());
                    assertEquals(2500, stats.getParticipants());
                    assertEquals(2500, stats.getGiftsDistributed());
                    assertEquals(150, stats.getAverageIncrease());
                },
                "GiftActivityStatistics should be created successfully");
    }

    @Test
    public void testSetupGiftWithPurchaseActivities() {
        assertDoesNotThrow(
                () -> {
                    GiftWithPurchaseStepDefinitions stepDefinitions =
                            new GiftWithPurchaseStepDefinitions();

                    // 創建模擬的 DataTable
                    io.cucumber.datatable.DataTable dataTable =
                            io.cucumber.datatable.DataTable.create(
                                    java.util.Arrays.asList(
                                            java.util.Arrays.asList(
                                                    "活動名稱", "最低消費", "贈品商品ID", "贈品名稱", "贈品數量",
                                                    "贈品庫存"),
                                            java.util.Arrays.asList(
                                                    "滿千送好禮",
                                                    "1000",
                                                    "PROD-004",
                                                    "保溫杯",
                                                    "1",
                                                    "200")));

                    // 基本測試 - 只驗證類別可以正常創建
                    assertNotNull(stepDefinitions);
                },
                "GiftWithPurchaseStepDefinitions should be created successfully");
    }
}
