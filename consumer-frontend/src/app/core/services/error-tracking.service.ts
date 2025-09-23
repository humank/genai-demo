import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable } from '@angular/core';
import { ToastService } from '../../shared/services/toast.service';
import { ObservabilityService } from './observability.service';

export interface ErrorContext {
    component?: string;
    action?: string;
    userId?: string;
    sessionId?: string;
    url?: string;
    userAgent?: string;
    timestamp?: number;
    additionalData?: Record<string, any>;
}

export interface JavaScriptError {
    message: string;
    stack?: string;
    filename?: string;
    lineno?: number;
    colno?: number;
    error?: Error;
    context?: ErrorContext;
}

export interface ApiError {
    url: string;
    method: string;
    status: number;
    statusText: string;
    message: string;
    responseBody?: any;
    requestBody?: any;
    context?: ErrorContext;
}

export interface ImageLoadError {
    src: string;
    alt?: string;
    element?: string;
    context?: ErrorContext;
}

export interface UserOperationError {
    operation: string;
    errorType: 'validation' | 'network' | 'permission' | 'timeout' | 'unknown';
    message: string;
    context?: ErrorContext;
}

@Injectable({
    providedIn: 'root'
})
export class ErrorTrackingService {
    private errorQueue: any[] = [];
    private maxQueueSize = 100;
    private flushInterval = 30000; // 30 seconds
    private retryAttempts = 3;

    constructor(
        private observabilityService: ObservabilityService,
        private toastService: ToastService
    ) {
        this.setupGlobalErrorHandling();
        this.setupUnhandledRejectionHandling();
        this.startErrorQueueProcessor();
    }

    /**
     * 設置全域錯誤處理
     */
    private setupGlobalErrorHandling(): void {
        // Override console.error to capture console errors
        const originalConsoleError = console.error;
        console.error = (...args: any[]) => {
            this.trackConsoleError(args);
            originalConsoleError.apply(console, args);
        };

        // Listen for unhandled errors
        window.addEventListener('error', (event) => {
            this.trackJavaScriptError({
                message: event.message,
                filename: event.filename,
                lineno: event.lineno,
                colno: event.colno,
                error: event.error,
                stack: event.error?.stack
            });
        });
    }

    /**
     * 設置未處理的 Promise 拒絕處理
     */
    private setupUnhandledRejectionHandling(): void {
        window.addEventListener('unhandledrejection', (event) => {
            this.trackJavaScriptError({
                message: `Unhandled Promise Rejection: ${event.reason}`,
                stack: event.reason?.stack,
                error: event.reason
            });
        });
    }

