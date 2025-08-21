package solid.humank.genaidemo.testutils.database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import solid.humank.genaidemo.testutils.isolation.TestIsolationManager;
import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * 純領域測試資料庫管理器
 * 不依賴 Spring Boot 或任何基礎設施框架
 * 使用記憶體儲存模擬資料庫操作，支援測試隔離
 */
public class TestDatabaseManager implements TestResource {

    private static final ThreadLocal<TestDatabaseManager> INSTANCE = new ThreadLocal<>();
    private final Map<String, Map<String, Object>> tables;
    private final ThreadLocal<Map<String, Map<String, Object>>> transactionData;
    private boolean isCleanedUp = false;

    private TestDatabaseManager() {
        this.tables = new ConcurrentHashMap<>();
        this.transactionData = new ThreadLocal<>();
    }

    public static TestDatabaseManager getInstance() {
        TestDatabaseManager instance = INSTANCE.get();
        if (instance == null || instance.isCleanedUp()) {
            instance = new TestDatabaseManager();
            INSTANCE.set(instance);

            // 註冊到測試隔離管理器
            if (TestIsolationManager.hasActiveContext()) {
                TestIsolationManager.registerResource("databaseManager", instance);
            }
        }
        return instance;
    }

    /**
     * 初始化測試資料庫
     * 每個測試場景開始前調用
     */
    public void initializeDatabase() {
        if (isCleanedUp) {
            throw new IllegalStateException("Database manager has been cleaned up");
        }

        // 清理所有表格
        tables.clear();

        // 初始化基本表格結構
        initializeTable("customers");
        initializeTable("products");
        initializeTable("orders");
        initializeTable("payments");
        initializeTable("inventory");
        initializeTable("shopping_carts");
        initializeTable("promotions");
        initializeTable("vouchers");
        initializeTable("notifications");
        initializeTable("deliveries");
        initializeTable("reviews");
        initializeTable("sellers");
        initializeTable("pricing_rules");
    }

    /**
     * 清理測試資料庫
     * 每個測試場景結束後調用
     */
    public void cleanupDatabase() {
        if (!isCleanedUp) {
            tables.clear();
            transactionData.remove();
        }
    }

    /**
     * 開始事務
     */
    public void beginTransaction() {
        if (isCleanedUp) {
            throw new IllegalStateException("Database manager has been cleaned up");
        }

        Map<String, Map<String, Object>> snapshot = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : tables.entrySet()) {
            snapshot.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        transactionData.set(snapshot);
    }

    /**
     * 提交事務
     */
    public void commitTransaction() {
        if (!isCleanedUp) {
            transactionData.remove();
        }
    }

    /**
     * 回滾事務
     */
    public void rollbackTransaction() {
        if (isCleanedUp) {
            return;
        }

        Map<String, Map<String, Object>> snapshot = transactionData.get();
        if (snapshot != null) {
            tables.clear();
            tables.putAll(snapshot);
            transactionData.remove();
        }
    }

    /**
     * 儲存實體到指定表格
     */
    public void save(String tableName, String id, Object entity) {
        if (isCleanedUp) {
            throw new IllegalStateException("Database manager has been cleaned up");
        }

        Map<String, Object> table = tables.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>());
        table.put(id, entity);
    }

    /**
     * 從指定表格查找實體
     */
    public <T> T findById(String tableName, String id, Class<T> entityClass) {
        if (isCleanedUp) {
            return null;
        }

        Map<String, Object> table = tables.get(tableName);
        if (table == null) {
            return null;
        }
        Object entity = table.get(id);
        if (entity != null && entityClass.isInstance(entity)) {
            return entityClass.cast(entity);
        }
        return null;
    }

    /**
     * 從指定表格刪除實體
     */
    public void deleteById(String tableName, String id) {
        if (isCleanedUp) {
            return;
        }

        Map<String, Object> table = tables.get(tableName);
        if (table != null) {
            table.remove(id);
        }
    }

    /**
     * 檢查實體是否存在
     */
    public boolean exists(String tableName, String id) {
        if (isCleanedUp) {
            return false;
        }

        Map<String, Object> table = tables.get(tableName);
        return table != null && table.containsKey(id);
    }

    /**
     * 取得表格中所有實體
     */
    public Map<String, Object> findAll(String tableName) {
        if (isCleanedUp) {
            return new HashMap<>();
        }

        return new HashMap<>(tables.getOrDefault(tableName, new HashMap<>()));
    }

    /**
     * 取得表格記錄數量
     */
    public int count(String tableName) {
        if (isCleanedUp) {
            return 0;
        }

        Map<String, Object> table = tables.get(tableName);
        return table != null ? table.size() : 0;
    }

    /**
     * 檢查資料庫是否為空（用於驗證清理效果）
     */
    public boolean isEmpty() {
        if (isCleanedUp) {
            return true;
        }

        return tables.values().stream().allMatch(Map::isEmpty);
    }

    /**
     * 獲取所有表名
     */
    public java.util.Set<String> getTableNames() {
        if (isCleanedUp) {
            return java.util.Set.of();
        }

        return new java.util.HashSet<>(tables.keySet());
    }

    private void initializeTable(String tableName) {
        if (!isCleanedUp) {
            tables.put(tableName, new ConcurrentHashMap<>());
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            cleanupDatabase();
            isCleanedUp = true;
            // Don't remove ThreadLocal immediately to allow for proper cleanup verification
            // INSTANCE.remove(); // This will be removed when a new instance is created
        }
    }

    @Override
    public String getResourceName() {
        return "TestDatabaseManager";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }
}