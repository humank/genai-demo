package solid.humank.genaidemo.infrastructure.customer.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * 客戶 JPA 實體 用於與數據庫交互的實體類
 *
 * <p>
 * 注意：這個類僅作為持久化的技術實現，不包含業務邏輯 所有業務邏輯都應該在Customer聚合根中實現
 */
@Entity
@Table(name = "customers")
public class JpaCustomerEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "membership_level", nullable = false)
    private String membershipLevel;

    @Column(name = "reward_points_balance", nullable = false)
    private Integer rewardPointsBalance;

    @Column(name = "reward_points_last_updated", nullable = false)
    private LocalDateTime rewardPointsLastUpdated;

    @Column(name = "notification_enabled_types", columnDefinition = "TEXT")
    private String notificationEnabledTypes;

    @Column(name = "notification_enabled_channels", columnDefinition = "TEXT")
    private String notificationEnabledChannels;

    @Column(name = "marketing_enabled", nullable = false)
    private Boolean marketingEnabled;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_district")
    private String addressDistrict;

    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_postal_code")
    private String addressPostalCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 默認建構子，JPA 需要
    public JpaCustomerEntity() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getMembershipLevel() {
        return membershipLevel;
    }

    public void setMembershipLevel(String membershipLevel) {
        this.membershipLevel = membershipLevel;
    }

    public Integer getRewardPointsBalance() {
        return rewardPointsBalance;
    }

    public void setRewardPointsBalance(Integer rewardPointsBalance) {
        this.rewardPointsBalance = rewardPointsBalance;
    }

    public LocalDateTime getRewardPointsLastUpdated() {
        return rewardPointsLastUpdated;
    }

    public void setRewardPointsLastUpdated(LocalDateTime rewardPointsLastUpdated) {
        this.rewardPointsLastUpdated = rewardPointsLastUpdated;
    }

    public String getNotificationEnabledTypes() {
        return notificationEnabledTypes;
    }

    public void setNotificationEnabledTypes(String notificationEnabledTypes) {
        this.notificationEnabledTypes = notificationEnabledTypes;
    }

    public String getNotificationEnabledChannels() {
        return notificationEnabledChannels;
    }

    public void setNotificationEnabledChannels(String notificationEnabledChannels) {
        this.notificationEnabledChannels = notificationEnabledChannels;
    }

    public Boolean getMarketingEnabled() {
        return marketingEnabled;
    }

    public void setMarketingEnabled(Boolean marketingEnabled) {
        this.marketingEnabled = marketingEnabled;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (registrationDate == null) {
            registrationDate = now.toLocalDate();
        }
        if (rewardPointsLastUpdated == null) {
            rewardPointsLastUpdated = now;
        }
        if (rewardPointsBalance == null) {
            rewardPointsBalance = 0;
        }
        if (marketingEnabled == null) {
            marketingEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
