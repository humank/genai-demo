package solid.humank.genaidemo.application.stats;

import java.util.Map;

/**
 * 統計數據傳輸對象
 */
public record StatsDto(
        Map<String, Object> stats,
        String status,
        String message
) {
}