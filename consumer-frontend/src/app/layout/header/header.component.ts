import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Observable, Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

// PrimeNG
import { MenuItem } from 'primeng/api';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';

// Services
import { Cart } from '../../core/models/cart.model';
import { CartService } from '../../core/services/cart.service';
import { ErrorTrackingService } from '../../core/services/error-tracking.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { UserBehaviorAnalyticsService } from '../../core/services/user-behavior-analytics.service';
import { WebVitalsService } from '../../core/services/web-vitals.service';

// Tracking Directives
import { TrackClickDirective } from '../../shared/directives/track-click.directive';
import { TrackViewDirective } from '../../shared/directives/track-view.directive';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    BadgeModule,
    MenuModule,
    OverlayPanelModule,
    AutoCompleteModule,
    TrackClickDirective,
    TrackViewDirective
  ],
  template: `
    <!-- Top Announcement Bar -->
    <div class="header-announcement bg-gradient-to-r from-primary-500 to-secondary-500 text-white text-center py-2 text-sm">
      <p class="animate-fade-in">🎉 新用戶註冊享 9 折優惠！免費配送全台 🚚</p>
    </div>

    <!-- Modern Header -->
    <header class="modern-header bg-white/95 backdrop-blur-lg shadow-lg border-b border-gray-100 sticky top-0 z-50 transition-all duration-300"
            [class.scrolled]="isScrolled">
      <div class="nav-container container-modern">
        <div class="main-navigation flex items-center justify-between h-20">
          <!-- Brand Logo -->
          <div class="nav-brand flex items-center">
            <a routerLink="/home" 
               class="brand-link flex items-center space-x-3 hover-lift"
               trackClick="header_brand_logo"
               [trackClickData]="{
                 section: 'header',
                 action: 'brand_logo_click',
                 destination: 'home'
               }"
               trackClickCategory="navigation">
              <div class="brand-logo w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-2xl flex items-center justify-center shadow-lg">
                <i class="pi pi-shopping-cart text-white text-xl"></i>
              </div>
              <div class="brand-text">
                <span class="text-2xl font-bold text-gradient">購物商城</span>
                <div class="text-xs text-gray-500 font-medium">Premium Shopping</div>
              </div>
            </a>
          </div>

          <!-- Smart Search Bar -->
          <div class="nav-search hidden lg:flex flex-1 max-w-2xl mx-8">
            <div class="search-container relative w-full">
              <div class="search-input-wrapper relative">
                <input 
                  type="text"
                  placeholder="搜尋商品、品牌或分類..."
                  class="search-input w-full pl-12 pr-16 py-4 bg-gray-50 border-2 border-transparent rounded-2xl text-base transition-all duration-300 focus:bg-white focus:border-primary-500 focus:shadow-lg focus:shadow-primary-500/20"
                  [formControl]="searchControl"
                  (keyup.enter)="onSearch()"
                  (focus)="trackSearchFocus()"
                  #searchInput
                  trackClick="header_search_input_focus"
                  [trackClickData]="{
                    section: 'header',
                    action: 'search_input_focus',
                    inputType: 'desktop_search'
                  }"
                  trackClickCategory="search">
                <button class="search-btn absolute right-2 top-1/2 transform -translate-y-1/2 w-10 h-10 bg-primary-500 hover:bg-primary-600 text-white rounded-xl transition-all duration-200 hover:scale-105"
                        (click)="onSearch()"
                        trackClick="header_search_button"
                        [trackClickData]="{
                          section: 'header',
                          action: 'search_button_click',
                          searchQuery: searchControl.value || '',
                          hasQuery: !!searchControl.value
                        }"
                        trackClickCategory="search">
                  <i class="pi pi-search"></i>
                </button>
                <i class="pi pi-search absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 text-lg"></i>
              </div>
              
              <!-- Search Suggestions Dropdown -->
              <div class="search-suggestions absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 max-h-96 overflow-y-auto"
                   *ngIf="searchSuggestions.length > 0 && showSuggestions">
                <div class="p-4">
                  <h4 class="text-sm font-semibold text-gray-500 mb-3">熱門搜尋</h4>
                  <div *ngFor="let suggestion of searchSuggestions" 
                       class="suggestion-item flex items-center p-3 hover:bg-gray-50 rounded-xl cursor-pointer transition-colors"
                       (click)="selectSuggestion(suggestion)">
                    <i class="pi pi-search text-gray-400 mr-3"></i>
                    <span class="flex-1">{{ suggestion.text }}</span>
                    <span class="text-xs text-gray-400">{{ suggestion.count }} 個結果</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Main Navigation Menu -->
          <div class="nav-menu hidden xl:flex items-center space-x-8">
            <a *ngFor="let item of navigationItems; let i = index" 
               [routerLink]="item.route"
               routerLinkActive="nav-link-active"
               class="nav-link relative px-4 py-2 text-gray-700 hover:text-primary-600 font-medium transition-all duration-200 hover:bg-primary-50 rounded-xl"
               trackClick="header_nav_item"
               [trackClickData]="{
                 section: 'header',
                 action: 'nav_item_click',
                 navItem: item.label,
                 navRoute: item.route,
                 position: i + 1
               }"
               trackClickCategory="navigation">
              <i [class]="item.icon" class="mr-2"></i>
              {{ item.label }}
            </a>
          </div>

          <!-- User Actions -->
          <div class="nav-actions flex items-center space-x-3">
            <!-- Wishlist -->
            <button class="nav-action-btn relative p-3 text-gray-600 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all duration-200 hover:scale-105"
                    title="願望清單"
                    trackClick="header_wishlist_button"
                    [trackClickData]="{
                      section: 'header',
                      action: 'wishlist_button_click',
                      wishlistCount: wishlistCount
                    }"
                    trackClickCategory="wishlist">
              <i class="pi pi-heart text-xl"></i>
              <span class="action-badge absolute -top-1 -right-1 bg-accent-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center"
                    *ngIf="wishlistCount > 0">{{ wishlistCount }}</span>
            </button>
            
            <!-- Shopping Cart -->
            <button class="cart-btn nav-action-btn relative p-3 text-gray-600 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all duration-200 hover:scale-105"
                    (click)="toggleCart()"
                    title="購物車"
                    trackClick="header_cart_button"
                    [trackClickData]="{
                      section: 'header',
                      action: 'cart_button_click',
                      cartCount: cartCount,
                      cartOpen: isCartOpen
                    }"
                    trackClickCategory="cart">
              <i class="pi pi-shopping-cart text-xl"></i>
              <span class="action-badge absolute -top-1 -right-1 bg-secondary-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center animate-bounce-in"
                    *ngIf="cartCount > 0">{{ cartCount }}</span>
            </button>
            
            <!-- User Menu -->
            <div class="user-menu relative">
              <button class="user-avatar w-10 h-10 bg-gray-200 hover:bg-primary-100 rounded-xl flex items-center justify-center transition-all duration-200 hover:scale-105"
                      *ngIf="!isLoggedIn"
                      (click)="toggleUserMenu($event)"
                      trackClick="header_user_menu_guest"
                      [trackClickData]="{
                        section: 'header',
                        action: 'user_menu_click',
                        userType: 'guest'
                      }"
                      trackClickCategory="user_interaction">
                <i class="pi pi-user text-gray-600"></i>
              </button>
              <img [src]="user?.avatar || '/assets/images/default-avatar.jpg'" 
                   [alt]="user?.name || 'User'"
                   class="user-avatar w-10 h-10 rounded-xl object-cover border-2 border-primary-200 hover:border-primary-400 transition-all duration-200 cursor-pointer hover:scale-105"
                   *ngIf="isLoggedIn"
                   (click)="toggleUserMenu($event)"
                   trackClick="header_user_menu_logged_in"
                   [trackClickData]="{
                     section: 'header',
                     action: 'user_menu_click',
                     userType: 'logged_in',
                     userName: user?.name
                   }"
                   trackClickCategory="user_interaction">
            </div>

            <!-- Mobile Menu Toggle -->
            <button class="mobile-menu-btn xl:hidden p-3 text-gray-600 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all duration-200"
                    (click)="toggleMobileMenu()"
                    trackClick="header_mobile_menu_toggle"
                    [trackClickData]="{
                      section: 'header',
                      action: 'mobile_menu_toggle',
                      menuOpen: showMobileMenu
                    }"
                    trackClickCategory="navigation">
              <i class="pi pi-bars text-xl"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Category Navigation (Optional) -->
      <div class="category-navigation bg-white border-b border-gray-100 py-3"
           *ngIf="showCategoryNav">
        <div class="category-nav-container container-modern">
          <div class="flex items-center space-x-8 overflow-x-auto">
            <a *ngFor="let category of mainCategories" 
               [routerLink]="['/category', category.slug]"
               class="category-nav-link whitespace-nowrap px-4 py-2 text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all duration-200">
              <i [class]="category.icon" class="mr-2"></i>
              {{ category.name }}
            </a>
          </div>
        </div>
      </div>

      <!-- Mobile Search Bar -->
      <div class="mobile-search lg:hidden bg-white border-b border-gray-100 p-4">
        <div class="relative">
          <input
            type="text"
            placeholder="搜尋商品、品牌或分類..."
            class="w-full pl-12 pr-4 py-3 bg-gray-50 border-2 border-transparent rounded-xl text-base focus:bg-white focus:border-primary-500 focus:shadow-lg transition-all duration-300"
            [formControl]="mobileSearchControl"
            (keyup.enter)="onMobileSearch()">
          <i class="pi pi-search absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400"></i>
        </div>
      </div>
    </header>

    <!-- Mobile Navigation Overlay -->
    <div class="mobile-nav-overlay fixed inset-0 bg-black/50 z-40 xl:hidden transition-opacity duration-300"
         *ngIf="showMobileMenu"
         [class.opacity-100]="showMobileMenu"
         [class.opacity-0]="!showMobileMenu"
         (click)="closeMobileMenu()"></div>

    <!-- Mobile Navigation Panel -->
    <div class="mobile-nav-panel fixed top-0 right-0 h-full w-80 bg-white shadow-2xl z-50 xl:hidden transform transition-transform duration-300"
         [class.translate-x-0]="showMobileMenu"
         [class.translate-x-full]="!showMobileMenu">
      
      <!-- Mobile Nav Header -->
      <div class="mobile-nav-header flex items-center justify-between p-6 border-b border-gray-100">
        <h3 class="text-lg font-semibold text-gray-900">選單</h3>
        <button class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                (click)="closeMobileMenu()">
          <i class="pi pi-times text-xl"></i>
        </button>
      </div>

      <!-- Mobile Nav Content -->
      <div class="mobile-nav-content p-6 space-y-6">
        <!-- User Section -->
        <div class="user-section" *ngIf="!isLoggedIn">
          <div class="flex space-x-3">
            <button class="btn-primary flex-1" routerLink="/auth/login" (click)="closeMobileMenu()">
              登入
            </button>
            <button class="btn-secondary flex-1" routerLink="/auth/register" (click)="closeMobileMenu()">
              註冊
            </button>
          </div>
        </div>

        <div class="user-section" *ngIf="isLoggedIn">
          <div class="flex items-center space-x-3 p-4 bg-gray-50 rounded-xl">
            <img [src]="user?.avatar || '/assets/images/default-avatar.jpg'" 
                 [alt]="user?.name"
                 class="w-12 h-12 rounded-xl object-cover">
            <div>
              <div class="font-semibold text-gray-900">{{ user?.name }}</div>
              <div class="text-sm text-gray-500">{{ user?.email }}</div>
            </div>
          </div>
        </div>

        <!-- Navigation Links -->
        <nav class="mobile-nav-links space-y-2">
          <a *ngFor="let item of navigationItems" 
             [routerLink]="item.route"
             routerLinkActive="bg-primary-50 text-primary-600"
             class="mobile-nav-link flex items-center p-4 text-gray-700 hover:bg-gray-50 rounded-xl transition-colors"
             (click)="closeMobileMenu()">
            <i [class]="item.icon" class="mr-4 text-xl"></i>
            <span class="font-medium">{{ item.label }}</span>
          </a>
        </nav>

        <!-- Quick Actions -->
        <div class="quick-actions space-y-3">
          <button class="w-full flex items-center justify-between p-4 bg-primary-50 text-primary-600 rounded-xl"
                  (click)="toggleCart(); closeMobileMenu()">
            <div class="flex items-center">
              <i class="pi pi-shopping-cart mr-3 text-xl"></i>
              <span class="font-medium">購物車</span>
            </div>
            <span class="bg-primary-500 text-white text-sm px-2 py-1 rounded-lg" *ngIf="cartCount > 0">
              {{ cartCount }}
            </span>
          </button>

          <button class="w-full flex items-center p-4 text-gray-700 hover:bg-gray-50 rounded-xl transition-colors">
            <i class="pi pi-heart mr-3 text-xl"></i>
            <span class="font-medium">願望清單</span>
            <span class="ml-auto bg-gray-200 text-gray-600 text-sm px-2 py-1 rounded-lg" *ngIf="wishlistCount > 0">
              {{ wishlistCount }}
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- Side Cart Panel -->
    <div class="cart-sidebar fixed top-0 right-0 h-full w-96 bg-white shadow-2xl z-50 transform transition-transform duration-300"
         [class.translate-x-0]="isCartOpen"
         [class.translate-x-full]="!isCartOpen">
      
      <div class="cart-overlay fixed inset-0 bg-black/50 z-40"
           *ngIf="isCartOpen"
           (click)="closeCart()"></div>
      
      <div class="cart-panel relative z-50 h-full flex flex-col">
        <!-- Cart Header -->
        <div class="cart-header flex items-center justify-between p-6 border-b border-gray-100">
          <h3 class="text-lg font-semibold text-gray-900">購物車 ({{ cartCount }})</h3>
          <button class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                  (click)="closeCart()">
            <i class="pi pi-times text-xl"></i>
          </button>
        </div>

        <!-- Cart Content -->
        <div class="cart-content flex-1 overflow-y-auto p-6">
          <!-- Cart items will be implemented here -->
          <div class="text-center text-gray-500 py-12" *ngIf="cartCount === 0">
            <i class="pi pi-shopping-cart text-4xl mb-4 block"></i>
            <p class="text-lg font-medium mb-2">購物車是空的</p>
            <p class="text-sm">快去挑選您喜愛的商品吧！</p>
          </div>
        </div>

        <!-- Cart Footer -->
        <div class="cart-footer p-6 border-t border-gray-100" *ngIf="cartCount > 0">
          <div class="space-y-4">
            <div class="flex justify-between text-lg font-semibold">
              <span>總計</span>
              <span class="text-primary-600">NT$ 0</span>
            </div>
            <button class="btn-primary w-full" routerLink="/checkout" (click)="closeCart()">
              前往結帳
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Modern Header Styles */
    .header-announcement {
      font-weight: var(--font-weight-medium);
      letter-spacing: 0.5px;
    }

    .modern-header {
      transition: all var(--transition-normal);
    }

    .modern-header.scrolled {
      background: rgba(255, 255, 255, 0.98);
      backdrop-filter: blur(20px);
      box-shadow: var(--shadow-lg);
    }

    .brand-link:hover .brand-logo {
      transform: scale(1.05);
      box-shadow: var(--shadow-primary);
    }

    .brand-text {
      line-height: 1.2;
    }

    .search-input {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-medium);
    }

    .search-input:focus {
      outline: none;
    }

    .search-btn:hover {
      background: var(--color-primary-600);
    }

    .search-suggestions {
      animation: fadeInUp var(--duration-300) var(--easing-ease-out);
    }

    .suggestion-item:hover {
      background: var(--color-primary-50);
    }

    .nav-link {
      position: relative;
      font-weight: var(--font-weight-medium);
    }

    .nav-link::after {
      content: '';
      position: absolute;
      bottom: -2px;
      left: 50%;
      width: 0;
      height: 2px;
      background: var(--color-primary-500);
      transition: all var(--transition-fast);
      transform: translateX(-50%);
    }

    .nav-link:hover::after,
    .nav-link-active::after {
      width: 100%;
    }

    .nav-action-btn {
      position: relative;
    }

    .action-badge {
      font-size: 10px;
      font-weight: var(--font-weight-bold);
      min-width: 20px;
      height: 20px;
    }

    .user-avatar {
      cursor: pointer;
      border: 2px solid transparent;
      transition: all var(--transition-fast);
    }

    .user-avatar:hover {
      border-color: var(--color-primary-300);
    }

    .category-nav-container {
      scrollbar-width: none;
      -ms-overflow-style: none;
    }

    .category-nav-container::-webkit-scrollbar {
      display: none;
    }

    .mobile-nav-overlay {
      backdrop-filter: blur(4px);
    }

    .mobile-nav-panel {
      box-shadow: var(--shadow-2xl);
    }

    .mobile-nav-link {
      font-weight: var(--font-weight-medium);
    }

    .cart-sidebar {
      box-shadow: var(--shadow-2xl);
    }

    .cart-panel {
      background: white;
    }

    /* Responsive Design */
    @media (max-width: 1024px) {
      .nav-menu {
        display: none;
      }
    }

    @media (max-width: 768px) {
      .nav-search {
        display: none;
      }
      
      .main-navigation {
        height: 64px;
      }
      
      .brand-text .text-2xl {
        font-size: 1.25rem;
      }
    }

    /* Animation Classes */
    .animate-fade-in {
      animation: fadeIn var(--duration-500) var(--easing-ease-out);
    }

    .animate-bounce-in {
      animation: bounceIn var(--duration-700) var(--easing-bounce);
    }

    .hover-lift:hover {
      transform: translateY(-2px);
    }

    .text-gradient {
      background: var(--gradient-primary);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  `]
})
export class HeaderComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Observables
  cart$: Observable<Cart>;

  // Form Controls
  searchControl = new FormControl('');
  mobileSearchControl = new FormControl('');

  // State
  showMobileMenu = false;
  isCartOpen = false;
  isScrolled = false;
  showSuggestions = false;
  showCategoryNav = true;
  isLoggedIn = false;

  // Data
  cartCount = 0;
  wishlistCount = 0;
  user: any = null;

  searchSuggestions = [
    { text: 'iPhone 15', count: 156 },
    { text: '筆記型電腦', count: 89 },
    { text: '運動鞋', count: 234 },
    { text: '咖啡機', count: 67 },
    { text: '藍牙耳機', count: 145 }
  ];

  navigationItems = [
    { label: '所有分類', route: '/categories', icon: 'pi pi-th-large' },
    { label: '今日特價', route: '/deals', icon: 'pi pi-tag' },
    { label: '新品上市', route: '/new-arrivals', icon: 'pi pi-star' },
    { label: '品牌專區', route: '/brands', icon: 'pi pi-bookmark' }
  ];

  mainCategories = [
    { name: '電子產品', slug: 'electronics', icon: 'pi pi-mobile' },
    { name: '時尚服飾', slug: 'fashion', icon: 'pi pi-shopping-bag' },
    { name: '居家生活', slug: 'home', icon: 'pi pi-home' },
    { name: '運動健身', slug: 'sports', icon: 'pi pi-heart' },
    { name: '美妝保養', slug: 'beauty', icon: 'pi pi-star' },
    { name: '食品飲料', slug: 'food', icon: 'pi pi-apple' }
  ];

  userMenuItems: MenuItem[] = [];

  constructor(
    private cartService: CartService,
    private observabilityService: ObservabilityService,
    private webVitalsService: WebVitalsService,
    private userBehaviorAnalytics: UserBehaviorAnalyticsService,
    private errorTrackingService: ErrorTrackingService
  ) {
    this.cart$ = this.cartService.cart$;
  }



  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:scroll', ['$event'])
  onWindowScroll() {
    this.isScrolled = window.pageYOffset > 50;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.search-container')) {
      this.showSuggestions = false;
    }
  }

  setupSearchSubscriptions() {
    // Desktop search
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((value: string | null) => {
        if (value && value.length > 2) {
          this.showSuggestions = true;
          // TODO: Fetch search suggestions from API
        } else {
          this.showSuggestions = false;
        }
      });

    // Mobile search
    this.mobileSearchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((value: string | null) => {
        // TODO: Handle mobile search suggestions
      });
  }

  setupCartSubscription() {
    this.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe((cart: Cart | null) => {
        this.cartCount = cart?.itemCount || 0;
      });
  }

  setupUserMenuItems() {
    this.userMenuItems = [
      {
        label: '我的帳戶',
        icon: 'pi pi-user',
        routerLink: '/profile'
      },
      {
        label: '訂單記錄',
        icon: 'pi pi-list',
        routerLink: '/orders'
      },
      {
        label: '願望清單',
        icon: 'pi pi-heart',
        routerLink: '/wishlist'
      },
      {
        separator: true
      },
      {
        label: '登出',
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    ];
  }

  loadUserData() {
    // TODO: Load user data from service
    // For now, simulate logged out state
    this.isLoggedIn = false;
    this.user = null;
    this.wishlistCount = 0;
  }

  onSearch() {
    const searchTerm = this.searchControl.value?.trim();
    if (searchTerm) {
      this.showSuggestions = false;

      // Track search action
      this.observabilityService.trackBusinessEvent({
        type: 'search',
        data: {
          query: searchTerm,
          searchType: 'header_search',
          source: 'desktop',
          timestamp: Date.now()
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });

      // TODO: Navigate to search results
      console.log('Searching for:', searchTerm);
    }
  }

  onMobileSearch() {
    const searchTerm = this.mobileSearchControl.value?.trim();
    if (searchTerm) {
      // Track mobile search action
      this.observabilityService.trackBusinessEvent({
        type: 'search',
        data: {
          query: searchTerm,
          searchType: 'header_search',
          source: 'mobile',
          timestamp: Date.now()
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });

      // TODO: Navigate to search results
      console.log('Mobile searching for:', searchTerm);
    }
  }

  selectSuggestion(suggestion: any) {
    this.searchControl.setValue(suggestion.text);
    this.showSuggestions = false;

    // Track search suggestion selection
    this.observabilityService.trackUserAction('search_suggestion_select', {
      selectedSuggestion: suggestion.text,
      suggestionCount: suggestion.count,
      section: 'header',
      source: 'desktop'
    });

    this.onSearch();
  }



  logout() {
    // Track logout action
    this.observabilityService.trackUserAction('user_logout', {
      section: 'header',
      userType: 'logged_in',
      userName: this.user?.name
    });

    // TODO: Implement logout logic
    console.log('Logging out...');
    this.isLoggedIn = false;
    this.user = null;
  }

  // New tracking methods
  trackSearchFocus() {
    this.observabilityService.trackUserAction('search_input_focus', {
      section: 'header',
      inputType: 'desktop_search',
      timestamp: Date.now()
    });
  }

  toggleCart() {
    const wasOpen = this.isCartOpen;
    this.isCartOpen = !this.isCartOpen;

    // Track cart toggle action
    this.observabilityService.trackUserAction('cart_sidebar_toggle', {
      section: 'header',
      action: this.isCartOpen ? 'open' : 'close',
      cartCount: this.cartCount,
      previousState: wasOpen
    });

    if (this.isCartOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
  }

  closeCart() {
    this.observabilityService.trackUserAction('cart_sidebar_close', {
      section: 'header',
      action: 'close',
      cartCount: this.cartCount,
      method: 'overlay_click'
    });

    this.isCartOpen = false;
    document.body.style.overflow = '';
  }

  toggleMobileMenu() {
    const wasOpen = this.showMobileMenu;
    this.showMobileMenu = !this.showMobileMenu;

    // Track mobile menu toggle
    this.observabilityService.trackUserAction('mobile_menu_toggle', {
      section: 'header',
      action: this.showMobileMenu ? 'open' : 'close',
      previousState: wasOpen
    });

    if (this.showMobileMenu) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
  }

  closeMobileMenu() {
    this.observabilityService.trackUserAction('mobile_menu_close', {
      section: 'header',
      action: 'close',
      method: 'overlay_click'
    });

    this.showMobileMenu = false;
    document.body.style.overflow = '';
  }

  toggleUserMenu(event: Event) {
    this.observabilityService.trackUserAction('user_menu_toggle', {
      section: 'header',
      userType: this.isLoggedIn ? 'logged_in' : 'guest',
      userName: this.user?.name
    });

    // TODO: Implement user menu dropdown
    console.log('User menu clicked');
  }

  // Performance monitoring methods for header
  private setupHeaderPerformanceMonitoring() {
    // Monitor search input performance
    this.monitorSearchPerformance();

    // Monitor navigation performance
    this.monitorNavigationPerformance();

    // Monitor cart sidebar performance
    this.monitorCartPerformance();
  }

  private monitorSearchPerformance() {
    // Track search input response time
    this.searchControl.valueChanges.subscribe(() => {
      const startTime = performance.now();

      // Monitor search suggestions rendering
      setTimeout(() => {
        const renderTime = performance.now() - startTime;

        this.observabilityService.trackPerformanceMetric({
          type: 'page_load', // Using page_load type for UI response time
          value: renderTime,
          page: window.location.pathname,
          timestamp: Date.now()
        });

        this.observabilityService.trackUserAction('search_suggestions_render', {
          renderTime: renderTime,
          suggestionsCount: this.searchSuggestions.length,
          section: 'header'
        });
      }, 0);
    });

    // Monitor mobile search performance
    this.mobileSearchControl.valueChanges.subscribe(() => {
      const startTime = performance.now();

      setTimeout(() => {
        const renderTime = performance.now() - startTime;

        this.observabilityService.trackUserAction('mobile_search_render', {
          renderTime: renderTime,
          section: 'header'
        });
      }, 0);
    });
  }

  private monitorNavigationPerformance() {
    // Monitor navigation menu rendering
    const navMenu = document.querySelector('.nav-menu');
    if (navMenu) {
      const observer = new MutationObserver(() => {
        const renderTime = performance.now();

        this.observabilityService.trackUserAction('navigation_menu_render', {
          renderTime: renderTime,
          menuItemsCount: this.navigationItems.length,
          section: 'header'
        });
      });

      observer.observe(navMenu, { childList: true, subtree: true });
    }

    // Monitor mobile menu performance
    const mobileMenu = document.querySelector('.mobile-nav-panel');
    if (mobileMenu) {
      const observer = new MutationObserver(() => {
        const renderTime = performance.now();

        this.observabilityService.trackUserAction('mobile_menu_render', {
          renderTime: renderTime,
          section: 'header'
        });
      });

      observer.observe(mobileMenu, { attributes: true, attributeFilter: ['class'] });
    }
  }

  private monitorCartPerformance() {
    // Monitor cart sidebar opening performance
    const originalToggleCart = this.toggleCart.bind(this);

    this.toggleCart = () => {
      const startTime = performance.now();

      originalToggleCart();

      // Monitor cart sidebar render time
      setTimeout(() => {
        const renderTime = performance.now() - startTime;

        this.observabilityService.trackPerformanceMetric({
          type: 'page_load',
          value: renderTime,
          page: window.location.pathname,
          timestamp: Date.now()
        });

        this.observabilityService.trackUserAction('cart_sidebar_render', {
          renderTime: renderTime,
          cartCount: this.cartCount,
          action: this.isCartOpen ? 'open' : 'close',
          section: 'header'
        });
      }, 0);
    };
  }

  ngOnInit() {
    this.setupSearchSubscriptions();
    this.setupCartSubscription();
    this.setupUserMenuItems();
    this.loadUserData();

    // Setup performance monitoring after initial render
    setTimeout(() => {
      this.setupHeaderPerformanceMonitoring();
    }, 100);
  }
}