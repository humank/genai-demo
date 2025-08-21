package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.customer.exception.InsufficientPointsException;

/** 紅利點數值對象 封裝點數餘額和相關業務邏輯 */
@ValueObject(name = "RewardPoints", description = "紅利點數值對象，封裝點數餘額和相關業務邏輯")
public record RewardPoints(int balance, LocalDateTime lastUpdated) {
    public RewardPoints {
        if (balance < 0) {
            throw new IllegalArgumentException("點數餘額不能為負數");
        }
    }

    public boolean canRedeem(int points) {
        return balance >= points;
    }

    public RewardPoints redeem(int points) {
        if (!canRedeem(points)) {
            throw new InsufficientPointsException("紅利點數不足，目前餘額: " + balance + "，需要: " + points);
        }
        return new RewardPoints(balance - points, LocalDateTime.now());
    }

    public RewardPoints add(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("新增點數不能為負數");
        }
        return new RewardPoints(balance + points, LocalDateTime.now());
    }

    public static RewardPoints empty() {
        return new RewardPoints(0, LocalDateTime.now());
    }

    public static RewardPoints of(int initialBalance) {
        return new RewardPoints(initialBalance, LocalDateTime.now());
    }
}
