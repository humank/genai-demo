package solid.humank.genaidemo.domain.customer.model.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 測試 Customer 聚合根的混搭方案實作
 * 驗證 Annotation + Interface Default Methods 的正確性
 */
class CustomerAggregateRootTest {

    @BeforeEach
    void setUp() {
        // 清除所有事件收集器狀態，確保測試隔離
        solid.humank.genaidemo.domain.common.aggregate.AggregateRootEventCollectorHolder.clearAllEventCollectors();
    }

    @Test
    void testCustomerImplementsAggregateRoot() {
        // Given
        Customer customer = createTestCustomer();

        // When & Then - 驗證 AggregateRoot 介面方法可用
        assertNotNull(customer.getAggregateRootName());
        assertEquals("Customer", customer.getAggregateRootName());
        assertEquals("Customer", customer.getBoundedContext());
        assertEquals("2.0", customer.getVersion());

        // 驗證事件管理功能 - 創建客戶時應該有 CustomerCreatedEvent
        assertTrue(customer.hasUncommittedEvents());

        // 調試信息：打印實際的事件數量和事件類型
        System.out.println("實際事件數量: " + customer.getUncommittedEvents().size());
        customer.getUncommittedEvents()
                .forEach(event -> System.out.println("事件類型: " + event.getClass().getSimpleName()));

        // 驗證只有一個 CustomerCreatedEvent
        assertEquals(1, customer.getUncommittedEvents().size());
        assertEquals("CustomerCreatedEvent", customer.getUncommittedEvents().get(0).getClass().getSimpleName());
    }

    @Test
    void testEventCollectionWorks() {
        // Given
        Customer customer = createTestCustomer();

        // When - 執行業務操作，應該會收集事件
        customer.updateProfile(
                new CustomerName("Updated Name"),
                new Email("updated@example.com"),
                new Phone("0987654321"));

        // Then - 驗證事件被正確收集
        assertTrue(customer.hasUncommittedEvents());
        assertTrue(customer.getUncommittedEvents().size() > 0);

        // 清除事件
        customer.markEventsAsCommitted();
        assertFalse(customer.hasUncommittedEvents());
    }

    @Test
    void testAnnotationValidation() {
        // Given
        Customer customer = createTestCustomer();

        // When & Then - 驗證註解資訊正確讀取
        assertEquals("Customer", customer.getAggregateRootName());
        assertEquals("Customer", customer.getBoundedContext());
        assertEquals("2.0", customer.getVersion());

        // 驗證註解描述（如果需要的話，可以添加 getDescription 方法）
        // 清除現有事件，然後測試手動添加事件
        customer.markEventsAsCommitted();
        assertDoesNotThrow(() -> {
            customer.collectEvent(solid.humank.genaidemo.domain.customer.model.events.CustomerCreatedEvent.create(
                    customer.getId(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getMembershipLevel()));
        });
    }

    private Customer createTestCustomer() {
        return new Customer(
                CustomerId.of("test-customer-id"),
                new CustomerName("Test Customer"),
                new Email("test@example.com"),
                new Phone("0912345678"),
                new Address("信義路一段100號", "台北市信義區", "110", "台灣"),
                MembershipLevel.SILVER,
                LocalDate.of(1990, 1, 1),
                LocalDateTime.now());
    }
}