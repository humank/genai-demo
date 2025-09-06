package solid.humank.genaidemo.domain.inventory.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryThresholdId;

/**
 * 庫存閾值 Entity
 * 
 * 管理庫存閾值規則，包含不同類型的閾值和警告機制
 */
@Entity(name = "InventoryThreshold", description = "庫存閾值實體，管理庫存閾值規則和警告機制")
public class InventoryThreshold {

    public enum ThresholdType {
        LOW_STOCK("低庫存警告"),
        CRITICAL_STOCK("庫存緊急警告"),
        REORDER_POINT("補貨點"),
        SAFETY_STOCK("安全庫存"),
        MAX_STOCK("最大庫存"),
        SEASONAL_ADJUSTMENT("季節性調整");

        private final String description;

        ThresholdType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AlertLevel {
        INFO("資訊"),
        WARNING("警告"),
        CRITICAL("緊急"),
        URGENT("急迫");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final InventoryThresholdId id;
    private final ThresholdType type;
    private int thresholdValue;
    private final AlertLevel alertLevel;
    private boolean isActive;
    private final String description;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime lastTriggeredAt;
    private int triggerCount; // 觸發次數
    private String notes; // 備註

    // 自動補貨相關
    private boolean autoReorderEnabled;
    private int reorderQuantity;
    private String supplierId;

    public InventoryThreshold(InventoryThresholdId id, ThresholdType type, int thresholdValue,
            AlertLevel alertLevel, String description) {
        this.id = Objects.requireNonNull(id, "InventoryThreshold ID cannot be null");
        this.type = Objects.requireNonNull(type, "Threshold type cannot be null");
        this.thresholdValue = validateThresholdValue(thresholdValue);
        this.alertLevel = Objects.requireNonNull(alertLevel, "Alert level cannot be null");
        this.description = description != null ? description : type.getDescription();
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = this.createdAt;
        this.lastTriggeredAt = null;
        this.triggerCount = 0;
        this.notes = "";
        this.autoReorderEnabled = false;
        this.reorderQuantity = 0;
        this.supplierId = null;
    }

    // Getters
    public InventoryThresholdId getId() {
        return id;
    }

    public ThresholdType getType() {
        return type;
    }

    public int getThresholdValue() {
        return thresholdValue;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isAutoReorderEnabled() {
        return autoReorderEnabled;
    }

    public int getReorderQuantity() {
        return reorderQuantity;
    }

    public String getSupplierId() {
        return supplierId;
    }

    // 業務方法

    /** 檢查庫存是否觸發閾值 */
    public boolean isTriggered(int currentStock) {
        if (!isActive) {
            return false;
        }

        switch (type) {
            case LOW_STOCK:
            case CRITICAL_STOCK:
            case REORDER_POINT:
            case SAFETY_STOCK:
                return currentStock <= thresholdValue;
            case MAX_STOCK:
                return currentStock >= thresholdValue;
            case SEASONAL_ADJUSTMENT:
                // 季節性調整需要額外的邏輯
                return evaluateSeasonalThreshold(currentStock);
            default:
                return false;
        }
    }

    /** 觸發閾值警告 */
    public void trigger() {
        if (!isActive) {
            throw new IllegalStateException("閾值未啟用，無法觸發");
        }

        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount++;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 更新閾值 */
    public void updateThreshold(int newThresholdValue) {
        this.thresholdValue = validateThresholdValue(newThresholdValue);
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 啟用閾值 */
    public void activate() {
        this.isActive = true;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 停用閾值 */
    public void deactivate() {
        this.isActive = false;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 更新備註 */
    public void updateNotes(String notes) {
        this.notes = Optional.ofNullable(notes).orElse("");
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 設定自動補貨 */
    public void configureAutoReorder(boolean enabled, int reorderQuantity, String supplierId) {
        this.autoReorderEnabled = enabled;
        if (enabled) {
            if (reorderQuantity <= 0) {
                throw new IllegalArgumentException("補貨數量必須大於0");
            }
            if (supplierId == null || supplierId.isBlank()) {
                throw new IllegalArgumentException("供應商ID不能為空");
            }
            this.reorderQuantity = reorderQuantity;
            this.supplierId = supplierId;
        } else {
            this.reorderQuantity = 0;
            this.supplierId = null;
        }
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 檢查是否需要自動補貨 */
    public boolean shouldAutoReorder(int currentStock) {
        return autoReorderEnabled &&
                type == ThresholdType.REORDER_POINT &&
                isTriggered(currentStock);
    }

    /** 檢查是否為低庫存閾值 */
    public boolean isLowStockThreshold() {
        return type == ThresholdType.LOW_STOCK || type == ThresholdType.CRITICAL_STOCK;
    }

    /** 檢查是否為補貨點閾值 */
    public boolean isReorderPointThreshold() {
        return type == ThresholdType.REORDER_POINT;
    }

    /** 檢查是否為安全庫存閾值 */
    public boolean isSafetyStockThreshold() {
        return type == ThresholdType.SAFETY_STOCK;
    }

    /** 檢查是否為最大庫存閾值 */
    public boolean isMaxStockThreshold() {
        return type == ThresholdType.MAX_STOCK;
    }

    /** 檢查是否頻繁觸發（最近24小時內觸發超過5次） */
    public boolean isFrequentlyTriggered() {
        return lastTriggeredAt != null &&
                lastTriggeredAt.isAfter(LocalDateTime.now().minusHours(24)) &&
                triggerCount >= 5;
    }

    /** 重置觸發計數 */
    public void resetTriggerCount() {
        this.triggerCount = 0;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 獲取建議的補貨數量 */
    public int getSuggestedReorderQuantity(int currentStock) {
        if (type == ThresholdType.REORDER_POINT && autoReorderEnabled) {
            return reorderQuantity;
        }

        // 基於閾值計算建議補貨數量
        int shortage = thresholdValue - currentStock;
        return Math.max(shortage, 0) + (reorderQuantity > 0 ? reorderQuantity : thresholdValue);
    }

    /** 評估季節性閾值 */
    private boolean evaluateSeasonalThreshold(int currentStock) {
        // 這裡可以實現季節性邏輯，例如根據月份調整閾值
        // 目前簡化為基本比較
        return currentStock <= thresholdValue;
    }

    /** 驗證閾值 */
    private int validateThresholdValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("閾值不能為負數");
        }
        return value;
    }

    /** 創建低庫存閾值 */
    public static InventoryThreshold createLowStockThreshold(int thresholdValue, String description) {
        return new InventoryThreshold(
                InventoryThresholdId.generate(),
                ThresholdType.LOW_STOCK,
                thresholdValue,
                AlertLevel.WARNING,
                description);
    }

    /** 創建緊急庫存閾值 */
    public static InventoryThreshold createCriticalStockThreshold(int thresholdValue, String description) {
        return new InventoryThreshold(
                InventoryThresholdId.generate(),
                ThresholdType.CRITICAL_STOCK,
                thresholdValue,
                AlertLevel.CRITICAL,
                description);
    }

    /** 創建補貨點閾值 */
    public static InventoryThreshold createReorderPointThreshold(int thresholdValue,
            int reorderQuantity,
            String supplierId,
            String description) {
        InventoryThreshold threshold = new InventoryThreshold(
                InventoryThresholdId.generate(),
                ThresholdType.REORDER_POINT,
                thresholdValue,
                AlertLevel.INFO,
                description);
        threshold.configureAutoReorder(true, reorderQuantity, supplierId);
        return threshold;
    }

    /** 創建安全庫存閾值 */
    public static InventoryThreshold createSafetyStockThreshold(int thresholdValue, String description) {
        return new InventoryThreshold(
                InventoryThresholdId.generate(),
                ThresholdType.SAFETY_STOCK,
                thresholdValue,
                AlertLevel.WARNING,
                description);
    }

    /** 創建最大庫存閾值 */
    public static InventoryThreshold createMaxStockThreshold(int thresholdValue, String description) {
        return new InventoryThreshold(
                InventoryThresholdId.generate(),
                ThresholdType.MAX_STOCK,
                thresholdValue,
                AlertLevel.WARNING,
                description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        InventoryThreshold that = (InventoryThreshold) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("InventoryThreshold{id=%s, type=%s, value=%d, level=%s, active=%s}",
                id, type, thresholdValue, alertLevel, isActive);
    }
}