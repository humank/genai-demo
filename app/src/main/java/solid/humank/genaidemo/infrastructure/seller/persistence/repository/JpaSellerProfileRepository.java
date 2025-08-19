package solid.humank.genaidemo.infrastructure.seller.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerProfileEntity;

/** 賣家檔案JPA儲存庫 */
@Repository
public interface JpaSellerProfileRepository extends JpaRepository<JpaSellerProfileEntity, String> {

    Optional<JpaSellerProfileEntity> findBySellerId(String sellerId);

    List<JpaSellerProfileEntity> findByIsVerifiedTrue();

    List<JpaSellerProfileEntity> findByRatingBetween(double minRating, double maxRating);

    List<JpaSellerProfileEntity> findByVerificationStatusContaining(String verificationStatus);
}