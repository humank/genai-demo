import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ObservabilityService } from './observability.service';

export interface ApiCallMetric {
    url: string;
    method: string;
    duration: number;
    status: number;
    success: boolean;
    timestamp: number;
    requestSize?: number;
    responseSize?: number;
}

@Injectable({
    providedIn: 'root'
})
export class ApiPerformanceService {
    private activeRequests = new Map<string, number>();

    constructor(private observabilityService: ObservabilityService) { }

    /**
     * 開始追蹤 API 請求
     */
    startTracking(requestId: string, url: string, method: string): void {
        this.activeRequests.set(requestId, performance.now());
    }

    /**
     * 完成 API 請求追蹤
     */
    completeTracking(
        requestId: string,
        url: string,
        method: string,
        status: number,
        success: boolean,
        requestSize?: number,
        responseSize?: number
    ): void {
        const startTime = this.activeRequests.get(requestId);
        if (!startTime) return;

        const duration = performance.now() - startTime;
        this.activeRequests.delete(requestId);

        const metric: ApiCallMetric = {
            url,
            method,
            duration,
            status,
            success,
            timestamp: Date.now(),
            requestSize,
            responseSize
        };

        this.trackApiMetric(metric);
    }

    /**
     * 追蹤 API 指標
     */
    private trackApiMetric(metric: ApiCallMetric): void {
        // Track performance metric
        this.observabilityService.trackPerformanceMetric({
            type: 'page_load', // Using page_load type for API response time
            value: metric.duration,
            page: window.location.pathname,
            timestamp: metric.timestamp
        });

        // Track user action for API call
        this.observabilityService.trackUserAction('api_call_complete', {
            url: this.sanitizeUrl(metric.url),
            method: metric.method,
            duration: metric.duration,
            status: metric.status,
            success: metric.success,
            requestSize: metric.requestSize,
            responseSize: metric.responseSize,
            performanceRating: this.getPerformanceRating(metric.duration)
        });

        // Track business event for slow API calls
        if (metric.duration > 2000) { // Slow API call threshold
            this.observabilityService.trackBusinessEvent({
                type: 'performance_issue',
                data: {
                    issueType: 'slow_api_call',
                    url: this.sanitizeUrl(metric.url),
                    method: metric.method,
                    duration: metric.duration,
                    status: metric.status
                },
                timestamp: metric.timestamp,
                sessionId: this.observabilityService.getSessionId()
            });
        }
    }

    /**
     * 清理 URL 以移除敏感資訊
     */
    private sanitizeUrl(url: string): string {
        try {
            const urlObj = new URL(url);
            // Remove query parameters that might contain sensitive data
            const sanitizedUrl = `${urlObj.origin}${urlObj.pathname}`;

            // Replace IDs with placeholders
            return sanitizedUrl.replace(/\/\d+/g, '/{id}')
                .replace(/\/[a-f0-9-]{36}/g, '/{uuid}')
                .replace(/\/[a-zA-Z0-9]{20,}/g, '/{token}');
        } catch {
            return url.split('?')[0]; // Fallback: just remove query params
        }
    }

    /**
     * 獲取效能評級
     */
    private getPerformanceRating(duration: number): 'excellent' | 'good' | 'fair' | 'poor' {
        if (duration < 500) return 'excellent';
        if (duration < 1000) return 'good';
        if (duration < 2000) return 'fair';
        return 'poor';
    }

    /**
     * 獲取當前活躍請求數量
     */
    getActiveRequestsCount(): number {
        return this.activeRequests.size;
    }

    /**
     * 清理過期的請求追蹤
     */
    cleanupStaleRequests(): void {
        const now = performance.now();
        const staleThreshold = 30000; // 30 seconds

        for (const [requestId, startTime] of this.activeRequests.entries()) {
            if (now - startTime > staleThreshold) {
                this.activeRequests.delete(requestId);
            }
        }
    }
}

/**
 * HTTP 攔截器用於自動追蹤 API 效能
 */
@Injectable()
export class ApiPerformanceInterceptor implements HttpInterceptor {
    constructor(private apiPerformanceService: ApiPerformanceService) { }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Skip tracking for non-API requests
        if (!this.shouldTrackRequest(req)) {
            return next.handle(req);
        }

        const requestId = this.generateRequestId();
        const requestSize = this.getRequestSize(req);

        // Start tracking
        this.apiPerformanceService.startTracking(requestId, req.url, req.method);

        return next.handle(req).pipe(
            tap({
                next: (event) => {
                    if (event instanceof HttpResponse) {
                        const responseSize = this.getResponseSize(event);

                        this.apiPerformanceService.completeTracking(
                            requestId,
                            req.url,
                            req.method,
                            event.status,
                            true,
                            requestSize,
                            responseSize
                        );
                    }
                },
                error: (error) => {
                    if (error instanceof HttpErrorResponse) {
                        this.apiPerformanceService.completeTracking(
                            requestId,
                            req.url,
                            req.method,
                            error.status,
                            false,
                            requestSize
                        );
                    }
                }
            })
        );
    }

    /**
     * 判斷是否應該追蹤此請求
     */
    private shouldTrackRequest(req: HttpRequest<any>): boolean {
        // Only track API requests
        return req.url.includes('/api/') || req.url.includes('/analytics/');
    }

    /**
     * 生成請求 ID
     */
    private generateRequestId(): string {
        return `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * 獲取請求大小
     */
    private getRequestSize(req: HttpRequest<any>): number {
        if (!req.body) return 0;

        try {
            return JSON.stringify(req.body).length;
        } catch {
            return 0;
        }
    }

    /**
     * 獲取響應大小
     */
    private getResponseSize(response: HttpResponse<any>): number {
        if (!response.body) return 0;

        try {
            return JSON.stringify(response.body).length;
        } catch {
            return 0;
        }
    }
}