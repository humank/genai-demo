package solid.humank.genaidemo.domain.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 領域事件接口 所有領域事件都應該實現此接口 */
public interface DomainEvent extends Serializable {

    /**
     * 獲取事件發生時間
     *
     * @return 事件發生時間
     */
    LocalDateTime getOccurredOn();

    /**
     * 獲取事件類型
     *
     * @return 事件類型
     */
    String getEventType();

    /**
     * 獲取事件來源
     *
     * @return 事件來源
     */
    String getSource();
}
