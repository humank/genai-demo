import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ObservabilityConfigService } from '../config/observability.config';
import { ObservabilityService } from './observability.service';

export interface ApiCallMetric {
  id: string;
  method: string;
  url: string;
  status: number;
  duration: number;
  timestamp: number;
  traceId: string;
  requestSize?: number;
  responseSize?: number;
  errorType?: string;
  errorMessage?: string;
  retryCount?: number;
}

export interface ApiEndpointStats {
  endpoint: string;
  method: string;
  totalCalls: number;
  successCount: number;
  errorCount: number;
  averageResponseTime: number;
  minResponseTime: number;
  maxResponseTime: number;
  errorRate: number;
  lastCallTimestamp: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
}

export interface ApiMonitoringStats {
  totalApiCalls: number;
  totalErrors: number;
  overallErrorRate: number;
  averageResponseTime: number;
  slowestEndpoints: ApiEndpointStats[];
  errorProneEndpoints: ApiEndpointStats[];
  recentErrors: ApiCallMetric[];
  timeWindowMinutes: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiMonitoringService {
  private apiCalls: ApiCallMetric[] = [];
  private endpointStats = new Map<string, ApiEndpointStats>();
  private maxStoredCalls = 1000; // 最多儲存 1000 個 API 呼叫記錄
  private timeWindowMs = 30 * 60 * 1000; // 30 分鐘時間窗口

  // 即時統計 Observable
  private statsSubject = new BehaviorSubject<ApiMonitoringStats>(this.getEmptyStats());
  public stats$: Observable<ApiMonitoringStats> = this.statsSubject.asObservable();

  // 錯誤率閾值
  private readonly errorRateThresholds = {
    warning: 0.05, // 5%
    critical: 0.15  // 15%
  };

  // 響應時間閾值 (毫秒)
  private readonly responseTimeThresholds = {
    fast: 500,
    slow: 2000,
    critical: 5000
  };

  constructor(
    private observabilityService: ObservabilityService,
    private configService: ObservabilityConfigService
  ) {
    this.startPeriodicStatsUpdate();
  }

  /**
   * 記錄 API 呼叫開始
   */
  recordApiCallStart(
    method: string,
    url: string,
    traceId: string,
    requestSize?: number
  ): string {
    const callId = this.generateCallId();
    
    if (this.configService.isDebugEnabled()) {
      console.log(`ApiMonitoringService: API call started - ${method} ${url} [${traceId}]`);
    }

    return callId;
  }

  /**
   * 記錄 API 呼叫完成
   */
  recordApiCallComplete(
    callId: string,
    method: string,
    url: string,
    status: number,
    duration: number,
    traceId: string,
    responseSize?: number,
    errorMessage?: string,
    retryCount?: number
  ): void {
    const metric: ApiCallMetric = {
      id: callId,
      method,
      url: this.normalizeUrl(url),
      status,
      duration,
      timestamp: Date.now(),
      traceId,
      responseSize,
      errorType: status >= 400 ? this.getErrorType(status) : undefined,
      errorMessage,
      retryCount
    };

    this.addApiCall(metric);
    this.updateEndpointStats(metric);
    this.trackPerformanceMetric(metric);
    this.checkAlerts(metric);

    if (this.configService.isDebugEnabled()) {
      console.log(`ApiMonitoringService: API call completed - ${method} ${url} [${status}] ${duration}ms [${traceId}]`);
    }
  }

  /**
   * 獲取當前 API 監控統計
   */
  getCurrentStats(): ApiMonitoringStats {
    return this.calculateStats();
  }

  /**
   * 獲取特定端點的統計資訊
   */
  getEndpointStats(endpoint: string, method?: string): ApiEndpointStats | undefined {
    const key = method ? `${method}:${endpoint}` : endpoint;
    return this.endpointStats.get(key);
  }

  /**
   * 獲取錯誤率最高的端點
   */
  getErrorProneEndpoints(limit = 5): ApiEndpointStats[] {
    return Array.from(this.endpointStats.values())
      .filter(stats => stats.totalCalls >= 5) // 至少 5 次呼叫才考慮
      .sort((a, b) => b.errorRate - a.errorRate)
      .slice(0, limit);
  }

  /**
   * 獲取響應時間最慢的端點
   */
  getSlowestEndpoints(limit = 5): ApiEndpointStats[] {
    return Array.from(this.endpointStats.values())
      .filter(stats => stats.totalCalls >= 5)
      .sort((a, b) => b.averageResponseTime - a.averageResponseTime)
      .slice(0, limit);
  }

  /**
   * 獲取最近的錯誤
   */
  getRecentErrors(limit = 10): ApiCallMetric[] {
    return this.apiCalls
      .filter(call => call.status >= 400)
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, limit);
  }

