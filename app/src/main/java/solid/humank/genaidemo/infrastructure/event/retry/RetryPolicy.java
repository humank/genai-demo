package solid.humank.genaidemo.infrastructure.event.retry;

import java.time.Duration;
import java.util.function.Predicate;

import solid.humank.genaidemo.infrastructure.event.EventProcessingException;

/**
 * 重試策略配置
 * 需求 9.1: 實現事件處理失敗的錯誤記錄和重試機制
 */
public class RetryPolicy {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Predicate<Throwable> retryCondition;

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.retryCondition = builder.retryCondition;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public boolean shouldRetry(Throwable throwable, int attemptCount) {
        return attemptCount < maxAttempts && retryCondition.test(throwable);
    }

    /**
     * 計算下次重試的延遲時間
     */
    public Duration calculateDelay(int attemptCount) {
        if (attemptCount <= 1) {
            return initialDelay;
        }

        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptCount - 1));
        Duration calculatedDelay = Duration.ofMillis(delayMillis);

        return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
    }

    /**
     * 默認重試策略：最多重試3次，指數退避
     */
    public static RetryPolicy defaultPolicy() {
        return new Builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofSeconds(1))
                .maxDelay(Duration.ofMinutes(1))
                .backoffMultiplier(2.0)
                .retryOn(throwable -> !(throwable instanceof EventProcessingException) ||
                        ((EventProcessingException) throwable).isRetryable())
                .build();
    }

    /**
     * 快速重試策略：適用於輕量級操作
     */
    public static RetryPolicy fastRetryPolicy() {
        return new Builder()
                .maxAttempts(5)
                .initialDelay(Duration.ofMillis(100))
                .maxDelay(Duration.ofSeconds(5))
                .backoffMultiplier(1.5)
                .retryOn(throwable -> true)
                .build();
    }

    /**
     * 無重試策略：適用於不可重試的操作
     */
    public static RetryPolicy noRetryPolicy() {
        return new Builder()
                .maxAttempts(1)
                .initialDelay(Duration.ZERO)
                .maxDelay(Duration.ZERO)
                .backoffMultiplier(1.0)
                .retryOn(throwable -> false)
                .build();
    }

    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofMinutes(1);
        private double backoffMultiplier = 2.0;
        private Predicate<Throwable> retryCondition = throwable -> true;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder retryOn(Predicate<Throwable> retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}