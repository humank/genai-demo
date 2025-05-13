package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 客戶通知偏好值對象
 * 封裝客戶的通知偏好設置
 */
@ValueObject
public class CustomerNotificationPreference {
    private final String customerId;
    private final List<NotificationType> selectedTypes;
    private final List<NotificationChannel> selectedChannels;
    private final boolean optedOut;

    /**
     * 建立客戶通知偏好
     *
     * @param customerId 客戶ID
     * @param selectedTypes 選擇的通知類型
     * @param selectedChannels 選擇的通知渠道
     * @param optedOut 是否選擇不接收通知
     */
    public CustomerNotificationPreference(String customerId, List<NotificationType> selectedTypes, List<NotificationChannel> selectedChannels, boolean optedOut) {
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        
        if (selectedTypes == null || selectedTypes.isEmpty()) {
            throw new IllegalArgumentException("選擇的通知類型不能為空");
        }
        this.selectedTypes = Collections.unmodifiableList(selectedTypes);
        
        if (selectedChannels == null || selectedChannels.isEmpty()) {
            throw new IllegalArgumentException("選擇的通知渠道不能為空");
        }
        this.selectedChannels = Collections.unmodifiableList(selectedChannels);
        
        this.optedOut = optedOut;
    }

    /**
     * 檢查是否應該發送指定類型的通知
     *
     * @param type 通知類型
     * @return 是否應該發送
     */
    public boolean shouldSendNotificationType(NotificationType type) {
        if (optedOut) {
            return false;
        }
        
        return selectedTypes.contains(type);
    }

    /**
     * 獲取客戶偏好的通知渠道
     *
     * @return 通知渠道列表
     */
    public List<NotificationChannel> getPreferredChannels() {
        return selectedChannels;
    }

    /**
     * 獲取客戶ID
     *
     * @return 客戶ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * 獲取選擇的通知類型
     *
     * @return 通知類型列表
     */
    public List<NotificationType> getSelectedTypes() {
        return selectedTypes;
    }

    /**
     * 獲取選擇的通知渠道
     *
     * @return 通知渠道列表
     */
    public List<NotificationChannel> getSelectedChannels() {
        return selectedChannels;
    }

    /**
     * 是否選擇不接收通知
     *
     * @return 是否選擇不接收通知
     */
    public boolean isOptedOut() {
        return optedOut;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerNotificationPreference that = (CustomerNotificationPreference) o;
        return optedOut == that.optedOut &&
                Objects.equals(customerId, that.customerId) &&
                Objects.equals(selectedTypes, that.selectedTypes) &&
                Objects.equals(selectedChannels, that.selectedChannels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, selectedTypes, selectedChannels, optedOut);
    }
}