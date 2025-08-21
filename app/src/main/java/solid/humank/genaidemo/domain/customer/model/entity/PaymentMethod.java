package solid.humank.genaidemo.domain.customer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.customer.model.valueobject.PaymentMethodId;

/**
 * 支付方式 Entity
 * 
 * 管理客戶的支付方式，包含卡片資訊、數位錢包等
 */
@Entity(name = "PaymentMethod", description = "支付方式實體，管理客戶的各種支付方式和相關設定")
public class PaymentMethod {
    
    public enum PaymentType {
        CREDIT_CARD("信用卡"),
        DEBIT_CARD("金融卡"),
        BANK_TRANSFER("銀行轉帳"),
        DIGITAL_WALLET("數位錢包"),
        MOBILE_PAYMENT("行動支付"),
        CRYPTOCURRENCY("加密貨幣"),
        GIFT_CARD("禮品卡"),
        STORE_CREDIT("商店信用");
        
        private final String description;
        
        PaymentType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum PaymentStatus {
        ACTIVE("有效"),
        INACTIVE("無效"),
        EXPIRED("已過期"),
        SUSPENDED("已暫停"),
        PENDING_VERIFICATION("待驗證");
        
        private final String description;
        
        PaymentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final PaymentMethodId id;
    private final PaymentType type;
    private String displayName;
    private String provider;
    private String maskedNumber;
    private String expiryDate;
    private String holderName;
    private String billingAddress;
    private boolean isDefault;
    private PaymentStatus status;
    private String verificationToken;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
    private int usageCount;
    private String notes;
    
    public PaymentMethod(PaymentMethodId id, PaymentType type, String displayName, 
                        String provider, String maskedNumber, String holderName) {
        this.id = Objects.requireNonNull(id, "PaymentMethod ID cannot be null");
        this.type = Objects.requireNonNull(type, "Payment type cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.provider = provider;
        this.maskedNumber = maskedNumber;
        this.holderName = holderName;
        this.billingAddress = "";
        this.isDefault = false;
        this.status = PaymentStatus.PENDING_VERIFICATION;
        this.verificationToken = null;
        this.lastUsedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.verifiedAt = null;
        this.usageCount = 0;
        this.notes = "";
        
        validatePaymentMethod();
    }
    
    // Getters
    public PaymentMethodId getId() { return id; }
    public PaymentType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getProvider() { return provider; }
    public String getMaskedNumber() { return maskedNumber; }
    public String getExpiryDate() { return expiryDate; }
    public String getHolderName() { return holderName; }
    public String getBillingAddress() { return billingAddress; }
    public boolean isDefault() { return isDefault; }
    public PaymentStatus getStatus() { return status; }
    public String getVerificationToken() { return verificationToken; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public int getUsageCount() { return usageCount; }
    public String getNotes() { return notes; }
    
    // 業務方法
    
    /** 設為預設支付方式 */
    public void setAsDefault() {
        if (!isActive()) {
            throw new IllegalStateException("無效的支付方式不能設為預設");
        }
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 取消預設設定 */
    public void unsetAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 啟用支付方式 */
    public void activate() {
        if (status == PaymentStatus.EXPIRED) {
            throw new IllegalStateException("已過期的支付方式無法啟用");
        }
        this.status = PaymentStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 停用支付方式 */
    public void deactivate() {
        this.status = PaymentStatus.INACTIVE;
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 暫停支付方式 */
    public void suspend() {
        this.status = PaymentStatus.SUSPENDED;
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 標記為已過期 */
    public void markAsExpired() {
        this.status = PaymentStatus.EXPIRED;
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 驗證支付方式 */
    public void verify(String verificationToken) {
        if (this.verificationToken == null || !this.verificationToken.equals(verificationToken)) {
            throw new IllegalArgumentException("驗證令牌無效");
        }
        this.status = PaymentStatus.ACTIVE;
        this.verifiedAt = LocalDateTime.now();
        this.verificationToken = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 設定驗證令牌 */
    public void setVerificationToken(String token) {
        this.verificationToken = token;
        this.status = PaymentStatus.PENDING_VERIFICATION;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 更新顯示名稱 */
    public void updateDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 更新到期日 */
    public void updateExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = LocalDateTime.now();
        
        if (isExpired()) {
            markAsExpired();
        }
    }
    
    /** 更新持卡人姓名 */
    public void updateHolderName(String holderName) {
        this.holderName = holderName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 更新帳單地址 */
    public void updateBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress != null ? billingAddress : "";
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 更新備註 */
    public void updateNotes(String notes) {
        this.notes = notes != null ? notes : "";
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 記錄使用 */
    public void recordUsage() {
        if (!isActive()) {
            throw new IllegalStateException("無效的支付方式無法使用");
        }
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /** 檢查是否有效 */
    public boolean isActive() {
        return status == PaymentStatus.ACTIVE;
    }
    
    /** 檢查是否已驗證 */
    public boolean isVerified() {
        return verifiedAt != null && status == PaymentStatus.ACTIVE;
    }
    
    /** 檢查是否已過期 */
    public boolean isExpired() {
        if (expiryDate == null || expiryDate.isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = expiryDate.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]);
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiry = LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1).minusDays(1);
            
            return now.isAfter(expiry);
        } catch (Exception e) {
            return false;
        }
    }
    
    /** 檢查是否可以使用 */
    public boolean canUse() {
        return isActive() && !isExpired();
    }
    
    /** 檢查是否為信用卡 */
    public boolean isCreditCard() {
        return type == PaymentType.CREDIT_CARD;
    }
    
    /** 檢查是否為數位錢包 */
    public boolean isDigitalWallet() {
        return type == PaymentType.DIGITAL_WALLET;
    }
    
    /** 檢查是否需要驗證 */
    public boolean needsVerification() {
        return status == PaymentStatus.PENDING_VERIFICATION;
    }
    
    /** 獲取支付方式摘要 */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(type.getDescription());
        if (provider != null && !provider.isEmpty()) {
            summary.append(" - ").append(provider);
        }
        if (maskedNumber != null && !maskedNumber.isEmpty()) {
            summary.append(" ").append(maskedNumber);
        }
        return summary.toString();
    }
    
    /** 驗證支付方式資料 */
    private void validatePaymentMethod() {
        if (displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("顯示名稱不能為空");
        }
        
        if (isCreditCard() && (maskedNumber == null || maskedNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("信用卡必須提供遮罩號碼");
        }
    }
    
    /** 創建信用卡支付方式 */
    public static PaymentMethod createCreditCard(String displayName, String provider, 
                                               String maskedNumber, String expiryDate, 
                                               String holderName) {
        PaymentMethod paymentMethod = new PaymentMethod(
            PaymentMethodId.generate(),
            PaymentType.CREDIT_CARD,
            displayName,
            provider,
            maskedNumber,
            holderName
        );
        paymentMethod.updateExpiryDate(expiryDate);
        return paymentMethod;
    }
    
    /** 創建數位錢包支付方式 */
    public static PaymentMethod createDigitalWallet(String displayName, String provider, 
                                                  String accountInfo) {
        return new PaymentMethod(
            PaymentMethodId.generate(),
            PaymentType.DIGITAL_WALLET,
            displayName,
            provider,
            accountInfo,
            null
        );
    }
    
    /** 創建銀行轉帳支付方式 */
    public static PaymentMethod createBankTransfer(String displayName, String bankName, 
                                                 String maskedAccountNumber, String holderName) {
        return new PaymentMethod(
            PaymentMethodId.generate(),
            PaymentType.BANK_TRANSFER,
            displayName,
            bankName,
            maskedAccountNumber,
            holderName
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PaymentMethod that = (PaymentMethod) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("PaymentMethod{id=%s, type=%s, displayName='%s', provider='%s', status=%s}", 
                id, type, displayName, provider, status);
    }
}