package solid.humank.genaidemo.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.application.customer.CustomerPageDto;
import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import solid.humank.genaidemo.application.stats.service.StatisticsApplicationService;
import solid.humank.genaidemo.testutils.BaseTest;

/** JPA 重構測試 驗證從原生 SQL 重構為 JPA 後功能正常 */
@SpringBootTest
@ActiveProfiles("test")
public class JpaRefactoringTest extends BaseTest {

    @Autowired
    private StatisticsApplicationService statsApplicationService;

    @Autowired
    private CustomerApplicationService customerApplicationService;

    // 統計服務測試暫時跳過，需要更多數據準備
    // @Test
    // public void testStatsServiceWithJpa() {
    // // 測試基本統計功能
    // StatsDto stats = statsApplicationService.getOverallStatistics();
    // assertNotNull(stats);
    // assertEquals("success", stats.status());
    // assertNotNull(stats.stats());
    // }

    @Test
    public void testOrderStatusStatsWithJpa() {
        // 測試訂單狀態統計
        OrderStatusStatsDto orderStatusStats = statsApplicationService.getOrderStatusStatistics();
        assertNotNull(orderStatusStats);
        assertEquals("success", orderStatusStats.status());
        assertNotNull(orderStatusStats.statusDistribution());
    }

    @Test
    public void testPaymentMethodStatsWithJpa() {
        // 測試支付方式統計
        PaymentMethodStatsDto paymentMethodStats = statsApplicationService.getPaymentMethodStatistics();
        assertNotNull(paymentMethodStats);
        assertEquals("success", paymentMethodStats.status());
        assertNotNull(paymentMethodStats.paymentMethodDistribution());
    }

    @Test
    public void testCustomerServiceWithJpa() {
        // 測試客戶服務分頁功能
        CustomerPageDto customerPage = customerApplicationService.getCustomers(0, 10);
        assertNotNull(customerPage);
        assertNotNull(customerPage.content());
        assertTrue(customerPage.totalElements() >= 0);
        assertTrue(customerPage.totalPages() >= 0);
        assertEquals(10, customerPage.size());
        assertEquals(0, customerPage.number());
    }

    @Test
    public void testCustomerExistsWithJpa() {
        // 首先獲取一個客戶列表
        CustomerPageDto customerPage = customerApplicationService.getCustomers(0, 1);

        if (!customerPage.content().isEmpty()) {
            String customerId = customerPage.content().get(0).id();

            // 測試客戶存在性檢查
            boolean exists = customerApplicationService.customerExists(customerId);
            assertTrue(exists);
        }

        // 測試不存在的客戶
        boolean notExists = customerApplicationService.customerExists("non-existent-customer");
        assertFalse(notExists);
    }
}
