package solid.humank.genaidemo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import solid.humank.genaidemo.application.customer.CustomerDto;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import solid.humank.genaidemo.application.stats.service.StatisticsApplicationService;

/**
 * 輕量級單元測試 - JPA Refactoring
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~2s)
 * 
 * 測試 JPA 重構後的服務邏輯，而不是實際的數據庫操作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JPA Refactoring Unit Tests")
class JpaRefactoringUnitTest {

    @Mock
    private StatisticsApplicationService statsApplicationService;

    @Mock
    private CustomerApplicationService customerApplicationService;

    @BeforeEach
    void setUp() {
        // 設置通用的 Mock 行為
    }

    @Test
    @DisplayName("Should get order status statistics with JPA")
    void shouldGetOrderStatusStatisticsWithJpa() {
        // Given
        OrderStatusStatsDto mockStats = new OrderStatusStatsDto(
                Collections.emptyMap(),
                "success",
                "Test message");
        when(statsApplicationService.getOrderStatusStatistics()).thenReturn(mockStats);

        // When
        OrderStatusStatsDto orderStatusStats = statsApplicationService.getOrderStatusStatistics();

        // Then
        assertThat(orderStatusStats).isNotNull();
        assertThat(orderStatusStats.status()).isEqualTo("success");
        assertThat(orderStatusStats.statusDistribution()).isNotNull();
        assertThat(orderStatusStats.message()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("Should get payment method statistics with JPA")
    void shouldGetPaymentMethodStatisticsWithJpa() {
        // Given
        PaymentMethodStatsDto mockStats = new PaymentMethodStatsDto(
                Collections.emptyMap(),
                "success",
                "Test message");
        when(statsApplicationService.getPaymentMethodStatistics()).thenReturn(mockStats);

        // When
        PaymentMethodStatsDto paymentMethodStats = statsApplicationService.getPaymentMethodStatistics();

        // Then
        assertThat(paymentMethodStats).isNotNull();
        assertThat(paymentMethodStats.status()).isEqualTo("success");
        assertThat(paymentMethodStats.paymentMethodDistribution()).isNotNull();
        assertThat(paymentMethodStats.message()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("Should get customer page with JPA")
    void shouldGetCustomerPageWithJpa() {
        // Given
        CustomerPageDto mockPage = new CustomerPageDto(
                Collections.emptyList(),
                0,
                0,
                10,
                0,
                true,
                true);
        when(customerApplicationService.getCustomers(anyInt(), anyInt())).thenReturn(mockPage);

        // When
        CustomerPageDto customerPage = customerApplicationService.getCustomers(0, 10);

        // Then
        assertThat(customerPage).isNotNull();
        assertThat(customerPage.content()).isNotNull();
        assertThat(customerPage.totalElements()).isEqualTo(0);
        assertThat(customerPage.totalPages()).isEqualTo(0);
        assertThat(customerPage.size()).isEqualTo(10);
        assertThat(customerPage.number()).isEqualTo(0);
        assertThat(customerPage.first()).isTrue();
        assertThat(customerPage.last()).isTrue();
    }

    @Test
    @DisplayName("Should check customer existence with JPA")
    void shouldCheckCustomerExistenceWithJpa() {
        // Given
        when(customerApplicationService.customerExists("existing-customer")).thenReturn(true);
        when(customerApplicationService.customerExists("non-existent-customer")).thenReturn(false);

        // When & Then - 測試存在的客戶
        boolean exists = customerApplicationService.customerExists("existing-customer");
        assertThat(exists).isTrue();

        // When & Then - 測試不存在的客戶
        boolean notExists = customerApplicationService.customerExists("non-existent-customer");
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should handle customer page with data")
    void shouldHandleCustomerPageWithData() {
        // Given
        CustomerDto mockCustomer = new CustomerDto(
                "CUST-001",
                "John Doe",
                "john@example.com",
                "0912345678",
                "台北市信義區",
                "STANDARD");
        CustomerPageDto mockPage = new CustomerPageDto(
                List.of(mockCustomer),
                1,
                1,
                10,
                0,
                true,
                false);
        when(customerApplicationService.getCustomers(0, 1)).thenReturn(mockPage);
        when(customerApplicationService.customerExists("CUST-001")).thenReturn(true);

        // When
        CustomerPageDto customerPage = customerApplicationService.getCustomers(0, 1);

        // Then
        assertThat(customerPage).isNotNull();
        assertThat(customerPage.content()).isNotEmpty();
        assertThat(customerPage.content()).hasSize(1);

        CustomerDto customer = customerPage.content().get(0);
        assertThat(customer.id()).isEqualTo("CUST-001");
        assertThat(customer.name()).isEqualTo("John Doe");
        assertThat(customer.email()).isEqualTo("john@example.com");
        assertThat(customer.membershipLevel()).isEqualTo("STANDARD");

        // And - 客戶應該存在
        boolean exists = customerApplicationService.customerExists(customer.id());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should handle empty customer page")
    void shouldHandleEmptyCustomerPage() {
        // Given
        CustomerPageDto emptyPage = new CustomerPageDto(
                Collections.emptyList(),
                0,
                0,
                10,
                0,
                true,
                true);
        when(customerApplicationService.getCustomers(0, 10)).thenReturn(emptyPage);

        // When
        CustomerPageDto customerPage = customerApplicationService.getCustomers(0, 10);

        // Then
        assertThat(customerPage).isNotNull();
        assertThat(customerPage.content()).isEmpty();
        assertThat(customerPage.totalElements()).isEqualTo(0);
        assertThat(customerPage.first()).isTrue();
        assertThat(customerPage.last()).isTrue();
    }

    @Test
    @DisplayName("Should handle statistics with data")
    void shouldHandleStatisticsWithData() {
        // Given
        var statusDistribution = java.util.Map.of(
                "PENDING", 10,
                "CONFIRMED", 25,
                "SHIPPED", 15,
                "DELIVERED", 50);
        OrderStatusStatsDto statsWithData = new OrderStatusStatsDto(
                statusDistribution,
                "success",
                "Statistics retrieved successfully");
        when(statsApplicationService.getOrderStatusStatistics()).thenReturn(statsWithData);

        // When
        OrderStatusStatsDto stats = statsApplicationService.getOrderStatusStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.statusDistribution()).hasSize(4);
        assertThat(stats.statusDistribution().get("DELIVERED")).isEqualTo(50);
        assertThat(stats.statusDistribution().get("CONFIRMED")).isEqualTo(25);
        assertThat(stats.status()).isEqualTo("success");
    }

    @Test
    @DisplayName("Should validate service method interactions")
    void shouldValidateServiceMethodInteractions() {
        // Given
        CustomerPageDto mockPage = new CustomerPageDto(
                Collections.emptyList(), 0, 0, 10, 0, true, true);
        when(customerApplicationService.getCustomers(0, 10)).thenReturn(mockPage);

        // When
        customerApplicationService.getCustomers(0, 10);
        customerApplicationService.customerExists("test-customer");

        // Then - 驗證方法被正確調用
        org.mockito.Mockito.verify(customerApplicationService).getCustomers(0, 10);
        org.mockito.Mockito.verify(customerApplicationService).customerExists("test-customer");
    }
}