package solid.humank.genaidemo.infrastructure.observability.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 可觀測性追蹤上下文管理器
 * 
 * 整合現有 MDC 系統，提供統一的追蹤上下文管理。
 * 支援前端到後端的端到端追蹤，確保追蹤 ID 在整個請求生命週期中傳播。
 * 
 * 功能：
 * - 設定和清理 MDC 追蹤上下文
 * - 整合現有 correlationId 系統
 * - 支援會話 ID 和追蹤 ID 管理
 * - 提供追蹤上下文查詢方法
 * 
 * 需求: 1.1, 1.2, 2.1
 */
@Component
public class ObservabilityTraceContextManager {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityTraceContextManager.class);

    // MDC 鍵常量
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_ID_KEY = "requestId";

    // HTTP 標頭常量
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SESSION_ID_HEADER = "X-Session-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 設定可觀測性上下文
     * 整合現有 MDC 系統，使用 traceId 作為 correlationId
     * 
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     */
    public void setObservabilityContext(String traceId, String sessionId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            // 整合現有 MDC 系統 - 使用 traceId 作為 correlationId
            MDC.put(CORRELATION_ID_KEY, traceId);
            MDC.put(TRACE_ID_KEY, traceId);

            logger.debug("Set trace context - traceId: {}", traceId);
        }

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            MDC.put(SESSION_ID_KEY, sessionId);
            logger.debug("Set session context - sessionId: {}", sessionId);
        }
    }

    /**
     * 設定完整的可觀測性上下文
     * 
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     * @param userId    用戶 ID
     * @param requestId 請求 ID
     */
    public void setFullObservabilityContext(String traceId, String sessionId,
            String userId, String requestId) {
        setObservabilityContext(traceId, sessionId);

        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID_KEY, userId);
            logger.debug("Set user context - userId: {}", userId);
        }

        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID_KEY, requestId);
            logger.debug("Set request context - requestId: {}", requestId);
        }
    }

    /**
     * 清理可觀測性上下文
     */
    public void clearObservabilityContext() {
        String traceId = MDC.get(TRACE_ID_KEY);

        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SESSION_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(REQUEST_ID_KEY);

        if (traceId != null) {
            logger.debug("Cleared trace context - traceId: {}", traceId);
        }
    }

    /**
     * 獲取當前追蹤 ID
     * 
     * @return 追蹤 ID，如果未設定則返回 null
     */
    public String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 獲取當前關聯 ID（與現有系統兼容）
     * 
     * @return 關聯 ID，如果未設定則返回 null
     */
    public String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * 獲取當前會話 ID
     * 
     * @return 會話 ID，如果未設定則返回 null
     */
    public String getCurrentSessionId() {
        return MDC.get(SESSION_ID_KEY);
    }

    /**
     * 獲取當前用戶 ID
     * 
     * @return 用戶 ID，如果未設定則返回 null
     */
    public String getCurrentUserId() {
        return MDC.get(USER_ID_KEY);
    }

    /**
     * 獲取當前請求 ID
     * 
     * @return 請求 ID，如果未設定則返回 null
     */
    public String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    /**
     * 檢查是否有活動的追蹤上下文
     * 
     * @return 如果有追蹤上下文返回 true
     */
    public boolean hasActiveTraceContext() {
        return getCurrentTraceId() != null;
    }

    /**
     * 檢查是否有活動的會話上下文
     * 
     * @return 如果有會話上下文返回 true
     */
    public boolean hasActiveSessionContext() {
        return getCurrentSessionId() != null;
    }

    /**
     * 生成新的追蹤 ID
     * 格式：trace-{timestamp}-{random}
     * 
     * @return 新的追蹤 ID
     */
    public String generateTraceId() {
        return String.format("trace-%d-%s",
                System.currentTimeMillis(),
                java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 生成新的請求 ID
     * 格式：req-{timestamp}-{random}
     * 
     * @return 新的請求 ID
     */
    public String generateRequestId() {
        return String.format("req-%d-%s",
                System.currentTimeMillis(),
                java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 確保有追蹤上下文，如果沒有則生成新的
     * 
     * @return 追蹤 ID
     */
    public String ensureTraceContext() {
        String traceId = getCurrentTraceId();
        if (traceId == null) {
            traceId = generateTraceId();
            setObservabilityContext(traceId, null);
            logger.debug("Generated new trace context - traceId: {}", traceId);
        }
        return traceId;
    }

    /**
     * 複製當前上下文到新線程
     * 用於異步處理時保持追蹤上下文
     * 
     * @return 上下文快照
     */
    public TraceContextSnapshot captureContext() {
        return new TraceContextSnapshot(
                getCurrentTraceId(),
                getCurrentSessionId(),
                getCurrentUserId(),
                getCurrentRequestId());
    }

    /**
     * 恢復追蹤上下文
     * 
     * @param snapshot 上下文快照
     */
    public void restoreContext(TraceContextSnapshot snapshot) {
        if (snapshot != null) {
            setFullObservabilityContext(
                    snapshot.traceId(),
                    snapshot.sessionId(),
                    snapshot.userId(),
                    snapshot.requestId());
        }
    }

    /**
     * 追蹤上下文快照
     * 用於在異步處理中傳遞上下文
     */
    public record TraceContextSnapshot(
            String traceId,
            String sessionId,
            String userId,
            String requestId) {
    }

    /**
     * 獲取當前上下文的摘要資訊（用於日誌）
     * 
     * @return 上下文摘要
     */
    public String getContextSummary() {
        return String.format("TraceContext[trace=%s, session=%s, user=%s, request=%s]",
                truncate(getCurrentTraceId()),
                truncate(getCurrentSessionId()),
                truncate(getCurrentUserId()),
                truncate(getCurrentRequestId()));
    }

    private String truncate(String value) {
        if (value == null)
            return "null";
        return value.length() > 8 ? value.substring(0, 8) + "..." : value;
    }
}