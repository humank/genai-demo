package solid.humank.genaidemo.domain.notification.service;

import java.util.List;
import java.util.Optional;
import solid.humank.genaidemo.domain.notification.model.valueobject.CustomerNotificationPreference;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 客戶通知偏好服務 負責管理客戶的通知偏好設置 */
public class CustomerNotificationPreferenceService {
    private final CustomerNotificationPreferenceRepository preferenceRepository;

    public CustomerNotificationPreferenceService(
            CustomerNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * 獲取客戶通知偏好
     *
     * @param customerId 客戶ID
     * @return 客戶通知偏好
     */
    public Optional<CustomerNotificationPreference> getCustomerPreference(String customerId) {
        return preferenceRepository.findByCustomerId(customerId);
    }

    /**
     * 更新客戶通知偏好
     *
     * @param customerId 客戶ID
     * @param selectedTypes 選擇的通知類型
     * @param selectedChannels 選擇的通知渠道
     * @return 是否更新成功
     */
    public boolean updateCustomerPreference(
            String customerId,
            List<NotificationType> selectedTypes,
            List<NotificationChannel> selectedChannels) {
        CustomerNotificationPreference preference =
                new CustomerNotificationPreference(
                        customerId, selectedTypes, selectedChannels, false);

        return preferenceRepository.save(preference);
    }

    /**
     * 選擇不接收通知
     *
     * @param customerId 客戶ID
     * @return 是否設置成功
     */
    public boolean optOut(String customerId) {
        Optional<CustomerNotificationPreference> existingPreference =
                preferenceRepository.findByCustomerId(customerId);

        if (existingPreference.isPresent()) {
            CustomerNotificationPreference preference = existingPreference.get();
            CustomerNotificationPreference updatedPreference =
                    new CustomerNotificationPreference(
                            customerId,
                            preference.getSelectedTypes(),
                            preference.getSelectedChannels(),
                            true);

            return preferenceRepository.save(updatedPreference);
        } else {
            // 如果客戶沒有設置偏好，創建一個默認的並設置為選擇不接收
            CustomerNotificationPreference preference =
                    new CustomerNotificationPreference(
                            customerId,
                            List.of(NotificationType.values()),
                            List.of(NotificationChannel.EMAIL),
                            true);

            return preferenceRepository.save(preference);
        }
    }

    /**
     * 重新接收通知
     *
     * @param customerId 客戶ID
     * @return 是否設置成功
     */
    public boolean optIn(String customerId) {
        Optional<CustomerNotificationPreference> existingPreference =
                preferenceRepository.findByCustomerId(customerId);

        if (existingPreference.isPresent()) {
            CustomerNotificationPreference preference = existingPreference.get();
            CustomerNotificationPreference updatedPreference =
                    new CustomerNotificationPreference(
                            customerId,
                            preference.getSelectedTypes(),
                            preference.getSelectedChannels(),
                            false);

            return preferenceRepository.save(updatedPreference);
        } else {
            // 如果客戶沒有設置偏好，創建一個默認的
            CustomerNotificationPreference preference =
                    new CustomerNotificationPreference(
                            customerId,
                            List.of(NotificationType.values()),
                            List.of(NotificationChannel.EMAIL),
                            false);

            return preferenceRepository.save(preference);
        }
    }

    /**
     * 檢查是否應該發送指定類型的通知
     *
     * @param customerId 客戶ID
     * @param type 通知類型
     * @return 是否應該發送
     */
    public boolean shouldSendNotificationType(String customerId, NotificationType type) {
        Optional<CustomerNotificationPreference> preference =
                preferenceRepository.findByCustomerId(customerId);

        return preference
                .map(p -> p.shouldSendNotificationType(type))
                .orElse(true); // 如果沒有設置偏好，默認發送
    }

    /**
     * 獲取客戶偏好的通知渠道
     *
     * @param customerId 客戶ID
     * @return 通知渠道列表
     */
    public List<NotificationChannel> getPreferredChannels(String customerId) {
        Optional<CustomerNotificationPreference> preference =
                preferenceRepository.findByCustomerId(customerId);

        return preference
                .map(CustomerNotificationPreference::getPreferredChannels)
                .orElse(List.of(NotificationChannel.EMAIL)); // 如果沒有設置偏好，默認使用郵件
    }
}
