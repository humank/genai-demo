package solid.humank.genaidemo.domain.review.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.review.model.valueobject.ImageStatus;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewImageId;

/**
 * 評價圖片實體
 */
@Entity(name = "ReviewImage", description = "評價圖片實體")
public class ReviewImage {
    private final ReviewImageId id;
    private String originalUrl;
    private String thumbnailUrl;
    private String fileName;
    private long fileSize;
    private ImageStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private String processingError;

    public ReviewImage(ReviewImageId id, String originalUrl, String fileName, long fileSize) {
        this.id = Objects.requireNonNull(id, "ReviewImage ID cannot be null");
        this.originalUrl = Objects.requireNonNull(originalUrl, "Original URL cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.fileSize = fileSize;
        this.status = ImageStatus.PENDING;
        this.uploadedAt = LocalDateTime.now();

        validateFileSize();
        validateFileName();
    }

    // 業務邏輯方法

    /**
     * 標記圖片為已處理
     */
    public void markAsProcessed() {
        if (this.status != ImageStatus.PENDING) {
            throw new IllegalStateException("只有待處理的圖片可以標記為已處理");
        }
        this.status = ImageStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 標記圖片處理失敗
     */
    public void markAsFailed(String error) {
        if (this.status != ImageStatus.PENDING) {
            throw new IllegalStateException("只有待處理的圖片可以標記為失敗");
        }
        this.status = ImageStatus.FAILED;
        this.processingError = error;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 刪除圖片
     */
    public void delete() {
        if (!canBeDeleted()) {
            throw new IllegalStateException("圖片無法刪除");
        }
        this.status = ImageStatus.DELETED;
    }

    /**
     * 生成縮圖
     */
    public void generateThumbnail() {
        if (this.status != ImageStatus.PROCESSED) {
            throw new IllegalStateException("只有已處理的圖片可以生成縮圖");
        }
        // 生成縮圖邏輯
        this.thumbnailUrl = originalUrl + "_thumb";
    }

    /**
     * 設置縮圖URL
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 驗證圖片是否有效
     */
    public boolean isValidImage() {
        return fileSize > 0 && fileName.matches(".*\\.(jpg|jpeg|png|gif|webp)$");
    }

    /**
     * 檢查是否可以刪除
     */
    public boolean canBeDeleted() {
        return status.canBeDeleted();
    }

    /**
     * 檢查圖片是否可見
     */
    public boolean isVisible() {
        return status.isActive();
    }

    /**
     * 獲取圖片年齡（小時）
     */
    public long getAgeInHours() {
        return java.time.Duration.between(uploadedAt, LocalDateTime.now()).toHours();
    }

    // 私有驗證方法

    private void validateFileSize() {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必須大於0");
        }
        if (fileSize > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("文件大小不能超過10MB");
        }
    }

    private void validateFileName() {
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能為空");
        }
        if (!isValidImage()) {
            throw new IllegalArgumentException("不支持的圖片格式");
        }
    }

    // Getters

    public ReviewImageId getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public ImageStatus getStatus() {
        return status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getProcessingError() {
        return processingError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReviewImage that = (ReviewImage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReviewImage{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", status=" + status +
                '}';
    }
}