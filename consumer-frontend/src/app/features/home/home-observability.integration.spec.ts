import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { ProductService } from '../../core/services/product.service';
import { SmoothScrollService } from '../../shared/services/smooth-scroll.service';
import { ToastService } from '../../shared/services/toast.service';
import { HomeComponent } from './home.component';

describe('HomeComponent Observability Integration', () => {
    let component: HomeComponent;
    let fixture: ComponentFixture<HomeComponent>;
    let observabilityService: jasmine.SpyObj<ObservabilityService>;
    let productService: jasmine.SpyObj<ProductService>;
    let cartService: jasmine.SpyObj<CartService>;

    beforeEach(async () => {
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
            'trackPageView',
            'trackUserAction',
            'trackBusinessEvent',
            'getSessionId'
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
                { provide: ProductService, useValue: productSpy },
                { provide: CartService, useValue: cartSpy },
                { provide: SmoothScrollService, useValue: smoothScrollSpy },
                { provide: ToastService, useValue: toastSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HomeComponent);
        component = fixture.componentInstance;
        observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
        cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;

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
        Object.defineProperty(cartService, 'cart$', {
            value: of({ items: [], totalAmount: 0, itemCount: 0 }),
            writable: false
        });
        cartService.addToCart.and.returnValue(of({ items: [], totalAmount: 0, itemCount: 0 }));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should track page view on initialization', () => {
        // When
        component.ngOnInit();

        // Then
        expect(observabilityService.trackPageView).toHaveBeenCalledWith('/home', jasmine.objectContaining({
            pageTitle: '首頁',
            pageType: 'home',
            userAgent: navigator.userAgent
        }));
    });

    it('should track category hover events', () => {
        // Given
        const category = {
            label: '電子產品',
            value: 'ELECTRONICS',
            productCount: 156
        };

        // When
        component.trackCategoryHover(category, 0);

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('category_hover', jasmine.objectContaining({
            categoryId: 'ELECTRONICS',
            categoryName: '電子產品',
            position: 1,
            section: 'categories_showcase',
            productCount: 156
        }));
    });

    it('should track filter changes', () => {
        // Given
        component.activeFilter = 'all';
        component.featuredProducts = [
            {
                id: 'product-1',
                name: 'Test Product',
                price: { amount: 100, currency: 'TWD' },
                category: 'ELECTRONICS',
                inStock: true,
                images: []
            } as any
        ];

        // When
        component.setActiveFilter('new');

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'filter_apply',
            data: jasmine.objectContaining({
                filterType: 'product_showcase',
                newFilter: 'new',
                previousFilter: 'all',
                section: 'products_showcase'
            })
        }));
    });

    it('should track add to cart events', () => {
        // Given
        const product = {
            id: 'product-1',
            name: 'Test Product',
            price: { amount: 100, currency: 'TWD' },
            category: 'ELECTRONICS',
            inStock: true,
            images: []
        } as any;

        // When
        component.addToCart(product);

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'cart_add',
            data: jasmine.objectContaining({
                productId: 'product-1',
                productName: 'Test Product',
                productPrice: 100,
                productCategory: 'ELECTRONICS',
                section: 'products_showcase',
                inStock: true
            })
        }));
    });

    it('should track wishlist toggle events', () => {
        // Given
        const product = {
            id: 'product-1',
            name: 'Test Product',
            price: { amount: 100, currency: 'TWD' },
            category: 'ELECTRONICS',
            inStock: true,
            images: []
        } as any;

        // When
        component.toggleWishlist(product);

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'wishlist_action',
            data: jasmine.objectContaining({
                productId: 'product-1',
                productName: 'Test Product',
                productPrice: 100,
                action: 'add',
                section: 'products_showcase'
            })
        }));
    });

    it('should track quick view events', () => {
        // Given
        const product = {
            id: 'product-1',
            name: 'Test Product',
            price: { amount: 100, currency: 'TWD' },
            category: 'ELECTRONICS',
            inStock: true,
            images: []
        } as any;

        // When
        component.quickView(product);

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'product_view',
            data: jasmine.objectContaining({
                productId: 'product-1',
                productName: 'Test Product',
                viewType: 'quick_view',
                section: 'products_showcase'
            })
        }));
    });

    it('should track newsletter subscription', () => {
        // Given
        const email = 'test@example.com';

        // When
        component.subscribeNewsletter(email);

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'newsletter_subscription',
            data: jasmine.objectContaining({
                email: 'test@example.com',
                source: 'home_page',
                section: 'newsletter'
            })
        }));
    });

    it('should not track newsletter subscription with invalid email', () => {
        // Given
        const invalidEmail = 'invalid-email';

        // When
        component.subscribeNewsletter(invalidEmail);

        // Then
        expect(observabilityService.trackBusinessEvent).not.toHaveBeenCalled();
    });
});