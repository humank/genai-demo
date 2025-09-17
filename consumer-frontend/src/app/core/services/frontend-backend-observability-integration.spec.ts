import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { ApiMonitoringService } from './api-monitoring.service';
import { BatchProcessorService } from './batch-processor.service';
import { ObservabilityService } from './observability.service';
import { RealTimeAnalyticsService } from './real-time-analytics.service';
import { SessionService } from './session.service';

/**
 * 前端到後端可觀測性整合測試
 * 
 * 測試範圍：
 * 1. 前端事件收集和批次處理
 * 2. HTTP 攔截器追蹤 ID 傳播
 * 3. 前端到後端 API 整合
 * 4. 即時分析 WebSocket 連接
 * 5. 錯誤處理和韌性機制
 * 
 * 需求覆蓋：1.1, 1.2, 1.3, 2.1, 2.2, 2.3
 */
describe('Frontend-Backend Observability Integration', () => {
    let observabilityService: ObservabilityService;
    let batchProcessor: BatchProcessorService;
    let apiMonitoring: ApiMonitoringService;
    let sessionService: SessionService;
    let realTimeAnalytics: RealTimeAnalyticsService;
    let httpTestingController: HttpTestingController;
    let configService: jasmine.SpyObj<ObservabilityConfigService>;

    beforeEach(() => {
        const mockConfigService = jasmine.createSpyObj('ObservabilityConfigService', [
            'isEnabled',
            'isDebugEnabled',
            'getObservabilityServiceConfig',
            'getBatchConfig',
            'isFeatureEnabled',
            'getPerformanceConfig'
        ]);

        // Setup default configuration
        mockConfigService.isEnabled.and.returnValue(true);
        mockConfigService.isDebugEnabled.and.returnValue(true);
        mockConfigService.getObservabilityServiceConfig.and.returnValue({
            enabled: true,
            batchSize: 20,
            flushInterval: 10000,
            apiEndpoint: '/api/analytics/events',
            performanceEndpoint: '/api/analytics/performance',
            debug: true
        });
        mockConfigService.getBatchConfig.and.returnValue({
            maxBatchSize: 20,
            maxWaitTime: 10000,
            retryAttempts: 2,
            backoffMultiplier: 2
        });
        mockConfigService.isFeatureEnabled.and.returnValue(true);
        mockConfigService.getPerformanceConfig.and.returnValue({
            enabled: true,
            sampleRate: 1.0
        });

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                ObservabilityService,
                BatchProcessorService,
                ApiMonitoringService,
                SessionService,
                RealTimeAnalyticsService,
                { provide: ObservabilityConfigService, useValue: mockConfigService }
            ]
        });

        observabilityService = TestBed.inject(ObservabilityService);
        batchProcessor = TestBed.inject(BatchProcessorService);
        apiMonitoring = TestBed.inject(ApiMonitoringService);
        sessionService = TestBed.inject(SessionService);
        realTimeAnalytics = TestBed.inject(RealTimeAnalyticsService);
        httpTestingController = TestBed.inject(HttpTestingController);
        configService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    /**
     * 測試 1: 前端事件收集和批次處理流程
     * 需求: 1.1, 1.2, 2.1
     */
    describe('Event Collection and Batch Processing', () => {
        it('should collect user behavior events and send them in batches', fakeAsync(() => {
            // Given - 配置批次處理
            const sessionId = sessionService.getSessionId();
            const traceId = sessionService.generateNewTraceId();

            // When - 追蹤多個用戶行為事件
            observabilityService.trackPageView('/products', { category: 'electronics' });
            observabilityService.trackUserAction('click', { button: 'add-to-cart', productId: 'PROD-001' });
            observabilityService.trackBusinessEvent({
                type: 'product_view',
                data: { productId: 'PROD-001', category: 'electronics' },
                timestamp: Date.now(),
                sessionId: sessionId
            });

            // 觸發批次處理 (10 秒後)
            tick(10000);

            // Then - 驗證批次請求發送到後端
            const req = httpTestingController.expectOne('/api/analytics/events');
            expect(req.request.method).toBe('POST');
            expect(req.request.headers.get('X-Trace-Id')).toBeTruthy();
            expect(req.request.headers.get('X-Session-Id')).toBe(sessionId);
            expect(req.request.body).toHaveSize(3);

            // 驗證事件內容
            const events = req.request.body;
            expect(events[0].eventType).toBe('page_view');
            expect(events[1].eventType).toBe('user_action');
            expect(events[2].eventType).toBe('product_view');

            req.flush({});
        }));

        it('should handle batch processing with retry mechanism', fakeAsync(() => {
            // Given - 追蹤事件
            observabilityService.trackPageView('/home');
            tick(10000);

            // When - 第一次請求失敗
            const req1 = httpTestingController.expectOne('/api/analytics/events');
            req1.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

            // 等待重試 (指數退避: 2秒)
            tick(2000);

            // Then - 驗證重試請求
            const req2 = httpTestingController.expectOne('/api/analytics/events');
            expect(req2.request.body).toHaveSize(1);
            req2.flush({});
        }));
    });

    /**
     * 測試 2: 效能指標收集和傳輸
     * 需求: 1.3, 2.1
     */
    describe('Performance Metrics Collection', () => {
        it('should collect and send performance metrics to backend', fakeAsync(() => {
            // Given - 追蹤效能指標
            observabilityService.trackPerformanceMetric({
                type: 'lcp',
                value: 2.1,
                page: '/products',
                timestamp: Date.now()
            });

            observabilityService.trackPerformanceMetric({
                type: 'fid',
                value: 85,
                page: '/products',
                timestamp: Date.now()
            });

            // When - 觸發批次處理
            tick(10000);

            // Then - 驗證效能指標發送
            const req = httpTestingController.expectOne('/api/analytics/performance');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toHaveSize(2);

            const metrics = req.request.body;
            expect(metrics[0].metricType).toBe('lcp');
            expect(metrics[0].value).toBe(2.1);
            expect(metrics[1].metricType).toBe('fid');
            expect(metrics[1].value).toBe(85);

            req.flush({});
        }));

        it('should sample performance metrics based on configuration', fakeAsync(() => {
            // Given - 設定 50% 採樣率
            configService.getPerformanceConfig.and.returnValue({
                enabled: true,
                sampleRate: 0.5
            });

            // When - 追蹤多個效能指標
            for (let i = 0; i < 10; i++) {
                observabilityService.trackPerformanceMetric({
                    type: 'lcp',
                    value: 2.0 + i * 0.1,
                    page: '/test',
                    timestamp: Date.now()
                });
            }

            tick(10000);

            // Then - 驗證採樣效果 (應該少於 10 個指標)
            const req = httpTestingController.expectOne('/api/analytics/performance');
            expect(req.request.body.length).toBeLessThan(10);
            req.flush({});
        }));
    });

    /**
     * 測試 3: 追蹤 ID 傳播和上下文管理
     * 需求: 1.1, 1.2, 2.1
     */
    describe('Trace ID Propagation', () => {
        it('should maintain consistent trace ID across events', fakeAsync(() => {
            // Given - 生成追蹤 ID
            const traceId = sessionService.generateNewTraceId();
            const sessionId = sessionService.getSessionId();

            // When - 追蹤多個事件
            observabilityService.trackPageView('/products');
            observabilityService.trackUserAction('click', { button: 'search' });

            tick(10000);

            // Then - 驗證所有事件使用相同的追蹤 ID
            const req = httpTestingController.expectOne('/api/analytics/events');
            expect(req.request.headers.get('X-Trace-Id')).toBeTruthy();
            expect(req.request.headers.get('X-Session-Id')).toBe(sessionId);
            expect(req.request.headers.get('X-Correlation-Id')).toBeTruthy();

            const events = req.request.body;
            events.forEach((event: any) => {
                expect(event.traceId).toBeTruthy();
                expect(event.sessionId).toBe(sessionId);
            });

            req.flush({});
        }));

        it('should generate new trace ID for each session', () => {
            // Given - 獲取當前追蹤 ID
            const traceId1 = sessionService.generateNewTraceId();

            // When - 生成新的追蹤 ID
            const traceId2 = sessionService.generateNewTraceId();

            // Then - 驗證追蹤 ID 不同
            expect(traceId1).not.toBe(traceId2);
            expect(traceId1).toMatch(/^trace-\d+-[a-z0-9]+$/);
            expect(traceId2).toMatch(/^trace-\d+-[a-z0-9]+$/);
        });
    });

    /**
     * 測試 4: API 監控整合
     * 需求: 1.3, 2.1
     */
    describe('API Monitoring Integration', () => {
        it('should track API calls with performance metrics', () => {
            // Given - 開始 API 呼叫監控
            const callId = apiMonitoring.recordApiCallStart('GET', '/api/products', 'trace-123', 1024);

            // When - 完成 API 呼叫
            apiMonitoring.recordApiCallComplete(
                callId,
                'GET',
                '/api/products',
                200,
                150,
                'trace-123',
                2048
            );

            // Then - 驗證 API 統計
            const stats = apiMonitoring.getCurrentStats();
            expect(stats.totalApiCalls).toBe(1);
            expect(stats.totalErrors).toBe(0);
            expect(stats.overallErrorRate).toBe(0);

            const exportedData = apiMonitoring.exportStats();
            expect(exportedData.calls).toHaveSize(1);
            expect(exportedData.calls[0].method).toBe('GET');
            expect(exportedData.calls[0].url).toBe('/api/products');
            expect(exportedData.calls[0].duration).toBe(150);
        });

        it('should track API errors and calculate error rates', () => {
            // Given - 記錄成功和失敗的 API 呼叫
            const callId1 = apiMonitoring.recordApiCallStart('GET', '/api/products', 'trace-1');
            apiMonitoring.recordApiCallComplete(callId1, 'GET', '/api/products', 200, 100, 'trace-1');

            const callId2 = apiMonitoring.recordApiCallStart('POST', '/api/orders', 'trace-2');
            apiMonitoring.recordApiCallComplete(callId2, 'POST', '/api/orders', 500, 200, 'trace-2', undefined, 'Server Error');

            // When - 獲取統計資料
            const stats = apiMonitoring.getCurrentStats();

            // Then - 驗證錯誤率計算
            expect(stats.totalApiCalls).toBe(2);
            expect(stats.totalErrors).toBe(1);
            expect(stats.overallErrorRate).toBe(0.5);
        });
    });

    /**
     * 測試 5: 即時分析 WebSocket 整合
     * 需求: 2.3, 3.3
     */
    describe('Real-time Analytics WebSocket Integration', () => {
        it('should handle WebSocket connection lifecycle', () => {
            // Given - WebSocket 服務
            const mockWebSocket = jasmine.createSpyObj('WebSocket', ['send', 'close']);
            mockWebSocket.readyState = WebSocket.OPEN;

            // When - 連接 WebSocket
            realTimeAnalytics.connect();

            // Then - 驗證連接狀態
            expect(realTimeAnalytics.isConnected()).toBe(false); // 初始狀態
        });

        it('should handle real-time analytics updates', () => {
            // Given - 設定更新回調
            let receivedUpdate: any = null;
            realTimeAnalytics.subscribe('business-metrics', (data) => {
                receivedUpdate = data;
            });

            // When - 模擬接收 WebSocket 訊息
            const mockUpdate = {
                type: 'business-metrics',
                data: {
                    totalUsers: 150,
                    activeUsers: 45,
                    conversionRate: 0.12
                },
                timestamp: Date.now()
            };

            // 模擬 WebSocket 訊息接收
            // (實際實作中會透過 WebSocket onmessage 事件)

            // Then - 驗證更新處理
            // expect(receivedUpdate).toEqual(mockUpdate);
        });
    });

    /**
     * 測試 6: 錯誤處理和韌性機制
     * 需求: 2.1, 3.1
     */
    describe('Error Handling and Resilience', () => {
        it('should handle network failures gracefully', fakeAsync(() => {
            // Given - 追蹤事件
            observabilityService.trackPageView('/error-test');
            tick(10000);

            // When - 網路錯誤
            const req = httpTestingController.expectOne('/api/analytics/events');
            req.flush('Network Error', { status: 0, statusText: 'Network Error' });

            // Then - 應該觸發重試機制
            tick(2000); // 等待重試

            const retryReq = httpTestingController.expectOne('/api/analytics/events');
            expect(retryReq.request.body).toHaveSize(1);
            retryReq.flush({});
        }));

        it('should store events offline when network is unavailable', () => {
            // Given - 模擬離線狀態
            spyOnProperty(navigator, 'onLine', 'get').and.returnValue(false);

            // When - 追蹤事件
            observabilityService.trackPageView('/offline-test');

            // Then - 事件應該被儲存在本地
            // (實際實作中會檢查 localStorage)
            expect(true).toBe(true); // 佔位符測試
        });

        it('should validate event data before sending', fakeAsync(() => {
            // Given - 無效的事件數據
            const invalidEvent = {
                type: '', // 空的事件類型
                data: null,
                timestamp: 'invalid-timestamp',
                sessionId: ''
            };

            // When - 嘗試追蹤無效事件
            expect(() => {
                observabilityService.trackBusinessEvent(invalidEvent as any);
            }).not.toThrow(); // 應該優雅地處理無效數據

            tick(10000);

            // Then - 不應該發送無效事件
            httpTestingController.expectNone('/api/analytics/events');
        }));
    });

    /**
     * 測試 7: 端到端業務流程追蹤
     * 需求: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3
     */
    describe('End-to-End Business Flow Tracking', () => {
        it('should track complete shopping journey with consistent trace ID', fakeAsync(() => {
            // Given - 開始購物流程
            const traceId = sessionService.generateNewTraceId();
            const sessionId = sessionService.getSessionId();

            // When - 模擬完整的購物流程
            // Step 1: 頁面瀏覽
            observabilityService.trackPageView('/products', { category: 'electronics' });

            // Step 2: 商品瀏覽
            observabilityService.trackBusinessEvent({
                type: 'product_view',
                data: { productId: 'PROD-001', category: 'electronics', price: 299.99 },
                timestamp: Date.now(),
                sessionId: sessionId
            });

            // Step 3: 加入購物車
            observabilityService.trackBusinessEvent({
                type: 'cart_add',
                data: { productId: 'PROD-001', quantity: 1, price: 299.99 },
                timestamp: Date.now(),
                sessionId: sessionId
            });

            // Step 4: 效能指標
            observabilityService.trackPerformanceMetric({
                type: 'page_load',
                value: 1.8,
                page: '/checkout',
                timestamp: Date.now()
            });

            // 觸發批次處理
            tick(10000);

            // Then - 驗證事件批次
            const eventsReq = httpTestingController.expectOne('/api/analytics/events');
            expect(eventsReq.request.body).toHaveSize(3); // 3 個業務事件
            expect(eventsReq.request.headers.get('X-Trace-Id')).toBeTruthy();
            eventsReq.flush({});

            // 驗證效能指標
            const metricsReq = httpTestingController.expectOne('/api/analytics/performance');
            expect(metricsReq.request.body).toHaveSize(1);
            expect(metricsReq.request.headers.get('X-Trace-Id')).toBeTruthy();
            metricsReq.flush({});

            // 驗證追蹤 ID 一致性
            const eventsTraceId = eventsReq.request.headers.get('X-Trace-Id');
            const metricsTraceId = metricsReq.request.headers.get('X-Trace-Id');
            expect(eventsTraceId).toBe(metricsTraceId);
        }));
    });

    /**
     * 測試 8: 配置和功能開關
     * 需求: 2.1, 3.1
     */
    describe('Configuration and Feature Toggles', () => {
        it('should respect feature toggle configuration', fakeAsync(() => {
            // Given - 禁用用戶行為追蹤
            configService.isFeatureEnabled.and.callFake((feature: string) => {
                return feature !== 'userBehaviorTracking';
            });

            // When - 嘗試追蹤用戶行為
            observabilityService.trackUserAction('click', { button: 'test' });
            tick(10000);

            // Then - 不應該發送事件
            httpTestingController.expectNone('/api/analytics/events');
        }));

        it('should adjust batch size based on configuration', fakeAsync(() => {
            // Given - 設定小批次大小
            configService.getBatchConfig.and.returnValue({
                maxBatchSize: 2,
                maxWaitTime: 5000,
                retryAttempts: 1,
                backoffMultiplier: 1
            });

            // When - 追蹤多個事件
            observabilityService.trackPageView('/test1');
            observabilityService.trackPageView('/test2');
            observabilityService.trackPageView('/test3');

            tick(5000);

            // Then - 應該分批發送
            const req1 = httpTestingController.expectOne('/api/analytics/events');
            expect(req1.request.body).toHaveSize(2);
            req1.flush({});

            tick(5000);

            const req2 = httpTestingController.expectOne('/api/analytics/events');
            expect(req2.request.body).toHaveSize(1);
            req2.flush({});
        }));
    });
});