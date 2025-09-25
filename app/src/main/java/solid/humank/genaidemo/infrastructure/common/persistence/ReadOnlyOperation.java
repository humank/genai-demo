package solid.humank.genaidemo.infrastructure.common.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只讀操作註解
 * 
 * 標記方法為只讀操作，自動路由到讀取數據源
 * 
 * 使用方式：
 * - 在 Repository 或 Service 方法上使用此註解
 * - 系統會自動將操作路由到 Aurora 只讀副本
 * - 適用於查詢、統計等只讀操作
 * 
 * 注意事項：
 * - 不要在事務方法中使用，事務中會統一使用寫入數據源
 * - 確保方法確實是只讀的，不會修改數據
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnlyOperation {

    /**
     * 操作描述
     * 
     * @return 操作描述
     */
    String value() default "";

    /**
     * 是否強制使用讀取數據源
     * 如果為 true，即使在事務中也會嘗試使用讀取數據源
     * 
     * @return 是否強制使用讀取數據源
     */
    boolean forceReadDataSource() default false;
}