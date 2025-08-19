package solid.humank.genaidemo.infrastructure.promotion.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 優惠券JPA實體 */
@Entity
@Table(name = "vouchers")
public class JpaVoucherEntity {

    @Id
    private String id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "value_amount", nullable = false)
    private BigDecimal valueAmount;

    @Column(name = "value_currency", nullable = false)
    private String valueCurrency;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "redemption_code", unique = true)
    private String redemptionCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "usage_location")
    private String usageLocation;

    @Column(name = "lost_report_reason")
    private String lostReportReason;

    @Column(name = "lost_reported_at")
    private LocalDateTime lostReportedAt;

    // Constructors
    public JpaVoucherEntity() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(BigDecimal valueAmount) {
        this.valueAmount = valueAmount;
    }

    public String getValueCurrency() {
        return valueCurrency;
    }

    public void setValueCurrency(String valueCurrency) {
        this.valueCurrency = valueCurrency;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public void setRedemptionCode(String redemptionCode) {
        this.redemptionCode = redemptionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(LocalDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getUsageLocation() {
        return usageLocation;
    }

    public void setUsageLocation(String usageLocation) {
        this.usageLocation = usageLocation;
    }

    public String getLostReportReason() {
        return lostReportReason;
    }

    public void setLostReportReason(String lostReportReason) {
        this.lostReportReason = lostReportReason;
    }

    public LocalDateTime getLostReportedAt() {
        return lostReportedAt;
    }

    public void setLostReportedAt(LocalDateTime lostReportedAt) {
        this.lostReportedAt = lostReportedAt;
    }
}