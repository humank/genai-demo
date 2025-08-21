package solid.humank.genaidemo.infrastructure.seller.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerEntity;

/** 賣家JPA儲存庫 */
@Repository
public interface JpaSellerRepository extends JpaRepository<JpaSellerEntity, String> {

    Optional<JpaSellerEntity> findByEmail(String email);

    Optional<JpaSellerEntity> findByPhone(String phone);

    List<JpaSellerEntity> findByIsActiveTrue();

    List<JpaSellerEntity> findByNameContainingIgnoreCase(String name);

    // 從 SellerProfileRepository 遷移的查詢方法

    List<JpaSellerEntity> findByIsVerifiedTrue();

    List<JpaSellerEntity> findByRatingBetween(double minRating, double maxRating);

    List<JpaSellerEntity> findByVerificationStatusContaining(String verificationStatus);

    Optional<JpaSellerEntity> findByBusinessLicense(String businessLicense);
}