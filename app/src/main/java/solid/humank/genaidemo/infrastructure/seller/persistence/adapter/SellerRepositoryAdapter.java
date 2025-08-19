package solid.humank.genaidemo.infrastructure.seller.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.seller.model.aggregate.Seller;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.repository.SellerRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerEntity;
import solid.humank.genaidemo.infrastructure.seller.persistence.mapper.SellerMapper;
import solid.humank.genaidemo.infrastructure.seller.persistence.repository.JpaSellerRepository;

/** 賣家儲存庫適配器 */
@Component
public class SellerRepositoryAdapter
        extends BaseRepositoryAdapter<Seller, SellerId, JpaSellerEntity, String>
        implements SellerRepository {

    private final JpaSellerRepository jpaSellerRepository;
    private final SellerMapper mapper;

    public SellerRepositoryAdapter(JpaSellerRepository jpaSellerRepository, SellerMapper mapper) {
        super(jpaSellerRepository);
        this.jpaSellerRepository = jpaSellerRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Seller> findByEmail(String email) {
        return jpaSellerRepository.findByEmail(email)
                .map(mapper::toDomainModel);
    }

    @Override
    public Optional<Seller> findByPhone(String phone) {
        return jpaSellerRepository.findByPhone(phone)
                .map(mapper::toDomainModel);
    }

    @Override
    public List<Seller> findActiveSellers() {
        return jpaSellerRepository.findByIsActiveTrue()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Seller> findByNameContaining(String name) {
        return jpaSellerRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaSellerEntity toJpaEntity(Seller aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Seller toDomainModel(JpaSellerEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(SellerId domainId) {
        return domainId.getId();
    }

    @Override
    protected SellerId extractId(Seller aggregateRoot) {
        return aggregateRoot.getSellerId();
    }
}