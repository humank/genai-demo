package solid.humank.genaidemo.infrastructure.review.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.model.aggregate.ProductReview;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewRating;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaProductReviewEntity;

/** 商品評價映射器 */
@Component
public class ProductReviewMapper {

    /**
     * 將領域模型轉換為JPA實體
     */
    public JpaProductReviewEntity toJpaEntity(ProductReview productReview) {
        JpaProductReviewEntity entity = new JpaProductReviewEntity();
        entity.setId(productReview.getId().value());
        entity.setProductId(productReview.getProductId().getId());
        entity.setReviewerId(productReview.getReviewerId().getValue());
        entity.setRating(productReview.getRating().score());
        entity.setComment(productReview.getRating().comment());
        entity.setStatus(productReview.getStatus().name());
        entity.setSubmittedAt(productReview.getSubmittedAt());
        entity.setLastModifiedAt(productReview.getLastModifiedAt());
        entity.setImages(productReview.getImages());
        entity.setModeratorComment(productReview.getModeratorComment());
        entity.setModeratedAt(productReview.getModeratedAt());
        entity.setReported(productReview.isReported());
        entity.setReportReason(productReview.getReportReason());
        entity.setReportedAt(productReview.getReportedAt());
        return entity;
    }

    /**
     * 將JPA實體轉換為領域模型
     */
    public ProductReview toDomainModel(JpaProductReviewEntity entity) {
        ReviewId reviewId = ReviewId.of(entity.getId());
        ProductId productId = new ProductId(entity.getProductId());
        CustomerId reviewerId = CustomerId.of(entity.getReviewerId());
        ReviewRating rating = new ReviewRating(entity.getRating(), entity.getComment());

        ProductReview productReview = new ProductReview(reviewId, productId, reviewerId, rating);

        // Set additional fields using reflection or package-private setters
        // Note: This is a simplified version. In a real implementation, you might need
        // to use reflection or provide package-private setters in the aggregate root

        return productReview;
    }
}