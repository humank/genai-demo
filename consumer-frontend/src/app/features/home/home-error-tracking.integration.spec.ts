import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { ErrorTrackingService } from '../../core/services/error-tracking.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { ProductService } from '../../core/services/product.service';
import { UserBehaviorAnalyticsService } from '../../core/services/user-behavior-analytics.service';
import { WebVitalsService } from '../../core/services/web-vitals.service';
import { SmoothScrollService } from '../../shared/services/smooth-scroll.service';
import { ToastService } from '../../shared/services/toast.service';
import { HomeComponent } from './home.component';

describe('HomeComponent Error Tracking Integration', () => {
    let component: HomeComponent;
    let fixture: ComponentFixture<HomeComponent>;
    let errorTrackingService: jasmine.SpyObj<ErrorTrackingService>;
    let observabilityService: jasmine.SpyObj<ObservabilityService>;
    let productService: jasmine.SpyObj<ProductService>;
    let cartService: jasmine.SpyObj<CartService>;
    let httpMock: HttpTestingController;

    beforeEach(async () => {
        const errorTrackingSpy = jasmine.createSpyObj('ErrorTrackingService', [
            'trackImageLoadError',
            'trackUserOperationError',
            'trackAnimationError',
            'trackNetworkError',
            'getErrorStats',
            'cleanup'
        ]);
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
            'trackPageView',
            'trackUserAction',
            'trackBusinessEvent',
            'trackPerformanceMetric',
            'getSessionId'
        ]);
        const userBehaviorSpy = jasmine.createSpyObj('UserBehaviorAnalyticsService', [
            'trackProductInteraction',
            'trackProductHover',
            'trackProductHoverEnd',
            'trackConversionStep',
            'trackCartInteraction',
            'trackFilterUsage',
            'trackSearchBehavior',
            'getSessionStats',
            'cleanup'
        ]);
        const webVitalsSpy = jasmine.createSpyObj('WebVitalsService', [
            'collectCurrentMetrics',
            'getStatus',
            'cleanup'
        ]);
        const productSpy = jasmine.createSpyObj('ProductService', ['getFeaturedProducts']);
        const cartSpy = jasmine.createSpyObj('CartService', ['addToCart']);
        const smoothScrollSpy = jasmine.createSpyObj('SmoothScrollService', [
            'registerParallaxElement',
            'setupAutoHideHeader',
            'destroy'
        ]);
        const toastSpy = jasmine.createSpyObj('ToastService', [
            'quickSuccess',
            'quickError',
            'loadingFeedback',
            'buttonSuccess',
            'buttonError',
            'successFeedback',
            'info'
        ]);

        await TestBed.configureTestingModule({
            imports: [
                HomeComponent,
                RouterTestingModule,
                HttpClientTestingModule,
                NoopAnimationsModule
            ],
            providers: [
                { provide: ErrorTrackingService, useValue: errorTrackingSpy },
                { provide: ObservabilityService, useValue: observabilitySpy },
                { provide: UserBehaviorAnalyticsService, useValue: userBehaviorSpy },
                { provide: WebVitalsService, useValue: webVitalsSpy },
                { provide: ProductService, useValue: productSpy },
                { provide: CartService, useValue: cartSpy },
                { provide: SmoothScrollService, useValue: smoothScrollSpy },
                { provide: ToastService, useValue: toastSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HomeComponent);
        component = fixture.componentInstance;
        errorTrackingService = TestBed.inject(ErrorTrackingService) as jasmine.SpyObj<ErrorTrackingService>;
        observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
        cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;
        httpMock = TestBed.inject(HttpTestingController);

        // Setup default mocks
        observabilityService.getSessionId.and.returnValue('test-session-123');
        productService.getFeaturedProducts.and.returnValue(of({
            content: [
                {
                    id: 'product-1',
                    name: 'Test Product 1',
                    price: { amount: 100, currency: 'TWD' },
                    category: 'ELECTRONICS' as any,
                    inStock: true,
                    status: 'ACTIVE' as any,
                    images: [{ 
                        id: 'img-1', 
                        url: 'test-image.jpg', 
                        type: 'PRIMARY' as any, 
                        sortOrder: 1 
                    }]
                }
            ],
            totalElements: 1,
            totalPages: 1,
            size: 12,
            number: 0,
            first: true,
            last: true
        }));

        // Mock cart$ as a property
        Object.defineProperty(cartService, 'cart$', {
            value: of({ items: [], totalAmount: { amount: 0, currency: 'USD' }, itemCount: 0 }),
            writable: false
        });

        cartService.addToCart.and.returnValue(of({
            items: [],
            totalAmount: { amount: 0, currency: 'USD' },
            itemCount: 0
        }));
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should track image load errors', () => {
        // Given
        const mockImg = document.createElement('img');
        mockImg.src = 'https://example.com/test-image.jpg';
        mockImg.alt = 'Test Image';
        mockImg.className = 'product-image';

        const mockEvent = {
            target: mockImg
        };

        // When
        component.onImageError(mockEvent);

        // Then
        expect(errorTrackingService.trackImageLoadError).toHaveBeenCalledWith(
            {
                src: 'https://example.com/test-image.jpg',
                alt: 'Test Image',
                element: 'product-image'
            },
            {
                component: 'HomeComponent',
                action: 'image_load',
                additionalData: {
                    section: jasmine.any(String)
                }
            }
        );

        expect(observabilityService.trackUserAction).toHaveBeenCalledWith(
            'image_fallback_used',
            jasmine.objectContaining({
                originalSrc: 'https://example.com/test-image.jpg',
                fallbackSrc: '/assets/images/placeholder-product.jpg'
            })
        );

        expect(mockImg.src).toBe('/assets/images/placeholder-product.jpg');
    });

    it('should track user operation errors for add to cart failures', () => {
        // Given
        const product = {
            id: 'product-1',
            name: 'Test Product',
            price: { amount: 100, currency: 'TWD' },
            category: 'ELECTRONICS' as any,
            inStock: true,
            status: 'ACTIVE' as any,
            images: []
        } as any;

        const mockError = {
            status: 500,
            message: 'Internal Server Error',
            statusText: 'Internal Server Error'
        };

        cartService.addToCart.and.returnValue(throwError(() => mockError));

        // When
        component.addToCart(product);

        // Then
        expect(errorTrackingService.trackUserOperationError).toHaveBeenCalledWith(
            {
                operation: 'add_to_cart',
                errorType: 'unknown',
                message: 'Internal Server Error'
            },
            {
                component: 'HomeComponent',
                action: 'add_to_cart',
                additionalData: {
                    productId: 'product-1',
                    productName: 'Test Product',
                    productPrice: 100
                }
            }
        );
    });

    it('should categorize errors correctly', () => {
        // Test validation error
        const validationError = { status: 400 };
        expect(component['categorizeError'](validationError)).toBe('validation');

        // Test permission error
        const permissionError = { status: 401 };
        expect(component['categorizeError'](permissionError)).toBe('permission');

        // Test network error
        const networkError = { status: 0 };
        expect(component['categorizeError'](networkError)).toBe('network');

        // Test timeout error
        const timeoutError = { name: 'TimeoutError' };
        expect(component['categorizeError'](timeoutError)).toBe('timeout');

        // Test unknown error
        const unknownError = { status: 500 };
        expect(component['categorizeError'](unknownError)).toBe('unknown');
    });

    it('should sanitize URLs correctly', () => {
        // Test full URL
        const fullUrl = 'https://example.com/path/to/image.jpg?param=value';
        expect(component['sanitizeUrl'](fullUrl)).toBe('https://example.com/path/to/image.jpg');

        // Test relative URL
        const relativeUrl = '/path/to/image.jpg?param=value';
        expect(component['sanitizeUrl'](relativeUrl)).toBe('/path/to/image.jpg');

        // Test invalid URL
        const invalidUrl = 'not-a-url';
        expect(component['sanitizeUrl'](invalidUrl)).toBe('not-a-url');
    });
});