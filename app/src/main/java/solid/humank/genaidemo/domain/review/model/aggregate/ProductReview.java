package solid.humank.genaidemo.domain.review.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.exception.ReviewModificationNotAllowedException;
import solid.humank.genaidemo.domain.review.model.entity.ModerationRecord;
import solid.humank.genaidemo.domain.review.model.entity.ReviewImage;
import solid.humank.genaidemo.domain.review.model.entity.ReviewResponse;
import solid.humank.genaidemo.domain.review.model.events.ReviewApprovedEvent;
import solid.humank.genaidemo.domain.review.model.events.ReviewCreatedEvent;
import solid.humank.genaidemo.domain.review.model.events.ReviewModifiedEvent;
import solid.humank.genaidemo.domain.review.model.events.ReviewRejectedEvent;
import solid.humank.genaidemo.domain.review.model.events.ReviewReportedEvent;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationAction;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationRecordId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewImageId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewRating;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewResponseId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewStatus;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 商品評價聚合根 */
@AggregateRoot(name = "ProductReview", description = "商品評價聚合根，管理消費者的商品評價和評分", boundedContext = "Review", version = "2.0")
public class ProductReview implements AggregateRootInterface {

    private final ReviewId id;
    private final ProductId productId;
    private final CustomerId reviewerId;
    private ReviewRating rating;
    private ReviewStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime lastModifiedAt;

    // Entity 集合
    private final List<ReviewImage> images; // 原 List<String>
    private final List<ModerationRecord> moderations; // 原 ReviewModeration 聚合根
    private final List<ReviewResponse> responses; // 新增商家回覆功能

    // 檢舉相關屬性
    private boolean isReported;
    private String reportReason;
    private LocalDateTime reportedAt;

