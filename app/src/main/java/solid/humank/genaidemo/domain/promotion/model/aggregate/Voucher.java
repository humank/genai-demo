package solid.humank.genaidemo.domain.promotion.model.aggregate;

import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.exception.VoucherAlreadyUsedException;
import solid.humank.genaidemo.domain.promotion.exception.VoucherExpiredException;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherCode;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherStatus;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherType;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 超商優惠券聚合根 已移至 Promotion Context */
@AggregateRoot(name = "Voucher", description = "超商優惠券聚合根，管理優惠券的完整生命週期", boundedContext = "Promotion", version = "1.0")
public class Voucher extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final VoucherId id;
    private final VoucherType type;
    private final String name;
    private final String description;
    private final Money value;
    private final CustomerId ownerId;
    private VoucherCode code;
    private VoucherStatus status;
    private LocalDateTime purchasedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private String usageLocation;
    private String lostReportReason;
    private LocalDateTime lostReportedAt;

    public Voucher(
            VoucherId id,
            VoucherType type,
            String name,
            String description,
            Money value,
            CustomerId ownerId,
            int validDays) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.value = value;
        this.ownerId = ownerId;
        this.code = VoucherCode.generate(validDays);
        this.status = VoucherStatus.ACTIVE;
        this.purchasedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(validDays);
    }

    // Getters
    public VoucherId getId() {
        return id;
    }

    public VoucherType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getValue() {
        return value;
    }

    public CustomerId getOwnerId() {
        return ownerId;
    }

    public VoucherCode getCode() {
        return code;
    }

    public VoucherStatus getStatus() {
        return status;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getUsageLocation() {
        return usageLocation;
    }

    public String getLostReportReason() {
        return lostReportReason;
    }

    public LocalDateTime getLostReportedAt() {
        return lostReportedAt;
    }

    // 業務方法

    /** 使用優惠券 */
    public void use(String location) {
        if (status != VoucherStatus.ACTIVE) {
            throw new VoucherAlreadyUsedException("優惠券已被使用或無效");
        }

        if (isExpired()) {
            throw new VoucherExpiredException("優惠券已過期");
        }

        this.status = VoucherStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.usageLocation = location;

        // TODO: 發布領域事件
        // registerEvent(new VoucherUsedEvent(this.id, this.ownerId, location));
    }

    /** 檢查是否已過期 */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** 檢查是否可以使用 */
    public boolean canUse() {
        return status == VoucherStatus.ACTIVE && !isExpired();
    }

    /** 報失優惠券 */
    public void reportLost(String reason) {
        if (status == VoucherStatus.USED) {
            throw new IllegalStateException("已使用的優惠券無法報失");
        }

        this.status = VoucherStatus.LOST;
        this.lostReportReason = reason;
        this.lostReportedAt = LocalDateTime.now();

        // TODO: 發布領域事件
        // registerEvent(new VoucherLostReportedEvent(this.id, this.ownerId, reason));
    }

    /** 補發優惠券 */
    public Voucher reissue() {
        if (status != VoucherStatus.LOST) {
            throw new IllegalStateException("只有報失的優惠券才能補發");
        }

        // 計算剩餘有效天數
        long remainingDays = java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
        if (remainingDays <= 0) {
            throw new VoucherExpiredException("優惠券已過期，無法補發");
        }

        // 創建新的優惠券
        Voucher newVoucher = new Voucher(
                VoucherId.generate(),
                this.type,
                this.name,
                this.description,
                this.value,
                this.ownerId,
                (int) remainingDays);

        // 標記原優惠券為已補發
        this.status = VoucherStatus.REISSUED;

        return newVoucher;
    }

    /** 取消優惠券 */
    public void cancel() {
        if (status == VoucherStatus.USED) {
            throw new IllegalStateException("已使用的優惠券無法取消");
        }

        this.status = VoucherStatus.CANCELLED;

        // TODO: 發布領域事件
        // registerEvent(new VoucherCancelledEvent(this.id, this.ownerId));
    }

    /** 檢查優惠券是否屬於指定客戶 */
    public boolean belongsTo(CustomerId customerId) {
        return this.ownerId.equals(customerId);
    }

    /** 獲取剩餘有效天數 */
    public long getRemainingDays() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }

    // 為測試兼容性添加的方法

    /** 獲取有效天數 (為測試兼容性) */
    public int getValidDays() {
        return (int) java.time.Duration.between(purchasedAt, expiresAt).toDays();
    }

    /** 獲取兌換地點 (為測試兼容性) */
    public String getRedemptionLocation() {
        return usageLocation != null ? usageLocation : "Any 7-11 in Taiwan";
    }

    /** 獲取內容 (為測試兼容性) */
    public String getContents() {
        return description;
    }

    /** 獲取兌換碼 (為測試兼容性) */
    public String getRedemptionCode() {
        return code != null ? code.code() : null;
    }

    /** 獲取發行日期 (為測試兼容性) */
    public java.time.LocalDate getIssueDate() {
        return purchasedAt != null ? purchasedAt.toLocalDate() : null;
    }

    /** 獲取到期日期 (為測試兼容性) */
    public java.time.LocalDate getExpirationDate() {
        return expiresAt != null ? expiresAt.toLocalDate() : null;
    }

    /** 檢查是否有效 (為測試兼容性) */
    public boolean isValid() {
        return canUse();
    }

    /** 檢查是否已使用 (為測試兼容性) */
    public boolean isUsed() {
        return status == VoucherStatus.USED;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Voucher voucher = (Voucher) obj;
        return id.equals(voucher.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}