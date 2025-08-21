package solid.humank.genaidemo.application.stats;

import java.util.Map;

/** 支付方式統計數據傳輸對象 */
public record PaymentMethodStatsDto(
        Map<String, Integer> paymentMethodDistribution, String status, String message) {}
