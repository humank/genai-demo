import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ObservabilityConfigService } from '../config/observability.config';
import { ApiService } from './api.service';
import { BatchProcessorService } from './batch-processor.service';
import { ObservabilityEvent } from './observability.service';

describe('BatchProcessorService', () => {
  let service: BatchProcessorService;
  let mockApiService: jasmine.SpyObj<ApiService>;
  let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;

  const createMockEvent = (id: string): ObservabilityEvent => ({
    id,
    type: 'page_view',
    timestamp: Date.now(),
    sessionId: 'session-123',
    traceId: 'trace-123',
    data: { page: '/test' }
  });

  beforeEach(() => {
    const apiSpy = jasmine.createSpyObj('ApiService', ['post']);
    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', ['getApiEndpoints']);

    TestBed.configureTestingModule({
      providers: [
        BatchProcessorService,
        { provide: ApiService, useValue: apiSpy },
        { provide: ObservabilityConfigService, useValue: configSpy }
      ]
    });

    service = TestBed.inject(BatchProcessorService);
    mockApiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;

    // 設定預設的 mock 返回值
    mockConfigService.getApiEndpoints.and.returnValue({
      analytics: '/api/analytics/events',
      performance: '/api/analytics/performance',
      error: '/api/monitoring/events'
    });

    // 清空佇列和統計
    service.clearQueue();
    service.resetStats();
    service.clearOfflineStorage();

    // Mock navigator.onLine
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: true
    });
  });

  afterEach(() => {
    service.destroy();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should add events to queue', () => {
    const event1 = createMockEvent('event-1');
    const event2 = createMockEvent('event-2');

    service.addEvent(event1);
    service.addEvent(event2);

    expect(service.getStatus().queueLength).toBe(2);
  });

  it('should process batch successfully', async () => {
    mockApiService.post.and.returnValue(of({}));

    service.addEvent(createMockEvent('event-1'));
    service.addEvent(createMockEvent('event-2'));

    await service.processBatch();

    expect(mockApiService.post).toHaveBeenCalledWith('/api/analytics/events', jasmine.any(Array));
    expect(service.getStatus().queueLength).toBe(0);
    expect(service.getStatus().totalProcessed).toBe(2);
  });

  it('should handle batch processing failure', async () => {
    mockApiService.post.and.returnValue(throwError(() => ({ status: 500, message: 'Server Error' })));

    service.configure({ retryAttempts: 1 });
    service.addEvent(createMockEvent('event-1'));

    await service.processBatch();

    expect(service.getStatus().totalFailed).toBe(1);
    expect(service.getStatus().queueLength).toBe(1);
  });

  it('should clear queue', () => {
    service.addEvent(createMockEvent('event-1'));
    service.addEvent(createMockEvent('event-2'));

    expect(service.getStatus().queueLength).toBe(2);

    service.clearQueue();

    expect(service.getStatus().queueLength).toBe(0);
  });

  it('should handle offline storage', () => {
    Object.defineProperty(navigator, 'onLine', { value: false });
    window.dispatchEvent(new Event('offline'));

    service.addEvent(createMockEvent('event-1'));

    expect(service.getOfflineStorageSize()).toBe(1);
  });
});