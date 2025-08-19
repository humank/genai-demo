package solid.humank.genaidemo.infrastructure.review.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaProductReviewEntity;

/** 商品評價JPA儲存庫 */
@Repository
public interface JpaProductReviewRepository extends JpaRepository<JpaProductReviewEntity, String> {

    /**
     * 根據產品ID查詢評價
     */
    List<JpaProductReviewEntity> findByProductId(String productId);

    /**
     * 根據評價者ID查詢評價
     */
    List<JpaProductReviewEntity> findByReviewerId(String reviewerId);

    /**
     * 根據狀態查詢評價
     */
    List<JpaProductReviewEntity> findByStatus(String status);

    /**
     * 根據產品ID和評價者ID查詢評價
     */
    Optional<JpaProductReviewEntity> findByProductIdAndReviewerId(String productId, String reviewerId);

    /**
     * 查詢被檢舉的評價
     */
    List<JpaProductReviewEntity> findByIsReportedTrue();
}