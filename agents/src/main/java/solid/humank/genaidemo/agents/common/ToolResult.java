package solid.humank.genaidemo.agents.common;

import java.util.Map;

/**
 * Tool 執行結果
 * 
 * 封裝 Tool 執行的結果，包括成功/失敗狀態和回傳資料。
 */
public record ToolResult(
    boolean success,
    String message,
    Map<String, Object> data
) {
    
    /**
     * 建立成功結果
     */
    public static ToolResult success(String message) {
        return new ToolResult(true, message, Map.of());
    }
    
    /**
     * 建立成功結果（含資料）
     */
    public static ToolResult success(String message, Map<String, Object> data) {
        return new ToolResult(true, message, data);
    }
    
    /**
     * 建立失敗結果
     */
    public static ToolResult error(String message) {
        return new ToolResult(false, message, Map.of());
    }
    
    /**
     * 建立失敗結果（含錯誤詳情）
     */
    public static ToolResult error(String message, Map<String, Object> errorDetails) {
        return new ToolResult(false, message, errorDetails);
    }
}
