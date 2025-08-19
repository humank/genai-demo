package solid.humank.genaidemo.infrastructure.review.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.review.model.aggregate.ReviewModeration;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaReviewModerationEntity;

/** 評價審核映射器 */
@Component
public class ReviewModerationMapper {

    /**
     * 將領域模型轉換為JPA實體
     */
    public JpaReviewModerationEntity toJpaEntity(ReviewModeration reviewModeration) {
        JpaReviewModerationEntity entity = new JpaReviewModerationEntity();
        entity.setReviewId(reviewModeration.getReviewId().value());
        entity.setModeratorId(reviewModeration.getModeratorId());
        entity.setAction(reviewModeration.getAction());
        entity.setReason(reviewModeration.getReason());
        entity.setComments(reviewModeration.getComments());
        entity.setModeratedAt(reviewModeration.getModeratedAt());
        return entity;
    }

    /**
     * 將JPA實體轉換為領域模型
     */
    public ReviewModeration toDomainModel(JpaReviewModerationEntity entity) {
        ReviewId reviewId = ReviewId.of(entity.getReviewId());

        return new ReviewModeration(
                reviewId,
                entity.getModeratorId(),
                entity.getAction(),
                entity.getReason(),
                entity.getComments());
    }
}