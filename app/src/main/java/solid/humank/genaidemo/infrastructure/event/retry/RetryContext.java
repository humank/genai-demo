package solid.humank.genaidemo.infrastructure.event.retry;

import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 重試上下文
 * 保存事件重試過程中的狀態信息
 */
public class RetryContext {

    private final DomainEvent event;
    private final String handlerName;
    private final RetryPolicy retryPolicy;
    private final String contextKey;
    private final LocalDateTime createdAt;

    private int attemptCount = 0;
    private LocalDateTime nextRetryTime;
    private LocalDateTime lastAttemptTime;

    public RetryContext(DomainEvent event, String handlerName, RetryPolicy retryPolicy) {
        this.event = event;
        this.handlerName = handlerName;
        this.retryPolicy = retryPolicy;
        this.contextKey = generateContextKey(event, handlerName);
        this.createdAt = LocalDateTime.now();
    }

    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttemptTime = LocalDateTime.now();
    }

    private String generateContextKey(DomainEvent event, String handlerName) {
        return String.format("%s-%s-%s", event.getEventType(), event.getAggregateId(), handlerName);
    }

    // Getters and Setters
    public DomainEvent getEvent() {
        return event;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public String getContextKey() {
        return contextKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public LocalDateTime getLastAttemptTime() {
        return lastAttemptTime;
    }
}