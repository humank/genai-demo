package solid.humank.genaidemo.domain.seller.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.seller.model.valueobject.ContactInfoId;

/** 聯繫資訊實體 */
@Entity(name = "ContactInfo", description = "聯繫資訊實體")
public class ContactInfo {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private final ContactInfoId id;
    private String email;
    private String phone;
    private String preferredContactMethod;
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime lastUpdated;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime phoneVerifiedAt;

    public ContactInfo(ContactInfoId id, String email, String phone) {
        this.id = Objects.requireNonNull(id, "聯繫資訊ID不能為空");
        this.email = validateEmail(email);
        this.phone = validatePhone(phone);
        this.preferredContactMethod = "EMAIL";
        this.emailVerified = false;
        this.phoneVerified = false;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 重建用建構子（用於從持久化層重建）
     */
    public ContactInfo(ContactInfoId id, String email, String phone, String preferredContactMethod,
            boolean emailVerified, boolean phoneVerified, LocalDateTime lastUpdated,
            LocalDateTime emailVerifiedAt, LocalDateTime phoneVerifiedAt) {
        this.id = Objects.requireNonNull(id, "聯繫資訊ID不能為空");
        this.email = email;
        this.phone = phone;
        this.preferredContactMethod = preferredContactMethod != null ? preferredContactMethod : "EMAIL";
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
        this.emailVerifiedAt = emailVerifiedAt;
        this.phoneVerifiedAt = phoneVerifiedAt;
    }

    // 業務邏輯方法

    /**
     * 更新聯繫資訊
     */
    public void updateContactInfo(String email, String phone) {
        String newEmail = validateEmail(email);
        String newPhone = validatePhone(phone);

        // 如果郵箱或電話變更，需要重新驗證
        if (!Objects.equals(this.email, newEmail)) {
            this.emailVerified = false;
            this.emailVerifiedAt = null;
        }

        if (!Objects.equals(this.phone, newPhone)) {
            this.phoneVerified = false;
            this.phoneVerifiedAt = null;
        }

        this.email = newEmail;
        this.phone = newPhone;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 驗證郵箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 驗證電話
     */
    public void verifyPhone() {
        this.phoneVerified = true;
        this.phoneVerifiedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 設置偏好的聯繫方式
     */
    public void setPreferredContactMethod(String method) {
        if (!"EMAIL".equals(method) && !"PHONE".equals(method)) {
            throw new IllegalArgumentException("聯繫方式必須是 EMAIL 或 PHONE");
        }
        this.preferredContactMethod = method;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 檢查是否完全驗證
     */
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified;
    }

    /**
     * 檢查是否有有效的聯繫方式
     */
    public boolean hasValidContactMethod() {
        return (email != null && emailVerified) || (phone != null && phoneVerified);
    }

    // 私有驗證方法

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("郵箱不能為空");
        }
        email = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("郵箱格式不正確");
        }
        return email;
    }

    private String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("電話不能為空");
        }
        phone = phone.trim().replaceAll("[\\s-()]", "");
        if (!phone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("電話格式不正確");
        }
        return phone;
    }

    // Getters

    public ContactInfoId getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public LocalDateTime getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContactInfo that = (ContactInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ContactInfo{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", emailVerified=" + emailVerified +
                ", phoneVerified=" + phoneVerified +
                '}';
    }
}