package solid.humank.genaidemo.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Business Metrics Service
 * 
 * Provides methods to record custom business metrics that are automatically
 * exported to CloudWatch.
 * 
 * Metric Types:
 * - Counter: Monotonically increasing values (orders created, payments
 * processed)
 * - Gauge: Current value that can go up or down (active users, queue size)
 * - Timer: Duration and rate of events (order processing time)
 * 
 * @author GenAI Demo Team
 * @since 1.0
 */
@Service
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record order created event
     */
    public void recordOrderCreated(String orderType, double orderValue) {
        Counter.builder("business.orders.created")
                .description("Total number of orders created")
                .tag("order_type", orderType)
                .register(meterRegistry)
                .increment();

        // Record order value
        meterRegistry.summary("business.orders.value")
                .record(orderValue);
    }

    /**
     * Record payment processed event
     */
    public void recordPaymentProcessed(String paymentMethod, double amount, boolean success) {
        Counter.builder("business.payments.processed")
                .description("Total number of payments processed")
                .tag("payment_method", paymentMethod)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();

        if (success) {
            meterRegistry.summary("business.payments.amount")
                    .record(amount);
        }
    }

    /**
     * Record customer registration event
     */
    public void recordCustomerRegistration(String registrationSource) {
        Counter.builder("business.customers.registered")
                .description("Total number of customers registered")
                .tag("source", registrationSource)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record API call duration
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timer and record duration
     */
    public void stopTimer(Timer.Sample sample, String operation, String status) {
        sample.stop(Timer.builder("business.operation.duration")
                .description("Duration of business operations")
                .tag("operation", operation)
                .tag("status", status)
                .register(meterRegistry));
    }

    /**
     * Register a gauge for monitoring current values
     * 
     * Example: Active user count, queue size, cache size
     */
    public <T extends Number> void registerGauge(String name, String description,
            Supplier<T> valueSupplier,
            String... tags) {
        Gauge.builder(name, valueSupplier, supplier -> supplier.get().doubleValue())
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }

    /**
     * Record cache hit/miss
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        Counter.builder("business.cache.access")
                .description("Cache access count")
                .tag("cache", cacheName)
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database query execution
     */
    public void recordDatabaseQuery(String queryType, long durationMs) {
        Timer.builder("business.database.query")
                .description("Database query execution time")
                .tag("query_type", queryType)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Record external API call
     */
    public void recordExternalApiCall(String apiName, int statusCode, long durationMs) {
        Counter.builder("business.external.api.calls")
                .description("External API calls")
                .tag("api", apiName)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();

        Timer.builder("business.external.api.duration")
                .description("External API call duration")
                .tag("api", apiName)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Record business error
     */
    public void recordBusinessError(String errorType, String errorCode) {
        Counter.builder("business.errors")
                .description("Business errors count")
                .tag("error_type", errorType)
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Example: Register active users gauge
     */
    public void registerActiveUsersGauge(Supplier<Integer> activeUsersSupplier) {
        registerGauge(
                "business.users.active",
                "Number of currently active users",
                activeUsersSupplier);
    }

    /**
     * Example: Register queue size gauge
     */
    public void registerQueueSizeGauge(String queueName, Supplier<Integer> queueSizeSupplier) {
        registerGauge(
                "business.queue.size",
                "Current queue size",
                queueSizeSupplier,
                "queue", queueName);
    }
}
