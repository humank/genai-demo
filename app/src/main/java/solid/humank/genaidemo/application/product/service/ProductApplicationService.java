package solid.humank.genaidemo.application.product.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.common.service.DomainEventApplicationService;
import solid.humank.genaidemo.application.product.PriceDto;
import solid.humank.genaidemo.application.product.ProductDto;
import solid.humank.genaidemo.application.product.ProductPageDto;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.repository.ProductRepository;

/** 產品應用服務 */
@Service
@Transactional
public class ProductApplicationService
                implements solid.humank.genaidemo.application.product.port.incoming.ProductManagementUseCase {

        private final ProductRepository productRepository;
        private final DomainEventApplicationService domainEventApplicationService;

        public ProductApplicationService(
                        ProductRepository productRepository,
                        DomainEventApplicationService domainEventApplicationService) {
                this.productRepository = productRepository;
                this.domainEventApplicationService = domainEventApplicationService;
        }

        /** 獲取產品詳情 */
        public Optional<ProductDto> getProduct(String productId) {
                ProductId id = new ProductId(productId);
                Optional<Product> product = productRepository.findById(id);
                return product.map(this::toDto);
        }

        /** 分頁獲取產品列表 */
        public ProductPageDto getProducts(int page, int size) {
                List<Product> products = productRepository.findAll(); // 暫時使用 findAll，稍後可以改為分頁
                long totalElements = productRepository.count();

                List<ProductDto> productDtos = products.stream().map(this::toDto).toList();

                return new ProductPageDto(
                                productDtos,
                                (int) totalElements,
                                (int) Math.ceil((double) totalElements / size),
                                size,
                                page,
                                page == 0,
                                page >= Math.ceil((double) totalElements / size) - 1);
        }

        /** 檢查產品是否存在 */
        public boolean productExists(String productId) {
                ProductId id = new ProductId(productId);
                return productRepository.existsById(id);
        }

        /** 更新產品信息 */
        public ProductDto updateProduct(
                        solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand command) {
                ProductId id = new ProductId(command.getProductId());
                Optional<Product> existingProduct = productRepository.findById(id);

                if (existingProduct.isEmpty()) {
                        throw new RuntimeException("產品不存在: " + command.getProductId());
                }

                Product oldProduct = existingProduct.get();

                // 創建新的產品實例（不可變對象）
                Product updatedProduct = new Product(
                                id,
                                new solid.humank.genaidemo.domain.product.model.valueobject.ProductName(
                                                command.getName()),
                                new solid.humank.genaidemo.domain.product.model.valueobject.ProductDescription(
                                                command.getDescription()),
                                new solid.humank.genaidemo.domain.common.valueobject.Money(
                                                command.getPrice(),
                                                java.util.Currency.getInstance(command.getCurrency())),
                                new solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory(
                                                command.getCategory(), command.getCategory()),
                                oldProduct.getStockQuantity(), // 保持原有庫存
                                oldProduct.getImageUrl() // 保持原有圖片
                );

                Product savedProduct = productRepository.save(updatedProduct);

                // 發布領域事件
                domainEventApplicationService.publishEventsFromAggregate(savedProduct);

                return toDto(savedProduct);
        }

        /** 刪除產品 */
        public void deleteProduct(String productId) {
                ProductId id = new ProductId(productId);
                if (!productRepository.existsById(id)) {
                        throw new RuntimeException("產品不存在: " + productId);
                }
                productRepository.deleteById(id);
        }

        private ProductDto toDto(Product product) {
                return new ProductDto(
                                product.getId().getId(),
                                product.getName().getName(),
                                product.getDescription().getDescription(),
                                new PriceDto(
                                                product.getPrice().getAmount(),
                                                product.getPrice().getCurrency().getCurrencyCode()),
                                product.getCategory().getName(),
                                solid.humank.genaidemo.application.product.dto.ProductStatus.ACTIVE, // Default status
                                product.isInStock(),
                                product.getStockQuantity().getValue(),
                                null, // sku
                                null, // brand
                                null, // model
                                null, // weight
                                null, // barcode
                                null, // tags
                                product.getImageUrl() != null ? List
                                                .of(new solid.humank.genaidemo.application.product.dto.ProductImageDto(
                                                                "img-" + product.getId().getId(), // id
                                                                product.getImageUrl(), // url
                                                                product.getName().getName(), // altText
                                                                product.getName().getName(), // title
                                                                solid.humank.genaidemo.application.product.dto.ProductImageDto.ImageType.PRIMARY
                                                                                .name(), // type
                                                                0, // sortOrder
                                                                800, // width
                                                                600, // height
                                                                null, // fileSize
                                                                "image/jpeg" // mimeType
                                                )) : null, // images
                                null, // attributes
                                null, // warrantyMonths
                                null, // manufacturer
                                null, // countryOfOrigin
                                java.time.LocalDateTime.now(), // createdAt
                                java.time.LocalDateTime.now() // updatedAt
                );
        }
}
