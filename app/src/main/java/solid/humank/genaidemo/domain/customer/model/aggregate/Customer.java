package solid.humank.genaidemo.domain.customer.model.aggregate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerStatus;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.customer.model.valueobject.RewardPoints;

/** 增強的客戶聚合根 */
@AggregateRoot(name = "Customer", description = "增強的客戶聚合根，支援完整的消費者功能")
public class Customer {

    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private Phone phone;
    private Address address;
    private MembershipLevel membershipLevel;

    // 新增屬性
    private LocalDate birthDate;
    private LocalDateTime registrationDate;
    private RewardPoints rewardPoints;
    private NotificationPreferences notificationPreferences;
    private List<Address> deliveryAddresses;
    private CustomerStatus status;

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
        this.notificationPreferences = NotificationPreferences.defaultPreferences();
        this.deliveryAddresses = new ArrayList<>();
        this.status = CustomerStatus.ACTIVE;
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

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public List<Address> getDeliveryAddresses() {
        return new ArrayList<>(deliveryAddresses);
    }

    public CustomerStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return id.equals(customer.id);
    }

    // 業務方法

    /** 檢查是否為新會員（註冊30天內） */
    public boolean isNewMember() {
        if (registrationDate == null) return false;
        return registrationDate.isAfter(LocalDateTime.now().minusDays(30));
    }

    /** 檢查是否為生日月 */
    public boolean isBirthdayMonth() {
        if (birthDate == null) return false;
        return birthDate.getMonth() == LocalDate.now().getMonth();
    }

    /** 兌換紅利點數 */
    public void redeemPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.redeem(points);
        // TODO: 實現事件發布機制
        // registerEvent(new RewardPointsRedeemedEvent(this.id, points, reason));
    }

    /** 添加紅利點數 */
    public void addRewardPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.add(points);
        // TODO: 實現事件發布機制
        // registerEvent(new RewardPointsEarnedEvent(this.id, points, reason));
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
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
        // TODO: 實現事件發布機制
        // registerEvent(new CustomerProfileUpdatedEvent(this.id, newName, newEmail,
        // newPhone));
    }

    /** 更新通知偏好 */
    public void updateNotificationPreferences(NotificationPreferences preferences) {
        this.notificationPreferences = preferences;
        // TODO: 實現事件發布機制
        // registerEvent(new NotificationPreferencesUpdatedEvent(this.id, preferences));
    }

    /** 添加配送地址 */
    public void addDeliveryAddress(Address address) {
        if (!deliveryAddresses.contains(address)) {
            deliveryAddresses.add(address);
        }
    }

    /** 移除配送地址 */
    public void removeDeliveryAddress(Address address) {
        deliveryAddresses.remove(address);
    }

    /** 更新客戶狀態 */
    public void updateStatus(CustomerStatus newStatus) {
        this.status = newStatus;
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
        if (newLevel.ordinal() < this.membershipLevel.ordinal()) {
            throw new IllegalArgumentException("不能降級會員等級");
        }
        this.membershipLevel = newLevel;
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

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
