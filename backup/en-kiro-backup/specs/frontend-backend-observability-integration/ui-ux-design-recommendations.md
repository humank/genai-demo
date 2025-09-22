
# Design

## Design

### Design

#### **1. Apple Store 風格 - 極簡主義**

- **特色**: 大量白色空間、產品為中心、簡潔導航
- **色彩**: 白色主調 + 品牌色點綴
- **佈局**: 大尺寸產品圖 + 最小化文字

#### **2. Nike/Adidas 風格 - 動態沉浸式**

- **特色**: 全屏英雄區、動態背景、強烈視覺衝擊
- **色彩**: 品牌色主導 + 高對比度
- **佈局**: 視頻背景 + 分層內容

#### **3. Shopify Plus 商店 - 現代卡片式**

- **特色**: 卡片式佈局、微動畫、響應式設計
- **色彩**: 柔和漸變 + 現代配色
- **佈局**: 網格系統 + 智能間距

## Design

### Design

結合 Apple 的簡潔性和 Nike 的動態感，創造既專業又吸引人的購物體驗。

## 🌈 **新色彩系統recommendations**

### **主色調方案 1: 現代藍綠漸變**

```scss
// 主要品牌色
$primary-50: #f0fdfa;   // 極淺青色
$primary-100: #ccfbf1;  // 淺青色
$primary-500: #14b8a6;  // 主青色 (Teal)
$primary-600: #0d9488;  // 深青色
$primary-900: #134e4a;  // 極深青色

// 輔助色
$secondary-500: #f59e0b; // 琥珀色 (用於 CTA 按鈕)
$accent-500: #ec4899;    // 粉紅色 (用於促銷標籤)

// 中性色
$gray-50: #f9fafb;
$gray-100: #f3f4f6;
$gray-500: #6b7280;
$gray-900: #111827;
```

### **主色調方案 2: 溫暖橙紫漸變**

```scss
// 主要品牌色
$primary-50: #fef7ff;   // 極淺紫色
$primary-100: #fce7f3;  // 淺紫色
$primary-500: #a855f7;  // 主紫色
$primary-600: #9333ea;  // 深紫色
$primary-900: #581c87;  // 極深紫色

// 輔助色
$secondary-500: #f97316; // 橙色 (用於 CTA 按鈕)
$accent-500: #06b6d4;    // 青色 (用於資訊標籤)
```

## Design

### Design

#### **英雄區塊 (Hero Section)**

```html
<!-- 現代化英雄區塊 -->
<section class="hero-section">
  <div class="hero-background">
    <!-- 動態背景或視頻 -->
    <div class="hero-gradient"></div>
  </div>
  
  <div class="hero-content">
    <h1 class="hero-title">發現生活的美好</h1>
    <p class="hero-subtitle">精選優質商品，為您帶來卓越購物體驗</p>
    
    <div class="hero-actions">
      <button class="btn-primary-large">立即探索</button>
      <button class="btn-secondary-large">了解更多</button>
    </div>
  </div>
  
  <!-- 浮動產品卡片 -->
  <div class="floating-product-cards">
    <div class="product-card-float" *ngFor="let product of featuredProducts.slice(0,3)">
      <!-- 產品卡片內容 -->
    </div>
  </div>
</section>
```

#### **分類展示區**

```html
<!-- 互動式分類展示 -->
<section class="categories-showcase">
  <div class="section-header">
    <h2 class="section-title">探索分類</h2>
    <p class="section-subtitle">找到您喜愛的商品類型</p>
  </div>
  
  <div class="categories-grid">
    <div class="category-card" 
         *ngFor="let category of categories"
         [class.active]="hoveredCategory === category.value"
         (mouseenter)="hoveredCategory = category.value"
         (mouseleave)="hoveredCategory = null">
      
      <div class="category-image">
        <img [src]="category.image" [alt]="category.label">
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
</section>
```

#### **產品展示區**

