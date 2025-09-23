import { HTTP_INTERCEPTORS, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErrorTrackingService } from '../services/error-tracking.service';
import { ErrorTrackingInterceptor } from './error-tracking.interceptor';

describe('ErrorTrackingInterceptor', () => {
    let httpClient: HttpClient;
    let httpMock: HttpTestingController;
    let errorTrackingService: jasmine.SpyObj<ErrorTrackingService>;

    beforeEach(() => {
        const errorTrackingSpy = jasmine.createSpyObj('ErrorTrackingService', [
            'trackApiError'
        ]);

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: ErrorTrackingService, useValue: errorTrackingSpy },
                {
                    provide: HTTP_INTERCEPTORS,
                    useClass: ErrorTrackingInterceptor,
                    multi: true
                }
            ]
        });

        httpClient = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        errorTrackingService = TestBed.inject(ErrorTrackingService) as jasmine.SpyObj<ErrorTrackingService>;
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should track API errors when HTTP requests fail', () => {
        // Given
        const testUrl = '/api/products';
        const errorResponse = {
            status: 500,
            statusText: 'Internal Server Error',
            error: { message: 'Server error occurred' }
        };

        // When
        httpClient.get(testUrl).subscribe({
            next: () => fail('Should have failed'),
            error: (error: HttpErrorResponse) => {
                expect(error.status).toBe(500);
            }
        });

        // Simulate HTTP error
        const req = httpMock.expectOne(testUrl);
        req.flush(errorResponse.error, errorResponse);

        // Then
        expect(errorTrackingService.trackApiError).toHaveBeenCalledWith(
            {
                url: testUrl,
                method: 'GET',
                status: 500,
                statusText: 'Internal Server Error',
                message: jasmine.any(String),
                responseBody: { message: 'Server error occurred' },
                requestBody: null
            },
            {
                component: 'products',
                action: 'fetch',
                additionalData: {
                    requestHeaders: jasmine.any(Object),
                    responseHeaders: jasmine.any(Object)
                }
            }
        );
    });

    it('should not interfere with successful requests', () => {
        // Given
        const testUrl = '/api/products';
        const successResponse = { products: [] };

        // When
        httpClient.get(testUrl).subscribe({
            next: (response) => {
                expect(response).toEqual(successResponse);
            },
            error: () => fail('Should not have failed')
        });

        // Simulate successful HTTP response
        const req = httpMock.expectOne(testUrl);
        req.flush(successResponse);

        // Then
        expect(errorTrackingService.trackApiError).not.toHaveBeenCalled();
    });
});