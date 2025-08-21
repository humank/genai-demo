package solid.humank.genaidemo.domain.seller.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.seller.model.valueobject.RatingStatus;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerRatingId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 賣家評級實體 */
@Entity(name = "SellerRating", description = "賣家評級實體")
public class SellerRating {

    private final SellerRatingId id;
    private final CustomerId customerId;
    private int rating;
    private String comment;
    private LocalDateTime ratedAt;
    private LocalDateTime lastModified;
    private RatingStatus status;
    private String moderatorComment;

    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment) {
        this.id = Objects.requireNonNull(id, "評級ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.rating = validateRating(rating);
        this.comment = comment != null ? comment.trim() : "";
        this.ratedAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.status = RatingStatus.ACTIVE;
    }

    /**
     * 重建用建構子（用於從持久化層重建）
     */
    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment,
            LocalDateTime ratedAt, LocalDateTime lastModified, RatingStatus status,
            String moderatorComment) {
        this.id = Objects.requireNonNull(id, "評級ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.rating = rating;
        this.comment = comment != null ? comment : "";
        this.ratedAt = ratedAt != null ? ratedAt : LocalDateTime.now();
        this.lastModified = lastModified != null ? lastModified : LocalDateTime.now();
        this.status = status != null ? status : RatingStatus.ACTIVE;
        this.moderatorComment = moderatorComment;
    }

    // 業務邏輯方法

    /**
     * 更新評級
     */
    public void updateRating(int newRating, String newComment) {
        if (status != RatingStatus.ACTIVE) {
            throw new IllegalStateException("只有活躍狀態的評級可以更新");
        }

        this.rating = validateRating(newRating);
        this.comment = newComment != null ? newComment.trim() : "";
        this.lastModified = LocalDateTime.now();
    }

    /**
     * 隱藏評級
     */
    public void hide(String moderatorComment) {
        this.status = RatingStatus.HIDDEN;
        this.moderatorComment = moderatorComment;
        this.lastModified = LocalDateTime.now();
    }

    /**
     * 恢復評級
     */
    public void restore(String moderatorComment) {
        if (status == RatingStatus.DELETED) {
            throw new IllegalStateException("已刪除的評級無法恢復");
        }

        this.status = RatingStatus.ACTIVE;
        this.moderatorComment = moderatorComment;
        this.lastModified = LocalDateTime.now();
    }

    /**
     * 刪除評級
     */
    public void delete(String moderatorComment) {
        this.status = RatingStatus.DELETED;
        this.moderatorComment = moderatorComment;
        this.lastModified = LocalDateTime.now();
    }

    /**
     * 檢查是否可見
     */
    public boolean isVisible() {
        return status == RatingStatus.ACTIVE;
    }

    /**
     * 檢查是否可以修改
     */
    public boolean canBeModified() {
        return status == RatingStatus.ACTIVE;
    }

    /**
     * 檢查評級年齡（天數）
     */
    public long getAgeInDays() {
        return java.time.Duration.between(ratedAt, LocalDateTime.now()).toDays();
    }

    /**
     * 檢查是否為近期評級（7天內）
     */
    public boolean isRecent() {
        return getAgeInDays() <= 7;
    }

    // 私有驗證方法

    private int validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("評級必須在1-5之間");
        }
        return rating;
    }

    // Getters

    public SellerRatingId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public RatingStatus getStatus() {
        return status;
    }

    public String getModeratorComment() {
        return moderatorComment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SellerRating that = (SellerRating) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SellerRating{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", rating=" + rating +
                ", status=" + status +
                ", ratedAt=" + ratedAt +
                '}';
    }
}