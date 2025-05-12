package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 實體註解
 * 用於標記實體類
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    
    /**
     * 實體名稱
     * 
     * @return 實體名稱
     */
    String name() default "";
    
    /**
     * 實體描述
     * 
     * @return 實體描述
     */
    String description() default "";
}