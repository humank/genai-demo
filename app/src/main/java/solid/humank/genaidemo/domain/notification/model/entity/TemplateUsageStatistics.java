package solid.humank.genaidemo.domain.notification.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.TemplateUsageStatisticsId;

/**
 * 模板使用統計 Entity
 * 
 * 追蹤和統計通知模板的使用情況，包含發送次數、成功率、效能指標等
 */
@Entity(name = "TemplateUsageStatistics", description = "模板使用統計實體，追蹤通知模板的使用情況和效能指標")
public class TemplateUsageStatistics {

    private final TemplateUsageStatisticsId id;
    private final String templateId; // 關聯的模板ID
    private final LocalDate statisticsDate; // 統計日期
    private final NotificationChannel channel; // 通知渠道

    // 使用統計
    private int totalSent; // 總發送次數
    private int successfulSent; // 成功發送次數
    private int failedSent; // 失敗發送次數
    private int bounced; // 退信次數
    private int opened; // 開啟次數（適用於郵件）
    private int clicked; // 點擊次數（適用於郵件）
    private int unsubscribed; // 取消訂閱次數

    // 效能統計
    private long totalRenderTime; // 總渲染時間（毫秒）
    private long averageRenderTime; // 平均渲染時間（毫秒）
    private long maxRenderTime; // 最大渲染時間（毫秒）
    private long minRenderTime; // 最小渲染時間（毫秒）

    // 錯誤統計
    private int renderErrors; // 渲染錯誤次數
    private int validationErrors; // 驗證錯誤次數
    private int templateNotFoundErrors; // 模板不存在錯誤次數
    private int variableMissingErrors; // 變數缺失錯誤次數

    // 時間戳
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime lastUsedAt; // 最後使用時間

    public TemplateUsageStatistics(TemplateUsageStatisticsId id, String templateId,
            LocalDate statisticsDate, NotificationChannel channel) {
        this.id = Objects.requireNonNull(id, "Template usage statistics ID cannot be null");
        this.templateId = Objects.requireNonNull(templateId, "Template ID cannot be null");
        this.statisticsDate = Objects.requireNonNull(statisticsDate, "Statistics date cannot be null");
        this.channel = Objects.requireNonNull(channel, "Notification channel cannot be null");

        // 初始化統計數據
        this.totalSent = 0;
        this.successfulSent = 0;
        this.failedSent = 0;
        this.bounced = 0;
        this.opened = 0;
        this.clicked = 0;
        this.unsubscribed = 0;

        this.totalRenderTime = 0;
        this.averageRenderTime = 0;
        this.maxRenderTime = 0;
        this.minRenderTime = Long.MAX_VALUE;

        this.renderErrors = 0;
        this.validationErrors = 0;
        this.templateNotFoundErrors = 0;
        this.variableMissingErrors = 0;

        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = this.createdAt;
    }

