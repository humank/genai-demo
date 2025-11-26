import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Subject, interval, takeUntil } from 'rxjs';

// Shared Components and Directives
import { PageLoaderComponent } from '../../shared/components/page-loader/page-loader.component';
import { ScrollProgressComponent } from '../../shared/components/scroll-progress/scroll-progress.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader/skeleton-loader.component';
import { ToastComponent } from '../../shared/components/toast/toast.component';
import { FeedbackAnimationDirective } from '../../shared/directives/feedback-animation.directive';
import { ImageZoomDirective } from '../../shared/directives/image-zoom.directive';
import { ParallaxDirective } from '../../shared/directives/parallax.directive';
import { ProductHoverDirective } from '../../shared/directives/product-hover.directive';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';
import { TrackClickDirective } from '../../shared/directives/track-click.directive';
import { TrackViewDirective } from '../../shared/directives/track-view.directive';

// Services
import { ErrorTrackingService } from '../../core/services/error-tracking.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { UserBehaviorAnalyticsService } from '../../core/services/user-behavior-analytics.service';
import { WebVitalsService } from '../../core/services/web-vitals.service';
import { SmoothScrollService } from '../../shared/services/smooth-scroll.service';
import { ToastService } from '../../shared/services/toast.service';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CarouselModule } from 'primeng/carousel';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

