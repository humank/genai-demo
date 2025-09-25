package solid.humank.genaidemo.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

/**
 * 事件處理配置
 * 配置異步處理和調度任務，並整合 KEDA 和 HPA 的指標導出
 */
@Configuration
@EnableAsync
// @EnableScheduling // 禁用所有定時任務以避免記憶體問題
public class EventProcessingConfig {

    private final MeterRegistry meterRegistry;

    public EventProcessingConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 事件處理線程池 - 支援 KEDA 指標監控
     */
    @Bean(name = "eventProcessingExecutor")
    public ThreadPoolTaskExecutor eventProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("EventProcessing-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 設置允許核心線程超時，支援動態調整
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        
        // 註冊 Micrometer 指標以供 KEDA 使用
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), 
            "event_processing_executor", Tags.of("executor", "event-processing"));
        
        // 註冊額外的 KEDA 監控指標
        registerKEDAMetrics(meterRegistry, executor, "event_processing");
        
        return executor;
    }

    /**
     * 重試處理線程池 - 支援 KEDA 指標監控
     */
    @Bean(name = "retryExecutor")
    public ThreadPoolTaskExecutor retryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EventRetry-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 設置允許核心線程超時，支援動態調整
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        
        // 註冊 Micrometer 指標以供 KEDA 使用
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), 
            "retry_executor", Tags.of("executor", "retry"));
        
        // 註冊額外的 KEDA 監控指標
        registerKEDAMetrics(meterRegistry, executor, "retry");
        
        return executor;
    }

    /**
     * 動態線程池配置屬性
     * 支援 Kubernetes ConfigMap 動態更新
     */
    @ConfigurationProperties(prefix = "app.thread-pool")
    public static class ThreadPoolProperties {
        private int eventProcessingCoreSize = 10;
        private int eventProcessingMaxSize = 50;
        private int eventProcessingQueueCapacity = 200;
        
        private int retryCoreSize = 5;
        private int retryMaxSize = 20;
        private int retryQueueCapacity = 100;
        
        // Getters and setters
        public int getEventProcessingCoreSize() { return eventProcessingCoreSize; }
        public void setEventProcessingCoreSize(int eventProcessingCoreSize) { this.eventProcessingCoreSize = eventProcessingCoreSize; }
        
        public int getEventProcessingMaxSize() { return eventProcessingMaxSize; }
        public void setEventProcessingMaxSize(int eventProcessingMaxSize) { this.eventProcessingMaxSize = eventProcessingMaxSize; }
        
        public int getEventProcessingQueueCapacity() { return eventProcessingQueueCapacity; }
        public void setEventProcessingQueueCapacity(int eventProcessingQueueCapacity) { this.eventProcessingQueueCapacity = eventProcessingQueueCapacity; }
        
        public int getRetryCoreSize() { return retryCoreSize; }
        public void setRetryCoreSize(int retryCoreSize) { this.retryCoreSize = retryCoreSize; }
        
        public int getRetryMaxSize() { return retryMaxSize; }
        public void setRetryMaxSize(int retryMaxSize) { this.retryMaxSize = retryMaxSize; }
        
        public int getRetryQueueCapacity() { return retryQueueCapacity; }
        public void setRetryQueueCapacity(int retryQueueCapacity) { this.retryQueueCapacity = retryQueueCapacity; }
    }

    /**
     * 註冊 KEDA 專用的監控指標
     * 這些指標將被 Prometheus 收集並用於 KEDA 自動擴展決策
     */
    private void registerKEDAMetrics(MeterRegistry meterRegistry, ThreadPoolTaskExecutor executor, String poolName) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        
        // 活躍線程數 - KEDA 主要監控指標
        Gauge.builder("thread_pool_active_threads", threadPoolExecutor, ThreadPoolExecutor::getActiveCount)
            .description("Number of active threads in the thread pool")
            .tags("pool", poolName, "component", "thread-pool")
            .register(meterRegistry);
        
        // 線程池使用率 - KEDA 擴展觸發器
        Gauge.builder("thread_pool_utilization_ratio", threadPoolExecutor, tpe -> {
                int maxPoolSize = tpe.getMaximumPoolSize();
                int activeCount = tpe.getActiveCount();
                return maxPoolSize > 0 ? (double) activeCount / maxPoolSize : 0.0;
            })
            .description("Thread pool utilization ratio (active/max)")
            .tags("pool", poolName, "component", "thread-pool")
            .register(meterRegistry);
        
        // 佇列使用率 - KEDA 背壓監控
        Gauge.builder("thread_pool_queue_utilization_ratio", threadPoolExecutor, tpe -> {
                int queueCapacity = tpe.getQueue().remainingCapacity() + tpe.getQueue().size();
                int queueSize = tpe.getQueue().size();
                return queueCapacity > 0 ? (double) queueSize / queueCapacity : 0.0;
            })
            .description("Thread pool queue utilization ratio (queued/capacity)")
            .tags("pool", poolName, "component", "thread-pool")
            .register(meterRegistry);
        
        // 線程池壓力指標 - KEDA 高級擴展邏輯
        Gauge.builder("thread_pool_pressure_score", threadPoolExecutor, tpe -> {
                int maxPoolSize = tpe.getMaximumPoolSize();
                int activeCount = tpe.getActiveCount();
                int queueSize = tpe.getQueue().size();
                
                double threadUtilization = maxPoolSize > 0 ? (double) activeCount / maxPoolSize : 0.0;
                double queuePressure = queueSize > 0 ? Math.min(queueSize / 10.0, 1.0) : 0.0;
                
                // 綜合壓力分數：線程使用率 * 0.7 + 佇列壓力 * 0.3
                return (threadUtilization * 0.7) + (queuePressure * 0.3);
            })
            .description("Thread pool pressure score combining utilization and queue metrics")
            .tags("pool", poolName, "component", "thread-pool")
            .register(meterRegistry);
        
        // 完成任務數 - KEDA 吞吐量監控
        Gauge.builder("thread_pool_completed_tasks_total", threadPoolExecutor, ThreadPoolExecutor::getCompletedTaskCount)
            .description("Total number of completed tasks")
            .tags("pool", poolName, "component", "thread-pool")
            .register(meterRegistry);
    }
}