package solid.humank.genaidemo.application.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 庫存響應 DTO */
@Schema(description = "庫存資訊回應資料，包含產品的完整庫存狀態和相關資訊")
public class InventoryResponse {
    @Schema(description = "庫存記錄唯一識別碼", example = "INV-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "產品唯一識別碼", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productId;

    @Schema(description = "產品名稱", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    @Schema(description = "總庫存數量，包含可用庫存和預留庫存", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int totalQuantity;

    @Schema(description = "可用庫存數量，可以立即銷售的庫存", example = "80", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int availableQuantity;

    @Schema(description = "預留庫存數量，已被訂單預留但尚未出貨的庫存", example = "20", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int reservedQuantity;

    @Schema(description = "庫存警戒閾值，當可用庫存低於此值時需要補貨", example = "10", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int threshold;

    @Schema(
            description = "庫存狀態",
            allowableValues = {"SUFFICIENT", "LOW_STOCK", "OUT_OF_STOCK", "RESERVED"},
            example = "SUFFICIENT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "庫存記錄創建時間", example = "2024-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "庫存記錄最後更新時間", example = "2024-01-15T14:45:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;

    public InventoryResponse() {}

    public InventoryResponse(
            String id,
            String productId,
            String productName,
            int totalQuantity,
            int availableQuantity,
            int reservedQuantity,
            int threshold,
            String status,
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

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
