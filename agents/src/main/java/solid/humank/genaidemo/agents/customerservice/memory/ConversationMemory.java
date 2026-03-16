package solid.humank.genaidemo.agents.customerservice.memory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 對話記憶
 * 
 * 儲存單一對話 Session 的短期記憶，包括對話歷史和上下文。
 */
public class ConversationMemory {
    
    private static final int MAX_HISTORY_SIZE = 20;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    
    private final String sessionId;
    private final List<Message> history;
    private final Map<String, Object> context;
    private Instant lastActivity;
    private final Duration ttl;
    
    public ConversationMemory(String sessionId) {
        this(sessionId, DEFAULT_TTL);
    }
    
    public ConversationMemory(String sessionId, Duration ttl) {
        this.sessionId = sessionId;
        this.history = new ArrayList<>();
        this.context = new java.util.HashMap<>();
        this.lastActivity = Instant.now();
        this.ttl = ttl;
    }
    
    /**
     * 新增使用者訊息
     */
    public void addUserMessage(String content) {
        addMessage(new Message(Role.USER, content, Instant.now()));
    }
    
    /**
     * 新增助手訊息
     */
    public void addAssistantMessage(String content) {
        addMessage(new Message(Role.ASSISTANT, content, Instant.now()));
    }
    
    private void addMessage(Message message) {
        history.add(message);
        lastActivity = Instant.now();
        
        // 保持歷史記錄在限制內
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }
    
    /**
     * 取得對話歷史
     */
    public List<Message> getHistory() {
        return List.copyOf(history);
    }
    
    /**
     * 設定上下文變數
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
        lastActivity = Instant.now();
    }
    
    /**
     * 取得上下文變數
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) context.get(key);
    }
    
    /**
     * 檢查記憶是否過期
     */
    public boolean isExpired() {
        return Instant.now().isAfter(lastActivity.plus(ttl));
    }
    
    /**
     * 取得對話輪數
     */
    public int getTurnCount() {
        return (int) history.stream()
            .filter(m -> m.role() == Role.USER)
            .count();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Instant getLastActivity() {
        return lastActivity;
    }
    
    /**
     * 訊息角色
     */
    public enum Role {
        USER, ASSISTANT, SYSTEM
    }
    
    /**
     * 對話訊息
     */
    public record Message(Role role, String content, Instant timestamp) {}
}
