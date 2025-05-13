package solid.humank.genaidemo.application.inventory.port.outgoing;

import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 庫存持久化端口 - 次要輸出端口
 * 定義系統與持久化層的交互方式
 * 專門處理 Inventory 聚合根
 */
public interface InventoryPersistencePort {
    /**
     * 保存庫存聚合根
     */
    Inventory save(Inventory aggregateRoot);

    /**
     * 根據ID查詢庫存聚合根
     */
    Optional<Inventory> findById(InventoryId inventoryId);

    /**
     * 根據產品ID查詢庫存聚合根
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * 查詢所有庫存聚合根
     */
    List<Inventory> findAll();

    /**
     * 刪除庫存聚合根
     */
    void delete(InventoryId inventoryId);

    /**
     * 更新庫存聚合根
     */
    void update(Inventory aggregateRoot);

    /**
     * 檢查庫存聚合根是否存在
     */
    boolean exists(InventoryId inventoryId);

    /**
     * 根據預留ID查詢庫存聚合根
     */
    Optional<Inventory> findByReservationId(ReservationId reservationId);

    /**
     * 查詢低於閾值的庫存聚合根
     */
    List<Inventory> findBelowThreshold();

    /**
     * 根據訂單ID查詢相關的庫存聚合根預留
     */
    List<Inventory> findByOrderId(UUID orderId);
}