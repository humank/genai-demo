package solid.humank.genaidemo.interfaces.observability;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.application.observability.query.AnalyticsQueryService;
import solid.humank.genaidemo.infrastructure.observability.persistence.AnalyticsEventEntity;

/**
 * 分析數據查詢 API 控制器
 * 
 * 提供可觀測性數據的查詢和統計 API，支援業務分析和監控儀表板。
 * 
 * 需求: 2.3, 3.3
 */
@RestController
@RequestMapping("/api/v1/analytics/query")
@Tag(name = "Analytics Query", description = "分析數據查詢 API")
public class AnalyticsQueryController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsQueryController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/user-behavior")
    @Operation(summary = "查詢用戶行為統計", description = "根據用戶 ID 和時間範圍查詢用戶行為統計數據")
    public ResponseEntity<AnalyticsQueryService.UserBehaviorStats> getUserBehaviorStats(
            @Parameter(description = "用戶 ID") @RequestParam String userId,
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AnalyticsQueryService.UserBehaviorStats stats = analyticsQueryService.getUserBehaviorStats(
                userId, startTime, endTime);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/page-performance")
    @Operation(summary = "查詢頁面效能統計", description = "根據頁面路徑和時間範圍查詢頁面效能統計數據")
    public ResponseEntity<AnalyticsQueryService.PagePerformanceStats> getPagePerformanceStats(
            @Parameter(description = "頁面路徑") @RequestParam String page,
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AnalyticsQueryService.PagePerformanceStats stats = analyticsQueryService.getPagePerformanceStats(
                page, startTime, endTime);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/business-metrics")
    @Operation(summary = "查詢業務指標統計", description = "根據時間範圍查詢業務指標統計數據")
    public ResponseEntity<AnalyticsQueryService.BusinessMetricsStats> getBusinessMetricsStats(
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AnalyticsQueryService.BusinessMetricsStats stats = analyticsQueryService.getBusinessMetricsStats(
                startTime, endTime);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/session-stats")
    @Operation(summary = "查詢會話統計", description = "根據時間範圍查詢會話統計數據")
    public ResponseEntity<AnalyticsQueryService.SessionStats> getSessionStats(
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AnalyticsQueryService.SessionStats stats = analyticsQueryService.getSessionStats(startTime, endTime);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/events")
    @Operation(summary = "分頁查詢分析事件", description = "分頁查詢所有分析事件數據")
    public ResponseEntity<Page<AnalyticsEventEntity>> getAnalyticsEvents(Pageable pageable) {
        Page<AnalyticsEventEntity> events = analyticsQueryService.getAnalyticsEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/by-type")
    @Operation(summary = "根據事件類型查詢事件", description = "根據事件類型和時間範圍查詢事件")
    public ResponseEntity<List<AnalyticsEventEntity>> getEventsByType(
            @Parameter(description = "事件類型") @RequestParam String eventType,
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<AnalyticsEventEntity> events = analyticsQueryService.getEventsByType(eventType, startTime, endTime);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/popular-pages")
    @Operation(summary = "查詢熱門頁面", description = "根據時間範圍查詢熱門頁面排行")
    public ResponseEntity<List<AnalyticsQueryService.PopularPage>> getPopularPages(
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "返回數量限制") @RequestParam(defaultValue = "10") int limit) {

        List<AnalyticsQueryService.PopularPage> pages = analyticsQueryService.getPopularPages(
                startTime, endTime, limit);
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/activity-trends")
    @Operation(summary = "查詢用戶活動趨勢", description = "根據時間範圍查詢用戶活動趨勢數據")
    public ResponseEntity<List<AnalyticsQueryService.ActivityTrend>> getActivityTrends(
            @Parameter(description = "開始時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "結束時間") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<AnalyticsQueryService.ActivityTrend> trends = analyticsQueryService.getActivityTrends(
                startTime, endTime);
        return ResponseEntity.ok(trends);
    }
}