import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiMonitoringService } from '../services/api-monitoring.service';
import { ObservabilityService } from '../services/observability.service';
import { SessionService } from '../services/session.service';
import { ObservabilityTraceInterceptor } from './observability-trace.interceptor';

describe('ObservabilityTraceInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
  let mockSessionService: jasmine.SpyObj<SessionService>;
  let mockApiMonitoringService: jasmine.SpyObj<ApiMonitoringService>;

  beforeEach(() => {
    const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
      'trackUserAction',
      'trackPerformanceMetric',
      'trackBusinessEvent'
    ]);

    const sessionSpy = jasmine.createSpyObj('SessionService', [
      'generateNewTraceId',
      'getSessionId'
    ]);

    const apiMonitoringSpy = jasmine.createSpyObj('ApiMonitoringService', [
      'recordApiCallStart',
      'recordApiCallComplete'
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        {
          provide: HTTP_INTERCEPTORS,
          useClass: ObservabilityTraceInterceptor,
          multi: true
        },
        { provide: ObservabilityService, useValue: observabilitySpy },
        { provide: SessionService, useValue: sessionSpy },
        { provide: ApiMonitoringService, useValue: apiMonitoringSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
    mockSessionService = TestBed.inject(SessionService) as jasmine.SpyObj<SessionService>;
    mockApiMonitoringService = TestBed.inject(ApiMonitoringService) as jasmine.SpyObj<ApiMonitoringService>;

    // Setup default mock returns
    mockSessionService.generateNewTraceId.and.returnValue('test-trace-id');
    mockSessionService.getSessionId.and.returnValue('test-session-id');
    mockApiMonitoringService.recordApiCallStart.and.returnValue('test-api-call-id');
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Request Tracing', () => {
    it('should add tracing headers to requests', () => {
      httpClient.get('/api/test').subscribe();

      const req = httpTestingController.expectOne('/api/test');
      
      expect(req.request.headers.get('X-Trace-Id')).toBe('test-trace-id');
      expect(req.request.headers.get('X-Session-Id')).toBe('test-session-id');
      expect(req.request.headers.get('X-Correlation-Id')).toBe('test-trace-id');
      expect(req.request.headers.get('X-Request-Id')).toMatch(/^req-\d+-[a-z0-9]+$/);
      expect(req.request.headers.get('X-User-Agent')).toBeTruthy();
      expect(req.request.headers.get('X-Timestamp')).toBeTruthy();
      expect(req.request.headers.get('X-Api-Call-Id')).toBe('test-api-call-id');

      req.flush({});
    });

    it('should generate new trace ID for each request', () => {
      mockSessionService.generateNewTraceId.and.returnValues('trace-1', 'trace-2');

      httpClient.get('/api/test1').subscribe();
      httpClient.get('/api/test2').subscribe();

      const req1 = httpTestingController.expectOne('/api/test1');
      const req2 = httpTestingController.expectOne('/api/test2');

      expect(req1.request.headers.get('X-Trace-Id')).toBe('trace-1');
      expect(req2.request.headers.get('X-Trace-Id')).toBe('trace-2');

      req1.flush({});
      req2.flush({});
    });
  });

  describe('API Monitoring Integration', () => {
    it('should record API call start with monitoring service', () => {
      httpClient.post('/api/orders', { productId: 123 }).subscribe();

      expect(mockApiMonitoringService.recordApiCallStart).toHaveBeenCalledWith(
        'POST',
        '/api/orders',
        'test-trace-id',
        jasmine.any(Number) // request size
      );

      const req = httpTestingController.expectOne('/api/orders');
      req.flush({});
    });

    it('should record successful API call completion', () => {
      httpClient.get('/api/products').subscribe();

      const req = httpTestingController.expectOne('/api/products');
      req.flush({ products: [] }, { 
        status: 200, 
        statusText: 'OK',
        headers: { 'Content-Length': '100' }
      });

      expect(mockApiMonitoringService.recordApiCallComplete).toHaveBeenCalledWith(
        'test-api-call-id',
        'GET',
        '/api/products',
        200,
        jasmine.any(Number), // duration
        'test-trace-id',
        jasmine.any(Number) // response size
      );
    });

    it('should record failed API call completion', () => {
      httpClient.get('/api/error').subscribe({
        error: () => {} // Handle error to prevent test failure
      });

      const req = httpTestingController.expectOne('/api/error');
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(mockApiMonitoringService.recordApiCallComplete).toHaveBeenCalledWith(
        'test-api-call-id',
        'GET',
        '/api/error',
        500,
        jasmine.any(Number), // duration
        'test-trace-id',
        undefined, // no response size for errors
        jasmine.any(String) // error message
      );
    });
  });

  describe('Observability Event Tracking', () => {
    it('should track API call start event', () => {
      httpClient.get('/api/test').subscribe();

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_call_start',
        jasmine.objectContaining({
          method: 'GET',
          url: '/api/test',
          traceId: 'test-trace-id',
          hasBody: false
        })
      );

      const req = httpTestingController.expectOne('/api/test');
      req.flush({});
    });

    it('should track successful API call', () => {
      httpClient.get('/api/success').subscribe();

      const req = httpTestingController.expectOne('/api/success');
      req.flush({ data: 'test' }, { status: 200, statusText: 'OK' });

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_call_success',
        jasmine.objectContaining({
          method: 'GET',
          url: '/api/success',
          status: 200,
          statusText: 'OK',
          traceId: 'test-trace-id'
        })
      );
    });

    it('should track failed API call', () => {
      httpClient.get('/api/failure').subscribe({
        error: () => {} // Handle error
      });

      const req = httpTestingController.expectOne('/api/failure');
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_call_error',
        jasmine.objectContaining({
          method: 'GET',
          url: '/api/failure',
          status: 404,
          statusText: 'Not Found',
          traceId: 'test-trace-id',
          errorType: 'client_error'
        })
      );
    });

    it('should track performance metrics for API calls', () => {
      httpClient.get('/api/performance').subscribe();

      const req = httpTestingController.expectOne('/api/performance');
      req.flush({});

      expect(mockObservabilityService.trackPerformanceMetric).toHaveBeenCalledWith({
        type: 'ttfb',
        value: jasmine.any(Number),
        page: jasmine.any(String),
        timestamp: jasmine.any(Number)
      });
    });
  });

  describe('Business Event Tracking', () => {
    it('should track product view business event', () => {
      httpClient.get('/api/consumer/products/123').subscribe();

      const req = httpTestingController.expectOne('/api/consumer/products/123');
      req.flush({ id: 123, name: 'Test Product' });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'product_view',
        data: jasmine.objectContaining({
          productId: '123',
          traceId: 'test-trace-id'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });

    it('should track search business event', () => {
      httpClient.get('/api/consumer/products/search?q=laptop').subscribe();

      const req = httpTestingController.expectOne('/api/consumer/products/search?q=laptop');
      req.flush({ results: [] });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'search',
        data: jasmine.objectContaining({
          traceId: 'test-trace-id'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });

    it('should track cart add business event', () => {
      const cartData = { productId: 123, quantity: 2 };
      httpClient.post('/api/consumer/cart', cartData).subscribe();

      const req = httpTestingController.expectOne('/api/consumer/cart');
      req.flush({ success: true });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'cart_add',
        data: jasmine.objectContaining({
          cartData: cartData,
          traceId: 'test-trace-id'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });

    it('should track purchase complete business event', () => {
      const orderData = { items: [{ productId: 123, quantity: 1 }], total: 99.99 };
      httpClient.post('/api/orders', orderData).subscribe();

      const req = httpTestingController.expectOne('/api/orders');
      req.flush({ orderId: 'order-123' });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'purchase_complete',
        data: jasmine.objectContaining({
          orderData: orderData,
          traceId: 'test-trace-id'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', () => {
      httpClient.get('/api/network-error').subscribe({
        error: () => {} // Handle error
      });

      const req = httpTestingController.expectOne('/api/network-error');
      req.error(new ErrorEvent('Network error'), { status: 0 });

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'network_error',
        jasmine.objectContaining({
          url: '/api/network-error',
          traceId: 'test-trace-id'
        })
      );
    });

    it('should categorize client errors correctly', () => {
      httpClient.get('/api/client-error').subscribe({
        error: () => {} // Handle error
      });

      const req = httpTestingController.expectOne('/api/client-error');
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_call_error',
        jasmine.objectContaining({
          errorType: 'client_error'
        })
      );
    });

    it('should categorize server errors correctly', () => {
      httpClient.get('/api/server-error').subscribe({
        error: () => {} // Handle error
      });

      const req = httpTestingController.expectOne('/api/server-error');
      req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_call_error',
        jasmine.objectContaining({
          errorType: 'server_error'
        })
      );
    });
  });

  describe('Request Size Calculation', () => {
    it('should calculate request size for JSON body', () => {
      const requestBody = { name: 'Test Product', price: 99.99 };
      httpClient.post('/api/products', requestBody).subscribe();

      expect(mockApiMonitoringService.recordApiCallStart).toHaveBeenCalledWith(
        'POST',
        '/api/products',
        'test-trace-id',
        JSON.stringify(requestBody).length
      );

      const req = httpTestingController.expectOne('/api/products');
      req.flush({});
    });

    it('should handle string request body', () => {
      const requestBody = 'test string data';
      httpClient.post('/api/text', requestBody).subscribe();

      expect(mockApiMonitoringService.recordApiCallStart).toHaveBeenCalledWith(
        'POST',
        '/api/text',
        'test-trace-id',
        requestBody.length
      );

      const req = httpTestingController.expectOne('/api/text');
      req.flush({});
    });

    it('should handle FormData request body', () => {
      const formData = new FormData();
      formData.append('file', new Blob(['test']), 'test.txt');
      
      httpClient.post('/api/upload', formData).subscribe();

      expect(mockApiMonitoringService.recordApiCallStart).toHaveBeenCalledWith(
        'POST',
        '/api/upload',
        'test-trace-id',
        1024 // FormData estimated size
      );

      const req = httpTestingController.expectOne('/api/upload');
      req.flush({});
    });

    it('should handle requests without body', () => {
      httpClient.get('/api/no-body').subscribe();

      expect(mockApiMonitoringService.recordApiCallStart).toHaveBeenCalledWith(
        'GET',
        '/api/no-body',
        'test-trace-id',
        0
      );

      const req = httpTestingController.expectOne('/api/no-body');
      req.flush({});
    });
  });

  describe('Response Size Calculation', () => {
    it('should calculate response size from Content-Length header', () => {
      httpClient.get('/api/with-content-length').subscribe();

      const req = httpTestingController.expectOne('/api/with-content-length');
      req.flush({ data: 'test' }, { 
        headers: { 'Content-Length': '500' }
      });

      expect(mockApiMonitoringService.recordApiCallComplete).toHaveBeenCalledWith(
        'test-api-call-id',
        'GET',
        '/api/with-content-length',
        200,
        jasmine.any(Number),
        'test-trace-id',
        500
      );
    });

    it('should estimate response size from body when no Content-Length', () => {
      httpClient.get('/api/no-content-length').subscribe();

      const responseBody = { message: 'test response' };
      const req = httpTestingController.expectOne('/api/no-content-length');
      req.flush(responseBody);

      expect(mockApiMonitoringService.recordApiCallComplete).toHaveBeenCalledWith(
        'test-api-call-id',
        'GET',
        '/api/no-content-length',
        200,
        jasmine.any(Number),
        'test-trace-id',
        JSON.stringify(responseBody).length
      );
    });
  });

  describe('Product ID Extraction', () => {
    it('should extract product ID from URL correctly', () => {
      httpClient.get('/api/consumer/products/abc123').subscribe();

      const req = httpTestingController.expectOne('/api/consumer/products/abc123');
      req.flush({ id: 'abc123' });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'product_view',
        data: jasmine.objectContaining({
          productId: 'abc123'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });

    it('should handle URLs with query parameters', () => {
      httpClient.get('/api/consumer/products/xyz789?details=true').subscribe();

      const req = httpTestingController.expectOne('/api/consumer/products/xyz789?details=true');
      req.flush({ id: 'xyz789' });

      expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
        type: 'product_view',
        data: jasmine.objectContaining({
          productId: 'xyz789'
        }),
        timestamp: jasmine.any(Number),
        sessionId: 'test-session-id'
      });
    });
  });
});