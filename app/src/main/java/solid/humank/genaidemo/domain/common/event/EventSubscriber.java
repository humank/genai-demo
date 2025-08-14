package solid.humank.genaidemo.domain.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 事件訂閱者註解 用於標記處理特定領域事件的方法 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSubscriber {

    /**
     * 要訂閱的事件類型
     *
     * @return 事件類型的Class
     */
    Class<? extends DomainEvent> value();
}
