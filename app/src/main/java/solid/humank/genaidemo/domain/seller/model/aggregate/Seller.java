package solid.humank.genaidemo.domain.seller.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.seller.model.entity.ContactInfo;
import solid.humank.genaidemo.domain.seller.model.entity.SellerProfile;
import solid.humank.genaidemo.domain.seller.model.entity.SellerRating;
import solid.humank.genaidemo.domain.seller.model.entity.SellerVerification;
import solid.humank.genaidemo.domain.seller.model.events.SellerProfileUpdatedEvent;
import solid.humank.genaidemo.domain.seller.model.events.SellerRatingAddedEvent;
import solid.humank.genaidemo.domain.seller.model.events.SellerRegisteredEvent;
import solid.humank.genaidemo.domain.seller.model.events.SellerVerificationApprovedEvent;
import solid.humank.genaidemo.domain.seller.model.valueobject.ContactInfoId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerProfileId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerRatingId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerVerificationId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 賣家聚合根 - 重構後包含豐富的 Entity 結構 */
@AggregateRoot(name = "Seller", description = "賣家聚合根，管理賣家基本資訊和狀態", boundedContext = "Seller", version = "2.0")
public class Seller extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final SellerId sellerId;
    private String name;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Entity 集合 - 新的聚合內結構
    private SellerProfile profile; // Entity (原 SellerProfile 聚合根)
    private ContactInfo contactInfo; // 新 Entity
    private List<SellerRating> ratings; // 新 Entity 集合
    private SellerVerification verification; // 新 Entity

    // Private constructor for JPA
    @SuppressWarnings("unused")
    private Seller() {
        this.sellerId = null;
        this.ratings = new ArrayList<>();
    }

    /**
     * 創建新賣家的建構子
     */
    public Seller(SellerId sellerId, String name, String email, String phone,
            String businessName, String businessAddress, String businessLicense) {
        this.sellerId = Objects.requireNonNull(sellerId, "賣家ID不能為空");
        this.name = Objects.requireNonNull(name, "賣家名稱不能為空");
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.ratings = new ArrayList<>();

        // 創建內部 Entity
        this.profile = new SellerProfile(
                SellerProfileId.generate(),
                businessName,
                businessAddress,
                businessLicense);

        this.contactInfo = new ContactInfo(
                ContactInfoId.generate(),
                email,
                phone);

        this.verification = new SellerVerification(
                SellerVerificationId.generate());

        // 發布賣家註冊事件
        collectEvent(SellerRegisteredEvent.create(sellerId, name, email));
    }

    /**
     * 重建用建構子（用於從持久化層重建）
     */
    public Seller(SellerId sellerId, String name, boolean isActive,
            LocalDateTime createdAt, LocalDateTime lastUpdated,
            SellerProfile profile, ContactInfo contactInfo,
            List<SellerRating> ratings, SellerVerification verification) {
        this.sellerId = Objects.requireNonNull(sellerId, "賣家ID不能為空");
        this.name = Objects.requireNonNull(name, "賣家名稱不能為空");
        this.isActive = isActive;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
        this.profile = profile;
        this.contactInfo = contactInfo;
        this.ratings = ratings != null ? new ArrayList<>(ratings) : new ArrayList<>();
        this.verification = verification;
    }

    /**
     * 向後相容的工廠方法 - 用於從舊的持久化資料重建
     * 這個方法提供向後相容性，確保現有程式碼不受影響
     */
    public static Seller createFromLegacyData(SellerId sellerId, String name, String email, String phone) {
        return createFromLegacyData(sellerId, name, email, phone, true);
    }

    /**
     * 向後相容的工廠方法 - 用於從舊的持久化資料重建（包含狀態）
     */
    public static Seller createFromLegacyData(SellerId sellerId, String name, String email, String phone,
            boolean isActive) {
        // 創建基本的 Entity 結構
        SellerProfile profile = new SellerProfile(
                SellerProfileId.generate(),
                name, // 使用 name 作為 businessName 的預設值
                null, // businessAddress
                null // businessLicense
        );

        ContactInfo contactInfo = new ContactInfo(
                ContactInfoId.generate(),
                email,
                phone);

        SellerVerification verification = new SellerVerification(
                SellerVerificationId.generate());

        return new Seller(
                sellerId,
                name,
                isActive,
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // lastUpdated
                profile,
                contactInfo,
                new ArrayList<>(), // ratings
                verification);
    }

    // 聚合根業務邏輯方法

    /**
     * 激活賣家
     */
    public void activate() {
        this.isActive = true;
        this.lastUpdated = LocalDateTime.now();
        if (profile != null) {
            profile.markAsActive();
        }
    }

    /**
     * 停用賣家
     */
    public void deactivate() {
        this.isActive = false;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 更新聯繫資訊 - 委派給 ContactInfo Entity
     */
    public void updateContactInfo(String email, String phone) {
        if (contactInfo != null) {
            contactInfo.updateContactInfo(email, phone);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * 更新商業資訊 - 委派給 SellerProfile Entity
     */
    public void updateBusinessInfo(String businessName, String businessAddress, String description) {
        if (profile != null) {
            profile.updateBusinessInfo(businessName, businessAddress, description);
            this.lastUpdated = LocalDateTime.now();

            // 發布檔案更新事件
            collectEvent(SellerProfileUpdatedEvent.create(sellerId, businessName, businessAddress, description));
        }
    }

    /**
     * 添加評級 - 管理 SellerRating Entity 集合
     */
    public void addRating(CustomerId customerId, int rating, String comment) {
        SellerRatingId ratingId = SellerRatingId.generate();
        SellerRating newRating = new SellerRating(
                ratingId,
                customerId,
                rating,
                comment);
        ratings.add(newRating);
        this.lastUpdated = LocalDateTime.now();

        // 發布評級添加事件
        collectEvent(SellerRatingAddedEvent.create(sellerId, ratingId, customerId, rating, comment));
    }

    /**
     * 隱藏評級
     */
    public void hideRating(SellerRatingId ratingId, String moderatorComment) {
        findRatingById(ratingId).ifPresent(rating -> {
            rating.hide(moderatorComment);
            this.lastUpdated = LocalDateTime.now();
        });
    }

    /**
     * 提交驗證文件 - 委派給 SellerVerification Entity
     */
    public void submitVerificationDocument(String documentPath) {
        if (verification != null) {
            verification.submitDocument(documentPath);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * 驗證通過 - 委派給 SellerVerification Entity
     */
    public void approveVerification(String verifierUserId, LocalDateTime expiresAt) {
        if (verification != null) {
            verification.approve(verifierUserId, expiresAt);
            this.lastUpdated = LocalDateTime.now();

            // 發布驗證通過事件
            collectEvent(SellerVerificationApprovedEvent.create(
                    sellerId, verifierUserId, verification.getVerifiedAt(), expiresAt));
        }
    }

    /**
     * 驗證拒絕 - 委派給 SellerVerification Entity
     */
    public void rejectVerification(String verifierUserId, String reason) {
        if (verification != null) {
            verification.reject(verifierUserId, reason);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    // 聚合內一致性規則和業務邏輯

    /**
     * 檢查賣家是否可以接受訂單
     */
    public boolean canAcceptOrders() {
        return isActive &&
                isVerified() &&
                hasValidContactInfo() &&
                hasCompleteProfile();
    }

    /**
     * 檢查是否已驗證
     */
    public boolean isVerified() {
        return verification != null && verification.isVerified();
    }

    /**
     * 檢查是否有有效的聯繫資訊
     */
    public boolean hasValidContactInfo() {
        return contactInfo != null && contactInfo.hasValidContactMethod();
    }

    /**
     * 檢查檔案是否完整
     */
    public boolean hasCompleteProfile() {
        return profile != null && profile.isProfileComplete();
    }

    /**
     * 計算平均評級
     */
    public double calculateAverageRating() {
        List<SellerRating> activeRatings = getActiveRatings();
        if (activeRatings.isEmpty()) {
            return 0.0;
        }

        return activeRatings.stream()
                .mapToInt(SellerRating::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * 獲取總評級數
     */
    public int getTotalRatings() {
        return getActiveRatings().size();
    }

    /**
     * 獲取活躍評級列表
     */
    public List<SellerRating> getActiveRatings() {
        return ratings.stream()
                .filter(SellerRating::isVisible)
                .toList();
    }

    /**
     * 根據ID查找評級
     */
    private Optional<SellerRating> findRatingById(SellerRatingId ratingId) {
        return ratings.stream()
                .filter(rating -> rating.getId().equals(ratingId))
                .findFirst();
    }

    // 向後相容的 API - 確保現有程式碼不受影響

    /**
     * 獲取郵箱 - 向後相容
     */
    public String getEmail() {
        return contactInfo != null ? contactInfo.getEmail() : null;
    }

    /**
     * 獲取電話 - 向後相容
     */
    public String getPhone() {
        return contactInfo != null ? contactInfo.getPhone() : null;
    }

    /**
     * 獲取商業名稱 - 向後相容
     */
    public String getBusinessName() {
        return profile != null ? profile.getBusinessName() : null;
    }

    /**
     * 獲取商業地址 - 向後相容
     */
    public String getBusinessAddress() {
        return profile != null ? profile.getBusinessAddress() : null;
    }

    /**
     * 獲取商業執照 - 向後相容
     */
    public String getBusinessLicense() {
        return profile != null ? profile.getBusinessLicense() : null;
    }

    /**
     * 獲取描述 - 向後相容
     */
    public String getDescription() {
        return profile != null ? profile.getDescription() : null;
    }

    /**
     * 獲取評級 - 向後相容
     */
    public double getRating() {
        return calculateAverageRating();
    }

    /**
     * 獲取總評論數 - 向後相容
     */
    public int getTotalReviews() {
        return getTotalRatings();
    }

    /**
     * 獲取加入時間 - 向後相容
     */
    public LocalDateTime getJoinedAt() {
        return profile != null ? profile.getJoinedAt() : createdAt;
    }

    /**
     * 獲取最後活躍時間 - 向後相容
     */
    public LocalDateTime getLastActiveAt() {
        return profile != null ? profile.getLastActiveAt() : lastUpdated;
    }

    /**
     * 獲取驗證狀態 - 向後相容
     */
    public String getVerificationStatus() {
        if (verification == null) {
            return "PENDING";
        }
        return verification.getStatus().name();
    }

    // Override getId() 方法以支援 AggregateRoot 的 equals/hashCode
    @Override
    public Object getId() {
        return sellerId;
    }

    // Getters for Entity access

    public SellerId getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public SellerProfile getProfile() {
        return profile;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public List<SellerRating> getRatings() {
        return Collections.unmodifiableList(ratings);
    }

    public SellerVerification getVerification() {
        return verification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Seller seller = (Seller) o;
        return Objects.equals(sellerId, seller.sellerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerId);
    }

    @Override
    public String toString() {
        return "Seller{" +
                "sellerId=" + sellerId +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", isVerified=" + isVerified() +
                ", averageRating=" + calculateAverageRating() +
                '}';
    }
}
