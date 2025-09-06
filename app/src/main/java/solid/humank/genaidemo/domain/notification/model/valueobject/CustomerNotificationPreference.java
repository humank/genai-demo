package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.List;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 客戶通知偏好值對象 封裝客戶的通知偏好設置 */
@ValueObject
public record CustomerNotificationPreference(
        String customerId,
        List<NotificationType> selectedTypes,
        List<NotificationChannel> selectedChannels,
        boolean optedOut) {

    /**
     * 建立客戶通知偏好
     */
    public CustomerNotificationPreference {
        Objects.requireNonNull(customerId, "客戶ID不能為空");
        Objects.requireNonNull(selectedTypes, "選擇的通知類型不能為空");
        Objects.requireNonNull(selectedChannels, "選擇的通知渠道不能為空");

        if (customerId.isBlank()) {
            throw new IllegalArgumentException("客戶ID不能為空字符串");
        }
        if (selectedTypes.isEmpty()) {
            throw new IllegalArgumentException("選擇的通知類型不能為空");
        }
        if (selectedChannels.isEmpty()) {
            throw new IllegalArgumentException("選擇的通知渠道不能為空");
        }

        // Make defensive copies to ensure immutability
        selectedTypes = List.copyOf(selectedTypes);
        selectedChannels = List.copyOf(selectedChannels);
    }

    public static CustomerNotificationPreference create(
            String customerId,
            List<NotificationType> selectedTypes,
            List<NotificationChannel> selectedChannels,
            boolean optedOut) {
        return new CustomerNotificationPreference(customerId, selectedTypes, selectedChannels, optedOut);
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CustomerNotificationPreference that = (CustomerNotificationPreference) o;
        return optedOut == that.optedOut
                && Objects.equals(customerId, that.customerId)
                && Objects.equals(selectedTypes, that.selectedTypes)
                && Objects.equals(selectedChannels, that.selectedChannels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, selectedTypes, selectedChannels, optedOut);
    }
}
