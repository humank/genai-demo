package solid.humank.genaidemo.infrastructure.shoppingcart.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.entity.JpaShoppingCartEntity;

/** 購物車 JPA Repository */
@Repository
public interface JpaShoppingCartRepository extends JpaRepository<JpaShoppingCartEntity, String> {

    /** 根據客戶ID查找購物車 */
    Optional<JpaShoppingCartEntity> findByCustomerId(String customerId);

    /** 根據客戶ID和狀態查找購物車 */
    Optional<JpaShoppingCartEntity> findByCustomerIdAndStatus(String customerId, String status);
}
