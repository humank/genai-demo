package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記一個類為實體
 * 實體是具有唯一標識的對象，其生命週期由聚合根管理
 * 實體類應該放在domain.*.entity包中
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    /**
     * 實體的描述
     */
    String description() default "";
}
