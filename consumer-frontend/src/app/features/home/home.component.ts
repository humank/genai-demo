import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

// PrimeNG
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { CarouselModule } from 'primeng/carousel';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';

// Models & Services
import { Product, ProductListResponse } from '../../core/models/product.model';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';

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
    SkeletonModule
  ],
  template: `
    <div class="min-h-screen">
      <!-- Hero Section -->
      <section class="bg-gradient-to-r from-primary-600 to-primary-800 text-white py-20">
        <div class="container-custom">
          <div class="text-center space-y-6">
            <h1 class="text-4xl md:text-6xl font-bold animate-fade-in">
              歡迎來到購物商城
            </h1>
            <p class="text-xl md:text-2xl text-primary-100 max-w-2xl mx-auto animate-slide-up">
              發現優質商品，享受便利購物體驗
            </p>
            <div class="flex flex-col sm:flex-row gap-4 justify-center animate-bounce-in">
              <p-button 
                label="立即購物" 
                icon="pi pi-shopping-cart"
                size="large"
                severity="secondary"
                routerLink="/products">
              </p-button>
              <p-button 
                label="了解更多" 
                icon="pi pi-info-circle"
                size="large"
                [outlined]="true"
                severity="secondary">
              </p-button>
            </div>
          </div>
        </div>
      </section>

      <!-- Featured Categories -->
      <section class="py-16 bg-white">
        <div class="container-custom">
          <div class="text-center mb-12">
            <h2 class="text-3xl font-bold text-gray-900 mb-4">熱門分類</h2>
            <p class="text-gray-600 text-lg">探索我們精選的商品分類</p>
          </div>
          
          <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div *ngFor="let category of categories" 
                 class="group cursor-pointer"
                 [routerLink]="['/products']"
                 [queryParams]="{category: category.value}">
              <div class="bg-gray-50 rounded-xl p-8 text-center group-hover:bg-primary-50 transition-colors duration-200">
                <i [class]="category.icon" class="text-4xl text-primary-600 mb-4 group-hover:scale-110 transition-transform duration-200"></i>
                <h3 class="font-semibold text-gray-900 group-hover:text-primary-700">{{ category.label }}</h3>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Featured Products -->
      <section class="py-16 bg-gray-50">
        <div class="container-custom">
          <div class="text-center mb-12">
            <h2 class="text-3xl font-bold text-gray-900 mb-4">精選商品</h2>
            <p class="text-gray-600 text-lg">為您推薦的熱門商品</p>
          </div>

          <!-- Loading State -->
          <div *ngIf="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div *ngFor="let item of [1,2,3,4]" class="bg-white rounded-lg shadow-sm p-4">
              <p-skeleton height="200px" class="mb-4"></p-skeleton>
              <p-skeleton height="20px" class="mb-2"></p-skeleton>
              <p-skeleton height="16px" width="60%"></p-skeleton>
            </div>
          </div>

          <!-- Products Grid -->
          <div *ngIf="!loading && featuredProducts.length > 0" 
               class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div *ngFor="let product of featuredProducts" 
                 class="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow duration-200 overflow-hidden group">
              
              <!-- Product Image -->
              <div class="relative overflow-hidden">
                <img 
                  [src]="product.images?.[0]?.url || '/assets/images/placeholder-product.jpg'"
                  [alt]="product.name"
                  class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-200"
                  (error)="onImageError($event)">
                
                <div *ngIf="!product.inStock" 
                     class="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                  <span class="text-white font-semibold">缺貨中</span>
                </div>
              </div>

              <!-- Product Info -->
              <div class="p-4">
                <h3 class="font-semibold text-gray-900 mb-2 line-clamp-2">{{ product.name }}</h3>
                <p class="text-gray-600 text-sm mb-3 line-clamp-2">{{ product.description }}</p>
                
                <div class="flex items-center justify-between mb-4">
                  <span class="text-2xl font-bold text-primary-600">
                    NT$ {{ product.price.amount | number:'1.0-0' }}
                  </span>
                  <p-tag *ngIf="product.category" [value]="getCategoryLabel(product.category)" severity="info"></p-tag>
                </div>

                <div class="flex space-x-2">
                  <p-button 
                    label="查看詳情"
                    icon="pi pi-eye"
                    [outlined]="true"
                    size="small"
                    class="flex-1"
                    [routerLink]="['/products', product.id]">
                  </p-button>
                  
                  <p-button 
                    icon="pi pi-shopping-cart"
                    size="small"
                    [disabled]="!product.inStock"
                    (onClick)="addToCart(product)"
                    [loading]="addingToCart[product.id]">
                  </p-button>
                </div>
              </div>
            </div>
          </div>

          <!-- View All Products -->
          <div class="text-center mt-12">
            <p-button 
              label="查看所有商品" 
              icon="pi pi-arrow-right"
              size="large"
              [outlined]="true"
              routerLink="/products">
            </p-button>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section class="py-16 bg-white">
        <div class="container-custom">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div class="text-center space-y-4">
              <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto">
                <i class="pi pi-truck text-2xl text-primary-600"></i>
              </div>
              <h3 class="text-xl font-semibold text-gray-900">快速配送</h3>
              <p class="text-gray-600">全台24小時快速到貨服務</p>
            </div>
            
            <div class="text-center space-y-4">
              <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto">
                <i class="pi pi-shield text-2xl text-primary-600"></i>
              </div>
              <h3 class="text-xl font-semibold text-gray-900">品質保證</h3>
              <p class="text-gray-600">嚴選優質商品，品質有保障</p>
            </div>
            
            <div class="text-center space-y-4">
              <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto">
                <i class="pi pi-headphones text-2xl text-primary-600"></i>
              </div>
              <h3 class="text-xl font-semibold text-gray-900">客戶服務</h3>
              <p class="text-gray-600">專業客服團隊，隨時為您服務</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .line-clamp-2 {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  `]
})
export class HomeComponent implements OnInit {
  featuredProducts: Product[] = [];
  loading = true;
  addingToCart: { [key: string]: boolean } = {};

  categories = [
    { label: '電子產品', value: 'ELECTRONICS', icon: 'pi pi-mobile' },
    { label: '時尚服飾', value: 'FASHION', icon: 'pi pi-shopping-bag' },
    { label: '居家生活', value: 'HOME_LIVING', icon: 'pi pi-home' },
    { label: '運動健身', value: 'SPORTS_FITNESS', icon: 'pi pi-heart' }
  ];

  constructor(
    private productService: ProductService,
    private cartService: CartService
  ) {}

  ngOnInit() {
    this.loadFeaturedProducts();
  }

  loadFeaturedProducts() {
    this.loading = true;
    this.productService.getFeaturedProducts(8).subscribe({
      next: (response: ProductListResponse) => {
        this.featuredProducts = response.content;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading featured products:', error);
        this.loading = false;
      }
    });
  }

  addToCart(product: Product) {
    if (!product.inStock) return;

    this.addingToCart[product.id] = true;
    
    this.cartService.addToCart({
      productId: product.id,
      quantity: 1
    }).subscribe({
      next: () => {
        this.addingToCart[product.id] = false;
        // TODO: Show success message
      },
      error: (error) => {
        console.error('Error adding to cart:', error);
        this.addingToCart[product.id] = false;
        // TODO: Show error message
      }
    });
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
    event.target.src = '/assets/images/placeholder-product.jpg';
  }
}