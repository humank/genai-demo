package solid.humank.genaidemo.interfaces.web.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import solid.humank.genaidemo.application.inventory.dto.InventoryDto;

import java.time.LocalDateTime;

/**
 * 庫存響應對象
 */
public class InventoryResponse {
    private String id;
    private String productId;
    private String productName;
    private int totalQuantity;
    private int availableQuantity;
    private int reservedQuantity;
    private int threshold;
    private String status;
    private boolean belowThreshold;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 默認構造函數
    public InventoryResponse() {
    }

    /**
     * 從應用層DTO創建響應對象
     */
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