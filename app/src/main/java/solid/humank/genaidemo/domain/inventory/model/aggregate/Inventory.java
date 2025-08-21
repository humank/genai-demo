package solid.humank.genaidemo.domain.inventory.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.inventory.model.entity.InventoryThreshold;
import solid.humank.genaidemo.domain.inventory.model.entity.StockMovement;
import solid.humank.genaidemo.domain.inventory.model.entity.StockReservation;
import solid.humank.genaidemo.domain.inventory.model.events.InventoryCreatedEvent;
import solid.humank.genaidemo.domain.inventory.model.events.StockAddedEvent;
import solid.humank.genaidemo.domain.inventory.model.events.StockReservedEvent;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

/** 庫存聚合根 管理產品庫存和預留 */
@AggregateRoot(name = "Inventory", description = "庫存聚合根，管理產品庫存和預留", boundedContext = "Inventory", version = "1.0")
@AggregateLifecycle.ManagedLifecycle
public class Inventory extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    private final InventoryId id;
    private final String productId;
    private final String productName;
    private int totalQuantity;
    private int availableQuantity;
    private int reservedQuantity;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private InventoryStatus status;

    // Entity 集合
    private final List<StockReservation> stockReservations; // 原 Map<ReservationId, Integer>
    private final List<StockMovement> stockMovements; // 庫存異動歷史
    private final List<InventoryThreshold> thresholds; // 原 int threshold

    /**
     * 建立庫存
     *
     * @param productId   產品ID
     * @param productName 產品名稱
     * @param quantity    初始庫存數量
     */
    public Inventory(String productId, String productName, int quantity) {
        this(InventoryId.generate(), productId, productName, quantity);
    }

    /**
     * 建立庫存
     *
     * @param id          庫存ID
     * @param productId   產品ID
     * @param productName 產品名稱
     * @param quantity    初始庫存數量
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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.status = InventoryStatus.ACTIVE;
        this.stockReservations = new ArrayList<>();
        this.stockMovements = new ArrayList<>();
        this.thresholds = new ArrayList<>();

        // 記錄初始庫存異動
        recordStockMovement(StockMovement.createInbound(
                quantity, 0, StockMovement.MovementReason.INVENTORY_SYNC,
                null, "SYSTEM", "系統初始化", "初始庫存建立", null, null));

        // 收集領域事件
        collectEvent(InventoryCreatedEvent.create(
                this.id, this.productId, this.productName, this.totalQuantity));
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
     * @param orderId  訂單ID
     * @param quantity 預留數量
     * @return 預留ID
     */
    public ReservationId reserve(UUID orderId, int quantity) {
        return reserve(orderId, quantity, "SYSTEM", LocalDateTime.now().plusHours(2));
    }

    /**
     * 預留庫存（完整版本）
     *
     * @param orderId      訂單ID
     * @param quantity     預留數量
     * @param customerInfo 客戶資訊
     * @param expiresAt    過期時間
     * @return 預留ID
     */
    public ReservationId reserve(UUID orderId, int quantity, String customerInfo, LocalDateTime expiresAt) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("預留數量必須大於零");
        }

        if (!isSufficient(quantity)) {
            throw new IllegalStateException("庫存不足，無法預留");
        }

        // 創建庫存預留 Entity
        StockReservation reservation = StockReservation.createOrderReservation(
                orderId, customerInfo, quantity, expiresAt);

        stockReservations.add(reservation);

        int beforeQuantity = availableQuantity;
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        updatedAt = LocalDateTime.now();

        // 記錄庫存異動
        recordStockMovement(StockMovement.createReservation(
                quantity, beforeQuantity, StockMovement.MovementReason.ORDER_RESERVE,
                reservation.getReservationId().toString(), "SYSTEM", "系統預留",
                "訂單預留庫存"));

        // 收集領域事件
        collectEvent(StockReservedEvent.create(
                this.id,
                this.productId,
                reservation.getReservationId(),
                orderId,
                quantity,
                availableQuantity));

        return reservation.getReservationId();
    }

    /**
     * 釋放預留庫存
     *
     * @param reservationId 預留ID
     */
    public void releaseReservation(ReservationId reservationId) {
        StockReservation reservation = findActiveReservation(reservationId);
        if (reservation != null && reservation.canRelease()) {
            int quantity = reservation.getQuantity();
            int beforeQuantity = availableQuantity;

            reservation.release();
            availableQuantity += quantity;
            reservedQuantity -= quantity;
            updatedAt = LocalDateTime.now();

            // 記錄庫存異動
            recordStockMovement(StockMovement.createRelease(
                    quantity, beforeQuantity, StockMovement.MovementReason.ORDER_CANCEL,
                    reservationId.toString(), "SYSTEM", "系統釋放",
                    "預留庫存釋放"));
        }
    }

    /**
     * 確認預留庫存（從預留轉為實際消耗）
     *
     * @param reservationId 預留ID
     */
    public void confirmReservation(ReservationId reservationId) {
        StockReservation reservation = findActiveReservation(reservationId);
        if (reservation != null && reservation.canConfirm()) {
            int quantity = reservation.getQuantity();
            int beforeQuantity = totalQuantity;

            reservation.confirm();
            totalQuantity -= quantity;
            reservedQuantity -= quantity;
            updatedAt = LocalDateTime.now();

            // 記錄庫存異動
            recordStockMovement(StockMovement.createOutbound(
                    quantity, beforeQuantity, StockMovement.MovementReason.ORDER_CONFIRM,
                    reservationId.toString(), "SYSTEM", "系統確認",
                    "預留庫存確認出庫", null, null));
        }
    }

    /**
     * 增加庫存
     *
     * @param quantity 增加的數量
     */
    public void addStock(int quantity) {
        addStock(quantity, StockMovement.MovementReason.PURCHASE, null, "SYSTEM", "系統入庫", "庫存補充");
    }

    /**
     * 增加庫存（完整版本）
     *
     * @param quantity     增加的數量
     * @param reason       異動原因
     * @param referenceId  關聯ID
     * @param operatorId   操作者ID
     * @param operatorName 操作者名稱
     * @param notes        備註
     */
    public void addStock(int quantity, StockMovement.MovementReason reason, String referenceId,
            String operatorId, String operatorName, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("增加的庫存數量必須大於零");
        }

        int beforeQuantity = totalQuantity;
        totalQuantity += quantity;
        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();

        // 記錄庫存異動
        recordStockMovement(StockMovement.createInbound(
                quantity, beforeQuantity, reason, referenceId,
                operatorId, operatorName, notes, null, null));

        // 收集領域事件
        collectEvent(StockAddedEvent.create(this.id, this.productId, quantity, totalQuantity));

        // 檢查閾值
        checkThresholds();
    }

    /**
     * 設置庫存閾值（向後兼容）
     *
     * @param threshold 閾值
     */
    public void setThreshold(int threshold) {
        setLowStockThreshold(threshold, "系統設定的低庫存閾值");
    }

    /**
     * 設置低庫存閾值
     *
     * @param thresholdValue 閾值
     * @param description    描述
     */
    public void setLowStockThreshold(int thresholdValue, String description) {
        if (thresholdValue < 0) {
            throw new IllegalArgumentException("庫存閾值不能為負數");
        }

        // 停用現有的低庫存閾值
        thresholds.stream()
                .filter(t -> t.getType() == InventoryThreshold.ThresholdType.LOW_STOCK)
                .forEach(InventoryThreshold::deactivate);

        // 添加新的低庫存閾值
        if (thresholdValue > 0) {
            InventoryThreshold threshold = InventoryThreshold.createLowStockThreshold(
                    thresholdValue, description);
            thresholds.add(threshold);
        }

        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加庫存閾值
     *
     * @param threshold 閾值 Entity
     */
    public void addThreshold(InventoryThreshold threshold) {
        thresholds.add(threshold);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 移除庫存閾值
     *
     * @param thresholdId 閾值ID
     */
    public void removeThreshold(String thresholdId) {
        thresholds.removeIf(t -> t.getId().getValue().equals(thresholdId));
        updatedAt = LocalDateTime.now();
    }

    /**
     * 檢查是否低於閾值（向後兼容）
     *
     * @return 是否低於閾值
     */
    public boolean isBelowThreshold() {
        return thresholds.stream()
                .filter(InventoryThreshold::isActive)
                .anyMatch(t -> t.isTriggered(availableQuantity));
    }

    /**
     * 檢查所有閾值並觸發警告
     */
    public void checkThresholds() {
        thresholds.stream()
                .filter(InventoryThreshold::isActive)
                .filter(t -> t.isTriggered(availableQuantity))
                .forEach(threshold -> {
                    threshold.trigger();
                    // 這裡可以發布閾值觸發事件
                });
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

        int beforeQuantity = totalQuantity;
        int difference = newTotalQuantity - totalQuantity;
        totalQuantity = newTotalQuantity;
        availableQuantity += difference;
        updatedAt = LocalDateTime.now();

        // 記錄庫存異動
        if (difference != 0) {
            recordStockMovement(StockMovement.createAdjustment(
                    difference, beforeQuantity, StockMovement.MovementReason.INVENTORY_SYNC,
                    "SYSTEM", "系統同步", "外部系統庫存同步"));
        }

        // 檢查閾值
        checkThresholds();
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

    public List<StockReservation> getStockReservations() {
        return new ArrayList<>(stockReservations);
    }

    public List<StockMovement> getStockMovements() {
        return new ArrayList<>(stockMovements);
    }

    public List<InventoryThreshold> getThresholds() {
        return new ArrayList<>(thresholds);
    }

    // 向後兼容的方法
    public int getThreshold() {
        return thresholds.stream()
                .filter(t -> t.getType() == InventoryThreshold.ThresholdType.LOW_STOCK && t.isActive())
                .mapToInt(InventoryThreshold::getThresholdValue)
                .findFirst()
                .orElse(0);
    }

    public Map<ReservationId, Integer> getReservations() {
        return stockReservations.stream()
                .filter(StockReservation::isActive)
                .collect(Collectors.toMap(
                        StockReservation::getReservationId,
                        StockReservation::getQuantity));
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // 輔助方法

    /**
     * 記錄庫存異動
     *
     * @param movement 庫存異動
     */
    private void recordStockMovement(StockMovement movement) {
        stockMovements.add(movement);
    }

    /**
     * 查找有效的預留
     *
     * @param reservationId 預留ID
     * @return 預留 Entity
     */
    private StockReservation findActiveReservation(ReservationId reservationId) {
        return stockReservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .filter(StockReservation::isActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * 清理過期的預留
     */
    public void cleanupExpiredReservations() {
        List<StockReservation> expiredReservations = stockReservations.stream()
                .filter(StockReservation::isExpired)
                .filter(r -> r.getStatus() == StockReservation.ReservationStatus.ACTIVE)
                .collect(Collectors.toList());

        for (StockReservation reservation : expiredReservations) {
            reservation.markAsExpired();
            int quantity = reservation.getQuantity();
            availableQuantity += quantity;
            reservedQuantity -= quantity;

            // 記錄庫存異動
            recordStockMovement(StockMovement.createRelease(
                    quantity, availableQuantity - quantity, StockMovement.MovementReason.CART_ABANDON,
                    reservation.getReservationId().toString(), "SYSTEM", "系統清理",
                    "過期預留自動釋放"));
        }

        if (!expiredReservations.isEmpty()) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 獲取有效的預留數量
     *
     * @return 有效預留數量
     */
    public int getActiveReservationCount() {
        return stockReservations.stream()
                .filter(StockReservation::isActive)
                .mapToInt(StockReservation::getQuantity)
                .sum();
    }

    /**
     * 獲取即將過期的預留
     *
     * @return 即將過期的預留列表
     */
    public List<StockReservation> getExpiringSoonReservations() {
        return stockReservations.stream()
                .filter(StockReservation::isActive)
                .filter(StockReservation::isExpiringSoon)
                .collect(Collectors.toList());
    }

    /**
     * 獲取觸發的閾值
     *
     * @return 觸發的閾值列表
     */
    public List<InventoryThreshold> getTriggeredThresholds() {
        return thresholds.stream()
                .filter(InventoryThreshold::isActive)
                .filter(t -> t.isTriggered(availableQuantity))
                .collect(Collectors.toList());
    }

    /**
     * 檢查是否需要自動補貨
     *
     * @return 是否需要自動補貨
     */
    public boolean shouldAutoReorder() {
        return thresholds.stream()
                .filter(InventoryThreshold::isActive)
                .anyMatch(t -> t.shouldAutoReorder(availableQuantity));
    }

    /**
     * 獲取建議的補貨數量
     *
     * @return 建議補貨數量
     */
    public int getSuggestedReorderQuantity() {
        return thresholds.stream()
                .filter(InventoryThreshold::isActive)
                .filter(t -> t.shouldAutoReorder(availableQuantity))
                .mapToInt(t -> t.getSuggestedReorderQuantity(availableQuantity))
                .max()
                .orElse(0);
    }
}
