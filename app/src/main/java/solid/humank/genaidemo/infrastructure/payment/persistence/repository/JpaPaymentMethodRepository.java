package solid.humank.genaidemo.infrastructure.payment.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentMethodEntity;

/** 支付方式JPA儲存庫 */
@Repository
public interface JpaPaymentMethodRepository extends JpaRepository<JpaPaymentMethodEntity, String> {

    List<JpaPaymentMethodEntity> findByCustomerId(String customerId);

    Optional<JpaPaymentMethodEntity> findByCustomerIdAndIsDefaultTrue(String customerId);

    List<JpaPaymentMethodEntity> findByCustomerIdAndType(String customerId, String type);

    List<JpaPaymentMethodEntity> findByCustomerIdAndIsActiveTrue(String customerId);

    List<JpaPaymentMethodEntity> findByProvider(String provider);
}