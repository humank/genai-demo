package solid.humank.genaidemo.application.stats;

import java.util.Map;

/** 訂單狀態統計數據傳輸對象 */
public record OrderStatusStatsDto(
        Map<String, Integer> statusDistribution, String status, String message) {}
