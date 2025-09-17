import { TestBed } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { ApiMonitoringService } from './api-monitoring.service';
import { BatchProcessorService } from './batch-processor.service';
import { ObservabilityService } from './observability.service';
import { WebVitalsService } from './web-vitals.service';

describe('Performance Monitoring Integration', () => {
  let webVitalsService: WebVitalsService;
  let apiMonitoringService: ApiMonitoringService;
  let observabilityService: ObservabilityService;

  beforeEach(() => {
    const mockObservabilityService = jasmine.createSpyObj('ObservabilityService', [
      'trackPerformanceMetric',
      'trackUserAction'
    ]);

    const mockConfigService = jasmine.createSpyObj('ObservabilityConfigService', [
      'isPerformanceMonitoringEnabled',
      'isDebugEnabled',
      'getObservabilityServiceConfig',
      'getBatchConfig',
      'isFeatureEnabled'
    ]);

    const mockBatchProcessor = jasmine.createSpyObj('BatchProcessorService', [
      'configure',
      'addEvent',
      'flush'
    ]);

    // Setup default returns
    mockConfigService.isPerformanceMonitoringEnabled.and.returnValue(true);
    mockConfigService.isDebugEnabled.and.returnValue(false);
    mockConfigService.getObservabilityServiceConfig.and.returnValue({
      enabled: true,
      batchSize: 50,
      flushInterval: 30000,
      apiEndpoint: '/api/analytics/events',
      debug: false
    });
    mockConfigService.getBatchConfig.and.returnValue({
      maxBatchSize: 50,
      maxWaitTime: 30000,
      retryAttempts: 3,
      backoffMultiplier: 2
    });
    mockConfigService.isFeatureEnabled.and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        WebVitalsService,
        ApiMonitoringService,
        { provide: ObservabilityService, useValue: mockObservabilityService },
        { provide: ObservabilityConfigService, useValue: mockConfigService },
        { provide: BatchProcessorService, useValue: mockBatchProcessor }
      ]
    });

    webVitalsService = TestBed.inject(WebVitalsService);
    apiMonitoringService = TestBed.inject(ApiMonitoringService);
    observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
  });

  it('should create all performance monitoring services', () => {
    expect(webVitalsService).toBeTruthy();
    expect(apiMonitoringService).toBeTruthy();
    expect(observabilityService).toBeTruthy();
  });

  it('should provide service status information', () => {
    const webVitalsStatus = webVitalsService.getStatus();
    const apiStats = apiMonitoringService.getCurrentStats();

    expect(webVitalsStatus).toBeDefined();
    expect(webVitalsStatus['isInitialized']).toBeDefined();
    expect(webVitalsStatus['performanceSupported']).toBeDefined();

    expect(apiStats).toBeDefined();
    expect(apiStats.totalApiCalls).toBeDefined();
    expect(apiStats.totalErrors).toBeDefined();
    expect(apiStats.overallErrorRate).toBeDefined();
  });

  it('should handle API monitoring workflow', () => {
    const callId = apiMonitoringService.recordApiCallStart('GET', '/api/test', 'trace-123');
    expect(callId).toBeTruthy();

    apiMonitoringService.recordApiCallComplete(
      callId,
      'GET',
      '/api/test',
      200,
      150,
      'trace-123'
    );

    const stats = apiMonitoringService.getCurrentStats();
    expect(stats.totalApiCalls).toBe(1);
    expect(stats.totalErrors).toBe(0);
  });

  it('should track performance metrics through observability service', () => {
    const callId = apiMonitoringService.recordApiCallStart('POST', '/api/data', 'trace-456');
    
    apiMonitoringService.recordApiCallComplete(
      callId,
      'POST',
      '/api/data',
      200,
      250,
      'trace-456'
    );

    expect(observabilityService.trackPerformanceMetric).toHaveBeenCalledWith({
      type: 'ttfb',
      value: 250,
      page: jasmine.any(String),
      timestamp: jasmine.any(Number)
    });
  });

  it('should handle error scenarios gracefully', () => {
    expect(() => {
      webVitalsService.cleanup();
      apiMonitoringService.reset();
    }).not.toThrow();
  });

  it('should export monitoring data for analysis', () => {
    // Record some test data
    const callId = apiMonitoringService.recordApiCallStart('GET', '/api/export', 'trace-export');
    apiMonitoringService.recordApiCallComplete(callId, 'GET', '/api/export', 200, 100, 'trace-export');

    const exportedData = apiMonitoringService.exportStats();
    
    expect(exportedData.calls).toBeDefined();
    expect(exportedData.endpoints).toBeDefined();
    expect(exportedData.summary).toBeDefined();
    expect(exportedData.calls.length).toBe(1);
  });
});