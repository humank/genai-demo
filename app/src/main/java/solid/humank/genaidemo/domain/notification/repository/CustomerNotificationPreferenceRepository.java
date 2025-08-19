package solid.humank.genaidemo.domain.notification.repository;

import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.notification.model.valueobject.CustomerNotificationPreference;

/** 客戶通知偏好儲存庫接口 */
@Repository(name = "CustomerNotificationPreferenceRepository", description = "客戶通知偏好儲存庫")
public interface CustomerNotificationPreferenceRepository
        extends solid.humank.genaidemo.domain.common.repository.Repository<CustomerNotificationPreference, String> {

    /**
     * 根據客戶ID查詢通知偏好
     *
     * @param customerId 客戶ID
     * @return 通知偏好
     */
    Optional<CustomerNotificationPreference> findByCustomerId(String customerId);

    /**
     * 根據客戶ID刪除通知偏好
     *
     * @param customerId 客戶ID
     */
    void deleteByCustomerId(String customerId);
}
