package solid.humank.genaidemo.domain.review.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;

/** 評價審核聚合根 */
@AggregateRoot(name = "ReviewModeration", description = "評價審核聚合根，管理評價的審核流程", boundedContext = "Review", version = "1.0")
public class ReviewModeration extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final ReviewId reviewId;
    private final String moderatorId;
    private final String action; // APPROVE, REJECT, HIDE
    private final String reason;
    private final LocalDateTime moderatedAt;
    private final String comments;

    public ReviewModeration(ReviewId reviewId, String moderatorId, String action, String reason, String comments) {
        this.reviewId = Objects.requireNonNull(reviewId, "評價ID不能為空");
        this.moderatorId = Objects.requireNonNull(moderatorId, "審核員ID不能為空");
        this.action = Objects.requireNonNull(action, "審核動作不能為空");
        this.reason = reason;
        this.comments = comments;
        this.moderatedAt = LocalDateTime.now();
    }

    // Getters
    public ReviewId getReviewId() {
        return reviewId;
    }

    public String getModeratorId() {
        return moderatorId;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public String getComments() {
        return comments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReviewModeration that = (ReviewModeration) o;
        return Objects.equals(reviewId, that.reviewId) && Objects.equals(moderatorId, that.moderatorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId, moderatorId);
    }
}