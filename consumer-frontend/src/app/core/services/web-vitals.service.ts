import { Injectable } from '@angular/core';
import { ObservabilityConfigService } from '../config/observability.config';
import { ObservabilityService, PerformanceMetric } from './observability.service';

export interface WebVitalsMetric {
  name: 'LCP' | 'FID' | 'CLS' | 'TTFB' | 'FCP' | 'INP';
  value: number;
  rating: 'good' | 'needs-improvement' | 'poor';
  delta: number;
  id: string;
  entries: PerformanceEntry[];
}

export interface PageLoadMetrics {
  domContentLoaded: number;
  loadComplete: number;
  firstPaint: number;
  firstContentfulPaint: number;
  navigationStart: number;
  responseStart: number;
  responseEnd: number;
}

@Injectable({
  providedIn: 'root'
})
export class WebVitalsService {
  private observers: PerformanceObserver[] = [];
  private clsValue = 0;
  private clsEntries: PerformanceEntry[] = [];
  private isInitialized = false;

  // Web Vitals 閾值 (基於 Google 建議)
  private readonly thresholds = {
    LCP: { good: 2500, poor: 4000 },
    FID: { good: 100, poor: 300 },
    CLS: { good: 0.1, poor: 0.25 },
    TTFB: { good: 800, poor: 1800 },
    FCP: { good: 1800, poor: 3000 },
    INP: { good: 200, poor: 500 }
  };

  constructor(
    private observabilityService: ObservabilityService,
    private configService: ObservabilityConfigService
  ) {
    this.initialize();
  }

  /**
   * 初始化 Web Vitals 收集
   */
  private initialize(): void {
    if (this.isInitialized || typeof window === 'undefined') {
      return;
    }

    if (!this.configService.isPerformanceMonitoringEnabled()) {
      console.log('WebVitalsService: Performance monitoring disabled');
      return;
    }

    this.isInitialized = true;
    this.setupWebVitalsObservers();
    this.setupPageLoadMetrics();
    this.setupNavigationTiming();
  }

  /**
   * 設定 Web Vitals 觀察器
   */
  private setupWebVitalsObservers(): void {
    if (!('PerformanceObserver' in window)) {
      console.warn('WebVitalsService: PerformanceObserver not supported');
      return;
    }

    try {
      this.observeLCP();
      this.observeFID();
      this.observeCLS();
      this.observeFCP();
      this.observeINP();
    } catch (error) {
      console.error('WebVitalsService: Failed to setup observers:', error);
    }
  }

