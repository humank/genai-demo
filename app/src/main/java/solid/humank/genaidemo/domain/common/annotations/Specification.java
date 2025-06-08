package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 規格註解
 * 用於標記封裝業務規則的規格類
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Specification {
    /**
     * 規格名稱
     * 
     * @return 規格名稱
     */
    String name() default "";
    
    /**
     * 規格描述
     * 
     * @return 規格描述
     */
    String description() default "";
}