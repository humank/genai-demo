package solid.humank.genaidemo.infrastructure.shoppingcart.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.repository.ShoppingCartRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity.JpaShoppingCartEntity;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.mapper.ShoppingCartMapper;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.repository.JpaShoppingCartRepository;

/** 購物車儲存庫適配器 實現領域儲存庫接口 專門處理 ShoppingCart 聚合根 */
@Component
public class ShoppingCartRepositoryAdapter
        extends BaseRepositoryAdapter<ShoppingCart, ShoppingCartId, JpaShoppingCartEntity, UUID>
        implements ShoppingCartRepository {

    private final JpaShoppingCartRepository jpaShoppingCartRepository;
    private final ShoppingCartMapper mapper;

    public ShoppingCartRepositoryAdapter(JpaShoppingCartRepository jpaShoppingCartRepository,
            ShoppingCartMapper mapper) {
        super(jpaShoppingCartRepository);
        this.jpaShoppingCartRepository = jpaShoppingCartRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ShoppingCart> findByCustomerId(CustomerId customerId) {
        return jpaShoppingCartRepository
                .findByCustomerId(customerId.getId())
                .map(mapper::toDomainModel);
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaShoppingCartEntity toJpaEntity(ShoppingCart aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected ShoppingCart toDomainModel(JpaShoppingCartEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected UUID convertToJpaId(ShoppingCartId domainId) {
        return UUID.fromString(domainId.value());
    }

    @Override
    protected ShoppingCartId extractId(ShoppingCart aggregateRoot) {
        return aggregateRoot.getId();
    }
}
