package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 策略/政策註解
 * 用於標記封裝特定業務規則或決策邏輯的策略類
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Policy {
    /**
     * 策略名稱
     * 
     * @return 策略名稱
     */
    String name() default "";
    
    /**
     * 策略描述
     * 
     * @return 策略描述
     */
    String description() default "";
}