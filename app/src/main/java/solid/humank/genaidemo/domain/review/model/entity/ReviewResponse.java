package solid.humank.genaidemo.domain.review.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.review.model.valueobject.ResponseStatus;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewResponseId;

/**
 * 評價回覆實體
 */
@Entity(name = "ReviewResponse", description = "評價回覆實體")
public class ReviewResponse {
    private final ReviewResponseId id;
    private final String responderId; // 回覆者ID（通常是商家）
    private final String responderType; // 回覆者類型：MERCHANT, ADMIN
    private String content;
    private ResponseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastModifiedAt;
    private boolean isOfficial; // 是否為官方回覆

    public ReviewResponse(ReviewResponseId id, String responderId, String responderType, String content) {
        this.id = Objects.requireNonNull(id, "ReviewResponse ID cannot be null");
        this.responderId = Objects.requireNonNull(responderId, "Responder ID cannot be null");
        this.responderType = Objects.requireNonNull(responderType, "Responder type cannot be null");
        this.content = Objects.requireNonNull(content, "Response content cannot be null");
        this.status = ResponseStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.isOfficial = "ADMIN".equals(responderType);

        validateContent();
        validateResponderType();
    }

    // 業務邏輯方法

    /**
     * 更新回覆內容
     */
    public void updateContent(String newContent) {
        if (!canBeModified()) {
            throw new IllegalStateException("回覆無法修改");
        }

        validateContentForUpdate(newContent);

        this.content = newContent;
        this.updatedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * 隱藏回覆
     */
    public void hide() {
        if (this.status == ResponseStatus.DELETED) {
            throw new IllegalStateException("已刪除的回覆無法隱藏");
        }
        this.status = ResponseStatus.HIDDEN;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 顯示回覆
     */
    public void show() {
        if (this.status == ResponseStatus.DELETED) {
            throw new IllegalStateException("已刪除的回覆無法顯示");
        }
        this.status = ResponseStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 刪除回覆
     */
    public void delete() {
        this.status = ResponseStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為官方回覆
     */
    public void markAsOfficial() {
        if (!"ADMIN".equals(responderType)) {
            throw new IllegalStateException("只有管理員回覆可以標記為官方回覆");
        }
        this.isOfficial = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 檢查是否可以修改
     */
    public boolean canBeModified() {
        return status.canBeModified() && !isDeleted();
    }

    /**
     * 檢查是否可見
     */
    public boolean isVisible() {
        return status.isVisible();
    }

    /**
     * 檢查是否已刪除
     */
    public boolean isDeleted() {
        return status.isDeleted();
    }

    /**
     * 檢查是否為商家回覆
     */
    public boolean isMerchantResponse() {
        return "MERCHANT".equals(responderType);
    }

    /**
     * 檢查是否為管理員回覆
     */
    public boolean isAdminResponse() {
        return "ADMIN".equals(responderType);
    }

    /**
     * 檢查回覆是否在修改期限內（7天）
     */
    public boolean isWithinModificationPeriod() {
        LocalDateTime modificationDeadline = createdAt.plusDays(7);
        return LocalDateTime.now().isBefore(modificationDeadline);
    }

    /**
     * 獲取回覆年齡（小時）
     */
    public long getAgeInHours() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    /**
     * 檢查是否為最近的回覆（24小時內）
     */
    public boolean isRecent() {
        return getAgeInHours() <= 24;
    }

    /**
     * 獲取內容長度
     */
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }

    // 私有驗證方法

    private void validateContent() {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("回覆內容不能為空");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("回覆內容不能超過1000字");
        }
    }

    private void validateContentForUpdate(String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("回覆內容不能為空");
        }
        if (newContent.length() > 1000) {
            throw new IllegalArgumentException("回覆內容不能超過1000字");
        }
    }

    private void validateResponderType() {
        if (!"MERCHANT".equals(responderType) && !"ADMIN".equals(responderType)) {
            throw new IllegalArgumentException("回覆者類型必須是 MERCHANT 或 ADMIN");
        }
    }

    // Getters

    public ReviewResponseId getId() {
        return id;
    }

    public String getResponderId() {
        return responderId;
    }

    public String getResponderType() {
        return responderType;
    }

    public String getContent() {
        return content;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public boolean isOfficial() {
        return isOfficial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReviewResponse that = (ReviewResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReviewResponse{" +
                "id=" + id +
                ", responderId='" + responderId + '\'' +
                ", responderType='" + responderType + '\'' +
                ", status=" + status +
                ", isOfficial=" + isOfficial +
                '}';
    }
}