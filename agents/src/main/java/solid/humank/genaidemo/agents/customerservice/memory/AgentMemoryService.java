package solid.humank.genaidemo.agents.customerservice.memory;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 記憶服務
 * 
 * 管理 Agent 的對話記憶，提供記憶的載入、更新和清理功能。
 * 
 * POC 版本使用 In-Memory 實作，生產環境應替換為 DynamoDB。
 */
@Service
public class AgentMemoryService {
    
    private final Map<String, ConversationMemory> memoryStore = new ConcurrentHashMap<>();
    
    /**
     * 載入或建立對話記憶
     */
    public ConversationMemory loadMemory(String sessionId) {
        return memoryStore.compute(sessionId, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new ConversationMemory(sessionId);
            }
            return existing;
        });
    }
    
    /**
     * 更新對話記憶
     */
    public void updateMemory(String sessionId, String userMessage, String assistantMessage) {
        ConversationMemory memory = loadMemory(sessionId);
        memory.addUserMessage(userMessage);
        memory.addAssistantMessage(assistantMessage);
    }
    
    /**
     * 設定對話上下文
     */
    public void setContext(String sessionId, String key, Object value) {
        ConversationMemory memory = loadMemory(sessionId);
        memory.setContext(key, value);
    }
    
    /**
     * 取得對話上下文
     */
    public <T> T getContext(String sessionId, String key) {
        ConversationMemory memory = memoryStore.get(sessionId);
        if (memory == null) {
            return null;
        }
        return memory.getContext(key);
    }
    
    /**
     * 清除對話記憶
     */
    public void clearMemory(String sessionId) {
        memoryStore.remove(sessionId);
    }
    
    /**
     * 清理過期記憶
     */
    public void cleanupExpiredMemories() {
        memoryStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 取得記憶統計
     */
    public MemoryStats getStats() {
        int total = memoryStore.size();
        int expired = (int) memoryStore.values().stream()
            .filter(ConversationMemory::isExpired)
            .count();
        return new MemoryStats(total, total - expired, expired);
    }
    
    public record MemoryStats(int total, int active, int expired) {}
}
