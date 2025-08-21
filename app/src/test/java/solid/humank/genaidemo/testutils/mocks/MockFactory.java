package solid.humank.genaidemo.testutils.mocks;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import solid.humank.genaidemo.testutils.isolation.TestIsolationManager;
import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * Mock 物件工廠
 * 提供統一的 Mock 物件創建和管理機制
 */
public class MockFactory implements TestResource {

    private static final ThreadLocal<MockFactory> INSTANCE = new ThreadLocal<>();

    private final Map<Class<?>, Object> mocks;
    private final Map<String, MockBehavior> behaviors;
    private boolean isCleanedUp = false;

    private MockFactory() {
        this.mocks = new ConcurrentHashMap<>();
        this.behaviors = new ConcurrentHashMap<>();
    }

    /**
     * 獲取當前線程的 Mock 工廠實例
     */
    public static MockFactory getInstance() {
        MockFactory instance = INSTANCE.get();
        if (instance == null || instance.isCleanedUp()) {
            instance = new MockFactory();
            INSTANCE.set(instance);

            // 註冊到測試隔離管理器
            if (TestIsolationManager.hasActiveContext()) {
                TestIsolationManager.registerResource("mockFactory", instance);
            }
        }
        return instance;
    }

    /**
     * 創建 Mock 物件
     */
    public <T> T createMock(Class<T> interfaceClass) {
        if (isCleanedUp) {
            throw new IllegalStateException("MockFactory has been cleaned up");
        }

        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Only interfaces can be mocked: " + interfaceClass.getName());
        }

        Object mock = mocks.computeIfAbsent(interfaceClass, clazz -> {
            return Proxy.newProxyInstance(
                    interfaceClass.getClassLoader(),
                    new Class<?>[] { interfaceClass },
                    new MockInvocationHandler(interfaceClass.getSimpleName()));
        });

        return interfaceClass.cast(mock);
    }

    /**
     * 創建帶有預設行為的 Mock 物件
     */
    public <T> T createMock(Class<T> interfaceClass, MockBehavior behavior) {
        T mock = createMock(interfaceClass);
        String mockKey = interfaceClass.getSimpleName();
        behaviors.put(mockKey, behavior);
        return mock;
    }

    /**
     * 為已存在的 Mock 設定行為
     */
    public <T> MockFactory when(Class<T> interfaceClass, MockBehavior behavior) {
        if (isCleanedUp) {
            throw new IllegalStateException("MockFactory has been cleaned up");
        }

        String mockKey = interfaceClass.getSimpleName();
        behaviors.put(mockKey, behavior);
        return this;
    }

    /**
     * 獲取 Mock 物件
     */
    public <T> T getMock(Class<T> interfaceClass) {
        Object mock = mocks.get(interfaceClass);
        return interfaceClass.cast(mock);
    }

    /**
     * 檢查是否存在指定類型的 Mock
     */
    public boolean hasMock(Class<?> interfaceClass) {
        return mocks.containsKey(interfaceClass);
    }

    /**
     * 重置所有 Mock 的行為
     */
    public void resetAllMocks() {
        if (!isCleanedUp) {
            behaviors.clear();
        }
    }

    /**
     * 重置特定 Mock 的行為
     */
    public void resetMock(Class<?> interfaceClass) {
        if (!isCleanedUp) {
            String mockKey = interfaceClass.getSimpleName();
            behaviors.remove(mockKey);
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            mocks.clear();
            behaviors.clear();
            isCleanedUp = true;
            // Don't remove ThreadLocal immediately
        }
    }

    @Override
    public String getResourceName() {
        return "MockFactory";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }

    /**
     * Mock 調用處理器
     */
    private class MockInvocationHandler implements InvocationHandler {
        private final String mockName;

        public MockInvocationHandler(String mockName) {
            this.mockName = mockName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 處理 Object 的基本方法
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(method, args);
            }

            // 查找預設的行為
            MockBehavior behavior = behaviors.get(mockName);
            if (behavior != null) {
                return behavior.handle(method, args);
            }

            // 預設行為：返回適當的預設值
            return getDefaultReturnValue(method.getReturnType());
        }

        private Object handleObjectMethod(Method method, Object[] args) {
            switch (method.getName()) {
                case "toString":
                    return "Mock[" + mockName + "]";
                case "hashCode":
                    return mockName.hashCode();
                case "equals":
                    return this == args[0];
                default:
                    throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
            }
        }

        private Object getDefaultReturnValue(Class<?> returnType) {
            if (returnType == void.class) {
                return null;
            } else if (returnType == boolean.class) {
                return false;
            } else if (returnType.isPrimitive()) {
                return 0;
            } else {
                return null;
            }
        }
    }
}