package solid.humank.genaidemo.domain.shoppingcart.repository;

import java.util.Optional;

import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 購物車Repository接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "ShoppingCartRepository", description = "購物車聚合根儲存庫")
public interface ShoppingCartRepository extends
        solid.humank.genaidemo.domain.common.repository.BaseRepository<solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart, solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId> {

    /** 根據客戶ID查找購物車 */
    Optional<ShoppingCart> findByCustomerId(CustomerId customerId);
}
