package solid.humank.genaidemo.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import solid.humank.genaidemo.application.stats.StatsDto;
import solid.humank.genaidemo.application.stats.service.StatsApplicationService;

/** JPA 重構測試 驗證從原生 SQL 重構為 JPA 後功能正常 */
@SpringBootTest
@ActiveProfiles("test")
public class JpaRefactoringTest {

    @Autowired private StatsApplicationService statsApplicationService;

    @Autowired private CustomerApplicationService customerApplicationService;

    @Test
    public void testStatsServiceWithJpa() {
        // 測試基本統計功能
        StatsDto stats = statsApplicationService.getStats();
        assertNotNull(stats);
        assertEquals("success", stats.status());
        assertNotNull(stats.stats());

        // 驗證統計數據包含必要的字段
        assertTrue(stats.stats().containsKey("totalOrders"));
        assertTrue(stats.stats().containsKey("totalOrderItems"));
        assertTrue(stats.stats().containsKey("totalPayments"));
        assertTrue(stats.stats().containsKey("totalInventories"));
        assertTrue(stats.stats().containsKey("totalReservations"));
        assertTrue(stats.stats().containsKey("totalRecords"));
        assertTrue(stats.stats().containsKey("uniqueCustomers"));
    }

    @Test
    public void testOrderStatusStatsWithJpa() {
        // 測試訂單狀態統計
        OrderStatusStatsDto orderStatusStats = statsApplicationService.getOrderStatusStats();
        assertNotNull(orderStatusStats);
        assertEquals("success", orderStatusStats.status());
        assertNotNull(orderStatusStats.statusDistribution());
    }

    @Test
    public void testPaymentMethodStatsWithJpa() {
        // 測試支付方式統計
        PaymentMethodStatsDto paymentMethodStats = statsApplicationService.getPaymentMethodStats();
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
