package solid.humank.genaidemo.domain.seller.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/**
 * 賣家驗證通過事件
 * 當賣家驗證通過時發布此事件
 */
public record SellerVerificationApprovedEvent(
        SellerId sellerId,
        String verifierUserId,
        LocalDateTime verifiedAt,
        LocalDateTime expiresAt,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static SellerVerificationApprovedEvent create(SellerId sellerId, String verifierUserId,
            LocalDateTime verifiedAt, LocalDateTime expiresAt) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new SellerVerificationApprovedEvent(sellerId, verifierUserId, verifiedAt, expiresAt,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return sellerId.getId();
    }
}