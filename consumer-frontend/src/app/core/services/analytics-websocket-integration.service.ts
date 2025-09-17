import { Injectable, OnDestroy } from '@angular/core';
import { filter, Subject, takeUntil } from 'rxjs';
import { ObservabilityService } from './observability.service';
import { AnalyticsUpdateMessage, RealTimeAnalyticsService } from './real-time-analytics.service';

/**
 * Integration service that connects WebSocket real-time analytics with the observability system.
 * Handles real-time updates and provides analytics dashboard functionality.
 */
@Injectable({
  providedIn: 'root'
})
export class AnalyticsWebSocketIntegrationService implements OnDestroy {
  private destroy$ = new Subject<void>();
  private isInitialized = false;

  // Analytics data streams
  private userBehaviorUpdates$ = new Subject<any>();
  private performanceMetricsUpdates$ = new Subject<any>();
  private businessMetricsUpdates$ = new Subject<any>();
  private systemStatusUpdates$ = new Subject<any>();
  private errorNotifications$ = new Subject<any>();

  // Public observables for components to subscribe to
  public readonly userBehaviorUpdates = this.userBehaviorUpdates$.asObservable();
  public readonly performanceMetricsUpdates = this.performanceMetricsUpdates$.asObservable();
  public readonly businessMetricsUpdates = this.businessMetricsUpdates$.asObservable();
  public readonly systemStatusUpdates = this.systemStatusUpdates$.asObservable();
  public readonly errorNotifications = this.errorNotifications$.asObservable();

  constructor(
    private realTimeService: RealTimeAnalyticsService,
    private observabilityService: ObservabilityService
  ) {
    this.initialize();
  }

  /**
   * Initialize the WebSocket integration
   */
  private initialize(): void {
    if (this.isInitialized) {
      return;
    }

    // Subscribe to WebSocket messages and route them appropriately
    this.realTimeService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => this.handleAnalyticsMessage(message));

    // Subscribe to connection state changes
    this.realTimeService.connectionState$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => this.handleConnectionStateChange(state));

    // Subscribe to WebSocket errors
    this.realTimeService.errors$
      .pipe(takeUntil(this.destroy$))
      .subscribe(error => this.handleWebSocketError(error));

    // Subscribe to analytics channels
    this.subscribeToAnalyticsChannels();

    this.isInitialized = true;
  }

  /**
   * Subscribe to all relevant analytics channels
   */
  private subscribeToAnalyticsChannels(): void {
    // Wait for connection to be established
    this.realTimeService.connectionState$
      .pipe(
        filter(state => state === 'connected'),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.realTimeService.subscribe('analytics');
        this.realTimeService.subscribe('performance');
        this.realTimeService.subscribe('business-metrics');
        this.realTimeService.subscribe('system-status');
        this.realTimeService.subscribe('errors');
      });
  }

  /**
   * Handle incoming analytics messages from WebSocket
   */
  private handleAnalyticsMessage(message: AnalyticsUpdateMessage): void {
    console.log('Received analytics message:', message);

    switch (message.type) {
      case 'user_behavior_update':
        this.handleUserBehaviorUpdate(message.data);
        break;

      case 'performance_metrics_update':
        this.handlePerformanceMetricsUpdate(message.data);
        break;

      case 'business_metrics_update':
        this.handleBusinessMetricsUpdate(message.data);
        break;

      case 'system_status_update':
        this.handleSystemStatusUpdate(message.data);
        break;

      case 'error_notification':
        this.handleErrorNotification(message.data);
        break;

      case 'connection_established':
        console.log('WebSocket connection established:', message.data);
        break;

      case 'subscription_confirmed':
        console.log('Subscription confirmed:', message.data);
        break;

      case 'pong':
        console.log('Received pong response');
        break;

      default:
        console.warn('Unknown message type:', message.type, message);
    }
  }

  /**
   * Handle user behavior updates
   */
  private handleUserBehaviorUpdate(data: any): void {
    this.userBehaviorUpdates$.next(data);

    // Optionally integrate with local observability tracking
    if (data.eventType && data.sessionId) {
      console.log(`Real-time user behavior: ${data.eventType} from session ${data.sessionId}`);
    }
  }

  /**
   * Handle performance metrics updates
   */
  private handlePerformanceMetricsUpdate(data: any): void {
    this.performanceMetricsUpdates$.next(data);

    // Log performance alerts if metrics are concerning
    if (data.metricType === 'lcp' && data.value > 2500) {
      console.warn('High LCP detected:', data.value, 'ms on page:', data.page);
    } else if (data.metricType === 'fid' && data.value > 100) {
      console.warn('High FID detected:', data.value, 'ms on page:', data.page);
    } else if (data.metricType === 'cls' && data.value > 0.1) {
      console.warn('High CLS detected:', data.value, 'on page:', data.page);
    }
  }

  /**
   * Handle business metrics updates
   */
  private handleBusinessMetricsUpdate(data: any): void {
    this.businessMetricsUpdates$.next(data);

    // Log important business metric changes
    console.log(`Business metric update: ${data.metricName} = ${data.metricValue}`);
  }

  /**
   * Handle system status updates
   */
  private handleSystemStatusUpdate(data: any): void {
    this.systemStatusUpdates$.next(data);

    // Log system status changes
    if (data.status === 'unhealthy') {
      console.warn('System status alert:', data.statusData);
    } else {
      console.log('System status update:', data.status);
    }
  }

  /**
   * Handle error notifications
   */
  private handleErrorNotification(data: any): void {
    this.errorNotifications$.next(data);

    // Log error notifications
    console.error('Real-time error notification:', {
      type: data.errorType,
      message: data.errorMessage,
      context: data.errorContext
    });

    // Optionally track errors in local observability system
    this.observabilityService.trackUserAction('websocket_error_notification', {
      errorType: data.errorType,
      errorMessage: data.errorMessage,
      errorContext: data.errorContext,
      timestamp: Date.now()
    });
  }

  /**
   * Handle WebSocket connection state changes
   */
  private handleConnectionStateChange(state: string): void {
    console.log('WebSocket connection state changed:', state);

    // Track connection events in observability
    this.observabilityService.trackUserAction('websocket_connection_state', {
      state: state,
      timestamp: Date.now()
    });

    // Handle specific states
    switch (state) {
      case 'connected':
        console.log('Real-time analytics connected');
        break;

      case 'disconnected':
        console.warn('Real-time analytics disconnected');
        break;

      case 'error':
        console.error('Real-time analytics connection error');
        break;

      case 'connecting':
        console.log('Connecting to real-time analytics...');
        break;
    }
  }

  /**
   * Handle WebSocket errors
   */
  private handleWebSocketError(error: Event): void {
    console.error('WebSocket error:', error);

    // Track WebSocket errors in observability
    this.observabilityService.trackUserAction('websocket_error', {
      message: 'WebSocket connection error',
      errorType: error.type,
      timestamp: Date.now()
    });
  }

  /**
   * Get current connection status
   */
  getConnectionStatus(): string {
    return this.realTimeService.getConnectionState();
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.realTimeService.isConnected();
  }

  /**
   * Manually reconnect WebSocket
   */
  reconnect(): void {
    this.realTimeService.disconnect();
    setTimeout(() => {
      this.realTimeService.connect();
    }, 1000);
  }

  /**
   * Send ping to test connection
   */
  ping(): void {
    this.realTimeService.ping();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}