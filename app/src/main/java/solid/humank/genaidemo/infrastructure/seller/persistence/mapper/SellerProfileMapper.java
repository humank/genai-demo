package solid.humank.genaidemo.infrastructure.seller.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.seller.model.aggregate.SellerProfile;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerProfileEntity;

/** 賣家檔案映射器 */
@Component
public class SellerProfileMapper {

    public JpaSellerProfileEntity toJpaEntity(SellerProfile sellerProfile) {
        JpaSellerProfileEntity entity = new JpaSellerProfileEntity();
        entity.setSellerId(sellerProfile.getSellerId().getId());
        entity.setBusinessName(sellerProfile.getBusinessName());
        entity.setBusinessAddress(sellerProfile.getBusinessAddress());
        entity.setBusinessLicense(sellerProfile.getBusinessLicense());
        entity.setDescription(sellerProfile.getDescription());
        entity.setRating(sellerProfile.getRating());
        entity.setTotalReviews(sellerProfile.getTotalReviews());
        entity.setTotalSales(sellerProfile.getTotalSales());
        entity.setJoinedAt(sellerProfile.getJoinedAt());
        entity.setLastActiveAt(sellerProfile.getLastActiveAt());
        entity.setVerified(sellerProfile.isVerified());
        entity.setVerificationStatus(sellerProfile.getVerificationStatus());
        return entity;
    }

    public SellerProfile toDomainModel(JpaSellerProfileEntity entity) {
        SellerId sellerId = new SellerId(entity.getSellerId());

        SellerProfile profile = new SellerProfile(
                sellerId,
                entity.getBusinessName(),
                entity.getBusinessAddress(),
                entity.getBusinessLicense());

        // Note: In a real implementation, you would need to set the internal state
        // This is a simplified version that shows the mapping pattern

        return profile;
    }
}