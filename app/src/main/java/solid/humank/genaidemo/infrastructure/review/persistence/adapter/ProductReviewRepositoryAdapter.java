package solid.humank.genaidemo.infrastructure.review.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.model.aggregate.ProductReview;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewStatus;
import solid.humank.genaidemo.domain.review.repository.ProductReviewRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaProductReviewEntity;
import solid.humank.genaidemo.infrastructure.review.persistence.mapper.ProductReviewMapper;
import solid.humank.genaidemo.infrastructure.review.persistence.repository.JpaProductReviewRepository;

/** 商品評價儲存庫適配器 */
@Component
public class ProductReviewRepositoryAdapter
        extends BaseRepositoryAdapter<ProductReview, ReviewId, JpaProductReviewEntity, String>
        implements ProductReviewRepository {

    private final JpaProductReviewRepository jpaProductReviewRepository;
    private final ProductReviewMapper mapper;

    public ProductReviewRepositoryAdapter(JpaProductReviewRepository jpaProductReviewRepository,
            ProductReviewMapper mapper) {
        super(jpaProductReviewRepository);
        this.jpaProductReviewRepository = jpaProductReviewRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ProductReview> findByProductId(ProductId productId) {
        return jpaProductReviewRepository.findByProductId(productId.getId())
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<ProductReview> findByReviewerId(CustomerId reviewerId) {
        return jpaProductReviewRepository.findByReviewerId(reviewerId.getValue())
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<ProductReview> findByStatus(ReviewStatus status) {
        return jpaProductReviewRepository.findByStatus(status.name())
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public Optional<ProductReview> findByProductIdAndReviewerId(ProductId productId, CustomerId reviewerId) {
        return jpaProductReviewRepository.findByProductIdAndReviewerId(productId.getId(), reviewerId.getValue())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<ProductReview> findReportedReviews() {
        return jpaProductReviewRepository.findByIsReportedTrue()
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaProductReviewEntity toJpaEntity(ProductReview aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected ProductReview toDomainModel(JpaProductReviewEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(ReviewId domainId) {
        return domainId.value();
    }

    @Override
    protected ReviewId extractId(ProductReview aggregateRoot) {
        return aggregateRoot.getId();
    }
}