package solid.humank.genaidemo.interfaces.web.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.application.monitoring.dto.BackpressureStatusDto;
import solid.humank.genaidemo.application.monitoring.dto.ProcessingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.RetryStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SequenceTrackingStatisticsDto;
import solid.humank.genaidemo.application.monitoring.dto.SystemHealthStatusDto;
import solid.humank.genaidemo.application.monitoring.service.EventMonitoringApplicationService;

/**
 * 事件監控控制器
 * 提供事件處理系統的監控和管理端點
 * 
 * 需求 9.2: 建立系統異常的適當錯誤回應和日誌記錄
 */
@RestController
@RequestMapping("/api/monitoring/events")
@Tag(name = "事件監控", description = "事件處理系統監控和管理")
public class EventMonitoringController {

    private final EventMonitoringApplicationService eventMonitoringService;

    public EventMonitoringController(EventMonitoringApplicationService eventMonitoringService) {
        this.eventMonitoringService = eventMonitoringService;
    }

    /**
     * 獲取系統健康狀態
     */
    @GetMapping("/health")
    @Operation(summary = "獲取系統健康狀態", description = "返回事件處理系統的整體健康狀態")
    public ResponseEntity<SystemHealthStatusDto> getSystemHealth() {
        SystemHealthStatusDto health = eventMonitoringService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * 獲取事件處理統計
     */
    @GetMapping("/statistics/processing")
    @Operation(summary = "獲取事件處理統計", description = "返回事件處理的詳細統計信息")
    public ResponseEntity<ProcessingStatisticsDto> getProcessingStatistics() {
        ProcessingStatisticsDto stats = eventMonitoringService.getProcessingStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 獲取重試統計
     */
    @GetMapping("/statistics/retry")
    @Operation(summary = "獲取重試統計", description = "返回事件重試的統計信息")
    public ResponseEntity<RetryStatisticsDto> getRetryStatistics() {
        RetryStatisticsDto stats = eventMonitoringService.getRetryStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 獲取背壓狀態
     */
    @GetMapping("/backpressure/status")
    @Operation(summary = "獲取背壓狀態", description = "返回當前的背壓狀態和負載信息")
    public ResponseEntity<BackpressureStatusDto> getBackpressureStatus() {
        BackpressureStatusDto status = eventMonitoringService.getBackpressureStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 獲取序列追蹤統計
     */
    @GetMapping("/statistics/sequence")
    @Operation(summary = "獲取序列追蹤統計", description = "返回事件序列追蹤的統計信息")
    public ResponseEntity<SequenceTrackingStatisticsDto> getSequenceStatistics() {
        SequenceTrackingStatisticsDto stats = eventMonitoringService.getSequenceStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 重置處理統計
     */
    @PostMapping("/statistics/processing/reset")
    @Operation(summary = "重置處理統計", description = "重置事件處理的統計信息")
    public ResponseEntity<String> resetProcessingStatistics() {
        eventMonitoringService.resetProcessingStatistics();
        return ResponseEntity.ok("Processing statistics reset successfully");
    }

    /**
     * 重置聚合根序列
     */
    @PostMapping("/sequence/{aggregateId}/reset")
    @Operation(summary = "重置聚合根序列", description = "重置指定聚合根的事件序列追蹤")
    public ResponseEntity<String> resetAggregateSequence(@PathVariable String aggregateId) {
        eventMonitoringService.resetAggregateSequence(aggregateId);
        return ResponseEntity.ok("Sequence reset successfully for aggregate: " + aggregateId);
    }

    /**
     * 強制更新聚合根序列
     */
    @PostMapping("/sequence/{aggregateId}/update")
    @Operation(summary = "強制更新聚合根序列", description = "強制更新指定聚合根的事件序列號")
    public ResponseEntity<String> forceUpdateSequence(@PathVariable String aggregateId,
            @RequestParam long sequence) {
        eventMonitoringService.forceUpdateSequence(aggregateId, sequence);
        return ResponseEntity
                .ok(String.format("Sequence updated successfully for aggregate: %s to: %d", aggregateId, sequence));
    }

    /**
     * 獲取聚合根當前序列
     */
    @GetMapping("/sequence/{aggregateId}")
    @Operation(summary = "獲取聚合根當前序列", description = "返回指定聚合根的當前事件序列號")
    public ResponseEntity<Long> getCurrentSequence(@PathVariable String aggregateId) {
        long sequence = eventMonitoringService.getCurrentSequence(aggregateId);
        return ResponseEntity.ok(sequence);
    }
}