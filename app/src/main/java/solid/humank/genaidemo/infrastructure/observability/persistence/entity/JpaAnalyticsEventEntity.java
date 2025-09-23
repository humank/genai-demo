package solid.humank.genaidemo.infrastructure.observability.persistence.entity;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import solid.humank.genaidemo.infrastructure.observability.persistence.converter.JsonMapConverter;

/**
 * 分析事件 JPA 實體
 * 
 * 用於持久化用戶行為事件和效能指標，僅在生產環境中使用。
 * 支援詳細的事件查詢和統計分析。
 * 
 * 設計原則：
 * - 僅作為持久化技術實現，不包含業務邏輯
 * - 支援高效查詢的索引設計
 * - 包含數據保留政策相關欄位
 * - 支援多種事件類型的統一儲存
 * 
 * 需求: 2.3, 3.3
 */
@Entity
@Table(name = "analytics_events")
public class JpaAnalyticsEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "domain_event_id", nullable = false)
    private String domainEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "frontend_event_type", nullable = false)
    private String frontendEventType;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "page_path")
    private String pagePath;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "metric_type")
    private String metricType;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "event_data", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> eventData;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "retention_date", nullable = false)
    private LocalDateTime retentionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 默認建構子，JPA 需要
    public JpaAnalyticsEventEntity() {
    }

    // 建構子
    public JpaAnalyticsEventEntity(String eventId, String domainEventId, String eventType,
            String frontendEventType, String sessionId, String userId,
            String traceId, Map<String, Object> eventData,
            LocalDateTime occurredAt, LocalDateTime receivedAt) {
        this.id = generateId(eventId, sessionId);
        this.eventId = eventId;
        this.domainEventId = domainEventId;
        this.eventType = eventType;
        this.frontendEventType = frontendEventType;
        this.sessionId = sessionId;
        this.userId = userId;
        this.traceId = traceId;
        this.eventData = eventData;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;

        // 從事件數據中提取常用欄位
        extractCommonFields();

        // 設定數據保留期限（預設 30 天）
        this.retentionDate = receivedAt.plusDays(30);
    }

    private String generateId(String eventId, String sessionId) {
        return "analytics_event_" + sessionId + "_" + eventId;
    }

    private void extractCommonFields() {
        if (eventData != null) {
            // 提取頁面路徑
            Object page = eventData.get("page");
            if (page != null) {
                this.pagePath = page.toString();
            }

            // 提取操作類型
            Object action = eventData.get("action");
            if (action != null) {
                this.actionType = action.toString();
            }

            // 提取指標類型和值（效能指標）
            Object metricType = eventData.get("metricType");
            if (metricType != null) {
                this.metricType = metricType.toString();
            }

            Object value = eventData.get("value");
            if (value != null) {
                try {
                    this.metricValue = Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    // 忽略無法轉換的值
                }
            }
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDomainEventId() {
        return domainEventId;
    }

    public void setDomainEventId(String domainEventId) {
        this.domainEventId = domainEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getFrontendEventType() {
        return frontendEventType;
    }

    public void setFrontendEventType(String frontendEventType) {
        this.frontendEventType = frontendEventType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
        extractCommonFields();
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getRetentionDate() {
        return retentionDate;
    }

    public void setRetentionDate(LocalDateTime retentionDate) {
        this.retentionDate = retentionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 業務方法
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(retentionDate);
    }

    public boolean isPageViewEvent() {
        return "page_view".equals(frontendEventType);
    }

    public boolean isUserActionEvent() {
        return "user_action".equals(frontendEventType);
    }

    public boolean isBusinessEvent() {
        return "business_event".equals(frontendEventType);
    }

    public boolean isPerformanceMetric() {
        return "performance_metric".equals(frontendEventType);
    }

    public boolean isAnonymousUser() {
        return userId == null || userId.trim().isEmpty();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (retentionDate == null) {
            retentionDate = createdAt.plusDays(30);
        }
    }
}