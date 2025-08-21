package solid.humank.genaidemo.domain.common.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 業務規則違反異常
 * 
 * 當聚合根的業務規則被違反時拋出此異常
 * 
 * 設計理念：
 * 1. 明確的錯誤資訊：提供詳細的業務規則違反資訊
 * 2. 多重違反支援：支援同時報告多個業務規則違反
 * 3. 上下文資訊：提供違反發生的上下文資訊
 * 4. 可序列化：支援跨邊界傳輸
 */
public class BusinessRuleViolationException extends DomainException {

    private final String aggregateType;
    private final String aggregateId;
    private final List<BusinessRuleViolation> violations;

    /**
     * 建立業務規則違反異常
     * 
     * @param aggregateType 聚合根類型
     * @param aggregateId   聚合根ID
     * @param violation     業務規則違反
     */
    public BusinessRuleViolationException(String aggregateType, String aggregateId,
            BusinessRuleViolation violation) {
        super("BUSINESS_RULE_VIOLATION",
                String.format("業務規則違反 - %s[%s]: %s",
                        aggregateType, aggregateId, violation.getMessage()));
        this.aggregateType = Objects.requireNonNull(aggregateType, "聚合根類型不能為空");
        this.aggregateId = Objects.requireNonNull(aggregateId, "聚合根ID不能為空");
        this.violations = List.of(Objects.requireNonNull(violation, "業務規則違反不能為空"));
    }

    /**
     * 建立業務規則違反異常（多重違反）
     * 
     * @param aggregateType 聚合根類型
     * @param aggregateId   聚合根ID
     * @param violations    業務規則違反列表
     */
    public BusinessRuleViolationException(String aggregateType, String aggregateId,
            List<BusinessRuleViolation> violations) {
        super("BUSINESS_RULE_VIOLATION",
                String.format("業務規則違反 - %s[%s]: %d 個規則違反",
                        aggregateType, aggregateId, violations.size()));
        this.aggregateType = Objects.requireNonNull(aggregateType, "聚合根類型不能為空");
        this.aggregateId = Objects.requireNonNull(aggregateId, "聚合根ID不能為空");
        this.violations = List.copyOf(Objects.requireNonNull(violations, "業務規則違反列表不能為空"));

        if (violations.isEmpty()) {
            throw new IllegalArgumentException("業務規則違反列表不能為空");
        }
    }

    /**
     * 建立業務規則違反異常（簡化版本）
     * 
     * @param aggregateType 聚合根類型
     * @param aggregateId   聚合根ID
     * @param ruleName      規則名稱
     * @param message       錯誤訊息
     */
    public BusinessRuleViolationException(String aggregateType, String aggregateId,
            String ruleName, String message) {
        this(aggregateType, aggregateId,
                new BusinessRuleViolation(ruleName, message, BusinessRuleViolation.Severity.ERROR));
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public List<BusinessRuleViolation> getViolations() {
        return violations;
    }

    /**
     * 獲取第一個違反（用於向後兼容）
     * 
     * @return 第一個業務規則違反
     */
    public BusinessRuleViolation getFirstViolation() {
        return violations.get(0);
    }

    /**
     * 檢查是否包含特定規則的違反
     * 
     * @param ruleName 規則名稱
     * @return 是否包含該規則的違反
     */
    public boolean hasViolation(String ruleName) {
        return violations.stream()
                .anyMatch(violation -> violation.getRuleName().equals(ruleName));
    }

    /**
     * 獲取特定嚴重程度的違反
     * 
     * @param severity 嚴重程度
     * @return 符合條件的違反列表
     */
    public List<BusinessRuleViolation> getViolationsBySeverity(BusinessRuleViolation.Severity severity) {
        return violations.stream()
                .filter(violation -> violation.getSeverity() == severity)
                .toList();
    }

    @Override
    public String getMessage() {
        // 返回第一個違反的訊息（用於向後兼容）
        if (!violations.isEmpty()) {
            return violations.get(0).getMessage();
        }
        return super.getMessage();
    }

    /**
     * 業務規則違反詳細資訊
     */
    public static class BusinessRuleViolation {
        private final String ruleName;
        private final String message;
        private final Severity severity;
        private final String context;

        public BusinessRuleViolation(String ruleName, String message, Severity severity) {
            this(ruleName, message, severity, null);
        }

        public BusinessRuleViolation(String ruleName, String message, Severity severity, String context) {
            this.ruleName = Objects.requireNonNull(ruleName, "規則名稱不能為空");
            this.message = Objects.requireNonNull(message, "錯誤訊息不能為空");
            this.severity = Objects.requireNonNull(severity, "嚴重程度不能為空");
            this.context = context;
        }

        public String getRuleName() {
            return ruleName;
        }

        public String getMessage() {
            return message;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getContext() {
            return context;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s%s",
                    severity, ruleName, message,
                    context != null ? " (上下文: " + context + ")" : "");
        }

        /**
         * 違反嚴重程度
         */
        public enum Severity {
            /** 錯誤：阻止操作繼續 */
            ERROR,
            /** 警告：允許操作但需要注意 */
            WARNING,
            /** 資訊：僅供參考 */
            INFO
        }
    }

    /**
     * 業務規則違反建構器
     */
    public static class Builder {
        private final String aggregateType;
        private final String aggregateId;
        private final List<BusinessRuleViolation> violations = new ArrayList<>();

        public Builder(String aggregateType, String aggregateId) {
            this.aggregateType = Objects.requireNonNull(aggregateType, "聚合根類型不能為空");
            this.aggregateId = Objects.requireNonNull(aggregateId, "聚合根ID不能為空");
        }

        public Builder addViolation(String ruleName, String message,
                BusinessRuleViolation.Severity severity) {
            violations.add(new BusinessRuleViolation(ruleName, message, severity));
            return this;
        }

        public Builder addViolation(String ruleName, String message,
                BusinessRuleViolation.Severity severity, String context) {
            violations.add(new BusinessRuleViolation(ruleName, message, severity, context));
            return this;
        }

        public Builder addError(String ruleName, String message) {
            return addViolation(ruleName, message, BusinessRuleViolation.Severity.ERROR);
        }

        public Builder addWarning(String ruleName, String message) {
            return addViolation(ruleName, message, BusinessRuleViolation.Severity.WARNING);
        }

        public Builder addInfo(String ruleName, String message) {
            return addViolation(ruleName, message, BusinessRuleViolation.Severity.INFO);
        }

        public boolean hasViolations() {
            return !violations.isEmpty();
        }

        public boolean hasErrors() {
            return violations.stream()
                    .anyMatch(v -> v.getSeverity() == BusinessRuleViolation.Severity.ERROR);
        }

        public BusinessRuleViolationException build() {
            if (violations.isEmpty()) {
                throw new IllegalStateException("至少需要一個業務規則違反");
            }
            return new BusinessRuleViolationException(aggregateType, aggregateId, violations);
        }

        public BusinessRuleViolationException buildIfHasErrors() {
            if (hasErrors()) {
                return build();
            }
            return null;
        }
    }
}