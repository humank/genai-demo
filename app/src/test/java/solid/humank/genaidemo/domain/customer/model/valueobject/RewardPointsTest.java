package solid.humank.genaidemo.domain.customer.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RewardPoints 值對象單元測試 測試獎勵積點的不變性和業務邏輯 */
@DisplayName("RewardPoints 值對象測試")
class RewardPointsTest {

    @Test
    @DisplayName("應該能夠創建獎勵積點")
    void shouldCreateRewardPoints() {
        // Given
        int balance = 1000;
        LocalDateTime lastUpdated = LocalDateTime.now();

        // When
        RewardPoints rewardPoints = new RewardPoints(balance, lastUpdated);

        // Then
        assertThat(rewardPoints.balance()).isEqualTo(balance);
        assertThat(rewardPoints.lastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    @DisplayName("不應該允許負數積點餘額")
    void shouldNotAllowNegativeBalance() {
        // Given
        int negativeBalance = -100;
        LocalDateTime lastUpdated = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new RewardPoints(negativeBalance, lastUpdated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("點數餘額不能為負數");
    }

    @Test
    @DisplayName("應該能夠檢查是否可以兌換")
    void shouldCheckIfCanRedeem() {
        // Given
        RewardPoints rewardPoints = new RewardPoints(1000, LocalDateTime.now());

        // When & Then
        assertThat(rewardPoints.canRedeem(500)).isTrue();
        assertThat(rewardPoints.canRedeem(1000)).isTrue();
        assertThat(rewardPoints.canRedeem(1500)).isFalse();
    }

    @Test
    @DisplayName("應該能夠兌換積點")
    void shouldRedeemPoints() {
        // Given
        RewardPoints originalPoints = new RewardPoints(1000, LocalDateTime.now().minusHours(1));
        int pointsToRedeem = 300;

        // When
        RewardPoints newPoints = originalPoints.redeem(pointsToRedeem);

        // Then
        assertThat(newPoints.balance()).isEqualTo(700);
        assertThat(newPoints.lastUpdated()).isAfter(originalPoints.lastUpdated());
    }

    @Test
    @DisplayName("當積點不足時兌換應該拋出異常")
    void shouldThrowExceptionWhenRedeemingInsufficientPoints() {
        // Given
        RewardPoints rewardPoints = new RewardPoints(100, LocalDateTime.now());
        int pointsToRedeem = 200;

        // When & Then
        assertThatThrownBy(() -> rewardPoints.redeem(pointsToRedeem))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("應該能夠增加積點")
    void shouldAddPoints() {
        // Given
        RewardPoints originalPoints = new RewardPoints(500, LocalDateTime.now().minusHours(1));
        int pointsToAdd = 300;

        // When
        RewardPoints newPoints = originalPoints.add(pointsToAdd);

        // Then
        assertThat(newPoints.balance()).isEqualTo(800);
        assertThat(newPoints.lastUpdated()).isAfter(originalPoints.lastUpdated());
    }

    @Test
    @DisplayName("應該能夠創建空的積點")
    void shouldCreateEmptyPoints() {
        // When
        RewardPoints emptyPoints = RewardPoints.empty();

        // Then
        assertThat(emptyPoints.balance()).isEqualTo(0);
        assertThat(emptyPoints.lastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("值對象應該是不可變的")
    void shouldBeImmutable() {
        // Given
        RewardPoints originalPoints = new RewardPoints(1000, LocalDateTime.now());

        // When
        RewardPoints newPoints = originalPoints.add(500);

        // Then
        assertThat(originalPoints.balance()).isEqualTo(1000);
        assertThat(newPoints.balance()).isEqualTo(1500);
        assertThat(originalPoints).isNotEqualTo(newPoints);
    }

    @Test
    @DisplayName("相同內容的值對象應該相等")
    void shouldBeEqualWithSameContent() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        RewardPoints points1 = new RewardPoints(1000, timestamp);
        RewardPoints points2 = new RewardPoints(1000, timestamp);

        // Then
        assertThat(points1).isEqualTo(points2);
        assertThat(points1.hashCode()).isEqualTo(points2.hashCode());
    }

    @Test
    @DisplayName("不同內容的值對象應該不相等")
    void shouldNotBeEqualWithDifferentContent() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        RewardPoints points1 = new RewardPoints(1000, timestamp);
        RewardPoints points2 = new RewardPoints(500, timestamp);
        RewardPoints points3 = new RewardPoints(1000, timestamp.plusHours(1));

        // Then
        assertThat(points1).isNotEqualTo(points2);
        assertThat(points1).isNotEqualTo(points3);
    }
}