    // Getters
    public TemplateUsageStatisticsId getId() {
        return id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public LocalDate getStatisticsDate() {
        return statisticsDate;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public int getTotalSent() {
        return totalSent;
    }

    public int getSuccessfulSent() {
        return successfulSent;
    }

    public int getFailedSent() {
        return failedSent;
    }

    public int getBounced() {
        return bounced;
    }

    public int getOpened() {
        return opened;
    }

    public int getClicked() {
        return clicked;
    }

    public int getUnsubscribed() {
        return unsubscribed;
    }

    public long getTotalRenderTime() {
        return totalRenderTime;
    }

    public long getAverageRenderTime() {
        return averageRenderTime;
    }

    public long getMaxRenderTime() {
        return maxRenderTime;
    }

    public long getMinRenderTime() {
        return minRenderTime == Long.MAX_VALUE ? 0 : minRenderTime;
    }

    public int getRenderErrors() {
        return renderErrors;
    }

    public int getValidationErrors() {
        return validationErrors;
    }

    public int getTemplateNotFoundErrors() {
        return templateNotFoundErrors;
    }

    public int getVariableMissingErrors() {
        return variableMissingErrors;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    // 業務方法

    /** 記錄成功發送 */
    public void recordSuccessfulSend(long renderTime) {
        this.totalSent++;
        this.successfulSent++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();

        updateRenderTimeStatistics(renderTime);
    }

    /** 記錄失敗發送 */
    public void recordFailedSend(long renderTime) {
        this.totalSent++;
        this.failedSent++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();

        updateRenderTimeStatistics(renderTime);
    }

    /** 記錄退信 */
    public void recordBounce() {
        this.bounced++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄開啟 */
    public void recordOpen() {
        this.opened++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄點擊 */
    public void recordClick() {
        this.clicked++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄取消訂閱 */
    public void recordUnsubscribe() {
        this.unsubscribed++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄渲染錯誤 */
    public void recordRenderError() {
        this.renderErrors++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄驗證錯誤 */
    public void recordValidationError() {
        this.validationErrors++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄模板不存在錯誤 */
    public void recordTemplateNotFoundError() {
        this.templateNotFoundErrors++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 記錄變數缺失錯誤 */
    public void recordVariableMissingError() {
        this.variableMissingErrors++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 更新渲染時間統計 */
    private void updateRenderTimeStatistics(long renderTime) {
        this.totalRenderTime += renderTime;

        if (this.totalSent > 0) {
            this.averageRenderTime = this.totalRenderTime / this.totalSent;
        }

        if (renderTime > this.maxRenderTime) {
            this.maxRenderTime = renderTime;
        }

        if (renderTime < this.minRenderTime) {
            this.minRenderTime = renderTime;
        }
    }

    /** 計算成功率 */
    public double getSuccessRate() {
        if (totalSent == 0)
            return 0.0;
        return (double) successfulSent / totalSent * 100.0;
    }

    /** 計算失敗率 */
    public double getFailureRate() {
        if (totalSent == 0)
            return 0.0;
        return (double) failedSent / totalSent * 100.0;
    }

    /** 計算退信率 */
    public double getBounceRate() {
        if (totalSent == 0)
            return 0.0;
        return (double) bounced / totalSent * 100.0;
    }

    /** 計算開啟率 */
    public double getOpenRate() {
        if (successfulSent == 0)
            return 0.0;
        return (double) opened / successfulSent * 100.0;
    }

    /** 計算點擊率 */
    public double getClickRate() {
        if (opened == 0)
            return 0.0;
        return (double) clicked / opened * 100.0;
    }

    /** 計算點擊開啟率 */
    public double getClickThroughRate() {
        if (successfulSent == 0)
            return 0.0;
        return (double) clicked / successfulSent * 100.0;
    }

    /** 計算取消訂閱率 */
    public double getUnsubscribeRate() {
        if (successfulSent == 0)
            return 0.0;
        return (double) unsubscribed / successfulSent * 100.0;
    }

    /** 計算總錯誤率 */
    public double getErrorRate() {
        int totalErrors = renderErrors + validationErrors + templateNotFoundErrors + variableMissingErrors;
        if (totalSent == 0)
            return 0.0;
        return (double) totalErrors / totalSent * 100.0;
    }

    /** 檢查是否有使用記錄 */
    public boolean hasUsage() {
        return totalSent > 0;
    }

    /** 檢查效能是否良好 */
    public boolean hasGoodPerformance() {
        return getSuccessRate() >= 95.0 && getAverageRenderTime() <= 1000; // 成功率95%以上，平均渲染時間1秒以內
    }

    /** 檢查是否需要優化 */
    public boolean needsOptimization() {
        return getSuccessRate() < 90.0 || getAverageRenderTime() > 2000 || getErrorRate() > 5.0;
    }

    /** 重置統計數據 */
    public void reset() {
        this.totalSent = 0;
        this.successfulSent = 0;
        this.failedSent = 0;
        this.bounced = 0;
        this.opened = 0;
        this.clicked = 0;
        this.unsubscribed = 0;

        this.totalRenderTime = 0;
        this.averageRenderTime = 0;
        this.maxRenderTime = 0;
        this.minRenderTime = Long.MAX_VALUE;

        this.renderErrors = 0;
        this.validationErrors = 0;
        this.templateNotFoundErrors = 0;
        this.variableMissingErrors = 0;

        this.lastUpdatedAt = LocalDateTime.now();
    }

    /** 合併統計數據 */
    public void merge(TemplateUsageStatistics other) {
        if (!this.templateId.equals(other.templateId) ||
                !this.statisticsDate.equals(other.statisticsDate) ||
                !this.channel.equals(other.channel)) {
            throw new IllegalArgumentException("Cannot merge statistics from different template, date, or channel");
        }

        this.totalSent += other.totalSent;
        this.successfulSent += other.successfulSent;
        this.failedSent += other.failedSent;
        this.bounced += other.bounced;
        this.opened += other.opened;
        this.clicked += other.clicked;
        this.unsubscribed += other.unsubscribed;

        this.totalRenderTime += other.totalRenderTime;
        if (this.totalSent > 0) {
            this.averageRenderTime = this.totalRenderTime / this.totalSent;
        }
        this.maxRenderTime = Math.max(this.maxRenderTime, other.maxRenderTime);
        this.minRenderTime = Math.min(this.getMinRenderTime(), other.getMinRenderTime());

        this.renderErrors += other.renderErrors;
        this.validationErrors += other.validationErrors;
        this.templateNotFoundErrors += other.templateNotFoundErrors;
        this.variableMissingErrors += other.variableMissingErrors;

        this.lastUpdatedAt = LocalDateTime.now();
        if (other.lastUsedAt != null &&
                (this.lastUsedAt == null || other.lastUsedAt.isAfter(this.lastUsedAt))) {
            this.lastUsedAt = other.lastUsedAt;
        }
    }

    /** 創建今日統計 */
    public static TemplateUsageStatistics createTodayStatistics(String templateId, NotificationChannel channel) {
        return new TemplateUsageStatistics(
                TemplateUsageStatisticsId.generate(),
                templateId,
                LocalDate.now(),
                channel);
    }

    /** 創建指定日期統計 */
    public static TemplateUsageStatistics createStatistics(String templateId, LocalDate date,
            NotificationChannel channel) {
        return new TemplateUsageStatistics(
                TemplateUsageStatisticsId.generate(),
                templateId,
                date,
                channel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        TemplateUsageStatistics that = (TemplateUsageStatistics) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                "TemplateUsageStatistics{id=%s, templateId='%s', date=%s, channel=%s, sent=%d, success=%.1f%%}",
                id, templateId, statisticsDate, channel, totalSent, getSuccessRate());
    }
}