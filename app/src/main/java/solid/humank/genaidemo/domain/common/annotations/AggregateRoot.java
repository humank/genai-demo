package solid.humank.genaidemo.domain.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聚合根註解 - 混搭方案：Annotation + Interface
 * 
 * 此註解必須與 AggregateRootInterface 一起使用：
 * 1. 類別必須標記此註解
 * 2. 類別必須實作 AggregateRootInterface
 * 
 * 使用方式：
 * 
 * @AggregateRoot(name = "Customer", boundedContext = "Customer")
 *                     public class Customer implements AggregateRootInterface {
 *                     // 業務邏輯
 *                     public void updateProfile(String name) {
 *                     this.name = name;
 *                     // 直接調用 interface 提供的方法
 *                     collectEvent(new CustomerUpdatedEvent(id, name));
 *                     }
 *                     }
 */
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

    /**
     * 所屬的 Bounded Context
     *
     * @return Bounded Context 名稱
     */
    String boundedContext() default "";

    /**
     * 是否啟用事件收集
     * 預設為 true，可以在測試或特殊情況下關閉
     *
     * @return 是否啟用事件收集
     */
    boolean enableEventCollection() default true;

    /**
     * 聚合根版本
     * 用於支援聚合根演進和向後兼容
     *
     * @return 版本號
     */
    String version() default "1.0";
}
