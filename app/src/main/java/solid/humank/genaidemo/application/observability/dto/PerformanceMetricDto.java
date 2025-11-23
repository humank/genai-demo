package solid.humank.genaidemo.application.observability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 效能指標 DTO
 *
 * 用於接收前端發送的效能指標數據，包括核心網頁指標和其他效能測量。
 *
 * 需求: 1.3, 2.1
 */
public record PerformanceMetricDto(
        @NotBlank(message = "Metric ID cannot be blank") String metricId,

        @NotBlank(message = "Metric type cannot be blank") String metricType,

        @PositiveOrZero(message = "Metric value must be positive or zero") double value,

        @NotBlank(message = "Page cannot be blank") String page,

        Long timestamp // 前端時間戳
) {

    /**
     * 創建效能指標 DTO
     *
     * @param metricId   指標 ID
     * @param metricType 指標類型
     * @param value      指標值
     * @param page       頁面路徑
     * @return 效能指標 DTO
     */
    public static PerformanceMetricDto create(String metricId, String metricType,
            double value, String page) {
        return new PerformanceMetricDto(metricId, metricType, value, page, System.currentTimeMillis());
    }

    /**
     * 檢查是否為核心網頁指標 (Core Web Vitals)
     *
     * @return 如果是核心網頁指標返回 true
     */
    public boolean isCoreWebVital() {
        return "lcp".equals(metricType) ||
                "fid".equals(metricType) ||
                "cls".equals(metricType);
    }

    /**
     * 檢查是否為載入時間指標
     *
     * @return 如果是載入時間指標返回 true
     */
    public boolean isLoadTimeMetric() {
        return "ttfb".equals(metricType) ||
                "page_load".equals(metricType);
    }

    /**
     * 檢查是否為 API 相關指標
     *
     * @return 如果是 API 相關指標返回 true
     */
    public boolean isApiMetric() {
        return "api_response_time".equals(metricType) ||
                "api_error_rate".equals(metricType);
    }

    /**
     * 獲取指標值的毫秒表示（如果適用）
     *
     * @return 毫秒值
     */
    public long getValueInMilliseconds() {
        // CLS 是比率，不需要轉換
        if ("cls".equals(metricType)) {
            return Math.round(value * 1000); // 轉換為千分之一
        }
        // 其他指標通常已經是毫秒
        return Math.round(value);
    }

    /**
     * 檢查指標是否超過建議閾值
     * 基於 Google Core Web Vitals 建議
     *
     * @return 如果超過閾值返回 true
     */
    public boolean exceedsRecommendedThreshold() {
        return switch (metricType) {
            case "lcp" -> value > 2500; // LCP > 2.5s
            case "fid" -> value > 100; // FID > 100ms
            case "cls" -> value > 0.1; // CLS > 0.1
            case "ttfb" -> value > 600; // TTFB > 600ms
            case "page_load" -> value > 3000; // Page Load > 3s
            case "api_response_time" -> value > 1000; // API > 1s
            default -> false;
        };
    }

    /**
     * 獲取效能等級
     *
     * @return 效能等級 (good, needs-improvement, poor)
     */
    public String getPerformanceGrade() {
        return switch (metricType) {
            case "lcp" -> {
                if (value <= 2500)
                    yield "good";
                if (value <= 4000)
                    yield "needs-improvement";
                yield "poor";
            }
            case "fid" -> {
                if (value <= 100)
                    yield "good";
                if (value <= 300)
                    yield "needs-improvement";
                yield "poor";
            }
            case "cls" -> {
                if (value <= 0.1)
                    yield "good";
                if (value <= 0.25)
                    yield "needs-improvement";
                yield "poor";
            }
            case "ttfb" -> {
                if (value <= 600)
                    yield "good";
                if (value <= 1500)
                    yield "needs-improvement";
                yield "poor";
            }
            default -> "unknown";
        };
    }
}
