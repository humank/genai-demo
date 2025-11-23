package solid.humank.genaidemo.domain.common.record;

import java.time.LocalDateTime;
import java.util.List;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;

/** 訂單摘要記錄類 使用 Java 21 的 Record 功能 */
public record OrderSummary(
        String orderId,
        String customerId,
        OrderStatus status,
        Money totalAmount,
        Money effectiveAmount,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    /** 使用緊湊建構子 */
    public OrderSummary {
        // 驗證參數
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("客戶ID不能為空");
        }
        if (status == null) {
            throw new IllegalArgumentException("訂單狀態不能為空");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("訂單總金額不能為空");
        }
        if (effectiveAmount == null) {
            throw new IllegalArgumentException("訂單實際金額不能為空");
        }
        if (itemCount < 0) {
            throw new IllegalArgumentException("訂單項數量不能為負數");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("創建時間不能為空");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("更新時間不能為空");
        }
    }

    /**
     * 檢查訂單是否已完成
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED;
    }

    /**
     * 檢查訂單是否已取消
     *
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    /**
     * 檢查訂單是否有折扣
     *
     * @return 是否有折扣
     */
    public boolean hasDiscount() {
        return !totalAmount.equals(effectiveAmount);
    }

    /**
     * 獲取折扣金額
     *
     * @return 折扣金額
     */
    public Money getDiscountAmount() {
        return totalAmount.subtract(effectiveAmount);
    }

    /**
     * 獲取訂單摘要信息
     *
     * @return 訂單摘要信息
     */
    public String getSummary() {
        // 使用 Java 21 的 String Templates
        return "訂單摘要:\n"
                + "訂單ID: "
                + orderId
                + "\n"
                + "客戶ID: "
                + customerId
                + "\n"
                + "狀態: "
                + status.getDescription()
                + "\n"
                + "總金額: "
                + totalAmount
                + "\n"
                + "實際金額: "
                + effectiveAmount
                + "\n"
                + "訂單項數量: "
                + itemCount
                + "\n"
                + "創建時間: "
                + createdAt
                + "\n"
                + "更新時間: "
                + updatedAt;
    }

    /**
     * 創建訂單摘要列表
     *
     * @param summaries 訂單摘要列表
     * @return 格式化的訂單摘要列表
     */
    public static String formatSummaryList(List<OrderSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "沒有訂單記錄";
        }

        var builder = new StringBuilder("訂單列表:\n");

        // 使用 Java 21 的 Stream API 增強功能
        summaries.forEach(
                summary -> builder.append("- 訂單 ")
                            .append(summary.orderId())
                            .append(": ")
                            .append(summary.status().getDescription())
                            .append(", ")
                            .append(summary.totalAmount())
                            .append("\n"));

        return builder.toString();
    }
}
