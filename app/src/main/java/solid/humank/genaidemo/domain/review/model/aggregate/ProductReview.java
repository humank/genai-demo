package solid.humank.genaidemo.domain.review.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.exception.ReviewModificationNotAllowedException;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewRating;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewStatus;

/** 商品評價聚合根 */
@AggregateRoot(name = "ProductReview", description = "商品評價聚合根，管理消費者的商品評價和評分")
public class ProductReview {

    private final ReviewId id;
    private final ProductId productId;
    private final CustomerId reviewerId;
    private ReviewRating rating;
    private ReviewStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime lastModifiedAt;
    private List<String> images;
    private String moderatorComment;
    private LocalDateTime moderatedAt;
    private boolean isReported;
    private String reportReason;
    private LocalDateTime reportedAt;

    public ProductReview(
            ReviewId id, ProductId productId, CustomerId reviewerId, ReviewRating rating) {
        this.id = id;
        this.productId = productId;
        this.reviewerId = reviewerId;
        this.rating = rating;
        this.status = ReviewStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.images = new ArrayList<>();
        this.isReported = false;
    }

    // Getters
    public ReviewId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public CustomerId getReviewerId() {
        return reviewerId;
    }

    public ReviewRating getRating() {
        return rating;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public List<String> getImages() {
        return new ArrayList<>(images);
    }

    public String getModeratorComment() {
        return moderatorComment;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public boolean isReported() {
        return isReported;
    }

    public String getReportReason() {
        return reportReason;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    // 業務方法

    /** 修改評價 */
    public void modifyRating(ReviewRating newRating) {
        if (!canModify()) {
            throw new ReviewModificationNotAllowedException("評價無法修改");
        }

        this.rating = newRating;
        this.lastModifiedAt = LocalDateTime.now();
        this.status = ReviewStatus.PENDING; // 修改後需要重新審核

        // TODO: 發布領域事件
        // registerEvent(new ReviewModifiedEvent(this.id, this.productId,
        // this.reviewerId));
    }

    /** 檢查是否可以修改 */
    public boolean canModify() {
        // 只有在提交後7天內且未被審核通過的評價可以修改
        LocalDateTime modificationDeadline = submittedAt.plusDays(7);
        return LocalDateTime.now().isBefore(modificationDeadline)
                && (status == ReviewStatus.PENDING || status == ReviewStatus.REJECTED);
    }

    /** 添加圖片 */
    public void addImage(String imageUrl) {
        if (images.size() >= 5) {
            throw new IllegalArgumentException("評價圖片不能超過5張");
        }

        if (!images.contains(imageUrl)) {
            images.add(imageUrl);
            this.lastModifiedAt = LocalDateTime.now();
        }
    }

    /** 移除圖片 */
    public void removeImage(String imageUrl) {
        if (images.remove(imageUrl)) {
            this.lastModifiedAt = LocalDateTime.now();
        }
    }

    /** 審核通過 */
    public void approve(String moderatorComment) {
        if (status != ReviewStatus.PENDING) {
            throw new IllegalStateException("只有待審核的評價可以通過審核");
        }

        this.status = ReviewStatus.APPROVED;
        this.moderatorComment = moderatorComment;
        this.moderatedAt = LocalDateTime.now();

        // TODO: 發布領域事件
        // registerEvent(new ReviewApprovedEvent(this.id, this.productId,
        // this.reviewerId));
    }

    /** 審核拒絕 */
    public void reject(String reason) {
        if (status != ReviewStatus.PENDING) {
            throw new IllegalStateException("只有待審核的評價可以拒絕");
        }

        this.status = ReviewStatus.REJECTED;
        this.moderatorComment = reason;
        this.moderatedAt = LocalDateTime.now();

        // TODO: 發布領域事件
        // registerEvent(new ReviewRejectedEvent(this.id, this.productId,
        // this.reviewerId, reason));
    }

    /** 檢舉評價 */
    public void report(String reason) {
        if (status != ReviewStatus.APPROVED) {
            throw new IllegalStateException("只有已通過審核的評價可以被檢舉");
        }

        this.isReported = true;
        this.reportReason = reason;
        this.reportedAt = LocalDateTime.now();
        this.status = ReviewStatus.UNDER_INVESTIGATION;

        // TODO: 發布領域事件
        // registerEvent(new ReviewReportedEvent(this.id, this.productId, reason));
    }

    /** 處理檢舉 - 維持原狀 */
    public void dismissReport(String moderatorComment) {
        if (status != ReviewStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("只有調查中的評價可以處理檢舉");
        }

        this.status = ReviewStatus.APPROVED;
        this.moderatorComment = moderatorComment;
        this.moderatedAt = LocalDateTime.now();
    }

    /** 處理檢舉 - 隱藏評價 */
    public void hideReview(String reason) {
        if (status != ReviewStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("只有調查中的評價可以處理檢舉");
        }

        this.status = ReviewStatus.HIDDEN;
        this.moderatorComment = reason;
        this.moderatedAt = LocalDateTime.now();
    }

    /** 檢查評價是否屬於指定客戶 */
    public boolean belongsTo(CustomerId customerId) {
        return this.reviewerId.equals(customerId);
    }

    /** 檢查評價是否可見 */
    public boolean isVisible() {
        return status == ReviewStatus.APPROVED;
    }

    /** 獲取評價年齡（天數） */
    public long getAgeInDays() {
        return java.time.Duration.between(submittedAt, LocalDateTime.now()).toDays();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductReview review = (ProductReview) obj;
        return id.equals(review.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
