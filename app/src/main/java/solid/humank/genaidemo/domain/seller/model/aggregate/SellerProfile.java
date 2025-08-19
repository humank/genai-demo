package solid.humank.genaidemo.domain.seller.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/** 賣家檔案聚合根 */
@AggregateRoot(name = "SellerProfile", description = "賣家檔案聚合根，管理賣家的詳細資訊和評級", boundedContext = "Seller", version = "1.0")
public class SellerProfile extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final SellerId sellerId;
    private String businessName;
    private String businessAddress;
    private String businessLicense;
    private String description;
    private double rating;
    private int totalReviews;
    private int totalSales;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    private boolean isVerified;
    private String verificationStatus;

    public SellerProfile(SellerId sellerId, String businessName, String businessAddress, String businessLicense) {
        this.sellerId = Objects.requireNonNull(sellerId, "賣家ID不能為空");
        this.businessName = Objects.requireNonNull(businessName, "商業名稱不能為空");
        this.businessAddress = businessAddress;
        this.businessLicense = businessLicense;
        this.rating = 0.0;
        this.totalReviews = 0;
        this.totalSales = 0;
        this.joinedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.isVerified = false;
        this.verificationStatus = "PENDING";
    }

    // Business methods
    public void updateBusinessInfo(String businessName, String businessAddress, String description) {
        this.businessName = Objects.requireNonNull(businessName, "商業名稱不能為空");
        this.businessAddress = businessAddress;
        this.description = description;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void updateRating(double newRating, int reviewCount) {
        this.rating = newRating;
        this.totalReviews = reviewCount;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void incrementSales() {
        this.totalSales++;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void verify() {
        this.isVerified = true;
        this.verificationStatus = "VERIFIED";
        this.lastActiveAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.isVerified = false;
        this.verificationStatus = "REJECTED: " + reason;
        this.lastActiveAt = LocalDateTime.now();
    }

    // Getters
    public SellerId getSellerId() {
        return sellerId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public String getBusinessLicense() {
        return businessLicense;
    }

    public String getDescription() {
        return description;
    }

    public double getRating() {
        return rating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getTotalSales() {
        return totalSales;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SellerProfile that = (SellerProfile) o;
        return Objects.equals(sellerId, that.sellerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerId);
    }
}