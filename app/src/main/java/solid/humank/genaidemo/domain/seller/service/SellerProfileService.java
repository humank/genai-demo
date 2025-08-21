package solid.humank.genaidemo.domain.seller.service;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.seller.model.aggregate.Seller;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.repository.SellerRepository;

/** 賣家檔案服務，處理賣家檔案相關的複雜業務邏輯 */
@DomainService(name = "SellerProfileService", description = "賣家檔案服務，處理賣家檔案相關的複雜業務邏輯", boundedContext = "Seller")
public class SellerProfileService {

    private final SellerRepository sellerRepository;

    public SellerProfileService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    /**
     * 驗證賣家檔案完整性
     * 
     * @param seller 賣家聚合根
     * @return 檔案是否完整
     */
    public boolean validateSellerProfile(Seller seller) {
        // 跨 Entity 的驗證邏輯
        return seller.hasCompleteProfile() &&
                seller.hasValidContactInfo() &&
                seller.getProfile() != null &&
                seller.getProfile().isProfileComplete();
    }

    /**
     * 計算賣家綜合評級
     * 
     * @param seller 賣家聚合根
     * @return 綜合評級
     */
    public double calculateOverallRating(Seller seller) {
        // 複雜的評級計算邏輯
        double averageRating = seller.calculateAverageRating();
        int totalRatings = seller.getTotalRatings();

        // 根據評級數量調整權重
        if (totalRatings < 5) {
            return averageRating * 0.8; // 評級數量少時降低權重
        } else if (totalRatings < 20) {
            return averageRating * 0.9;
        } else {
            return averageRating;
        }
    }

    /**
     * 檢查賣家是否符合推薦條件
     * 
     * @param seller 賣家聚合根
     * @return 是否符合推薦條件
     */
    public boolean isRecommendable(Seller seller) {
        return seller.isVerified() &&
                seller.isActive() &&
                calculateOverallRating(seller) >= 4.0 &&
                seller.getTotalRatings() >= 10;
    }

    /**
     * 根據賣家ID查詢檔案（向後相容方法）
     * 
     * @param sellerId 賣家ID
     * @return 賣家聚合根（如果存在）
     */
    public Optional<Seller> findProfileBySellerId(SellerId sellerId) {
        return sellerRepository.findById(sellerId);
    }

    /**
     * 查詢已驗證的賣家檔案（向後相容方法）
     * 
     * @return 已驗證的賣家列表
     */
    public List<Seller> findVerifiedProfiles() {
        return sellerRepository.findVerifiedSellers();
    }

    /**
     * 根據評級範圍查詢賣家檔案（向後相容方法）
     * 
     * @param minRating 最低評級
     * @param maxRating 最高評級
     * @return 賣家列表
     */
    public List<Seller> findProfilesByRatingRange(double minRating, double maxRating) {
        return sellerRepository.findByRatingBetween(minRating, maxRating);
    }

    /**
     * 根據驗證狀態查詢賣家檔案（向後相容方法）
     * 
     * @param verificationStatus 驗證狀態
     * @return 賣家列表
     */
    public List<Seller> findProfilesByVerificationStatus(String verificationStatus) {
        return sellerRepository.findByVerificationStatus(verificationStatus);
    }

    /**
     * 更新賣家檔案資訊
     * 
     * @param sellerId        賣家ID
     * @param businessName    商業名稱
     * @param businessAddress 商業地址
     * @param description     描述
     * @return 更新是否成功
     */
    public boolean updateSellerProfile(SellerId sellerId, String businessName,
            String businessAddress, String description) {
        Optional<Seller> sellerOpt = sellerRepository.findById(sellerId);
        if (sellerOpt.isPresent()) {
            Seller seller = sellerOpt.get();
            seller.updateBusinessInfo(businessName, businessAddress, description);
            sellerRepository.save(seller);
            return true;
        }
        return false;
    }

    /**
     * 批量驗證賣家檔案
     * 
     * @param sellerIds      賣家ID列表
     * @param verifierUserId 驗證者ID
     * @return 成功驗證的數量
     */
    public int batchVerifyProfiles(List<SellerId> sellerIds, String verifierUserId) {
        int successCount = 0;
        for (SellerId sellerId : sellerIds) {
            Optional<Seller> sellerOpt = sellerRepository.findById(sellerId);
            if (sellerOpt.isPresent()) {
                Seller seller = sellerOpt.get();
                if (validateSellerProfile(seller)) {
                    seller.approveVerification(verifierUserId,
                            java.time.LocalDateTime.now().plusYears(1));
                    sellerRepository.save(seller);
                    successCount++;
                }
            }
        }
        return successCount;
    }
}