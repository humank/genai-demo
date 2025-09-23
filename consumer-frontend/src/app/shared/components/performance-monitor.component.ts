import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { BatchProcessorService } from '../../core/services/batch-processor.service';
import { ObservabilityService } from '../../core/services/observability.service';

export interface PerformanceMetrics {
  // Web Vitals
  lcp?: number; // Largest Contentful Paint
  fid?: number; // First Input Delay
  cls?: number; // Cumulative Layout Shift
  ttfb?: number; // Time to First Byte

  // 頁面效能
  pageLoadTime?: number;
  domContentLoaded?: number;
  firstPaint?: number;
  firstContentfulPaint?: number;

  // 網路效能
  downlink?: number;
  rtt?: number;
  connectionType?: string;
  effectiveType?: string;

  // 記憶體效能
  usedJSHeapSize?: number;
  totalJSHeapSize?: number;
  jsHeapSizeLimit?: number;
}

export interface WebVitalsMetric {
  value: number;
  rating: 'good' | 'needs-improvement' | 'poor';
}

export interface WebVitalsReport {
  lcp?: WebVitalsMetric;
  fid?: WebVitalsMetric;
  cls?: WebVitalsMetric;
  fcp?: WebVitalsMetric;
  ttfb?: WebVitalsMetric;

  // 記憶體使用
  usedJSHeapSize?: number;
  totalJSHeapSize?: number;
  jsHeapSizeLimit?: number;

  // 網路資訊
  connectionType?: string;
  effectiveType?: string;
  downlink?: number;
  rtt?: number;
}

