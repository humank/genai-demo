package solid.humank.genaidemo.domain.review.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.review.model.valueobject.ModerationAction;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationRecordId;
import solid.humank.genaidemo.domain.review.model.valueobject.ModerationStatus;

@DisplayName("ModerationRecord Entity Tests")
class ModerationRecordTest {

    @Test
    @DisplayName("Should create ModerationRecord with valid parameters")
    void shouldCreateModerationRecordWithValidParameters() {
        // Given
        ModerationRecordId id = ModerationRecordId.generate();
        String moderatorId = "MOD-001";
        ModerationAction action = ModerationAction.APPROVE;
        String reason = "Content is appropriate";

        // When
        ModerationRecord record = new ModerationRecord(id, moderatorId, action, reason);

        // Then
        assertThat(record.getId()).isEqualTo(id);
        assertThat(record.getModeratorId()).isEqualTo(moderatorId);
        assertThat(record.getAction()).isEqualTo(action);
        assertThat(record.getReason()).isEqualTo(reason);
        assertThat(record.getStatus()).isEqualTo(ModerationStatus.COMPLETED);
        assertThat(record.getModeratedAt()).isNotNull();
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getUpdatedAt()).isNotNull();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when creating ModerationRecord with null ID")
    void shouldThrowExceptionWhenCreatingWithNullId() {
        // Given
        String moderatorId = "MOD-001";
        ModerationAction action = ModerationAction.APPROVE;
        String reason = "Content is appropriate";

        // When & Then
        assertThatThrownBy(() -> new ModerationRecord(null, moderatorId, action, reason))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ModerationRecord ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating ModerationRecord with null moderator ID")
    void shouldThrowExceptionWhenCreatingWithNullModeratorId() {
        // Given
        ModerationRecordId id = ModerationRecordId.generate();
        ModerationAction action = ModerationAction.APPROVE;
        String reason = "Content is appropriate";

        // When & Then
        assertThatThrownBy(() -> new ModerationRecord(id, null, action, reason))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Moderator ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating ModerationRecord with empty moderator ID")
    void shouldThrowExceptionWhenCreatingWithEmptyModeratorId() {
        // Given
        ModerationRecordId id = ModerationRecordId.generate();
        String emptyModeratorId = "   ";
        ModerationAction action = ModerationAction.APPROVE;
        String reason = "Content is appropriate";

        // When & Then
        assertThatThrownBy(() -> new ModerationRecord(id, emptyModeratorId, action, reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("審核員ID不能為空");
    }

    @Test
    @DisplayName("Should throw exception when creating ModerationRecord with null action")
    void shouldThrowExceptionWhenCreatingWithNullAction() {
        // Given
        ModerationRecordId id = ModerationRecordId.generate();
        String moderatorId = "MOD-001";
        String reason = "Content is appropriate";

        // When & Then
        assertThatThrownBy(() -> new ModerationRecord(id, moderatorId, null, reason))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Moderation action cannot be null");
    }

    @Test
    @DisplayName("Should add comments to completed moderation record")
    void shouldAddCommentsToCompletedModerationRecord() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        String comments = "Additional review notes";

        // When
        record.addComments(comments);

        // Then
        assertThat(record.getComments()).isEqualTo(comments);
        assertThat(record.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when adding comments to non-completed record")
    void shouldThrowExceptionWhenAddingCommentsToNonCompletedRecord() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        record.cancel("Test cancellation");

        // When & Then
        assertThatThrownBy(() -> record.addComments("Some comments"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只有已完成的審核記錄可以添加評論");
    }

    @Test
    @DisplayName("Should update reason successfully")
    void shouldUpdateReasonSuccessfully() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        String newReason = "Updated reason for moderation";

        // When
        record.updateReason(newReason);

        // Then
        assertThat(record.getReason()).isEqualTo(newReason);
        assertThat(record.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when updating reason of cancelled record")
    void shouldThrowExceptionWhenUpdatingReasonOfCancelledRecord() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        record.cancel("Test cancellation");

        // When & Then
        assertThatThrownBy(() -> record.updateReason("New reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("已取消的審核記錄無法更新");
    }

    @Test
    @DisplayName("Should cancel moderation record successfully")
    void shouldCancelModerationRecordSuccessfully() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        String cancelReason = "Cancelled due to policy change";

        // When
        record.cancel(cancelReason);

        // Then
        assertThat(record.getStatus()).isEqualTo(ModerationStatus.CANCELLED);
        assertThat(record.getComments()).isEqualTo(cancelReason);
        assertThat(record.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled record")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledRecord() {
        // Given
        ModerationRecord record = createValidModerationRecord();
        record.cancel("First cancellation");

        // When & Then
        assertThatThrownBy(() -> record.cancel("Second cancellation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("審核記錄已經被取消");
    }

    @Test
    @DisplayName("Should correctly identify approval action")
    void shouldCorrectlyIdentifyApprovalAction() {
        // Given
        ModerationRecord approvalRecord = new ModerationRecord(
                ModerationRecordId.generate(),
                "MOD-001",
                ModerationAction.APPROVE,
                "Content approved");

        // When & Then
        assertThat(approvalRecord.isApprovalAction()).isTrue();
        assertThat(approvalRecord.isRejectionAction()).isFalse();
        assertThat(approvalRecord.isHidingAction()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify rejection action")
    void shouldCorrectlyIdentifyRejectionAction() {
        // Given
        ModerationRecord rejectionRecord = new ModerationRecord(
                ModerationRecordId.generate(),
                "MOD-001",
                ModerationAction.REJECT,
                "Content rejected");

        // When & Then
        assertThat(rejectionRecord.isRejectionAction()).isTrue();
        assertThat(rejectionRecord.isApprovalAction()).isFalse();
        assertThat(rejectionRecord.isHidingAction()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify hiding action")
    void shouldCorrectlyIdentifyHidingAction() {
        // Given
        ModerationRecord hidingRecord = new ModerationRecord(
                ModerationRecordId.generate(),
                "MOD-001",
                ModerationAction.HIDE,
                "Content hidden");

        // When & Then
        assertThat(hidingRecord.isHidingAction()).isTrue();
        assertThat(hidingRecord.isApprovalAction()).isFalse();
        assertThat(hidingRecord.isRejectionAction()).isFalse();
    }

    @Test
    @DisplayName("Should calculate age in hours correctly")
    void shouldCalculateAgeInHoursCorrectly() {
        // Given
        ModerationRecord record = createValidModerationRecord();

        // When
        long ageInHours = record.getAgeInHours();

        // Then
        assertThat(ageInHours).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should identify recent moderation correctly")
    void shouldIdentifyRecentModerationCorrectly() {
        // Given
        ModerationRecord record = createValidModerationRecord();

        // When & Then
        assertThat(record.isRecent()).isTrue(); // Just created, should be recent
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        ModerationRecordId id = ModerationRecordId.generate();
        ModerationRecord record1 = new ModerationRecord(id, "MOD-001", ModerationAction.APPROVE, "Reason 1");
        ModerationRecord record2 = new ModerationRecord(id, "MOD-002", ModerationAction.REJECT, "Reason 2");
        ModerationRecord record3 = new ModerationRecord(ModerationRecordId.generate(), "MOD-001",
                ModerationAction.APPROVE, "Reason 1");

        // When & Then
        assertThat(record1).isEqualTo(record2); // Same ID
        assertThat(record1).isNotEqualTo(record3); // Different ID
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
        assertThat(record1.hashCode()).isNotEqualTo(record3.hashCode());
    }

    // Helper methods

    private ModerationRecord createValidModerationRecord() {
        return new ModerationRecord(
                ModerationRecordId.generate(),
                "MOD-001",
                ModerationAction.APPROVE,
                "Content is appropriate");
    }
}