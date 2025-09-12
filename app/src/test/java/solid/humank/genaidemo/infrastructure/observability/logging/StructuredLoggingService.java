package solid.humank.genaidemo.infrastructure.observability.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 結構化日誌服務
 * 提供結構化日誌記錄功能
 */
@Service
public class StructuredLoggingService {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingService.class);

    private final Map<String, Object> contextData = new ConcurrentHashMap<>();

    /**
     * 記錄業務事件
     */
    public void logBusinessEvent(String eventType, String eventId, Map<String, Object> eventData) {
        try {
            Map<String, Object> logData = new ConcurrentHashMap<>();
            logData.put("eventType", eventType);
            logData.put("eventId", eventId);
            logData.put("timestamp", System.currentTimeMillis());
            logData.putAll(eventData);

            log.info("Business event: {} - {}", eventType, logData);
        } catch (Exception e) {
            log.error("Error logging business event", e);
        }
    }

    /**
     * 記錄性能指標
     */
    public void logPerformanceMetric(String metricName, double value, String unit) {
        try {
            Map<String, Object> metricData = Map.of(
                    "metricName", metricName,
                    "value", value,
                    "unit", unit,
                    "timestamp", System.currentTimeMillis());

            log.info("Performance metric: {} = {} {} - {}", metricName, value, unit, metricData);
        } catch (Exception e) {
            log.error("Error logging performance metric", e);
        }
    }

    /**
     * 記錄錯誤事件
     */
    public void logError(String errorType, String errorMessage, Throwable throwable) {
        try {
            Map<String, Object> errorData = Map.of(
                    "errorType", errorType,
                    "errorMessage", errorMessage,
                    "timestamp", System.currentTimeMillis());

            log.error("Error occurred: {} - {}", errorMessage, errorData, throwable);
        } catch (Exception e) {
            log.error("Error logging error event", e);
        }
    }

    /**
     * 設置上下文數據
     */
    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }

    /**
     * 清除上下文數據
     */
    public void clearContext() {
        contextData.clear();
    }
}