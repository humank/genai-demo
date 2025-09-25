package solid.humank.genaidemo.infrastructure.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.customer.service.OptimisticLockingCustomerService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.AuroraReadWriteConfiguration.DataSourceRouter;
import solid.humank.genaidemo.infrastructure.common.persistence.AuroraReadWriteConfiguration.DataSourceType;

/**
 * Aurora 樂觀鎖機制整合測試
 * 
 * 測試 Aurora 樂觀鎖策略的完整實現，包含：
 * 1. 讀寫端點分離
 * 2. 樂觀鎖衝突檢測
 * 3. 自動重試機制
 * 4. 並發場景處理
 * 5. 應用服務整合
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Aurora 樂觀鎖機制整合測試")
class AuroraOptimisticLockingIntegrationTest {

    @Autowired
    private OptimisticLockingCustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OptimisticLockingRetryService retryService;

    @Autowired
    private OptimisticLockingConflictDetector conflictDetector;

    @Test
    @DisplayName("應該成功創建和更新客戶使用樂觀鎖")
    @Transactional
    void should_successfully_create_and_update_customer_with_optimistic_locking() {
        // Given - 創建測試客戶
        Customer customer = createTestCustomer("test-customer-1");
        customerRepository.save(customer);

        // When - 使用樂觀鎖服務更新客戶
        boolean result = customerService.upgradeCustomerMembership(
            customer.getId().getValue(), 
            MembershipLevel.GOLD, 
            "Integration test upgrade"
        );

        // Then
        assertThat(result).isTrue();
        
        // 驗證更新結果
        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedCustomer.getMembershipLevel()).isEqualTo(MembershipLevel.GOLD);
        assertThat(updatedCustomer.getVersion()).isGreaterThan(customer.getVersion());
    }

    @Test
    @DisplayName("應該正確處理並發更新衝突")
    void should_handle_concurrent_update_conflicts() throws InterruptedException {
        // Given - 創建測試客戶
        Customer customer = createTestCustomer("test-customer-concurrent");
        customerRepository.save(customer);
        String customerId = customer.getId().getValue();

        // When - 模擬並發更新
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有線程準備就緒
                    
                    boolean result = customerService.addRewardPointsWithRetry(
                        customerId, 
                        100 * (threadIndex + 1), 
                        "Concurrent test points " + threadIndex
                    );
                    
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 同時開始所有線程
        startLatch.countDown();
        
        // 等待所有線程完成
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 驗證最終狀態
        Customer finalCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(finalCustomer.getVersion()).isGreaterThan(customer.getVersion());
        
        System.out.printf("Concurrent test results: %d successful, %d failed%n", 
                         successCount.get(), failureCount.get());
    }

    @Test
    @DisplayName("應該正確使用讀寫端點分離")
    void should_correctly_use_read_write_endpoint_separation() {
        // Given - 創建測試客戶
        Customer customer = createTestCustomer("test-customer-read-write");
        customerRepository.save(customer);

        // When & Then - 測試讀取端點
        Customer readCustomer = DataSourceRouter.executeWithReadDataSource(() -> {
            return customerRepository.findById(customer.getId()).orElse(null);
        });
        
        assertThat(readCustomer).isNotNull();
        assertThat(readCustomer.getId()).isEqualTo(customer.getId());

        // When & Then - 測試寫入端點
        DataSourceRouter.executeWithWriteDataSource(() -> {
            Customer writeCustomer = customerRepository.findById(customer.getId()).orElseThrow();
            writeCustomer.addRewardPoints(500, "Read-write separation test");
            customerRepository.save(writeCustomer);
            return null;
        });

        // 驗證更新結果
        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedCustomer.getRewardPoints()).isEqualTo(500);
    }

    @Test
    @DisplayName("應該正確處理批量操作中的樂觀鎖衝突")
    void should_handle_optimistic_lock_conflicts_in_batch_operations() {
        // Given - 創建多個測試客戶
        java.util.List<String> customerIds = java.util.List.of(
            createAndSaveTestCustomer("batch-customer-1").getId().getValue(),
            createAndSaveTestCustomer("batch-customer-2").getId().getValue(),
            createAndSaveTestCustomer("batch-customer-3").getId().getValue()
        );

        // When - 執行批量更新
        int successCount = customerService.batchUpdateCustomers(customerIds, "add_loyalty_bonus");

        // Then
        assertThat(successCount).isEqualTo(customerIds.size());
        
        // 驗證每個客戶都被正確更新
        for (String customerId : customerIds) {
            Customer customer = customerRepository.findById(new CustomerId(customerId)).orElseThrow();
            assertThat(customer.getRewardPoints().balance()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("應該在重試失敗後拋出詳細的衝突異常")
    void should_throw_detailed_conflict_exception_after_retry_failure() {
        // Given - 創建一個會持續失敗的操作
        String entityType = "TestEntity";
        String entityId = "test-entity-1";
        String operation = "always-fail-operation";

        // When & Then - 驗證異常處理
        assertThatThrownBy(() -> {
            retryService.executeWithRetry(
                () -> {
                    throw new jakarta.persistence.OptimisticLockException("Simulated persistent conflict");
                },
                entityType,
                entityId,
                operation,
                2 // 只重試2次
            );
        })
        .isInstanceOf(OptimisticLockingConflictException.class)
        .satisfies(exception -> {
            OptimisticLockingConflictException conflictException = 
                (OptimisticLockingConflictException) exception;
            
            assertThat(conflictException.getEntityType()).isEqualTo(entityType);
            assertThat(conflictException.getEntityId()).isEqualTo(entityId);
            assertThat(conflictException.shouldRetry()).isTrue();
            assertThat(conflictException.getSuggestedRetryStrategy()).isNotNull();
        });
    }

    @Test
    @DisplayName("應該正確處理高頻率並發更新場景")
    void should_handle_high_frequency_concurrent_updates() throws InterruptedException {
        // Given - 創建測試客戶
        Customer customer = createAndSaveTestCustomer("high-frequency-customer");
        String customerId = customer.getId().getValue();

        // When - 模擬高頻率並發更新
        int operationCount = 20;
        CountDownLatch completeLatch = new CountDownLatch(operationCount);
        AtomicInteger totalSuccessCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < operationCount; i++) {
            final int operationIndex = i;
            
            CompletableFuture.runAsync(() -> {
                try {
                    // 隨機選擇不同類型的操作
                    if (operationIndex % 3 == 0) {
                        // 添加獎勵點數
                        boolean success = customerService.addRewardPointsWithRetry(
                            customerId, 10, "High frequency test " + operationIndex);
                        if (success) totalSuccessCount.incrementAndGet();
                        
                    } else if (operationIndex % 3 == 1) {
                        // 更新消費記錄
                        boolean success = customerService.updateCustomerSpendingWithRetry(
                            customerId, Money.twd(100), "order-" + operationIndex, 
                            "High frequency spending " + operationIndex);
                        if (success) totalSuccessCount.incrementAndGet();
                        
                    } else {
                        // 嘗試升級會員等級
                        boolean success = customerService.upgradeCustomerMembership(
                            customerId, MembershipLevel.SILVER, "High frequency upgrade " + operationIndex);
                        if (success) totalSuccessCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    System.err.printf("Operation %d failed: %s%n", operationIndex, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            }, executor);
        }

        // 等待所有操作完成
        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(completed).isTrue();
        assertThat(totalSuccessCount.get()).isGreaterThan(operationCount / 2); // 至少一半成功
        
        // 驗證最終狀態一致性
        Customer finalCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(finalCustomer.getVersion()).isGreaterThan(customer.getVersion());
        
        System.out.printf("High frequency test results: %d/%d operations successful%n", 
                         totalSuccessCount.get(), operationCount);
    }

    // === 私有輔助方法 ===

    private Customer createTestCustomer(String namePrefix) {
        return new Customer(
            new CustomerId(namePrefix + "-" + System.currentTimeMillis()),
            new CustomerName(namePrefix + " Test Customer"),
            new Email(namePrefix + "@test.com"),
            new Phone("0912345678"),
            new Address("Test Street 123", "Test City", "12345", "Taiwan"),
            MembershipLevel.STANDARD,
            java.time.LocalDate.of(1990, 1, 1),
            java.time.LocalDateTime.now()
        );
    }

    private Customer createAndSaveTestCustomer(String namePrefix) {
        Customer customer = createTestCustomer(namePrefix);
        return customerRepository.save(customer);
    }
}