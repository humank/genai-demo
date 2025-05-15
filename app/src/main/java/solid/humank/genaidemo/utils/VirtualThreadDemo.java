package solid.humank.genaidemo.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 虛擬線程示例類
 * 展示 Java 21 的 Virtual Threads 功能
 */
public class VirtualThreadDemo {
    
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    /**
     * 使用虛擬線程執行任務
     * 
     * @param taskCount 任務數量
     * @param sleepMillis 每個任務的睡眠時間（毫秒）
     * @return 執行時間（毫秒）
     */
    public static long runWithVirtualThreads(int taskCount, long sleepMillis) {
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        // 模擬工作負載
                        Thread.sleep(sleepMillis);
                        counter.incrementAndGet();
                        return STR."虛擬線程任務完成: \{Thread.currentThread()}";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "任務被中斷";
                    }
                }));
            }
            
            // 等待所有任務完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    System.err.println(STR."任務執行錯誤: \{e.getMessage()}");
                }
            }
        }
        
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
    
    /**
     * 使用平台線程執行任務
     * 
     * @param taskCount 任務數量
     * @param sleepMillis 每個任務的睡眠時間（毫秒）
     * @return 執行時間（毫秒）
     */
    public static long runWithPlatformThreads(int taskCount, long sleepMillis) {
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        // 模擬工作負載
                        Thread.sleep(sleepMillis);
                        counter.incrementAndGet();
                        return STR."平台線程任務完成: \{Thread.currentThread()}";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "任務被中斷";
                    }
                }));
            }
            
            // 等待所有任務完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    System.err.println(STR."任務執行錯誤: \{e.getMessage()}");
                }
            }
        }
        
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
    
    /**
     * 比較虛擬線程和平台線程的性能
     * 
     * @param taskCount 任務數量
     * @param sleepMillis 每個任務的睡眠時間（毫秒）
     * @return 比較結果
     */
    public static String comparePerformance(int taskCount, long sleepMillis) {
        counter.set(0);
        long virtualThreadTime = runWithVirtualThreads(taskCount, sleepMillis);
        
        counter.set(0);
        long platformThreadTime = runWithPlatformThreads(taskCount, sleepMillis);
        
        double speedup = (double) platformThreadTime / virtualThreadTime;
        
        return STR."""
            性能比較結果:
            任務數量: \{taskCount}
            每個任務睡眠時間: \{sleepMillis} 毫秒
            
            虛擬線程執行時間: \{virtualThreadTime} 毫秒
            平台線程執行時間: \{platformThreadTime} 毫秒
            
            虛擬線程加速比: \{String.format("%.2f", speedup)}x
            """;
    }
    
    /**
     * 創建並啟動一個虛擬線程
     * 
     * @param name 線程名稱
     * @param runnable 要執行的任務
     * @return 線程對象
     */
    public static Thread startVirtualThread(String name, Runnable runnable) {
        // 使用 Java 21 的 Thread.Builder API 創建虛擬線程
        Thread thread = Thread.ofVirtual()
                .name(name)
                .uncaughtExceptionHandler((t, e) -> 
                    System.err.println(STR."線程 \{t.getName()} 發生未捕獲異常: \{e.getMessage()}"))
                .start(runnable);
        
        return thread;
    }
}