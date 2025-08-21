package solid.humank.genaidemo.domain.review.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.review.model.valueobject.ImageStatus;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewImageId;

@DisplayName("ReviewImage Entity Tests")
class ReviewImageTest {

    @Test
    @DisplayName("Should create ReviewImage with valid parameters")
    void shouldCreateReviewImageWithValidParameters() {
        // Given
        ReviewImageId id = ReviewImageId.generate();
        String originalUrl = "https://example.com/image.jpg";
        String fileName = "image.jpg";
        long fileSize = 1024 * 1024; // 1MB

        // When
        ReviewImage reviewImage = new ReviewImage(id, originalUrl, fileName, fileSize);

        // Then
        assertThat(reviewImage.getId()).isEqualTo(id);
        assertThat(reviewImage.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(reviewImage.getFileName()).isEqualTo(fileName);
        assertThat(reviewImage.getFileSize()).isEqualTo(fileSize);
        assertThat(reviewImage.getStatus()).isEqualTo(ImageStatus.PENDING);
        assertThat(reviewImage.getUploadedAt()).isNotNull();
        assertThat(reviewImage.isValidImage()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewImage with null ID")
    void shouldThrowExceptionWhenCreatingWithNullId() {
        // Given
        String originalUrl = "https://example.com/image.jpg";
        String fileName = "image.jpg";
        long fileSize = 1024;

        // When & Then
        assertThatThrownBy(() -> new ReviewImage(null, originalUrl, fileName, fileSize))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ReviewImage ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewImage with invalid file size")
    void shouldThrowExceptionWhenCreatingWithInvalidFileSize() {
        // Given
        ReviewImageId id = ReviewImageId.generate();
        String originalUrl = "https://example.com/image.jpg";
        String fileName = "image.jpg";
        long invalidFileSize = 0;

        // When & Then
        assertThatThrownBy(() -> new ReviewImage(id, originalUrl, fileName, invalidFileSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文件大小必須大於0");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewImage with file size too large")
    void shouldThrowExceptionWhenCreatingWithFileSizeTooLarge() {
        // Given
        ReviewImageId id = ReviewImageId.generate();
        String originalUrl = "https://example.com/image.jpg";
        String fileName = "image.jpg";
        long tooLargeFileSize = 11 * 1024 * 1024; // 11MB

        // When & Then
        assertThatThrownBy(() -> new ReviewImage(id, originalUrl, fileName, tooLargeFileSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文件大小不能超過10MB");
    }

    @Test
    @DisplayName("Should throw exception when creating ReviewImage with invalid file format")
    void shouldThrowExceptionWhenCreatingWithInvalidFileFormat() {
        // Given
        ReviewImageId id = ReviewImageId.generate();
        String originalUrl = "https://example.com/document.pdf";
        String fileName = "document.pdf";
        long fileSize = 1024;

        // When & Then
        assertThatThrownBy(() -> new ReviewImage(id, originalUrl, fileName, fileSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不支持的圖片格式");
    }

    @Test
    @DisplayName("Should mark image as processed successfully")
    void shouldMarkImageAsProcessedSuccessfully() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();

        // When
        reviewImage.markAsProcessed();

        // Then
        assertThat(reviewImage.getStatus()).isEqualTo(ImageStatus.PROCESSED);
        assertThat(reviewImage.getProcessedAt()).isNotNull();
        assertThat(reviewImage.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when marking non-pending image as processed")
    void shouldThrowExceptionWhenMarkingNonPendingImageAsProcessed() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();
        reviewImage.markAsProcessed(); // Already processed

        // When & Then
        assertThatThrownBy(() -> reviewImage.markAsProcessed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只有待處理的圖片可以標記為已處理");
    }

    @Test
    @DisplayName("Should mark image as failed with error message")
    void shouldMarkImageAsFailedWithErrorMessage() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();
        String errorMessage = "Processing failed due to corrupted file";

        // When
        reviewImage.markAsFailed(errorMessage);

        // Then
        assertThat(reviewImage.getStatus()).isEqualTo(ImageStatus.FAILED);
        assertThat(reviewImage.getProcessingError()).isEqualTo(errorMessage);
        assertThat(reviewImage.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should generate thumbnail for processed image")
    void shouldGenerateThumbnailForProcessedImage() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();
        reviewImage.markAsProcessed();

        // When
        reviewImage.generateThumbnail();

        // Then
        assertThat(reviewImage.getThumbnailUrl()).isEqualTo(reviewImage.getOriginalUrl() + "_thumb");
    }

    @Test
    @DisplayName("Should throw exception when generating thumbnail for non-processed image")
    void shouldThrowExceptionWhenGeneratingThumbnailForNonProcessedImage() {
        // Given
        ReviewImage reviewImage = createValidReviewImage(); // Still pending

        // When & Then
        assertThatThrownBy(() -> reviewImage.generateThumbnail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只有已處理的圖片可以生成縮圖");
    }

    @Test
    @DisplayName("Should delete image successfully")
    void shouldDeleteImageSuccessfully() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();

        // When
        reviewImage.delete();

        // Then
        assertThat(reviewImage.getStatus()).isEqualTo(ImageStatus.DELETED);
        assertThat(reviewImage.canBeDeleted()).isFalse();
        assertThat(reviewImage.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Should validate supported image formats")
    void shouldValidateSupportedImageFormats() {
        // Given & When & Then
        assertThat(createReviewImageWithFileName("image.jpg").isValidImage()).isTrue();
        assertThat(createReviewImageWithFileName("image.jpeg").isValidImage()).isTrue();
        assertThat(createReviewImageWithFileName("image.png").isValidImage()).isTrue();
        assertThat(createReviewImageWithFileName("image.gif").isValidImage()).isTrue();
        assertThat(createReviewImageWithFileName("image.webp").isValidImage()).isTrue();
    }

    @Test
    @DisplayName("Should calculate age in hours correctly")
    void shouldCalculateAgeInHoursCorrectly() {
        // Given
        ReviewImage reviewImage = createValidReviewImage();

        // When
        long ageInHours = reviewImage.getAgeInHours();

        // Then
        assertThat(ageInHours).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        ReviewImageId id = ReviewImageId.generate();
        ReviewImage image1 = new ReviewImage(id, "https://example.com/image1.jpg", "image1.jpg", 1024);
        ReviewImage image2 = new ReviewImage(id, "https://example.com/image2.jpg", "image2.jpg", 2048);
        ReviewImage image3 = new ReviewImage(ReviewImageId.generate(), "https://example.com/image1.jpg", "image1.jpg",
                1024);

        // When & Then
        assertThat(image1).isEqualTo(image2); // Same ID
        assertThat(image1).isNotEqualTo(image3); // Different ID
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
        assertThat(image1.hashCode()).isNotEqualTo(image3.hashCode());
    }

    // Helper methods

    private ReviewImage createValidReviewImage() {
        return new ReviewImage(
                ReviewImageId.generate(),
                "https://example.com/image.jpg",
                "image.jpg",
                1024 * 1024);
    }

    private ReviewImage createReviewImageWithFileName(String fileName) {
        return new ReviewImage(
                ReviewImageId.generate(),
                "https://example.com/" + fileName,
                fileName,
                1024);
    }
}