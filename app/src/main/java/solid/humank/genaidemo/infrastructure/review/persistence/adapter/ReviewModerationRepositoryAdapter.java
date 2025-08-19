package solid.humank.genaidemo.infrastructure.review.persistence.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.review.model.aggregate.ReviewModeration;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.repository.ReviewModerationRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.review.persistence.entity.JpaReviewModerationEntity;
import solid.humank.genaidemo.infrastructure.review.persistence.mapper.ReviewModerationMapper;
import solid.humank.genaidemo.infrastructure.review.persistence.repository.JpaReviewModerationRepository;

/** 評價審核儲存庫適配器 */
@Component
public class ReviewModerationRepositoryAdapter
        extends BaseRepositoryAdapter<ReviewModeration, ReviewId, JpaReviewModerationEntity, String>
        implements ReviewModerationRepository {

    private final JpaReviewModerationRepository jpaReviewModerationRepository;
    private final ReviewModerationMapper mapper;

    public ReviewModerationRepositoryAdapter(JpaReviewModerationRepository jpaReviewModerationRepository,
            ReviewModerationMapper mapper) {
        super(jpaReviewModerationRepository);
        this.jpaReviewModerationRepository = jpaReviewModerationRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ReviewModeration> findByReviewId(ReviewId reviewId) {
        return jpaReviewModerationRepository.findByReviewId(reviewId.value())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewModeration> findByModeratorId(String moderatorId) {
        return jpaReviewModerationRepository.findByModeratorId(moderatorId)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewModeration> findByAction(String action) {
        return jpaReviewModerationRepository.findByAction(action)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaReviewModerationEntity toJpaEntity(ReviewModeration aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected ReviewModeration toDomainModel(JpaReviewModerationEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(ReviewId domainId) {
        return domainId.value();
    }

    @Override
    protected ReviewId extractId(ReviewModeration aggregateRoot) {
        return aggregateRoot.getReviewId();
    }
}