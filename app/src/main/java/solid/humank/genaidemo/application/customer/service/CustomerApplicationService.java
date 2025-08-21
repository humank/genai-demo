package solid.humank.genaidemo.application.customer.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.customer.CustomerDto;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 客戶應用服務
 * 處理客戶相關的應用層業務邏輯
 * 
 * 需求 4.3: 事件處理器只應該調用應用服務
 */
@Service
public class CustomerApplicationService {

        private final CustomerRepository customerRepository;

        public CustomerApplicationService(CustomerRepository customerRepository) {
                this.customerRepository = customerRepository;
        }

        /**
         * 根據消費金額自動升級會員等級
         * 
         * @param customerId     客戶ID
         * @param totalSpending  總消費金額
         * @param spendingAmount 本次消費金額
         */
        @Transactional
        public void upgradeCustomerMembershipBasedOnSpending(String customerId, Money totalSpending,
                        Money spendingAmount) {
                Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));

                if (customerOpt.isPresent()) {
                        Customer customer = customerOpt.get();

                        // 根據總消費金額決定會員等級升級
                        MembershipLevel newLevel = determineNewMembershipLevel(totalSpending,
                                        customer.getMembershipLevel());

                        if (newLevel != customer.getMembershipLevel()) {
                                // 自動升級會員等級
                                customer.upgradeMembershipLevel(newLevel);

                                // 根據消費金額給予紅利點數
                                int bonusPoints = calculateBonusPoints(spendingAmount, newLevel);
                                if (bonusPoints > 0) {
                                        customer.addRewardPoints(bonusPoints,
                                                        "Bonus points for reaching " + newLevel + " membership level");
                                }

                                // 保存更新後的客戶
                                customerRepository.save(customer);
                        }
                }
        }

        /**
         * 為客戶添加獎勵點數
         * 
         * @param customerId 客戶ID
         * @param points     點數
         * @param reason     原因
         */
        @Transactional
        public void addRewardPoints(String customerId, int points, String reason) {
                Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));

                if (customerOpt.isPresent()) {
                        Customer customer = customerOpt.get();
                        customer.addRewardPoints(points, reason);
                        customerRepository.save(customer);
                }
        }

        /**
         * 更新客戶消費記錄
         * 
         * @param customerId  客戶ID
         * @param amount      消費金額
         * @param orderId     訂單ID
         * @param description 描述
         */
        @Transactional
        public void updateCustomerSpending(String customerId, Money amount, String orderId, String description) {
                Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));

                if (customerOpt.isPresent()) {
                        Customer customer = customerOpt.get();
                        customer.updateSpending(amount, orderId, description);
                        customerRepository.save(customer);
                }
        }

        // 私有輔助方法
        private MembershipLevel determineNewMembershipLevel(Money totalSpending, MembershipLevel currentLevel) {
                // VIP: 消費滿 100,000
                if (totalSpending.isGreaterThan(Money.twd(100000)) || totalSpending.equals(Money.twd(100000))) {
                        return MembershipLevel.VIP;
                }
                // PLATINUM: 消費滿 50,000
                if (totalSpending.isGreaterThan(Money.twd(50000)) || totalSpending.equals(Money.twd(50000))) {
                        return MembershipLevel.PLATINUM;
                }
                // GOLD: 消費滿 20,000
                if (totalSpending.isGreaterThan(Money.twd(20000)) || totalSpending.equals(Money.twd(20000))) {
                        return MembershipLevel.GOLD;
                }
                // SILVER: 消費滿 5,000
                if (totalSpending.isGreaterThan(Money.twd(5000)) || totalSpending.equals(Money.twd(5000))) {
                        return MembershipLevel.SILVER;
                }
                // STANDARD: 預設等級
                return MembershipLevel.STANDARD;
        }

        private int calculateBonusPoints(Money spendingAmount, MembershipLevel membershipLevel) {
                // 根據會員等級計算紅利點數倍率
                int[] multipliers = new int[MembershipLevel.values().length];
                for (MembershipLevel level : MembershipLevel.values()) {
                        multipliers[level.ordinal()] = level.ordinal() + 1; // 1x, 2x, 3x, 4x, 5x
                }

                int basePoints = spendingAmount.getAmount().intValue() / 100; // 每100元1點
                return basePoints * multipliers[membershipLevel.ordinal()];
        }

        /**
         * 獲取客戶分頁列表
         * 
         * @param page 頁碼
         * @param size 每頁大小
         * @return 客戶分頁數據
         */
        public CustomerPageDto getCustomers(int page, int size) {
                // 使用repository的分頁方法
                List<CustomerId> customerIds = customerRepository.findCustomerIds(page, size);
                int totalElements = customerRepository.countCustomers();

                List<CustomerDto> customerDtos = customerIds.stream()
                                .map(id -> customerRepository.findById(id))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(this::convertToDto)
                                .collect(Collectors.toList());

                int totalPages = (int) Math.ceil((double) totalElements / size);
                boolean isFirst = page == 0;
                boolean isLast = page >= totalPages - 1;

                return new CustomerPageDto(
                                customerDtos,
                                totalElements,
                                totalPages,
                                size,
                                page,
                                isFirst,
                                isLast);
        }

        /**
         * 根據ID獲取客戶
         * 
         * @param customerId 客戶ID
         * @return 客戶數據
         */
        public Optional<CustomerDto> getCustomer(String customerId) {
                Optional<Customer> customerOpt = customerRepository.findById(new CustomerId(customerId));
                return customerOpt.map(this::convertToDto);
        }

        /**
         * 檢查客戶是否存在
         * 
         * @param customerId 客戶ID
         * @return 是否存在
         */
        public boolean customerExists(String customerId) {
                return customerRepository.findById(new CustomerId(customerId)).isPresent();
        }

        // 私有轉換方法
        private CustomerDto convertToDto(Customer customer) {
                return new CustomerDto(
                                customer.getId().getValue(),
                                maskName(customer.getName().getName()),
                                maskEmail(customer.getEmail().getEmail()),
                                maskPhone(customer.getPhone() != null ? customer.getPhone().getPhone() : null),
                                maskAddress(customer.getAddress() != null ? customer.getAddress().getFullAddress()
                                                : null),
                                customer.getMembershipLevel().name());
        }

        private String maskName(String name) {
                if (name == null || name.length() <= 1)
                        return name;
                return name.charAt(0) + "*".repeat(name.length() - 1);
        }

        private String maskEmail(String email) {
                if (email == null || !email.contains("@"))
                        return email;
                String[] parts = email.split("@");
                String localPart = parts[0];
                String domain = parts[1];

                if (localPart.length() <= 2) {
                        return localPart.charAt(0) + "*@" + domain;
                }
                return localPart.charAt(0) + "*".repeat(localPart.length() - 2)
                                + localPart.charAt(localPart.length() - 1) + "@" + domain;
        }

        private String maskPhone(String phone) {
                if (phone == null || phone.length() < 4)
                        return phone;
                return phone.substring(0, 2) + "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 2);
        }

        private String maskAddress(String address) {
                if (address == null || address.length() <= 6)
                        return address;
                return address.substring(0, 3) + "*".repeat(3) + address.substring(address.length() - 3);
        }
}