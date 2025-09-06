package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.LocalDateTime;
import java.util.Random;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 優惠券代碼值對象 */
@ValueObject(name = "VoucherCode", description = "優惠券兌換代碼值對象")
public record VoucherCode(String code, LocalDateTime generatedAt, LocalDateTime expiresAt) {
    public VoucherCode {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("優惠券代碼不能為空");
        }
        if (expiresAt.isBefore(generatedAt)) {
            throw new IllegalArgumentException("過期時間不能早於生成時間");
        }
    }

    public boolean isValid() {
        return LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isExpired() {
        return !isValid();
    }

    public static VoucherCode generate(int validDays) {
        LocalDateTime now = LocalDateTime.now();
        String code = generateUniqueCode();
        return new VoucherCode(code, now, now.plusDays(validDays));
    }

    private static String generateUniqueCode() {
        // 生成格式：VC + 時間戳後6位 + 4位隨機數
        long timestamp = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(timestamp).substring(7);
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return "VC" + timestampSuffix + randomSuffix;
    }

    public long getRemainingHours() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }
}