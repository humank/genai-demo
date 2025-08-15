package solid.humank.genaidemo.domain.customer.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** NotificationPreferences 值對象單元測試 測試通知偏好設定的不變性和業務邏輯 */
@DisplayName("NotificationPreferences 值對象測試")
class NotificationPreferencesTest {

    @Test
    @DisplayName("應該能夠創建通知偏好設定")
    void shouldCreateNotificationPreferences() {
        // Given
        Set<NotificationType> enabledTypes =
                Set.of(NotificationType.ORDER_STATUS, NotificationType.PROMOTION);
        Set<NotificationChannel> enabledChannels =
                Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP);
        boolean marketingEnabled = true;

        // When
        NotificationPreferences preferences =
                new NotificationPreferences(enabledTypes, enabledChannels, marketingEnabled);

        // Then
        assertThat(preferences.enabledTypes())
                .containsExactlyInAnyOrder(
                        NotificationType.ORDER_STATUS, NotificationType.PROMOTION);
        assertThat(preferences.enabledChannels())
                .containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.IN_APP);
        assertThat(preferences.marketingEnabled()).isTrue();
    }

    @Test
    @DisplayName("應該能夠檢查通知類型是否啟用")
    void shouldCheckIfNotificationTypeIsEnabled() {
        // Given
        Set<NotificationType> enabledTypes =
                Set.of(NotificationType.ORDER_STATUS, NotificationType.DELIVERY_STATUS);
        NotificationPreferences preferences =
                new NotificationPreferences(enabledTypes, Set.of(NotificationChannel.EMAIL), false);

        // When & Then
        assertThat(preferences.isEnabled(NotificationType.ORDER_STATUS)).isTrue();
        assertThat(preferences.isEnabled(NotificationType.DELIVERY_STATUS)).isTrue();
        assertThat(preferences.isEnabled(NotificationType.PROMOTION)).isFalse();
        assertThat(preferences.isEnabled(NotificationType.MARKETING)).isFalse();
    }

    @Test
    @DisplayName("應該能夠檢查通知渠道是否啟用")
    void shouldCheckIfNotificationChannelIsEnabled() {
        // Given
        Set<NotificationChannel> enabledChannels =
                Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
        NotificationPreferences preferences =
                new NotificationPreferences(
                        Set.of(NotificationType.ORDER_STATUS), enabledChannels, false);

        // When & Then
        assertThat(preferences.shouldSendVia(NotificationChannel.EMAIL)).isTrue();
        assertThat(preferences.shouldSendVia(NotificationChannel.SMS)).isTrue();
        assertThat(preferences.shouldSendVia(NotificationChannel.PUSH)).isFalse();
        assertThat(preferences.shouldSendVia(NotificationChannel.IN_APP)).isFalse();
    }

    @Test
    @DisplayName("應該能夠創建預設偏好設定")
    void shouldCreateDefaultPreferences() {
        // When
        NotificationPreferences defaultPreferences = NotificationPreferences.defaultPreferences();

        // Then
        assertThat(defaultPreferences.enabledTypes())
                .containsExactlyInAnyOrder(
                        NotificationType.ORDER_STATUS, NotificationType.DELIVERY_STATUS);
        assertThat(defaultPreferences.enabledChannels()).containsExactly(NotificationChannel.EMAIL);
        assertThat(defaultPreferences.marketingEnabled()).isFalse();
    }

    @Test
    @DisplayName("集合應該是不可變的")
    void shouldHaveImmutableCollections() {
        // Given
        Set<NotificationType> enabledTypes = Set.of(NotificationType.ORDER_STATUS);
        Set<NotificationChannel> enabledChannels = Set.of(NotificationChannel.EMAIL);
        NotificationPreferences preferences =
                new NotificationPreferences(enabledTypes, enabledChannels, false);

        // When & Then
        assertThatThrownBy(() -> preferences.enabledTypes().add(NotificationType.PROMOTION))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> preferences.enabledChannels().add(NotificationChannel.SMS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("相同內容的值對象應該相等")
    void shouldBeEqualWithSameContent() {
        // Given
        Set<NotificationType> types =
                Set.of(NotificationType.ORDER_STATUS, NotificationType.PROMOTION);
        Set<NotificationChannel> channels = Set.of(NotificationChannel.EMAIL);

        NotificationPreferences preferences1 = new NotificationPreferences(types, channels, true);
        NotificationPreferences preferences2 = new NotificationPreferences(types, channels, true);

        // Then
        assertThat(preferences1).isEqualTo(preferences2);
        assertThat(preferences1.hashCode()).isEqualTo(preferences2.hashCode());
    }

    @Test
    @DisplayName("不同內容的值對象應該不相等")
    void shouldNotBeEqualWithDifferentContent() {
        // Given
        Set<NotificationType> types1 = Set.of(NotificationType.ORDER_STATUS);
        Set<NotificationType> types2 = Set.of(NotificationType.PROMOTION);
        Set<NotificationChannel> channels = Set.of(NotificationChannel.EMAIL);

        NotificationPreferences preferences1 = new NotificationPreferences(types1, channels, true);
        NotificationPreferences preferences2 = new NotificationPreferences(types2, channels, true);
        NotificationPreferences preferences3 = new NotificationPreferences(types1, channels, false);

        // Then
        assertThat(preferences1).isNotEqualTo(preferences2);
        assertThat(preferences1).isNotEqualTo(preferences3);
    }

    @Test
    @DisplayName("應該能夠處理空集合")
    void shouldHandleEmptyCollections() {
        // Given
        Set<NotificationType> emptyTypes = Set.of();
        Set<NotificationChannel> emptyChannels = Set.of();

        // When
        NotificationPreferences preferences =
                new NotificationPreferences(emptyTypes, emptyChannels, false);

        // Then
        assertThat(preferences.enabledTypes()).isEmpty();
        assertThat(preferences.enabledChannels()).isEmpty();
        assertThat(preferences.isEnabled(NotificationType.ORDER_STATUS)).isFalse();
        assertThat(preferences.shouldSendVia(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("應該能夠處理null集合")
    void shouldHandleNullCollections() {
        // When & Then
        assertThatThrownBy(
                        () ->
                                new NotificationPreferences(
                                        null, Set.of(NotificationChannel.EMAIL), false))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(
                        () ->
                                new NotificationPreferences(
                                        Set.of(NotificationType.ORDER_STATUS), null, false))
                .isInstanceOf(NullPointerException.class);
    }
}
