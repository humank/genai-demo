package solid.humank.genaidemo.testutils.isolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 測試隔離管理器
 * 確保每個測試都有獨立的執行環境，避免測試間的相互影響
 */
public class TestIsolationManager {

    private static final ThreadLocal<TestExecutionContext> CONTEXT = new ThreadLocal<>();
    private static final Map<String, TestResourceRegistry> RESOURCE_REGISTRIES = new ConcurrentHashMap<>();
    private static final AtomicLong TEST_SEQUENCE = new AtomicLong(0);

    /**
     * 初始化測試執行上下文
     */
    public static void initializeTestContext(String testName) {
        String testId = generateTestId(testName);
        TestExecutionContext context = new TestExecutionContext(testId, testName);
        CONTEXT.set(context);

        // 為每個測試創建獨立的資源註冊表
        RESOURCE_REGISTRIES.put(testId, new TestResourceRegistry());
    }

    /**
     * 獲取當前測試上下文
     */
    public static TestExecutionContext getCurrentContext() {
        TestExecutionContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("Test context not initialized. Call initializeTestContext() first.");
        }
        return context;
    }

    /**
     * 清理測試上下文和資源
     */
    public static void cleanupTestContext() {
        TestExecutionContext context = CONTEXT.get();
        if (context != null) {
            // 清理測試資源
            TestResourceRegistry registry = RESOURCE_REGISTRIES.get(context.getTestId());
            if (registry != null) {
                registry.cleanup();
                RESOURCE_REGISTRIES.remove(context.getTestId());
            }

            // 清理上下文
            CONTEXT.remove();
        }
    }

    /**
     * 註冊測試資源
     */
    public static void registerResource(String resourceName, TestResource resource) {
        TestExecutionContext context = getCurrentContext();
        TestResourceRegistry registry = RESOURCE_REGISTRIES.get(context.getTestId());
        if (registry != null) {
            registry.register(resourceName, resource);
        }
    }

    /**
     * 獲取測試資源
     */
    public static <T extends TestResource> T getResource(String resourceName, Class<T> resourceType) {
        TestExecutionContext context = getCurrentContext();
        TestResourceRegistry registry = RESOURCE_REGISTRIES.get(context.getTestId());
        if (registry != null) {
            return registry.get(resourceName, resourceType);
        }
        return null;
    }

    /**
     * 檢查是否有活躍的測試上下文
     */
    public static boolean hasActiveContext() {
        return CONTEXT.get() != null;
    }

    /**
     * 生成唯一的測試ID
     */
    private static String generateTestId(String testName) {
        long sequence = TEST_SEQUENCE.incrementAndGet();
        long threadId = Thread.currentThread().threadId(); // Use threadId() instead of deprecated getId()
        return String.format("%s-%d-%d-%d",
                testName.replaceAll("[^a-zA-Z0-9]", "-"),
                System.currentTimeMillis(),
                threadId,
                sequence);
    }

    /**
     * 測試執行上下文
     */
    public static class TestExecutionContext {
        private final String testId;
        private final String testName;
        private final long startTime;
        private final Map<String, Object> attributes;

        public TestExecutionContext(String testId, String testName) {
            this.testId = testId;
            this.testName = testName;
            this.startTime = System.currentTimeMillis();
            this.attributes = new ConcurrentHashMap<>();
        }

        public String getTestId() {
            return testId;
        }

        public String getTestName() {
            return testName;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String key, Class<T> type) {
            Object value = attributes.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }

        public boolean hasAttribute(String key) {
            return attributes.containsKey(key);
        }
    }

    /**
     * 測試資源註冊表
     */
    private static class TestResourceRegistry {
        private final Map<String, TestResource> resources = new ConcurrentHashMap<>();

        public void register(String name, TestResource resource) {
            resources.put(name, resource);
        }

        @SuppressWarnings("unchecked")
        public <T extends TestResource> T get(String name, Class<T> type) {
            TestResource resource = resources.get(name);
            if (resource != null && type.isInstance(resource)) {
                return (T) resource;
            }
            return null;
        }

        public void cleanup() {
            List<Exception> exceptions = new ArrayList<>();

            for (TestResource resource : resources.values()) {
                try {
                    resource.cleanup();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }

            resources.clear();

            // 如果有清理異常，記錄但不拋出，避免影響測試結果
            if (!exceptions.isEmpty()) {
                System.err
                        .printf("Warning: %d exceptions occurred during test resource cleanup%n", exceptions.size());
                for (Exception e : exceptions) {
                    e.printStackTrace();
                }
            }
        }
    }
}