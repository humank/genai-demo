package solid.humank.genaidemo.domain.seller.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.seller.model.aggregate.SellerProfile;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/** 賣家檔案儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "SellerProfileRepository", description = "賣家檔案聚合根儲存庫")
public interface SellerProfileRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<SellerProfile, SellerId> {

    /**
     * 根據賣家ID查詢檔案
     * 
     * @param sellerId 賣家ID
     * @return 賣家檔案（如果存在）
     */
    Optional<SellerProfile> findBySellerId(SellerId sellerId);

    /**
     * 查詢已驗證的賣家檔案
     * 
     * @return 已驗證的賣家檔案列表
     */
    List<SellerProfile> findVerifiedProfiles();

    /**
     * 根據評級範圍查詢賣家檔案
     * 
     * @param minRating 最低評級
     * @param maxRating 最高評級
     * @return 賣家檔案列表
     */
    List<SellerProfile> findByRatingBetween(double minRating, double maxRating);

    /**
     * 根據驗證狀態查詢賣家檔案
     * 
     * @param verificationStatus 驗證狀態
     * @return 賣家檔案列表
     */
    List<SellerProfile> findByVerificationStatus(String verificationStatus);
}