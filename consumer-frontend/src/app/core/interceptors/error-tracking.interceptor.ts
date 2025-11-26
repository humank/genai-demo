import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ErrorTrackingService } from '../services/error-tracking.service';
import { ObservabilityConfigService } from '../config/observability.config';

@Injectable()
export class ErrorTrackingInterceptor implements HttpInterceptor {

    constructor(
        private errorTrackingService: ErrorTrackingService,
        private configService: ObservabilityConfigService
    ) { }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Skip tracking for analytics/error endpoints to prevent infinite loops
        const endpoints = this.configService.getApiEndpoints();
        if (req.url.includes(endpoints.analytics) || req.url.includes(endpoints.performance) || req.url.includes(endpoints.error)) {
            return next.handle(req);
        }

        return next.handle(req).pipe(
            catchError((error: HttpErrorResponse) => {
                // Track the API error
                this.errorTrackingService.trackApiError({
                    url: req.url,
                    method: req.method,
                    status: error.status,
                    statusText: error.statusText,
                    message: error.message,
                    responseBody: this.sanitizeResponseBody(error.error),
                    requestBody: this.sanitizeRequestBody(req.body)
                }, {
                    component: 'HttpInterceptor',
                    action: 'api_call',
                    additionalData: {
                        headers: this.sanitizeHeaders(req.headers.keys().reduce((acc, key) => {
                            acc[key] = req.headers.get(key) || '';
                            return acc;
                        }, {} as Record<string, string>))
                    }
                });

                // Re-throw the error so it can be handled by the calling code
                return throwError(() => error);
            })
        );
    }

    /**
     * 清理回應內容，移除敏感資訊
     */
    private sanitizeResponseBody(body: any): any {
        if (!body) return body;

        try {
            const sanitized = { ...body };

            // Remove sensitive fields
            const sensitiveFields = ['password', 'token', 'secret', 'key', 'authorization'];
            sensitiveFields.forEach(field => {
                if (sanitized[field]) {
                    sanitized[field] = '[REDACTED]';
                }
            });

            return sanitized;
        } catch {
            return '[UNABLE_TO_SANITIZE]';
        }
    }

    /**
     * 清理請求內容，移除敏感資訊
     */
    private sanitizeRequestBody(body: any): any {
        if (!body) return body;

        try {
            const sanitized = { ...body };

            // Remove sensitive fields
            const sensitiveFields = ['password', 'token', 'secret', 'key', 'authorization', 'creditCard'];
            sensitiveFields.forEach(field => {
                if (sanitized[field]) {
                    sanitized[field] = '[REDACTED]';
                }
            });

            return sanitized;
        } catch {
            return '[UNABLE_TO_SANITIZE]';
        }
    }

    /**
     * 清理請求標頭，移除敏感資訊
     */
    private sanitizeHeaders(headers: Record<string, string>): Record<string, string> {
        const sanitized = { ...headers };

        // Remove sensitive headers
        const sensitiveHeaders = ['authorization', 'cookie', 'x-api-key', 'x-auth-token'];
        sensitiveHeaders.forEach(header => {
            if (sanitized[header]) {
                sanitized[header] = '[REDACTED]';
            }
        });

        return sanitized;
    }
}