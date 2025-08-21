package solid.humank.genaidemo.infrastructure.delivery.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 配送JPA實體
 */
@Entity
@Table(name = "deliveries")
public class JpaDeliveryEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "delivery_person_name", length = 100)
    private String deliveryPersonName;

    @Column(name = "delivery_person_contact", length = 50)
    private String deliveryPersonContact;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refusal_reason", length = 500)
    private String refusalReason;

    @Column(name = "delay_reason", length = 500)
    private String delayReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public JpaDeliveryEntity() {
    }

    // Constructor
    public JpaDeliveryEntity(UUID id, String orderId, String shippingAddress, String status,
            String deliveryPersonName, String deliveryPersonContact,
            LocalDateTime estimatedDeliveryTime, LocalDateTime actualDeliveryTime,
            String failureReason, String refusalReason, String delayReason,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.shippingAddress = shippingAddress;
        this.status = status;
        this.deliveryPersonName = deliveryPersonName;
        this.deliveryPersonContact = deliveryPersonContact;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.actualDeliveryTime = actualDeliveryTime;
        this.failureReason = failureReason;
        this.refusalReason = refusalReason;
        this.delayReason = delayReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryPersonName() {
        return deliveryPersonName;
    }

    public void setDeliveryPersonName(String deliveryPersonName) {
        this.deliveryPersonName = deliveryPersonName;
    }

    public String getDeliveryPersonContact() {
        return deliveryPersonContact;
    }

    public void setDeliveryPersonContact(String deliveryPersonContact) {
        this.deliveryPersonContact = deliveryPersonContact;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public LocalDateTime getActualDeliveryTime() {
        return actualDeliveryTime;
    }

    public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) {
        this.actualDeliveryTime = actualDeliveryTime;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
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