import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Subject, interval, takeUntil } from 'rxjs';
import { ApiMonitoringService } from '../../../core/services/api-monitoring.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';

interface SystemMetric {
  name: string;
  value: number;
  threshold: number;
  status: 'healthy' | 'warning' | 'critical';
  unit: string;
}

interface ApiEndpoint {
  endpoint: string;
  method: string;
  responseTime: number;
  errorRate: number;
  status: 'healthy' | 'warning' | 'critical';
  lastChecked: Date;
}

interface Alert {
  id: string;
  type: 'performance' | 'error' | 'system';
  severity: 'info' | 'warning' | 'error';
  message: string;
  timestamp: Date;
  acknowledged: boolean;
}

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [
    CommonModule,
    ChartModule,
    CardModule,
    TableModule,
    TagModule,
    ButtonModule,
    ProgressBarModule,
    ToastModule
  ],
  providers: [MessageService],
  template: `
    <div class="system-health-container p-6">
      <div class="mb-6">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">系統健康監控</h1>
        <p class="text-gray-600">監控系統效能、API 響應時間和錯誤率</p>
      </div>

      <!-- System Overview Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <p-card *ngFor="let metric of systemMetrics" class="metric-card">
          <div class="text-center">
            <div class="flex items-center justify-center mb-3">
              <div 
                class="w-4 h-4 rounded-full mr-2"
                [class]="getStatusColor(metric.status)">
              </div>
              <span class="font-medium text-gray-700">{{ metric.name }}</span>
            </div>
            <div class="text-2xl font-bold text-gray-900 mb-2">
              {{ formatMetricValue(metric.value, metric.unit) }}
            </div>
            <p-progressBar 
              [value]="getProgressValue(metric)"
              [styleClass]="getProgressBarClass(metric.status)"
              [showValue]="false">
            </p-progressBar>
            <div class="text-xs text-gray-500 mt-1">
              閾值: {{ formatMetricValue(metric.threshold, metric.unit) }}
            </div>
          </div>
        </p-card>
      </div>

      <!-- Performance Charts -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <!-- Response Time Chart -->
        <p-card header="API 響應時間趨勢">
          <p-chart 
            type="line" 
            [data]="responseTimeChartData" 
            [options]="chartOptions"
            class="chart-container">
          </p-chart>
        </p-card>

        <!-- Error Rate Chart -->
        <p-card header="錯誤率監控">
          <p-chart 
            type="bar" 
            [data]="errorRateChartData" 
            [options]="barChartOptions"
            class="chart-container">
          </p-chart>
        </p-card>
      </div>

      <!-- API Endpoints Status -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <p-card header="API 端點狀態">
          <p-table 
            [value]="apiEndpoints" 
            [paginator]="true" 
            [rows]="10"
            responsiveLayout="scroll">
            <ng-template pTemplate="header">
              <tr>
                <th>端點</th>
                <th>方法</th>
                <th>響應時間</th>
                <th>錯誤率</th>
                <th>狀態</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-endpoint>
              <tr>
                <td class="font-mono text-sm">{{ endpoint.endpoint }}</td>
                <td>
                  <p-tag 
                    [value]="endpoint.method"
                    [severity]="getMethodSeverity(endpoint.method)">
                  </p-tag>
                </td>
                <td>{{ endpoint.responseTime }}ms</td>
                <td>{{ endpoint.errorRate.toFixed(2) }}%</td>
                <td>
                  <p-tag 
                    [value]="endpoint.status"
                    [severity]="getStatusSeverity(endpoint.status)">
                  </p-tag>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>

        <!-- User Activity Statistics -->
        <p-card header="用戶活動統計">
          <div class="space-y-4">
            <div class="flex justify-between items-center p-3 bg-gray-50 rounded">
              <span class="font-medium">當前線上用戶</span>
              <span class="text-2xl font-bold text-blue-600">{{ userStats.currentOnline }}</span>
            </div>
            <div class="flex justify-between items-center p-3 bg-gray-50 rounded">
              <span class="font-medium">今日總訪問</span>
              <span class="text-2xl font-bold text-green-600">{{ userStats.todayVisits }}</span>
            </div>
            <div class="flex justify-between items-center p-3 bg-gray-50 rounded">
              <span class="font-medium">平均會話時長</span>
              <span class="text-2xl font-bold text-purple-600">{{ userStats.avgSessionDuration }}分</span>
            </div>
            <div class="flex justify-between items-center p-3 bg-gray-50 rounded">
              <span class="font-medium">跳出率</span>
              <span class="text-2xl font-bold text-orange-600">{{ userStats.bounceRate }}%</span>
            </div>
          </div>
        </p-card>
      </div>

      <!-- Alerts and Notifications -->
      <div class="grid grid-cols-1 gap-6">
        <p-card header="警報和通知">
          <div class="mb-4 flex justify-between items-center">
            <div class="flex gap-2">
              <p-button 
                label="全部標記為已讀" 
                icon="pi pi-check"
                size="small"
                [disabled]="!hasUnacknowledgedAlerts()"
                (onClick)="acknowledgeAllAlerts()">
              </p-button>
              <p-button 
                label="清除已讀" 
                icon="pi pi-trash"
                size="small"
                severity="secondary"
                (onClick)="clearAcknowledgedAlerts()">
              </p-button>
            </div>
            <div class="text-sm text-gray-600">
              {{ getUnacknowledgedCount() }} 個未讀警報
            </div>
          </div>
          
          <p-table 
            [value]="alerts" 
            [paginator]="true" 
            [rows]="15"
            responsiveLayout="scroll">
            <ng-template pTemplate="header">
              <tr>
                <th style="width: 3rem"></th>
                <th>類型</th>
                <th>嚴重程度</th>
                <th>訊息</th>
                <th>時間</th>
                <th>操作</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-alert>
              <tr [class.opacity-50]="alert.acknowledged">
                <td>
                  <i 
                    class="pi pi-circle-fill text-sm"
                    [class]="alert.acknowledged ? 'text-gray-400' : 'text-red-500'">
                  </i>
                </td>
                <td>
                  <p-tag 
                    [value]="alert.type"
                    [severity]="getAlertTypeSeverity(alert.type)">
                  </p-tag>
                </td>
                <td>
                  <p-tag 
                    [value]="alert.severity"
                    [severity]="alert.severity">
                  </p-tag>
                </td>
                <td>{{ alert.message }}</td>
                <td>{{ formatAlertTime(alert.timestamp) }}</td>
                <td>
                  <p-button 
                    *ngIf="!alert.acknowledged"
                    icon="pi pi-check"
                    size="small"
                    severity="success"
                    [text]="true"
                    (onClick)="acknowledgeAlert(alert.id)"
                    pTooltip="標記為已讀">
                  </p-button>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      </div>
    </div>

    <p-toast></p-toast>
  `,
  styles: [`
    .system-health-container {
      min-height: 100vh;
      background-color: #f8fafc;
    }

    .metric-card {
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .metric-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    }

    .chart-container {
      height: 300px;
    }

    :host ::ng-deep .p-card {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border: none;
    }

    :host ::ng-deep .p-card-header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-weight: 600;
    }

    :host ::ng-deep .p-table .p-table-thead > tr > th {
      background-color: #f1f5f9;
      color: #475569;
      font-weight: 600;
    }

    :host ::ng-deep .p-progressbar.healthy .p-progressbar-value {
      background: linear-gradient(to right, #10b981, #34d399);
    }

    :host ::ng-deep .p-progressbar.warning .p-progressbar-value {
      background: linear-gradient(to right, #f59e0b, #fbbf24);
    }

    :host ::ng-deep .p-progressbar.critical .p-progressbar-value {
      background: linear-gradient(to right, #ef4444, #f87171);
    }
  `]
})
export class SystemHealthComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private apiMonitoring = inject(ApiMonitoringService);
  private realTimeAnalytics = inject(RealTimeAnalyticsService);
  private messageService = inject(MessageService);

  systemMetrics: SystemMetric[] = [
    { name: 'CPU 使用率', value: 0, threshold: 80, status: 'healthy', unit: '%' },
    { name: '記憶體使用率', value: 0, threshold: 85, status: 'healthy', unit: '%' },
    { name: '平均響應時間', value: 0, threshold: 2000, status: 'healthy', unit: 'ms' },
    { name: '錯誤率', value: 0, threshold: 5, status: 'healthy', unit: '%' }
  ];

  apiEndpoints: ApiEndpoint[] = [];
  alerts: Alert[] = [];

  userStats = {
    currentOnline: 0,
    todayVisits: 0,
    avgSessionDuration: 0,
    bounceRate: 0
  };

  responseTimeChartData: any = {
    labels: [],
    datasets: [{
      label: '平均響應時間 (ms)',
      data: [],
      borderColor: '#667eea',
      backgroundColor: 'rgba(102, 126, 234, 0.1)',
      tension: 0.4,
      fill: true
    }]
  };

  errorRateChartData: any = {
    labels: [],
    datasets: [{
      label: '錯誤率 (%)',
      data: [],
      backgroundColor: [
        '#ef4444',
        '#f97316',
        '#eab308',
        '#22c55e',
        '#3b82f6'
      ]
    }]
  };

  chartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      x: {
        grid: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      }
    }
  };

  barChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        max: 10,
        grid: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      x: {
        grid: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      }
    }
  };

  ngOnInit(): void {
    this.initializeMonitoring();
    this.loadInitialData();
    this.startPeriodicUpdates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeMonitoring(): void {
    // ⚠️ MOCK DATA: WebSocket backend not implemented, using mock data
    console.info('SystemHealth: Using mock WebSocket data (backend not implemented)');
    
    // Subscribe to mock system health updates
    this.realTimeAnalytics.subscribe('system-health');
    this.realTimeAnalytics.subscribe('api-performance');

    // Handle incoming mock health messages
    this.realTimeAnalytics.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        console.info('SystemHealth: Received mock data:', message);
        this.handleHealthMessage(message);
      });
  }

  private loadInitialData(): void {
    this.generateMockSystemMetrics();
    this.generateMockApiEndpoints();
    this.generateMockAlerts();
    this.generateMockUserStats();
    this.generateMockChartData();
  }

  private startPeriodicUpdates(): void {
    // Update system metrics every 10 seconds
    interval(10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.updateSystemMetrics();
        this.updateChartData();
      });

    // Check for new alerts every 30 seconds
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.checkForNewAlerts();
      });
  }

  private handleHealthMessage(message: any): void {
    switch (message.type) {
      case 'system_metrics':
        this.updateSystemMetricsFromMessage(message.data);
        break;
      case 'api_performance':
        this.updateSystemMetrics();
        break;
      case 'alert':
        this.addNewAlert(message.data);
        break;
    }
  }

  private updateSystemMetricsFromMessage(data: any): void {
    if (data.cpu !== undefined) {
      this.systemMetrics[0].value = data.cpu;
      this.systemMetrics[0].status = this.calculateStatus(data.cpu, this.systemMetrics[0].threshold);
    }

    if (data.memory !== undefined) {
      this.systemMetrics[1].value = data.memory;
      this.systemMetrics[1].status = this.calculateStatus(data.memory, this.systemMetrics[1].threshold);
    }
  }

  private updateSystemMetrics(): void {
    this.systemMetrics.forEach(metric => {
      // Simulate realistic metric updates
      const variation = (Math.random() - 0.5) * 10;
      metric.value = Math.max(0, Math.min(100, metric.value + variation));
      metric.status = this.calculateStatus(metric.value, metric.threshold);
    });
  }

  private updateChartData(): void {
    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    // Update response time chart
    this.responseTimeChartData.labels.push(timeLabel);
    this.responseTimeChartData.datasets[0].data.push(
      this.systemMetrics[2].value + (Math.random() - 0.5) * 200
    );

    // Update error rate chart
    this.errorRateChartData.labels.push(timeLabel);
    this.errorRateChartData.datasets[0].data.push(
      Math.max(0, this.systemMetrics[3].value + (Math.random() - 0.5) * 2)
    );

    // Keep only last 20 data points
    if (this.responseTimeChartData.labels.length > 20) {
      this.responseTimeChartData.labels.shift();
      this.responseTimeChartData.datasets[0].data.shift();
      this.errorRateChartData.labels.shift();
      this.errorRateChartData.datasets[0].data.shift();
    }

    // Trigger chart updates
    this.responseTimeChartData = { ...this.responseTimeChartData };
    this.errorRateChartData = { ...this.errorRateChartData };
  }

  private checkForNewAlerts(): void {
    // Check system metrics for alert conditions
    this.systemMetrics.forEach(metric => {
      if (metric.status === 'critical' && !this.hasRecentAlert(metric.name)) {
        this.addAlert({
          type: 'system',
          severity: 'error',
          message: `${metric.name} 超過臨界值 (${metric.value.toFixed(1)}${metric.unit})`
        });
      }
    });

    // Check API endpoints for performance issues
    this.apiEndpoints.forEach(endpoint => {
      if (endpoint.status === 'critical' && !this.hasRecentAlert(endpoint.endpoint)) {
        this.addAlert({
          type: 'performance',
          severity: 'warning',
          message: `API ${endpoint.endpoint} 響應時間過長 (${endpoint.responseTime}ms)`
        });
      }
    });
  }

  private hasRecentAlert(identifier: string): boolean {
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000);
    return this.alerts.some(alert =>
      alert.message.includes(identifier) &&
      alert.timestamp > fiveMinutesAgo
    );
  }

  private addAlert(alertData: Partial<Alert>): void {
    const alert: Alert = {
      id: `alert_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      type: alertData.type || 'system',
      severity: alertData.severity || 'info',
      message: alertData.message || '',
      timestamp: new Date(),
      acknowledged: false
    };

    this.alerts.unshift(alert);

    // Show toast notification
    this.messageService.add({
      severity: alert.severity,
      summary: '新警報',
      detail: alert.message,
      life: 5000
    });
  }

  private addNewAlert(data: any): void {
    this.addAlert(data);
  }

  private calculateStatus(value: number, threshold: number): 'healthy' | 'warning' | 'critical' {
    if (value >= threshold) return 'critical';
    if (value >= threshold * 0.8) return 'warning';
    return 'healthy';
  }

  private generateMockSystemMetrics(): void {
    this.systemMetrics[0].value = 45 + Math.random() * 20; // CPU
    this.systemMetrics[1].value = 60 + Math.random() * 15; // Memory
    this.systemMetrics[2].value = 800 + Math.random() * 400; // Response time
    this.systemMetrics[3].value = 1 + Math.random() * 3; // Error rate

    this.systemMetrics.forEach(metric => {
      metric.status = this.calculateStatus(metric.value, metric.threshold);
    });
  }

  private generateMockApiEndpoints(): void {
    const endpoints = [
      { endpoint: '/api/products', method: 'GET' },
      { endpoint: '/api/orders', method: 'POST' },
      { endpoint: '/api/customers', method: 'GET' },
      { endpoint: '/api/cart', method: 'PUT' },
      { endpoint: '/api/analytics/events', method: 'POST' }
    ];

    this.apiEndpoints = endpoints.map(ep => ({
      ...ep,
      responseTime: Math.floor(200 + Math.random() * 800),
      errorRate: Math.random() * 5,
      status: 'healthy' as const,
      lastChecked: new Date()
    }));

    this.apiEndpoints.forEach(endpoint => {
      endpoint.status = this.calculateStatus(endpoint.responseTime, 2000);
    });
  }

  private generateMockAlerts(): void {
    const alertTypes = ['performance', 'error', 'system'] as const;
    const severities = ['info', 'warning', 'error'] as const;
    const messages = [
      'API 響應時間超過 2 秒',
      '記憶體使用率達到 85%',
      '錯誤率超過 5%',
      '資料庫連接池接近滿載',
      '磁碟空間不足'
    ];

    for (let i = 0; i < 10; i++) {
      this.alerts.push({
        id: `alert_${i}`,
        type: alertTypes[Math.floor(Math.random() * alertTypes.length)],
        severity: severities[Math.floor(Math.random() * severities.length)],
        message: messages[Math.floor(Math.random() * messages.length)],
        timestamp: new Date(Date.now() - i * 60000 * 10),
        acknowledged: Math.random() > 0.6
      });
    }
  }

  private generateMockUserStats(): void {
    this.userStats = {
      currentOnline: Math.floor(50 + Math.random() * 100),
      todayVisits: Math.floor(1000 + Math.random() * 2000),
      avgSessionDuration: Math.floor(5 + Math.random() * 10),
      bounceRate: Math.floor(20 + Math.random() * 30)
    };
  }

  private generateMockChartData(): void {
    // Generate initial chart data
    for (let i = 0; i < 10; i++) {
      const time = new Date(Date.now() - (9 - i) * 60000);
      this.responseTimeChartData.labels.push(time.toLocaleTimeString());
      this.responseTimeChartData.datasets[0].data.push(
        800 + Math.random() * 400
      );

      this.errorRateChartData.labels.push(time.toLocaleTimeString());
      this.errorRateChartData.datasets[0].data.push(
        Math.random() * 5
      );
    }
  }

  // Public methods for template
  getStatusColor(status: string): string {
    switch (status) {
      case 'healthy': return 'bg-green-500';
      case 'warning': return 'bg-yellow-500';
      case 'critical': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  }

  getProgressValue(metric: SystemMetric): number {
    return Math.min(100, (metric.value / metric.threshold) * 100);
  }

  getProgressBarClass(status: string): string {
    return status;
  }

  formatMetricValue(value: number, unit: string): string {
    if (unit === '%' || unit === 'ms') {
      return `${value.toFixed(1)}${unit}`;
    }
    return value.toLocaleString();
  }

  getMethodSeverity(method: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (method) {
      case 'GET': return 'info';
      case 'POST': return 'success';
      case 'PUT': return 'warn';
      case 'DELETE': return 'danger';
      default: return 'info';
    }
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (status) {
      case 'healthy': return 'success';
      case 'warning': return 'warn';
      case 'critical': return 'danger';
      default: return 'info';
    }
  }

  getAlertTypeSeverity(type: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (type) {
      case 'performance': return 'warn';
      case 'error': return 'danger';
      case 'system': return 'info';
      default: return 'info';
    }
  }

  formatAlertTime(timestamp: Date): string {
    return timestamp.toLocaleString();
  }

  hasUnacknowledgedAlerts(): boolean {
    return this.alerts.some(alert => !alert.acknowledged);
  }

  getUnacknowledgedCount(): number {
    return this.alerts.filter(alert => !alert.acknowledged).length;
  }

  acknowledgeAlert(alertId: string): void {
    const alert = this.alerts.find(a => a.id === alertId);
    if (alert) {
      alert.acknowledged = true;
      this.messageService.add({
        severity: 'success',
        summary: '警報已確認',
        detail: '警報已標記為已讀',
        life: 3000
      });
    }
  }

  acknowledgeAllAlerts(): void {
    this.alerts.forEach(alert => {
      alert.acknowledged = true;
    });
    this.messageService.add({
      severity: 'success',
      summary: '所有警報已確認',
      detail: '所有警報已標記為已讀',
      life: 3000
    });
  }

  clearAcknowledgedAlerts(): void {
    const beforeCount = this.alerts.length;
    this.alerts = this.alerts.filter(alert => !alert.acknowledged);
    const clearedCount = beforeCount - this.alerts.length;

    if (clearedCount > 0) {
      this.messageService.add({
        severity: 'info',
        summary: '警報已清除',
        detail: `已清除 ${clearedCount} 個已讀警報`,
        life: 3000
      });
    }
  }
}