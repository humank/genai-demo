package solid.humank.genaidemo.domain.common.aggregate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

/**
 * 聚合根重建支援
 * 
 * 提供聚合根從持久化狀態重建時的支援機制
 * 
 * 設計理念：
 * 1. 狀態重建：支援從持久化狀態重建聚合根
 * 2. 事件抑制：重建時不產生領域事件
 * 3. 驗證機制：重建後驗證聚合根狀態的一致性
 * 4. 純領域：不依賴任何基礎設施框架
 */
public final class AggregateReconstruction {

    private AggregateReconstruction() {
        // 工具類，不允許實例化
    }

    /**
     * 標記建構子為重建用途
     * 
     * 用於標記專門用於重建聚合根的建構子，這些建構子不應該產生領域事件
     */
    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ReconstructionConstructor {
        /**
         * 重建說明
         */
        String value() default "用於從持久化狀態重建聚合根";
    }

    /**
     * 標記方法為重建後驗證
     * 
     * 用於標記在聚合根重建後需要執行的驗證方法
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PostReconstruction {
        /**
         * 驗證說明
         */
        String value() default "重建後驗證聚合根狀態";
    }

    /**
     * 重建上下文
     * 
     * 用於在重建過程中傳遞上下文資訊
     */
    public static class ReconstructionContext {
        private final boolean suppressEvents;
        private final boolean validateAfterReconstruction;

        private ReconstructionContext(boolean suppressEvents, boolean validateAfterReconstruction) {
            this.suppressEvents = suppressEvents;
            this.validateAfterReconstruction = validateAfterReconstruction;
        }

        public static ReconstructionContext create() {
            return new ReconstructionContext(true, true);
        }

        public static ReconstructionContext withEventSuppression(boolean suppressEvents) {
            return new ReconstructionContext(suppressEvents, true);
        }

        public static ReconstructionContext withValidation(boolean validateAfterReconstruction) {
            return new ReconstructionContext(true, validateAfterReconstruction);
        }

        public boolean shouldSuppressEvents() {
            return suppressEvents;
        }

        public boolean shouldValidateAfterReconstruction() {
            return validateAfterReconstruction;
        }
    }

    /**
     * 重建聚合根的輔助方法
     * 
     * @param aggregateRoot       要重建的聚合根
     * @param reconstructionLogic 重建邏輯
     * @param context             重建上下文
     * @param <T>                 聚合根類型
     */
    public static <T extends AggregateRootInterface> void reconstruct(
            T aggregateRoot,
            Consumer<T> reconstructionLogic,
            ReconstructionContext context) {

        // 如果需要抑制事件，清除現有事件
        if (context.shouldSuppressEvents()) {
            aggregateRoot.clearEvents();
        }

        // 執行重建邏輯
        reconstructionLogic.accept(aggregateRoot);

        // 如果需要抑制事件，再次清除重建過程中產生的事件
        if (context.shouldSuppressEvents()) {
            aggregateRoot.clearEvents();
        }

        // 如果需要驗證，執行驗證邏輯
        if (context.shouldValidateAfterReconstruction()) {
            validateReconstructedAggregate(aggregateRoot);
        }
    }

    /**
     * 重建聚合根（使用預設上下文）
     * 
     * @param aggregateRoot       要重建的聚合根
     * @param reconstructionLogic 重建邏輯
     * @param <T>                 聚合根類型
     */
    public static <T extends AggregateRootInterface> void reconstruct(
            T aggregateRoot,
            Consumer<T> reconstructionLogic) {
        reconstruct(aggregateRoot, reconstructionLogic, ReconstructionContext.create());
    }

    /**
     * 驗證重建後的聚合根
     * 
     * @param aggregateRoot 聚合根
     * @param <T>           聚合根類型
     */
    private static <T extends AggregateRootInterface> void validateReconstructedAggregate(T aggregateRoot) {
        // 檢查聚合根是否有必要的註解
        if (!aggregateRoot.getClass().isAnnotationPresent(
                solid.humank.genaidemo.domain.common.annotations.AggregateRoot.class)) {
            throw new IllegalStateException(
                    "重建的聚合根缺少 @AggregateRoot 註解: " + aggregateRoot.getClass().getName());
        }

        // 檢查聚合根是否有未提交的事件（重建時不應該有）
        if (aggregateRoot.hasUncommittedEvents()) {
            throw new IllegalStateException(
                    "重建的聚合根不應該有未提交的事件: " + aggregateRoot.getClass().getName());
        }

        // 可以在這裡添加更多的驗證邏輯
    }

    /**
     * 檢查建構子是否為重建用途
     * 
     * @param constructor 建構子
     * @return 是否為重建用途
     */
    public static boolean isReconstructionConstructor(java.lang.reflect.Constructor<?> constructor) {
        return constructor.isAnnotationPresent(ReconstructionConstructor.class);
    }

    /**
     * 檢查方法是否為重建後驗證
     * 
     * @param method 方法
     * @return 是否為重建後驗證
     */
    public static boolean isPostReconstructionMethod(java.lang.reflect.Method method) {
        return method.isAnnotationPresent(PostReconstruction.class);
    }
}