import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { ObservabilityTraceInterceptor } from './observability-trace.interceptor';
import { SessionService } from '../services/session.service';
import { ObservabilityService } from '../services/observability.service';
import { ApiMonitoringService } from '../services/api-monitoring.service';
import { ObservabilityConfigService } from '../config/observability.config';

describe('ObservabilityTraceInterceptor', () => {
  let client: HttpClient;
  let httpMock: HttpTestingController;
  let observabilityServiceSpy: jasmine.SpyObj<ObservabilityService>;
  let configServiceSpy: jasmine.SpyObj<ObservabilityConfigService>;

  beforeEach(() => {
    const sessionSpy = jasmine.createSpyObj('SessionService', ['generateNewTraceId', 'getSessionId']);
    const obsSpy = jasmine.createSpyObj('ObservabilityService', ['trackUserAction', 'trackPerformanceMetric', 'trackBusinessEvent']);
    const apiMonSpy = jasmine.createSpyObj('ApiMonitoringService', ['recordApiCallStart', 'recordApiCallComplete']);
    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', ['getApiEndpoints']);

    sessionSpy.generateNewTraceId.and.returnValue('trace-123');
    sessionSpy.getSessionId.and.returnValue('session-123');
    configSpy.getApiEndpoints.and.returnValue({
      analytics: '/api/analytics/events',
      performance: '/api/analytics/performance',
      error: '/api/monitoring/events'
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ObservabilityTraceInterceptor,
        { provide: HTTP_INTERCEPTORS, useClass: ObservabilityTraceInterceptor, multi: true },
        { provide: SessionService, useValue: sessionSpy },
        { provide: ObservabilityService, useValue: obsSpy },
        { provide: ApiMonitoringService, useValue: apiMonSpy },
        { provide: ObservabilityConfigService, useValue: configSpy }
      ]
    });

    client = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    observabilityServiceSpy = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
    configServiceSpy = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should track normal API calls', () => {
    client.get('/api/products').subscribe();

    const req = httpMock.expectOne('/api/products');
    req.flush({});

    expect(observabilityServiceSpy.trackUserAction).toHaveBeenCalledWith('api_call_start', jasmine.any(Object));
    expect(observabilityServiceSpy.trackUserAction).toHaveBeenCalledWith('api_call_success', jasmine.any(Object));
  });

  it('should NOT track analytics API calls', () => {
    client.post('/api/analytics/events', {}).subscribe();

    const req = httpMock.expectOne('/api/analytics/events');
    req.flush({});

    expect(observabilityServiceSpy.trackUserAction).not.toHaveBeenCalled();
  });

  it('should NOT track performance API calls', () => {
    client.post('/api/analytics/performance', {}).subscribe();

    const req = httpMock.expectOne('/api/analytics/performance');
    req.flush({});

    expect(observabilityServiceSpy.trackUserAction).not.toHaveBeenCalled();
  });
});