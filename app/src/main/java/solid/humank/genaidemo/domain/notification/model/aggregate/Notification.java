package solid.humank.genaidemo.domain.notification.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware;
import solid.humank.genaidemo.domain.notification.model.events.NotificationCreatedEvent;
import solid.humank.genaidemo.domain.notification.model.events.NotificationStatusChangedEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 通知聚合根 管理系統通知的發送和狀態 */
@AggregateRoot
@AggregateLifecycle.ManagedLifecycle
public class Notification {
    private final NotificationId id;
    private final String customerId;
    private final NotificationType type;
    private final String subject;
    private final String content;
    private final List<NotificationChannel> channels;
    private NotificationStatus status;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private LocalDateTime deliveredTime;
    private LocalDateTime readTime;
    private String failureReason;
    private int retryCount;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建立通知
     *
     * @param customerId 客戶ID
     * @param type 通知類型
     * @param subject 主題
     * @param content 內容
     * @param channels 通知渠道
     */
    public Notification(
            String customerId,
            NotificationType type,
            String subject,
            String content,
            List<NotificationChannel> channels) {
        this(NotificationId.generate(), customerId, type, subject, content, channels);
    }

    /**
     * 建立通知
     *
     * @param id 通知ID
     * @param customerId 客戶ID
     * @param type 通知類型
     * @param subject 主題
     * @param content 內容
     * @param channels 通知渠道
     */
    public Notification(
            NotificationId id,
            String customerId,
            NotificationType type,
            String subject,
            String content,
            List<NotificationChannel> channels) {
        this.id = Objects.requireNonNull(id, "通知ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.type = Objects.requireNonNull(type, "通知類型不能為空");
        this.subject = Objects.requireNonNull(subject, "主題不能為空");
        this.content = Objects.requireNonNull(content, "內容不能為空");

        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("通知渠道不能為空");
        }
        this.channels = new ArrayList<>(channels);

        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        // 發布通知創建事件
        AggregateLifecycleAware.apply(
                new NotificationCreatedEvent(
                        this.id, this.customerId, this.type, this.subject, this.channels));
    }

    /** 發送通知 */
    public void send() {
        if (status != NotificationStatus.PENDING && status != NotificationStatus.FAILED) {
            throw new IllegalStateException("只有待發送或發送失敗的通知可以發送");
        }

        NotificationStatus oldStatus = this.status;
        this.status = NotificationStatus.SENT;
        this.sentTime = LocalDateTime.now();
        this.updatedAt = this.sentTime;

        // 發布通知狀態變更事件
        AggregateLifecycleAware.apply(
                new NotificationStatusChangedEvent(
                        this.id, this.customerId, oldStatus, this.status, "通知已發送"));
    }

    /** 標記為已送達 */
    public void markAsDelivered() {
        if (status != NotificationStatus.SENT) {
            throw new IllegalStateException("只有已發送的通知可以標記為已送達");
        }

        NotificationStatus oldStatus = this.status;
        this.status = NotificationStatus.DELIVERED;
        this.deliveredTime = LocalDateTime.now();
        this.updatedAt = this.deliveredTime;

        // 發布通知狀態變更事件
        AggregateLifecycleAware.apply(
                new NotificationStatusChangedEvent(
                        this.id, this.customerId, oldStatus, this.status, "通知已送達"));
    }

    /** 標記為已讀 */
    public void markAsRead() {
        if (status != NotificationStatus.DELIVERED) {
            throw new IllegalStateException("只有已送達的通知可以標記為已讀");
        }

        NotificationStatus oldStatus = this.status;
        this.status = NotificationStatus.READ;
        this.readTime = LocalDateTime.now();
        this.updatedAt = this.readTime;

        // 發布通知狀態變更事件
        AggregateLifecycleAware.apply(
                new NotificationStatusChangedEvent(
                        this.id, this.customerId, oldStatus, this.status, "通知已讀"));
    }

    /**
     * 標記為發送失敗
     *
     * @param reason 失敗原因
     */
    public void markAsFailed(String reason) {
        if (status != NotificationStatus.PENDING && status != NotificationStatus.SENT) {
            throw new IllegalStateException("只有待發送或已發送的通知可以標記為失敗");
        }

        NotificationStatus oldStatus = this.status;
        this.status = NotificationStatus.FAILED;
        this.failureReason = Objects.requireNonNull(reason, "失敗原因不能為空");
        this.updatedAt = LocalDateTime.now();

        // 發布通知狀態變更事件
        AggregateLifecycleAware.apply(
                new NotificationStatusChangedEvent(
                        this.id, this.customerId, oldStatus, this.status, reason));
    }

    /** 重試發送 */
    public void retry() {
        if (status != NotificationStatus.FAILED) {
            throw new IllegalStateException("只有發送失敗的通知可以重試");
        }

        this.status = NotificationStatus.PENDING;
        this.retryCount++;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 排程發送
     *
     * @param scheduledTime 排程時間
     */
    public void schedule(LocalDateTime scheduledTime) {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException("只有待發送的通知可以排程");
        }

        this.scheduledTime = Objects.requireNonNull(scheduledTime, "排程時間不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public NotificationId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public List<NotificationChannel> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public LocalDateTime getDeliveredTime() {
        return deliveredTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