  /**
   * 檢查 API 健康狀況
   */
  getApiHealthStatus(): 'healthy' | 'warning' | 'critical' {
    const stats = this.getCurrentStats();
    
    if (stats.overallErrorRate >= this.errorRateThresholds.critical) {
      return 'critical';
    } else if (stats.overallErrorRate >= this.errorRateThresholds.warning) {
      return 'warning';
    } else {
      return 'healthy';
    }
  }

  /**
   * 清除舊的 API 呼叫記錄
   */
  cleanup(): void {
    const cutoffTime = Date.now() - this.timeWindowMs;
    
    // 清除舊的 API 呼叫記錄
    this.apiCalls = this.apiCalls.filter(call => call.timestamp > cutoffTime);
    
    // 重新計算端點統計
    this.recalculateEndpointStats();
    
    if (this.configService.isDebugEnabled()) {
      console.log(`ApiMonitoringService: Cleaned up old records, ${this.apiCalls.length} calls remaining`);
    }
  }

  /**
   * 重置所有統計資料
   */
  reset(): void {
    this.apiCalls = [];
    this.endpointStats.clear();
    this.statsSubject.next(this.getEmptyStats());
  }

  /**
   * 匯出統計資料 (用於除錯或分析)
   */
  exportStats(): {
    calls: ApiCallMetric[];
    endpoints: ApiEndpointStats[];
    summary: ApiMonitoringStats;
  } {
    return {
      calls: [...this.apiCalls],
      endpoints: Array.from(this.endpointStats.values()),
      summary: this.getCurrentStats()
    };
  }

  private addApiCall(metric: ApiCallMetric): void {
    this.apiCalls.push(metric);
    
    // 保持記錄數量在限制內
    if (this.apiCalls.length > this.maxStoredCalls) {
      this.apiCalls = this.apiCalls.slice(-this.maxStoredCalls);
    }
  }

  private updateEndpointStats(metric: ApiCallMetric): void {
    const key = `${metric.method}:${metric.url}`;
    const existing = this.endpointStats.get(key);
    
    if (existing) {
      // 更新現有統計
      existing.totalCalls++;
      if (metric.status < 400) {
        existing.successCount++;
      } else {
        existing.errorCount++;
      }
      
      // 更新響應時間統計
      existing.averageResponseTime = this.calculateNewAverage(
        existing.averageResponseTime,
        existing.totalCalls - 1,
        metric.duration
      );
      existing.minResponseTime = Math.min(existing.minResponseTime, metric.duration);
      existing.maxResponseTime = Math.max(existing.maxResponseTime, metric.duration);
      existing.errorRate = existing.errorCount / existing.totalCalls;
      existing.lastCallTimestamp = metric.timestamp;
      
      // 計算百分位數 (簡化版本)
      this.updatePercentiles(existing, key);
    } else {
      // 創建新的統計記錄
      const newStats: ApiEndpointStats = {
        endpoint: metric.url,
        method: metric.method,
        totalCalls: 1,
        successCount: metric.status < 400 ? 1 : 0,
        errorCount: metric.status >= 400 ? 1 : 0,
        averageResponseTime: metric.duration,
        minResponseTime: metric.duration,
        maxResponseTime: metric.duration,
        errorRate: metric.status >= 400 ? 1 : 0,
        lastCallTimestamp: metric.timestamp,
        p95ResponseTime: metric.duration,
        p99ResponseTime: metric.duration
      };
      
      this.endpointStats.set(key, newStats);
    }
  }

  private updatePercentiles(stats: ApiEndpointStats, key: string): void {
    // 獲取該端點最近的響應時間
    const recentCalls = this.apiCalls
      .filter(call => `${call.method}:${call.url}` === key)
      .map(call => call.duration)
      .sort((a, b) => a - b);
    
    if (recentCalls.length > 0) {
      const p95Index = Math.floor(recentCalls.length * 0.95);
      const p99Index = Math.floor(recentCalls.length * 0.99);
      
      stats.p95ResponseTime = recentCalls[Math.min(p95Index, recentCalls.length - 1)];
      stats.p99ResponseTime = recentCalls[Math.min(p99Index, recentCalls.length - 1)];
    }
  }