@Component({
  selector: 'app-performance-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="performance-monitor" *ngIf="showMonitor && isEnabled">
      <div class="performance-header">
        <h4>效能監控</h4>
        <button (click)="toggleExpanded()" class="toggle-btn">
          {{ isExpanded ? '收起' : '展開' }}
        </button>
      </div>
      
      <div class="performance-content" *ngIf="isExpanded">
        <!-- Enhanced Web Vitals -->
        <div class="metrics-section">
          <h5>Web Vitals (Enhanced)</h5>
          <div class="metrics-grid">
            <div class="metric-item" 
                 [class.good]="webVitalsReport?.lcp?.rating === 'good'" 
                 [class.needs-improvement]="webVitalsReport?.lcp?.rating === 'needs-improvement'"
                 [class.poor]="webVitalsReport?.lcp?.rating === 'poor'">
              <span class="metric-label">LCP</span>
              <span class="metric-value">{{ formatTime(webVitalsReport?.lcp?.value) }}</span>
              <span class="metric-rating">{{ webVitalsReport?.lcp?.rating || 'N/A' }}</span>
            </div>
            <div class="metric-item" 
                 [class.good]="webVitalsReport?.fid?.rating === 'good'" 
                 [class.needs-improvement]="webVitalsReport?.fid?.rating === 'needs-improvement'"
                 [class.poor]="webVitalsReport?.fid?.rating === 'poor'">
              <span class="metric-label">FID</span>
              <span class="metric-value">{{ formatTime(webVitalsReport?.fid?.value) }}</span>
              <span class="metric-rating">{{ webVitalsReport?.fid?.rating || 'N/A' }}</span>
            </div>
            <div class="metric-item" 
                 [class.good]="webVitalsReport?.cls?.rating === 'good'" 
                 [class.needs-improvement]="webVitalsReport?.cls?.rating === 'needs-improvement'"
                 [class.poor]="webVitalsReport?.cls?.rating === 'poor'">
              <span class="metric-label">CLS</span>
              <span class="metric-value">{{ formatCLS(webVitalsReport?.cls?.value) }}</span>
              <span class="metric-rating">{{ webVitalsReport?.cls?.rating || 'N/A' }}</span>
            </div>
            <div class="metric-item" 
                 [class.good]="webVitalsReport?.fcp?.rating === 'good'" 
                 [class.needs-improvement]="webVitalsReport?.fcp?.rating === 'needs-improvement'"
                 [class.poor]="webVitalsReport?.fcp?.rating === 'poor'">
              <span class="metric-label">FCP</span>
              <span class="metric-value">{{ formatTime(webVitalsReport?.fcp?.value) }}</span>
              <span class="metric-rating">{{ webVitalsReport?.fcp?.rating || 'N/A' }}</span>
            </div>
            <div class="metric-item" 
                 [class.good]="webVitalsReport?.ttfb?.rating === 'good'" 
                 [class.needs-improvement]="webVitalsReport?.ttfb?.rating === 'needs-improvement'"
                 [class.poor]="webVitalsReport?.ttfb?.rating === 'poor'">
              <span class="metric-label">TTFB</span>
              <span class="metric-value">{{ formatTime(webVitalsReport?.ttfb?.value) }}</span>
              <span class="metric-rating">{{ webVitalsReport?.ttfb?.rating || 'N/A' }}</span>
            </div>
          </div>
        </div>

        <!-- 頁面效能 -->
        <div class="metrics-section">
          <h5>頁面效能</h5>
          <div class="metrics-grid">
            <div class="metric-item">
              <span class="metric-label">頁面載入</span>
              <span class="metric-value">{{ formatTime(metrics.pageLoadTime) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">DOM 載入</span>
              <span class="metric-value">{{ formatTime(metrics.domContentLoaded) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">首次繪製</span>
              <span class="metric-value">{{ formatTime(metrics.firstPaint) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">首次內容繪製</span>
              <span class="metric-value">{{ formatTime(metrics.firstContentfulPaint) }}</span>
            </div>
          </div>
        </div>

        <!-- 記憶體使用 -->
        <div class="metrics-section" *ngIf="hasMemoryInfo">
          <h5>記憶體使用</h5>
          <div class="metrics-grid">
            <div class="metric-item">
              <span class="metric-label">已使用</span>
              <span class="metric-value">{{ formatMemory(metrics.usedJSHeapSize) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">總計</span>
              <span class="metric-value">{{ formatMemory(metrics.totalJSHeapSize) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">限制</span>
              <span class="metric-value">{{ formatMemory(metrics.jsHeapSizeLimit) }}</span>
            </div>
          </div>
        </div>

        <!-- 網路資訊 -->
        <div class="metrics-section" *ngIf="hasNetworkInfo">
          <h5>網路資訊</h5>
          <div class="metrics-grid">
            <div class="metric-item">
              <span class="metric-label">連線類型</span>
              <span class="metric-value">{{ metrics.connectionType || 'N/A' }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">有效類型</span>
              <span class="metric-value">{{ metrics.effectiveType || 'N/A' }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">下載速度</span>
              <span class="metric-value">{{ formatSpeed(metrics.downlink) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">RTT</span>
              <span class="metric-value">{{ formatTime(metrics.rtt) }}</span>
            </div>
          </div>
        </div>

        <!-- 批次處理狀態 -->
        <div class="metrics-section">
          <h5>事件處理狀態</h5>
          <div class="status-info">
            <div class="status-item">
              <span class="status-label">佇列長度:</span>
              <span class="status-value">{{ batchStatus.queueLength }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">已處理:</span>
              <span class="status-value">{{ batchStatus.totalProcessed }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">失敗:</span>
              <span class="status-value">{{ batchStatus.totalFailed }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">網路狀態:</span>
              <span class="status-value" [class.online]="batchStatus.isOnline" [class.offline]="!batchStatus.isOnline">
                {{ batchStatus.isOnline ? '在線' : '離線' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 控制按鈕 -->
        <div class="controls">
          <button (click)="refreshMetrics()" class="control-btn">刷新指標</button>
          <button (click)="clearMetrics()" class="control-btn">清除指標</button>
          <button (click)="exportMetrics()" class="control-btn">匯出數據</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .performance-monitor {
      position: fixed;
      top: 20px;
      right: 20px;
      background: rgba(0, 0, 0, 0.9);
      color: white;
      border-radius: 8px;
      padding: 16px;
      font-family: monospace;
      font-size: 12px;
      max-width: 400px;
      z-index: 10000;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    .performance-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .performance-header h4 {
      margin: 0;
      font-size: 14px;
      color: #fff;
    }

    .toggle-btn {
      background: #333;
      color: white;
      border: none;
      padding: 4px 8px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 10px;
    }

    .toggle-btn:hover {
      background: #555;
    }

    .metrics-section {
      margin-bottom: 16px;
    }

    .metrics-section h5 {
      margin: 0 0 8px 0;
      font-size: 12px;
      color: #ccc;
      border-bottom: 1px solid #333;
      padding-bottom: 4px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }

    .metric-item {
      display: flex;
      justify-content: space-between;
      padding: 4px 8px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 4px;
    }

    .metric-item.good {
      background: rgba(0, 255, 0, 0.2);
      border-left: 3px solid #00ff00;
    }

    .metric-item.needs-improvement {
      background: rgba(255, 165, 0, 0.2);
      border-left: 3px solid #ffa500;
    }

    .metric-item.poor {
      background: rgba(255, 0, 0, 0.2);
      border-left: 3px solid #ff0000;
    }

    .metric-rating {
      font-size: 10px;
      color: #999;
      display: block;
      margin-top: 2px;
    }

    .metric-label {
      color: #ccc;
    }

    .metric-value {
      color: #fff;
      font-weight: bold;
    }

    .status-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .status-item {
      display: flex;
      justify-content: space-between;
    }

    .status-label {
      color: #ccc;
    }

    .status-value {
      color: #fff;
    }

    .status-value.online {
      color: #0f0;
    }

    .status-value.offline {
      color: #f00;
    }

    .controls {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }

    .control-btn {
      flex: 1;
      background: #333;
      color: white;
      border: none;
      padding: 6px 8px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 10px;
    }

    .control-btn:hover {
      background: #555;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerformanceMonitorComponent implements OnInit, OnDestroy {
  @Input() showMonitor: boolean = false;
  @Input() updateInterval: number = 5000; // 5 秒更新一次
  @Input() position: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' = 'top-right';

  metrics: PerformanceMetrics = {};
  webVitalsReport: WebVitalsReport | null = null;
  batchStatus: any = {};
  isExpanded = false;
  isEnabled = false;
  hasMemoryInfo = false;
  hasNetworkInfo = false;

  private updateSubscription?: Subscription;
  private performanceObserver?: PerformanceObserver;

  constructor(
    private observabilityService: ObservabilityService,
    private configService: ObservabilityConfigService,
    private batchProcessor: BatchProcessorService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.isEnabled = this.configService.isPerformanceMonitoringEnabled() &&
      this.configService.isDevelopment();

    if (this.isEnabled) {
      this.initializeMonitoring();
      this.startPeriodicUpdates();
    }
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
    this.cdr.markForCheck();
  }

  async refreshMetrics(): Promise<void> {
    await this.collectMetrics();
    this.cdr.markForCheck();
  }

  clearMetrics(): void {
    this.metrics = {};
    this.cdr.markForCheck();
  }

  exportMetrics(): void {
    const data = {
      timestamp: new Date().toISOString(),
      metrics: this.metrics,
      batchStatus: this.batchStatus,
      page: window.location.pathname,
      userAgent: navigator.userAgent
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `performance-metrics-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private async initializeMonitoring(): Promise<void> {
    await this.collectMetrics();
    this.setupPerformanceObserver();
    this.checkBrowserCapabilities();
  }

  private startPeriodicUpdates(): void {
    this.updateSubscription = interval(this.updateInterval).subscribe(async () => {
      await this.collectMetrics();
      this.updateBatchStatus();
      this.cdr.markForCheck();
    });
  }

  private async collectMetrics(): Promise<void> {
    this.collectNavigationMetrics();
    this.collectMemoryMetrics();
    this.collectNetworkMetrics();
    await this.collectWebVitals();
  }

  private collectNavigationMetrics(): void {
    if (!performance.timing) return;

    const timing = performance.timing;
    const navigationStart = timing.navigationStart;

    this.metrics.pageLoadTime = timing.loadEventEnd - navigationStart;
    this.metrics.domContentLoaded = timing.domContentLoadedEventEnd - navigationStart;
    this.metrics.ttfb = timing.responseStart - navigationStart;

    // 使用 Performance API 獲取更精確的指標
    if (performance.getEntriesByType) {
      const paintEntries = performance.getEntriesByType('paint');
      paintEntries.forEach(entry => {
        if (entry.name === 'first-paint') {
          this.metrics.firstPaint = entry.startTime;
        } else if (entry.name === 'first-contentful-paint') {
          this.metrics.firstContentfulPaint = entry.startTime;
        }
      });
    }
  }

  private collectMemoryMetrics(): void {
    if ((performance as any).memory) {
      const memory = (performance as any).memory;
      this.metrics.usedJSHeapSize = memory.usedJSHeapSize;
      this.metrics.totalJSHeapSize = memory.totalJSHeapSize;
      this.metrics.jsHeapSizeLimit = memory.jsHeapSizeLimit;
      this.hasMemoryInfo = true;
    }
  }

  private collectNetworkMetrics(): void {
    if ((navigator as any).connection) {
      const connection = (navigator as any).connection;
      this.metrics.connectionType = connection.type;
      this.metrics.effectiveType = connection.effectiveType;
      this.metrics.downlink = connection.downlink;
      this.metrics.rtt = connection.rtt;
      this.hasNetworkInfo = true;
    }
  }

  private async collectWebVitals(): Promise<void> {
    try {
      // 使用基本的 Web Vitals 指標獲取
      this.webVitalsReport = this.getBasicWebVitals();

      // 更新本地指標對象以保持向後兼容
      if (this.webVitalsReport.lcp) {
        this.metrics.lcp = this.webVitalsReport.lcp.value;
      }
      if (this.webVitalsReport.fid) {
        this.metrics.fid = this.webVitalsReport.fid.value;
      }
      if (this.webVitalsReport.cls) {
        this.metrics.cls = this.webVitalsReport.cls.value;
      }
      if (this.webVitalsReport.fcp) {
        this.metrics.firstContentfulPaint = this.webVitalsReport.fcp.value;
      }
      if (this.webVitalsReport.ttfb) {
        this.metrics.ttfb = this.webVitalsReport.ttfb.value;
      }
    } catch (error) {
      console.warn('Error collecting Web Vitals:', error);
    }
  }

  private setupPerformanceObserver(): void {
    if (!('PerformanceObserver' in window)) return;

    try {
      // LCP Observer
      const lcpObserver = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        const lastEntry = entries[entries.length - 1] as any;
        if (lastEntry) {
          this.metrics.lcp = lastEntry.startTime;
          this.cdr.markForCheck();
        }
      });
      lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });

      // FID Observer
      const fidObserver = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        entries.forEach((entry: any) => {
          this.metrics.fid = entry.processingStart - entry.startTime;
          this.cdr.markForCheck();
        });
      });
      fidObserver.observe({ entryTypes: ['first-input'] });

      // CLS Observer
      let clsValue = 0;
      const clsObserver = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        entries.forEach((entry: any) => {
          if (!entry.hadRecentInput) {
            clsValue += entry.value;
          }
        });
        this.metrics.cls = clsValue;
        this.cdr.markForCheck();
      });
      clsObserver.observe({ entryTypes: ['layout-shift'] });

      this.performanceObserver = lcpObserver; // 保存一個引用用於清理

    } catch (error) {
      console.warn('Performance Observer setup failed:', error);
    }
  }

  private updateBatchStatus(): void {
    this.batchStatus = this.batchProcessor.getStatus();
  }

  private checkBrowserCapabilities(): void {
    this.hasMemoryInfo = !!(performance as any).memory;
    this.hasNetworkInfo = !!(navigator as any).connection;
  }

  private cleanup(): void {
    if (this.updateSubscription) {
      this.updateSubscription.unsubscribe();
    }

    if (this.performanceObserver) {
      this.performanceObserver.disconnect();
    }
  }

  // 格式化方法
  formatTime(value?: number): string {
    if (value === undefined || value === null) return 'N/A';
    return `${Math.round(value)}ms`;
  }

  formatCLS(value?: number): string {
    if (value === undefined || value === null) return 'N/A';
    return value.toFixed(3);
  }

  formatMemory(value?: number): string {
    if (value === undefined || value === null) return 'N/A';
    return `${Math.round(value / 1024 / 1024)}MB`;
  }

  formatSpeed(value?: number): string {
    if (value === undefined || value === null) return 'N/A';
    return `${value}Mbps`;
  }

  // Web Vitals 評分方法 (基本實現)
  isGoodLCP(value?: number): boolean {
    return value !== undefined && value <= 2500; // Good LCP threshold
  }

  isPoorLCP(value?: number): boolean {
    return value !== undefined && value > 4000; // Poor LCP threshold
  }

  isGoodFID(value?: number): boolean {
    return value !== undefined && value <= 100; // Good FID threshold
  }

  isPoorFID(value?: number): boolean {
    return value !== undefined && value > 300; // Poor FID threshold
  }

  isGoodCLS(value?: number): boolean {
    return value !== undefined && value <= 0.1; // Good CLS threshold
  }

  isPoorCLS(value?: number): boolean {
    return value !== undefined && value > 0.25; // Poor CLS threshold
  }

  // 基本的 Web Vitals 獲取方法
  private getBasicWebVitals(): WebVitalsReport {
    return {
      lcp: this.metrics.lcp ? {
        value: this.metrics.lcp,
        rating: this.isGoodLCP(this.metrics.lcp) ? 'good' :
          this.isPoorLCP(this.metrics.lcp) ? 'poor' : 'needs-improvement'
      } : undefined,
      fid: this.metrics.fid ? {
        value: this.metrics.fid,
        rating: this.isGoodFID(this.metrics.fid) ? 'good' :
          this.isPoorFID(this.metrics.fid) ? 'poor' : 'needs-improvement'
      } : undefined,
      cls: this.metrics.cls ? {
        value: this.metrics.cls,
        rating: this.isGoodCLS(this.metrics.cls) ? 'good' :
          this.isPoorCLS(this.metrics.cls) ? 'poor' : 'needs-improvement'
      } : undefined,
      fcp: this.metrics.firstContentfulPaint ? {
        value: this.metrics.firstContentfulPaint,
        rating: this.metrics.firstContentfulPaint <= 1800 ? 'good' :
          this.metrics.firstContentfulPaint > 3000 ? 'poor' : 'needs-improvement'
      } : undefined,
      ttfb: this.metrics.ttfb ? {
        value: this.metrics.ttfb,
        rating: this.metrics.ttfb <= 800 ? 'good' :
          this.metrics.ttfb > 1800 ? 'poor' : 'needs-improvement'
      } : undefined
    };
  }
}