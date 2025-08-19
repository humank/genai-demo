package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Repository 註解 用於標記 Repository 介面 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {

    /**
     * Repository 名稱
     *
     * @return Repository 名稱
     */
    String name() default "";

    /**
     * Repository 描述
     *
     * @return Repository 描述
     */
    String description() default "";
}