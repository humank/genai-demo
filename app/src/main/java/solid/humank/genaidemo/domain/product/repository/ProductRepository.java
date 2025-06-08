package solid.humank.genaidemo.domain.product.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends Repository<Product, ProductId> {
    @Override
    Optional<Product> findById(ProductId productId);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByName(String name);
    @Override
    Product save(Product product);
    void delete(ProductId productId);
}