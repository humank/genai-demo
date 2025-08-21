package solid.humank.genaidemo.testutils.mocks;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Mock 行為定義介面
 * 定義 Mock 物件在被調用時的行為
 */
@FunctionalInterface
public interface MockBehavior {

    /**
     * 處理方法調用
     */
    Object handle(Method method, Object[] args) throws Exception;

    /**
     * 創建簡單的返回值行為
     */
    static MockBehavior returns(Object returnValue) {
        return (method, args) -> returnValue;
    }

    /**
     * 創建拋出異常的行為
     */
    static MockBehavior throwsException(Exception exception) {
        return (method, args) -> {
            throw exception;
        };
    }

    /**
     * 創建基於方法名的行為映射
     */
    static MockBehavior methodMap(Map<String, Object> methodReturnValues) {
        return (method, args) -> methodReturnValues.get(method.getName());
    }

    /**
     * 創建基於參數的行為
     */
    static MockBehavior withArgs(Function<Object[], Object> argHandler) {
        return (method, args) -> argHandler.apply(args);
    }

    /**
     * 創建複合行為構建器
     */
    static BehaviorBuilder builder() {
        return new BehaviorBuilder();
    }

    /**
     * 行為構建器
     */
    class BehaviorBuilder {
        private final Map<String, MockBehavior> methodBehaviors = new ConcurrentHashMap<>();
        private MockBehavior defaultBehavior = (method, args) -> null;

        /**
         * 為特定方法設定行為
         */
        public BehaviorBuilder when(String methodName, Object returnValue) {
            methodBehaviors.put(methodName, returns(returnValue));
            return this;
        }

        /**
         * 為特定方法設定異常行為
         */
        public BehaviorBuilder whenThrows(String methodName, Exception exception) {
            methodBehaviors.put(methodName, throwsException(exception));
            return this;
        }

        /**
         * 為特定方法設定自定義行為
         */
        public BehaviorBuilder when(String methodName, MockBehavior behavior) {
            methodBehaviors.put(methodName, behavior);
            return this;
        }

        /**
         * 設定預設行為
         */
        public BehaviorBuilder defaultBehavior(MockBehavior behavior) {
            this.defaultBehavior = behavior;
            return this;
        }

        /**
         * 構建最終的行為
         */
        public MockBehavior build() {
            return (method, args) -> {
                MockBehavior behavior = methodBehaviors.get(method.getName());
                if (behavior != null) {
                    return behavior.handle(method, args);
                }
                return defaultBehavior.handle(method, args);
            };
        }
    }
}