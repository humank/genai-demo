import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { HeaderComponent } from './header.component';

// PrimeNG modules for testing
import { AutoCompleteModule } from 'primeng/autocomplete';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';

describe('HeaderComponent Observability Integration', () => {
    let component: HeaderComponent;
    let fixture: ComponentFixture<HeaderComponent>;
    let observabilityService: jasmine.SpyObj<ObservabilityService>;
    let cartService: jasmine.SpyObj<CartService>;

    beforeEach(async () => {
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
            'trackUserAction',
            'trackBusinessEvent',
            'getSessionId'
        ]);
        const cartSpy = jasmine.createSpyObj('CartService', ['cart$']);

        await TestBed.configureTestingModule({
            imports: [
                HeaderComponent,
                RouterTestingModule,
                ReactiveFormsModule,
                NoopAnimationsModule,
                ButtonModule,
                InputTextModule,
                BadgeModule,
                MenuModule,
                OverlayPanelModule,
                AutoCompleteModule
            ],
            providers: [
                { provide: ObservabilityService, useValue: observabilitySpy },
                { provide: CartService, useValue: cartSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HeaderComponent);
        component = fixture.componentInstance;
        observabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;

        // Setup default mocks
        observabilityService.getSessionId.and.returnValue('test-session-123');
        // Mock cart$ as a property
        Object.defineProperty(cartService, 'cart$', {
            value: of({ items: [], totalAmount: { amount: 0, currency: 'USD' }, itemCount: 0 }),
            writable: false
        });
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should track search focus events', () => {
        // When
        component.trackSearchFocus();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('search_input_focus', jasmine.objectContaining({
            section: 'header',
            inputType: 'desktop_search'
        }));
    });

    it('should track search events with query', () => {
        // Given
        component.searchControl.setValue('test product');

        // When
        component.onSearch();

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'search',
            data: jasmine.objectContaining({
                query: 'test product',
                searchType: 'header_search',
                source: 'desktop'
            })
        }));
    });

    it('should track mobile search events', () => {
        // Given
        component.mobileSearchControl.setValue('mobile search');

        // When
        component.onMobileSearch();

        // Then
        expect(observabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
            type: 'search',
            data: jasmine.objectContaining({
                query: 'mobile search',
                searchType: 'header_search',
                source: 'mobile'
            })
        }));
    });

    it('should track search suggestion selection', () => {
        // Given
        const suggestion = { text: 'iPhone 15', count: 156 };

        // When
        component.selectSuggestion(suggestion);

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('search_suggestion_select', jasmine.objectContaining({
            selectedSuggestion: 'iPhone 15',
            suggestionCount: 156,
            section: 'header',
            source: 'desktop'
        }));
    });

    it('should track cart toggle events', () => {
        // Given
        component.isCartOpen = false;
        component.cartCount = 3;

        // When
        component.toggleCart();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('cart_sidebar_toggle', jasmine.objectContaining({
            section: 'header',
            action: 'open',
            cartCount: 3,
            previousState: false
        }));
        expect(component.isCartOpen).toBe(true);
    });

    it('should track cart close events', () => {
        // Given
        component.isCartOpen = true;
        component.cartCount = 2;

        // When
        component.closeCart();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('cart_sidebar_close', jasmine.objectContaining({
            section: 'header',
            action: 'close',
            cartCount: 2,
            method: 'overlay_click'
        }));
        expect(component.isCartOpen).toBe(false);
    });

    it('should track mobile menu toggle events', () => {
        // Given
        component.showMobileMenu = false;

        // When
        component.toggleMobileMenu();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('mobile_menu_toggle', jasmine.objectContaining({
            section: 'header',
            action: 'open',
            previousState: false
        }));
        expect(component.showMobileMenu).toBe(true);
    });

    it('should track mobile menu close events', () => {
        // Given
        component.showMobileMenu = true;

        // When
        component.closeMobileMenu();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('mobile_menu_close', jasmine.objectContaining({
            section: 'header',
            action: 'close',
            method: 'overlay_click'
        }));
        expect(component.showMobileMenu).toBe(false);
    });

    it('should track user menu toggle for guest users', () => {
        // Given
        component.isLoggedIn = false;
        const mockEvent = new Event('click');

        // When
        component.toggleUserMenu(mockEvent);

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('user_menu_toggle', jasmine.objectContaining({
            section: 'header',
            userType: 'guest',
            userName: undefined
        }));
    });

    it('should track user menu toggle for logged in users', () => {
        // Given
        component.isLoggedIn = true;
        component.user = { name: 'John Doe', email: 'john@example.com' };
        const mockEvent = new Event('click');

        // When
        component.toggleUserMenu(mockEvent);

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('user_menu_toggle', jasmine.objectContaining({
            section: 'header',
            userType: 'logged_in',
            userName: 'John Doe'
        }));
    });

    it('should track logout events', () => {
        // Given
        component.isLoggedIn = true;
        component.user = { name: 'John Doe', email: 'john@example.com' };

        // When
        component.logout();

        // Then
        expect(observabilityService.trackUserAction).toHaveBeenCalledWith('user_logout', jasmine.objectContaining({
            section: 'header',
            userType: 'logged_in',
            userName: 'John Doe'
        }));
        expect(component.isLoggedIn).toBe(false);
        expect(component.user).toBe(null);
    });

    it('should not track search with empty query', () => {
        // Given
        component.searchControl.setValue('   '); // whitespace only

        // When
        component.onSearch();

        // Then
        expect(observabilityService.trackBusinessEvent).not.toHaveBeenCalled();
    });

    it('should not track mobile search with empty query', () => {
        // Given
        component.mobileSearchControl.setValue('');

        // When
        component.onMobileSearch();

        // Then
        expect(observabilityService.trackBusinessEvent).not.toHaveBeenCalled();
    });
});