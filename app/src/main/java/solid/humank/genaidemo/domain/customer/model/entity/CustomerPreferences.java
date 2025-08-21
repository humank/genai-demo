package solid.humank.genaidemo.domain.customer.model.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerPreferencesId;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;

/**
 * 客戶偏好設定 Entity
 * 
 * 統一管理客戶的各種偏好設定，包括通知偏好、購物偏好等
 */
@Entity(name = "CustomerPreferences", description = "客戶偏好設定實體，統一管理客戶的各種偏好設定")
public class CustomerPreferences {

    private final CustomerPreferencesId id;
    private NotificationPreferences notificationPreferences;
    private String preferredLanguage;
    private String preferredCurrency;
    private boolean enablePersonalizedRecommendations;
    private boolean enableMarketingEmails;
    private boolean enableSmsNotifications;
    private boolean enablePushNotifications;
    private Map<String, String> customPreferences; // 自定義偏好設定
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    public CustomerPreferences(CustomerPreferencesId id) {
        this.id = Objects.requireNonNull(id, "CustomerPreferences ID cannot be null");
        this.notificationPreferences = NotificationPreferences.defaultPreferences();
        this.preferredLanguage = "zh-TW";
        this.preferredCurrency = "TWD";
        this.enablePersonalizedRecommendations = true;
        this.enableMarketingEmails = false;
        this.enableSmsNotifications = true;
        this.enablePushNotifications = true;
        this.customPreferences = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters
    public CustomerPreferencesId getId() {
        return id;
    }

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getPreferredCurrency() {
        return preferredCurrency;
    }

    public boolean isPersonalizedRecommendationsEnabled() {
        return enablePersonalizedRecommendations;
    }

    public boolean isMarketingEmailsEnabled() {
        return enableMarketingEmails;
    }

    public boolean isSmsNotificationsEnabled() {
        return enableSmsNotifications;
    }

    public boolean isPushNotificationsEnabled() {
        return enablePushNotifications;
    }

    public Map<String, String> getCustomPreferences() {
        return new HashMap<>(customPreferences);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    // 業務方法

    /** 更新通知偏好 */
    public void updateNotificationPreferences(NotificationPreferences newPreferences) {
        this.notificationPreferences = Objects.requireNonNull(newPreferences,
                "Notification preferences cannot be null");
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 更新偏好語言 */
    public void updatePreferredLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("偏好語言不能為空");
        }
        if (!isValidLanguageCode(language)) {
            throw new IllegalArgumentException("無效的語言代碼: " + language);
        }
        this.preferredLanguage = language;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 更新偏好貨幣 */
    public void updatePreferredCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("偏好貨幣不能為空");
        }
        if (!isValidCurrencyCode(currency)) {
            throw new IllegalArgumentException("無效的貨幣代碼: " + currency);
        }
        this.preferredCurrency = currency;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 啟用/停用個人化推薦 */
    public void setPersonalizedRecommendations(boolean enabled) {
        this.enablePersonalizedRecommendations = enabled;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 啟用/停用行銷郵件 */
    public void setMarketingEmails(boolean enabled) {
        this.enableMarketingEmails = enabled;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 啟用/停用簡訊通知 */
    public void setSmsNotifications(boolean enabled) {
        this.enableSmsNotifications = enabled;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 啟用/停用推播通知 */
    public void setPushNotifications(boolean enabled) {
        this.enablePushNotifications = enabled;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 設定自定義偏好 */
    public void setCustomPreference(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("偏好設定鍵不能為空");
        }
        if (value == null) {
            this.customPreferences.remove(key);
        } else {
            this.customPreferences.put(key, value);
        }
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 獲取自定義偏好 */
    public String getCustomPreference(String key) {
        return customPreferences.get(key);
    }

    /** 移除自定義偏好 */
    public void removeCustomPreference(String key) {
        this.customPreferences.remove(key);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 批量更新偏好設定 */
    public void updatePreferences(
            String language,
            String currency,
            boolean personalizedRecommendations,
            boolean marketingEmails,
            boolean smsNotifications,
            boolean pushNotifications) {

        if (language != null)
            updatePreferredLanguage(language);
        if (currency != null)
            updatePreferredCurrency(currency);

        this.enablePersonalizedRecommendations = personalizedRecommendations;
        this.enableMarketingEmails = marketingEmails;
        this.enableSmsNotifications = smsNotifications;
        this.enablePushNotifications = pushNotifications;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 檢查是否允許接收通知 */
    public boolean canReceiveNotifications() {
        return enableSmsNotifications || enablePushNotifications ||
                notificationPreferences.shouldSendVia(
                        solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel.EMAIL);
    }

    /** 檢查是否允許接收行銷內容 */
    public boolean canReceiveMarketing() {
        return enableMarketingEmails;
    }

    /** 驗證語言代碼 */
    private boolean isValidLanguageCode(String language) {
        // 簡單的語言代碼驗證
        return language.matches("^[a-z]{2}(-[A-Z]{2})?$");
    }

    /** 驗證貨幣代碼 */
    private boolean isValidCurrencyCode(String currency) {
        // 簡單的貨幣代碼驗證
        return currency.matches("^[A-Z]{3}$");
    }

    /** 重置為預設設定 */
    public void resetToDefaults() {
        this.notificationPreferences = NotificationPreferences.defaultPreferences();
        this.preferredLanguage = "zh-TW";
        this.preferredCurrency = "TWD";
        this.enablePersonalizedRecommendations = true;
        this.enableMarketingEmails = false;
        this.enableSmsNotifications = true;
        this.enablePushNotifications = true;
        this.customPreferences.clear();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CustomerPreferences that = (CustomerPreferences) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("CustomerPreferences{id=%s, language='%s', currency='%s', personalizedRecommendations=%s}",
                id, preferredLanguage, preferredCurrency, enablePersonalizedRecommendations);
    }
}