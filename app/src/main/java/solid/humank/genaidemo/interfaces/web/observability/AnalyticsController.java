package solid.humank.genaidemo.interfaces.web.observability;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import solid.humank.genaidemo.application.observability.dto.AnalyticsEventDto;
import solid.humank.genaidemo.application.observability.dto.PerformanceMetricDto;
import solid.humank.genaidemo.application.observability.service.ObservabilityEventService;
import solid.humank.genaidemo.infrastructure.observability.tracing.ObservabilityTraceContextManager;

/**
 * 分析事件控制器
 * 
 * 接收前端發送的用戶行為分析事件和效能指標，轉換為領域事件並發布到事件系統。
 * 支援批次處理和追蹤上下文管理。
 * 
 * 功能：
 * - 接收分析事件批次
 * - 接收效能指標批次
 * - 管理追蹤上下文
 * - 整合現有 MDC 系統
 * 
 * 需求: 1.1, 1.2, 1.3, 2.1
 */
@RestController
@RequestMapping("/api/analytics")
@Validated
@Tag(name = "Analytics", description = "前端可觀測性分析事件 API")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final ObservabilityEventService observabilityEventService;
    private final ObservabilityTraceContextManager traceContextManager;

    public AnalyticsController(
            ObservabilityEventService observabilityEventService,
            ObservabilityTraceContextManager traceContextManager) {
        this.observabilityEventService = observabilityEventService;
        this.traceContextManager = traceContextManager;

        logger.info("AnalyticsController initialized");
    }

    /**
     * 接收分析事件批次
     * 
     * @param events    分析事件列表
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     * @return 處理結果
     */
    @PostMapping("/events")
    @Operation(summary = "接收分析事件批次", description = "接收前端發送的用戶行為分析事件批次，包括頁面瀏覽、用戶操作和業務事件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "事件處理成功"),
            @ApiResponse(responseCode = "400", description = "請求參數無效"),
            @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseEntity<Void> receiveEvents(
            @Valid @RequestBody List<AnalyticsEventDto> events,

            @Parameter(description = "追蹤 ID，用於端到端追蹤", required = true) @RequestHeader("X-Trace-Id") String traceId,

            @Parameter(description = "會話 ID，用於關聯用戶會話", required = true) @RequestHeader("X-Session-Id") String sessionId,

            @Parameter(description = "用戶 ID，可選") @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 立即設定 MDC 上下文以確保所有後續處理都有正確的 correlation ID
        traceContextManager.setFullObservabilityContext(traceId, sessionId, userId, null);

        try {
            logger.info("Received {} analytics events [correlationId: {}]", events.size(), traceId);

            // 處理事件 (內部會重新設定 MDC，但這裡先設定確保 Controller 層的日誌正確)
            observabilityEventService.processAnalyticsEvents(events, traceId, sessionId);

            logger.debug("Successfully processed {} analytics events [correlationId: {}]",
                    events.size(), traceId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Failed to process analytics events [correlationId: {}]", traceId, e);
            return ResponseEntity.internalServerError().build();

        } finally {
            // 清理 MDC 上下文
            traceContextManager.clearObservabilityContext();
        }
    }

    /**
     * 接收效能指標批次
     * 
     * @param metrics   效能指標列表
     * @param traceId   追蹤 ID
     * @param sessionId 會話 ID
     * @return 處理結果
     */
    @PostMapping("/performance")
    @Operation(summary = "接收效能指標批次", description = "接收前端發送的效能指標批次，包括核心網頁指標（LCP, FID, CLS）和其他效能測量")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "指標處理成功"),
            @ApiResponse(responseCode = "400", description = "請求參數無效"),
            @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseEntity<Void> receivePerformanceMetrics(
            @Valid @RequestBody List<PerformanceMetricDto> metrics,

            @Parameter(description = "追蹤 ID，用於端到端追蹤", required = true) @RequestHeader("X-Trace-Id") String traceId,

            @Parameter(description = "會話 ID，用於關聯用戶會話", required = true) @RequestHeader("X-Session-Id") String sessionId,

            @Parameter(description = "用戶 ID，可選") @RequestHeader(value = "X-User-Id", required = false) String userId) {
        traceContextManager.setFullObservabilityContext(traceId, sessionId, userId, null);

        try {
            logger.info("Received {} performance metrics [correlationId: {}]", metrics.size(), traceId);

            // 處理效能指標
            observabilityEventService.processPerformanceMetrics(metrics, traceId, sessionId);

            logger.debug("Successfully processed {} performance metrics [correlationId: {}]",
                    metrics.size(), traceId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Failed to process performance metrics [correlationId: {}]", traceId, e);
            return ResponseEntity.internalServerError().build();

        } finally {
            // 清理 MDC 上下文
            traceContextManager.clearObservabilityContext();
        }
    }

    /**
     * 健康檢查端點
     * 
     * @return 服務狀態
     */
    @PostMapping("/health")
    @Operation(summary = "分析服務健康檢查", description = "檢查分析服務的健康狀態")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics service is healthy");
    }
}