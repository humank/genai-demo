package solid.humank.genaidemo.domain.product.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

@Repository(name = "ProductRepository", description = "產品聚合根儲存庫")
public interface ProductRepository extends BaseRepository<Product, ProductId> {
    @Override
    Optional<Product> findById(ProductId productId);

    List<Product> findByCategory(ProductCategory category);

    List<Product> findByName(String name);

    @Override
    Product save(Product product);

    void delete(ProductId productId);
}
