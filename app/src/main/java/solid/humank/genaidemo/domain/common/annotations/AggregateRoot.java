package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 聚合根註解 用於標記聚合根類 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {

    /**
     * 聚合根名稱
     *
     * @return 聚合根名稱
     */
    String name() default "";

    /**
     * 聚合根描述
     *
     * @return 聚合根描述
     */
    String description() default "";
}
