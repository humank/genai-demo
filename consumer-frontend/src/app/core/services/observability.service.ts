import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ObservabilityConfigService } from '../config/observability.config';
import { BatchProcessorService } from './batch-processor.service';

// 可觀測性事件介面
export interface ObservabilityEvent {
  id: string;
  type: EventType;
  timestamp: number;
  sessionId: string;
  userId?: string;
  traceId: string;
  data: Record<string, any>;
}

export interface UserBehaviorEvent extends ObservabilityEvent {
  type: 'page_view' | 'user_action' | 'business_event';
  page?: string;
  action?: string;
  duration?: number;
}

export interface PerformanceEvent extends ObservabilityEvent {
  type: 'performance_metric';
  metricType: 'lcp' | 'fid' | 'cls' | 'ttfb' | 'page_load' | 'fcp' | 'inp';
  value: number;
  page: string;
}

export interface BusinessEvent {
  type: 'product_view' | 'cart_add' | 'purchase_complete' | 'search' | 'filter_apply' | 'wishlist_action' | 'newsletter_subscription' | 'error_occurred' | 'api_error' | 'network_error' | 'user_operation_failure' | 'critical_resource_failure' | 'product_interaction' | 'search_behavior' | 'conversion_funnel' | 'cart_interaction' | 'filter_usage' | 'cart_abandonment' | 'session_end';
  data: Record<string, any>;
  timestamp: number;
  sessionId: string;
  userId?: string;
}

export interface PerformanceMetric {
  type: 'lcp' | 'fid' | 'cls' | 'ttfb' | 'page_load' | 'fcp' | 'inp';
  value: number;
  page: string;
  timestamp: number;
}

export interface ObservabilityConfig {
  enabled: boolean;
  batchSize: number;
  flushInterval: number;
  apiEndpoint: string;
  debug: boolean;
}

export type EventType = 'page_view' | 'user_action' | 'business_event' | 'performance_metric';

@Injectable({
  providedIn: 'root'
})
export class ObservabilityService {
  private sessionId: string;
  private traceId: string;
  private userId?: string;
  private config: ObservabilityConfig;
  private eventQueue: ObservabilityEvent[] = [];
  private isEnabled = true;

  // 用於測試和監控的 Observable
  private eventsSubject = new BehaviorSubject<ObservabilityEvent[]>([]);
  public events$: Observable<ObservabilityEvent[]> = this.eventsSubject.asObservable();

  constructor(
    private batchProcessor: BatchProcessorService,
    private configService: ObservabilityConfigService
  ) {
    this.sessionId = this.generateSessionId();
    this.traceId = this.generateTraceId();
    this.config = this.configService.getObservabilityServiceConfig();
    this.isEnabled = this.config.enabled;

    // 配置批次處理器
    this.batchProcessor.configure(this.configService.getBatchConfig());

    this.initializePerformanceObserver();
  }

  /**
   * 配置可觀測性服務
   */
  configure(config: Partial<ObservabilityConfig>): void {
    this.config = { ...this.config, ...config };
    this.isEnabled = this.config.enabled;
  }

  /**
   * 設定用戶 ID
   */
  setUserId(userId: string): void {
    this.userId = userId;
  }

  /**
   * 追蹤頁面瀏覽
   */
  trackPageView(page: string, metadata?: Record<string, any>): void {
    if (!this.isEnabled || !this.configService.isFeatureEnabled('userBehaviorTracking')) return;

    const event: UserBehaviorEvent = {
      id: this.generateEventId(),
      type: 'page_view',
      timestamp: Date.now(),
      sessionId: this.sessionId,
      userId: this.userId,
      traceId: this.traceId,
      page,
      data: {
        url: window.location.href,
        referrer: document.referrer,
        userAgent: navigator.userAgent,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight
        },
        ...metadata
      }
    };

