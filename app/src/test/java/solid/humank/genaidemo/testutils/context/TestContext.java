package solid.humank.genaidemo.testutils.context;

import solid.humank.genaidemo.testutils.handlers.TestExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 測試上下文管理器
 * 用於在測試執行過程中管理共享狀態和異常處理
 */
public class TestContext {
    
    private final Map<String, Object> context = new HashMap<>();
    private final TestExceptionHandler exceptionHandler = new TestExceptionHandler();
    
    /**
     * 存儲測試上下文中的值
     */
    public <T> void put(String key, T value) {
        context.put(key, value);
    }
    
    /**
     * 從測試上下文中獲取值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new IllegalArgumentException(
            String.format("Value for key '%s' is not of expected type %s, but was %s", 
                key, type.getSimpleName(), value.getClass().getSimpleName())
        );
    }
    
    /**
     * 檢查是否包含指定的鍵
     */
    public boolean contains(String key) {
        return context.containsKey(key);
    }
    
    /**
     * 獲取異常處理器
     */
    public TestExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }
    
    /**
     * 重置測試上下文
     */
    public void reset() {
        context.clear();
        exceptionHandler.reset();
    }
    
    /**
     * 獲取上下文大小
     */
    public int size() {
        return context.size();
    }
    
    /**
     * 檢查上下文是否為空
     */
    public boolean isEmpty() {
        return context.isEmpty();
    }
}