```html
<!-- 現代化產品網格 -->
<section class="products-showcase">
  <div class="section-header">
    <h2 class="section-title">精選推薦</h2>
    <div class="section-controls">
      <div class="filter-tabs">
        <button class="filter-tab active">全部</button>
        <button class="filter-tab">新品</button>
        <button class="filter-tab">熱銷</button>
        <button class="filter-tab">特價</button>
      </div>
    </div>
  </div>
  
  <div class="products-grid">
    <div class="product-card-modern" 
         *ngFor="let product of featuredProducts"
         [class.loading]="loading">
      
      <!-- 產品圖片區 -->
      <div class="product-image-container">
        <img [src]="product.images?.[0]?.url" 
             [alt]="product.name"
             class="product-image">
        
        <!-- 懸停時的額外圖片 -->
        <img [src]="product.images?.[1]?.url" 
             [alt]="product.name"
             class="product-image-hover">
        
        <!-- 快速操作按鈕 -->
        <div class="product-quick-actions">
          <button class="quick-action-btn" title="快速查看">
            <i class="pi pi-eye"></i>
          </button>
          <button class="quick-action-btn" title="加入願望清單">
            <i class="pi pi-heart"></i>
          </button>
          <button class="quick-action-btn" title="比較">
            <i class="pi pi-refresh"></i>
          </button>
        </div>
        
        <!-- 標籤 -->
        <div class="product-badges">
          <span class="badge badge-new" *ngIf="product.isNew">新品</span>
          <span class="badge badge-sale" *ngIf="product.onSale">特價</span>
        </div>
      </div>
      
      <!-- 產品資訊區 -->
      <div class="product-info">
        <div class="product-category">{{ getCategoryLabel(product.category) }}</div>
        <h3 class="product-title">{{ product.name }}</h3>
        <p class="product-description">{{ product.description }}</p>
        
        <!-- 評分 -->
        <div class="product-rating">
          <div class="stars">
            <i class="pi pi-star-fill" *ngFor="let star of [1,2,3,4,5]"></i>
          </div>
          <span class="rating-count">({{ product.reviewCount }})</span>
        </div>
        
        <!-- 價格 -->
        <div class="product-pricing">
          <span class="price-current">NT$ {{ product.price.amount | number:'1.0-0' }}</span>
          <span class="price-original" *ngIf="product.originalPrice">
            NT$ {{ product.originalPrice | number:'1.0-0' }}
          </span>
        </div>
        
        <!-- 操作按鈕 -->
        <div class="product-actions">
          <button class="btn-add-to-cart" 
                  [disabled]="!product.inStock"
                  (click)="addToCart(product)">
            <i class="pi pi-shopping-cart"></i>
            <span>加入購物車</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</section>
```

### Design

```html
<!-- 現代化導航欄 -->
<header class="modern-header">
  <!-- 頂部通知欄 -->
  <div class="header-announcement">
    <p>🎉 新用戶註冊享 9 折優惠！免費配送全台 🚚</p>
  </div>
  
  <!-- 主導航 -->
  <nav class="main-navigation">
    <div class="nav-container">
      <!-- Logo -->
      <div class="nav-brand">
        <img src="/assets/logo.svg" alt="購物商城" class="brand-logo">
      </div>
      
      <!-- 搜尋欄 -->
      <div class="nav-search">
        <div class="search-container">
          <input type="text" 
                 placeholder="搜尋商品、品牌或分類..."
                 class="search-input"
                 [(ngModel)]="searchQuery"
                 (input)="onSearchInput($event)">
          <button class="search-btn">
            <i class="pi pi-search"></i>
          </button>
        </div>
        
        <!-- 搜尋recommendations下拉 -->
        <div class="search-suggestions" *ngIf="searchSuggestions.length > 0">
          <div class="suggestion-item" 
               *ngFor="let suggestion of searchSuggestions"
               (click)="selectSuggestion(suggestion)">
            <i class="pi pi-search suggestion-icon"></i>
            <span>{{ suggestion.text }}</span>
          </div>
        </div>
      </div>
      
      <!-- 導航選單 -->
      <div class="nav-menu">
        <a href="/categories" class="nav-link">所有分類</a>
        <a href="/deals" class="nav-link">今日特價</a>
        <a href="/new-arrivals" class="nav-link">新品上市</a>
        <a href="/brands" class="nav-link">品牌專區</a>
      </div>
      
      <!-- 用戶操作區 -->
      <div class="nav-actions">
        <!-- 願望清單 -->
        <button class="nav-action-btn">
          <i class="pi pi-heart"></i>
          <span class="action-badge" *ngIf="wishlistCount > 0">{{ wishlistCount }}</span>
        </button>
        
        <!-- 購物車 -->
        <button class="nav-action-btn cart-btn" (click)="toggleCart()">
          <i class="pi pi-shopping-cart"></i>
          <span class="action-badge" *ngIf="cartCount > 0">{{ cartCount }}</span>
        </button>
        
        <!-- 用戶選單 -->
        <div class="user-menu">
          <button class="user-avatar" *ngIf="!isLoggedIn">
            <i class="pi pi-user"></i>
          </button>
          <img [src]="user.avatar" 
               [alt]="user.name" 
               class="user-avatar"
               *ngIf="isLoggedIn">
        </div>
      </div>
    </div>
  </nav>
  
  <!-- 分類導航 (可選) -->
  <div class="category-navigation" *ngIf="showCategoryNav">
    <div class="category-nav-container">
      <a *ngFor="let category of mainCategories" 
         [routerLink]="['/category', category.slug]"
         class="category-nav-link">
        {{ category.name }}
      </a>
    </div>
  </div>
</header>

<!-- 側邊購物車 -->
<div class="cart-sidebar" [class.open]="isCartOpen">
  <div class="cart-overlay" (click)="closeCart()"></div>
  <div class="cart-panel">
    <!-- 購物車內容 -->
  </div>
</div>
```

