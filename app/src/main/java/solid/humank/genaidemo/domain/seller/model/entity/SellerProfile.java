package solid.humank.genaidemo.domain.seller.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerProfileId;

/** 賣家檔案實體 - 從聚合根轉換而來 */
@Entity(name = "SellerProfile", description = "賣家檔案實體")
public class SellerProfile {

    private final SellerProfileId id;
    private String businessName;
    private String businessAddress;
    private String businessLicense;
    private String description;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime lastUpdated;

    public SellerProfile(SellerProfileId id, String businessName, String businessAddress, String businessLicense) {
        this.id = Objects.requireNonNull(id, "賣家檔案ID不能為空");
        this.businessName = Objects.requireNonNull(businessName, "商業名稱不能為空");
        this.businessAddress = businessAddress;
        this.businessLicense = businessLicense;
        this.joinedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 重建用建構子（用於從持久化層重建）
     */
    public SellerProfile(SellerProfileId id, String businessName, String businessAddress,
            String businessLicense, String description, LocalDateTime joinedAt,
            LocalDateTime lastActiveAt, LocalDateTime lastUpdated) {
        this.id = Objects.requireNonNull(id, "賣家檔案ID不能為空");
        this.businessName = Objects.requireNonNull(businessName, "商業名稱不能為空");
        this.businessAddress = businessAddress;
        this.businessLicense = businessLicense;
        this.description = description;
        this.joinedAt = joinedAt != null ? joinedAt : LocalDateTime.now();
        this.lastActiveAt = lastActiveAt != null ? lastActiveAt : LocalDateTime.now();
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    // 業務邏輯方法

    /**
     * 更新商業資訊
     */
    public void updateBusinessInfo(String businessName, String businessAddress, String description) {
        this.businessName = Objects.requireNonNull(businessName, "商業名稱不能為空");
        this.businessAddress = businessAddress;
        this.description = description;
        this.lastActiveAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 檢查檔案是否完整
     */
    public boolean isProfileComplete() {
        return businessName != null && !businessName.trim().isEmpty() &&
                businessAddress != null && !businessAddress.trim().isEmpty() &&
                businessLicense != null && !businessLicense.trim().isEmpty();
    }

    /**
     * 驗證商業執照
     */
    public void validateBusinessLicense() {
        if (businessLicense == null || businessLicense.trim().isEmpty()) {
            throw new IllegalStateException("商業執照不能為空");
        }
        // 這裡可以添加更複雜的驗證邏輯
    }

    /**
     * 標記為活躍
     */
    public void markAsActive() {
        this.lastActiveAt = LocalDateTime.now();
    }

    // Getters

    public SellerProfileId getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public String getBusinessLicense() {
        return businessLicense;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SellerProfile that = (SellerProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SellerProfile{" +
                "id=" + id +
                ", businessName='" + businessName + '\'' +
                ", businessAddress='" + businessAddress + '\'' +
                ", isComplete=" + isProfileComplete() +
                '}';
    }
}