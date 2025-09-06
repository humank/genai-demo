package solid.humank.genaidemo.domain.review.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationAction;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationRecordId;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationStatus;

/**
 * 審核記錄實體
 */
@Entity(name = "ModerationRecord", description = "審核記錄實體")
public class ModerationRecord {
    private final ModerationRecordId id;
    private final String moderatorId;
    private ModerationAction action;
    private String reason;
    private String comments;
    private LocalDateTime moderatedAt;
    private ModerationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ModerationRecord(ModerationRecordId id, String moderatorId,
            ModerationAction action, String reason) {
        this.id = Objects.requireNonNull(id, "ModerationRecord ID cannot be null");
        this.moderatorId = Objects.requireNonNull(moderatorId, "Moderator ID cannot be null");
        this.action = Objects.requireNonNull(action, "Moderation action cannot be null");
        this.reason = reason;
        this.moderatedAt = LocalDateTime.now();
        this.status = ModerationStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        validateModeratorId();
    }

    // 業務邏輯方法

    /**
     * 添加審核評論
     */
    public void addComments(String comments) {
        if (this.status != ModerationStatus.COMPLETED) {
            throw new IllegalStateException("只有已完成的審核記錄可以添加評論");
        }
        this.comments = comments;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新審核理由
     */
    public void updateReason(String newReason) {
        if (this.status == ModerationStatus.CANCELLED) {
            throw new IllegalStateException("已取消的審核記錄無法更新");
        }
        this.reason = newReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消審核記錄
     */
    public void cancel(String cancelReason) {
        if (this.status == ModerationStatus.CANCELLED) {
            throw new IllegalStateException("審核記錄已經被取消");
        }
        this.status = ModerationStatus.CANCELLED;
        this.comments = cancelReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 檢查是否為通過審核
     */
    public boolean isApprovalAction() {
        return action.isApproval();
    }

    /**
     * 檢查是否為拒絕審核
     */
    public boolean isRejectionAction() {
        return action.isRejection();
    }

    /**
     * 檢查是否為隱藏動作
     */
    public boolean isHidingAction() {
        return action.isHiding();
    }

    /**
     * 檢查審核是否有效
     */
    public boolean isValid() {
        return status.isCompleted() && moderatorId != null && action != null;
    }

    /**
     * 檢查是否可以修改
     */
    public boolean canBeModified() {
        return status != ModerationStatus.CANCELLED;
    }

    /**
     * 獲取審核年齡（小時）
     */
    public long getAgeInHours() {
        return java.time.Duration.between(moderatedAt, LocalDateTime.now()).toHours();
    }

    /**
     * 檢查是否為最近的審核（24小時內）
     */
    public boolean isRecent() {
        return getAgeInHours() <= 24;
    }

    // 私有驗證方法

    private void validateModeratorId() {
        if (moderatorId == null || moderatorId.isBlank()) {
            throw new IllegalArgumentException("審核員ID不能為空");
        }
    }

    // Getters

    public ModerationRecordId getId() {
        return id;
    }

    public String getModeratorId() {
        return moderatorId;
    }

    public ModerationAction getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getComments() {
        return comments;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public ModerationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ModerationRecord that = (ModerationRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ModerationRecord{" +
                "id=" + id +
                ", moderatorId='" + moderatorId + '\'' +
                ", action=" + action +
                ", status=" + status +
                '}';
    }
}