package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 領域服務註解 用於標記領域服務類
 * 
 * 領域服務用於處理跨聚合根的複雜業務邏輯，
 * 或者不適合放在任何特定聚合根中的業務邏輯
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainService {

    /**
     * 服務名稱
     * 
     * @return 服務名稱
     */
    String name() default "";

    /**
     * 服務描述
     * 
     * @return 服務描述
     */
    String description() default "";

    /**
     * 所屬的 Bounded Context
     * 
     * @return Bounded Context 名稱
     */
    String boundedContext() default "";
}
