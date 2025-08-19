import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';

// Services
import { CartService } from '../../core/services/cart.service';
import { Cart } from '../../core/models/cart.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    InputTextModule,
    BadgeModule,
    MenuModule
  ],
  template: `
    <header class="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-50">
      <div class="container-custom">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <div class="flex items-center space-x-4">
            <a routerLink="/home" class="flex items-center space-x-2">
              <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <i class="pi pi-shopping-cart text-white text-sm"></i>
              </div>
              <span class="text-xl font-bold text-gray-900">購物商城</span>
            </a>
          </div>

          <!-- Search Bar -->
          <div class="hidden md:flex flex-1 max-w-lg mx-8">
            <div class="relative w-full">
              <input
                type="text"
                placeholder="搜尋商品..."
                class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                (keyup.enter)="onSearch($event)"
              >
              <i class="pi pi-search absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"></i>
            </div>
          </div>

          <!-- Navigation -->
          <nav class="hidden md:flex items-center space-x-6">
            <a routerLink="/products" 
               routerLinkActive="text-primary-600"
               class="text-gray-700 hover:text-primary-600 font-medium transition-colors">
              商品
            </a>
            <a routerLink="/orders" 
               routerLinkActive="text-primary-600"
               class="text-gray-700 hover:text-primary-600 font-medium transition-colors">
              訂單
            </a>
          </nav>

          <!-- User Actions -->
          <div class="flex items-center space-x-4">
            <!-- Cart -->
            <a routerLink="/cart" class="relative">
              <p-button 
                icon="pi pi-shopping-cart" 
                [text]="true" 
                [rounded]="true"
                severity="secondary"
                class="relative">
              </p-button>
              <span 
                *ngIf="(cart$ | async)?.itemCount && (cart$ | async)!.itemCount > 0"
                class="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                {{ (cart$ | async)?.itemCount }}
              </span>
            </a>

            <!-- User Menu -->
            <p-button 
              icon="pi pi-user" 
              [text]="true" 
              [rounded]="true"
              severity="secondary"
              (onClick)="toggleUserMenu($event)">
            </p-button>

            <!-- Mobile Menu -->
            <p-button 
              icon="pi pi-bars" 
              [text]="true" 
              [rounded]="true"
              severity="secondary"
              class="md:hidden"
              (onClick)="toggleMobileMenu()">
            </p-button>
          </div>
        </div>

        <!-- Mobile Search -->
        <div class="md:hidden pb-4">
          <div class="relative">
            <input
              type="text"
              placeholder="搜尋商品..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              (keyup.enter)="onSearch($event)"
            >
            <i class="pi pi-search absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"></i>
          </div>
        </div>

        <!-- Mobile Navigation -->
        <div *ngIf="showMobileMenu" class="md:hidden border-t border-gray-200 py-4 animate-fade-in">
          <nav class="flex flex-col space-y-2">
            <a routerLink="/products" 
               routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 text-gray-700 hover:text-primary-600 hover:bg-gray-50 rounded-lg transition-colors"
               (click)="closeMobileMenu()">
              商品
            </a>
            <a routerLink="/orders" 
               routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 text-gray-700 hover:text-primary-600 hover:bg-gray-50 rounded-lg transition-colors"
               (click)="closeMobileMenu()">
              訂單
            </a>
            <a routerLink="/auth/login" 
               class="px-4 py-2 text-gray-700 hover:text-primary-600 hover:bg-gray-50 rounded-lg transition-colors"
               (click)="closeMobileMenu()">
              登入
            </a>
          </nav>
        </div>
      </div>
    </header>
  `,
  styles: []
})
export class HeaderComponent implements OnInit {
  cart$: Observable<Cart>;
  showMobileMenu = false;
  userMenuItems: MenuItem[] = [];

  constructor(private cartService: CartService) {
    this.cart$ = this.cartService.cart$;
  }

  ngOnInit() {
    this.userMenuItems = [
      {
        label: '登入',
        icon: 'pi pi-sign-in',
        routerLink: '/auth/login'
      },
      {
        label: '註冊',
        icon: 'pi pi-user-plus',
        routerLink: '/auth/register'
      }
    ];
  }

  onSearch(event: any) {
    const searchTerm = event.target.value.trim();
    if (searchTerm) {
      // TODO: Navigate to search results
      console.log('Searching for:', searchTerm);
    }
  }

  toggleMobileMenu() {
    this.showMobileMenu = !this.showMobileMenu;
  }

  closeMobileMenu() {
    this.showMobileMenu = false;
  }

  toggleUserMenu(event: Event) {
    // TODO: Implement user menu
    console.log('User menu clicked');
  }
}