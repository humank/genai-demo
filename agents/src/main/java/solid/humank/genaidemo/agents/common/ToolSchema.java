package solid.humank.genaidemo.agents.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool 參數 Schema 定義
 * 
 * 用於描述 Tool 的輸入參數結構，供 LLM 理解如何調用工具。
 */
public class ToolSchema {
    
    private final List<Parameter> parameters;
    
    private ToolSchema(List<Parameter> parameters) {
        this.parameters = parameters;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    /**
     * 轉換為 JSON Schema 格式（供 Bedrock API 使用）
     */
    public String toJsonSchema() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        
        List<String> required = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = parameters.get(i);
            if (i > 0) sb.append(",");
            sb.append("\"").append(param.name()).append("\":{");
            sb.append("\"type\":\"").append(param.type()).append("\",");
            sb.append("\"description\":\"").append(param.description()).append("\"");
            sb.append("}");
            if (param.required()) {
                required.add(param.name());
            }
        }
        
        sb.append("},\"required\":[");
        for (int i = 0; i < required.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(required.get(i)).append("\"");
        }
        sb.append("]}");
        
        return sb.toString();
    }
    
    public record Parameter(String name, String type, String description, boolean required) {}
    
    public static class Builder {
        private final List<Parameter> parameters = new ArrayList<>();
        
        public Builder addParameter(String name, String type, String description, boolean required) {
            parameters.add(new Parameter(name, type, description, required));
            return this;
        }
        
        public ToolSchema build() {
            return new ToolSchema(parameters);
        }
    }
}
