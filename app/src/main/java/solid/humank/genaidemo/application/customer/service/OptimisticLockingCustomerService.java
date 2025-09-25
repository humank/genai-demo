package solid.humank.genaidemo.application.customer.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.OptimisticLockingRetryService;

/**
 * 客戶應用服務 - Aurora 樂觀鎖整合範例
 * 
 * 展示如何在應用服務中正確使用 Aurora 樂觀鎖機制，包含：
 * 1. 樂觀鎖重試機制的整合
 * 2. 並發衝突的處理
 * 3. 事務邊界的管理
 * 4. 錯誤處理和監控
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Service
public class OptimisticLockingCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockingCustomerService.class);

    private final CustomerRepository customerRepository;
    private final OptimisticLockingRetryService retryService;

    public OptimisticLockingCustomerService(CustomerRepository customerRepository,
                                          OptimisticLockingRetryService retryService) {
        this.customerRepository = customerRepository;
        this.retryService = retryService;
    }

    /**
     * 更新客戶會員等級 - 使用樂觀鎖重試機制
     * 
     * 此方法展示如何在高並發場景下安全地更新客戶信息：
     * 1. 使用樂觀鎖重試服務包裝業務操作
     * 2. 自動處理並發衝突和重試
     * 3. 提供詳細的錯誤信息和監控
     * 
     * @param customerId 客戶ID
     * @param newLevel 新的會員等級
     * @param reason 升級原因
     * @return 是否成功更新
     */
    @Transactional
    public boolean upgradeCustomerMembership(String customerId, MembershipLevel newLevel, String reason) {
        logger.info("Attempting to upgrade customer {} to membership level {}", customerId, newLevel);

        try {
            // 使用樂觀鎖重試服務執行更新操作
            Boolean result = retryService.executeWithRetry(
                () -> performMembershipUpgrade(customerId, newLevel, reason),
                "Customer",
                customerId,
                "upgradeCustomerMembership"
            );

            logger.info("Successfully upgraded customer {} to membership level {}", customerId, newLevel);
            return result != null && result;

        } catch (Exception e) {
            logger.error("Failed to upgrade customer {} to membership level {} after retries: {}", 
                        customerId, newLevel, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新客戶獎勵點數 - 使用樂觀鎖重試機制
     * 
     * @param customerId 客戶ID
     * @param points 要添加的點數
     * @param reason 添加原因
     * @return 是否成功更新
     */
    @Transactional
    public boolean addRewardPointsWithRetry(String customerId, int points, String reason) {
        logger.info("Attempting to add {} reward points to customer {}", points, customerId);

        try {
            Boolean result = retryService.executeWithRetry(
                () -> performAddRewardPoints(customerId, points, reason),
                "Customer",
                customerId,
                "addRewardPoints"
            );

            logger.info("Successfully added {} reward points to customer {}", points, customerId);
            return result != null && result;

        } catch (Exception e) {
            logger.error("Failed to add reward points to customer {} after retries: {}", 
                        customerId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新客戶消費記錄 - 使用樂觀鎖重試機制
     * 
     * @param customerId 客戶ID
     * @param amount 消費金額
     * @param orderId 訂單ID
     * @param description 描述
     * @return 是否成功更新
     */
    @Transactional
    public boolean updateCustomerSpendingWithRetry(String customerId, Money amount, 
                                                  String orderId, String description) {
        logger.info("Attempting to update spending for customer {} with amount {}", customerId, amount);

        try {
            Boolean result = retryService.executeWithRetry(
                () -> performUpdateSpending(customerId, amount, orderId, description),
                "Customer",
                customerId,
                "updateCustomerSpending"
            );

            logger.info("Successfully updated spending for customer {} with amount {}", customerId, amount);
            return result != null && result;

        } catch (Exception e) {
            logger.error("Failed to update spending for customer {} after retries: {}", 
                        customerId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量更新客戶信息 - 展示如何處理批量操作中的樂觀鎖衝突
     * 
     * @param customerIds 客戶ID列表
     * @param operation 要執行的操作描述
     * @return 成功更新的客戶數量
     */
    @Transactional
    public int batchUpdateCustomers(java.util.List<String> customerIds, String operation) {
        logger.info("Starting batch update for {} customers", customerIds.size());
        
        int successCount = 0;
        int failureCount = 0;

        for (String customerId : customerIds) {
            try {
                // 為每個客戶單獨使用重試機制
                retryService.executeWithRetry(
                    () -> performBatchOperation(customerId, operation),
                    "Customer",
                    customerId,
                    "batchUpdate_" + operation,
                    3 // 批量操作使用較少的重試次數
                );
                successCount++;
                
            } catch (Exception e) {
                logger.warn("Failed to update customer {} in batch operation: {}", 
                           customerId, e.getMessage());
                failureCount++;
            }
        }

        logger.info("Batch update completed: {} successful, {} failed", successCount, failureCount);
        return successCount;
    }

    // === 私有業務邏輯方法 ===

    /**
     * 執行會員等級升級的核心業務邏輯
     */
    private Boolean performMembershipUpgrade(String customerId, MembershipLevel newLevel, String reason) {
        Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));
        
        if (customerOpt.isEmpty()) {
            logger.warn("Customer not found: {}", customerId);
            return false;
        }

        Customer customer = customerOpt.get();
        
        // 檢查是否需要升級
        if (customer.getMembershipLevel().ordinal() >= newLevel.ordinal()) {
            logger.info("Customer {} already has membership level {} or higher", 
                       customerId, customer.getMembershipLevel());
            return true;
        }

        // 執行升級
        customer.upgradeMembershipLevel(newLevel);
        
        // 根據新等級給予歡迎獎勵
        int welcomeBonus = calculateWelcomeBonus(newLevel);
        if (welcomeBonus > 0) {
            customer.addRewardPoints(welcomeBonus, 
                String.format("Welcome bonus for %s membership: %s", newLevel, reason));
        }

        // 保存更新
        customerRepository.save(customer);
        
        logger.debug("Upgraded customer {} to {} with {} welcome bonus points", 
                    customerId, newLevel, welcomeBonus);
        return true;
    }

    /**
     * 執行添加獎勵點數的核心業務邏輯
     */
    private Boolean performAddRewardPoints(String customerId, int points, String reason) {
        Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));
        
        if (customerOpt.isEmpty()) {
            logger.warn("Customer not found: {}", customerId);
            return false;
        }

        Customer customer = customerOpt.get();
        customer.addRewardPoints(points, reason);
        customerRepository.save(customer);
        
        logger.debug("Added {} points to customer {} for reason: {}", points, customerId, reason);
        return true;
    }

    /**
     * 執行更新消費記錄的核心業務邏輯
     */
    private Boolean performUpdateSpending(String customerId, Money amount, String orderId, String description) {
        Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));
        
        if (customerOpt.isEmpty()) {
            logger.warn("Customer not found: {}", customerId);
            return false;
        }

        Customer customer = customerOpt.get();
        customer.updateSpending(amount, orderId, description);
        
        // 檢查是否需要自動升級會員等級
        MembershipLevel currentLevel = customer.getMembershipLevel();
        MembershipLevel newLevel = determineNewMembershipLevel(customer.getTotalSpending());
        
        if (newLevel.ordinal() > currentLevel.ordinal()) {
            customer.upgradeMembershipLevel(newLevel);
            logger.info("Auto-upgraded customer {} from {} to {} based on spending", 
                       customerId, currentLevel, newLevel);
        }

        customerRepository.save(customer);
        
        logger.debug("Updated spending for customer {} with amount {} for order {}", 
                    customerId, amount, orderId);
        return true;
    }

    /**
     * 執行批量操作的核心業務邏輯
     */
    private Void performBatchOperation(String customerId, String operation) {
        Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));
        
        if (customerOpt.isEmpty()) {
            logger.warn("Customer not found in batch operation: {}", customerId);
            return null;
        }

        Customer customer = customerOpt.get();
        
        // 根據操作類型執行不同的業務邏輯
        switch (operation.toLowerCase()) {
            case "refresh_membership":
                // 重新計算會員等級
                MembershipLevel newLevel = determineNewMembershipLevel(customer.getTotalSpending());
                if (newLevel != customer.getMembershipLevel()) {
                    customer.upgradeMembershipLevel(newLevel);
                }
                break;
                
            case "add_loyalty_bonus":
                // 添加忠誠度獎勵
                int loyaltyBonus = calculateLoyaltyBonus(customer.getMembershipLevel());
                customer.addRewardPoints(loyaltyBonus, "Monthly loyalty bonus");
                break;
                
            default:
                logger.warn("Unknown batch operation: {}", operation);
                return null;
        }

        customerRepository.save(customer);
        return null;
    }

    // === 私有輔助方法 ===

    private int calculateWelcomeBonus(MembershipLevel level) {
        return switch (level) {
            case SILVER -> 100;
            case GOLD -> 300;
            case PLATINUM -> 500;
            case VIP -> 1000;
            default -> 0;
        };
    }

    private int calculateLoyaltyBonus(MembershipLevel level) {
        return switch (level) {
            case STANDARD -> 50;
            case SILVER -> 100;
            case GOLD -> 200;
            case PLATINUM -> 300;
            case VIP -> 500;
        };
    }

    private MembershipLevel determineNewMembershipLevel(Money totalSpending) {
        if (totalSpending.isGreaterThan(Money.twd(100000)) || totalSpending.isEqualTo(Money.twd(100000))) {
            return MembershipLevel.VIP;
        } else if (totalSpending.isGreaterThan(Money.twd(50000)) || totalSpending.isEqualTo(Money.twd(50000))) {
            return MembershipLevel.PLATINUM;
        } else if (totalSpending.isGreaterThan(Money.twd(20000)) || totalSpending.isEqualTo(Money.twd(20000))) {
            return MembershipLevel.GOLD;
        } else if (totalSpending.isGreaterThan(Money.twd(5000)) || totalSpending.isEqualTo(Money.twd(5000))) {
            return MembershipLevel.SILVER;
        } else {
            return MembershipLevel.STANDARD;
        }
    }
}