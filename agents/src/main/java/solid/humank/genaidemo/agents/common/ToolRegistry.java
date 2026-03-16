package solid.humank.genaidemo.agents.common;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 註冊中心
 * 
 * 管理所有可用的 Agent Tools，提供工具查詢和執行功能。
 */
@Component
public class ToolRegistry {
    
    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();
    
    public ToolRegistry(List<AgentTool> agentTools) {
        agentTools.forEach(tool -> tools.put(tool.getName(), tool));
    }
    
    /**
     * 註冊工具
     */
    public void register(AgentTool tool) {
        tools.put(tool.getName(), tool);
    }
    
    /**
     * 取得工具
     */
    public Optional<AgentTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    /**
     * 取得所有可用工具
     */
    public Collection<AgentTool> getAvailableTools() {
        return tools.values();
    }
    
    /**
     * 執行工具
     */
    public ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        return getTool(toolName)
            .map(tool -> tool.execute(arguments))
            .orElse(ToolResult.error("Tool not found: " + toolName));
    }
    
    /**
     * 產生工具定義（供 LLM 使用）
     */
    public String generateToolDefinitions() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        int i = 0;
        for (AgentTool tool : tools.values()) {
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"name\":\"").append(tool.getName()).append("\",");
            sb.append("\"description\":\"").append(tool.getDescription()).append("\",");
            sb.append("\"input_schema\":").append(tool.getSchema().toJsonSchema());
            sb.append("}");
            i++;
        }
        
        sb.append("]");
        return sb.toString();
    }
}
