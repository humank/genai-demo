package solid.humank.genaidemo.domain.inventory.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.inventory.model.valueobject.StockMovementId;

/**
 * 庫存異動 Entity
 * 
 * 記錄庫存的所有異動歷史，包含入庫、出庫、預留、釋放等操作
 */
@Entity(name = "StockMovement", description = "庫存異動實體，記錄庫存的所有異動歷史和操作")
public class StockMovement {

    public enum MovementType {
        INBOUND("入庫"),
        OUTBOUND("出庫"),
        RESERVE("預留"),
        RELEASE("釋放"),
        CONFIRM("確認"),
        ADJUSTMENT("調整"),
        TRANSFER("轉移"),
        RETURN("退貨"),
        DAMAGE("損壞"),
        EXPIRED("過期");

        private final String description;

        MovementType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum MovementReason {
        PURCHASE("採購入庫"),
        SALE("銷售出庫"),
        ORDER_RESERVE("訂單預留"),
        CART_RESERVE("購物車預留"),
        PROMOTION_RESERVE("促銷預留"),
        MANUAL_RESERVE("手動預留"),
        ORDER_CANCEL("訂單取消"),
        CART_ABANDON("購物車放棄"),
        ORDER_CONFIRM("訂單確認"),
        INVENTORY_SYNC("庫存同步"),
        MANUAL_ADJUSTMENT("手動調整"),
        WAREHOUSE_TRANSFER("倉庫轉移"),
        CUSTOMER_RETURN("客戶退貨"),
        QUALITY_ISSUE("品質問題"),
        EXPIRATION("商品過期"),
        SYSTEM_CORRECTION("系統修正");

        private final String description;

        MovementReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final StockMovementId id;
    private final MovementType type;
    private final MovementReason reason;
    private final int quantity; // 正數表示增加，負數表示減少
    private final int beforeQuantity; // 異動前數量
    private final int afterQuantity; // 異動後數量
    private final String referenceId; // 關聯ID（訂單ID、預留ID等）
    private final String operatorId; // 操作者ID
    private final String operatorName; // 操作者名稱
    private final LocalDateTime occurredAt;
    private final String notes; // 備註
    private final String batchNumber; // 批次號
    private final String locationCode; // 位置代碼

    public StockMovement(StockMovementId id, MovementType type, MovementReason reason,
            int quantity, int beforeQuantity, int afterQuantity,
            String referenceId, String operatorId, String operatorName,
            String notes, String batchNumber, String locationCode) {
        this.id = Objects.requireNonNull(id, "StockMovement ID cannot be null");
        this.type = Objects.requireNonNull(type, "Movement type cannot be null");
        this.reason = Objects.requireNonNull(reason, "Movement reason cannot be null");
        this.quantity = quantity;
        this.beforeQuantity = validateQuantity(beforeQuantity, "Before quantity");
        this.afterQuantity = validateQuantity(afterQuantity, "After quantity");
        this.referenceId = referenceId;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.occurredAt = LocalDateTime.now();
        this.notes = Optional.ofNullable(notes).orElse("");
        this.batchNumber = batchNumber;
        this.locationCode = locationCode;

        validateMovement();
    }

    // Getters
    public StockMovementId getId() {
        return id;
    }

    public MovementType getType() {
        return type;
    }

    public MovementReason getReason() {
        return reason;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getBeforeQuantity() {
        return beforeQuantity;
    }

    public int getAfterQuantity() {
        return afterQuantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getNotes() {
        return notes;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public String getLocationCode() {
        return locationCode;
    }

    // 業務方法

    /** 檢查是否為入庫異動 */
    public boolean isInbound() {
        return type == MovementType.INBOUND || quantity > 0;
    }

    /** 檢查是否為出庫異動 */
    public boolean isOutbound() {
        return type == MovementType.OUTBOUND || quantity < 0;
    }

    /** 檢查是否為預留異動 */
    public boolean isReservation() {
        return type == MovementType.RESERVE;
    }

    /** 檢查是否為釋放異動 */
    public boolean isRelease() {
        return type == MovementType.RELEASE;
    }

    /** 檢查是否為確認異動 */
    public boolean isConfirmation() {
        return type == MovementType.CONFIRM;
    }

    /** 檢查是否為調整異動 */
    public boolean isAdjustment() {
        return type == MovementType.ADJUSTMENT;
    }

    /** 檢查是否為系統異動 */
    public boolean isSystemMovement() {
        return reason == MovementReason.INVENTORY_SYNC ||
                reason == MovementReason.SYSTEM_CORRECTION;
    }

    /** 檢查是否為手動異動 */
    public boolean isManualMovement() {
        return reason == MovementReason.MANUAL_ADJUSTMENT ||
                reason == MovementReason.MANUAL_RESERVE;
    }

    /** 檢查是否與訂單相關 */
    public boolean isOrderRelated() {
        return reason == MovementReason.ORDER_RESERVE ||
                reason == MovementReason.ORDER_CANCEL ||
                reason == MovementReason.ORDER_CONFIRM;
    }

    /** 檢查是否與購物車相關 */
    public boolean isCartRelated() {
        return reason == MovementReason.CART_RESERVE ||
                reason == MovementReason.CART_ABANDON;
    }

    /** 獲取異動的絕對數量 */
    public int getAbsoluteQuantity() {
        return Math.abs(quantity);
    }

    /** 獲取異動影響（正數表示增加庫存，負數表示減少庫存） */
    public int getImpact() {
        return afterQuantity - beforeQuantity;
    }

    /** 檢查異動是否一致 */
    public boolean isConsistent() {
        return (beforeQuantity + quantity) == afterQuantity;
    }

    /** 驗證數量 */
    private int validateQuantity(int quantity, String fieldName) {
        if (quantity < 0) {
            throw new IllegalArgumentException(fieldName + " 不能為負數");
        }
        return quantity;
    }

    /** 驗證異動邏輯 */
    private void validateMovement() {
        // 檢查數量一致性
        if (!isConsistent()) {
            throw new IllegalArgumentException(
                    String.format("異動數量不一致：%d + %d ≠ %d", beforeQuantity, quantity, afterQuantity));
        }

        // 檢查異動類型與數量的一致性
        switch (type) {
            case INBOUND:
                if (quantity <= 0) {
                    throw new IllegalArgumentException("入庫異動的數量必須為正數");
                }
                break;
            case OUTBOUND:
                if (quantity >= 0) {
                    throw new IllegalArgumentException("出庫異動的數量必須為負數");
                }
                break;
            case RESERVE:
                if (quantity >= 0) {
                    throw new IllegalArgumentException("預留異動的數量必須為負數");
                }
                break;
            case RELEASE:
                if (quantity <= 0) {
                    throw new IllegalArgumentException("釋放異動的數量必須為正數");
                }
                break;
            case CONFIRM:
                // 確認異動可以是正數或負數，取決於具體業務邏輯
                break;
            case ADJUSTMENT:
                // 調整異動可以是正數或負數
                break;
            case TRANSFER:
                // 轉移異動可以是正數或負數，取決於是轉入還是轉出
                break;
            case RETURN:
                if (quantity <= 0) {
                    throw new IllegalArgumentException("退貨異動的數量必須為正數");
                }
                break;
            case DAMAGE:
                if (quantity >= 0) {
                    throw new IllegalArgumentException("損壞異動的數量必須為負數");
                }
                break;
            case EXPIRED:
                if (quantity >= 0) {
                    throw new IllegalArgumentException("過期異動的數量必須為負數");
                }
                break;
        }
    }

    /** 創建入庫異動 */
    public static StockMovement createInbound(int quantity, int beforeQuantity,
            MovementReason reason, String referenceId,
            String operatorId, String operatorName,
            String notes, String batchNumber, String locationCode) {
        return new StockMovement(
                StockMovementId.generate(),
                MovementType.INBOUND,
                reason,
                Math.abs(quantity), // 確保為正數
                beforeQuantity,
                beforeQuantity + Math.abs(quantity),
                referenceId,
                operatorId,
                operatorName,
                notes,
                batchNumber,
                locationCode);
    }

    /** 創建出庫異動 */
    public static StockMovement createOutbound(int quantity, int beforeQuantity,
            MovementReason reason, String referenceId,
            String operatorId, String operatorName,
            String notes, String batchNumber, String locationCode) {
        int absQuantity = Math.abs(quantity);
        return new StockMovement(
                StockMovementId.generate(),
                MovementType.OUTBOUND,
                reason,
                -absQuantity, // 確保為負數
                beforeQuantity,
                beforeQuantity - absQuantity,
                referenceId,
                operatorId,
                operatorName,
                notes,
                batchNumber,
                locationCode);
    }

    /** 創建預留異動 */
    public static StockMovement createReservation(int quantity, int beforeQuantity,
            MovementReason reason, String referenceId,
            String operatorId, String operatorName,
            String notes) {
        int absQuantity = Math.abs(quantity);
        return new StockMovement(
                StockMovementId.generate(),
                MovementType.RESERVE,
                reason,
                -absQuantity, // 預留減少可用庫存
                beforeQuantity,
                beforeQuantity - absQuantity,
                referenceId,
                operatorId,
                operatorName,
                notes,
                null,
                null);
    }

    /** 創建釋放異動 */
    public static StockMovement createRelease(int quantity, int beforeQuantity,
            MovementReason reason, String referenceId,
            String operatorId, String operatorName,
            String notes) {
        int absQuantity = Math.abs(quantity);
        return new StockMovement(
                StockMovementId.generate(),
                MovementType.RELEASE,
                reason,
                absQuantity, // 釋放增加可用庫存
                beforeQuantity,
                beforeQuantity + absQuantity,
                referenceId,
                operatorId,
                operatorName,
                notes,
                null,
                null);
    }

    /** 創建調整異動 */
    public static StockMovement createAdjustment(int quantity, int beforeQuantity,
            MovementReason reason, String operatorId,
            String operatorName, String notes) {
        return new StockMovement(
                StockMovementId.generate(),
                MovementType.ADJUSTMENT,
                reason,
                quantity,
                beforeQuantity,
                beforeQuantity + quantity,
                null,
                operatorId,
                operatorName,
                notes,
                null,
                null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        StockMovement that = (StockMovement) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("StockMovement{id=%s, type=%s, reason=%s, quantity=%d, %d→%d, time=%s}",
                id, type, reason, quantity, beforeQuantity, afterQuantity, occurredAt);
    }
}