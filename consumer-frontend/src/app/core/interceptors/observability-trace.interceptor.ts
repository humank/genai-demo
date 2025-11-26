import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';
import { ApiMonitoringService } from '../services/api-monitoring.service';
import { ObservabilityService } from '../services/observability.service';
import { SessionService } from '../services/session.service';
import { ObservabilityConfigService } from '../config/observability.config';

@Injectable()
export class ObservabilityTraceInterceptor implements HttpInterceptor {

  constructor(
    private sessionService: SessionService,
    private observabilityService: ObservabilityService,
    private apiMonitoringService: ApiMonitoringService,
    private configService: ObservabilityConfigService
  ) { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip tracking for analytics endpoints to prevent infinite loops
    const endpoints = this.configService.getApiEndpoints();
    if (req.url.includes(endpoints.analytics) || req.url.includes(endpoints.performance) || req.url.includes(endpoints.error)) {
      return next.handle(req);
    }

    // 生成或獲取追蹤 ID
    const traceId = this.sessionService.generateNewTraceId();
    const sessionId = this.sessionService.getSessionId();
    const correlationId = traceId; // 使用 traceId 作為 correlationId，與後端 MDC 系統整合

    // 記錄請求開始時間和 API 監控
    const startTime = performance.now();
    const requestSize = this.getRequestSize(req);
    const apiCallId = this.apiMonitoringService.recordApiCallStart(
      req.method,
      req.url,
      traceId,
      requestSize
    );

    // 設定追蹤標頭，與後端 MDC 系統整合
    const tracedReq = req.clone({
      setHeaders: {
        'X-Trace-Id': traceId,        // 對應後端 MDC traceId
        'X-Session-Id': sessionId,    // 對應後端 MDC sessionId  
        'X-Correlation-Id': correlationId, // 對應後端 MDC correlationId
        'X-Request-Id': this.generateRequestId(), // 額外的請求 ID
        'X-User-Agent': navigator.userAgent,
        'X-Timestamp': new Date().toISOString(),
        'X-Api-Call-Id': apiCallId    // API 監控 ID
      }
    });

    // 追蹤 API 呼叫開始
    this.trackApiCallStart(tracedReq, traceId);

    return next.handle(tracedReq).pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse) {
            // 追蹤成功響應
            const duration = performance.now() - startTime;
            const responseSize = this.getResponseSize(event);

            // 記錄到 API 監控服務
            this.apiMonitoringService.recordApiCallComplete(
              apiCallId,
              req.method,
              req.url,
              event.status,
              duration,
              traceId,
              responseSize
            );

            this.trackApiCallSuccess(tracedReq, event, duration, traceId);
          }
        },
        error: (error: HttpErrorResponse) => {
          // 追蹤錯誤響應
          const duration = performance.now() - startTime;

          // 記錄到 API 監控服務
          this.apiMonitoringService.recordApiCallComplete(
            apiCallId,
            req.method,
            req.url,
            error.status || 0,
            duration,
            traceId,
            undefined,
            error.message
          );

          this.trackApiCallError(tracedReq, error, duration, traceId);
        }
      }),
      finalize(() => {
        // 請求完成後的清理工作
        const duration = performance.now() - startTime;
        this.trackApiCallComplete(tracedReq, duration, traceId);
      })
    );
  }

  private trackApiCallStart(req: HttpRequest<any>, traceId: string): void {
    this.observabilityService.trackUserAction('api_call_start', {
      method: req.method,
      url: req.url,
      traceId: traceId,
      hasBody: !!req.body,
      contentType: req.headers.get('Content-Type'),
      timestamp: Date.now()
    });
  }

  private trackApiCallSuccess(
    req: HttpRequest<any>,
    response: HttpResponse<any>,
    duration: number,
    traceId: string
  ): void {
    // 追蹤 API 效能指標
    this.observabilityService.trackPerformanceMetric({
      type: 'ttfb', // Time to First Byte
      value: duration,
      page: window.location.pathname,
      timestamp: Date.now()
    });

    // 追蹤 API 呼叫成功
    this.observabilityService.trackUserAction('api_call_success', {
      method: req.method,
      url: req.url,
      status: response.status,
      statusText: response.statusText,
      duration: duration,
      traceId: traceId,
      responseSize: this.getResponseSize(response),
      contentType: response.headers.get('Content-Type'),
      timestamp: Date.now()
    });

    // 如果是業務相關的 API，追蹤業務事件
    this.trackBusinessApiCall(req, response, duration, traceId);
  }

  private trackApiCallError(
    req: HttpRequest<any>,
    error: HttpErrorResponse,
    duration: number,
    traceId: string
  ): void {
    // 追蹤 API 錯誤
    this.observabilityService.trackUserAction('api_call_error', {
      method: req.method,
      url: req.url,
      status: error.status,
      statusText: error.statusText,
      errorMessage: error.message,
      duration: duration,
      traceId: traceId,
      errorType: this.getErrorType(error),
      timestamp: Date.now()
    });

    // 如果是網路錯誤，特別標記
    if (error.status === 0) {
      this.observabilityService.trackUserAction('network_error', {
        url: req.url,
        traceId: traceId,
        timestamp: Date.now()
      });
    }
  }

  private trackApiCallComplete(req: HttpRequest<any>, duration: number, traceId: string): void {
    // 追蹤 API 呼叫完成 (無論成功或失敗)
    this.observabilityService.trackUserAction('api_call_complete', {
      method: req.method,
      url: req.url,
      duration: duration,
      traceId: traceId,
      timestamp: Date.now()
    });
  }

  private trackBusinessApiCall(
    req: HttpRequest<any>,
    response: HttpResponse<any>,
    duration: number,
    traceId: string
  ): void {
    const url = req.url.toLowerCase();

    // 商品相關 API
    if (url.includes('/products')) {
      if (req.method === 'GET' && url.includes('/search')) {
        // 商品搜尋 - 檢查 /products/search 路徑
        this.observabilityService.trackBusinessEvent({
          type: 'search',
          data: {
            searchParams: req.params,
            apiDuration: duration,
            traceId: traceId
          },
          timestamp: Date.now(),
          sessionId: this.sessionService.getSessionId()
        });
      } else if (req.method === 'GET' && url.includes('/products/')) {
        // 商品詳情查看 - 但排除搜尋路徑
        if (!url.includes('/search')) {
          const productId = this.extractProductId(req.url);
          if (productId) {
            this.observabilityService.trackBusinessEvent({
              type: 'product_view',
              data: {
                productId: productId,
                apiDuration: duration,
                traceId: traceId
              },
              timestamp: Date.now(),
              sessionId: this.sessionService.getSessionId()
            });
          }
        }
      }
    }

    // 購物車相關 API
    if (url.includes('/cart')) {
      if (req.method === 'POST') {
        this.observabilityService.trackBusinessEvent({
          type: 'cart_add',
          data: {
            cartData: req.body,
            apiDuration: duration,
            traceId: traceId
          },
          timestamp: Date.now(),
          sessionId: this.sessionService.getSessionId()
        });
      }
    }

    // 訂單相關 API
    if (url.includes('/orders') && req.method === 'POST') {
      this.observabilityService.trackBusinessEvent({
        type: 'purchase_complete',
        data: {
          orderData: req.body,
          apiDuration: duration,
          traceId: traceId
        },
        timestamp: Date.now(),
        sessionId: this.sessionService.getSessionId()
      });
    }
  }

  private generateRequestId(): string {
    return `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private getResponseSize(response: HttpResponse<any>): number {
    const contentLength = response.headers.get('Content-Length');
    if (contentLength) {
      return parseInt(contentLength, 10);
    }

    // 估算響應大小
    if (response.body) {
      try {
        return JSON.stringify(response.body).length;
      } catch {
        return 0;
      }
    }

    return 0;
  }

  private getErrorType(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'network_error';
    } else if (error.status >= 400 && error.status < 500) {
      return 'client_error';
    } else if (error.status >= 500) {
      return 'server_error';
    } else {
      return 'unknown_error';
    }
  }

  private extractProductId(url: string): string | null {
    const match = url.match(/\/products\/([^\/\?]+)/);
    return match ? match[1] : null;
  }

  private getRequestSize(req: HttpRequest<any>): number {
    if (!req.body) {
      return 0;
    }

    try {
      if (typeof req.body === 'string') {
        return req.body.length;
      } else if (req.body instanceof FormData) {
        // FormData 大小估算比較複雜，這裡返回一個估算值
        return 1024; // 1KB 估算
      } else {
        return JSON.stringify(req.body).length;
      }
    } catch {
      return 0;
    }
  }
}