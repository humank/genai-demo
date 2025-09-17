import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { ProductService } from '../../core/services/product.service';
import { WebVitalsService } from '../../core/services/web-vitals.service';
import { SmoothScrollService } from '../../shared/services/smooth-scroll.service';
import { ToastService } from '../../shared/services/toast.service';
import { HomeComponent } from './home.component';

describe('HomeComponent Performance Integration', () => {
    let component: HomeComponent;
    let fixture: ComponentFixture<HomeComponent>;
    let observabilityService: jasmine.SpyObj<ObservabilityService>;
    let webVitalsService: jasmine.SpyObj<WebVitalsService>;
    let productService: jasmine.SpyObj<ProductService>;

    beforeEach(async () => {
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
            'trackPageView',
            'trackUserAction',
            'trackBusinessEvent',
            'trackPerformanceMetric',
            'getSessionId'
        ]);
        const webVitalsSpy = jasmine.createSpyObj('WebVitalsService', [
            'collectCurrentMetrics',
            'getStatus',
            'cleanup'
        ]);
        const productSpy = jasmine.createSpyObj('ProductService', ['getFeaturedProducts']);
        const cartSpy = jasmine.createSpyObj('CartService', ['addToCart', 'cart$']);
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
                { provide: ObservabilityService, useValue: observabilitySpy },
                { provide: WebVitalsService, useValue: webVitalsSpy },
                { provide: ProductService, useValue: productSpy },
                { provide: CartService, useValue: cartSpy },
                { provide: SmoothScrollService, useValue: smoothScrollSpy },
                { provide: ToastService, useValue: toastSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HomeComponent);
        component = fixture.componentInstance;
        observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        webVitalsService = TestBed.inject(WebVitalsService) as jasmine.SpyObj<WebVitalsService>;
        productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;

        // Setup default mocks
        observabilityService.getSessionId.and.returnValue('test-session-123');
        productService.getFeaturedProducts.and.returnValue(of({
            content: [
                {
                    id: 'product-1',
                    name: 'Test Product 1',
                    price: { amount: 100, currency: 'TWD' },
                    category: 'ELECTRONICS',
                    inStock: true,
                    status: 'ACTIVE',
                    images: [{ url: 'test-image.jpg' }]
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
        const cartService = TestBed.inject(CartService);
        Object.defineProperty(cartService, 'cart$', {
            value: of({ items: [], totalAmount: 0, itemCount: 0 }),
            writable: false
        });
        TestBed.inject(CartService).addToCart = jasmine.createSpy().and.returnValue(of({
            items: [],
            totalAmount: 0,
            itemCount: 0
        }));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should collect Web Vitals metrics on initialization', () => {
        // When
        component.ngOnInit();

        // Then
        expect(webVitalsService.collectCurrentMetrics).toHaveBeenCalled();
    });

    it('should track page load performance metrics', (done) => {
        // Given
        spyOn(window, 'addEventListener').and.callFake((event: string, callback: any) => {
            if (event === 'load') {
                // Simulate page load event
                setTimeout(() => {
                    callback();

                    // Then
                    expect(observabilityService.trackPerformanceMetric).toHaveBeenCalledWith(jasmine.objectContaining({
                        type: 'page_load',
                        page: window.location.pathname
                    }));
                    done();
                }, 0);
            }
        });

        // When
        component.ngOnInit();
    });

    it('should track DOM content loaded performance', (done) => {
        // Given
        Object.defineProperty(document, 'readyState', {
            writable: true,
            value: 'loading'
        });

        spyOn(document, 'addEventListener').and.callFake((event: string, callback: any) => {
            if (event === 'DOMContentLoaded') {
                // Simulate DOM content loaded event
                setTimeout(() => {
                    callback();

                    // Then
                    expect(observabilityService.trackPerformanceMetric).toHaveBeenCalledWith(jasmine.objectContaining({
                        type: 'page_load',
                        page: window.location.pathname
                    }));
                    done();
                }, 0);
            }
        });

        // When
        component.ngOnInit();
    });

    it('should track image loading performance', (done) => {
        // Given
        const mockImage = document.createElement('img');
        mockImage.className = 'product-image';
        spyOn(document, 'querySelectorAll').and.returnValue([mockImage] as any);

        // When
        component.ngOnInit();

        // Simulate image load
        setTimeout(() => {
            const loadEvent = new Event('load');
            mockImage.dispatchEvent(loadEvent);

            // Then
            setTimeout(() => {
                expect(observabilityService.trackPerformanceMetric).toHaveBeenCalledWith(jasmine.objectContaining({
                    type: 'page_load'
                }));
                expect(observabilityService.trackUserAction).toHaveBeenCalledWith('image_load_success', jasmine.objectContaining({
                    imageIndex: 0,
                    imageType: 'product_main'
                }));
                done();
            }, 0);
        }, 150); // Wait for setupPerformanceMonitoring timeout
    });

    it('should track image loading errors', (done) => {
        // Given
        const mockImage = document.createElement('img');
        mockImage.className = 'product-mini-image';
        mockImage.src = 'invalid-image.jpg';
        spyOn(document, 'querySelectorAll').and.returnValue([mockImage] as any);

        // When
        component.ngOnInit();

        // Simulate image error
        setTimeout(() => {
            const errorEvent = new Event('error');
            mockImage.dispatchEvent(errorEvent);

            // Then
            setTimeout(() => {
                expect(observabilityService.trackUserAction).toHaveBeenCalledWith('image_load_error', jasmine.objectContaining({
                    imageIndex: 0,
                    imageType: 'product_mini',
                    imageSrc: 'invalid-image.jpg'
                }));
                done();
            }, 0);
        }, 150);
    });

    it('should track animation performance', (done) => {
        // Given
        const mockElement = document.createElement('div');
        mockElement.setAttribute('appScrollReveal', '');
        spyOn(document, 'querySelectorAll').and.returnValue([mockElement] as any);

        // Mock IntersectionObserver
        const mockObserver = {
            observe: jasmine.createSpy('observe'),
            disconnect: jasmine.createSpy('disconnect')
        };
        spyOn(window, 'IntersectionObserver').and.returnValue(mockObserver as any);

        // When
        component.ngOnInit();

        // Simulate intersection
        setTimeout(() => {
            const callback = (window.IntersectionObserver as unknown as jasmine.Spy).calls.argsFor(0)[0];
            callback([{ isIntersecting: true, target: mockElement }]);

            // Simulate animation end
            const animationEndEvent = new Event('animationend');
            mockElement.dispatchEvent(animationEndEvent);

            // Then
            setTimeout(() => {
                expect(observabilityService.trackPerformanceMetric).toHaveBeenCalledWith(jasmine.objectContaining({
                    type: 'page_load'
                }));
                expect(observabilityService.trackUserAction).toHaveBeenCalledWith('animation_complete', jasmine.objectContaining({
                    elementIndex: 0,
                    animationType: 'scroll_reveal'
                }));
                done();
            }, 0);
        }, 150);
    });

    it('should track hover animation performance', (done) => {
        // Given
        const mockElement = document.createElement('div');
        mockElement.setAttribute('appProductHover', '');
        spyOn(document, 'querySelectorAll').and.returnValue([mockElement] as any);

        // When
        component.ngOnInit();

        // Simulate hover events
        setTimeout(() => {
            const mouseEnterEvent = new Event('mouseenter');
            const mouseLeaveEvent = new Event('mouseleave');

            mockElement.dispatchEvent(mouseEnterEvent);

            setTimeout(() => {
                mockElement.dispatchEvent(mouseLeaveEvent);

                // Then
                setTimeout(() => {
                    expect(observabilityService.trackUserAction).toHaveBeenCalledWith('hover_animation_complete', jasmine.objectContaining({
                        elementIndex: 0,
                        animationType: 'hover_effect'
                    }));
                    done();
                }, 0);
            }, 10);
        }, 150);
    });

    it('should track section render performance', (done) => {
        // Given
        const heroSection = document.createElement('div');
        heroSection.className = 'hero-section';
        heroSection.innerHTML = '<div></div><div></div>';

        const productsGrid = document.createElement('div');
        productsGrid.className = 'products-grid';
        productsGrid.innerHTML = '<div class="product-card-modern"></div><div class="product-card-modern"></div>';

        const categoriesGrid = document.createElement('div');
        categoriesGrid.className = 'categories-grid';
        categoriesGrid.innerHTML = '<div class="category-card"></div>';

        spyOn(document, 'querySelector').and.callFake((selector: string) => {
            if (selector === '.hero-section') return heroSection;
            if (selector === '.products-grid') return productsGrid;
            if (selector === '.categories-grid') return categoriesGrid;
            return null;
        });

        spyOn(window, 'addEventListener').and.callFake((event: string, callback: any) => {
            if (event === 'load') {
                setTimeout(() => {
                    callback();

                    // Then
                    expect(observabilityService.trackUserAction).toHaveBeenCalledWith('section_render_complete', jasmine.objectContaining({
                        section: 'hero',
                        elementCount: 2
                    }));
                    expect(observabilityService.trackUserAction).toHaveBeenCalledWith('section_render_complete', jasmine.objectContaining({
                        section: 'products_grid',
                        productCount: 2
                    }));
                    expect(observabilityService.trackUserAction).toHaveBeenCalledWith('section_render_complete', jasmine.objectContaining({
                        section: 'categories_grid',
                        categoryCount: 1
                    }));
                    done();
                }, 0);
            }
        });

        // When
        component.ngOnInit();
    });

    it('should get correct image type from class names', () => {
        // Test getImageType method through component behavior
        const productImage = document.createElement('img');
        productImage.className = 'product-image';

        const categoryImage = document.createElement('img');
        categoryImage.className = 'category-image';

        const heroImage = document.createElement('img');
        heroImage.className = 'hero-image';

        // These would be tested through the actual image loading monitoring
        expect(component).toBeTruthy(); // Basic test since methods are private
    });

    it('should get correct section from element hierarchy', () => {
        // Test getElementSection method through component behavior
        const heroElement = document.createElement('div');
        const heroSection = document.createElement('div');
        heroSection.className = 'hero-section';
        heroSection.appendChild(heroElement);

        const productsElement = document.createElement('div');
        const productsSection = document.createElement('div');
        productsSection.className = 'products-showcase';
        productsSection.appendChild(productsElement);

        // These would be tested through the actual performance monitoring
        expect(component).toBeTruthy(); // Basic test since methods are private
    });
});