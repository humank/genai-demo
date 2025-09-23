import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsWebSocketIntegrationService } from '../../../core/services/analytics-websocket-integration.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';

/**
 * Component to display real-time analytics connection status and recent updates.
 * Useful for debugging and monitoring WebSocket connectivity.
 */
@Component({
  selector: 'app-real-time-analytics-status',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="analytics-status-container" [ngClass]="'status-' + connectionState">
      <div class="status-header">
        <h4>Real-time Analytics</h4>
        <div class="connection-indicator" [ngClass]="'indicator-' + connectionState">
          <span class="status-dot"></span>
          <span class="status-text">{{ getStatusText() }}</span>
        </div>
      </div>

      <div class="status-actions" *ngIf="connectionState !== 'connected'">
        <button 
          class="reconnect-btn" 
          (click)="reconnect()" 
          [disabled]="connectionState === 'connecting'">
          {{ connectionState === 'connecting' ? 'Connecting...' : 'Reconnect' }}
        </button>
        <button class="ping-btn" (click)="ping()" *ngIf="connectionState === 'connected'">
          Ping
        </button>
      </div>

      <div class="recent-updates" *ngIf="recentUpdates.length > 0">
        <h5>Recent Updates ({{ recentUpdates.length }})</h5>
        <div class="updates-list">
          <div 
            class="update-item" 
            *ngFor="let update of recentUpdates; trackBy: trackByTimestamp"
            [ngClass]="'update-' + update.type">
            <span class="update-time">{{ formatTime(update.timestamp) }}</span>
            <span class="update-type">{{ update.type }}</span>
            <span class="update-summary">{{ getUpdateSummary(update) }}</span>
          </div>
        </div>
      </div>

      <div class="debug-info" *ngIf="showDebugInfo">
        <h5>Debug Information</h5>
        <div class="debug-item">
          <strong>Connection State:</strong> {{ connectionState }}
        </div>
        <div class="debug-item">
          <strong>Is Connected:</strong> {{ isConnected }}
        </div>
        <div class="debug-item">
          <strong>Total Updates:</strong> {{ totalUpdates }}
        </div>
      </div>

      <div class="toggle-debug">
        <button (click)="toggleDebugInfo()">
          {{ showDebugInfo ? 'Hide' : 'Show' }} Debug Info
        </button>
      </div>
    </div>
  `,
  styles: [`
    .analytics-status-container {
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 16px;
      margin: 16px 0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }

    .status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .status-header h4 {
      margin: 0;
      color: #495057;
      font-size: 16px;
    }

    .connection-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }

    .indicator-connected .status-dot {
      background-color: #28a745;
      box-shadow: 0 0 4px rgba(40, 167, 69, 0.5);
    }

    .indicator-connecting .status-dot {
      background-color: #ffc107;
      animation: pulse 1.5s infinite;
    }

    .indicator-disconnected .status-dot {
      background-color: #6c757d;
    }

    .indicator-error .status-dot {
      background-color: #dc3545;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .status-text {
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
    }

    .indicator-connected .status-text {
      color: #28a745;
    }

    .indicator-connecting .status-text {
      color: #ffc107;
    }

    .indicator-disconnected .status-text {
      color: #6c757d;
    }

    .indicator-error .status-text {
      color: #dc3545;
    }

    .status-actions {
      margin-bottom: 16px;
    }

    .reconnect-btn, .ping-btn {
      background: #007bff;
      color: white;
      border: none;
      padding: 6px 12px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
      margin-right: 8px;
    }

    .reconnect-btn:disabled {
      background: #6c757d;
      cursor: not-allowed;
    }

    .reconnect-btn:hover:not(:disabled) {
      background: #0056b3;
    }

    .ping-btn:hover {
      background: #0056b3;
    }

    .recent-updates h5 {
      margin: 0 0 8px 0;
      color: #495057;
      font-size: 14px;
    }

    .updates-list {
      max-height: 200px;
      overflow-y: auto;
      border: 1px solid #e9ecef;
      border-radius: 4px;
      background: white;
    }

    .update-item {
      display: flex;
      gap: 8px;
      padding: 8px 12px;
      border-bottom: 1px solid #f8f9fa;
      font-size: 12px;
    }

    .update-item:last-child {
      border-bottom: none;
    }

    .update-time {
      color: #6c757d;
      min-width: 60px;
    }

    .update-type {
      font-weight: 500;
      min-width: 120px;
    }

    .update-user_behavior_update .update-type {
      color: #007bff;
    }

    .update-performance_metrics_update .update-type {
      color: #28a745;
    }

    .update-business_metrics_update .update-type {
      color: #ffc107;
    }

    .update-error_notification .update-type {
      color: #dc3545;
    }

    .update-summary {
      color: #495057;
      flex: 1;
    }

    .debug-info {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #dee2e6;
    }

    .debug-info h5 {
      margin: 0 0 8px 0;
      color: #495057;
      font-size: 14px;
    }

    .debug-item {
      font-size: 12px;
      margin-bottom: 4px;
      color: #6c757d;
    }

    .toggle-debug {
      margin-top: 12px;
      text-align: center;
    }

    .toggle-debug button {
      background: none;
      border: 1px solid #dee2e6;
      color: #6c757d;
      padding: 4px 8px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 11px;
    }

    .toggle-debug button:hover {
      background: #f8f9fa;
    }
  `]
})
export class RealTimeAnalyticsStatusComponent implements OnInit, OnDestroy {
  connectionState: string = 'disconnected';
  isConnected: boolean = false;
  recentUpdates: any[] = [];
  totalUpdates: number = 0;
  showDebugInfo: boolean = false;
  private destroy$ = new Subject<void>();
  private maxUpdates = 10; // Keep only the last 10 updates

  constructor(
    private realTimeService: RealTimeAnalyticsService,
    private integrationService: AnalyticsWebSocketIntegrationService
  ) {}

  ngOnInit(): void {
    // Subscribe to connection state changes
    this.realTimeService.connectionState$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.connectionState = state;
        this.isConnected = this.realTimeService.isConnected();
      });

    // Subscribe to all types of analytics updates
    this.subscribeToUpdates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private subscribeToUpdates(): void {
    // User behavior updates
    this.integrationService.userBehaviorUpdates
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.addUpdate('user_behavior_update', data));

    // Performance metrics updates
    this.integrationService.performanceMetricsUpdates
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.addUpdate('performance_metrics_update', data));

    // Business metrics updates
    this.integrationService.businessMetricsUpdates
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.addUpdate('business_metrics_update', data));

    // System status updates
    this.integrationService.systemStatusUpdates
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.addUpdate('system_status_update', data));

    // Error notifications
    this.integrationService.errorNotifications
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.addUpdate('error_notification', data));
  }

  private addUpdate(type: string, data: any): void {
    const update = {
      type,
      data,
      timestamp: Date.now()
    };

    this.recentUpdates.unshift(update);
    
    // Keep only the most recent updates
    if (this.recentUpdates.length > this.maxUpdates) {
      this.recentUpdates = this.recentUpdates.slice(0, this.maxUpdates);
    }

    this.totalUpdates++;
  }

  getStatusText(): string {
    switch (this.connectionState) {
      case 'connected': return 'Connected';
      case 'connecting': return 'Connecting';
      case 'disconnected': return 'Disconnected';
      case 'error': return 'Error';
      default: return 'Unknown';
    }
  }

  reconnect(): void {
    this.integrationService.reconnect();
  }

  ping(): void {
    this.integrationService.ping();
  }

  toggleDebugInfo(): void {
    this.showDebugInfo = !this.showDebugInfo;
  }

  trackByTimestamp(index: number, item: any): number {
    return item.timestamp;
  }

  formatTime(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { 
      hour12: false, 
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit' 
    });
  }

  getUpdateSummary(update: any): string {
    switch (update.type) {
      case 'user_behavior_update':
        return `${update.data.eventType} from ${update.data.sessionId}`;
      
      case 'performance_metrics_update':
        return `${update.data.metricType}: ${update.data.value} on ${update.data.page}`;
      
      case 'business_metrics_update':
        return `${update.data.metricName}: ${update.data.metricValue}`;
      
      case 'system_status_update':
        return `Status: ${update.data.status}`;
      
      case 'error_notification':
        return `${update.data.errorType}: ${update.data.errorMessage}`;
      
      default:
        return JSON.stringify(update.data).substring(0, 50) + '...';
    }
  }
}