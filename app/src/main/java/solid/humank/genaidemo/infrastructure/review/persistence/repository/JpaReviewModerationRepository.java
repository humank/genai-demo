package solid.humank.genaidemo.infrastructure.review.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaReviewModerationEntity;

/** 評價審核JPA儲存庫 */
@Repository
public interface JpaReviewModerationRepository extends JpaRepository<JpaReviewModerationEntity, String> {

    /**
     * 根據評價ID查詢審核記錄
     */
    List<JpaReviewModerationEntity> findByReviewId(String reviewId);

    /**
     * 根據審核員ID查詢審核記錄
     */
    List<JpaReviewModerationEntity> findByModeratorId(String moderatorId);

    /**
     * 根據審核動作查詢記錄
     */
    List<JpaReviewModerationEntity> findByAction(String action);
}