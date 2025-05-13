package solid.humank.genaidemo.domain.inventory.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 庫存聚合根
 * 管理產品庫存和預留
 */
@AggregateRoot(name = "Inventory", description = "庫存聚合根，管理產品庫存和預留")
public class Inventory {
    private final InventoryId id;
    private final String productId;
    private final String productName;
    private int totalQuantity;
    private int availableQuantity;
    private int reservedQuantity;
    private int threshold;
    private final Map<ReservationId, Integer> reservations;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private InventoryStatus status;

    /**
     * 建立庫存
     *
     * @param productId 產品ID
     * @param productName 產品名稱
     * @param quantity 初始庫存數量
     */
    public Inventory(String productId, String productName, int quantity) {
        this(InventoryId.generate(), productId, productName, quantity);
    }

    /**
     * 建立庫存
     *
     * @param id 庫存ID
     * @param productId 產品ID
     * @param productName 產品名稱
     * @param quantity 初始庫存數量
     */
    public Inventory(InventoryId id, String productId, String productName, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("庫存數量不能為負數");
        }
        
        this.id = Objects.requireNonNull(id, "庫存ID不能為空");
        this.productId = Objects.requireNonNull(productId, "產品ID不能為空");
        this.productName = Objects.requireNonNull(productName, "產品名稱不能為空");
        this.totalQuantity = quantity;
        this.availableQuantity = quantity;
        this.reservedQuantity = 0;
        this.threshold = 0;
        this.reservations = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.status = InventoryStatus.ACTIVE;
    }

    /**
     * 檢查庫存是否充足
     *
     * @param requiredQuantity 需要的數量
     * @return 庫存是否充足
     */
    public boolean isSufficient(int requiredQuantity) {
        return availableQuantity >= requiredQuantity;
    }

    /**
     * 預留庫存
     *
     * @param orderId 訂單ID
     * @param quantity 預留數量
     * @return 預留ID
     */
    public ReservationId reserve(UUID orderId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("預留數量必須大於零");
        }
        
        if (!isSufficient(quantity)) {
            throw new IllegalStateException("庫存不足，無法預留");
        }
        
        ReservationId reservationId = ReservationId.create();
        reservations.put(reservationId, quantity);
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        updatedAt = LocalDateTime.now();
        
        return reservationId;
    }

    /**
     * 釋放預留庫存
     *
     * @param reservationId 預留ID
     */
    public void releaseReservation(ReservationId reservationId) {
        Integer quantity = reservations.remove(reservationId);
        if (quantity != null) {
            availableQuantity += quantity;
            reservedQuantity -= quantity;
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 確認預留庫存（從預留轉為實際消耗）
     *
     * @param reservationId 預留ID
     */
    public void confirmReservation(ReservationId reservationId) {
        Integer quantity = reservations.remove(reservationId);
        if (quantity != null) {
            totalQuantity -= quantity;
            reservedQuantity -= quantity;
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 增加庫存
     *
     * @param quantity 增加的數量
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("增加的庫存數量必須大於零");
        }
        
        totalQuantity += quantity;
        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 設置庫存閾值
     *
     * @param threshold 閾值
     */
    public void setThreshold(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("庫存閾值不能為負數");
        }
        
        this.threshold = threshold;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 檢查是否低於閾值
     *
     * @return 是否低於閾值
     */
    public boolean isBelowThreshold() {
        return threshold > 0 && availableQuantity < threshold;
    }

    /**
     * 同步庫存（從外部系統更新）
     *
     * @param newTotalQuantity 新的總庫存數量
     */
    public void synchronize(int newTotalQuantity) {
        if (newTotalQuantity < reservedQuantity) {
            throw new IllegalArgumentException("新的庫存數量不能小於已預留數量");
        }
        
        int difference = newTotalQuantity - totalQuantity;
        totalQuantity = newTotalQuantity;
        availableQuantity += difference;
        updatedAt = LocalDateTime.now();
    }

    // Getters
    public InventoryId getId() {
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

    public Map<ReservationId, Integer> getReservations() {
        return Map.copyOf(reservations);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}