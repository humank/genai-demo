package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 值對象註解
 * 用於標記值對象類
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueObject {
    
    /**
     * 值對象名稱
     * 
     * @return 值對象名稱
     */
    String name() default "";
    
    /**
     * 值對象描述
     * 
     * @return 值對象描述
     */
    String description() default "";
}