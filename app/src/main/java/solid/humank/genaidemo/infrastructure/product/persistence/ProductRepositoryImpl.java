package solid.humank.genaidemo.infrastructure.product.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.repository.ProductRepository;

/** 產品儲存庫實現 - 使用 JPA */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ProductJpaRepository jpaRepository;
    private final ProductMapper mapper;

    public ProductRepositoryImpl(ProductJpaRepository jpaRepository, ProductMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return jpaRepository
                .findByProductIdAndStatus(productId.getId(), ACTIVE_STATUS)
                .map(mapper::toDomain);
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        return jpaRepository.findByCategoryAndStatus(category.getName(), ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByName(String name) {
        return jpaRepository.findByNameContainingIgnoreCaseAndStatus(name, ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        Optional<ProductJpaEntity> existingEntity =
                jpaRepository.findByProductIdAndStatus(product.getId().getId(), ACTIVE_STATUS);

        ProductJpaEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            mapper.updateEntity(entity, product);
        } else {
            entity = mapper.toEntity(product);
        }

        ProductJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Product product) {
        deleteById(product.getId());
    }

    @Override
    public void deleteById(ProductId productId) {
        jpaRepository
                .findByProductIdAndStatus(productId.getId(), ACTIVE_STATUS)
                .ifPresent(
                        entity -> {
                            entity.setStatus("DELETED");
                            jpaRepository.save(entity);
                        });
    }

    @Override
    public void delete(ProductId productId) {
        deleteById(productId);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findByStatus(ACTIVE_STATUS).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.countByStatus(ACTIVE_STATUS);
    }

    @Override
    public boolean existsById(ProductId productId) {
        return jpaRepository.existsByProductIdAndStatus(productId.getId(), ACTIVE_STATUS);
    }

    // 分頁查詢方法 (如果需要的話)
    public Page<Product> findProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByStatus(ACTIVE_STATUS, pageable).map(mapper::toDomain);
    }
}
