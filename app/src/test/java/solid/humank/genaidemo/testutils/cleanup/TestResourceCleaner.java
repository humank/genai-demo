package solid.humank.genaidemo.testutils.cleanup;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * 測試資源清理器
 * 負責在測試結束後清理所有註冊的測試資源
 */
public class TestResourceCleaner {

    private static final ThreadLocal<TestResourceCleaner> INSTANCE = new ThreadLocal<>();

    private final List<TestResource> resources = new CopyOnWriteArrayList<>();
    private final List<Runnable> cleanupTasks = new CopyOnWriteArrayList<>();
    private boolean isCleanedUp = false;

    private TestResourceCleaner() {
    }

    /**
     * 獲取當前線程的清理器實例
     */
    public static TestResourceCleaner getInstance() {
        TestResourceCleaner instance = INSTANCE.get();
        if (instance == null) {
            instance = new TestResourceCleaner();
            INSTANCE.set(instance);
        }
        return instance;
    }

    /**
     * 註冊需要清理的資源
     */
    public void registerResource(TestResource resource) {
        if (!isCleanedUp && resource != null) {
            resources.add(resource);
        }
    }

    /**
     * 註冊清理任務
     */
    public void registerCleanupTask(Runnable task) {
        if (!isCleanedUp && task != null) {
            cleanupTasks.add(task);
        }
    }

    /**
     * 執行所有清理操作
     */
    public void cleanupAll() {
        if (isCleanedUp) {
            return;
        }

        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // 並行清理資源以提高效率
        List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();

        // 清理註冊的資源
        for (TestResource resource : resources) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    if (!resource.isCleanedUp()) {
                        resource.cleanup();
                    }
                } catch (Exception e) {
                    exceptions
                            .add(new RuntimeException("Failed to cleanup resource: " + resource.getResourceName(), e));
                }
            });
            futures.add(future);
        }

        // 執行清理任務
        for (Runnable task : cleanupTasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    exceptions.add(new RuntimeException("Failed to execute cleanup task", e));
                }
            });
            futures.add(future);
        }

        // 等待所有清理操作完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS); // 30秒超時
        } catch (Exception e) {
            exceptions.add(new RuntimeException("Cleanup operations timed out", e));
        }

        // 清理完成
        resources.clear();
        cleanupTasks.clear();
        isCleanedUp = true;
        INSTANCE.remove();

        // 如果有異常，記錄但不拋出，避免影響測試結果
        if (!exceptions.isEmpty()) {
            System.err.println("Warning: " + exceptions.size() + " exceptions occurred during cleanup");
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 檢查是否已經清理
     */
    public boolean isCleanedUp() {
        return isCleanedUp;
    }

    /**
     * 獲取註冊的資源數量
     */
    public int getResourceCount() {
        return isCleanedUp ? 0 : resources.size();
    }

    /**
     * 獲取註冊的清理任務數量
     */
    public int getTaskCount() {
        return isCleanedUp ? 0 : cleanupTasks.size();
    }

    /**
     * 強制清理（即使已經清理過）
     */
    public void forceCleanup() {
        isCleanedUp = false;
        cleanupAll();
    }
}