  private trackPerformanceMetric(metric: ApiCallMetric): void {
    // 將 API 呼叫指標發送到可觀測性服務
    this.observabilityService.trackPerformanceMetric({
      type: 'ttfb',
      value: metric.duration,
      page: window.location.pathname,
      timestamp: metric.timestamp
    });

    // 如果是慢請求，特別標記
    if (metric.duration > this.responseTimeThresholds.slow) {
      this.observabilityService.trackUserAction('slow_api_call', {
        method: metric.method,
        url: metric.url,
        duration: metric.duration,
        traceId: metric.traceId,
        status: metric.status
      });
    }
  }

  private checkAlerts(metric: ApiCallMetric): void {
    // 檢查錯誤率警報
    const endpointKey = `${metric.method}:${metric.url}`;
    const endpointStats = this.endpointStats.get(endpointKey);
    
    if (endpointStats && endpointStats.totalCalls >= 10) {
      if (endpointStats.errorRate >= this.errorRateThresholds.critical) {
        this.triggerAlert('critical_error_rate', {
          endpoint: metric.url,
          method: metric.method,
          errorRate: endpointStats.errorRate,
          totalCalls: endpointStats.totalCalls
        });
      } else if (endpointStats.errorRate >= this.errorRateThresholds.warning) {
        this.triggerAlert('high_error_rate', {
          endpoint: metric.url,
          method: metric.method,
          errorRate: endpointStats.errorRate,
          totalCalls: endpointStats.totalCalls
        });
      }
    }

    // 檢查響應時間警報
    if (metric.duration > this.responseTimeThresholds.critical) {
      this.triggerAlert('critical_response_time', {
        endpoint: metric.url,
        method: metric.method,
        duration: metric.duration,
        traceId: metric.traceId
      });
    }
  }

  private triggerAlert(alertType: string, data: any): void {
    this.observabilityService.trackUserAction('api_alert', {
      alertType,
      ...data,
      timestamp: Date.now()
    });

    if (this.configService.isDebugEnabled()) {
      console.warn(`ApiMonitoringService: Alert triggered - ${alertType}`, data);
    }
  }

  private calculateStats(): ApiMonitoringStats {
    const cutoffTime = Date.now() - this.timeWindowMs;
    const recentCalls = this.apiCalls.filter(call => call.timestamp > cutoffTime);
    
    const totalCalls = recentCalls.length;
    const errorCalls = recentCalls.filter(call => call.status >= 400);
    const totalErrors = errorCalls.length;
    const overallErrorRate = totalCalls > 0 ? totalErrors / totalCalls : 0;
    
    const totalResponseTime = recentCalls.reduce((sum, call) => sum + call.duration, 0);
    const averageResponseTime = totalCalls > 0 ? totalResponseTime / totalCalls : 0;

    return {
      totalApiCalls: totalCalls,
      totalErrors,
      overallErrorRate,
      averageResponseTime,
      slowestEndpoints: this.getSlowestEndpoints(),
      errorProneEndpoints: this.getErrorProneEndpoints(),
      recentErrors: this.getRecentErrors(),
      timeWindowMinutes: this.timeWindowMs / (60 * 1000)
    };
  }

  private recalculateEndpointStats(): void {
    this.endpointStats.clear();
    
    this.apiCalls.forEach(call => {
      this.updateEndpointStats(call);
    });
  }

  private startPeriodicStatsUpdate(): void {
    // 每 30 秒更新一次統計
    setInterval(() => {
      this.statsSubject.next(this.calculateStats());
    }, 30000);

    // 每 5 分鐘清理一次舊記錄
    setInterval(() => {
      this.cleanup();
    }, 5 * 60 * 1000);
  }

  private normalizeUrl(url: string): string {
    // 移除查詢參數和片段
    try {
      const urlObj = new URL(url, window.location.origin);
      return urlObj.pathname;
    } catch {
      // 如果不是完整 URL，直接返回路徑部分
      return url.split('?')[0].split('#')[0];
    }
  }

  private getErrorType(status: number): string {
    if (status >= 400 && status < 500) {
      return 'client_error';
    } else if (status >= 500) {
      return 'server_error';
    } else {
      return 'unknown_error';
    }
  }

  private calculateNewAverage(currentAvg: number, count: number, newValue: number): number {
    return (currentAvg * count + newValue) / (count + 1);
  }

  private generateCallId(): string {
    return `api-call-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private getEmptyStats(): ApiMonitoringStats {
    return {
      totalApiCalls: 0,
      totalErrors: 0,
      overallErrorRate: 0,
      averageResponseTime: 0,
      slowestEndpoints: [],
      errorProneEndpoints: [],
      recentErrors: [],
      timeWindowMinutes: this.timeWindowMs / (60 * 1000)
    };
  }
}