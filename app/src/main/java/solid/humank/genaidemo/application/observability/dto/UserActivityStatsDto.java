package solid.humank.genaidemo.application.observability.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用戶活躍度統計 DTO
 * 
 * 用於傳輸用戶活躍度相關的統計數據，包含用戶行為分析、活躍用戶排行等指標。
 * 僅在生產環境中使用，支援用戶行為分析。
 * 
 * 設計原則：
 * - 不可變 DTO，確保數據一致性
 * - 專注於用戶行為指標
 * - 支援 JSON 序列化
 * - 建構者模式，便於創建
 * 
 * 需求: 2.3, 3.3
 */
public record UserActivityStatsDto(
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Map<String, Object>> userActivityStats,
        List<Map<String, Object>> mostActiveUsers) {

    /**
     * 建構者類別
     */
    public static class Builder {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Map<String, Object>> userActivityStats;
        private List<Map<String, Object>> mostActiveUsers;

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder userActivityStats(List<Map<String, Object>> userActivityStats) {
            this.userActivityStats = userActivityStats;
            return this;
        }

        public Builder mostActiveUsers(List<Map<String, Object>> mostActiveUsers) {
            this.mostActiveUsers = mostActiveUsers;
            return this;
        }

        public UserActivityStatsDto build() {
            return new UserActivityStatsDto(
                    startTime, endTime, userActivityStats, mostActiveUsers);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 獲取總活躍用戶數
     */
    public Long getTotalActiveUsers() {
        if (userActivityStats == null) {
            return 0L;
        }
        return (long) userActivityStats.size();
    }

    /**
     * 獲取最活躍用戶
     */
    public Map<String, Object> getTopActiveUser() {
        if (mostActiveUsers == null || mostActiveUsers.isEmpty()) {
            return Map.of();
        }
        return mostActiveUsers.get(0);
    }

    /**
     * 計算平均用戶活躍度
     */
    public Double getAverageUserActivity() {
        if (userActivityStats == null || userActivityStats.isEmpty()) {
            return 0.0;
        }

        double totalActivity = userActivityStats.stream()
                .mapToDouble(user -> {
                    Object sessionCount = user.get("sessionCount");
                    Object pageViews = user.get("totalPageViews");
                    Object actions = user.get("totalUserActions");

                    double sessions = sessionCount instanceof Number ? ((Number) sessionCount).doubleValue() : 0.0;
                    double views = pageViews instanceof Number ? ((Number) pageViews).doubleValue() : 0.0;
                    double userActions = actions instanceof Number ? ((Number) actions).doubleValue() : 0.0;

                    // 活躍度分數 = 會話數 + 頁面瀏覽數 + 用戶操作數
                    return sessions + views + userActions;
                })
                .sum();

        return totalActivity / userActivityStats.size();
    }

    /**
     * 獲取高活躍度用戶數量
     */
    public Long getHighActivityUsersCount() {
        if (userActivityStats == null) {
            return 0L;
        }

        double avgActivity = getAverageUserActivity();
        double threshold = avgActivity * 1.5; // 高於平均值 50% 視為高活躍度

        return userActivityStats.stream()
                .mapToLong(user -> {
                    Object sessionCount = user.get("sessionCount");
                    Object pageViews = user.get("totalPageViews");
                    Object actions = user.get("totalUserActions");

                    double sessions = sessionCount instanceof Number ? ((Number) sessionCount).doubleValue() : 0.0;
                    double views = pageViews instanceof Number ? ((Number) pageViews).doubleValue() : 0.0;
                    double userActions = actions instanceof Number ? ((Number) actions).doubleValue() : 0.0;

                    double activity = sessions + views + userActions;
                    return activity > threshold ? 1L : 0L;
                })
                .sum();
    }

    /**
     * 計算用戶留存率
     */
    public Double getUserRetentionRate() {
        if (userActivityStats == null || userActivityStats.isEmpty()) {
            return 0.0;
        }

        // 計算有多個會話的用戶比例（簡化的留存率計算）
        long retainedUsers = userActivityStats.stream()
                .mapToLong(user -> {
                    Object sessionCount = user.get("sessionCount");
                    if (sessionCount instanceof Number) {
                        return ((Number) sessionCount).longValue() > 1 ? 1L : 0L;
                    }
                    return 0L;
                })
                .sum();

        return (double) retainedUsers / userActivityStats.size();
    }

    /**
     * 獲取用戶參與度分佈
     */
    public Map<String, Long> getUserEngagementDistribution() {
        if (userActivityStats == null) {
            return Map.of(
                    "low", 0L,
                    "medium", 0L,
                    "high", 0L);
        }

        double avgActivity = getAverageUserActivity();

        long lowEngagement = 0L;
        long mediumEngagement = 0L;
        long highEngagement = 0L;

        for (Map<String, Object> user : userActivityStats) {
            Object sessionCount = user.get("sessionCount");
            Object pageViews = user.get("totalPageViews");
            Object actions = user.get("totalUserActions");

            double sessions = sessionCount instanceof Number ? ((Number) sessionCount).doubleValue() : 0.0;
            double views = pageViews instanceof Number ? ((Number) pageViews).doubleValue() : 0.0;
            double userActions = actions instanceof Number ? ((Number) actions).doubleValue() : 0.0;

            double activity = sessions + views + userActions;

            if (activity < avgActivity * 0.5) {
                lowEngagement++;
            } else if (activity < avgActivity * 1.5) {
                mediumEngagement++;
            } else {
                highEngagement++;
            }
        }

        return Map.of(
                "low", lowEngagement,
                "medium", mediumEngagement,
                "high", highEngagement);
    }

    /**
     * 檢查用戶活躍度是否健康
     */
    public boolean hasHealthyUserActivity() {
        Double retentionRate = getUserRetentionRate();
        Long highActivityUsers = getHighActivityUsersCount();
        Long totalUsers = getTotalActiveUsers();

        // 健康指標：留存率 > 30%，高活躍用戶比例 > 20%
        boolean goodRetention = retentionRate != null && retentionRate > 0.3;
        boolean goodEngagement = totalUsers > 0 && (double) highActivityUsers / totalUsers > 0.2;

        return goodRetention && goodEngagement;
    }
}