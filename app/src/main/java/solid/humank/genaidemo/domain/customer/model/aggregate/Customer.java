package solid.humank.genaidemo.domain.customer.model.aggregate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import solid.humank.genaidemo.domain.common.aggregate.AggregateReconstruction;
import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.aggregate.AggregateStateTracker;
import solid.humank.genaidemo.domain.common.aggregate.CrossAggregateOperation;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.exception.BusinessRuleViolationException;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.entity.CustomerPreferences;
import solid.humank.genaidemo.domain.customer.model.entity.DeliveryAddress;
import solid.humank.genaidemo.domain.customer.model.entity.PaymentMethod;
import solid.humank.genaidemo.domain.customer.model.events.CustomerCreatedEvent;
import solid.humank.genaidemo.domain.customer.model.events.CustomerProfileUpdatedEvent;
import solid.humank.genaidemo.domain.customer.model.events.CustomerSpendingUpdatedEvent;
import solid.humank.genaidemo.domain.customer.model.events.CustomerStatusChangedEvent;
import solid.humank.genaidemo.domain.customer.model.events.DeliveryAddressAddedEvent;
import solid.humank.genaidemo.domain.customer.model.events.DeliveryAddressRemovedEvent;
import solid.humank.genaidemo.domain.customer.model.events.MembershipLevelUpgradedEvent;
import solid.humank.genaidemo.domain.customer.model.events.NotificationPreferencesUpdatedEvent;
import solid.humank.genaidemo.domain.customer.model.events.RewardPointsEarnedEvent;
import solid.humank.genaidemo.domain.customer.model.events.RewardPointsRedeemedEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerPreferencesId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerStatus;
import solid.humank.genaidemo.domain.customer.model.valueobject.DeliveryAddressId;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;
import solid.humank.genaidemo.domain.customer.model.valueobject.PaymentMethodId;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.customer.model.valueobject.RewardPoints;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 增強的客戶聚合根 - 混搭方案：Annotation + Interface */
@AggregateRoot(name = "Customer", description = "增強的客戶聚合根，支援完整的消費者功能", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {

    private final CustomerId id;
    private final AggregateStateTracker<Customer> stateTracker = new AggregateStateTracker<>(this);
    private CustomerName name;
    private Email email;
    private Phone phone;
    private Address address;
    private MembershipLevel membershipLevel;

    // 新增屬性
    private LocalDate birthDate;
    private LocalDateTime registrationDate;
    private RewardPoints rewardPoints;
    private CustomerStatus status;
    private Money totalSpending; // 總消費金額

    // Entity 集合
    private final List<DeliveryAddress> deliveryAddresses; // 原 List<Address>
    private CustomerPreferences preferences; // 統一偏好設定管理
    private final List<PaymentMethod> paymentMethods; // 支付方式集合

    public Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
        this.birthDate = birthDate;
        this.registrationDate = registrationDate != null ? registrationDate : LocalDateTime.now();
        this.rewardPoints = RewardPoints.empty();
        this.status = CustomerStatus.ACTIVE;
        this.totalSpending = Money.twd(0);
        this.deliveryAddresses = new ArrayList<>();
        this.preferences = new CustomerPreferences(CustomerPreferencesId.generate());
        this.paymentMethods = new ArrayList<>();

        // 發布客戶創建事件
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }

    // 向後兼容的建構子
    public Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel) {
        this(id, name, email, phone, address, membershipLevel, null, LocalDateTime.now());
    }

    /**
     * 重建用建構子 - 用於從持久化狀態重建聚合根
     * 此建構子不會產生領域事件
     */
    @AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建客戶聚合根")
    protected Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate,
            RewardPoints rewardPoints,
            List<DeliveryAddress> deliveryAddresses,
            CustomerPreferences preferences,
            List<PaymentMethod> paymentMethods,
            CustomerStatus status,
            Money totalSpending) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
        this.birthDate = birthDate;
        this.registrationDate = registrationDate != null ? registrationDate : LocalDateTime.now();
        this.rewardPoints = rewardPoints != null ? rewardPoints : RewardPoints.empty();
        this.status = status != null ? status : CustomerStatus.ACTIVE;
        this.totalSpending = totalSpending != null ? totalSpending : Money.twd(0);
        this.deliveryAddresses = deliveryAddresses != null ? new ArrayList<>(deliveryAddresses) : new ArrayList<>();
        this.preferences = preferences != null ? preferences
                : new CustomerPreferences(CustomerPreferencesId.generate());
        this.paymentMethods = paymentMethods != null ? new ArrayList<>(paymentMethods) : new ArrayList<>();

        // 重建時不發布事件
    }

    // Getters
    public CustomerId getId() {
        return id;
    }

    public CustomerName getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Phone getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }

    public MembershipLevel getMembershipLevel() {
        return membershipLevel;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public RewardPoints getRewardPoints() {
        return rewardPoints;
    }

    public List<DeliveryAddress> getDeliveryAddresses() {
        return new ArrayList<>(deliveryAddresses);
    }

    public CustomerPreferences getPreferences() {
        return preferences;
    }

    // 向後兼容的方法
    public NotificationPreferences getNotificationPreferences() {
        return preferences.getNotificationPreferences();
    }

    // 向後兼容的方法 - 返回簡單的 Address 列表
    public List<Address> getDeliveryAddressList() {
        return deliveryAddresses.stream()
                .map(DeliveryAddress::getAddress)
                .collect(Collectors.toList());
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Money getTotalSpending() {
        return totalSpending;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Customer customer = (Customer) obj;
        return id.equals(customer.id);
    }

    // 業務方法

    /** 檢查是否為新會員（註冊30天內） */
    public boolean isNewMember() {
        if (registrationDate == null)
            return false;
        return registrationDate.isAfter(LocalDateTime.now().minusDays(30));
    }

    /** 檢查是否為生日月 */
    public boolean isBirthdayMonth() {
        if (birthDate == null)
            return false;
        return birthDate.getMonth() == LocalDate.now().getMonth();
    }

    /** 兌換紅利點數 */
    public void redeemPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.redeem(points);

        // 發布紅利點數兌換事件
        collectEvent(RewardPointsRedeemedEvent.create(
                this.id, points, this.rewardPoints.balance(), reason));
    }

    /** 添加紅利點數 */
    public void addRewardPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.add(points);

        // 發布紅利點數獲得事件
        collectEvent(RewardPointsEarnedEvent.create(this.id, points, this.rewardPoints.balance(), reason));
    }

    /** 使用獎勵積分（向後兼容） */
    public boolean useRewardPoints(int points) {
        try {
            redeemPoints(points, "Legacy usage");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 更新個人資料 */
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        // 驗證業務規則
        validateProfileUpdate(newName, newEmail, newPhone);

        // 檢查是否有任何變化
        boolean hasChanges = !Objects.equals(this.name, newName) ||
                !Objects.equals(this.email, newEmail) ||
                !Objects.equals(this.phone, newPhone);

        if (hasChanges) {
            // 使用狀態追蹤器追蹤變化（不產生事件）
            stateTracker.trackChange("name", this.name, newName);
            stateTracker.trackChange("email", this.email, newEmail);
            stateTracker.trackChange("phone", this.phone, newPhone);

            // 更新值
            this.name = newName;
            this.email = newEmail;
            this.phone = newPhone;

            // 產生單一的個人資料更新事件
            collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
        }
    }

    /** 驗證個人資料更新的業務規則 */
    private void validateProfileUpdate(CustomerName newName, Email newEmail, Phone newPhone) {
        BusinessRuleViolationException.Builder violationBuilder = new BusinessRuleViolationException.Builder("Customer",
                this.id.getValue());

        if (newName == null) {
            violationBuilder.addError("CUSTOMER_NAME_REQUIRED", "客戶姓名不能為空");
        }

        if (newEmail == null) {
            violationBuilder.addError("CUSTOMER_EMAIL_REQUIRED", "客戶郵箱不能為空");
        }

        if (newPhone == null) {
            violationBuilder.addError("CUSTOMER_PHONE_REQUIRED", "客戶電話不能為空");
        }

        // 檢查是否有錯誤並拋出異常
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }

    /** 更新通知偏好 */
    public void updateNotificationPreferences(NotificationPreferences newPreferences) {
        NotificationPreferences oldPreferences = this.preferences.getNotificationPreferences();
        this.preferences.updateNotificationPreferences(newPreferences);

        // 發布通知偏好設定更新事件
        collectEvent(NotificationPreferencesUpdatedEvent.create(this.id, oldPreferences, newPreferences));
    }

    /** 更新客戶偏好設定 */
    public void updatePreferences(String language, String currency,
            boolean personalizedRecommendations,
            boolean marketingEmails,
            boolean smsNotifications,
            boolean pushNotifications) {
        this.preferences.updatePreferences(language, currency,
                personalizedRecommendations, marketingEmails,
                smsNotifications, pushNotifications);
    }

    /** 設定自定義偏好 */
    public void setCustomPreference(String key, String value) {
        this.preferences.setCustomPreference(key, value);
    }

    /** 獲取自定義偏好 */
    public String getCustomPreference(String key) {
        return this.preferences.getCustomPreference(key);
    }

    /** 檢查是否可以接收通知 */
    public boolean canReceiveNotifications() {
        return this.preferences.canReceiveNotifications();
    }

    /** 檢查是否可以接收行銷內容 */
    public boolean canReceiveMarketing() {
        return this.preferences.canReceiveMarketing();
    }

    /** 添加配送地址 */
    public DeliveryAddressId addDeliveryAddress(Address address, String label) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        if (deliveryAddresses.size() >= 10) {
            throw new IllegalArgumentException("Cannot have more than 10 delivery addresses");
        }

        // 檢查是否已存在相同地址
        boolean addressExists = deliveryAddresses.stream()
                .anyMatch(da -> da.isSameAddress(address));
        if (addressExists) {
            throw new IllegalArgumentException("Address already exists");
        }

        DeliveryAddress deliveryAddress = new DeliveryAddress(
                DeliveryAddressId.generate(), address, label);

        // 如果是第一個地址，自動設為預設
        if (deliveryAddresses.isEmpty()) {
            deliveryAddress.setAsDefault();
        }

        deliveryAddresses.add(deliveryAddress);

        // 發布配送地址添加事件
        collectEvent(DeliveryAddressAddedEvent.create(this.id, address, deliveryAddresses.size()));

        return deliveryAddress.getId();
    }

    /** 向後兼容的添加配送地址方法 */
    public void addDeliveryAddress(Address address) {
        addDeliveryAddress(address, "Default");
    }

    /** 移除配送地址 */
    public void removeDeliveryAddress(DeliveryAddressId addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }

        DeliveryAddress toRemove = deliveryAddresses.stream()
                .filter(da -> da.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        boolean wasDefault = toRemove.isDefault();
        Address address = toRemove.getAddress();
        deliveryAddresses.remove(toRemove);

        // 如果移除的是預設地址，設定第一個地址為預設
        if (wasDefault && !deliveryAddresses.isEmpty()) {
            deliveryAddresses.get(0).setAsDefault();
        }

        // 發布配送地址移除事件
        collectEvent(DeliveryAddressRemovedEvent.create(this.id, address, deliveryAddresses.size()));
    }

    /** 向後兼容的移除配送地址方法 */
    public void removeDeliveryAddress(Address address) {
        DeliveryAddress toRemove = deliveryAddresses.stream()
                .filter(da -> da.isSameAddress(address))
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            removeDeliveryAddress(toRemove.getId());
        }
    }

    /** 設定預設配送地址 */
    public void setDefaultDeliveryAddress(DeliveryAddressId addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }

        // 取消所有地址的預設狀態
        deliveryAddresses.forEach(DeliveryAddress::unsetAsDefault);

        // 設定指定地址為預設
        DeliveryAddress targetAddress = deliveryAddresses.stream()
                .filter(da -> da.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        targetAddress.setAsDefault();
    }

    /** 更新配送地址標籤 */
    public void updateDeliveryAddressLabel(DeliveryAddressId addressId, String newLabel) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }

        DeliveryAddress address = deliveryAddresses.stream()
                .filter(da -> da.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        address.updateLabel(newLabel);
    }

    /** 標記地址為已使用 */
    public void markAddressAsUsed(DeliveryAddressId addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }

        DeliveryAddress address = deliveryAddresses.stream()
                .filter(da -> da.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        address.markAsUsed();
    }

    /** 獲取預設配送地址 */
    public Optional<DeliveryAddress> getDefaultDeliveryAddress() {
        return deliveryAddresses.stream()
                .filter(DeliveryAddress::isDefault)
                .findFirst();
    }

    /** 獲取常用配送地址 */
    public List<DeliveryAddress> getFrequentlyUsedAddresses() {
        return deliveryAddresses.stream()
                .filter(DeliveryAddress::isFrequentlyUsed)
                .sorted((a, b) -> Integer.compare(b.getUsageCount(), a.getUsageCount()))
                .collect(Collectors.toList());
    }

    // ==================== 支付方式管理 ====================

    /** 獲取所有支付方式 */
    public List<PaymentMethod> getPaymentMethods() {
        return new ArrayList<>(paymentMethods);
    }

    /** 添加信用卡支付方式 */
    public PaymentMethodId addCreditCard(String displayName, String provider,
            String maskedNumber, String expiryDate, String holderName) {
        PaymentMethod paymentMethod = PaymentMethod.createCreditCard(
                displayName, provider, maskedNumber, expiryDate, holderName);

        paymentMethods.add(paymentMethod);

        // 如果是第一個支付方式，自動設為預設
        if (paymentMethods.size() == 1) {
            paymentMethod.setAsDefault();
        }

        // 發布支付方式添加事件（需要創建相應的事件）
        // collectEvent(PaymentMethodAddedEvent.create(this.id, paymentMethod.getId(),
        // paymentMethod.getType()));

        return paymentMethod.getId();
    }

    /** 添加數位錢包支付方式 */
    public PaymentMethodId addDigitalWallet(String displayName, String provider, String accountInfo) {
        PaymentMethod paymentMethod = PaymentMethod.createDigitalWallet(displayName, provider, accountInfo);

        paymentMethods.add(paymentMethod);

        // 如果是第一個支付方式，自動設為預設
        if (paymentMethods.size() == 1) {
            paymentMethod.setAsDefault();
        }

        return paymentMethod.getId();
    }

    /** 添加銀行轉帳支付方式 */
    public PaymentMethodId addBankTransfer(String displayName, String bankName,
            String maskedAccountNumber, String holderName) {
        PaymentMethod paymentMethod = PaymentMethod.createBankTransfer(
                displayName, bankName, maskedAccountNumber, holderName);

        paymentMethods.add(paymentMethod);

        // 如果是第一個支付方式，自動設為預設
        if (paymentMethods.size() == 1) {
            paymentMethod.setAsDefault();
        }

        return paymentMethod.getId();
    }

    /** 移除支付方式 */
    public void removePaymentMethod(PaymentMethodId paymentMethodId) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod toRemove = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        boolean wasDefault = toRemove.isDefault();
        paymentMethods.remove(toRemove);

        // 如果移除的是預設支付方式，且還有其他支付方式，則設定第一個為預設
        if (wasDefault && !paymentMethods.isEmpty()) {
            paymentMethods.get(0).setAsDefault();
        }

        // 發布支付方式移除事件
        // collectEvent(PaymentMethodRemovedEvent.create(this.id, paymentMethodId));
    }

    /** 設定預設支付方式 */
    public void setDefaultPaymentMethod(PaymentMethodId paymentMethodId) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod targetMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        if (!targetMethod.canUse()) {
            throw new IllegalStateException("Cannot set inactive or expired payment method as default");
        }

        // 取消所有支付方式的預設狀態
        paymentMethods.forEach(PaymentMethod::unsetAsDefault);

        // 設定新的預設支付方式
        targetMethod.setAsDefault();
    }

    /** 啟用支付方式 */
    public void activatePaymentMethod(PaymentMethodId paymentMethodId) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod paymentMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentMethod.activate();
    }

    /** 停用支付方式 */
    public void deactivatePaymentMethod(PaymentMethodId paymentMethodId) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod paymentMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentMethod.deactivate();

        // 如果停用的是預設支付方式，需要選擇新的預設支付方式
        if (paymentMethod.isDefault()) {
            Optional<PaymentMethod> newDefault = paymentMethods.stream()
                    .filter(pm -> pm.canUse() && !pm.getId().equals(paymentMethodId))
                    .findFirst();

            if (newDefault.isPresent()) {
                newDefault.get().setAsDefault();
            }
        }
    }

    /** 驗證支付方式 */
    public void verifyPaymentMethod(PaymentMethodId paymentMethodId, String verificationToken) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod paymentMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentMethod.verify(verificationToken);
    }

    /** 更新支付方式顯示名稱 */
    public void updatePaymentMethodDisplayName(PaymentMethodId paymentMethodId, String displayName) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod paymentMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentMethod.updateDisplayName(displayName);
    }

    /** 記錄支付方式使用 */
    public void recordPaymentMethodUsage(PaymentMethodId paymentMethodId) {
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method ID cannot be null");
        }

        PaymentMethod paymentMethod = paymentMethods.stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentMethod.recordUsage();
    }

    /** 獲取預設支付方式 */
    public Optional<PaymentMethod> getDefaultPaymentMethod() {
        return paymentMethods.stream()
                .filter(PaymentMethod::isDefault)
                .findFirst();
    }

    /** 獲取有效的支付方式 */
    public List<PaymentMethod> getActivePaymentMethods() {
        return paymentMethods.stream()
                .filter(PaymentMethod::canUse)
                .collect(Collectors.toList());
    }

    /** 獲取特定類型的支付方式 */
    public List<PaymentMethod> getPaymentMethodsByType(PaymentMethod.PaymentType type) {
        return paymentMethods.stream()
                .filter(pm -> pm.getType() == type)
                .collect(Collectors.toList());
    }

    /** 檢查是否有有效的支付方式 */
    public boolean hasActivePaymentMethod() {
        return paymentMethods.stream().anyMatch(PaymentMethod::canUse);
    }

    /** 檢查是否有預設支付方式 */
    public boolean hasDefaultPaymentMethod() {
        return paymentMethods.stream().anyMatch(PaymentMethod::isDefault);
    }

    /** 清理過期的支付方式 */
    public void cleanupExpiredPaymentMethods() {
        List<PaymentMethod> expiredMethods = paymentMethods.stream()
                .filter(PaymentMethod::isExpired)
                .collect(Collectors.toList());

        for (PaymentMethod expiredMethod : expiredMethods) {
            expiredMethod.markAsExpired();
        }

        // 如果預設支付方式過期，選擇新的預設支付方式
        if (getDefaultPaymentMethod().map(PaymentMethod::isExpired).orElse(false)) {
            Optional<PaymentMethod> newDefault = paymentMethods.stream()
                    .filter(pm -> pm.canUse())
                    .findFirst();

            if (newDefault.isPresent()) {
                paymentMethods.forEach(PaymentMethod::unsetAsDefault);
                newDefault.get().setAsDefault();
            }
        }
    }

    /** 更新客戶狀態 */
    public void updateStatus(CustomerStatus newStatus, String reason) {
        CustomerStatus oldStatus = this.status;
        this.status = newStatus;

        // 發布客戶狀態變更事件
        collectEvent(new CustomerStatusChangedEvent(this.id, oldStatus, newStatus, reason));
    }

    /** 更新客戶狀態（向後兼容） */
    public void updateStatus(CustomerStatus newStatus) {
        updateStatus(newStatus, "Status updated");
    }

    /** 檢查是否可以進行購買 */
    public boolean canMakePurchase() {
        return status.canMakePurchase();
    }

    // 測試需要的額外方法

    /** 更新生日 */
    public void updateBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /** 更新註冊日期 */
    public void updateRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate.atStartOfDay();
    }

    /** 兌換獎勵積點（測試兼容方法） */
    public void redeemRewardPoints(int points, String reason) {
        if (!this.rewardPoints.canRedeem(points)) {
            throw new IllegalArgumentException("積點不足，無法兌換 " + points + " 點");
        }
        redeemPoints(points, reason);
    }

    /** 升級會員等級 */
    public void upgradeMembershipLevel(MembershipLevel newLevel) {
        // 驗證業務規則
        validateMembershipUpgrade(newLevel);

        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("membershipLevel", this.membershipLevel, newLevel,
                (oldValue, newValue) -> new MembershipLevelUpgradedEvent(this.id, oldValue, newValue));

        this.membershipLevel = newLevel;

        // 跨聚合根操作：通知促銷系統更新客戶折扣資格
        CrossAggregateOperation.publishEventIf(this,
                newLevel == MembershipLevel.VIP,
                () -> new solid.humank.genaidemo.domain.customer.model.events.CustomerVipUpgradedEvent(
                        this.id, this.membershipLevel, newLevel));
    }

    /** 驗證會員等級升級的業務規則 */
    private void validateMembershipUpgrade(MembershipLevel newLevel) {
        if (newLevel == null) {
            throw new BusinessRuleViolationException("Customer", this.id.getValue(),
                    "MEMBERSHIP_LEVEL_REQUIRED", "會員等級不能為空");
        }

        if (newLevel.ordinal() < this.membershipLevel.ordinal()) {
            throw new BusinessRuleViolationException("Customer", this.id.getValue(),
                    "MEMBERSHIP_DOWNGRADE_NOT_ALLOWED",
                    String.format("不能從 %s 降級到 %s", this.membershipLevel, newLevel));
        }
    }

    /** 檢查會員折扣資格 */
    public boolean isEligibleForMemberDiscount() {
        return membershipLevel == MembershipLevel.GOLD
                || membershipLevel == MembershipLevel.PLATINUM
                || membershipLevel == MembershipLevel.VIP;
    }

    /** 檢查生日折扣資格 */
    public boolean isEligibleForBirthdayDiscount() {
        return isBirthdayMonth();
    }

    /** 更新地址 */
    public void updateAddress(Address newAddress) {
        this.address = newAddress;
    }

    /** 更新郵箱 */
    public void updateEmail(Email newEmail) {
        this.email = newEmail;
    }

    /** 更新電話 */
    public void updatePhone(Phone newPhone) {
        this.phone = newPhone;
    }

    /** 更新消費記錄 */
    public void updateSpending(Money amount, String orderId, String spendingType) {
        // 驗證業務規則
        validateSpendingUpdate(amount, orderId, spendingType);

        Money oldTotalSpending = this.totalSpending;
        this.totalSpending = this.totalSpending.add(amount);

        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("totalSpending", oldTotalSpending, this.totalSpending,
                (oldValue, newValue) -> CustomerSpendingUpdatedEvent.create(
                        this.id, amount, newValue, orderId, spendingType));

        // 檢查是否達到會員升級條件
        checkMembershipUpgradeEligibility();
    }

    /** 驗證消費記錄更新的業務規則 */
    private void validateSpendingUpdate(Money amount, String orderId, String spendingType) {
        BusinessRuleViolationException.Builder violationBuilder = new BusinessRuleViolationException.Builder("Customer",
                this.id.getValue());

        if (amount == null) {
            violationBuilder.addError("SPENDING_AMOUNT_REQUIRED", "消費金額不能為空");
        } else if (amount.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            violationBuilder.addError("SPENDING_AMOUNT_NEGATIVE", "消費金額不能為負數");
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            violationBuilder.addError("ORDER_ID_REQUIRED", "訂單ID不能為空");
        }

        if (spendingType == null || spendingType.trim().isEmpty()) {
            violationBuilder.addError("SPENDING_TYPE_REQUIRED", "消費類型不能為空");
        }

        // 檢查是否有錯誤並拋出異常
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }

    /** 檢查會員升級資格 */
    private void checkMembershipUpgradeEligibility() {
        // 根據消費金額自動升級會員等級
        java.math.BigDecimal totalAmount = this.totalSpending.getAmount();

        if (this.membershipLevel == MembershipLevel.STANDARD &&
                totalAmount.compareTo(java.math.BigDecimal.valueOf(10000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.SILVER);
        } else if (this.membershipLevel == MembershipLevel.SILVER &&
                totalAmount.compareTo(java.math.BigDecimal.valueOf(50000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.GOLD);
        } else if (this.membershipLevel == MembershipLevel.GOLD &&
                totalAmount.compareTo(java.math.BigDecimal.valueOf(100000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.PLATINUM);
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 重建後驗證聚合根狀態
     */
    @AggregateReconstruction.PostReconstruction("驗證重建後的客戶聚合根狀態")
    public void validateReconstructedState() {
        BusinessRuleViolationException.Builder violationBuilder = new BusinessRuleViolationException.Builder("Customer",
                this.id.getValue());

        if (this.id == null) {
            violationBuilder.addError("CUSTOMER_ID_REQUIRED", "客戶ID不能為空");
        }

        if (this.name == null) {
            violationBuilder.addError("CUSTOMER_NAME_REQUIRED", "客戶姓名不能為空");
        }

        if (this.email == null) {
            violationBuilder.addError("CUSTOMER_EMAIL_REQUIRED", "客戶郵箱不能為空");
        }

        if (this.membershipLevel == null) {
            violationBuilder.addError("MEMBERSHIP_LEVEL_REQUIRED", "會員等級不能為空");
        }

        if (this.status == null) {
            violationBuilder.addError("CUSTOMER_STATUS_REQUIRED", "客戶狀態不能為空");
        }

        if (this.totalSpending == null) {
            violationBuilder.addError("TOTAL_SPENDING_REQUIRED", "總消費金額不能為空");
        }

        // 檢查是否有錯誤並拋出異常
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }

    // === 聚合根事件管理方法由 AggregateRoot interface 自動提供 ===
    // 無需 override 任何方法！所有功能都由 interface default methods 提供：
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
    // - clearEvents()
    // - getAggregateRootName()
    // - getBoundedContext()
    // - getVersion()
}
