package solid.humank.genaidemo.infrastructure.shoppingcart.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.port.ShoppingCartRepository;
import solid.humank.genaidemo.infrastructure.entity.JpaCartItemEntity;
import solid.humank.genaidemo.infrastructure.entity.JpaShoppingCartEntity;

/** 購物車Repository適配器 */
@Component
public class ShoppingCartRepositoryAdapter implements ShoppingCartRepository {

    private final JpaShoppingCartRepository jpaRepository;

    public ShoppingCartRepositoryAdapter(JpaShoppingCartRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ShoppingCart save(ShoppingCart cart) {
        JpaShoppingCartEntity entity = toEntity(cart);
        JpaShoppingCartEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<ShoppingCart> findById(ShoppingCartId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<ShoppingCart> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.getId()).map(this::toDomain);
    }

    @Override
    public void delete(ShoppingCart cart) {
        jpaRepository.deleteById(cart.getId().value());
    }

    private JpaShoppingCartEntity toEntity(ShoppingCart cart) {
        JpaShoppingCartEntity entity = new JpaShoppingCartEntity();
        entity.setId(cart.getId().value());
        entity.setCustomerId(cart.getConsumerId().getId());
        entity.setTotalAmount(cart.getTotalAmount().getAmount());
        entity.setCurrency(cart.getTotalAmount().getCurrency().getCurrencyCode());
        entity.setDiscountAmount(calculateDiscountAmount(cart));
        entity.setFinalAmount(cart.getTotalAmount().getAmount());
        entity.setStatus(cart.getStatus().name());
        entity.setCreatedAt(cart.getCreatedAt());
        entity.setUpdatedAt(cart.getUpdatedAt());

        // 轉換購物車項目
        List<JpaCartItemEntity> itemEntities =
                cart.getItems().stream().map(this::toCartItemEntity).collect(Collectors.toList());
        entity.setItems(itemEntities);

        return entity;
    }

    private JpaCartItemEntity toCartItemEntity(CartItem item) {
        JpaCartItemEntity entity = new JpaCartItemEntity();
        entity.setProductId(item.productId().getId());
        entity.setQuantity(item.quantity());
        entity.setUnitPrice(item.unitPrice().getAmount());
        entity.setCurrency(item.unitPrice().getCurrency().getCurrencyCode());
        entity.setTotalPrice(item.totalPrice().getAmount());
        return entity;
    }

    private ShoppingCart toDomain(JpaShoppingCartEntity entity) {
        ShoppingCartId id = ShoppingCartId.of(entity.getId());
        CustomerId customerId = new CustomerId(entity.getCustomerId());

        ShoppingCart cart = new ShoppingCart(id, customerId);

        // 重建購物車項目
        for (JpaCartItemEntity itemEntity : entity.getItems()) {
            Money unitPrice =
                    new Money(
                            itemEntity.getUnitPrice(),
                            java.util.Currency.getInstance(itemEntity.getCurrency()));

            cart.addItem(
                    new ProductId(itemEntity.getProductId()), itemEntity.getQuantity(), unitPrice);
        }

        return cart;
    }

    private java.math.BigDecimal calculateDiscountAmount(ShoppingCart cart) {
        // 簡化實現：計算折扣金額
        java.math.BigDecimal totalAmount = cart.getTotalAmount().getAmount();

        // 假設滿1000元打9折
        if (totalAmount.compareTo(java.math.BigDecimal.valueOf(1000)) >= 0) {
            return totalAmount.multiply(java.math.BigDecimal.valueOf(0.1));
        }

        return java.math.BigDecimal.ZERO;
    }
}
