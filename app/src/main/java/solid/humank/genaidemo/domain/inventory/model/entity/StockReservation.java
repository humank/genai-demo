package solid.humank.genaidemo.domain.inventory.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.StockReservationId;

/**
 * 庫存預留 Entity
 * 
 * 管理庫存預留的詳細資訊，包含預留狀態、過期時間等
 */
@Entity(name = "StockReservation", description = "庫存預留實體，管理庫存預留的詳細資訊和狀態")
public class StockReservation {

    public enum ReservationStatus {
        ACTIVE("有效"),
        EXPIRED("已過期"),
        CONFIRMED("已確認"),
        RELEASED("已釋放");

        private final String description;

        ReservationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ReservationType {
        ORDER("訂單預留"),
        CART("購物車預留"),
        PROMOTION("促銷預留"),
        MANUAL("手動預留");

        private final String description;

        ReservationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final StockReservationId id;
    private final ReservationId reservationId; // 向後兼容的 ID
    private final UUID orderId;
    private final String customerInfo; // 客戶資訊
    private final ReservationType type;
    private final int quantity;
    private final LocalDateTime reservedAt;
    private final LocalDateTime expiresAt;
    private ReservationStatus status;
    private LocalDateTime lastModifiedAt;
    private String notes; // 備註

    public StockReservation(StockReservationId id, ReservationId reservationId, UUID orderId,
            String customerInfo, ReservationType type, int quantity,
            LocalDateTime expiresAt) {
        this.id = Objects.requireNonNull(id, "StockReservation ID cannot be null");
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation ID cannot be null");
        this.orderId = orderId; // 可以為 null（如購物車預留）
        this.customerInfo = customerInfo;
        this.type = Objects.requireNonNull(type, "Reservation type cannot be null");
        this.quantity = validateQuantity(quantity);
        this.reservedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.ACTIVE;
        this.lastModifiedAt = this.reservedAt;
        this.notes = "";
    }

    // Getters
    public StockReservationId getId() {
        return id;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerInfo() {
        return customerInfo;
    }

    public ReservationType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getNotes() {
        return notes;
    }

    // 業務方法

    /** 檢查預留是否已過期 */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /** 檢查預留是否有效 */
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    /** 檢查預留是否可以確認 */
    public boolean canConfirm() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    /** 檢查預留是否可以釋放 */
    public boolean canRelease() {
        return status == ReservationStatus.ACTIVE || status == ReservationStatus.EXPIRED;
    }

    /** 確認預留 */
    public void confirm() {
        if (!canConfirm()) {
            throw new IllegalStateException("無法確認預留：狀態=" + status + ", 是否過期=" + isExpired());
        }
        this.status = ReservationStatus.CONFIRMED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 釋放預留 */
    public void release() {
        if (!canRelease()) {
            throw new IllegalStateException("無法釋放預留：狀態=" + status);
        }
        this.status = ReservationStatus.RELEASED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 標記為過期 */
    public void markAsExpired() {
        if (status == ReservationStatus.ACTIVE) {
            this.status = ReservationStatus.EXPIRED;
            this.lastModifiedAt = LocalDateTime.now();
        }
    }

    /** 延長預留時間 */
    public StockReservation extendExpiration(LocalDateTime newExpiresAt) {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("只能延長有效的預留");
        }
        if (newExpiresAt != null && newExpiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("新的過期時間不能早於當前時間");
        }

        return new StockReservation(
                this.id, this.reservationId, this.orderId, this.customerInfo,
                this.type, this.quantity, newExpiresAt);
    }

    /** 更新備註 */
    public void updateNotes(String notes) {
        this.notes = Optional.ofNullable(notes).orElse("");
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 獲取剩餘有效時間（分鐘） */
    public long getRemainingMinutes() {
        if (expiresAt == null) {
            return Long.MAX_VALUE;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        return java.time.Duration.between(now, expiresAt).toMinutes();
    }

    /** 檢查是否即將過期（30分鐘內） */
    public boolean isExpiringSoon() {
        return getRemainingMinutes() <= 30 && getRemainingMinutes() > 0;
    }

    /** 檢查是否為訂單預留 */
    public boolean isOrderReservation() {
        return type == ReservationType.ORDER && orderId != null;
    }

    /** 檢查是否為購物車預留 */
    public boolean isCartReservation() {
        return type == ReservationType.CART;
    }

    /** 檢查是否為促銷預留 */
    public boolean isPromotionReservation() {
        return type == ReservationType.PROMOTION;
    }

    /** 檢查是否為手動預留 */
    public boolean isManualReservation() {
        return type == ReservationType.MANUAL;
    }

    /** 驗證數量 */
    private int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("預留數量必須大於0");
        }
        return quantity;
    }

    /** 創建訂單預留 */
    public static StockReservation createOrderReservation(UUID orderId, String customerInfo,
            int quantity, LocalDateTime expiresAt) {
        return new StockReservation(
                StockReservationId.generate(),
                ReservationId.create(),
                orderId,
                customerInfo,
                ReservationType.ORDER,
                quantity,
                expiresAt);
    }

    /** 創建購物車預留 */
    public static StockReservation createCartReservation(String customerInfo, int quantity,
            LocalDateTime expiresAt) {
        return new StockReservation(
                StockReservationId.generate(),
                ReservationId.create(),
                null,
                customerInfo,
                ReservationType.CART,
                quantity,
                expiresAt);
    }

    /** 創建促銷預留 */
    public static StockReservation createPromotionReservation(String customerInfo, int quantity,
            LocalDateTime expiresAt) {
        return new StockReservation(
                StockReservationId.generate(),
                ReservationId.create(),
                null,
                customerInfo,
                ReservationType.PROMOTION,
                quantity,
                expiresAt);
    }

    /** 創建手動預留 */
    public static StockReservation createManualReservation(String customerInfo, int quantity,
            LocalDateTime expiresAt, String notes) {
        StockReservation reservation = new StockReservation(
                StockReservationId.generate(),
                ReservationId.create(),
                null,
                customerInfo,
                ReservationType.MANUAL,
                quantity,
                expiresAt);
        reservation.updateNotes(notes);
        return reservation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        StockReservation that = (StockReservation) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("StockReservation{id=%s, type=%s, quantity=%d, status=%s, expires=%s}",
                id, type, quantity, status, expiresAt);
    }
}