## 🎨 **CSS 樣式系統**

### Design

```scss
// design-tokens.scss
:root {
  // 色彩系統
  --color-primary-50: #f0fdfa;
  --color-primary-500: #14b8a6;
  --color-primary-600: #0d9488;
  --color-secondary-500: #f59e0b;
  --color-accent-500: #ec4899;
  
  // 字體系統
  --font-family-primary: 'Inter', 'Noto Sans TC', sans-serif;
  --font-family-display: 'Poppins', 'Inter', sans-serif;
  
  --font-size-xs: 0.75rem;    // 12px
  --font-size-sm: 0.875rem;   // 14px
  --font-size-base: 1rem;     // 16px
  --font-size-lg: 1.125rem;   // 18px
  --font-size-xl: 1.25rem;    // 20px
  --font-size-2xl: 1.5rem;    // 24px
  --font-size-3xl: 1.875rem;  // 30px
  --font-size-4xl: 2.25rem;   // 36px
  
  // 間距系統
  --spacing-1: 0.25rem;   // 4px
  --spacing-2: 0.5rem;    // 8px
  --spacing-3: 0.75rem;   // 12px
  --spacing-4: 1rem;      // 16px
  --spacing-6: 1.5rem;    // 24px
  --spacing-8: 2rem;      // 32px
  --spacing-12: 3rem;     // 48px
  --spacing-16: 4rem;     // 64px
  
  // 圓角系統
  --radius-sm: 0.375rem;  // 6px
  --radius-md: 0.5rem;    // 8px
  --radius-lg: 0.75rem;   // 12px
  --radius-xl: 1rem;      // 16px
  --radius-2xl: 1.5rem;   // 24px
  
  // 陰影系統
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
  
  // 動畫系統
  --transition-fast: 150ms ease-in-out;
  --transition-normal: 250ms ease-in-out;
  --transition-slow: 350ms ease-in-out;
  
  --easing-ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --easing-ease-out: cubic-bezier(0, 0, 0.2, 1);
  --easing-ease-in: cubic-bezier(0.4, 0, 1, 1);
}
```

### **2. 組件樣式系統**

```scss
// components/buttons.scss
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-6);
  border: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-family-primary);
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transition: left var(--transition-slow);
  }
  
  &:hover::before {
    left: 100%;
  }
  
  // 主要按鈕
  &-primary {
    background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
    color: white;
    box-shadow: var(--shadow-md);
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-lg);
    }
    
    &:active {
      transform: translateY(0);
    }
    
    &-large {
      @extend .btn-primary;
      padding: var(--spacing-4) var(--spacing-8);
      font-size: var(--font-size-lg);
      border-radius: var(--radius-xl);
    }
  }
  
  // 次要按鈕
  &-secondary {
    background: transparent;
    color: var(--color-primary-600);
    border: 2px solid var(--color-primary-500);
    
    &:hover {
      background: var(--color-primary-500);
      color: white;
      transform: translateY(-1px);
    }
    
    &-large {
      @extend .btn-secondary;
      padding: var(--spacing-4) var(--spacing-8);
      font-size: var(--font-size-lg);
      border-radius: var(--radius-xl);
    }
  }
}

// components/cards.scss
.product-card-modern {
  background: white;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  position: relative;
  
  &:hover {
    transform: translateY(-8px);
    box-shadow: var(--shadow-xl);
    
    .product-image {
      transform: scale(1.05);
    }
    
    .product-image-hover {
      opacity: 1;
    }
    
    .product-quick-actions {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .product-image-container {
    position: relative;
    aspect-ratio: 1;
    overflow: hidden;
    background: var(--color-gray-50);
    
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform var(--transition-slow);
    }
    
    .product-image-hover {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      opacity: 0;
      transition: opacity var(--transition-normal);
    }
    
    .product-quick-actions {
      position: absolute;
      top: var(--spacing-4);
      right: var(--spacing-4);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-2);
      opacity: 0;
      transform: translateY(-10px);
      transition: all var(--transition-normal);
      
      .quick-action-btn {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(10px);
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all var(--transition-fast);
        
        &:hover {
          background: var(--color-primary-500);
          color: white;
          transform: scale(1.1);
        }
      }
    }
    
    .product-badges {
      position: absolute;
      top: var(--spacing-4);
      left: var(--spacing-4);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-2);
      
      .badge {
        padding: var(--spacing-1) var(--spacing-3);
        border-radius: var(--radius-md);
        font-size: var(--font-size-xs);
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        
        &-new {
          background: var(--color-accent-500);
          color: white;
        }
        
        &-sale {
          background: var(--color-secondary-500);
          color: white;
        }
      }
    }
  }
  
  .product-info {
    padding: var(--spacing-6);
    
    .product-category {
      font-size: var(--font-size-sm);
      color: var(--color-primary-500);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: var(--spacing-2);
    }
    
    .product-title {
      font-size: var(--font-size-lg);
      font-weight: 700;
      color: var(--color-gray-900);
      margin-bottom: var(--spacing-2);
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .product-description {
      font-size: var(--font-size-sm);
      color: var(--color-gray-500);
      margin-bottom: var(--spacing-4);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .product-rating {
      display: flex;
      align-items: center;
      gap: var(--spacing-2);
      margin-bottom: var(--spacing-4);
      
      .stars {
        display: flex;
        gap: 2px;
        
        .pi-star-fill {
          color: var(--color-secondary-500);
          font-size: var(--font-size-sm);
        }
      }
      
      .rating-count {
        font-size: var(--font-size-sm);
        color: var(--color-gray-500);
      }
    }
    
    .product-pricing {
      display: flex;
      align-items: center;
      gap: var(--spacing-3);
      margin-bottom: var(--spacing-6);
      
      .price-current {
        font-size: var(--font-size-xl);
        font-weight: 700;
        color: var(--color-primary-600);
      }
      
      .price-original {
        font-size: var(--font-size-base);
        color: var(--color-gray-500);
        text-decoration: line-through;
      }
    }
    
    .product-actions {
      .btn-add-to-cart {
        width: 100%;
        background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
        color: white;
        border: none;
        padding: var(--spacing-3) var(--spacing-4);
        border-radius: var(--radius-lg);
        font-weight: 600;
        cursor: pointer;
        transition: all var(--transition-normal);
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--spacing-2);
        
        &:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: var(--shadow-lg);
        }
        
        &:disabled {
          background: var(--color-gray-300);
          cursor: not-allowed;
        }
      }
    }
  }
}
```

