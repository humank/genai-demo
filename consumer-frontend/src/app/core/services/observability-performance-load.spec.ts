import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { BatchProcessorService } from './batch-processor.service';
import { ObservabilityService } from './observability.service';
import { SessionService } from './session.service';

/**
 * 前端可觀測性效能和負載測試
 * 
 * 測試範圍：
 * 1. 前端批次處理效能驗證
 * 2. 大量事件收集和處理能力
 * 3. 記憶體使用和性能影響
 * 4. 並發事件處理能力
 * 5. 離線儲存和同步效能
 * 
 * 需求覆蓋：3.1, 3.2, 3.3
 * 
 * 效能目標：
 * - 批次處理：50 個事件 < 100ms
 * - 事件收集：1000 個事件 < 500ms
 * - 記憶體增長：< 10MB
 * - 並發處理：100 個並發事件 < 200ms
 */
describe('Observability Performance and Load Tests', () => {
    let observabilityService: ObservabilityService;
    let batchProcessor: BatchProcessorService;
    let sessionService: SessionService;
    let httpTestingController: HttpTestingController;
    let configService: jasmine.SpyObj<ObservabilityConfigService>;

    // 效能基準常數
    const BATCH_SIZE_TARGET = 50;
    const BATCH_PROCESSING_TIME_LIMIT_MS = 100;
    const LARGE_EVENT_COUNT = 1000;
    const LARGE_EVENT_TIME_LIMIT_MS = 500;
    const CONCURRENT_EVENTS = 100;
    const CONCURRENT_TIME_LIMIT_MS = 200;
    const MEMORY_GROWTH_LIMIT_MB = 10;

    beforeEach(() => {
        const mockConfigService = jasmine.createSpyObj('ObservabilityConfigService', [
            'isEnabled',
            'isDebugEnabled',
            'getObservabilityServiceConfig',
            'getBatchConfig',
            'isFeatureEnabled',
            'getPerformanceConfig'
        ]);

        // 設定高效能配置
        mockConfigService.isEnabled.and.returnValue(true);
        mockConfigService.isDebugEnabled.and.returnValue(false);
        mockConfigService.getObservabilityServiceConfig.and.returnValue({
            enabled: true,
            batchSize: 50,
            flushInterval: 5000, // 較短的刷新間隔用於測試
            apiEndpoint: '/api/analytics/events',
            performanceEndpoint: '/api/analytics/performance',
            debug: false
        });
        mockConfigService.getBatchConfig.and.returnValue({
            maxBatchSize: 50,
            maxWaitTime: 5000,
            retryAttempts: 1, // 減少重試以加快測試
            backoffMultiplier: 1,
            enableOfflineStorage: true,
            storageKey: 'test-storage',
            maxStorageSize: 1000
        });
        mockConfigService.isFeatureEnabled.and.returnValue(true);
        mockConfigService.getObservabilityServiceConfig.and.returnValue({
            enabled: true,
            batchSize: 50,
            flushInterval: 5000,
            apiEndpoint: '/api/analytics/events',
            debug: false
        });

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                ObservabilityService,
                BatchProcessorService,
                SessionService,
                { provide: ObservabilityConfigService, useValue: mockConfigService }
            ]
        });

        observabilityService = TestBed.inject(ObservabilityService);
        batchProcessor = TestBed.inject(BatchProcessorService);
        sessionService = TestBed.inject(SessionService);
        httpTestingController = TestBed.inject(HttpTestingController);
        configService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    /**
     * 測試 1: 批次處理效能驗證
     * 需求: 3.1 - 驗證批次處理效能目標
     */
    describe('Batch Processing Performance', () => {
        it('should meet batch processing performance targets', fakeAsync(() => {
            // Given - 標準批次大小的事件
            const startTime = performance.now();

            // When - 快速追蹤多個事件
            for (let i = 0; i < BATCH_SIZE_TARGET; i++) {
                observabilityService.trackPageView(`/test-page-${i}`, { index: i });
            }

            const collectionTime = performance.now() - startTime;

            // 觸發批次處理
            tick(5000);

            // Then - 驗證收集效能
            expect(collectionTime).toBeLessThan(BATCH_PROCESSING_TIME_LIMIT_MS);

            // 驗證批次發送
            const req = httpTestingController.expectOne('/api/analytics/events');
            expect(req.request.body).toHaveSize(BATCH_SIZE_TARGET);
            req.flush({});

            console.log(`批次處理效能: ${BATCH_SIZE_TARGET} 個事件收集時間 ${collectionTime.toFixed(2)} ms`);
        }));

        it('should handle rapid event collection efficiently', () => {
            // Given - 快速事件收集測試
            const events: any[] = [];
            const startTime = performance.now();

            // When - 快速收集大量事件
            for (let i = 0; i < LARGE_EVENT_COUNT; i++) {
                const event = {
                    type: 'search' as const,
                    data: { index: i, timestamp: Date.now() },
                    timestamp: Date.now(),
                    sessionId: sessionService.getSessionId()
                };
                events.push(event);
                observabilityService.trackBusinessEvent(event);
            }

            const collectionTime = performance.now() - startTime;

            // Then - 驗證大量事件收集效能
            expect(collectionTime).toBeLessThan(LARGE_EVENT_TIME_LIMIT_MS);
            expect(events.length).toBe(LARGE_EVENT_COUNT);

            console.log(`大量事件收集效能: ${LARGE_EVENT_COUNT} 個事件收集時間 ${collectionTime.toFixed(2)} ms`);
        });
    });

    /**
     * 測試 2: 並發事件處理能力
     * 需求: 3.1, 3.2 - 測試並發事件處理能力
     */
    describe('Concurrent Event Processing', () => {
        it('should handle concurrent events efficiently', fakeAsync(() => {
            // Given - 並發事件處理
            const startTime = performance.now();
            const promises: Promise<void>[] = [];

            // When - 並發追蹤事件
            for (let i = 0; i < CONCURRENT_EVENTS; i++) {
                const promise = new Promise<void>((resolve) => {
                    setTimeout(() => {
                        observabilityService.trackUserAction('concurrent_test', {
                            index: i,
                            timestamp: Date.now()
                        });
                        resolve();
                    }, Math.random() * 10); // 隨機延遲模擬並發
                });
                promises.push(promise);
            }

            // 等待所有並發事件完成
            Promise.all(promises).then(() => {
                const processingTime = performance.now() - startTime;
                expect(processingTime).toBeLessThan(CONCURRENT_TIME_LIMIT_MS);
                console.log(`並發事件處理: ${CONCURRENT_EVENTS} 個並發事件處理時間 ${processingTime.toFixed(2)} ms`);
            });

            // 觸發批次處理
            tick(5000);

            // Then - 驗證所有事件都被處理
            const req = httpTestingController.expectOne('/api/analytics/events');
            expect(req.request.body.length).toBeGreaterThan(0);
            req.flush({});
        }));
    });

    /**
     * 測試 3: 記憶體使用效能測試
     * 需求: 3.1, 3.2 - 驗證記憶體使用效能
     */
    describe('Memory Usage Performance', () => {
        it('should maintain reasonable memory usage during heavy processing', fakeAsync(() => {
            // Given - 記錄初始記憶體 (模擬)
            const initialMemoryUsage = (performance as any).memory?.usedJSHeapSize || 0;

            // When - 處理大量事件
            for (let batch = 0; batch < 10; batch++) {
                for (let i = 0; i < 100; i++) {
                    observabilityService.trackPageView(`/memory-test-${batch}-${i}`, {
                        batch: batch,
                        index: i,
                        largeData: 'x'.repeat(1000) // 模擬較大的數據
                    });
                }

                // 每批次後觸發處理
                tick(5000);

                const req = httpTestingController.expectOne('/api/analytics/events');
                req.flush({});
            }

            // Then - 檢查記憶體使用 (如果支援)
            if ((performance as any).memory) {
                const finalMemoryUsage = (performance as any).memory.usedJSHeapSize;
                const memoryGrowthMB = (finalMemoryUsage - initialMemoryUsage) / (1024 * 1024);

                expect(memoryGrowthMB).toBeLessThan(MEMORY_GROWTH_LIMIT_MB);
                console.log(`記憶體使用: 增長 ${memoryGrowthMB.toFixed(2)} MB`);
            } else {
                console.log('記憶體測試: 瀏覽器不支援 performance.memory API');
            }
        }));
    });

    /**
     * 測試 4: 效能指標收集效能
     * 需求: 3.1, 3.2 - 測試效能指標收集效能
     */
    describe('Performance Metrics Collection', () => {
        it('should collect performance metrics efficiently', fakeAsync(() => {
            // Given - 大量效能指標
            const metricsCount = 500;
            const startTime = performance.now();

            // When - 快速收集效能指標
            for (let i = 0; i < metricsCount; i++) {
                observabilityService.trackPerformanceMetric({
                    type: 'lcp',
                    value: Math.random() * 3000,
                    page: `/perf-test-${i}`,
                    timestamp: Date.now()
                });
            }

            const collectionTime = performance.now() - startTime;

            // 觸發批次處理
            tick(5000);

            // Then - 驗證效能指標收集效能
            expect(collectionTime).toBeLessThan(200); // 500 個指標應在 200ms 內收集完成

            const req = httpTestingController.expectOne('/api/analytics/performance');
            expect(req.request.body.length).toBeGreaterThan(0);
            req.flush({});

            console.log(`效能指標收集: ${metricsCount} 個指標收集時間 ${collectionTime.toFixed(2)} ms`);
        }));

        it('should handle mixed event types efficiently', fakeAsync(() => {
            // Given - 混合事件類型
            const totalEvents = 300;
            const startTime = performance.now();

            // When - 混合追蹤不同類型的事件
            for (let i = 0; i < totalEvents; i++) {
                if (i % 3 === 0) {
                    observabilityService.trackPageView(`/mixed-test-${i}`);
                } else if (i % 3 === 1) {
                    observabilityService.trackUserAction('click', { button: `btn-${i}` });
                } else {
                    observabilityService.trackPerformanceMetric({
                        type: 'fid',
                        value: Math.random() * 100,
                        page: `/mixed-test-${i}`,
                        timestamp: Date.now()
                    });
                }
            }

            const collectionTime = performance.now() - startTime;

            // 觸發批次處理
            tick(5000);

            // Then - 驗證混合事件處理效能
            expect(collectionTime).toBeLessThan(150); // 300 個混合事件應在 150ms 內收集完成

            // 應該有兩個請求：事件和效能指標
            const eventsReq = httpTestingController.expectOne('/api/analytics/events');
            const metricsReq = httpTestingController.expectOne('/api/analytics/performance');

            expect(eventsReq.request.body.length + metricsReq.request.body.length).toBeGreaterThan(0);

            eventsReq.flush({});
            metricsReq.flush({});

            console.log(`混合事件處理: ${totalEvents} 個混合事件收集時間 ${collectionTime.toFixed(2)} ms`);
        }));
    });

    /**
     * 測試 5: 批次配置優化測試
     * 需求: 3.1, 3.2 - 測試不同批次配置的效能影響
     */
    describe('Batch Configuration Optimization', () => {
        it('should optimize performance with different batch sizes', fakeAsync(() => {
            // Given - 測試不同批次大小的效能
            const batchSizes = [10, 25, 50, 100];
            const results: { size: number; time: number }[] = [];

            for (const batchSize of batchSizes) {
                // 重新配置批次大小
                configService.getBatchConfig.and.returnValue({
                    maxBatchSize: batchSize,
                    maxWaitTime: 1000,
                    retryAttempts: 1,
                    backoffMultiplier: 1,
                    enableOfflineStorage: true,
                    storageKey: 'test-storage',
                    maxStorageSize: 1000
                });

                const startTime = performance.now();

                // When - 追蹤事件直到達到批次大小
                for (let i = 0; i < batchSize; i++) {
                    observabilityService.trackPageView(`/batch-opt-${batchSize}-${i}`);
                }

                // 觸發批次處理
                tick(1000);

                const processingTime = performance.now() - startTime;
                results.push({ size: batchSize, time: processingTime });

                // 處理 HTTP 請求
                const req = httpTestingController.expectOne('/api/analytics/events');
                expect(req.request.body).toHaveSize(batchSize);
                req.flush({});
            }

            // Then - 分析不同批次大小的效能
            results.forEach(result => {
                console.log(`批次大小 ${result.size}: 處理時間 ${result.time.toFixed(2)} ms`);
            });

            // 驗證較大批次通常更有效率 (每個事件的平均時間更少)
            const smallBatchEfficiency = results[0].time / results[0].size;
            const largeBatchEfficiency = results[results.length - 1].time / results[results.length - 1].size;

            expect(largeBatchEfficiency).toBeLessThanOrEqual(smallBatchEfficiency * 1.5); // 允許一些變化
        }));
    });

    /**
     * 測試 6: 壓力測試 - 持續高負載
     * 需求: 3.1, 3.2, 3.3 - 前端壓力測試
     */
    describe('Stress Testing', () => {
        it('should maintain performance under sustained load', fakeAsync(() => {
            // Given - 持續負載測試
            const durationMs = 5000; // 5 秒測試
            const eventsPerSecond = 20;
            const totalEvents = (durationMs / 1000) * eventsPerSecond;
            let eventCount = 0;
            const responseTimes: number[] = [];

            // When - 持續發送事件
            const interval = setInterval(() => {
                const startTime = performance.now();

                for (let i = 0; i < eventsPerSecond; i++) {
                    observabilityService.trackUserAction('stress_test', {
                        eventNumber: eventCount++,
                        timestamp: Date.now()
                    });
                }

                const responseTime = performance.now() - startTime;
                responseTimes.push(responseTime);
            }, 1000);

            // 運行測試
            tick(durationMs);
            clearInterval(interval);

            // 觸發最終批次處理
            tick(5000);

            // Then - 分析壓力測試結果
            const averageResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
            const maxResponseTime = Math.max(...responseTimes);

            expect(averageResponseTime).toBeLessThan(50); // 平均響應時間應在 50ms 以內
            expect(maxResponseTime).toBeLessThan(100); // 最大響應時間應在 100ms 以內
            expect(eventCount).toBeGreaterThanOrEqual(totalEvents * 0.9); // 至少處理 90% 的事件

            // 處理所有 HTTP 請求
            while (true) {
                try {
                    const req = httpTestingController.expectOne('/api/analytics/events');
                    req.flush({});
                } catch {
                    break; // 沒有更多請求
                }
            }

            console.log(`壓力測試結果: 處理 ${eventCount} 個事件, 平均響應時間 ${averageResponseTime.toFixed(2)} ms, 最大響應時間 ${maxResponseTime.toFixed(2)} ms`);
        }));
    });

    /**
     * 測試 7: 離線儲存效能測試
     * 需求: 3.1, 3.2 - 測試離線儲存和同步效能
     */
    describe('Offline Storage Performance', () => {
        it('should handle offline storage efficiently', () => {
            // Given - 模擬離線狀態
            spyOnProperty(navigator, 'onLine', 'get').and.returnValue(false);
            const eventCount = 200;
            const startTime = performance.now();

            // When - 在離線狀態下追蹤事件
            for (let i = 0; i < eventCount; i++) {
                observabilityService.trackPageView(`/offline-test-${i}`, {
                    index: i,
                    offline: true
                });
            }

            const storageTime = performance.now() - startTime;

            // Then - 驗證離線儲存效能
            expect(storageTime).toBeLessThan(100); // 200 個事件的離線儲存應在 100ms 內完成

            console.log(`離線儲存效能: ${eventCount} 個事件儲存時間 ${storageTime.toFixed(2)} ms`);

            // 不應該有 HTTP 請求 (因為離線)
            httpTestingController.expectNone('/api/analytics/events');
        });
    });
});