package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工廠註解
 * 用於標記負責創建複雜聚合根或值對象的工廠類
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Factory {
    /**
     * 工廠名稱
     * 
     * @return 工廠名稱
     */
    String name() default "";
    
    /**
     * 工廠描述
     * 
     * @return 工廠描述
     */
    String description() default "";
}