    /**
     * 追蹤 JavaScript 錯誤
     */
    trackJavaScriptError(error: Omit<JavaScriptError, 'context'>, context?: ErrorContext): void {
        const fullError: JavaScriptError = {
            ...error,
            context: {
                ...context,
                url: window.location.href,
                userAgent: navigator.userAgent,
                timestamp: Date.now(),
                sessionId: this.observabilityService.getSessionId()
            }
        };

        // Add to error queue
        this.addToErrorQueue('javascript_error', fullError);

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'error_occurred',
            data: {
                errorType: 'javascript',
                message: fullError.message,
                filename: fullError.filename,
                lineno: fullError.lineno,
                colno: fullError.colno,
                stack: this.sanitizeStack(fullError.stack),
                component: context?.component,
                action: context?.action
            },
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        });

        // Show user feedback for critical errors
        if (this.isCriticalError(fullError)) {
            this.showErrorFeedback('發生了一個意外錯誤，請重新整理頁面或稍後再試。');
        }
    }

    /**
     * 追蹤 API 錯誤
     */
    trackApiError(error: Omit<ApiError, 'context'>, context?: ErrorContext): void {
        const fullError: ApiError = {
            ...error,
            context: {
                ...context,
                url: window.location.href,
                userAgent: navigator.userAgent,
                timestamp: Date.now(),
                sessionId: this.observabilityService.getSessionId()
            }
        };

        // Add to error queue
        this.addToErrorQueue('api_error', fullError);

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'api_error',
            data: {
                url: this.sanitizeUrl(fullError.url),
                method: fullError.method,
                status: fullError.status,
                statusText: fullError.statusText,
                message: fullError.message,
                component: context?.component,
                action: context?.action,
                errorCategory: this.categorizeApiError(fullError.status)
            },
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        });

        // Show appropriate user feedback
        this.showApiErrorFeedback(fullError);
    }

    /**
     * 追蹤圖片載入錯誤
     */
    trackImageLoadError(error: Omit<ImageLoadError, 'context'>, context?: ErrorContext): void {
        const fullError: ImageLoadError = {
            ...error,
            context: {
                ...context,
                url: window.location.href,
                userAgent: navigator.userAgent,
                timestamp: Date.now(),
                sessionId: this.observabilityService.getSessionId()
            }
        };

        // Add to error queue
        this.addToErrorQueue('image_load_error', fullError);

        // Track as user action
        this.observabilityService.trackUserAction('image_load_error', {
            imageSrc: this.sanitizeUrl(fullError.src),
            imageAlt: fullError.alt,
            element: fullError.element,
            component: context?.component,
            section: context?.additionalData?.['section']
        });

        // Track as business event for critical images
        if (this.isCriticalImage(fullError.src)) {
            this.observabilityService.trackBusinessEvent({
                type: 'critical_resource_failure',
                data: {
                    resourceType: 'image',
                    resourceUrl: this.sanitizeUrl(fullError.src),
                    component: context?.component
                },
                timestamp: Date.now(),
                sessionId: this.observabilityService.getSessionId()
            });
        }
    }

    /**
     * 追蹤用戶操作錯誤
     */
    trackUserOperationError(error: Omit<UserOperationError, 'context'>, context?: ErrorContext): void {
        const fullError: UserOperationError = {
            ...error,
            context: {
                ...context,
                url: window.location.href,
                userAgent: navigator.userAgent,
                timestamp: Date.now(),
                sessionId: this.observabilityService.getSessionId()
            }
        };

        // Add to error queue
        this.addToErrorQueue('user_operation_error', fullError);

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'user_operation_failure',
            data: {
                operation: fullError.operation,
                errorType: fullError.errorType,
                message: fullError.message,
                component: context?.component,
                action: context?.action
            },
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        });

        // Show user feedback
        this.showOperationErrorFeedback(fullError);
    }

    /**
     * 追蹤動畫失敗
     */
    trackAnimationError(animationType: string, element: string, error: string, context?: ErrorContext): void {
        const fullContext: ErrorContext = {
            ...context,
            url: window.location.href,
            userAgent: navigator.userAgent,
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        };

        // Track as user action
        this.observabilityService.trackUserAction('animation_error', {
            animationType,
            element,
            error,
            component: context?.component,
            section: context?.additionalData?.['section']
        });

        // Add to error queue
        this.addToErrorQueue('animation_error', {
            animationType,
            element,
            error,
            context: fullContext
        });
    }

    /**
     * 追蹤網路錯誤
     */
    trackNetworkError(url: string, error: string, context?: ErrorContext): void {
        const fullContext: ErrorContext = {
            ...context,
            url: window.location.href,
            userAgent: navigator.userAgent,
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        };

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'network_error',
            data: {
                targetUrl: this.sanitizeUrl(url),
                error,
                component: context?.component,
                action: context?.action
            },
            timestamp: Date.now(),
            sessionId: this.observabilityService.getSessionId()
        });

        // Add to error queue
        this.addToErrorQueue('network_error', {
            url: this.sanitizeUrl(url),
            error,
            context: fullContext
        });

        // Show network error feedback
        this.showNetworkErrorFeedback();
    }

    /**
     * 追蹤控制台錯誤
     */
    private trackConsoleError(args: any[]): void {
        const message = args.map(arg =>
            typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
        ).join(' ');

        this.trackJavaScriptError({
            message: `Console Error: ${message}`,
            stack: new Error().stack
        });
    }

    /**
     * 添加錯誤到佇列
     */
    private addToErrorQueue(type: string, error: any): void {
        this.errorQueue.push({
            type,
            error,
            timestamp: Date.now(),
            retryCount: 0
        });

        // Limit queue size
        if (this.errorQueue.length > this.maxQueueSize) {
            this.errorQueue.shift();
        }
    }

    /**
     * 開始錯誤佇列處理器
     */
    private startErrorQueueProcessor(): void {
        setInterval(() => {
            this.processErrorQueue();
        }, this.flushInterval);
    }

    /**
     * 處理錯誤佇列
     */
    private processErrorQueue(): void {
        if (this.errorQueue.length === 0) return;

        const errorsToProcess = [...this.errorQueue];
        this.errorQueue = [];

        // Group errors by type for batch processing
        const groupedErrors = errorsToProcess.reduce((groups, item) => {
            const key = item.type;
            if (!groups[key]) groups[key] = [];
            groups[key].push(item);
            return groups;
        }, {} as Record<string, any[]>);

        // Process each group
        Object.entries(groupedErrors).forEach(([type, errors]) => {
            this.processBatchErrors(type, errors as any[]);
        });
    }

    /**
     * 批次處理錯誤
     */
    private processBatchErrors(type: string, errors: any[]): void {
        // This would typically send errors to a logging service
        console.log(`Processing ${errors.length} ${type} errors:`, errors);

        // Track batch processing
        this.observabilityService.trackUserAction('error_batch_processed', {
            errorType: type,
            errorCount: errors.length,
            timestamp: Date.now()
        });
    }

    /**
     * 判斷是否為關鍵錯誤
     */
    private isCriticalError(error: JavaScriptError): boolean {
        const criticalPatterns = [
            /chunk.*failed/i,
            /network.*error/i,
            /script.*error/i,
            /cannot.*read.*property/i,
            /undefined.*is.*not.*function/i
        ];

        return criticalPatterns.some(pattern => pattern.test(error.message));
    }

    /**
     * 判斷是否為關鍵圖片
     */
    private isCriticalImage(src: string): boolean {
        const criticalPatterns = [
            /logo/i,
            /hero/i,
            /banner/i,
            /featured/i
        ];

        return criticalPatterns.some(pattern => pattern.test(src));
    }

    /**
     * 分類 API 錯誤
     */
    private categorizeApiError(status: number): string {
        if (status >= 400 && status < 500) return 'client_error';
        if (status >= 500) return 'server_error';
        if (status === 0) return 'network_error';
        return 'unknown_error';
    }

    /**
     * 清理堆疊追蹤
     */
    private sanitizeStack(stack?: string): string {
        if (!stack) return '';

        // Remove sensitive information from stack traces
        return stack
            .split('\n')
            .map(line => line.replace(/https?:\/\/[^\s]+/g, '[URL]'))
            .slice(0, 10) // Limit stack trace length
            .join('\n');
    }

    /**
     * 清理 URL
     */
    private sanitizeUrl(url: string): string {
        try {
            const urlObj = new URL(url);
            return `${urlObj.origin}${urlObj.pathname}`;
        } catch {
            return url.split('?')[0];
        }
    }

    /**
     * 顯示錯誤回饋
     */
    private showErrorFeedback(message: string): void {
        this.toastService.quickError(message);
    }

    /**
     * 顯示 API 錯誤回饋
     */
    private showApiErrorFeedback(error: ApiError): void {
        let message = '操作失敗，請稍後再試。';

        switch (error.status) {
            case 400:
                message = '請求格式錯誤，請檢查輸入資料。';
                break;
            case 401:
                message = '請先登入後再進行此操作。';
                break;
            case 403:
                message = '您沒有權限執行此操作。';
                break;
            case 404:
                message = '找不到請求的資源。';
                break;
            case 429:
                message = '請求過於頻繁，請稍後再試。';
                break;
            case 500:
                message = '伺服器發生錯誤，請稍後再試。';
                break;
            case 0:
                message = '網路連線異常，請檢查網路設定。';
                break;
        }

        this.toastService.quickError(message);
    }

    /**
     * 顯示操作錯誤回饋
     */
    private showOperationErrorFeedback(error: UserOperationError): void {
        let message = error.message;

        switch (error.errorType) {
            case 'validation':
                message = `輸入驗證失敗：${error.message}`;
                break;
            case 'network':
                message = '網路連線異常，請稍後再試。';
                break;
            case 'permission':
                message = '您沒有權限執行此操作。';
                break;
            case 'timeout':
                message = '操作逾時，請稍後再試。';
                break;
        }

        this.toastService.quickError(message);
    }

    /**
     * 顯示網路錯誤回饋
     */
    private showNetworkErrorFeedback(): void {
        this.toastService.quickError('網路連線異常，請檢查網路設定後重試。');
    }

    /**
     * 獲取錯誤統計
     */
    getErrorStats(): {
        queueSize: number;
        totalErrors: number;
        errorsByType: Record<string, number>;
    } {
        const errorsByType = this.errorQueue.reduce((stats, item) => {
            stats[item.type] = (stats[item.type] || 0) + 1;
            return stats;
        }, {} as Record<string, number>);

        return {
            queueSize: this.errorQueue.length,
            totalErrors: this.errorQueue.length,
            errorsByType
        };
    }

    /**
     * 清理錯誤追蹤服務
     */
    cleanup(): void {
        this.processErrorQueue(); // Process remaining errors
        this.errorQueue = [];
    }
}

/**
 * 全域錯誤處理器
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
    constructor(private errorTrackingService: ErrorTrackingService) { }

    handleError(error: any): void {
        console.error('Global error caught:', error);

        if (error instanceof HttpErrorResponse) {
            this.errorTrackingService.trackApiError({
                url: error.url || 'unknown',
                method: 'unknown',
                status: error.status,
                statusText: error.statusText,
                message: error.message,
                responseBody: error.error
            });
        } else {
            this.errorTrackingService.trackJavaScriptError({
                message: error.message || 'Unknown error',
                stack: error.stack,
                error: error
            });
        }
    }
}