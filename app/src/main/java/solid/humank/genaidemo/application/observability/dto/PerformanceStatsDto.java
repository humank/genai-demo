package solid.humank.genaidemo.application.observability.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 效能統計 DTO
 * 
 * 用於傳輸效能相關的統計數據，包含 Core Web Vitals、效能問題頁面等指標。
 * 僅在生產環境中使用，支援效能監控和優化。
 * 
 * 設計原則：
 * - 不可變 DTO，確保數據一致性
 * - 專注於效能監控指標
 * - 支援 JSON 序列化
 * - 建構者模式，便於創建
 * 
 * 需求: 2.3, 3.3
 */
public record PerformanceStatsDto(
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Map<String, Object>> performanceMetrics,
        List<Map<String, Object>> performanceIssuePages) {

    /**
     * 建構者類別
     */
    public static class Builder {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Map<String, Object>> performanceMetrics;
        private List<Map<String, Object>> performanceIssuePages;

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder performanceMetrics(List<Map<String, Object>> performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }

        public Builder performanceIssuePages(List<Map<String, Object>> performanceIssuePages) {
            this.performanceIssuePages = performanceIssuePages;
            return this;
        }

        public PerformanceStatsDto build() {
            return new PerformanceStatsDto(
                    startTime, endTime, performanceMetrics, performanceIssuePages);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 獲取 Core Web Vitals 統計
     */
    public Map<String, Object> getCoreWebVitalsStats() {
        if (performanceMetrics == null) {
            return Map.of();
        }

        return performanceMetrics.stream()
                .filter(metric -> {
                    String metricType = (String) metric.get("metricType");
                    return "lcp".equals(metricType) || "fid".equals(metricType) || "cls".equals(metricType);
                })
                .findFirst()
                .orElse(Map.of());
    }

    /**
     * 檢查是否有嚴重效能問題
     */
    public boolean hasCriticalPerformanceIssues() {
        if (performanceIssuePages == null || performanceIssuePages.isEmpty()) {
            return false;
        }

        return performanceIssuePages.stream()
                .anyMatch(page -> {
                    Object avgValue = page.get("averageValue");
                    if (avgValue instanceof Number) {
                        double value = ((Number) avgValue).doubleValue();
                        String metricType = (String) page.get("metricType");

                        // 檢查是否超過嚴重閾值
                        return switch (metricType) {
                            case "lcp" -> value > 4000; // LCP > 4s (嚴重)
                            case "fid" -> value > 300; // FID > 300ms (嚴重)
                            case "cls" -> value > 0.25; // CLS > 0.25 (嚴重)
                            case "ttfb" -> value > 1800; // TTFB > 1.8s (嚴重)
                            default -> false;
                        };
                    }
                    return false;
                });
    }

    /**
     * 計算效能分數 (0-100)
     */
    public Double getPerformanceScore() {
        if (performanceMetrics == null || performanceMetrics.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int metricCount = 0;

        for (Map<String, Object> metric : performanceMetrics) {
            String metricType = (String) metric.get("metricType");
            Object avgValue = metric.get("averageValue");

            if (avgValue instanceof Number) {
                double value = ((Number) avgValue).doubleValue();
                double score = calculateMetricScore(metricType, value);
                totalScore += score;
                metricCount++;
            }
        }

        return metricCount > 0 ? totalScore / metricCount : 0.0;
    }

    private double calculateMetricScore(String metricType, double value) {
        return switch (metricType) {
            case "lcp" -> {
                if (value <= 2500)
                    yield 100.0;
                if (value <= 4000)
                    yield 50.0;
                yield 0.0;
            }
            case "fid" -> {
                if (value <= 100)
                    yield 100.0;
                if (value <= 300)
                    yield 50.0;
                yield 0.0;
            }
            case "cls" -> {
                if (value <= 0.1)
                    yield 100.0;
                if (value <= 0.25)
                    yield 50.0;
                yield 0.0;
            }
            case "ttfb" -> {
                if (value <= 600)
                    yield 100.0;
                if (value <= 1800)
                    yield 50.0;
                yield 0.0;
            }
            default -> 50.0; // 未知指標給予中等分數
        };
    }

    /**
     * 獲取效能等級
     */
    public String getPerformanceGrade() {
        Double score = getPerformanceScore();
        if (score >= 90)
            return "A";
        if (score >= 80)
            return "B";
        if (score >= 70)
            return "C";
        if (score >= 60)
            return "D";
        return "F";
    }

    /**
     * 獲取最需要優化的頁面
     */
    public Map<String, Object> getWorstPerformingPage() {
        if (performanceIssuePages == null || performanceIssuePages.isEmpty()) {
            return Map.of();
        }

        return performanceIssuePages.stream()
                .max((page1, page2) -> {
                    Object value1 = page1.get("averageValue");
                    Object value2 = page2.get("averageValue");

                    if (value1 instanceof Number && value2 instanceof Number) {
                        return Double.compare(
                                ((Number) value1).doubleValue(),
                                ((Number) value2).doubleValue());
                    }
                    return 0;
                })
                .orElse(Map.of());
    }
}