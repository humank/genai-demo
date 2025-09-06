package solid.humank.genaidemo.infrastructure.persistence.test;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.testutils.database.TestDatabaseManager;

/**
 * 測試用 Repository 適配器基礎類別
 * 提供純領域測試的資料存取功能，不依賴任何基礎設施框架
 * 
 * @param <T>  聚合根類型
 * @param <ID> 識別碼類型
 */
public abstract class TestRepositoryAdapter<T, ID> {

    protected final TestDatabaseManager databaseManager;
    protected final String tableName;

    protected TestRepositoryAdapter(String tableName) {
        this.databaseManager = TestDatabaseManager.getInstance();
        this.tableName = tableName;
    }

    /**
     * 儲存聚合根
     */
    public T save(T aggregateRoot) {
        String id = extractId(aggregateRoot);
        databaseManager.save(tableName, id, aggregateRoot);
        return aggregateRoot;
    }

    /**
     * 根據 ID 查找聚合根
     */
    public Optional<T> findById(ID id) {
        T entity = databaseManager.findById(tableName, id.toString(), getEntityClass());
        return Optional.ofNullable(entity);
    }

    /**
     * 查找所有聚合根
     */
    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return databaseManager.findAll(tableName).values().stream()
                .map(entity -> (T) entity)
                .toList();
    }

    /**
     * 刪除聚合根
     */
    public void delete(T aggregateRoot) {
        String id = extractId(aggregateRoot);
        databaseManager.deleteById(tableName, id);
    }

    /**
     * 根據 ID 刪除聚合根
     */
    public void deleteById(ID id) {
        databaseManager.deleteById(tableName, id.toString());
    }

    /**
     * 檢查聚合根是否存在
     */
    public boolean existsById(ID id) {
        return databaseManager.exists(tableName, id.toString());
    }

    /**
     * 計算聚合根數量
     */
    public long count() {
        return databaseManager.count(tableName);
    }

    /**
     * 子類別需要實作：從聚合根提取 ID
     */
    protected abstract String extractId(T aggregateRoot);

    /**
     * 子類別需要實作：取得實體類別
     */
    protected abstract Class<T> getEntityClass();
}