package solid.humank.genaidemo.agents.common;

import java.util.Map;

/**
 * Agent Tool 介面
 * 
 * 定義 Agent 可以調用的工具標準介面。
 * 每個 Tool 負責將 Agent 的請求轉換為對應的 Application Service 調用。
 */
public interface AgentTool {
    
    /**
     * 取得工具名稱
     * @return 工具名稱，用於 LLM 識別
     */
    String getName();
    
    /**
     * 取得工具描述
     * @return 工具描述，幫助 LLM 理解何時使用此工具
     */
    String getDescription();
    
    /**
     * 取得工具參數 Schema
     * @return 參數定義
     */
    ToolSchema getSchema();
    
    /**
     * 執行工具
     * @param arguments 工具參數
     * @return 執行結果
     */
    ToolResult execute(Map<String, Object> arguments);
}
