package solid.humank.genaidemo.infrastructure.shoppingcart.persistence.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity.JpaCartItemEntity;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity.JpaShoppingCartEntity;

/** 購物車映射器 負責在領域模型和JPA實體之間進行轉換 */
@Component
public class ShoppingCartMapper implements DomainMapper<ShoppingCart, JpaShoppingCartEntity> {

    /** 將領域模型轉換為JPA實體 */
    @Override
    public JpaShoppingCartEntity toJpaEntity(ShoppingCart cart) {
        JpaShoppingCartEntity entity = new JpaShoppingCartEntity();
        entity.setId(UUID.fromString(cart.getId().value()));
        entity.setCustomerId(cart.getConsumerId().getId());

        Money totalAmount = cart.getTotalAmount();
        entity.setTotalAmount(totalAmount.getAmount());
        entity.setCurrency(totalAmount.getCurrency().getCurrencyCode());
        entity.setDiscountAmount(BigDecimal.ZERO);
        entity.setFinalAmount(totalAmount.getAmount());
        entity.setStatus(mapDomainStatusToJpa(cart.getStatus()));

        for (CartItem item : cart.getItems()) {
            JpaCartItemEntity itemEntity = toJpaCartItemEntity(item);
            entity.addItem(itemEntity);
        }

        return entity;
    }

    /** 將JPA實體轉換為領域模型 */
    @Override
    public ShoppingCart toDomainModel(JpaShoppingCartEntity entity) {
        ShoppingCartId id = ShoppingCartId.of(entity.getId().toString());
        CustomerId customerId = new CustomerId(entity.getCustomerId());

        ShoppingCart cart = new ShoppingCart(id, customerId);

        for (JpaCartItemEntity itemEntity : entity.getItems()) {
            ProductId productId = new ProductId(itemEntity.getProductId());
            Money unitPrice = Money.of(itemEntity.getUnitPrice(), itemEntity.getCurrency());
            cart.addItem(productId, itemEntity.getQuantity(), unitPrice);
        }

        cart.updateStatus(mapJpaStatusToDomain(entity.getStatus()));

        return cart;
    }

    /** 將領域購物車項目轉換為JPA實體 */
    private static JpaCartItemEntity toJpaCartItemEntity(CartItem item) {
        JpaCartItemEntity entity = new JpaCartItemEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductId(item.productId().getId());
        entity.setProductName("");
        entity.setQuantity(item.quantity());
        entity.setUnitPrice(item.unitPrice().getAmount());
        entity.setTotalPrice(item.totalPrice().getAmount());
        entity.setCurrency(item.unitPrice().getCurrency().getCurrencyCode());
        return entity;
    }

    /** 將領域狀態映射到JPA狀態 */
    private static JpaShoppingCartEntity.CartStatusEnum mapDomainStatusToJpa(
            ShoppingCartStatus status) {
        switch (status) {
            case ACTIVE:
                return JpaShoppingCartEntity.CartStatusEnum.ACTIVE;
            case CHECKED_OUT:
                return JpaShoppingCartEntity.CartStatusEnum.CHECKED_OUT;
            case ABANDONED:
                return JpaShoppingCartEntity.CartStatusEnum.ABANDONED;
            default:
                throw new IllegalArgumentException("Unknown cart status: " + status);
        }
    }

    /** 將JPA狀態映射到領域狀態 */
    private static ShoppingCartStatus mapJpaStatusToDomain(
            JpaShoppingCartEntity.CartStatusEnum status) {
        switch (status) {
            case ACTIVE:
                return ShoppingCartStatus.ACTIVE;
            case CHECKED_OUT:
                return ShoppingCartStatus.CHECKED_OUT;
            case ABANDONED:
                return ShoppingCartStatus.ABANDONED;
            case EXPIRED:
                return ShoppingCartStatus.ABANDONED;
            default:
                throw new IllegalArgumentException("Unknown JPA cart status: " + status);
        }
    }

}
