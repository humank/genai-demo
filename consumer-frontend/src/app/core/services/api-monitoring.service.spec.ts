import { TestBed } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { ApiMonitoringService } from './api-monitoring.service';
import { ObservabilityService } from './observability.service';

describe('ApiMonitoringService', () => {
  let service: ApiMonitoringService;
  let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
  let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;

  beforeEach(() => {
    const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
      'trackPerformanceMetric',
      'trackUserAction'
    ]);

    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', [
      'isDebugEnabled'
    ]);

    TestBed.configureTestingModule({
      providers: [
        ApiMonitoringService,
        { provide: ObservabilityService, useValue: observabilitySpy },
        { provide: ObservabilityConfigService, useValue: configSpy }
      ]
    });

    mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
    mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;

    // Default config setup
    mockConfigService.isDebugEnabled.and.returnValue(false);

    service = TestBed.inject(ApiMonitoringService);
  });

  describe('Service Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize with empty stats', () => {
      const stats = service.getCurrentStats();
      expect(stats.totalApiCalls).toBe(0);
      expect(stats.totalErrors).toBe(0);
      expect(stats.overallErrorRate).toBe(0);
    });
  });

  describe('API Call Recording', () => {
    it('should record API call start and return call ID', () => {
      const callId = service.recordApiCallStart('GET', '/api/products', 'trace-123');
      
      expect(callId).toBeTruthy();
      expect(callId).toMatch(/^api-call-\d+-[a-z0-9]+$/);
    });

    it('should record successful API call completion', () => {
      const callId = service.recordApiCallStart('GET', '/api/products', 'trace-123');
      
      service.recordApiCallComplete(
        callId,
        'GET',
        '/api/products',
        200,
        150,
        'trace-123',
        1024
      );

      const stats = service.getCurrentStats();
      expect(stats.totalApiCalls).toBe(1);
      expect(stats.totalErrors).toBe(0);
      expect(stats.averageResponseTime).toBe(150);
    });

    it('should record failed API call completion', () => {
      const callId = service.recordApiCallStart('POST', '/api/orders', 'trace-456');
      
      service.recordApiCallComplete(
        callId,
        'POST',
        '/api/orders',
        500,
        300,
        'trace-456',
        undefined,
        'Internal Server Error'
      );

      const stats = service.getCurrentStats();
      expect(stats.totalApiCalls).toBe(1);
      expect(stats.totalErrors).toBe(1);
      expect(stats.overallErrorRate).toBe(1);
    });

    it('should track performance metrics for API calls', () => {
      const callId = service.recordApiCallStart('GET', '/api/products', 'trace-123');
      
      service.recordApiCallComplete(
        callId,
        'GET',
        '/api/products',
        200,
        150,
        'trace-123'
      );

      expect(mockObservabilityService.trackPerformanceMetric).toHaveBeenCalledWith({
        type: 'ttfb',
        value: 150,
        page: jasmine.any(String),
        timestamp: jasmine.any(Number)
      });
    });
  });

  describe('Endpoint Statistics', () => {
    beforeEach(() => {
      // Record multiple API calls for testing
      const endpoints = [
        { method: 'GET', url: '/api/products', status: 200, duration: 100 },
        { method: 'GET', url: '/api/products', status: 200, duration: 150 },
        { method: 'GET', url: '/api/products', status: 404, duration: 50 },
        { method: 'POST', url: '/api/orders', status: 201, duration: 300 },
        { method: 'POST', url: '/api/orders', status: 500, duration: 200 }
      ];

      endpoints.forEach((endpoint, index) => {
        const callId = service.recordApiCallStart(endpoint.method, endpoint.url, `trace-${index}`);
        service.recordApiCallComplete(
          callId,
          endpoint.method,
          endpoint.url,
          endpoint.status,
          endpoint.duration,
          `trace-${index}`
        );
      });
    });

    it('should calculate endpoint statistics correctly', () => {
      const productStats = service.getEndpointStats('/api/products', 'GET');
      
      expect(productStats).toBeDefined();
      expect(productStats!.totalCalls).toBe(3);
      expect(productStats!.successCount).toBe(2);
      expect(productStats!.errorCount).toBe(1);
      expect(productStats!.errorRate).toBeCloseTo(1/3, 2);
      expect(productStats!.averageResponseTime).toBeCloseTo(100, 0); // (100 + 150 + 50) / 3
    });

    it('should track min and max response times', () => {
      const productStats = service.getEndpointStats('/api/products', 'GET');
      
      expect(productStats!.minResponseTime).toBe(50);
      expect(productStats!.maxResponseTime).toBe(150);
    });

    it('should identify error-prone endpoints', () => {
      // Add more calls to meet the minimum threshold of 5 calls
      for (let i = 5; i < 10; i++) {
        const callId = service.recordApiCallStart('POST', '/api/orders', `trace-${i}`);
        service.recordApiCallComplete(
          callId,
          'POST',
          '/api/orders',
          i < 7 ? 500 : 200, // 2 more errors out of 5 additional calls
          200,
          `trace-${i}`
        );
      }

      const errorProneEndpoints = service.getErrorProneEndpoints();
      
      expect(errorProneEndpoints.length).toBeGreaterThan(0);
      // The POST /api/orders endpoint should have error rate > 0
      const ordersEndpoint = errorProneEndpoints.find(e => e.endpoint === '/api/orders');
      expect(ordersEndpoint?.errorRate).toBeGreaterThan(0);
    });

    it('should identify slowest endpoints', () => {
      // Add more calls to meet the minimum threshold of 5 calls
      for (let i = 5; i < 10; i++) {
        const callId = service.recordApiCallStart('POST', '/api/orders', `trace-${i}`);
        service.recordApiCallComplete(
          callId,
          'POST',
          '/api/orders',
          200,
          400, // Slower response times
          `trace-${i}`
        );
      }

      const slowestEndpoints = service.getSlowestEndpoints();
      
      expect(slowestEndpoints.length).toBeGreaterThan(0);
      // The POST /api/orders endpoint should be in the slowest endpoints
      const ordersEndpoint = slowestEndpoints.find(e => e.endpoint === '/api/orders');
      expect(ordersEndpoint?.averageResponseTime).toBeGreaterThan(200);
    });
  });

  describe('Error Tracking', () => {
    it('should track recent errors', () => {
      const callId = service.recordApiCallStart('DELETE', '/api/products/123', 'trace-error');
      
      service.recordApiCallComplete(
        callId,
        'DELETE',
        '/api/products/123',
        403,
        100,
        'trace-error',
        undefined,
        'Forbidden'
      );

      const recentErrors = service.getRecentErrors();
      expect(recentErrors.length).toBe(1);
      expect(recentErrors[0].status).toBe(403);
      expect(recentErrors[0].errorMessage).toBe('Forbidden');
    });

    it('should categorize error types correctly', () => {
      const testCases = [
        { status: 400, expectedType: 'client_error' },
        { status: 404, expectedType: 'client_error' },
        { status: 500, expectedType: 'server_error' },
        { status: 503, expectedType: 'server_error' }
      ];

      testCases.forEach((testCase, index) => {
        const callId = service.recordApiCallStart('GET', `/api/test${index}`, `trace-${index}`);
        service.recordApiCallComplete(
          callId,
          'GET',
          `/api/test${index}`,
          testCase.status,
          100,
          `trace-${index}`,
          undefined,
          'Test error'
        );
      });

      const recentErrors = service.getRecentErrors();
      expect(recentErrors.length).toBe(testCases.length);
      
      recentErrors.forEach((error, index) => {
        expect(error.errorType).toBe(testCases[index].expectedType);
      });
    });
  });

  describe('Health Status', () => {
    it('should report healthy status with low error rate', () => {
      // Record mostly successful calls
      for (let i = 0; i < 10; i++) {
        const callId = service.recordApiCallStart('GET', '/api/healthy', `trace-${i}`);
        service.recordApiCallComplete(
          callId,
          'GET',
          '/api/healthy',
          200,
          100,
          `trace-${i}`
        );
      }

      expect(service.getApiHealthStatus()).toBe('healthy');
    });

    it('should report warning status with moderate error rate', () => {
      // Record calls with 10% error rate (warning threshold is 5%)
      for (let i = 0; i < 10; i++) {
        const callId = service.recordApiCallStart('GET', '/api/warning', `trace-${i}`);
        const status = i === 0 ? 500 : 200; // 1 error out of 10 = 10%
        service.recordApiCallComplete(
          callId,
          'GET',
          '/api/warning',
          status,
          100,
          `trace-${i}`
        );
      }

      expect(service.getApiHealthStatus()).toBe('warning');
    });

    it('should report critical status with high error rate', () => {
      // Record calls with 20% error rate (critical threshold is 15%)
      for (let i = 0; i < 10; i++) {
        const callId = service.recordApiCallStart('GET', '/api/critical', `trace-${i}`);
        const status = i < 2 ? 500 : 200; // 2 errors out of 10 = 20%
        service.recordApiCallComplete(
          callId,
          'GET',
          '/api/critical',
          status,
          100,
          `trace-${i}`
        );
      }

      expect(service.getApiHealthStatus()).toBe('critical');
    });
  });

  describe('Alert System', () => {
    it('should trigger alert for slow API calls', () => {
      const callId = service.recordApiCallStart('GET', '/api/slow', 'trace-slow');
      
      // Record a very slow API call (> 5000ms critical threshold)
      service.recordApiCallComplete(
        callId,
        'GET',
        '/api/slow',
        200,
        6000,
        'trace-slow'
      );

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'slow_api_call',
        jasmine.objectContaining({
          duration: 6000,
          method: 'GET',
          url: '/api/slow'
        })
      );

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_alert',
        jasmine.objectContaining({
          alertType: 'critical_response_time'
        })
      );
    });

    it('should trigger alert for high error rate endpoints', () => {
      // Create an endpoint with high error rate
      for (let i = 0; i < 15; i++) {
        const callId = service.recordApiCallStart('POST', '/api/failing', `trace-${i}`);
        const status = i < 5 ? 500 : 200; // 5 errors out of 15 = 33% error rate
        service.recordApiCallComplete(
          callId,
          'POST',
          '/api/failing',
          status,
          100,
          `trace-${i}`
        );
      }

      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'api_alert',
        jasmine.objectContaining({
          alertType: 'critical_error_rate'
        })
      );
    });
  });

  describe('Data Management', () => {
    it('should cleanup old records', () => {
      // Mock Date.now to simulate old records
      const originalDateNow = Date.now;
      const baseTime = 1000000;
      spyOn(Date, 'now').and.returnValue(baseTime);

      // Record some API calls
      for (let i = 0; i < 5; i++) {
        const callId = service.recordApiCallStart('GET', '/api/test', `trace-${i}`);
        service.recordApiCallComplete(callId, 'GET', '/api/test', 200, 100, `trace-${i}`);
      }

      // Advance time by 35 minutes (beyond 30-minute window)
      Date.now = jasmine.createSpy('now').and.returnValue(baseTime + 35 * 60 * 1000);

      service.cleanup();

      const stats = service.getCurrentStats();
      expect(stats.totalApiCalls).toBe(0);

      // Restore original Date.now
      Date.now = originalDateNow;
    });

    it('should reset all statistics', () => {
      // Record some API calls
      const callId = service.recordApiCallStart('GET', '/api/test', 'trace-test');
      service.recordApiCallComplete(callId, 'GET', '/api/test', 200, 100, 'trace-test');

      service.reset();

      const stats = service.getCurrentStats();
      expect(stats.totalApiCalls).toBe(0);
      expect(stats.totalErrors).toBe(0);
      expect(service.getEndpointStats('/api/test', 'GET')).toBeUndefined();
    });

    it('should export statistics for analysis', () => {
      // Record some API calls
      const callId = service.recordApiCallStart('GET', '/api/export', 'trace-export');
      service.recordApiCallComplete(callId, 'GET', '/api/export', 200, 100, 'trace-export');

      const exported = service.exportStats();

      expect(exported.calls.length).toBe(1);
      expect(exported.endpoints.length).toBe(1);
      expect(exported.summary.totalApiCalls).toBe(1);
    });
  });

  describe('URL Normalization', () => {
    it('should normalize URLs by removing query parameters', () => {
      const callId1 = service.recordApiCallStart('GET', '/api/products?page=1&size=10', 'trace-1');
      const callId2 = service.recordApiCallStart('GET', '/api/products?page=2&size=10', 'trace-2');
      
      service.recordApiCallComplete(callId1, 'GET', '/api/products?page=1&size=10', 200, 100, 'trace-1');
      service.recordApiCallComplete(callId2, 'GET', '/api/products?page=2&size=10', 200, 150, 'trace-2');

      const stats = service.getEndpointStats('/api/products', 'GET');
      expect(stats?.totalCalls).toBe(2);
      expect(stats?.averageResponseTime).toBe(125); // (100 + 150) / 2
    });

    it('should handle malformed URLs gracefully', () => {
      const callId = service.recordApiCallStart('GET', 'not-a-valid-url', 'trace-invalid');
      
      expect(() => {
        service.recordApiCallComplete(callId, 'GET', 'not-a-valid-url', 200, 100, 'trace-invalid');
      }).not.toThrow();
    });
  });

  describe('Debug Mode', () => {
    it('should log debug information when debug is enabled', () => {
      mockConfigService.isDebugEnabled.and.returnValue(true);
      spyOn(console, 'log');

      const callId = service.recordApiCallStart('GET', '/api/debug', 'trace-debug');
      service.recordApiCallComplete(callId, 'GET', '/api/debug', 200, 100, 'trace-debug');

      expect(console.log).toHaveBeenCalled();
    });

    it('should not log debug information when debug is disabled', () => {
      mockConfigService.isDebugEnabled.and.returnValue(false);
      spyOn(console, 'log');

      const callId = service.recordApiCallStart('GET', '/api/no-debug', 'trace-no-debug');
      service.recordApiCallComplete(callId, 'GET', '/api/no-debug', 200, 100, 'trace-no-debug');

      expect(console.log).not.toHaveBeenCalled();
    });
  });
});