    this.collectEvent(event);
  }

  /**
   * 追蹤用戶操作 (點擊、滾動、表單提交)
   */
  trackUserAction(action: string, context: Record<string, any>): void {
    if (!this.isEnabled || !this.configService.isFeatureEnabled('userBehaviorTracking')) return;

    const event: UserBehaviorEvent = {
      id: this.generateEventId(),
      type: 'user_action',
      timestamp: Date.now(),
      sessionId: this.sessionId,
      userId: this.userId,
      traceId: this.traceId,
      action,
      data: {
        page: window.location.pathname,
        ...context
      }
    };

    this.collectEvent(event);
  }

  /**
   * 追蹤業務事件 (商品瀏覽、加入購物車、搜尋)
   */
  trackBusinessEvent(event: BusinessEvent): void {
    if (!this.isEnabled || !this.configService.isFeatureEnabled('businessEvents')) return;

    const observabilityEvent: UserBehaviorEvent = {
      id: this.generateEventId(),
      type: 'business_event',
      timestamp: event.timestamp || Date.now(),
      sessionId: event.sessionId || this.sessionId,
      userId: event.userId || this.userId,
      traceId: this.traceId,
      data: {
        businessEventType: event.type,
        page: window.location.pathname,
        ...event.data
      }
    };

    this.collectEvent(observabilityEvent);
  }

  /**
   * 追蹤效能指標
   */
  trackPerformanceMetric(metric: PerformanceMetric): void {
    if (!this.isEnabled || !this.configService.isFeatureEnabled('performanceMetrics')) return;

    const event: PerformanceEvent = {
      id: this.generateEventId(),
      type: 'performance_metric',
      timestamp: metric.timestamp || Date.now(),
      sessionId: this.sessionId,
      userId: this.userId,
      traceId: this.traceId,
      metricType: metric.type,
      value: metric.value,
      page: metric.page,
      data: {
        metricType: metric.type,
        value: metric.value,
        page: metric.page
      }
    };

    this.collectEvent(event);
  }

  /**
   * 手動刷新事件佇列
   */
  async flush(): Promise<void> {
    try {
      // 使用 BatchProcessor 刷新事件
      await this.batchProcessor.flush();

      // 清空本地佇列 (事件已經發送到 BatchProcessor)
      this.eventQueue = [];
      this.eventsSubject.next([]);

      if (this.config.debug) {
        console.log('ObservabilityService: Events flushed via BatchProcessor');
      }
    } catch (error) {
      console.error('Failed to flush events:', error);
      throw error;
    }
  }

  /**
   * 獲取當前會話 ID
   */
  getSessionId(): string {
    return this.sessionId;
  }

  /**
   * 獲取當前追蹤 ID
   */
  getTraceId(): string {
    return this.traceId;
  }

  /**
   * 獲取當前用戶 ID
   */
  getUserId(): string | undefined {
    return this.userId;
  }

  /**
   * 獲取事件佇列長度 (用於測試)
   */
  getQueueLength(): number {
    return this.eventQueue.length;
  }

  /**
   * 清空事件佇列 (用於測試)
   */
  clearQueue(): void {
    this.eventQueue = [];
    this.eventsSubject.next([]);
  }

  private collectEvent(event: ObservabilityEvent): void {
    this.eventQueue.push(event);
    this.eventsSubject.next([...this.eventQueue]);

    // 將事件發送到批次處理器
    this.batchProcessor.addEvent(event);

    if (this.config.debug) {
      console.log('ObservabilityService: Event collected', event);
    }
  }

  private generateSessionId(): string {
    return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private generateTraceId(): string {
    return `trace-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private generateEventId(): string {
    return `event-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private getDefaultConfig(): ObservabilityConfig {
    return {
      enabled: true,
      batchSize: 50,
      flushInterval: 30000, // 30 seconds
      apiEndpoint: '/api/analytics/events',
      debug: false
    };
  }

  private initializePerformanceObserver(): void {
    // 檢查效能監控是否啟用
    if (!this.configService.isPerformanceMonitoringEnabled()) {
      return;
    }

    // Web Vitals 收集現在由專用的 WebVitalsService 處理
    // 這裡保留基本的效能監控初始化，主要用於向後兼容
    if (typeof window !== 'undefined') {
      if (this.config.debug) {
        console.log('ObservabilityService: Performance monitoring initialized, Web Vitals handled by WebVitalsService');
      }
    }
  }
}