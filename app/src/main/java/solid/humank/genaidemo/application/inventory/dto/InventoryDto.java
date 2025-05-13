package solid.humank.genaidemo.application.inventory.dto;

import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 庫存數據傳輸對象
 */
public class InventoryDto {
    private final String id;
    private final String productId;
    private final String productName;
    private final int totalQuantity;
    private final int availableQuantity;
    private final int reservedQuantity;
    private final int threshold;
    private final InventoryStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public InventoryDto(String id, String productId, String productName, int totalQuantity,
                        int availableQuantity, int reservedQuantity, int threshold,
                        InventoryStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    /**
     * 從領域模型創建DTO
     */
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
                inventory.getUpdatedAt()
        );
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