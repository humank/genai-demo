import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { AnalyticsWebSocketIntegrationService } from '../../../core/services/analytics-websocket-integration.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';
import { RealTimeAnalyticsStatusComponent } from './real-time-analytics-status.component';

describe('RealTimeAnalyticsStatusComponent', () => {
  let component: RealTimeAnalyticsStatusComponent;
  let fixture: ComponentFixture<RealTimeAnalyticsStatusComponent>;
  let mockRealTimeService: jasmine.SpyObj<RealTimeAnalyticsService>;
  let mockIntegrationService: jasmine.SpyObj<AnalyticsWebSocketIntegrationService>;
  let connectionStateSubject: Subject<string>;

  beforeEach(async () => {
    connectionStateSubject = new Subject<string>();

    const realTimeServiceSpy = jasmine.createSpyObj('RealTimeAnalyticsService', [
      'isConnected'
    ], {
      connectionState$: connectionStateSubject.asObservable()
    });

    const integrationServiceSpy = jasmine.createSpyObj('AnalyticsWebSocketIntegrationService', [
      'reconnect',
      'ping'
    ], {
      userBehaviorUpdates: of(),
      performanceMetricsUpdates: of(),
      businessMetricsUpdates: of(),
      systemStatusUpdates: of(),
      errorNotifications: of()
    });

    await TestBed.configureTestingModule({
      imports: [RealTimeAnalyticsStatusComponent],
      providers: [
        { provide: RealTimeAnalyticsService, useValue: realTimeServiceSpy },
        { provide: AnalyticsWebSocketIntegrationService, useValue: integrationServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RealTimeAnalyticsStatusComponent);
    component = fixture.componentInstance;
    mockRealTimeService = TestBed.inject(RealTimeAnalyticsService) as jasmine.SpyObj<RealTimeAnalyticsService>;
    mockIntegrationService = TestBed.inject(AnalyticsWebSocketIntegrationService) as jasmine.SpyObj<AnalyticsWebSocketIntegrationService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update connection state when service state changes', () => {
    mockRealTimeService.isConnected.and.returnValue(true);
    
    fixture.detectChanges();
    
    // Simulate connection state change
    connectionStateSubject.next('connected');
    
    expect(component.connectionState).toBe('connected');
    expect(component.isConnected).toBe(true);
  });

  it('should display correct status text for different states', () => {
    expect(component.getStatusText()).toBe('Disconnected'); // Initial state
    
    component.connectionState = 'connected';
    expect(component.getStatusText()).toBe('Connected');
    
    component.connectionState = 'connecting';
    expect(component.getStatusText()).toBe('Connecting');
    
    component.connectionState = 'error';
    expect(component.getStatusText()).toBe('Error');
  });

  it('should call reconnect on integration service', () => {
    component.reconnect();
    expect(mockIntegrationService.reconnect).toHaveBeenCalled();
  });

  it('should call ping on integration service', () => {
    component.ping();
    expect(mockIntegrationService.ping).toHaveBeenCalled();
  });

  it('should toggle debug info visibility', () => {
    expect(component.showDebugInfo).toBe(false);
    
    component.toggleDebugInfo();
    expect(component.showDebugInfo).toBe(true);
    
    component.toggleDebugInfo();
    expect(component.showDebugInfo).toBe(false);
  });

  it('should format time correctly', () => {
    const timestamp = new Date('2023-01-01T12:30:45').getTime();
    const formatted = component.formatTime(timestamp);
    
    expect(formatted).toMatch(/\d{2}:\d{2}:\d{2}/);
  });

  it('should generate correct update summaries', () => {
    const userBehaviorUpdate = {
      type: 'user_behavior_update',
      data: { eventType: 'page_view', sessionId: 'session-123' }
    };
    
    const performanceUpdate = {
      type: 'performance_metrics_update',
      data: { metricType: 'lcp', value: 2500, page: '/products' }
    };
    
    const businessUpdate = {
      type: 'business_metrics_update',
      data: { metricName: 'conversion_rate', metricValue: 0.15 }
    };
    
    expect(component.getUpdateSummary(userBehaviorUpdate)).toBe('page_view from session-123');
    expect(component.getUpdateSummary(performanceUpdate)).toBe('lcp: 2500 on /products');
    expect(component.getUpdateSummary(businessUpdate)).toBe('conversion_rate: 0.15');
  });

  it('should track updates by timestamp', () => {
    const update = { timestamp: 12345 };
    expect(component.trackByTimestamp(0, update)).toBe(12345);
  });

  it('should add updates and maintain max limit', () => {
    // Add more updates than the max limit
    for (let i = 0; i < 15; i++) {
      (component as any).addUpdate('test_update', { index: i });
    }
    
    expect(component.recentUpdates.length).toBe(10); // maxUpdates
    expect(component.totalUpdates).toBe(15);
    
    // Verify most recent updates are kept (should be index 14 to 5)
    expect(component.recentUpdates[0].data.index).toBe(14);
    expect(component.recentUpdates[9].data.index).toBe(5);
  });

  it('should render connection status indicator', () => {
    component.connectionState = 'connected';
    fixture.detectChanges();
    
    const statusIndicator = fixture.nativeElement.querySelector('.connection-indicator');
    expect(statusIndicator).toBeTruthy();
    expect(statusIndicator.classList.contains('indicator-connected')).toBe(true);
  });

  it('should show reconnect button when not connected', () => {
    component.connectionState = 'disconnected';
    fixture.detectChanges();
    
    const reconnectBtn = fixture.nativeElement.querySelector('.reconnect-btn');
    expect(reconnectBtn).toBeTruthy();
    expect(reconnectBtn.textContent.trim()).toBe('Reconnect');
  });

  it('should show connecting state on button when connecting', () => {
    component.connectionState = 'connecting';
    fixture.detectChanges();
    
    const reconnectBtn = fixture.nativeElement.querySelector('.reconnect-btn');
    expect(reconnectBtn).toBeTruthy();
    expect(reconnectBtn.textContent.trim()).toBe('Connecting...');
    expect(reconnectBtn.disabled).toBe(true);
  });
});