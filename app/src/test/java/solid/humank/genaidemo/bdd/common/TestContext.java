package solid.humank.genaidemo.bdd.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * BDD 測試上下文
 * 用於在不同步驟定義之間共享測試數據
 */
@Component
public class TestContext {

    private static TestContext instance;
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private String lastErrorMessage;

    public TestContext() {
        instance = this;
    }

    public static TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) context.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = context.get(key);
        return type.cast(value);
    }

    public boolean containsKey(String key) {
        return context.containsKey(key);
    }

    public boolean contains(String key) {
        return containsKey(key);
    }

    public void remove(String key) {
        context.remove(key);
    }

    public void clear() {
        context.clear();
        lastErrorMessage = null;
    }

    public Map<String, Object> getAll() {
        return new ConcurrentHashMap<>(context);
    }

    public boolean hasError() {
        return lastErrorMessage != null;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String errorMessage) {
        this.lastErrorMessage = errorMessage;
    }
}