    public ProductReview(
            ReviewId id, ProductId productId, CustomerId reviewerId, ReviewRating rating) {
        this.id = Objects.requireNonNull(id, "Review ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.reviewerId = Objects.requireNonNull(reviewerId, "Reviewer ID cannot be null");
        this.rating = Objects.requireNonNull(rating, "Rating cannot be null");
        this.status = ReviewStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.images = new ArrayList<>();
        this.moderations = new ArrayList<>();
        this.responses = new ArrayList<>();
        this.isReported = false;

        // 收集領域事件
        collectEvent(ReviewCreatedEvent.create(id, productId, reviewerId, rating));
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

    public List<ReviewImage> getImages() {
        return new ArrayList<>(images);
    }

    public List<ModerationRecord> getModerations() {
        return new ArrayList<>(moderations);
    }

    public List<ReviewResponse> getResponses() {
        return new ArrayList<>(responses);
    }

    public Optional<String> getLatestModeratorComment() {
        return moderations.stream()
                .filter(ModerationRecord::isValid)
                .reduce((first, second) -> second) // 獲取最新的
                .map(ModerationRecord::getComments);
    }

    public Optional<LocalDateTime> getLatestModerationTime() {
        return moderations.stream()
                .filter(ModerationRecord::isValid)
                .reduce((first, second) -> second) // 獲取最新的
                .map(ModerationRecord::getModeratedAt);
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

        ReviewRating oldRating = this.rating;
        this.rating = newRating;
        this.lastModifiedAt = LocalDateTime.now();
        this.status = ReviewStatus.PENDING; // 修改後需要重新審核

        // 發布領域事件
        collectEvent(ReviewModifiedEvent.create(this.id, this.productId, this.reviewerId, oldRating, newRating));
    }

    /** 檢查是否可以修改 */
    public boolean canModify() {
        // 只有在提交後7天內且未被審核通過的評價可以修改
        LocalDateTime modificationDeadline = submittedAt.plusDays(7);
        return LocalDateTime.now().isBefore(modificationDeadline)
                && (status == ReviewStatus.PENDING || status == ReviewStatus.REJECTED);
    }

    /** 添加圖片 */
    public ReviewImageId addImage(String imageUrl, String fileName, long fileSize) {
        if (getActiveImageCount() >= 5) {
            throw new IllegalArgumentException("評價圖片不能超過5張");
        }

        // 檢查是否已存在相同URL的圖片
        boolean imageExists = images.stream()
                .anyMatch(img -> img.getOriginalUrl().equals(imageUrl) && img.isVisible());

        if (imageExists) {
            throw new IllegalArgumentException("圖片已存在");
        }

        ReviewImageId imageId = ReviewImageId.generate();
        ReviewImage reviewImage = new ReviewImage(imageId, imageUrl, fileName, fileSize);
        images.add(reviewImage);
        this.lastModifiedAt = LocalDateTime.now();

        return imageId;
    }

    /** 移除圖片 */
    public void removeImage(ReviewImageId imageId) {
        ReviewImage image = findImageById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("圖片不存在"));

        if (!image.canBeDeleted()) {
            throw new IllegalStateException("圖片無法刪除");
        }

        image.delete();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 獲取活躍圖片數量 */
    private int getActiveImageCount() {
        return (int) images.stream()
                .filter(ReviewImage::isVisible)
                .count();
    }

    /** 根據ID查找圖片 */
    private Optional<ReviewImage> findImageById(ReviewImageId imageId) {
        return images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst();
    }

    /** 審核通過 */
    public ModerationRecordId approve(String moderatorId, String reason) {
        if (status != ReviewStatus.PENDING) {
            throw new IllegalStateException("只有待審核的評價可以通過審核");
        }

        // 創建審核記錄
        ModerationRecordId moderationId = ModerationRecordId.generate();
        ModerationRecord moderation = new ModerationRecord(
                moderationId, moderatorId, ModerationAction.APPROVE, reason);
        moderations.add(moderation);

        // 更新狀態
        this.status = ReviewStatus.APPROVED;
        this.lastModifiedAt = LocalDateTime.now();

        // 發布領域事件
        collectEvent(ReviewApprovedEvent.create(this.id, this.productId, this.reviewerId, moderatorId));

        return moderationId;
    }

    /** 審核拒絕 */
    public ModerationRecordId reject(String moderatorId, String reason) {
        if (status != ReviewStatus.PENDING) {
            throw new IllegalStateException("只有待審核的評價可以拒絕");
        }

        // 創建審核記錄
        ModerationRecordId moderationId = ModerationRecordId.generate();
        ModerationRecord moderation = new ModerationRecord(
                moderationId, moderatorId, ModerationAction.REJECT, reason);
        moderations.add(moderation);

        // 更新狀態
        this.status = ReviewStatus.REJECTED;
        this.lastModifiedAt = LocalDateTime.now();

        // 發布領域事件
        collectEvent(ReviewRejectedEvent.create(this.id, this.productId, this.reviewerId, moderatorId, reason));

        return moderationId;
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

        // 發布領域事件
        collectEvent(ReviewReportedEvent.create(this.id, this.productId, reason));
    }

    /** 處理檢舉 - 維持原狀 */
    public ModerationRecordId dismissReport(String moderatorId, String moderatorComment) {
        if (status != ReviewStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("只有調查中的評價可以處理檢舉");
        }

        // 創建審核記錄
        ModerationRecordId moderationId = ModerationRecordId.generate();
        ModerationRecord moderation = new ModerationRecord(
                moderationId, moderatorId, ModerationAction.APPROVE, moderatorComment);
        moderations.add(moderation);

        this.status = ReviewStatus.APPROVED;
        this.lastModifiedAt = LocalDateTime.now();

        return moderationId;
    }

    /** 處理檢舉 - 隱藏評價 */
    public ModerationRecordId hideReview(String moderatorId, String reason) {
        if (status != ReviewStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("只有調查中的評價可以處理檢舉");
        }

        // 創建審核記錄
        ModerationRecordId moderationId = ModerationRecordId.generate();
        ModerationRecord moderation = new ModerationRecord(
                moderationId, moderatorId, ModerationAction.HIDE, reason);
        moderations.add(moderation);

        this.status = ReviewStatus.HIDDEN;
        this.lastModifiedAt = LocalDateTime.now();

        return moderationId;
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

    // === 回覆管理方法 ===

    /** 添加商家回覆 */
    public ReviewResponseId addMerchantResponse(String merchantId, String content) {
        if (!isVisible()) {
            throw new IllegalStateException("只有可見的評價可以回覆");
        }

        // 檢查是否已有商家回覆
        boolean hasMerchantResponse = responses.stream()
                .anyMatch(response -> response.isMerchantResponse() && response.isVisible());

        if (hasMerchantResponse) {
            throw new IllegalStateException("商家已經回覆過此評價");
        }

        ReviewResponseId responseId = ReviewResponseId.generate();
        ReviewResponse response = new ReviewResponse(responseId, merchantId, "MERCHANT", content);
        responses.add(response);
        this.lastModifiedAt = LocalDateTime.now();

        return responseId;
    }

    /** 添加管理員回覆 */
    public ReviewResponseId addAdminResponse(String adminId, String content) {
        ReviewResponseId responseId = ReviewResponseId.generate();
        ReviewResponse response = new ReviewResponse(responseId, adminId, "ADMIN", content);
        response.markAsOfficial();
        responses.add(response);
        this.lastModifiedAt = LocalDateTime.now();

        return responseId;
    }

    /** 更新回覆內容 */
    public void updateResponse(ReviewResponseId responseId, String newContent) {
        ReviewResponse response = findResponseById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("回覆不存在"));

        response.updateContent(newContent);
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 刪除回覆 */
    public void deleteResponse(ReviewResponseId responseId) {
        ReviewResponse response = findResponseById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("回覆不存在"));

        response.delete();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 根據ID查找回覆 */
    private Optional<ReviewResponse> findResponseById(ReviewResponseId responseId) {
        return responses.stream()
                .filter(response -> response.getId().equals(responseId))
                .findFirst();
    }

    /** 獲取可見的回覆 */
    public List<ReviewResponse> getVisibleResponses() {
        return responses.stream()
                .filter(ReviewResponse::isVisible)
                .toList();
    }

    // === 聚合一致性檢查方法 ===

    /** 檢查聚合狀態一致性 */
    public boolean isConsistent() {
        // 檢查狀態與審核記錄的一致性
        if (status == ReviewStatus.APPROVED || status == ReviewStatus.REJECTED || status == ReviewStatus.HIDDEN) {
            boolean hasModerationRecord = moderations.stream()
                    .anyMatch(ModerationRecord::isValid);
            if (!hasModerationRecord) {
                return false;
            }
        }

        // 檢查圖片狀態
        boolean hasInvalidImages = images.stream()
                .anyMatch(img -> !img.isValidImage());
        if (hasInvalidImages) {
            return false;
        }

        // 檢查回覆狀態
        boolean hasInvalidResponses = responses.stream()
                .anyMatch(response -> response.getContentLength() == 0);
        if (hasInvalidResponses) {
            return false;
        }

        return true;
    }

    /** 獲取審核歷史摘要 */
    public String getModerationSummary() {
        if (moderations.isEmpty()) {
            return "無審核記錄";
        }

        return moderations.stream()
                .filter(ModerationRecord::isValid)
                .map(mod -> String.format("%s: %s",
                        mod.getAction().getDescription(),
                        mod.getReason()))
                .reduce((first, second) -> first + "; " + second)
                .orElse("無有效審核記錄");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProductReview review = (ProductReview) obj;
        return id.equals(review.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