// Models & Services
import { Product, ProductListResponse } from '../../core/models/product.model';
import { CartService } from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    CarouselModule,
    TagModule,
    SkeletonModule,
    // Shared Components and Directives
    ScrollRevealDirective,
    ImageZoomDirective,
    ProductHoverDirective,
    ParallaxDirective,
    FeedbackAnimationDirective,
    TrackClickDirective,
    TrackViewDirective,
    PageLoaderComponent,
    SkeletonLoaderComponent,
    ToastComponent,
    ScrollProgressComponent
  ],
  template: `
    <div class="min-h-screen">
      <!-- Page Loader -->
      <app-page-loader *ngIf="showPageLoader" 
                       [loadingText]="'載入精彩內容中...'"
                       [fadeOut]="pageLoaderFadeOut">
      </app-page-loader>

      <!-- Toast Container -->
      <app-toast></app-toast>

      <!-- Scroll Progress Indicator -->
      <app-scroll-progress type="circle" position="bottom-right"></app-scroll-progress>

      <!-- Modern Hero Section with Dynamic Background -->
      <section class="hero-section relative overflow-hidden" 
               appParallax 
               [parallaxSpeed]="0.3"
               [parallaxDirection]="'down'">
        <!-- Dynamic Background -->
        <div class="hero-background absolute inset-0">
          <div class="hero-gradient absolute inset-0"></div>
          <!-- Floating Elements -->
          <div class="floating-elements absolute inset-0">
            <div class="floating-circle floating-circle-1" 
                 appParallax 
                 [parallaxSpeed]="0.2"
                 [parallaxDirection]="'up'"></div>
            <div class="floating-circle floating-circle-2"
                 appParallax 
                 [parallaxSpeed]="0.4"
                 [parallaxDirection]="'down'"></div>
            <div class="floating-circle floating-circle-3"
                 appParallax 
                 [parallaxSpeed]="0.1"
                 [parallaxDirection]="'left'"></div>
          </div>
        </div>
        
        <!-- Hero Content -->
        <div class="hero-content relative z-10 container-modern">
          <div class="text-center space-y-8">
            <h1 class="hero-title text-gradient" 
                appScrollReveal 
                [animation]="'fade-in-up'"
                [delay]="0">
              發現生活的美好
            </h1>
            <p class="hero-subtitle" 
               appScrollReveal 
               [animation]="'fade-in-up'"
               [delay]="200">
              精選優質商品，為您帶來卓越購物體驗
            </p>
            
            <div class="hero-actions" 
                 appScrollReveal 
                 [animation]="'fade-in-up'"
                 [delay]="400">
              <button class="btn-primary-large hover-lift" 
                      routerLink="/products"
                      appFeedbackAnimation
                      [feedbackType]="'success'"
                      [animationType]="'ripple'"
                      [trigger]="'click'"
                      trackClick="hero_explore_products"
                      [trackClickData]="{
                        section: 'hero',
                        action: 'explore_products',
                        buttonType: 'primary',
                        position: 'hero_cta_primary'
                      }"
                      trackClickCategory="navigation">
                <i class="pi pi-shopping-cart btn-icon btn-icon-left"></i>
                立即探索
              </button>
              <button class="btn-secondary-large hover-lift ml-4"
                      appFeedbackAnimation
                      [feedbackType]="'info'"
                      [animationType]="'pulse'"
                      [trigger]="'hover'"
                      trackClick="hero_learn_more"
                      [trackClickData]="{
                        section: 'hero',
                        action: 'learn_more',
                        buttonType: 'secondary',
                        position: 'hero_cta_secondary'
                      }"
                      trackClickCategory="engagement">
                <i class="pi pi-info-circle btn-icon btn-icon-left"></i>
                了解更多
              </button>
            </div>
          </div>
          
          <!-- Floating Product Cards -->
          <div class="floating-product-cards" 
               appScrollReveal 
               [animation]="'fade-in-up'"
               [delay]="600"
               trackView="hero_floating_products_section"
               [trackViewData]="{
                 section: 'hero',
                 contentType: 'floating_products',
                 productCount: featuredProducts.slice(0,3).length
               }">
            <div *ngFor="let product of featuredProducts.slice(0,3); let i = index" 
                 class="product-card-float animate-float"
                 [style.animation-delay]="(i * 0.5) + 's'"
                 appProductHover
                 [hoverEffect]="'lift'"
                 [intensity]="'subtle'"
                 trackView="hero_floating_product_card"
                 [trackViewData]="{
                   productId: product.id,
                   productName: product.name,
                   productPrice: product.price.amount,
                   position: i + 1,
                   section: 'hero_floating'
                 }"
                 trackClick="hero_floating_product_click"
                 [trackClickData]="{
                   productId: product.id,
                   productName: product.name,
                   productPrice: product.price.amount,
                   position: i + 1,
                   section: 'hero_floating'
                 }"
                 [attr.data-product-id]="product.id">
              <div class="product-card-mini">
                <img [src]="product.images?.[0]?.url || '/assets/images/placeholder-product.jpg'"
                     [alt]="product.name"
                     class="product-mini-image"
                     appImageZoom
                     [zoomFactor]="1.1"
                     [hoverImage]="product.images?.[1]?.url">
                <div class="product-mini-info">
                  <h4 class="product-mini-title">{{ product.name }}</h4>
                  <span class="product-mini-price">NT$ {{ product.price.amount | number:'1.0-0' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Interactive Categories Showcase -->
      <section class="categories-showcase section">
        <div class="container-modern">
          <div class="section-header text-center mb-16">
            <h2 class="section-title" 
                appScrollReveal 
                [animation]="'fade-in-up'">探索分類</h2>
            <p class="section-subtitle" 
               appScrollReveal 
               [animation]="'fade-in-up'"
               [delay]="100">找到您喜愛的商品類型</p>
          </div>
          
          <div class="categories-grid">
            <div *ngFor="let category of categories; let i = index" 
                 class="category-card"
                 [class.active]="hoveredCategory === category.value"
                 (mouseenter)="hoveredCategory = category.value; trackCategoryHover(category, i)"
                 (mouseleave)="hoveredCategory = null"
                 [routerLink]="['/products']"
                 [queryParams]="{category: category.value}"
                 appScrollReveal 
                 [animation]="'fade-in-up'"
                 [delay]="i * 100"
                 appProductHover
                 [hoverEffect]="'tilt'"
                 [intensity]="'normal'"
                 appFeedbackAnimation
                 [feedbackType]="'info'"
                 [animationType]="'scale'"
                 [trigger]="'click'"
                 trackView="category_card_view"
                 [trackViewData]="{
                   categoryId: category.value,
                   categoryName: category.label,
                   position: i + 1,
                   section: 'categories_showcase'
                 }"
                 trackClick="category_card_click"
                 [trackClickData]="{
                   categoryId: category.value,
                   categoryName: category.label,
                   position: i + 1,
                   section: 'categories_showcase',
                   productCount: category.productCount
                 }"
                 trackClickCategory="navigation">
              
              <div class="category-image">
                <img [src]="category.image" 
                     [alt]="category.label"
                     appImageZoom
                     [zoomFactor]="1.15">
                <div class="category-overlay"></div>
              </div>
              
              <div class="category-content">
                <i [class]="category.icon" class="category-icon"></i>
                <h3 class="category-title">{{ category.label }}</h3>
                <p class="category-description">{{ category.description }}</p>
                <span class="category-count">{{ category.productCount }} 件商品</span>
              </div>
              
              <div class="category-hover-effect"></div>
            </div>
          </div>
        </div>
      </section>

      <!-- Modern Products Showcase -->
      <section class="products-showcase section bg-gray-50">
        <div class="container-modern">
          <div class="section-header text-center mb-16">
            <h2 class="section-title animate-fade-in-up">精選推薦</h2>
            <div class="section-controls animate-fade-in-up delay-100">
              <div class="filter-tabs">
                <button *ngFor="let filter of productFilters; let i = index" 
                        class="filter-tab"
                        [class.active]="activeFilter === filter.value"
                        (click)="setActiveFilter(filter.value)"
                        trackClick="product_filter_tab"
                        [trackClickData]="{
                          filterType: filter.value,
                          filterLabel: filter.label,
                          position: i + 1,
                          section: 'products_showcase',
                          previousFilter: activeFilter
                        }"
                        trackClickCategory="filter">
                  {{ filter.label }}
                </button>
              </div>
            </div>
          </div>
          
          <!-- Loading State with Modern Skeleton -->
          <div *ngIf="loading" class="products-grid">
            <app-skeleton-loader *ngFor="let item of [1,2,3,4,5,6,7,8]" 
                                 type="product-card">
            </app-skeleton-loader>
          </div>

          <!-- Modern Products Grid -->
          <div *ngIf="!loading && featuredProducts.length > 0" 
               class="products-grid">
            <div *ngFor="let product of filteredProducts; let i = index" 
                 class="product-card-modern gpu-accelerated"
                 [class.loading]="addingToCart[product.id]"
                 appScrollReveal 
                 [animation]="'fade-in-up'"
                 [delay]="i * 50"
                 appProductHover
                 [hoverEffect]="'lift'"
                 [intensity]="'normal'"
                 [showQuickActions]="true"
                 (mouseenter)="trackProductHoverStart(product, i)"
                 (mouseleave)="trackProductHoverEnd(product, i)"
                 trackView="product_card_view"
                 [trackViewData]="{
                   productId: product.id,
                   productName: product.name,
                   productPrice: product.price.amount,
                   productCategory: product.category,
                   position: i + 1,
                   section: 'products_showcase',
                   filter: activeFilter,
                   isNew: product.isNew,
                   isHot: product.isHot,
                   onSale: product.onSale
                 }"
                 [attr.data-product-id]="product.id">
              
              <!-- Product Image Container -->
              <div class="product-image-container">
                <img [src]="product.images?.[0]?.url || '/assets/images/placeholder-product.jpg'"
                     [alt]="product.name"
                     class="product-image"
                     (error)="onImageError($event)"
                     (mouseenter)="trackImageZoom(product, 1.2)"
                     appImageZoom
                     [zoomFactor]="1.2"
                     [hoverImage]="product.images?.[1]?.url"
                     trackClick="product_image_click"
                     [trackClickData]="{
                       productId: product.id,
                       productName: product.name,
                       imageUrl: product.images?.[0]?.url,
                       position: i + 1,
                       section: 'products_showcase',
                       action: 'image_click'
                     }"
                     trackClickCategory="product_interaction">
                
                <!-- Hover Image -->
                <img *ngIf="product.images?.[1]?.url"
                     [src]="product.images?.[1]?.url"
                     [alt]="product.name"
                     class="product-image-hover">
                
                <!-- Quick Actions -->
                <div class="product-quick-actions">
                  <button class="quick-action-btn" 
                          title="快速查看" 
                          (click)="quickView(product)"
                          trackClick="product_quick_view"
                          [trackClickData]="{
                            productId: product.id,
                            productName: product.name,
                            position: i + 1,
                            section: 'products_showcase',
                            action: 'quick_view'
                          }"
                          trackClickCategory="product_interaction">
                    <i class="pi pi-eye"></i>
                  </button>
                  <button class="quick-action-btn" 
                          title="加入願望清單"
                          [class.active]="isInWishlist(product.id)"
                          (click)="toggleWishlist(product)"
                          trackClick="product_wishlist_toggle"
                          [trackClickData]="{
                            productId: product.id,
                            productName: product.name,
                            position: i + 1,
                            section: 'products_showcase',
                            action: isInWishlist(product.id) ? 'remove_wishlist' : 'add_wishlist',
                            currentState: isInWishlist(product.id)
                          }"
                          trackClickCategory="wishlist">
                    <i class="pi pi-heart"></i>
                  </button>
                  <button class="quick-action-btn" 
                          title="比較" 
                          (click)="addToCompare(product)"
                          trackClick="product_compare_add"
                          [trackClickData]="{
                            productId: product.id,
                            productName: product.name,
                            position: i + 1,
                            section: 'products_showcase',
                            action: 'add_compare'
                          }"
                          trackClickCategory="product_interaction">
                    <i class="pi pi-refresh"></i>
                  </button>
                </div>
                
                <!-- Product Badges -->
                <div class="product-badges">
                  <span class="badge badge-new" *ngIf="product.isNew">新品</span>
                  <span class="badge badge-sale" *ngIf="product.onSale">特價</span>
                  <span class="badge badge-hot" *ngIf="product.isHot">熱銷</span>
                </div>
                
                <!-- Discount Percentage -->
                <div *ngIf="product.originalPrice && product.originalPrice > product.price.amount" 
                     class="discount-percentage">
                  -{{ getDiscountPercentage(product) }}%
                </div>
              </div>
              
              <!-- Product Information -->
              <div class="product-info">
                <div class="product-category">{{ getCategoryLabel(product.category) }}</div>
                <h3 class="product-title" 
                    [routerLink]="['/products', product.id]"
                    trackClick="product_title_click"
                    [trackClickData]="{
                      productId: product.id,
                      productName: product.name,
                      position: i + 1,
                      section: 'products_showcase',
                      action: 'title_click'
                    }"
                    trackClickCategory="navigation">{{ product.name }}</h3>
                <p class="product-description">{{ product.description }}</p>
                
                <!-- Product Rating -->
                <div class="product-rating" *ngIf="product.rating">
                  <div class="stars">
                    <i *ngFor="let star of [1,2,3,4,5]" 
                       class="pi pi-star-fill star"
                       [class.empty]="star > product.rating"></i>
                  </div>
                  <span class="rating-count">({{ product.reviewCount || 0 }})</span>
                </div>
                
                <!-- Product Pricing -->
                <div class="product-pricing">
                  <span class="price-current">NT$ {{ product.price.amount | number:'1.0-0' }}</span>
                  <span class="price-original" *ngIf="product.originalPrice && product.originalPrice > product.price.amount">
                    NT$ {{ product.originalPrice | number:'1.0-0' }}
                  </span>
                  <span class="price-discount" *ngIf="product.originalPrice && product.originalPrice > product.price.amount">
                    省 NT$ {{ (product.originalPrice - product.price.amount) | number:'1.0-0' }}
                  </span>
                </div>
                
                <!-- Product Actions -->
                <div class="product-actions">
                  <div class="product-actions-row">
                    <button class="btn-add-to-cart"
                            [disabled]="!product.inStock"
                            [class.loading]="addingToCart[product.id]"
                            (click)="addToCart(product)"
                            #addToCartBtn
                            appFeedbackAnimation
                            [feedbackType]="'success'"
                            [animationType]="'bounce'"
                            [trigger]="'manual'"
                            trackClick="product_add_to_cart"
                            [trackClickData]="{
                              productId: product.id,
                              productName: product.name,
                              productPrice: product.price.amount,
                              position: i + 1,
                              section: 'products_showcase',
                              action: 'add_to_cart',
                              inStock: product.inStock
                            }"
                            trackClickCategory="cart">
                      <i class="pi pi-shopping-cart cart-icon"></i>
                      <span>{{ product.inStock ? '加入購物車' : '缺貨中' }}</span>
                    </button>
                    <button class="btn-wishlist"
                            [class.active]="isInWishlist(product.id)"
                            (click)="toggleWishlist(product)"
                            appFeedbackAnimation
                            [feedbackType]="'info'"
                            [animationType]="'pulse'"
                            [trigger]="'click'"
                            trackClick="product_wishlist_main"
                            [trackClickData]="{
                              productId: product.id,
                              productName: product.name,
                              position: i + 1,
                              section: 'products_showcase',
                              action: isInWishlist(product.id) ? 'remove_wishlist' : 'add_wishlist',
                              currentState: isInWishlist(product.id)
                            }"
                            trackClickCategory="wishlist">
                      <i class="pi pi-heart"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- View All Products -->
          <div class="text-center mt-16 animate-fade-in-up">
            <button class="btn-primary-large hover-lift" 
                    routerLink="/products"
                    trackClick="view_all_products"
                    [trackClickData]="{
                      section: 'products_showcase',
                      action: 'view_all_products',
                      currentFilter: activeFilter,
                      visibleProductsCount: filteredProducts.length
                    }"
                    trackClickCategory="navigation">
              查看所有商品
              <i class="pi pi-arrow-right btn-icon btn-icon-right"></i>
            </button>
          </div>
        </div>
      </section>

      <!-- Enhanced Features Section -->
      <section class="features-section section bg-white">
        <div class="container-modern">
          <div class="features-grid stagger-fade-in">
            <div *ngFor="let feature of features" class="feature-card hover-lift">
              <div class="feature-icon">
                <i [class]="feature.icon"></i>
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-description">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Newsletter Section -->
      <section class="newsletter-section section bg-gradient-to-r from-primary-500 to-primary-600 text-white">
        <div class="container-modern">
          <div class="text-center space-y-8">
            <h2 class="text-3xl lg:text-4xl font-bold animate-fade-in-up">訂閱我們的電子報</h2>
            <p class="text-xl text-primary-100 animate-fade-in-up delay-100">
              獲取最新優惠資訊和新品通知
            </p>
            <div class="newsletter-form animate-fade-in-up delay-200">
              <div class="flex flex-col sm:flex-row gap-4 max-w-md mx-auto">
                <input type="email" 
                       placeholder="輸入您的電子郵件"
                       class="newsletter-input flex-1"
                       #newsletterEmail
                       trackClick="newsletter_email_focus"
                       [trackClickData]="{
                         section: 'newsletter',
                         action: 'email_input_focus'
                       }"
                       trackClickCategory="engagement">
                <button class="btn-secondary hover-lift"
                        (click)="subscribeNewsletter(newsletterEmail.value)"
                        trackClick="newsletter_subscribe"
                        [trackClickData]="{
                          section: 'newsletter',
                          action: 'subscribe_attempt',
                          hasEmail: !!newsletterEmail.value
                        }"
                        trackClickCategory="conversion">
                  訂閱
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    /* Modern Hero Section Styles */
    .hero-section {
      min-height: 100vh;
      display: flex;
      align-items: center;
      position: relative;
    }

    .hero-background {
      background: var(--gradient-hero);
    }

    .hero-gradient {
      background: linear-gradient(135deg, 
        rgba(20, 184, 166, 0.9) 0%, 
        rgba(13, 148, 136, 0.8) 50%, 
        rgba(245, 158, 11, 0.7) 100%);
    }

    .floating-elements {
      pointer-events: none;
    }

    .floating-circle {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
    }

    .floating-circle-1 {
      width: 200px;
      height: 200px;
      top: 10%;
      left: 10%;
      animation: float 6s ease-in-out infinite;
    }

    .floating-circle-2 {
      width: 150px;
      height: 150px;
      top: 60%;
      right: 15%;
      animation: float 8s ease-in-out infinite reverse;
    }

    .floating-circle-3 {
      width: 100px;
      height: 100px;
      bottom: 20%;
      left: 60%;
      animation: float 7s ease-in-out infinite;
    }

    .hero-content {
      padding: var(--spacing-20) 0;
    }

    .hero-title {
      font-size: clamp(2.5rem, 8vw, 4rem);
      font-weight: var(--font-weight-extrabold);
      line-height: var(--line-height-tight);
      margin-bottom: var(--spacing-6);
    }

    .hero-subtitle {
      font-size: clamp(1.125rem, 3vw, 1.5rem);
      color: rgba(255, 255, 255, 0.9);
      max-width: 600px;
      margin: 0 auto var(--spacing-8);
      line-height: var(--line-height-relaxed);
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--spacing-4);
      justify-content: center;
      margin-bottom: var(--spacing-16);
    }

    .floating-product-cards {
      display: none;
      position: absolute;
      top: 50%;
      left: 0;
      right: 0;
      transform: translateY(-50%);
      pointer-events: none;
    }

    @media (min-width: 1200px) {
      .floating-product-cards {
        display: block;
      }
    }

    .product-card-float {
      position: absolute;
      width: 200px;
    }

    .product-card-float:nth-child(1) {
      top: -100px;
      left: 5%;
    }

    .product-card-float:nth-child(2) {
      top: 50px;
      right: 5%;
    }

    .product-card-float:nth-child(3) {
      bottom: -50px;
      left: 15%;
    }

    .product-card-mini {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(20px);
      border-radius: var(--radius-2xl);
      padding: var(--spacing-4);
      box-shadow: var(--shadow-xl);
      transition: all var(--transition-normal);
    }

    .product-card-mini:hover {
      transform: scale(1.05);
      box-shadow: var(--shadow-2xl);
    }

    .product-mini-image {
      width: 100%;
      height: 120px;
      object-fit: cover;
      border-radius: var(--radius-lg);
      margin-bottom: var(--spacing-3);
    }

    .product-mini-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-gray-900);
      margin-bottom: var(--spacing-1);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .product-mini-price {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-bold);
      color: var(--color-primary-600);
    }

    /* Section Styles */
    .section-header {
      text-align: center;
    }

    .section-title {
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-gray-900);
      margin-bottom: var(--spacing-4);
    }

    .section-subtitle {
      font-size: var(--font-size-lg);
      color: var(--color-gray-600);
      line-height: var(--line-height-relaxed);
    }

    .section-controls {
      margin-top: var(--spacing-8);
    }

    .filter-tabs {
      display: flex;
      justify-content: center;
      gap: var(--spacing-2);
      flex-wrap: wrap;
    }

    .filter-tab {
      padding: var(--spacing-2) var(--spacing-4);
      border: 2px solid var(--color-gray-200);
      background: white;
      color: var(--color-gray-600);
      border-radius: var(--radius-lg);
      font-weight: var(--font-weight-medium);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .filter-tab:hover {
      border-color: var(--color-primary-300);
      color: var(--color-primary-600);
    }

    .filter-tab.active {
      background: var(--color-primary-500);
      border-color: var(--color-primary-500);
      color: white;
    }

    /* Product Card Skeleton */
    .product-card-skeleton {
      background: white;
      border-radius: var(--radius-2xl);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }

    .skeleton-image {
      aspect-ratio: 1;
      margin-bottom: 0;
    }

    .skeleton-content {
      padding: var(--spacing-6);
    }

    /* Newsletter Section */
    .newsletter-section {
      background: var(--gradient-primary);
    }

    .newsletter-input {
      padding: var(--spacing-3) var(--spacing-4);
      border: 2px solid rgba(255, 255, 255, 0.2);
      border-radius: var(--radius-lg);
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      color: white;
      font-size: var(--font-size-base);
    }

    .newsletter-input::placeholder {
      color: rgba(255, 255, 255, 0.7);
    }

    .newsletter-input:focus {
      outline: none;
      border-color: rgba(255, 255, 255, 0.5);
      background: rgba(255, 255, 255, 0.15);
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .hero-actions {
        flex-direction: column;
        align-items: center;
      }

      .btn-primary-large,
      .btn-secondary-large {
        width: 100%;
        max-width: 280px;
      }

      .filter-tabs {
        justify-content: flex-start;
        overflow-x: auto;
        padding-bottom: var(--spacing-2);
      }

      .filter-tab {
        white-space: nowrap;
        flex-shrink: 0;
      }
    }

    /* GPU Acceleration for smooth animations */
    .gpu-accelerated {
      transform: translateZ(0);
      backface-visibility: hidden;
      perspective: 1000px;
    }

    /* Loading state for product cards */
    .product-card-modern.loading {
      opacity: 0.7;
      pointer-events: none;
    }

    .product-card-modern.loading .product-image {
      filter: blur(1px);
    }

    /* Enhanced hover effects */
    .product-card-modern:hover .product-image {
      transform: scale(1.05);
    }

    .category-card:hover .category-image img {
      transform: scale(1.1);
    }

    /* Smooth transitions */
    .product-image,
    .category-image img {
      transition: transform var(--duration-500) var(--easing-ease-out);
    }

    /* Animation performance optimizations */
    .floating-circle,
    .product-card-float,
    .hero-background {
      will-change: transform;
    }

    /* Accessibility improvements */
    @media (prefers-reduced-motion: reduce) {
      .floating-circle,
      .product-card-float,
      .hero-background,
      .product-image,
      .category-image img {
        animation: none !important;
        transition: none !important;
        transform: none !important;
      }
    }
  `]
})
export class HomeComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // State
  featuredProducts: Product[] = [];
  filteredProducts: Product[] = [];
  loading = true;
  addingToCart: { [key: string]: boolean } = {};
  wishlist: Set<string> = new Set();
  hoveredCategory: string | null = null;
  activeFilter = 'all';

  // Data
  categories = [
    {
      label: '電子產品',
      value: 'ELECTRONICS',
      icon: 'pi pi-mobile',
      image: '/assets/images/categories/electronics.jpg',
      description: '最新科技產品',
      productCount: 156
    },
    {
      label: '時尚服飾',
      value: 'FASHION',
      icon: 'pi pi-shopping-bag',
      image: '/assets/images/categories/fashion.jpg',
      description: '潮流時尚單品',
      productCount: 234
    },
    {
      label: '居家生活',
      value: 'HOME_LIVING',
      icon: 'pi pi-home',
      image: '/assets/images/categories/home.jpg',
      description: '舒適居家用品',
      productCount: 189
    },
    {
      label: '運動健身',
      value: 'SPORTS_FITNESS',
      icon: 'pi pi-heart',
      image: '/assets/images/categories/sports.jpg',
      description: '運動健身器材',
      productCount: 98
    },
    {
      label: '美妝保養',
      value: 'BEAUTY_CARE',
      icon: 'pi pi-star',
      image: '/assets/images/categories/beauty.jpg',
      description: '美妝護膚產品',
      productCount: 167
    },
    {
      label: '食品飲料',
      value: 'FOOD_BEVERAGE',
      icon: 'pi pi-apple',
      image: '/assets/images/categories/food.jpg',
      description: '新鮮食品飲料',
      productCount: 145
    }
  ];

  productFilters = [
    { label: '全部', value: 'all' },
    { label: '新品', value: 'new' },
    { label: '熱銷', value: 'hot' },
    { label: '特價', value: 'sale' }
  ];

  features = [
    {
      icon: 'pi pi-truck',
      title: '快速配送',
      description: '全台24小時快速到貨服務，讓您購物無憂'
    },
    {
      icon: 'pi pi-shield',
      title: '品質保證',
      description: '嚴選優質商品，每件商品都經過品質檢驗'
    },
    {
      icon: 'pi pi-headphones',
      title: '客戶服務',
      description: '專業客服團隊，7x24小時為您提供服務'
    }
  ];

  // Page loader state
  showPageLoader = true;
  pageLoaderFadeOut = false;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private smoothScrollService: SmoothScrollService,
    private toastService: ToastService,
    private observabilityService: ObservabilityService,
    private webVitalsService: WebVitalsService,
    private userBehaviorAnalytics: UserBehaviorAnalyticsService,
    private errorTrackingService: ErrorTrackingService
  ) { }

  ngOnInit() {
    this.initializePageLoader();
    this.loadFeaturedProducts();
    this.loadWishlist();
    this.startAutoRotation();
    this.setupScrollEffects();
    this.trackPageView();
    this.trackPageLoadPerformance();

    // Setup performance monitoring after initial render
    setTimeout(() => {
      this.setupPerformanceMonitoring();
    }, 100);
  }

  private initializePageLoader() {
    // Simulate page loading
    setTimeout(() => {
      this.pageLoaderFadeOut = true;
      setTimeout(() => {
        this.showPageLoader = false;
      }, 500);
    }, 2000);
  }

  private setupScrollEffects() {
    // Setup parallax elements
    const heroBackground = document.querySelector('.hero-background') as HTMLElement;
    if (heroBackground) {
      this.smoothScrollService.registerParallaxElement(heroBackground, {
        speed: 0.5,
        direction: 'up'
      });
    }

    // Setup auto-hide header
    this.smoothScrollService.setupAutoHideHeader('.header');
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.smoothScrollService.destroy();
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: Event) {
    // Add scroll-based animations or effects here
    const scrolled = window.pageYOffset;
    const parallax = document.querySelector('.hero-background') as HTMLElement;
    if (parallax) {
      parallax.style.transform = `translateY(${scrolled * 0.5}px)`;
    }
  }

  loadFeaturedProducts() {
    this.loading = true;
    this.productService.getFeaturedProducts(12).subscribe({
      next: (response: ProductListResponse) => {
        this.featuredProducts = response.content.map(product => ({
          ...product,
          isNew: Math.random() > 0.7,
          isHot: Math.random() > 0.8,
          onSale: Math.random() > 0.6,
          rating: Math.floor(Math.random() * 2) + 4, // 4-5 stars
          reviewCount: Math.floor(Math.random() * 100) + 10,
          originalPrice: product.price.amount * (1 + Math.random() * 0.3) // Add original price for discounts
        }));
        this.filteredProducts = this.featuredProducts;
        this.loading = false;
      },
      error: (error) => {
        console.warn('Backend API not available, using mock data:', error);

        // Use mock data when API is not available
        this.featuredProducts = this.getMockProducts();
        this.filteredProducts = this.featuredProducts;
        this.loading = false;

        // Enhanced error tracking
        this.errorTrackingService.trackUserOperationError({
          operation: 'load_featured_products',
          errorType: this.categorizeError(error),
          message: error.message || 'Using mock data - backend not available'
        }, {
          component: 'HomeComponent',
          action: 'load_data_fallback',
          additionalData: {
            apiEndpoint: 'getFeaturedProducts',
            errorStatus: error.status,
            usingMockData: true
          }
        });

        // Show info message instead of error
        this.toastService.info('展示範例商品資料', '後端服務未連接，正在顯示示範數據');
      }
    });
  }

  private getMockProducts(): any[] {
    return [
      {
        id: 'mock-1',
        name: 'iPhone 15 Pro Max',
        description: '最新旗艦手機，配備 A17 Pro 晶片，鈦金屬邊框設計',
        price: { amount: 39900, currency: 'TWD' },
        originalPrice: 45900,
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/iphone/400/400', alt: 'iPhone 15 Pro Max' },
          { url: 'https://picsum.photos/seed/iphone2/400/400', alt: 'iPhone 15 Pro Max - View 2' }
        ],
        inStock: true,
        isNew: true,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 128
      },
      {
        id: 'mock-2',
        name: 'MacBook Pro 14"',
        description: 'M3 Pro 晶片，16GB 記憶體，極致效能體驗',
        price: { amount: 59900, currency: 'TWD' },
        originalPrice: 64900,
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/macbook/400/400', alt: 'MacBook Pro' }
        ],
        inStock: true,
        isNew: true,
        isHot: false,
        onSale: true,
        rating: 5,
        reviewCount: 89
      },
      {
        id: 'mock-3',
        name: 'AirPods Pro (第二代)',
        description: '主動式降噪，空間音訊，USB-C 充電',
        price: { amount: 7490, currency: 'TWD' },
        originalPrice: 8490,
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/airpods/400/400', alt: 'AirPods Pro' }
        ],
        inStock: true,
        isNew: false,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 256
      },
      {
        id: 'mock-4',
        name: 'Nike Air Max 2024',
        description: '經典氣墊科技，舒適透氣，運動時尚必備',
        price: { amount: 4280, currency: 'TWD' },
        category: 'FASHION',
        images: [
          { url: 'https://picsum.photos/seed/nike/400/400', alt: 'Nike Air Max' }
        ],
        inStock: true,
        isNew: true,
        isHot: false,
        onSale: false,
        rating: 4,
        reviewCount: 67
      },
      {
        id: 'mock-5',
        name: 'Sony WH-1000XM5',
        description: '業界領先降噪耳機，30小時續航',
        price: { amount: 9990, currency: 'TWD' },
        originalPrice: 11990,
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/sony/400/400', alt: 'Sony Headphones' }
        ],
        inStock: true,
        isNew: false,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 145
      },
      {
        id: 'mock-6',
        name: 'Dyson V15 吸塵器',
        description: '雷射偵測灰塵，強大吸力，智能顯示',
        price: { amount: 21900, currency: 'TWD' },
        category: 'HOME',
        images: [
          { url: 'https://picsum.photos/seed/dyson/400/400', alt: 'Dyson V15' }
        ],
        inStock: true,
        isNew: true,
        isHot: false,
        onSale: false,
        rating: 5,
        reviewCount: 92
      },
      {
        id: 'mock-7',
        name: 'Samsung The Frame 電視',
        description: '55吋 4K QLED，藝術模式，美學與科技結合',
        price: { amount: 35900, currency: 'TWD' },
        originalPrice: 42900,
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/samsung/400/400', alt: 'Samsung TV' }
        ],
        inStock: true,
        isNew: false,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 78
      },
      {
        id: 'mock-8',
        name: 'Nespresso 咖啡機',
        description: '膠囊咖啡機，19 bar 壓力，專業級萃取',
        price: { amount: 5990, currency: 'TWD' },
        category: 'HOME',
        images: [
          { url: 'https://picsum.photos/seed/nespresso/400/400', alt: 'Nespresso' }
        ],
        inStock: true,
        isNew: false,
        isHot: false,
        onSale: false,
        rating: 4,
        reviewCount: 134
      },
      {
        id: 'mock-9',
        name: 'Lululemon 瑜珈墊',
        description: '5mm 厚度，防滑設計，環保材質',
        price: { amount: 2480, currency: 'TWD' },
        category: 'SPORTS',
        images: [
          { url: 'https://picsum.photos/seed/yoga/400/400', alt: 'Yoga Mat' }
        ],
        inStock: true,
        isNew: true,
        isHot: false,
        onSale: false,
        rating: 5,
        reviewCount: 45
      },
      {
        id: 'mock-10',
        name: 'The North Face 登山背包',
        description: '35L 容量，防水設計，透氣背負系統',
        price: { amount: 5680, currency: 'TWD' },
        originalPrice: 6980,
        category: 'SPORTS',
        images: [
          { url: 'https://picsum.photos/seed/backpack/400/400', alt: 'Backpack' }
        ],
        inStock: true,
        isNew: false,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 67
      },
      {
        id: 'mock-11',
        name: 'Bose SoundLink 藍牙音箱',
        description: '360度環繞音效，12小時續航，防水設計',
        price: { amount: 6990, currency: 'TWD' },
        category: 'ELECTRONICS',
        images: [
          { url: 'https://picsum.photos/seed/bose/400/400', alt: 'Bose Speaker' }
        ],
        inStock: true,
        isNew: false,
        isHot: false,
        onSale: false,
        rating: 4,
        reviewCount: 98
      },
      {
        id: 'mock-12',
        name: 'Herman Miller 人體工學椅',
        description: '12年保固，全網背設計，完美支撐',
        price: { amount: 29900, currency: 'TWD' },
        originalPrice: 35900,
        category: 'HOME',
        images: [
          { url: 'https://picsum.photos/seed/chair/400/400', alt: 'Office Chair' }
        ],
        inStock: true,
        isNew: true,
        isHot: true,
        onSale: true,
        rating: 5,
        reviewCount: 156
      }
    ];
  }

  loadWishlist() {
    // Load wishlist from localStorage or service
    const savedWishlist = localStorage.getItem('wishlist');
    if (savedWishlist) {
      this.wishlist = new Set(JSON.parse(savedWishlist));
    }
  }

  startAutoRotation() {
    // Auto-rotate featured products or categories
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        // Implement auto-rotation logic if needed
      });
  }

  setActiveFilter(filter: string) {
    const previousFilter = this.activeFilter;
    this.activeFilter = filter;
    this.filterProducts();

    // Track filter change
    this.observabilityService.trackBusinessEvent({
      type: 'filter_apply',
      data: {
        filterType: 'product_showcase',
        newFilter: filter,
        previousFilter: previousFilter,
        section: 'products_showcase',
        resultCount: this.filteredProducts.length
      },
      timestamp: Date.now(),
      sessionId: this.observabilityService.getSessionId()
    });

    // Enhanced behavior tracking - filter usage
    this.userBehaviorAnalytics.trackFilterUsage(
      'product_showcase',
      filter,
      this.filteredProducts.length
    );
  }

  filterProducts() {
    switch (this.activeFilter) {
      case 'new':
        this.filteredProducts = this.featuredProducts.filter(p => (p as any).isNew);
        break;
      case 'hot':
        this.filteredProducts = this.featuredProducts.filter(p => (p as any).isHot);
        break;
      case 'sale':
        this.filteredProducts = this.featuredProducts.filter(p => (p as any).onSale);
        break;
      default:
        this.filteredProducts = this.featuredProducts;
    }
  }

  addToCart(product: Product, event?: Event) {
    if (!product.inStock) return;

    // Track add to cart attempt
    this.observabilityService.trackBusinessEvent({
      type: 'cart_add',
      data: {
        productId: product.id,
        productName: product.name,
        productPrice: product.price.amount,
        productCategory: product.category,
        section: 'products_showcase',
        inStock: product.inStock
      },
      timestamp: Date.now(),
      sessionId: this.observabilityService.getSessionId()
    });

    // Enhanced behavior tracking - conversion funnel
    this.userBehaviorAnalytics.trackConversionStep({
      step: 'add_to_cart',
      productId: product.id,
      value: product.price.amount,
      timestamp: Date.now(),
      metadata: {
        productName: product.name,
        productCategory: product.category,
        section: 'products_showcase'
      }
    });

    // Track product interaction
    this.userBehaviorAnalytics.trackProductInteraction({
      productId: product.id,
      productName: product.name,
      interactionType: 'add_to_cart',
      section: 'products_showcase'
    });

    // Track cart interaction
    this.userBehaviorAnalytics.trackCartInteraction('add_item', {
      productId: product.id,
      productName: product.name,
      productPrice: product.price.amount
    });

    this.addingToCart[product.id] = true;

    // Show loading feedback
    if (event?.target) {
      this.toastService.loadingFeedback(event.target as HTMLElement, true);
    }

    this.cartService.addToCart({
      productId: product.id,
      quantity: 1
    }).subscribe({
      next: () => {
        this.addingToCart[product.id] = false;

        // Show success feedback
        if (event?.target) {
          this.toastService.loadingFeedback(event.target as HTMLElement, false);
          this.toastService.buttonSuccess(event.target as HTMLElement);
        }

        // Show success toast
        this.toastService.quickSuccess(`${product.name} 已加入購物車`);
      },
      error: (error) => {
        console.error('Error adding to cart:', error);
        this.addingToCart[product.id] = false;

        // Enhanced error tracking
        this.errorTrackingService.trackUserOperationError({
          operation: 'add_to_cart',
          errorType: this.categorizeError(error),
          message: error.message || 'Failed to add item to cart'
        }, {
          component: 'HomeComponent',
          action: 'add_to_cart',
          additionalData: {
            productId: product.id,
            productName: product.name,
            productPrice: product.price.amount
          }
        });

        // Show error feedback
        if (event?.target) {
          this.toastService.loadingFeedback(event.target as HTMLElement, false);
          this.toastService.buttonError(event.target as HTMLElement);
        }

        // Show error toast
        this.toastService.quickError('加入購物車失敗，請稍後再試');
      }
    });
  }

  toggleWishlist(product: Product, event?: Event) {
    const isAdding = !this.wishlist.has(product.id);

    // Track wishlist action
    this.observabilityService.trackBusinessEvent({
      type: 'wishlist_action',
      data: {
        productId: product.id,
        productName: product.name,
        productPrice: product.price.amount,
        action: isAdding ? 'add' : 'remove',
        section: 'products_showcase'
      },
      timestamp: Date.now(),
      sessionId: this.observabilityService.getSessionId()
    });

    // Enhanced behavior tracking
    this.userBehaviorAnalytics.trackProductInteraction({
      productId: product.id,
      productName: product.name,
      interactionType: 'add_to_wishlist',
      section: 'products_showcase'
    });

    if (isAdding) {
      this.wishlist.add(product.id);
      this.toastService.quickSuccess(`${product.name} 已加入願望清單`);
    } else {
      this.wishlist.delete(product.id);
      this.toastService.info('已移除', `${product.name} 已從願望清單移除`);
    }

    // Save to localStorage
    localStorage.setItem('wishlist', JSON.stringify(Array.from(this.wishlist)));

    // Show feedback animation
    if (event?.target) {
      this.toastService.successFeedback(event.target as HTMLElement);
    }

    // TODO: Sync with backend service
  }

  isInWishlist(productId: string): boolean {
    return this.wishlist.has(productId);
  }

  quickView(product: Product) {
    // Track quick view action
    this.observabilityService.trackBusinessEvent({
      type: 'product_view',
      data: {
        productId: product.id,
        productName: product.name,
        viewType: 'quick_view',
        section: 'products_showcase'
      },
      timestamp: Date.now(),
      sessionId: this.observabilityService.getSessionId()
    });

    // Enhanced behavior tracking - conversion funnel
    this.userBehaviorAnalytics.trackConversionStep({
      step: 'product_view',
      productId: product.id,
      timestamp: Date.now(),
      metadata: {
        viewType: 'quick_view',
        productName: product.name,
        section: 'products_showcase'
      }
    });

    // Track product interaction
    this.userBehaviorAnalytics.trackProductInteraction({
      productId: product.id,
      productName: product.name,
      interactionType: 'view',
      section: 'products_showcase'
    });

    // TODO: Open quick view modal
    console.log('Quick view for product:', product.name);
  }

  addToCompare(product: Product) {
    // TODO: Add to comparison list
    console.log('Add to compare:', product.name);
  }

  getDiscountPercentage(product: Product): number {
    const originalPrice = (product as any).originalPrice;
    if (!originalPrice || originalPrice <= product.price.amount) {
      return 0;
    }
    return Math.round(((originalPrice - product.price.amount) / originalPrice) * 100);
  }

  getCategoryLabel(category: string): string {
    const categoryMap: { [key: string]: string } = {
      'ELECTRONICS': '電子產品',
      'FASHION': '時尚服飾',
      'HOME_LIVING': '居家生活',
      'SPORTS_FITNESS': '運動健身',
      'BEAUTY_CARE': '美妝保養',
      'FOOD_BEVERAGE': '食品飲料',
      'BOOKS_STATIONERY': '書籍文具',
      'TOYS_GAMES': '玩具遊戲',
      'AUTOMOTIVE': '汽車用品',
      'OTHER': '其他'
    };
    return categoryMap[category] || category;
  }

  onImageError(event: any) {
    const img = event.target as HTMLImageElement;
    const originalSrc = img.src;

    // Track image load error
    this.errorTrackingService.trackImageLoadError({
      src: originalSrc,
      alt: img.alt,
      element: img.className
    }, {
      component: 'HomeComponent',
      action: 'image_load',
      additionalData: {
        section: this.getImageSection(img)
      }
    });

    // Set fallback image
    img.src = '/assets/images/placeholder-product.jpg';

    // Track fallback usage
    this.observabilityService.trackUserAction('image_fallback_used', {
      originalSrc: this.sanitizeUrl(originalSrc),
      fallbackSrc: '/assets/images/placeholder-product.jpg',
      imageType: this.getImageType(img),
      section: this.getImageSection(img)
    });
  }

  private sanitizeUrl(url: string): string {
    try {
      const urlObj = new URL(url);
      return `${urlObj.origin}${urlObj.pathname}`;
    } catch {
      return url.split('?')[0];
    }
  }

  // Observability tracking methods
  private trackPageView() {
    this.observabilityService.trackPageView('/home', {
      pageTitle: '首頁',
      pageType: 'home',
      featuredProductsCount: this.featuredProducts.length,
      categoriesCount: this.categories.length,
      userAgent: navigator.userAgent,
      loadTime: Date.now()
    });
  }

  trackCategoryHover(category: any, position: number) {
    this.observabilityService.trackUserAction('category_hover', {
      categoryId: category.value,
      categoryName: category.label,
      position: position + 1,
      section: 'categories_showcase',
      productCount: category.productCount,
      hoverTimestamp: Date.now()
    });

    // Enhanced behavior tracking
    this.userBehaviorAnalytics.trackProductInteraction({
      productId: category.value,
      productName: category.label,
      interactionType: 'hover',
      position: position + 1,
      section: 'categories_showcase'
    });
  }

  subscribeNewsletter(email: string) {
    if (!email || !email.includes('@')) {
      this.toastService.quickError('請輸入有效的電子郵件地址');
      return;
    }

    // Track newsletter subscription attempt
    this.observabilityService.trackBusinessEvent({
      type: 'newsletter_subscription',
      data: {
        email: email,
        source: 'home_page',
        section: 'newsletter',
        timestamp: Date.now()
      },
      timestamp: Date.now(),
      sessionId: this.observabilityService.getSessionId()
    });

    // TODO: Implement actual newsletter subscription
    this.toastService.quickSuccess('感謝您的訂閱！');
  }

  // Enhanced user behavior tracking methods
  trackProductHoverStart(product: Product, position: number) {
    this.userBehaviorAnalytics.trackProductHover(
      product.id,
      product.name,
      'products_showcase'
    );
  }

  trackProductHoverEnd(product: Product, position: number) {
    this.userBehaviorAnalytics.trackProductHoverEnd(
      product.id,
      product.name,
      'products_showcase'
    );
  }

  trackImageZoom(product: Product, zoomLevel: number) {
    this.userBehaviorAnalytics.trackProductInteraction({
      productId: product.id,
      productName: product.name,
      interactionType: 'zoom',
      section: 'products_showcase'
    });

    this.observabilityService.trackUserAction('product_image_zoom', {
      productId: product.id,
      productName: product.name,
      zoomLevel: zoomLevel,
      section: 'products_showcase'
    });
  }

  trackSearchInteraction(query: string, resultsCount?: number) {
    this.userBehaviorAnalytics.trackSearchBehavior({
      query: query,
      resultsCount: resultsCount,
      refinements: []
    });
  }

  // Performance monitoring methods
  private setupPerformanceMonitoring() {
    // Monitor image loading performance
    this.monitorImageLoading();

    // Monitor animation performance
    this.monitorAnimationPerformance();

    // Monitor API call performance (already handled by interceptors)

    // Collect Web Vitals metrics
    this.webVitalsService.collectCurrentMetrics();
  }

  private monitorImageLoading() {
    // Monitor product images loading
    const productImages = document.querySelectorAll('.product-image, .product-mini-image, .category-image img');

    productImages.forEach((img, index) => {
      const startTime = performance.now();

      img.addEventListener('load', () => {
        const loadTime = performance.now() - startTime;

        this.observabilityService.trackPerformanceMetric({
          type: 'page_load', // Using page_load type for image loading
          value: loadTime,
          page: window.location.pathname,
          timestamp: Date.now()
        });

        // Track business metric for image loading
        this.observabilityService.trackUserAction('image_load_success', {
          imageIndex: index,
          loadTime: loadTime,
          imageType: this.getImageType(img),
          section: this.getImageSection(img)
        });
      });

      img.addEventListener('error', () => {
        const errorTime = performance.now() - startTime;

        this.observabilityService.trackUserAction('image_load_error', {
          imageIndex: index,
          errorTime: errorTime,
          imageType: this.getImageType(img),
          section: this.getImageSection(img),
          imageSrc: (img as HTMLImageElement).src
        });
      });
    });
  }

  private monitorAnimationPerformance() {
    // Monitor scroll reveal animations
    const animatedElements = document.querySelectorAll('[appScrollReveal]');

    animatedElements.forEach((element, index) => {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const startTime = performance.now();

            // Monitor animation completion
            element.addEventListener('animationend', () => {
              const animationDuration = performance.now() - startTime;

              this.observabilityService.trackPerformanceMetric({
                type: 'page_load', // Using page_load type for animation performance
                value: animationDuration,
                page: window.location.pathname,
                timestamp: Date.now()
              });

              this.observabilityService.trackUserAction('animation_complete', {
                elementIndex: index,
                animationDuration: animationDuration,
                animationType: 'scroll_reveal',
                section: this.getElementSection(element)
              });
            }, { once: true });
          }
        });
      });

      observer.observe(element);
    });

    // Monitor hover animations
    const hoverElements = document.querySelectorAll('[appProductHover], [appImageZoom]');

    hoverElements.forEach((element, index) => {
      let hoverStartTime: number;

      element.addEventListener('mouseenter', () => {
        hoverStartTime = performance.now();
      });

      element.addEventListener('mouseleave', () => {
        if (hoverStartTime) {
          const hoverDuration = performance.now() - hoverStartTime;

          this.observabilityService.trackUserAction('hover_animation_complete', {
            elementIndex: index,
            hoverDuration: hoverDuration,
            animationType: 'hover_effect',
            section: this.getElementSection(element)
          });
        }
      });
    });
  }

  private getImageType(img: Element): string {
    const classList = img.className;

    if (classList.includes('product-image')) return 'product_main';
    if (classList.includes('product-mini-image')) return 'product_mini';
    if (classList.includes('category-image')) return 'category';
    if (classList.includes('hero-image')) return 'hero';

    return 'unknown';
  }

  private getImageSection(img: Element): string {
    const section = img.closest('.hero-section, .categories-showcase, .products-showcase, .features-section');

    if (section?.classList.contains('hero-section')) return 'hero';
    if (section?.classList.contains('categories-showcase')) return 'categories';
    if (section?.classList.contains('products-showcase')) return 'products';
    if (section?.classList.contains('features-section')) return 'features';

    return 'unknown';
  }

  private getElementSection(element: Element): string {
    const section = element.closest('.hero-section, .categories-showcase, .products-showcase, .features-section, .newsletter-section');

    if (section?.classList.contains('hero-section')) return 'hero';
    if (section?.classList.contains('categories-showcase')) return 'categories';
    if (section?.classList.contains('products-showcase')) return 'products';
    if (section?.classList.contains('features-section')) return 'features';
    if (section?.classList.contains('newsletter-section')) return 'newsletter';

    return 'unknown';
  }

  // Enhanced page load monitoring
  private trackPageLoadPerformance() {
    // Track page load completion
    window.addEventListener('load', () => {
      const loadTime = performance.now();

      this.observabilityService.trackPerformanceMetric({
        type: 'page_load',
        value: loadTime,
        page: window.location.pathname,
        timestamp: Date.now()
      });

      // Track specific page metrics
      this.trackPageSpecificMetrics();
    });

    // Track DOM content loaded
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        const domLoadTime = performance.now();

        this.observabilityService.trackPerformanceMetric({
          type: 'page_load',
          value: domLoadTime,
          page: window.location.pathname,
          timestamp: Date.now()
        });
      });
    }
  }

  private trackPageSpecificMetrics() {
    // Track hero section render time
    const heroSection = document.querySelector('.hero-section');
    if (heroSection) {
      const heroRenderTime = performance.now();

      this.observabilityService.trackUserAction('section_render_complete', {
        section: 'hero',
        renderTime: heroRenderTime,
        elementCount: heroSection.querySelectorAll('*').length
      });
    }

    // Track products grid render time
    const productsGrid = document.querySelector('.products-grid');
    if (productsGrid) {
      const productsRenderTime = performance.now();
      const productCount = productsGrid.querySelectorAll('.product-card-modern').length;

      this.observabilityService.trackUserAction('section_render_complete', {
        section: 'products_grid',
        renderTime: productsRenderTime,
        productCount: productCount
      });
    }

    // Track categories grid render time
    const categoriesGrid = document.querySelector('.categories-grid');
    if (categoriesGrid) {
      const categoriesRenderTime = performance.now();
      const categoryCount = categoriesGrid.querySelectorAll('.category-card').length;

      this.observabilityService.trackUserAction('section_render_complete', {
        section: 'categories_grid',
        renderTime: categoriesRenderTime,
        categoryCount: categoryCount
      });
    }
  }

  // Error handling and categorization methods
  private categorizeError(error: any): 'validation' | 'network' | 'permission' | 'timeout' | 'unknown' {
    if (error.status === 400) return 'validation';
    if (error.status === 401 || error.status === 403) return 'permission';
    if (error.status === 0 || error.name === 'TimeoutError') return 'network';
    if (error.name === 'TimeoutError') return 'timeout';
    return 'unknown';
  }

  // Animation error tracking
  trackAnimationError(element: HTMLElement, animationType: string, error: string) {
    this.errorTrackingService.trackAnimationError(
      animationType,
      element.className,
      error,
      {
        component: 'HomeComponent',
        additionalData: {
          section: this.getElementSection(element)
        }
      }
    );
  }

  // Network error tracking for failed API calls
  trackNetworkError(url: string, error: string) {
    this.errorTrackingService.trackNetworkError(
      url,
      error,
      {
        component: 'HomeComponent',
        action: 'api_call'
      }
    );
  }

}