  /**
   * 觀察 Largest Contentful Paint (LCP)
   */
  private observeLCP(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        const lastEntry = entries[entries.length - 1] as any;
        
        if (lastEntry) {
          const metric: WebVitalsMetric = {
            name: 'LCP',
            value: lastEntry.startTime,
            rating: this.getRating('LCP', lastEntry.startTime),
            delta: lastEntry.startTime,
            id: this.generateMetricId(),
            entries: [lastEntry]
          };

          this.reportWebVital(metric);
        }
      });

      observer.observe({ entryTypes: ['largest-contentful-paint'] });
      this.observers.push(observer);
    } catch (error) {
      console.warn('WebVitalsService: LCP observer failed:', error);
    }
  }

  /**
   * 觀察 First Input Delay (FID)
   */
  private observeFID(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        
        entries.forEach((entry: any) => {
          const fidValue = entry.processingStart - entry.startTime;
          
          const metric: WebVitalsMetric = {
            name: 'FID',
            value: fidValue,
            rating: this.getRating('FID', fidValue),
            delta: fidValue,
            id: this.generateMetricId(),
            entries: [entry]
          };

          this.reportWebVital(metric);
        });
      });

      observer.observe({ entryTypes: ['first-input'] });
      this.observers.push(observer);
    } catch (error) {
      console.warn('WebVitalsService: FID observer failed:', error);
    }
  }

  /**
   * 觀察 Cumulative Layout Shift (CLS)
   */
  private observeCLS(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        
        entries.forEach((entry: any) => {
          if (!entry.hadRecentInput) {
            this.clsValue += entry.value;
            this.clsEntries.push(entry);
          }
        });

        // 定期報告 CLS 值
        this.reportCLS();
      });

      observer.observe({ entryTypes: ['layout-shift'] });
      this.observers.push(observer);

      // 在頁面卸載時報告最終 CLS 值
      window.addEventListener('beforeunload', () => {
        this.reportCLS(true);
      });
    } catch (error) {
      console.warn('WebVitalsService: CLS observer failed:', error);
    }
  }

  /**
   * 觀察 First Contentful Paint (FCP)
   */
  private observeFCP(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        
        entries.forEach((entry: any) => {
          if (entry.name === 'first-contentful-paint') {
            const metric: WebVitalsMetric = {
              name: 'FCP',
              value: entry.startTime,
              rating: this.getRating('FCP', entry.startTime),
              delta: entry.startTime,
              id: this.generateMetricId(),
              entries: [entry]
            };

            this.reportWebVital(metric);
          }
        });
      });

      observer.observe({ entryTypes: ['paint'] });
      this.observers.push(observer);
    } catch (error) {
      console.warn('WebVitalsService: FCP observer failed:', error);
    }
  }

  /**
   * 觀察 Interaction to Next Paint (INP)
   */
  private observeINP(): void {
    try {
      // INP 需要較新的瀏覽器支援
      if (!('PerformanceEventTiming' in window)) {
        return;
      }

      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        
        entries.forEach((entry: any) => {
          if (entry.processingEnd && entry.startTime) {
            const inpValue = entry.processingEnd - entry.startTime;
            
            const metric: WebVitalsMetric = {
              name: 'INP',
              value: inpValue,
              rating: this.getRating('INP', inpValue),
              delta: inpValue,
              id: this.generateMetricId(),
              entries: [entry]
            };

            this.reportWebVital(metric);
          }
        });
      });

      observer.observe({ entryTypes: ['event'] });
      this.observers.push(observer);
    } catch (error) {
      console.warn('WebVitalsService: INP observer failed:', error);
    }
  }

  /**
   * 設定頁面載入指標
   */
  private setupPageLoadMetrics(): void {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        this.measurePageLoadMetrics();
      });
    } else {
      // DOM 已經載入完成
      setTimeout(() => this.measurePageLoadMetrics(), 0);
    }

    window.addEventListener('load', () => {
      this.measurePageLoadMetrics();
    });
  }

  /**
   * 測量頁面載入指標
   */
  private measurePageLoadMetrics(): void {
    if (!('performance' in window) || !performance.timing) {
      return;
    }

    const timing = performance.timing;
    const navigation = performance.navigation;

    const metrics: PageLoadMetrics = {
      navigationStart: timing.navigationStart,
      responseStart: timing.responseStart - timing.navigationStart,
      responseEnd: timing.responseEnd - timing.navigationStart,
      domContentLoaded: timing.domContentLoadedEventEnd - timing.navigationStart,
      loadComplete: timing.loadEventEnd - timing.navigationStart,
      firstPaint: 0,
      firstContentfulPaint: 0
    };

    // 獲取 Paint Timing
    if ('getEntriesByType' in performance) {
      const paintEntries = performance.getEntriesByType('paint');
      paintEntries.forEach((entry: any) => {
        if (entry.name === 'first-paint') {
          metrics.firstPaint = entry.startTime;
        } else if (entry.name === 'first-contentful-paint') {
          metrics.firstContentfulPaint = entry.startTime;
        }
      });
    }

    // 報告頁面載入時間
    if (metrics.loadComplete > 0) {
      this.observabilityService.trackPerformanceMetric({
        type: 'page_load',
        value: metrics.loadComplete,
        page: window.location.pathname,
        timestamp: Date.now()
      });
    }

    // 報告 TTFB (Time to First Byte)
    if (metrics.responseStart > 0) {
      const ttfbMetric: WebVitalsMetric = {
        name: 'TTFB',
        value: metrics.responseStart,
        rating: this.getRating('TTFB', metrics.responseStart),
        delta: metrics.responseStart,
        id: this.generateMetricId(),
        entries: []
      };

      this.reportWebVital(ttfbMetric);
    }

    if (this.configService.isDebugEnabled()) {
      console.log('WebVitalsService: Page load metrics:', metrics);
    }
  }

  /**
   * 設定 Navigation Timing API
   */
  private setupNavigationTiming(): void {
    if (!('performance' in window)) {
      return;
    }

    // 使用 Navigation Timing API Level 2 (如果可用)
    if ('getEntriesByType' in performance) {
      const navEntries = performance.getEntriesByType('navigation') as PerformanceNavigationTiming[];
      
      if (navEntries.length > 0) {
        const navEntry = navEntries[0];
        
        // DNS 查詢時間
        const dnsTime = navEntry.domainLookupEnd - navEntry.domainLookupStart;
        if (dnsTime > 0) {
          this.observabilityService.trackPerformanceMetric({
            type: 'ttfb', // 使用 ttfb 類型來表示網路相關指標
            value: dnsTime,
            page: window.location.pathname,
            timestamp: Date.now()
          });
        }

        // TCP 連接時間
        const tcpTime = navEntry.connectEnd - navEntry.connectStart;
        if (tcpTime > 0) {
          this.observabilityService.trackPerformanceMetric({
            type: 'ttfb',
            value: tcpTime,
            page: window.location.pathname,
            timestamp: Date.now()
          });
        }
      }
    }
  }

  /**
   * 報告 CLS 指標
   */
  private reportCLS(isFinal = false): void {
    if (this.clsValue > 0 || isFinal) {
      const metric: WebVitalsMetric = {
        name: 'CLS',
        value: this.clsValue,
        rating: this.getRating('CLS', this.clsValue),
        delta: this.clsValue,
        id: this.generateMetricId(),
        entries: [...this.clsEntries]
      };

      this.reportWebVital(metric);

      if (isFinal) {
        // 重置 CLS 值
        this.clsValue = 0;
        this.clsEntries = [];
      }
    }
  }

  /**
   * 報告 Web Vital 指標
   */
  private reportWebVital(metric: WebVitalsMetric): void {
    const performanceMetric: PerformanceMetric = {
      type: metric.name.toLowerCase() as any,
      value: metric.value,
      page: window.location.pathname,
      timestamp: Date.now()
    };

    this.observabilityService.trackPerformanceMetric(performanceMetric);

    if (this.configService.isDebugEnabled()) {
      console.log(`WebVitalsService: ${metric.name}`, {
        value: metric.value,
        rating: metric.rating,
        delta: metric.delta
      });
    }
  }

  /**
   * 獲取指標評級
   */
  private getRating(metricName: keyof typeof this.thresholds, value: number): 'good' | 'needs-improvement' | 'poor' {
    const threshold = this.thresholds[metricName];
    
    if (value <= threshold.good) {
      return 'good';
    } else if (value <= threshold.poor) {
      return 'needs-improvement';
    } else {
      return 'poor';
    }
  }

  /**
   * 生成指標 ID
   */
  private generateMetricId(): string {
    return `metric-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * 手動觸發指標收集 (用於測試)
   */
  public collectCurrentMetrics(): void {
    this.measurePageLoadMetrics();
    this.reportCLS(true);
  }

  /**
   * 清理觀察器
   */
  public cleanup(): void {
    this.observers.forEach(observer => {
      try {
        observer.disconnect();
      } catch (error) {
        console.warn('WebVitalsService: Failed to disconnect observer:', error);
      }
    });
    this.observers = [];
    this.isInitialized = false;
  }

  /**
   * 獲取當前 Web Vitals 狀態 (用於除錯)
   */
  public getStatus(): Record<string, any> {
    return {
      isInitialized: this.isInitialized,
      observersCount: this.observers.length,
      currentCLS: this.clsValue,
      clsEntriesCount: this.clsEntries.length,
      performanceSupported: 'performance' in window,
      performanceObserverSupported: 'PerformanceObserver' in window
    };
  }
}