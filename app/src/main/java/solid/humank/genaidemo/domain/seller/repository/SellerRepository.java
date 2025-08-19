package solid.humank.genaidemo.domain.seller.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.seller.model.aggregate.Seller;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/** 賣家儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "SellerRepository", description = "賣家聚合根儲存庫")
public interface SellerRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<Seller, SellerId> {

    /**
     * 根據電子郵件查詢賣家
     * 
     * @param email 電子郵件
     * @return 賣家（如果存在）
     */
    Optional<Seller> findByEmail(String email);

    /**
     * 根據電話號碼查詢賣家
     * 
     * @param phone 電話號碼
     * @return 賣家（如果存在）
     */
    Optional<Seller> findByPhone(String phone);

    /**
     * 查詢活躍的賣家
     * 
     * @return 活躍賣家列表
     */
    List<Seller> findActiveSellers();

    /**
     * 根據名稱模糊查詢賣家
     * 
     * @param name 賣家名稱
     * @return 賣家列表
     */
    List<Seller> findByNameContaining(String name);
}