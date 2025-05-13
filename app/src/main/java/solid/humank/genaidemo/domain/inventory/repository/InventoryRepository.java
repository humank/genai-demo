package solid.humank.genaidemo.domain.inventory.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 庫存儲存庫接口
 * 定義在領域層，由基礎設施層實現
 * 專門處理 Inventory 聚合根
 */
public interface InventoryRepository extends Repository<Inventory, InventoryId> {
    /**
     * 根據產品ID查詢庫存聚合根
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * 刪除庫存聚合根
     */
    void delete(InventoryId inventoryId);

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