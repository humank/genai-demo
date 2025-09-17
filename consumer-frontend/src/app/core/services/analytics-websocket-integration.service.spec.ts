import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { AnalyticsWebSocketIntegrationService } from './analytics-websocket-integration.service';
import { ObservabilityService } from './observability.service';
import { AnalyticsUpdateMessage, RealTimeAnalyticsService } from './real-time-analytics.service';

describe('AnalyticsWebSocketIntegrationService', () => {
  let service: AnalyticsWebSocketIntegrationService;
  let mockRealTimeService: jasmine.SpyObj<RealTimeAnalyticsService>;
  let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
  let messagesSubject: Subject<AnalyticsUpdateMessage>;
  let connectionStateSubject: Subject<string>;
  let errorsSubject: Subject<Event>;

  beforeEach(() => {
    messagesSubject = new Subject<AnalyticsUpdateMessage>();
    connectionStateSubject = new Subject<string>();
    errorsSubject = new Subject<Event>();

    const realTimeServiceSpy = jasmine.createSpyObj('RealTimeAnalyticsService', [
      'subscribe',
      'getConnectionState',
      'isConnected',
      'disconnect',
      'connect',
      'ping'
    ], {
      messages$: messagesSubject.asObservable(),
      connectionState$: connectionStateSubject.asObservable(),
      errors$: errorsSubject.asObservable()
    });

    const observabilityServiceSpy = jasmine.createSpyObj('ObservabilityService', [
      'trackUserAction'
    ]);

    TestBed.configureTestingModule({
      providers: [
        AnalyticsWebSocketIntegrationService,
        { provide: RealTimeAnalyticsService, useValue: realTimeServiceSpy },
        { provide: ObservabilityService, useValue: observabilityServiceSpy }
      ]
    });

    service = TestBed.inject(AnalyticsWebSocketIntegrationService);
    mockRealTimeService = TestBed.inject(RealTimeAnalyticsService) as jasmine.SpyObj<RealTimeAnalyticsService>;
    mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
  });

  afterEach(() => {
    service.ngOnDestroy();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should subscribe to analytics channels when connected', () => {
    // Simulate connection established
    connectionStateSubject.next('connected');

    expect(mockRealTimeService.subscribe).toHaveBeenCalledWith('analytics');
    expect(mockRealTimeService.subscribe).toHaveBeenCalledWith('performance');
    expect(mockRealTimeService.subscribe).toHaveBeenCalledWith('business-metrics');
    expect(mockRealTimeService.subscribe).toHaveBeenCalledWith('system-status');
    expect(mockRealTimeService.subscribe).toHaveBeenCalledWith('errors');
  });

  it('should handle user behavior updates', (done) => {
    const testData = {
      eventType: 'page_view',
      sessionId: 'test-session-123',
      eventData: { page: '/products' }
    };

    service.userBehaviorUpdates.subscribe(data => {
      expect(data).toEqual(testData);
      done();
    });

    // Simulate receiving a user behavior update
    messagesSubject.next({
      type: 'user_behavior_update',
      data: testData,
      timestamp: new Date().toISOString()
    });
  });

  it('should handle performance metrics updates and warn on high values', (done) => {
    const testData = {
      metricType: 'lcp',
      value: 3000, // High LCP value
      page: '/products'
    };

    const consoleSpy = spyOn(console, 'warn');

    service.performanceMetricsUpdates.subscribe(data => {
      expect(data).toEqual(testData);
      expect(consoleSpy).toHaveBeenCalledWith('High LCP detected:', 3000, 'ms on page:', '/products');
      done();
    });

    // Simulate receiving a performance metrics update
    messagesSubject.next({
      type: 'performance_metrics_update',
      data: testData,
      timestamp: new Date().toISOString()
    });
  });

  it('should handle business metrics updates', (done) => {
    const testData = {
      metricName: 'conversion_rate',
      metricValue: 0.15,
      additionalData: { period: 'daily' }
    };

    service.businessMetricsUpdates.subscribe(data => {
      expect(data).toEqual(testData);
      done();
    });

    // Simulate receiving a business metrics update
    messagesSubject.next({
      type: 'business_metrics_update',
      data: testData,
      timestamp: new Date().toISOString()
    });
  });

  it('should handle system status updates', (done) => {
    const testData = {
      status: 'healthy',
      statusData: { cpu: 45, memory: 67 }
    };

    service.systemStatusUpdates.subscribe(data => {
      expect(data).toEqual(testData);
      done();
    });

    // Simulate receiving a system status update
    messagesSubject.next({
      type: 'system_status_update',
      data: testData,
      timestamp: new Date().toISOString()
    });
  });

  it('should handle error notifications and track them', (done) => {
    const testData = {
      errorType: 'api_error',
      errorMessage: 'Database timeout',
      errorContext: { endpoint: '/api/products' }
    };

    service.errorNotifications.subscribe(data => {
      expect(data).toEqual(testData);
      expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
        'websocket_error_notification',
        {
          errorType: 'api_error',
          errorMessage: 'Database timeout',
          errorContext: { endpoint: '/api/products' },
          timestamp: jasmine.any(Number)
        }
      );
      done();
    });

    // Simulate receiving an error notification
    messagesSubject.next({
      type: 'error_notification',
      data: testData,
      timestamp: new Date().toISOString()
    });
  });

  it('should track connection state changes in observability', () => {
    // Simulate connection state change
    connectionStateSubject.next('connected');

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
      'websocket_connection_state',
      {
        state: 'connected',
        timestamp: jasmine.any(Number)
      }
    );
  });

  it('should handle WebSocket errors and track them', () => {
    const testError = new Event('error');

    // Simulate WebSocket error
    errorsSubject.next(testError);

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith(
      'websocket_error',
      {
        message: 'WebSocket connection error',
        errorType: 'error',
        timestamp: jasmine.any(Number)
      }
    );
  });

  it('should provide connection status methods', () => {
    mockRealTimeService.getConnectionState.and.returnValue('connected');
    mockRealTimeService.isConnected.and.returnValue(true);

    expect(service.getConnectionStatus()).toBe('connected');
    expect(service.isConnected()).toBe(true);
  });

  it('should handle reconnection', () => {
    service.reconnect();

    expect(mockRealTimeService.disconnect).toHaveBeenCalled();
    
    // Verify connect is called after timeout
    setTimeout(() => {
      expect(mockRealTimeService.connect).toHaveBeenCalled();
    }, 1100);
  });

  it('should handle ping', () => {
    service.ping();
    expect(mockRealTimeService.ping).toHaveBeenCalled();
  });

  it('should handle unknown message types gracefully', () => {
    const consoleSpy = spyOn(console, 'warn');

    // Simulate receiving an unknown message type
    messagesSubject.next({
      type: 'unknown_message_type',
      data: { test: 'data' },
      timestamp: new Date().toISOString()
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      'Unknown message type:',
      'unknown_message_type',
      jasmine.any(Object)
    );
  });

  it('should handle performance metrics warnings for different metric types', () => {
    const consoleSpy = spyOn(console, 'warn');

    // Test FID warning
    messagesSubject.next({
      type: 'performance_metrics_update',
      data: { metricType: 'fid', value: 150, page: '/home' },
      timestamp: new Date().toISOString()
    });

    expect(consoleSpy).toHaveBeenCalledWith('High FID detected:', 150, 'ms on page:', '/home');

    // Test CLS warning
    messagesSubject.next({
      type: 'performance_metrics_update',
      data: { metricType: 'cls', value: 0.15, page: '/checkout' },
      timestamp: new Date().toISOString()
    });

    expect(consoleSpy).toHaveBeenCalledWith('High CLS detected:', 0.15, 'on page:', '/checkout');
  });
});