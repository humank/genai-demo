package solid.humank.genaidemo.infrastructure.inventory.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 庫存JPA實體 */
@Entity
@Table(name = "inventories")
public class JpaInventoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "threshold")
    private int threshold;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryStatusEnum status;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JpaReservationEntity> reservations = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 默認構造函數
    public JpaInventoryEntity() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public InventoryStatusEnum getStatus() {
        return status;
    }

    public void setStatus(InventoryStatusEnum status) {
        this.status = status;
    }

    public Set<JpaReservationEntity> getReservations() {
        return reservations;
    }

    public void setReservations(Set<JpaReservationEntity> reservations) {
        this.reservations = reservations;
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

    // 添加預留
    public void addReservation(JpaReservationEntity reservation) {
        reservations.add(reservation);
        reservation.setInventory(this);
    }

    // 移除預留
    public void removeReservation(JpaReservationEntity reservation) {
        reservations.remove(reservation);
        reservation.setInventory(null);
    }

    /** 庫存狀態枚舉 */
    public enum InventoryStatusEnum {
        ACTIVE,
        INACTIVE,
        DISCONTINUED
    }
}