### **3. 動畫系統**

```scss
// animations.scss
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

// 動畫類別
.animate-fade-in-up {
  animation: fadeInUp 0.6s var(--easing-ease-out) forwards;
}

.animate-slide-in-right {
  animation: slideInRight 0.6s var(--easing-ease-out) forwards;
}

.animate-scale-in {
  animation: scaleIn 0.4s var(--easing-ease-out) forwards;
}

// 骨架屏動畫
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200px 100%;
  animation: shimmer 1.5s infinite;
}

// 懸停效果
.hover-lift {
  transition: transform var(--transition-normal);
  
  &:hover {
    transform: translateY(-4px);
  }
}

.hover-scale {
  transition: transform var(--transition-normal);
  
  &:hover {
    transform: scale(1.05);
  }
}
```

## Design

```scss
// responsive.scss
// 斷點系統
$breakpoints: (
  'xs': 0,
  'sm': 576px,
  'md': 768px,
  'lg': 992px,
  'xl': 1200px,
  'xxl': 1400px
);

@mixin respond-to($breakpoint) {
  @if map-has-key($breakpoints, $breakpoint) {
    @media (min-width: map-get($breakpoints, $breakpoint)) {
      @content;
    }
  }
}

// 響應式網格
.products-grid {
  display: grid;
  gap: var(--spacing-6);
  grid-template-columns: 1fr;
  
  @include respond-to('sm') {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @include respond-to('lg') {
    grid-template-columns: repeat(3, 1fr);
  }
  
  @include respond-to('xl') {
    grid-template-columns: repeat(4, 1fr);
  }
}

.categories-grid {
  display: grid;
  gap: var(--spacing-4);
  grid-template-columns: repeat(2, 1fr);
  
  @include respond-to('md') {
    grid-template-columns: repeat(4, 1fr);
  }
  
  @include respond-to('lg') {
    grid-template-columns: repeat(6, 1fr);
  }
}
```

## 🚀 **實作優先順序recommendations**

### **Phase 1: 基礎視覺升級**

1. 實作新的色彩系統和設計 token
2. 更新字體系統和基礎樣式
3. 重新設計按鈕和基礎組件

### **Phase 2: 佈局現代化**

1. 重新設計首頁英雄區塊
2. 更新產品卡片設計
3. 實作新的導航系統

### **Phase 3: 互動體驗增強**

1. 添加微動畫和過渡效果
2. 實作懸停效果和互動回饋
3. 添加載入動畫和骨架屏

### **Phase 4: 高級功能**

1. 實作側邊購物車
2. 添加搜尋recommendations功能
3. 實作產品快速查看功能

這個設計升級將讓你的電商網站具備 2024 年最現代的視覺效果和用戶體驗，同時保持良好的Availability和效能。
