package solid.humank.genaidemo.infrastructure.seller.persistence.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.seller.model.aggregate.Seller;
import solid.humank.genaidemo.domain.seller.model.entity.ContactInfo;
import solid.humank.genaidemo.domain.seller.model.entity.SellerProfile;
import solid.humank.genaidemo.domain.seller.model.entity.SellerVerification;
import solid.humank.genaidemo.domain.seller.model.valueobject.ContactInfoId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerProfileId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerVerificationId;
import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerEntity;

/** 賣家映射器 - 包含從 SellerProfile 遷移的映射邏輯 */
@Component
public class SellerMapper {

    public JpaSellerEntity toJpaEntity(Seller seller) {
        JpaSellerEntity entity = new JpaSellerEntity();
        entity.setSellerId(seller.getSellerId().getId());
        entity.setName(seller.getName());
        entity.setEmail(seller.getEmail());
        entity.setPhone(seller.getPhone());
        entity.setActive(seller.isActive());
        entity.setCreatedAt(seller.getCreatedAt());
        entity.setLastUpdated(seller.getLastUpdated());

        // 映射 SellerProfile 資料
        if (seller.getProfile() != null) {
            SellerProfile profile = seller.getProfile();
            entity.setBusinessName(profile.getBusinessName());
            entity.setBusinessAddress(profile.getBusinessAddress());
            entity.setBusinessLicense(profile.getBusinessLicense());
            entity.setDescription(profile.getDescription());
            entity.setJoinedAt(profile.getJoinedAt());
            entity.setLastActiveAt(profile.getLastActiveAt());
        }

        // 映射驗證狀態
        if (seller.getVerification() != null) {
            entity.setVerified(seller.isVerified());
            entity.setVerificationStatus(seller.getVerificationStatus());
        }

        // 映射評級資料
        entity.setRating(seller.getRating());
        entity.setTotalReviews(seller.getTotalReviews());
        entity.setTotalSales(0); // 預設值，可以後續從其他地方計算

        return entity;
    }

    public Seller toDomainModel(JpaSellerEntity entity) {
        SellerId sellerId = new SellerId(entity.getSellerId());

        // 重建 SellerProfile Entity
        SellerProfile profile = new SellerProfile(
                SellerProfileId.generate(),
                entity.getBusinessName() != null ? entity.getBusinessName() : entity.getName(),
                entity.getBusinessAddress(),
                entity.getBusinessLicense());

        // 設置 profile 的其他屬性（需要使用反射或提供 setter 方法）
        // 這裡簡化處理，實際實作可能需要更複雜的重建邏輯

        // 重建 ContactInfo Entity
        ContactInfo contactInfo = new ContactInfo(
                ContactInfoId.generate(),
                entity.getEmail(),
                entity.getPhone());

        // 重建 SellerVerification Entity
        SellerVerification verification = new SellerVerification(
                SellerVerificationId.generate());

        // 設置驗證狀態
        if (entity.isVerified()) {
            verification.approve("system", LocalDateTime.now().plusYears(1));
        }

        // 使用重建用建構子創建 Seller
        return new Seller(
                sellerId,
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now(),
                entity.getLastUpdated() != null ? entity.getLastUpdated() : LocalDateTime.now(),
                profile,
                contactInfo,
                new ArrayList<>(), // ratings - 可以從其他表載入
                verification);
    }
}