package solid.humank.genaidemo.infrastructure.product.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.repository.ProductRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.product.persistence.ProductJpaEntity;
import solid.humank.genaidemo.infrastructure.product.persistence.mapper.ProductMapper;
import solid.humank.genaidemo.infrastructure.product.persistence.repository.ProductJpaRepository;

/** 產品儲存庫適配器 - 使用 JPA，遵循統一的 Repository Pattern */
@Component
public class ProductRepositoryAdapter extends BaseRepositoryAdapter<Product, ProductId, ProductJpaEntity, String>
        implements ProductRepository {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ProductJpaRepository productJpaRepository;
    private final ProductMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository productJpaRepository, ProductMapper mapper) {
        super(productJpaRepository);
        this.productJpaRepository = productJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return productJpaRepository
                .findByProductIdAndStatus(productId.getId(), ACTIVE_STATUS)
                .map(mapper::toDomain);
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        return productJpaRepository.findByCategoryAndStatus(category.getName(), ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByName(String name) {
        return productJpaRepository.findByNameContainingIgnoreCaseAndStatus(name, ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Product save(Product product) {
        Optional<ProductJpaEntity> existingEntity = productJpaRepository
                .findByProductIdAndStatus(product.getId().getId(), ACTIVE_STATUS);

        ProductJpaEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            mapper.updateEntity(entity, product);
        } else {
            entity = mapper.toEntity(product);
        }

        productJpaRepository.save(entity);
        return product; // Return original aggregate root to maintain consistency
    }

    @Override
    public void deleteById(ProductId productId) {
        productJpaRepository
                .findByProductIdAndStatus(productId.getId(), ACTIVE_STATUS)
                .ifPresent(
                        entity -> {
                            entity.setStatus("DELETED");
                            productJpaRepository.save(entity);
                        });
    }

    @Override
    public void delete(ProductId productId) {
        deleteById(productId);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findByStatus(ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return productJpaRepository.countByStatus(ACTIVE_STATUS);
    }

    @Override
    public boolean existsById(ProductId productId) {
        return productJpaRepository.existsByProductIdAndStatus(productId.getId(), ACTIVE_STATUS);
    }

    // 分頁查詢方法 (如果需要的話)
    public Page<Product> findProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productJpaRepository.findByStatus(ACTIVE_STATUS, pageable).map(mapper::toDomain);
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected ProductJpaEntity toJpaEntity(Product aggregateRoot) {
        return mapper.toEntity(aggregateRoot);
    }

    @Override
    protected Product toDomainModel(ProductJpaEntity entity) {
        return mapper.toDomain(entity);
    }

    @Override
    protected String convertToJpaId(ProductId domainId) {
        return domainId.getId();
    }

    @Override
    protected ProductId extractId(Product aggregateRoot) {
        return aggregateRoot.getId();
    }
}