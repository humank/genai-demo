import { TestBed } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { BatchProcessorService } from './batch-processor.service';
import { ObservabilityService } from './observability.service';

describe('ObservabilityService', () => {
  let service: ObservabilityService;
  let mockBatchProcessor: jasmine.SpyObj<BatchProcessorService>;
  let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;

  beforeEach(() => {
    const batchProcessorSpy = jasmine.createSpyObj('BatchProcessorService', ['addEvent', 'flush', 'configure']);
    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', ['getObservabilityServiceConfig', 'getBatchConfig', 'isFeatureEnabled', 'isPerformanceMonitoringEnabled']);

    // 設定預設的 mock 返回值
    configSpy.getObservabilityServiceConfig.and.returnValue({
      enabled: true,
      batchSize: 50,
      flushInterval: 30000,
      apiEndpoint: '/api/analytics/events',
      debug: false
    });
    configSpy.getBatchConfig.and.returnValue({
      maxBatchSize: 50,
      maxWaitTime: 30000,
      retryAttempts: 3,
      backoffMultiplier: 2,
      enableOfflineStorage: true,
      storageKey: 'observability_offline_events',
      maxStorageSize: 1000
    });
    configSpy.isFeatureEnabled.and.returnValue(true);
    configSpy.isPerformanceMonitoringEnabled.and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        { provide: BatchProcessorService, useValue: batchProcessorSpy },
        { provide: ObservabilityConfigService, useValue: configSpy }
      ]
    });
    
    service = TestBed.inject(ObservabilityService);
    mockBatchProcessor = TestBed.inject(BatchProcessorService) as jasmine.SpyObj<BatchProcessorService>;
    mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have session ID', () => {
    expect(service.getSessionId()).toBeTruthy();
  });

  it('should have trace ID', () => {
    expect(service.getTraceId()).toBeTruthy();
  });

  it('should track page view when enabled', () => {
    service.trackPageView('/test');
    
    expect(mockBatchProcessor.addEvent).toHaveBeenCalled();
  });

  it('should not track when feature is disabled', () => {
    mockConfigService.isFeatureEnabled.and.returnValue(false);
    
    service.trackPageView('/test');
    
    expect(mockBatchProcessor.addEvent).not.toHaveBeenCalled();
  });
});