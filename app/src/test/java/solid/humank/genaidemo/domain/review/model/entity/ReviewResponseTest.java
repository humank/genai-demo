package solid.humank.genaidemo.domain.review.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.review.model.valueobject.ResponseStatus;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewResponseId;

@DisplayName("ReviewResponse Entity Tests")
class ReviewResponseTest {

    @Test
    @DisplayName("Should create ReviewResponse with valid parameters")
    void shouldCreateReviewResponseWithValidParameters() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderId = "MERCHANT-001";
        String responderType = "MERCHANT";
        String content = "Thank you for your review!";

        // When
        ReviewResponse response = new ReviewResponse(id, responderId, responderType, content);

        // Then
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getResponderId()).isEqualTo(responderId);
        assertThat(response.getResponderType()).isEqualTo(responderType);
        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.ACTIVE);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(response.isOfficial()).isFalse();
        assertThat(response.isMerchantResponse()).isTrue();
        assertThat(response.isAdminResponse()).isFalse();
    }

    @Test
    @DisplayName("Should create admin response as official")
    void shouldCreateAdminResponseAsOfficial() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderId = "ADMIN-001";
        String responderType = "ADMIN";
        String content = "This is an official response.";

        // When
        ReviewResponse response = new ReviewResponse(id, responderId, responderType, content);

        // Then
        assertThat(response.isOfficial()).isTrue();
        assertThat(response.isAdminResponse()).isTrue();
        assertThat(response.isMerchantResponse()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewResponse with null ID")
    void shouldThrowExceptionWhenCreatingWithNullId() {
        // Given
        String responderId = "MERCHANT-001";
        String responderType = "MERCHANT";
        String content = "Thank you for your review!";

        // When & Then
        assertThatThrownBy(() -> new ReviewResponse(null, responderId, responderType, content))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ReviewResponse ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewResponse with null responder ID")
    void shouldThrowExceptionWhenCreatingWithNullResponderId() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderType = "MERCHANT";
        String content = "Thank you for your review!";

        // When & Then
        assertThatThrownBy(() -> new ReviewResponse(id, null, responderType, content))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Responder ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewResponse with invalid responder type")
    void shouldThrowExceptionWhenCreatingWithInvalidResponderType() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderId = "USER-001";
        String invalidResponderType = "CUSTOMER";
        String content = "Thank you for your review!";

        // When & Then
        assertThatThrownBy(() -> new ReviewResponse(id, responderId, invalidResponderType, content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("回覆者類型必須是 MERCHANT 或 ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewResponse with empty content")
    void shouldThrowExceptionWhenCreatingWithEmptyContent() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderId = "MERCHANT-001";
        String responderType = "MERCHANT";
        String emptyContent = "   ";

        // When & Then
        assertThatThrownBy(() -> new ReviewResponse(id, responderId, responderType, emptyContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("回覆內容不能為空");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewResponse with content too long")
    void shouldThrowExceptionWhenCreatingWithContentTooLong() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        String responderId = "MERCHANT-001";
        String responderType = "MERCHANT";
        String tooLongContent = "a".repeat(1001); // 1001 characters

        // When & Then
        assertThatThrownBy(() -> new ReviewResponse(id, responderId, responderType, tooLongContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("回覆內容不能超過1000字");
    }

    @Test
    @DisplayName("Should update content successfully")
    void shouldUpdateContentSuccessfully() {
        // Given
        ReviewResponse response = createValidMerchantResponse();
        String newContent = "Updated response content";

        // When
        response.updateContent(newContent);

        // Then
        assertThat(response.getContent()).isEqualTo(newContent);
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(response.getLastModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when updating content of deleted response")
    void shouldThrowExceptionWhenUpdatingContentOfDeletedResponse() {
        // Given
        ReviewResponse response = createValidMerchantResponse();
        response.delete();

        // When & Then
        assertThatThrownBy(() -> response.updateContent("New content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("回覆無法修改");
    }

    @Test
    @DisplayName("Should hide response successfully")
    void shouldHideResponseSuccessfully() {
        // Given
        ReviewResponse response = createValidMerchantResponse();

        // When
        response.hide();

        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.HIDDEN);
        assertThat(response.isVisible()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should show hidden response successfully")
    void shouldShowHiddenResponseSuccessfully() {
        // Given
        ReviewResponse response = createValidMerchantResponse();
        response.hide();

        // When
        response.show();

        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.ACTIVE);
        assertThat(response.isVisible()).isTrue();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete response successfully")
    void shouldDeleteResponseSuccessfully() {
        // Given
        ReviewResponse response = createValidMerchantResponse();

        // When
        response.delete();

        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.DELETED);
        assertThat(response.isDeleted()).isTrue();
        assertThat(response.canBeModified()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when hiding deleted response")
    void shouldThrowExceptionWhenHidingDeletedResponse() {
        // Given
        ReviewResponse response = createValidMerchantResponse();
        response.delete();

        // When & Then
        assertThatThrownBy(() -> response.hide())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("已刪除的回覆無法隱藏");
    }

    @Test
    @DisplayName("Should mark admin response as official")
    void shouldMarkAdminResponseAsOfficial() {
        // Given
        ReviewResponse adminResponse = createValidAdminResponse();

        // When
        adminResponse.markAsOfficial();

        // Then
        assertThat(adminResponse.isOfficial()).isTrue();
        assertThat(adminResponse.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when marking merchant response as official")
    void shouldThrowExceptionWhenMarkingMerchantResponseAsOfficial() {
        // Given
        ReviewResponse merchantResponse = createValidMerchantResponse();

        // When & Then
        assertThatThrownBy(() -> merchantResponse.markAsOfficial())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只有管理員回覆可以標記為官方回覆");
    }

    @Test
    @DisplayName("Should check if response is within modification period")
    void shouldCheckIfResponseIsWithinModificationPeriod() {
        // Given
        ReviewResponse response = createValidMerchantResponse();

        // When & Then
        assertThat(response.isWithinModificationPeriod()).isTrue(); // Just created
    }

    @Test
    @DisplayName("Should calculate age in hours correctly")
    void shouldCalculateAgeInHoursCorrectly() {
        // Given
        ReviewResponse response = createValidMerchantResponse();

        // When
        long ageInHours = response.getAgeInHours();

        // Then
        assertThat(ageInHours).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should identify recent response correctly")
    void shouldIdentifyRecentResponseCorrectly() {
        // Given
        ReviewResponse response = createValidMerchantResponse();

        // When & Then
        assertThat(response.isRecent()).isTrue(); // Just created, should be recent
    }

    @Test
    @DisplayName("Should calculate content length correctly")
    void shouldCalculateContentLengthCorrectly() {
        // Given
        String content = "Thank you for your review!";
        ReviewResponse response = new ReviewResponse(
                ReviewResponseId.generate(),
                "MERCHANT-001",
                "MERCHANT",
                content);

        // When & Then
        assertThat(response.getContentLength()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        ReviewResponseId id = ReviewResponseId.generate();
        ReviewResponse response1 = new ReviewResponse(id, "MERCHANT-001", "MERCHANT", "Content 1");
        ReviewResponse response2 = new ReviewResponse(id, "MERCHANT-002", "MERCHANT", "Content 2");
        ReviewResponse response3 = new ReviewResponse(ReviewResponseId.generate(), "MERCHANT-001", "MERCHANT",
                "Content 1");

        // When & Then
        assertThat(response1).isEqualTo(response2); // Same ID
        assertThat(response1).isNotEqualTo(response3); // Different ID
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.hashCode()).isNotEqualTo(response3.hashCode());
    }

    // Helper methods

    private ReviewResponse createValidMerchantResponse() {
        return new ReviewResponse(
                ReviewResponseId.generate(),
                "MERCHANT-001",
                "MERCHANT",
                "Thank you for your review!");
    }

    private ReviewResponse createValidAdminResponse() {
        return new ReviewResponse(
                ReviewResponseId.generate(),
                "ADMIN-001",
                "ADMIN",
                "This is an official response.");
    }
}