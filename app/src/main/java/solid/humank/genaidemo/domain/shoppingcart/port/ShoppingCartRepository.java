package solid.humank.genaidemo.domain.shoppingcart.port;

import java.util.Optional;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/** 購物車Repository接口 */
public interface ShoppingCartRepository {

    /** 保存購物車 */
    ShoppingCart save(ShoppingCart cart);

    /** 根據ID查找購物車 */
    Optional<ShoppingCart> findById(ShoppingCartId id);

    /** 根據客戶ID查找購物車 */
    Optional<ShoppingCart> findByCustomerId(CustomerId customerId);

    /** 刪除購物車 */
    void delete(ShoppingCart cart);
}
