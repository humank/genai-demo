package solid.humank.genaidemo.domain.customer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.DeliveryAddressId;

/**
 * 配送地址 Entity
 * 
 * 管理客戶的配送地址，包含狀態管理和驗證邏輯
 */
@Entity(name = "DeliveryAddress", description = "配送地址實體，管理客戶的配送地址和相關狀態")
public class DeliveryAddress {

    private final DeliveryAddressId id;
    private final Address address;
    private String label; // 地址標籤，如 "家", "公司", "朋友家"
    private boolean isDefault;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private int usageCount; // 使用次數

    public DeliveryAddress(DeliveryAddressId id, Address address, String label) {
        this.id = Objects.requireNonNull(id, "DeliveryAddress ID cannot be null");
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.label = label != null ? label : "預設地址";
        this.isDefault = false;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = null;
        this.usageCount = 0;

        validateAddress();
    }

    // Getters
    public DeliveryAddressId getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public int getUsageCount() {
        return usageCount;
    }

    // 業務方法

    /** 設為預設地址 */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /** 取消預設地址 */
    public void unsetAsDefault() {
        this.isDefault = false;
    }

    /** 更新標籤 */
    public void updateLabel(String newLabel) {
        if (newLabel == null || newLabel.trim().isEmpty()) {
            throw new IllegalArgumentException("地址標籤不能為空");
        }
        if (newLabel.length() > 20) {
            throw new IllegalArgumentException("地址標籤不能超過20個字符");
        }
        this.label = newLabel.trim();
    }

    /** 標記為已使用 */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.usageCount++;
    }

    /** 停用地址 */
    public void deactivate() {
        this.isActive = false;
        this.isDefault = false; // 停用的地址不能是預設地址
    }

    /** 啟用地址 */
    public void activate() {
        this.isActive = true;
    }

    /** 檢查地址是否可用 */
    public boolean isAvailable() {
        return isActive;
    }

    /** 檢查是否為常用地址（使用次數 >= 3） */
    public boolean isFrequentlyUsed() {
        return usageCount >= 3;
    }

    /** 檢查是否為最近使用的地址（7天內） */
    public boolean isRecentlyUsed() {
        if (lastUsedAt == null)
            return false;
        return lastUsedAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    /** 驗證地址 */
    private void validateAddress() {
        if (address.getStreet() == null || address.getStreet().trim().isEmpty()) {
            throw new IllegalArgumentException("街道地址不能為空");
        }
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("城市不能為空");
        }
        if (address.getZipCode() == null || address.getZipCode().trim().isEmpty()) {
            throw new IllegalArgumentException("郵遞區號不能為空");
        }
    }

    /** 獲取完整地址字符串 */
    public String getFullAddressString() {
        return address.getFullAddress();
    }

    /** 檢查地址是否相同 */
    public boolean isSameAddress(Address otherAddress) {
        return this.address.equals(otherAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DeliveryAddress that = (DeliveryAddress) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("DeliveryAddress{id=%s, label='%s', address='%s', isDefault=%s, isActive=%s}",
                id, label, address.getFullAddress(), isDefault, isActive);
    }
}