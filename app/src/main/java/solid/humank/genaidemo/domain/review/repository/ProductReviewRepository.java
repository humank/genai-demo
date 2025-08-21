package solid.humank.genaidemo.domain.review.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.model.aggregate.ProductReview;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewStatus;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 商品評價儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "ProductReviewRepository", description = "商品評價聚合根儲存庫")
public interface ProductReviewRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<ProductReview, ReviewId> {

    /**
     * 根據產品ID查詢評價
     * 
     * @param productId 產品ID
     * @return 評價列表
     */
    List<ProductReview> findByProductId(ProductId productId);

    /**
     * 根據評價者ID查詢評價
     * 
     * @param reviewerId 評價者ID
     * @return 評價列表
     */
    List<ProductReview> findByReviewerId(CustomerId reviewerId);

    /**
     * 根據狀態查詢評價
     * 
     * @param status 評價狀態
     * @return 評價列表
     */
    List<ProductReview> findByStatus(ReviewStatus status);

    /**
     * 根據產品ID和評價者ID查詢評價
     * 
     * @param productId  產品ID
     * @param reviewerId 評價者ID
     * @return 評價（如果存在）
     */
    Optional<ProductReview> findByProductIdAndReviewerId(ProductId productId, CustomerId reviewerId);

    /**
     * 查詢被檢舉的評價
     * 
     * @return 被檢舉的評價列表
     */
    List<ProductReview> findReportedReviews();
}