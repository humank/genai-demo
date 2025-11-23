package solid.humank.genaidemo.application.observability.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.common.service.DomainEventApplicationService;
import solid.humank.genaidemo.application.observability.dto.AnalyticsEventDto;
import solid.humank.genaidemo.application.observability.dto.PerformanceMetricDto;
import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;
import solid.humank.genaidemo.infrastructure.observability.tracing.ObservabilityTraceContextManager;

/**
 * 可觀測性事件服務
 * 
 * 處理從前端接收的分析事件和效能指標，轉換為領域事件並發布。
 * 整合現有 DDD 系統和 MDC 追蹤機制。
 * 
 * 職責：
 * - 處理前端分析事件
 * - 處理效能指標
 * - 轉換為領域事件
 * - 管理追蹤上下文
 * - 發布事件到現有事件系統
 * 
 * 需求: 1.1, 1.2, 1.3, 2.1, 2.2
 */
@Service
@Transactional
public class ObservabilityEventService {    private static final Logger logger = LoggerFactory.getLogger(ObservabilityEventService.class);

    private final DomainEventApplicationService domainEventService;
    private final ObservabilityTraceContextManager traceContextManager;

    public ObservabilityEventService(
            DomainEventApplicationService domainEventService,
            ObservabilityTraceContextManager traceContextManager) {
        this.domainEventService = domainEventService;
        this.traceContextManager = traceContextManager;

        logger.info("ObservabilityEventService initialized with domain event service: {}",
                domainEventService.getPublisherInfo());
    }

    /**
     * 處理分析事件批次
     * 
     * @param events    分析事件列表
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     */
    public void processAnalyticsEvents(List<AnalyticsEventDto> events, String traceId, String sessionId) {
        if (events == null || events.isEmpty()) {
            logger.debug("No analytics events to process");
            return;
        }

        // 設定 MDC 上下文，整合現有追蹤系統
        traceContextManager.setObservabilityContext(traceId, sessionId);

        try {
            logger.info("Processing {} analytics events [correlationId: {}]", events.size(), traceId);

            // 轉換為領域事件並發布
            for (AnalyticsEventDto eventDto : events) {
                UserBehaviorAnalyticsEvent domainEvent = convertToAnalyticsEvent(eventDto, traceId, sessionId);
                domainEventService.publishEvent(domainEvent);
            }

            logger.debug("Successfully processed {} analytics events [correlationId: {}]",
                    events.size(), traceId);

        } catch (Exception e) {
            logger.error("Failed to process analytics events [correlationId: {}]", traceId, e);
            throw new ObservabilityEventProcessingException("Failed to process analytics events", e);
        } finally {
            // 清理 MDC 上下文
            traceContextManager.clearObservabilityContext();
        }
    }

    /**
     * 處理效能指標批次
     * 
     * @param metrics   效能指標列表
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     */
    public void processPerformanceMetrics(List<PerformanceMetricDto> metrics, String traceId, String sessionId) {
        if (metrics == null || metrics.isEmpty()) {
            logger.debug("No performance metrics to process");
            return;
        }

        // 設定 MDC 上下文
        traceContextManager.setObservabilityContext(traceId, sessionId);

        try {
            logger.info("Processing {} performance metrics [correlationId: {}]", metrics.size(), traceId);

            // 轉換為領域事件並發布
            for (PerformanceMetricDto metricDto : metrics) {
                PerformanceMetricReceivedEvent domainEvent = convertToPerformanceEvent(metricDto, traceId, sessionId);
                domainEventService.publishEvent(domainEvent);
            }

            logger.debug("Successfully processed {} performance metrics [correlationId: {}]",
                    metrics.size(), traceId);

        } catch (Exception e) {
            logger.error("Failed to process performance metrics [correlationId: {}]", traceId, e);
            throw new ObservabilityEventProcessingException("Failed to process performance metrics", e);
        } finally {
            // 清理 MDC 上下文
            traceContextManager.clearObservabilityContext();
        }
    }

    /**
     * 處理單個分析事件
     * 
     * @param eventDto  分析事件 DTO
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     */
    public void processAnalyticsEvent(AnalyticsEventDto eventDto, String traceId, String sessionId) {
        processAnalyticsEvents(List.of(eventDto), traceId, sessionId);
    }

    /**
     * 處理單個效能指標
     * 
     * @param metricDto 效能指標 DTO
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     */
    public void processPerformanceMetric(PerformanceMetricDto metricDto, String traceId, String sessionId) {
        processPerformanceMetrics(List.of(metricDto), traceId, sessionId);
    }

    // === Private Methods ===

    /**
     * 轉換分析事件 DTO 為領域事件
     * 
     * @param eventDto  分析事件 DTO
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     * @return 用戶行為分析事件
     */
    private UserBehaviorAnalyticsEvent convertToAnalyticsEvent(
            AnalyticsEventDto eventDto, String traceId, String sessionId) {

        validateAnalyticsEventDto(eventDto);

        return UserBehaviorAnalyticsEvent.create(
                eventDto.eventId(),
                eventDto.eventType(),
                sessionId,
                Optional.ofNullable(eventDto.userId()),
                traceId,
                eventDto.data() != null ? eventDto.data() : Map.of());
    }

    /**
     * 轉換效能指標 DTO 為領域事件
     * 
     * @param metricDto 效能指標 DTO
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     * @return 效能指標接收事件
     */
    private PerformanceMetricReceivedEvent convertToPerformanceEvent(
            PerformanceMetricDto metricDto, String traceId, String sessionId) {

        validatePerformanceMetricDto(metricDto);

        return PerformanceMetricReceivedEvent.create(
                metricDto.metricId(),
                metricDto.metricType(),
                metricDto.value(),
                metricDto.page(),
                sessionId,
                traceId);
    }

    /**
     * 驗證分析事件 DTO
     * 
     * @param eventDto 分析事件 DTO
     */
    private void validateAnalyticsEventDto(AnalyticsEventDto eventDto) {
        if (eventDto == null) {
            throw new IllegalArgumentException("Analytics event DTO cannot be null");
        }
        if (eventDto.eventId() == null || eventDto.eventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (eventDto.eventType() == null || eventDto.eventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
    }

    /**
     * 驗證效能指標 DTO
     * 
     * @param metricDto 效能指標 DTO
     */
    private void validatePerformanceMetricDto(PerformanceMetricDto metricDto) {
        if (metricDto == null) {
            throw new IllegalArgumentException("Performance metric DTO cannot be null");
        }
        if (metricDto.metricId() == null || metricDto.metricId().trim().isEmpty()) {
            throw new IllegalArgumentException("Metric ID cannot be null or empty");
        }
        if (metricDto.metricType() == null || metricDto.metricType().trim().isEmpty()) {
            throw new IllegalArgumentException("Metric type cannot be null or empty");
        }
        if (metricDto.value() < 0) {
            throw new IllegalArgumentException("Metric value cannot be negative");
        }
        if (metricDto.page() == null || metricDto.page().trim().isEmpty()) {
            throw new IllegalArgumentException("Page cannot be null or empty");
        }
    }

    /**
     * 可觀測性事件處理異常
     */
    public static class ObservabilityEventProcessingException extends RuntimeException {
        public ObservabilityEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}