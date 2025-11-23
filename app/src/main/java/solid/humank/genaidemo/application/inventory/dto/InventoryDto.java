package solid.humank.genaidemo.application.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;

/** 庫存數據傳輸對象 */
@Schema(description = "庫存數據傳輸對象，包含完整的庫存資訊和狀態")
public class InventoryDto {
    @Schema(description = "庫存記錄唯一識別碼", example = "INV-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String id;

    @Schema(description = "產品唯一識別碼", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String productId;

    @Schema(description = "產品名稱", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String productName;

    @Schema(description = "總庫存數量，包含可用庫存和預留庫存", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int totalQuantity;

    @Schema(description = "可用庫存數量，可以立即銷售的庫存", example = "80", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int availableQuantity;

    @Schema(description = "預留庫存數量，已被訂單預留但尚未出貨的庫存", example = "20", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int reservedQuantity;

    @Schema(description = "庫存警戒閾值，當可用庫存低於此值時需要補貨", example = "10", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int threshold;

    @Schema(
            description = "庫存狀態",
            allowableValues = {"ACTIVE", "INACTIVE", "DISCONTINUED"},
            example = "ACTIVE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final InventoryStatus status;

    @Schema(description = "庫存記錄創建時間", example = "2024-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final LocalDateTime createdAt;

    @Schema(description = "庫存記錄最後更新時間", example = "2024-01-15T14:45:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final LocalDateTime updatedAt;

    public InventoryDto(
            String id,
            String productId,
            String productName,
            int totalQuantity,
            int availableQuantity,
            int reservedQuantity,
            int threshold,
            InventoryStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.threshold = threshold;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 從領域模型創建DTO */
    public static InventoryDto fromDomain(Inventory inventory) {
        return new InventoryDto(
                inventory.getId().toString(),
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getThreshold(),
                inventory.getStatus(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt());
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getThreshold() {
        return threshold;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    /**
     * 獲取庫存狀態的字符串表示
     *
     * @return 庫存狀態的字符串表示
     */
    public String getStatusName() {
        return status.name();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isBelowThreshold() {
        return threshold > 0 && availableQuantity < threshold;
    }

    public boolean isSufficient(int requiredQuantity) {
        return availableQuantity >= requiredQuantity;
    }
}
