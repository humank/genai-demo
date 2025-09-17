import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { Subject, interval, takeUntil } from 'rxjs';
import { ObservabilityService } from '../../../core/services/observability.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';

interface BusinessMetric {
  name: string;
  value: number;
  change: number;
  trend: 'up' | 'down' | 'stable';
  unit?: string;
}

interface RealtimeData {
  timestamp: string;
  pageViews: number;
  activeUsers: number;
  conversionRate: number;
  averageOrderValue: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ChartModule,
    CardModule,
    TableModule,
    TagModule,
    ButtonModule,
    ProgressBarModule
  ],
  template: `
    <div class="dashboard-container p-6">
      <div class="mb-6">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">即時分析儀表板</h1>
        <p class="text-gray-600">監控業務指標和用戶行為的即時數據</p>
      </div>

      <!-- Connection Status -->
      <div class="mb-6">
        <p-card>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div 
                class="w-3 h-3 rounded-full"
                [class]="getConnectionStatusClass()">
              </div>
              <span class="font-medium">WebSocket 連接狀態: {{ connectionStatus }}</span>
            </div>
            <p-button 
              label="重新連接" 
              icon="pi pi-refresh"
              [disabled]="connectionStatus === 'connected'"
              (onClick)="reconnect()"
              size="small">
            </p-button>
          </div>
        </p-card>
      </div>

      <!-- Key Metrics Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <p-card *ngFor="let metric of businessMetrics" class="metric-card">
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-900 mb-1">
              {{ formatMetricValue(metric.value, metric.unit) }}
            </div>
            <div class="text-sm text-gray-600 mb-2">{{ metric.name }}</div>
            <div class="flex items-center justify-center gap-1">
              <i 
                class="pi text-sm"
                [class]="getTrendIcon(metric.trend)"
                [style.color]="getTrendColor(metric.trend)">
              </i>
              <span 
                class="text-sm font-medium"
                [style.color]="getTrendColor(metric.trend)">
                {{ formatChange(metric.change) }}%
              </span>
            </div>
          </div>
        </p-card>
      </div>

      <!-- Charts Row -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <!-- Real-time Activity Chart -->
        <p-card header="即時活動">
          <p-chart 
            type="line" 
            [data]="activityChartData" 
            [options]="chartOptions"
            class="chart-container">
          </p-chart>
        </p-card>

        <!-- Conversion Funnel -->
        <p-card header="轉換漏斗">
          <p-chart 
            type="doughnut" 
            [data]="conversionChartData" 
            [options]="doughnutOptions"
            class="chart-container">
          </p-chart>
        </p-card>
      </div>

      <!-- User Activity Table -->
      <div class="grid grid-cols-1 gap-6">
        <p-card header="即時用戶活動">
          <p-table 
            [value]="recentActivities" 
            [paginator]="true" 
            [rows]="10"
            [loading]="isLoadingActivities"
            responsiveLayout="scroll">
            <ng-template pTemplate="header">
              <tr>
                <th>時間</th>
                <th>用戶ID</th>
                <th>動作</th>
                <th>頁面</th>
                <th>狀態</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-activity>
              <tr>
                <td>{{ formatTime(activity.timestamp) }}</td>
                <td>{{ activity.userId || '匿名' }}</td>
                <td>{{ activity.action }}</td>
                <td>{{ activity.page }}</td>
                <td>
                  <p-tag 
                    [value]="activity.status"
                    [severity]="getActivitySeverity(activity.status)">
                  </p-tag>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
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
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private realTimeAnalytics = inject(RealTimeAnalyticsService);
  private observabilityService = inject(ObservabilityService);

  connectionStatus: 'connecting' | 'connected' | 'disconnected' | 'error' = 'disconnected';
  isLoadingActivities = false;

  businessMetrics: BusinessMetric[] = [
    { name: '頁面瀏覽量', value: 0, change: 0, trend: 'stable', unit: '' },
    { name: '活躍用戶', value: 0, change: 0, trend: 'stable', unit: '' },
    { name: '轉換率', value: 0, change: 0, trend: 'stable', unit: '%' },
    { name: '平均訂單價值', value: 0, change: 0, trend: 'stable', unit: '$' }
  ];

  recentActivities: any[] = [];

  activityChartData: any = {
    labels: [],
    datasets: [
      {
        label: '頁面瀏覽量',
        data: [],
        borderColor: '#667eea',
        backgroundColor: 'rgba(102, 126, 234, 0.1)',
        tension: 0.4,
        fill: true
      },
      {
        label: '活躍用戶',
        data: [],
        borderColor: '#f093fb',
        backgroundColor: 'rgba(240, 147, 251, 0.1)',
        tension: 0.4,
        fill: true
      }
    ]
  };

  conversionChartData: any = {
    labels: ['訪客', '瀏覽商品', '加入購物車', '結帳', '完成購買'],
    datasets: [{
      data: [100, 75, 45, 25, 15],
      backgroundColor: [
        '#667eea',
        '#764ba2',
        '#f093fb',
        '#f5576c',
        '#4facfe'
      ],
      borderWidth: 0
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

  doughnutOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  };

  ngOnInit(): void {
    this.initializeWebSocketConnection();
    this.startDataPolling();
    this.loadInitialData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeWebSocketConnection(): void {
    // Monitor connection state
    this.realTimeAnalytics.connectionState$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.connectionStatus = state;
      });

    // Subscribe to real-time analytics updates
    this.realTimeAnalytics.subscribe('business-metrics');
    this.realTimeAnalytics.subscribe('user-activity');

    // Handle incoming messages
    this.realTimeAnalytics.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        this.handleRealtimeMessage(message);
      });
  }

  private startDataPolling(): void {
    // Poll for updated metrics every 30 seconds
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.updateBusinessMetrics();
        this.updateChartData();
      });
  }

  private loadInitialData(): void {
    this.isLoadingActivities = true;

    // Simulate loading initial data
    setTimeout(() => {
      this.generateMockData();
      this.isLoadingActivities = false;
    }, 1000);
  }

  private handleRealtimeMessage(message: any): void {
    switch (message.type) {
      case 'business_metrics':
        this.updateBusinessMetricsFromMessage(message.data);
        break;
      case 'user_activity':
        this.addUserActivity(message.data);
        break;
      case 'chart_update':
        this.updateChartData();
        break;
    }
  }

  private updateBusinessMetricsFromMessage(data: any): void {
    if (data.pageViews !== undefined) {
      this.businessMetrics[0].value = data.pageViews;
      this.businessMetrics[0].change = data.pageViewsChange || 0;
      this.businessMetrics[0].trend = this.calculateTrend(data.pageViewsChange);
    }

    if (data.activeUsers !== undefined) {
      this.businessMetrics[1].value = data.activeUsers;
      this.businessMetrics[1].change = data.activeUsersChange || 0;
      this.businessMetrics[1].trend = this.calculateTrend(data.activeUsersChange);
    }
  }

  private addUserActivity(activity: any): void {
    this.recentActivities.unshift({
      timestamp: new Date(),
      userId: activity.userId,
      action: activity.action,
      page: activity.page,
      status: activity.status || 'success'
    });

    // Keep only the latest 50 activities
    if (this.recentActivities.length > 50) {
      this.recentActivities = this.recentActivities.slice(0, 50);
    }
  }

  private updateBusinessMetrics(): void {
    // Simulate real-time metric updates
    this.businessMetrics.forEach(metric => {
      const change = (Math.random() - 0.5) * 10;
      metric.value += Math.floor(change);
      metric.change = change;
      metric.trend = this.calculateTrend(change);
    });
  }

  private updateChartData(): void {
    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    // Add new data point
    this.activityChartData.labels.push(timeLabel);
    this.activityChartData.datasets[0].data.push(Math.floor(Math.random() * 100) + 50);
    this.activityChartData.datasets[1].data.push(Math.floor(Math.random() * 50) + 20);

    // Keep only last 20 data points
    if (this.activityChartData.labels.length > 20) {
      this.activityChartData.labels.shift();
      this.activityChartData.datasets[0].data.shift();
      this.activityChartData.datasets[1].data.shift();
    }

    // Trigger chart update
    this.activityChartData = { ...this.activityChartData };
  }

  private generateMockData(): void {
    // Generate initial chart data
    for (let i = 0; i < 10; i++) {
      const time = new Date(Date.now() - (9 - i) * 60000);
      this.activityChartData.labels.push(time.toLocaleTimeString());
      this.activityChartData.datasets[0].data.push(Math.floor(Math.random() * 100) + 50);
      this.activityChartData.datasets[1].data.push(Math.floor(Math.random() * 50) + 20);
    }

    // Generate initial activities
    const actions = ['頁面瀏覽', '商品點擊', '加入購物車', '開始結帳', '完成購買'];
    const pages = ['/home', '/products', '/cart', '/checkout', '/orders'];

    for (let i = 0; i < 20; i++) {
      this.recentActivities.push({
        timestamp: new Date(Date.now() - i * 30000),
        userId: `user_${Math.floor(Math.random() * 1000)}`,
        action: actions[Math.floor(Math.random() * actions.length)],
        page: pages[Math.floor(Math.random() * pages.length)],
        status: Math.random() > 0.1 ? 'success' : 'error'
      });
    }

    // Initialize business metrics
    this.businessMetrics[0].value = 1250;
    this.businessMetrics[1].value = 89;
    this.businessMetrics[2].value = 3.2;
    this.businessMetrics[3].value = 156.78;
  }

  private calculateTrend(change: number): 'up' | 'down' | 'stable' {
    if (change > 2) return 'up';
    if (change < -2) return 'down';
    return 'stable';
  }

  reconnect(): void {
    this.realTimeAnalytics.disconnect();
    setTimeout(() => {
      this.realTimeAnalytics.connect();
    }, 1000);
  }

  getConnectionStatusClass(): string {
    switch (this.connectionStatus) {
      case 'connected': return 'bg-green-500';
      case 'connecting': return 'bg-yellow-500';
      case 'error': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  }

  getTrendIcon(trend: string): string {
    switch (trend) {
      case 'up': return 'pi-arrow-up';
      case 'down': return 'pi-arrow-down';
      default: return 'pi-minus';
    }
  }

  getTrendColor(trend: string): string {
    switch (trend) {
      case 'up': return '#10b981';
      case 'down': return '#ef4444';
      default: return '#6b7280';
    }
  }

  formatMetricValue(value: number, unit?: string): string {
    if (unit === '%') {
      return `${value.toFixed(1)}%`;
    }
    if (unit === '$') {
      return `$${value.toFixed(2)}`;
    }
    return value.toLocaleString();
  }

  formatChange(change: number): string {
    return change > 0 ? `+${change.toFixed(1)}` : change.toFixed(1);
  }

  formatTime(timestamp: Date): string {
    return timestamp.toLocaleTimeString();
  }

  getActivitySeverity(status: string): 'success' | 'info' | 'warning' | 'danger' {
    switch (status) {
      case 'success': return 'success';
      case 'error': return 'danger';
      case 'warning': return 'warning';
      default: return 'info';
    }
  }
}