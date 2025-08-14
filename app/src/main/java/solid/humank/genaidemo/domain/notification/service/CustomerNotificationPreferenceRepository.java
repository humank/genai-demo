package solid.humank.genaidemo.domain.notification.service;

import java.util.Optional;
import solid.humank.genaidemo.domain.notification.model.valueobject.CustomerNotificationPreference;

/** 客戶通知偏好儲存庫接口 */
public interface CustomerNotificationPreferenceRepository {

    /**
     * 根據客戶ID查詢通知偏好
     *
     * @param customerId 客戶ID
     * @return 通知偏好
     */
    Optional<CustomerNotificationPreference> findByCustomerId(String customerId);

    /**
     * 保存通知偏好
     *
     * @param preference 通知偏好
     * @return 是否保存成功
     */
    boolean save(CustomerNotificationPreference preference);

    /**
     * 刪除通知偏好
     *
     * @param customerId 客戶ID
     * @return 是否刪除成功
     */
    boolean delete(String customerId);
}
