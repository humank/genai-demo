package solid.humank.genaidemo.domain.review.repository;

import java.util.List;

import solid.humank.genaidemo.domain.review.model.aggregate.ReviewModeration;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;

/** 評價審核儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "ReviewModerationRepository", description = "評價審核聚合根儲存庫")
public interface ReviewModerationRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<ReviewModeration, ReviewId> {

    /**
     * 根據評價ID查詢審核記錄
     * 
     * @param reviewId 評價ID
     * @return 審核記錄列表
     */
    List<ReviewModeration> findByReviewId(ReviewId reviewId);

    /**
     * 根據審核員ID查詢審核記錄
     * 
     * @param moderatorId 審核員ID
     * @return 審核記錄列表
     */
    List<ReviewModeration> findByModeratorId(String moderatorId);

    /**
     * 根據審核動作查詢記錄
     * 
     * @param action 審核動作
     * @return 審核記錄列表
     */
    List<ReviewModeration> findByAction(String action);
}