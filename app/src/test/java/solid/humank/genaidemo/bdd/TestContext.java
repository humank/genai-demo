package solid.humank.genaidemo.bdd;

import java.util.HashMap;
import java.util.Map;

/**
 * 測試上下文類，用於在步驟之間共享數據
 * 不依賴 Spring 上下文
 */
public class TestContext {
    private static final TestContext INSTANCE = new TestContext();
    
    private final Map<String, Object> contextMap = new HashMap<>();
    
    private TestContext() {
        // 私有構造函數，確保單例
    }
    
    public static TestContext getInstance() {
        return INSTANCE;
    }
    
    public void set(String key, Object value) {
        contextMap.put(key, value);
    }
    
    public Object get(String key) {
        return contextMap.get(key);
    }
    
    public void clear() {
        contextMap.clear();
    }
}