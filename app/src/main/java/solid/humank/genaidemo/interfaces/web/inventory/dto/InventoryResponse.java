package solid.humank.genaidemo.interfaces.web.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import solid.humank.genaidemo.application.inventory.dto.InventoryDto;

/** 庫存響應對象 */
@Schema(description = "庫存資訊回應資料，包含產品的完整庫存狀態和相關資訊")
public class InventoryResponse {
    @Schema(description = "庫存記錄唯一識別碼", example = "INV-001", required = true)
    private String id;

    @Schema(description = "產品唯一識別碼", example = "PROD-001", required = true)
    private String productId;

    @Schema(description = "產品名稱", example = "iPhone 15 Pro", required = true)
    private String productName;

    @Schema(description = "總庫存數量，包含可用庫存和預留庫存", example = "100", minimum = "0", required = true)
    private int totalQuantity;

    @Schema(description = "可用庫存數量，可以立即銷售的庫存", example = "80", minimum = "0", required = true)
    private int availableQuantity;

    @Schema(description = "預留庫存數量，已被訂單預留但尚未出貨的庫存", example = "20", minimum = "0", required = true)
    private int reservedQuantity;

    @Schema(description = "庫存警戒閾值，當可用庫存低於此值時需要補貨", example = "10", minimum = "0", required = true)
    private int threshold;

    @Schema(
            description = "庫存狀態",
            allowableValues = {"ACTIVE", "INACTIVE", "DISCONTINUED"},
            example = "ACTIVE",
            required = true)
    private String status;

    @Schema(description = "是否低於警戒閾值，true表示需要補貨", example = "false", required = true)
    private boolean belowThreshold;

    @Schema(description = "庫存記錄創建時間", example = "2024-01-15 10:30:00", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "庫存記錄最後更新時間", example = "2024-01-15 14:45:00", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 默認構造函數
    public InventoryResponse() {}

    /** 從應用層DTO創建響應對象 */
    public static InventoryResponse fromDto(InventoryDto dto) {
        InventoryResponse response = new InventoryResponse();
        response.setId(dto.getId());
        response.setProductId(dto.getProductId());
        response.setProductName(dto.getProductName());
        response.setTotalQuantity(dto.getTotalQuantity());
        response.setAvailableQuantity(dto.getAvailableQuantity());
        response.setReservedQuantity(dto.getReservedQuantity());
        response.setThreshold(dto.getThreshold());
        response.setStatus(dto.getStatusName());
        response.setBelowThreshold(dto.isBelowThreshold());
        response.setCreatedAt(dto.getCreatedAt());
        response.setUpdatedAt(dto.getUpdatedAt());
        return response;
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

    public boolean isBelowThreshold() {
        return belowThreshold;
    }

    public void setBelowThreshold(boolean belowThreshold) {
        this.belowThreshold = belowThreshold;
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
