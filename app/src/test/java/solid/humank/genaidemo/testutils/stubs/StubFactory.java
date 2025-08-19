package solid.humank.genaidemo.testutils.stubs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import solid.humank.genaidemo.testutils.isolation.TestIsolationManager;
import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * Stub 物件工廠
 * 提供預設實作的測試替身物件
 */
public class StubFactory implements TestResource {

    private static final ThreadLocal<StubFactory> INSTANCE = new ThreadLocal<>();

    private final Map<Class<?>, Object> stubs;
    private boolean isCleanedUp = false;

    private StubFactory() {
        this.stubs = new ConcurrentHashMap<>();
    }

    /**
     * 獲取當前線程的 Stub 工廠實例
     */
    public static StubFactory getInstance() {
        StubFactory instance = INSTANCE.get();
        if (instance == null || instance.isCleanedUp()) {
            instance = new StubFactory();
            INSTANCE.set(instance);

            // 註冊到測試隔離管理器
            if (TestIsolationManager.hasActiveContext()) {
                TestIsolationManager.registerResource("stubFactory", instance);
            }
        }
        return instance;
    }

    /**
     * 創建或獲取 Stub 物件
     */
    public <T> T createStub(Class<T> stubClass) {
        if (isCleanedUp) {
            throw new IllegalStateException("StubFactory has been cleaned up");
        }

        Object stub = stubs.computeIfAbsent(stubClass, this::createStubInstance);
        return stubClass.cast(stub);
    }

    /**
     * 註冊自定義 Stub 實例
     */
    public <T> void registerStub(Class<T> stubClass, T stubInstance) {
        if (isCleanedUp) {
            throw new IllegalStateException("StubFactory has been cleaned up");
        }

        stubs.put(stubClass, stubInstance);
    }

    /**
     * 獲取已存在的 Stub
     */
    public <T> T getStub(Class<T> stubClass) {
        Object stub = stubs.get(stubClass);
        return stub != null ? stubClass.cast(stub) : null;
    }

    /**
     * 檢查是否存在指定類型的 Stub
     */
    public boolean hasStub(Class<?> stubClass) {
        return stubs.containsKey(stubClass);
    }

    /**
     * 移除 Stub
     */
    public void removeStub(Class<?> stubClass) {
        if (!isCleanedUp) {
            stubs.remove(stubClass);
        }
    }

    /**
     * 創建 Stub 實例
     */
    private Object createStubInstance(Class<?> stubClass) {
        // 根據不同的類型創建對應的 Stub
        String className = stubClass.getSimpleName();

        switch (className) {
            case "NotificationServiceStub":
                return new NotificationServiceStub();
            case "PaymentServiceStub":
                return new PaymentServiceStub();
            case "EmailServiceStub":
                return new EmailServiceStub();
            default:
                // 對於其他類型，嘗試創建實例
                try {
                    return stubClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot create stub instance for: " + className, e);
                }
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            // 清理所有實現了 TestResource 的 Stub
            for (Object stub : stubs.values()) {
                if (stub instanceof TestResource) {
                    ((TestResource) stub).cleanup();
                }
            }

            stubs.clear();
            isCleanedUp = true;
            // Don't remove ThreadLocal immediately
        }
    }

    @Override
    public String getResourceName() {
        return "StubFactory";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }
}