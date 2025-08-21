package solid.humank.genaidemo.infrastructure.promotion.persistence.mapper;

import java.util.Currency;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Voucher;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherType;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaVoucherEntity;

/** 優惠券映射器 */
@Component
public class VoucherMapper {

    public JpaVoucherEntity toJpaEntity(Voucher voucher) {
        JpaVoucherEntity entity = new JpaVoucherEntity();
        entity.setId(voucher.getId().value());
        entity.setType(voucher.getType().name());
        entity.setName(voucher.getName());
        entity.setDescription(voucher.getDescription());
        entity.setValueAmount(voucher.getValue().getAmount());
        entity.setValueCurrency(voucher.getValue().getCurrency().getCurrencyCode());
        entity.setOwnerId(voucher.getOwnerId().getValue()); // 使用 getValue() 方法
        entity.setRedemptionCode(voucher.getCode().code()); // 使用 code() 方法 (record accessor)
        entity.setStatus(voucher.getStatus().name());
        entity.setPurchasedAt(voucher.getPurchasedAt());
        entity.setUsedAt(voucher.getUsedAt());
        entity.setExpiresAt(voucher.getExpiresAt());
        entity.setUsageLocation(voucher.getUsageLocation());
        entity.setLostReportReason(voucher.getLostReportReason());
        entity.setLostReportedAt(voucher.getLostReportedAt());
        return entity;
    }

    public Voucher toDomainModel(JpaVoucherEntity entity) {
        VoucherId voucherId = VoucherId.of(entity.getId());
        VoucherType type = VoucherType.valueOf(entity.getType());
        Money value = new Money(entity.getValueAmount(), Currency.getInstance(entity.getValueCurrency()));
        CustomerId ownerId = CustomerId.of(entity.getOwnerId());

        // Calculate remaining days from expires_at
        long remainingDays = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                entity.getExpiresAt()).toDays();

        return new Voucher(
                voucherId,
                type,
                entity.getName(),
                entity.getDescription(),
                value,
                ownerId,
                Math.max(1, (int) remainingDays) // Ensure at least 1 day
        );
    }
}