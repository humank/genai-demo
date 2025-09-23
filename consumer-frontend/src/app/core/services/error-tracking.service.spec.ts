import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ToastService } from '../../shared/services/toast.service';
import { ErrorTrackingService, GlobalErrorHandler } from './error-tracking.service';
import { ObservabilityService } from './observability.service';

describe('ErrorTrackingService', () => {
    let service: ErrorTrackingService;
    let observabilityService: jasmine.SpyObj<ObservabilityService>;
    let toastService: jasmine.SpyObj<ToastService>;

    beforeEach(() => {
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
            'trackBusinessEvent',
            'trackUserAction',
            'getSessionId'
        ]);
        const toastSpy = jasmine.createSpyObj('ToastService', [
            'quickError',
            'quickSuccess'
        ]);

        TestBed.configureTestingModule({
            providers: [
                ErrorTrackingService,
                { provide: ObservabilityService, useValue: observabilitySpy },
                { provide: ToastService, useValue: toastSpy }
            ]
        });

        service = TestBed.inject(ErrorTrackingService);
        observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;

        observabilityService.getSessionId.and.returnValue('test-session-123');
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('JavaScript Error Tracking', () => {
        it('should track JavaScript errors', () => {
            // Given
            const error = {
                message: 'Test error',
                stack: 'Error: Test error\\n    at test.js:1:1',
                filename: 'test.js',
                lineno: 1,
                colno: 1
            };

            const context = {
                component: 'TestComponent',
                action: 'test_action'
            };

            // When
            service.trackJavaScriptError(error, context);

            // Then
            expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith({
                type: 'error_occurred',
                data: jasmine.objectContaining({
                    errorType: 'javascript',
                    message: 'Test error',
                    filename: 'test.js',
                    lineno: 1,
                    colno: 1,
                    component: 'TestComponent',
                    action: 'test_action'
                }),
                timestamp: jasmine.any(Number),
                sessionId: 'test-session-123'
            });
        });

        it('should show user feedback for critical errors', () => {
            // Given
            const criticalError = {
                message: 'Chunk load failed',
                stack: 'Error: Chunk load failed'
            };

            // When
            service.trackJavaScriptError(criticalError);

            // Then
            expect(toastService.quickError).toHaveBeenCalledWith(
                '發生了一個意外錯誤，請重新整理頁面或稍後再試。'
            );
        });
    });

    describe('API Error Tracking', () => {
        it('should track API errors', () => {
            // Given
            const apiError = {
                url: '/api/test',
                method: 'GET',
                status: 500,
                statusText: 'Internal Server Error',
                message: 'Server error occurred'
            };

            const context = {
                component: 'TestComponent',
                action: 'api_call'
            };

            // When
            service.trackApiError(apiError, context);

            // Then
            expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith({
                type: 'api_error',
                data: jasmine.objectContaining({
                    url: '/api/test',
                    method: 'GET',
                    status: 500,
                    statusText: 'Internal Server Error',
                    message: 'Server error occurred',
                    component: 'TestComponent',
                    action: 'api_call',
                    errorCategory: 'server_error'
                }),
                timestamp: jasmine.any(Number),
                sessionId: 'test-session-123'
            });
        });
    });

    describe('Error Statistics', () => {
        it('should return error statistics', () => {
            // Given - track some errors first
            service.trackJavaScriptError({ message: 'Error 1', stack: 'stack1' });

            // When
            const stats = service.getErrorStats();

            // Then
            expect(stats).toEqual(jasmine.objectContaining({
                queueSize: jasmine.any(Number),
                totalErrors: jasmine.any(Number),
                errorsByType: jasmine.any(Object)
            }));
        });
    });
});

describe('GlobalErrorHandler', () => {
    let errorHandler: GlobalErrorHandler;
    let errorTrackingService: jasmine.SpyObj<ErrorTrackingService>;

    beforeEach(() => {
        const errorTrackingSpy = jasmine.createSpyObj('ErrorTrackingService', [
            'trackApiError',
            'trackJavaScriptError'
        ]);

        TestBed.configureTestingModule({
            providers: [
                GlobalErrorHandler,
                { provide: ErrorTrackingService, useValue: errorTrackingSpy }
            ]
        });

        errorHandler = TestBed.inject(GlobalErrorHandler);
        errorTrackingService = TestBed.inject(ErrorTrackingService) as jasmine.SpyObj<ErrorTrackingService>;
    });

    it('should handle HTTP errors', () => {
        // Given
        const httpError = new HttpErrorResponse({
            error: 'Server Error',
            status: 500,
            statusText: 'Internal Server Error',
            url: '/api/test'
        });

        // When
        errorHandler.handleError(httpError);

        // Then
        expect(errorTrackingService.trackApiError).toHaveBeenCalledWith({
            url: '/api/test',
            method: 'unknown',
            status: 500,
            statusText: 'Internal Server Error',
            message: jasmine.any(String),
            responseBody: 'Server Error'
        });
    });

    it('should handle JavaScript errors', () => {
        // Given
        const jsError = new Error('Test JavaScript error');

        // When
        errorHandler.handleError(jsError);

        // Then
        expect(errorTrackingService.trackJavaScriptError).toHaveBeenCalledWith({
            message: 'Test JavaScript error',
            stack: jasmine.any(String),
            error: jsError
        });
    });
});