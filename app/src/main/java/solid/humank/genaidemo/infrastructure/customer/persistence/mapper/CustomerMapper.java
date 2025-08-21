package solid.humank.genaidemo.infrastructure.customer.persistence.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.customer.persistence.entity.JpaCustomerEntity;

/**
 * 客戶領域模型與 JPA 實體之間的映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
@Component
public class CustomerMapper implements DomainMapper<Customer, JpaCustomerEntity> {

    @Override
    public JpaCustomerEntity toJpaEntity(Customer customer) {
        if (customer == null) {
            return null;
        }

        JpaCustomerEntity entity = new JpaCustomerEntity();
        entity.setId(customer.getId().getValue());
        entity.setName(customer.getName().getName());
        entity.setEmail(customer.getEmail().getEmail());
        entity.setPhone(customer.getPhone().getPhone());
        entity.setBirthDate(customer.getBirthDate());
        entity.setRegistrationDate(customer.getRegistrationDate().toLocalDate());
        entity.setMembershipLevel(customer.getMembershipLevel().name());
        entity.setRewardPointsBalance(customer.getRewardPoints().balance());
        entity.setRewardPointsLastUpdated(LocalDateTime.now());

        // 處理通知偏好 - 簡化處理，使用默認值
        if (customer.getNotificationPreferences() != null) {
            entity.setNotificationEnabledTypes("EMAIL,SMS");
            entity.setNotificationEnabledChannels("EMAIL,SMS");
            entity.setMarketingEnabled(true);
        }

        // 處理地址
        if (customer.getAddress() != null) {
            entity.setAddressCity(customer.getAddress().getCity());
            entity.setAddressDistrict(""); // Address 沒有 district，使用空字符串
            entity.setAddressStreet(customer.getAddress().getStreet());
            entity.setAddressPostalCode(customer.getAddress().getZipCode());
        }

        return entity;
    }

    @Override
    public Customer toDomainModel(JpaCustomerEntity entity) {
        if (entity == null) {
            return null;
        }

        // 創建基本值對象
        CustomerId customerId = CustomerId.of(entity.getId());
        CustomerName customerName = new CustomerName(entity.getName());
        Email email = new Email(entity.getEmail());
        Phone phone = new Phone(entity.getPhone());
        MembershipLevel membershipLevel = MembershipLevel.valueOf(entity.getMembershipLevel());

        // 創建地址
        Address address = null;
        if (entity.getAddressCity() != null) {
            address = new Address(
                    entity.getAddressStreet() != null ? entity.getAddressStreet() : "",
                    entity.getAddressCity(),
                    entity.getAddressPostalCode() != null ? entity.getAddressPostalCode() : "",
                    "Taiwan" // 默認國家
            );
        }

        // 創建客戶聚合根
        Customer customer = new Customer(
                customerId,
                customerName,
                email,
                phone,
                address,
                membershipLevel,
                entity.getBirthDate(),
                entity.getRegistrationDate().atStartOfDay());

        // 設置獎勵積分
        if (entity.getRewardPointsBalance() != null && entity.getRewardPointsBalance() > 0) {
            customer.addRewardPoints(entity.getRewardPointsBalance(), "Initial load from database");
        }

        // 設置通知偏好 - 使用默認偏好
        if (entity.getMarketingEnabled() != null) {
            NotificationPreferences preferences = NotificationPreferences.defaultPreferences();
            customer.updateNotificationPreferences(preferences);
        }

        // 清除在重建過程中產生的事件，因為這些不是新的業務事件
        customer.markEventsAsCommitted();

        return customer;
    }
}