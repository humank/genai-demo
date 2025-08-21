package solid.humank.genaidemo.infrastructure.promotion.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaVoucherEntity;

/** 優惠券JPA儲存庫 */
@Repository
public interface JpaVoucherRepository extends JpaRepository<JpaVoucherEntity, String> {

    Optional<JpaVoucherEntity> findByRedemptionCode(String redemptionCode);

    List<JpaVoucherEntity> findByOwnerId(String customerId);

    List<JpaVoucherEntity> findByExpiresAtAfterAndStatus(LocalDateTime expirationDate, String status);

}