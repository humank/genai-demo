import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <footer class="bg-gray-900 text-white">
      <div class="container-custom py-12">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <!-- Company Info -->
          <div class="space-y-4">
            <div class="flex items-center space-x-2">
              <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <i class="pi pi-shopping-cart text-white text-sm"></i>
              </div>
              <span class="text-xl font-bold">購物商城</span>
            </div>
            <p class="text-gray-400 text-sm">
              提供優質商品與服務，讓您享受便利的購物體驗。
            </p>
            <div class="flex space-x-4">
              <a href="#" class="text-gray-400 hover:text-white transition-colors">
                <i class="pi pi-facebook text-lg"></i>
              </a>
              <a href="#" class="text-gray-400 hover:text-white transition-colors">
                <i class="pi pi-twitter text-lg"></i>
              </a>
              <a href="#" class="text-gray-400 hover:text-white transition-colors">
                <i class="pi pi-instagram text-lg"></i>
              </a>
            </div>
          </div>

          <!-- Quick Links -->
          <div class="space-y-4">
            <h3 class="text-lg font-semibold">快速連結</h3>
            <ul class="space-y-2 text-sm">
              <li><a routerLink="/products" class="text-gray-400 hover:text-white transition-colors">所有商品</a></li>
              <li><a routerLink="/products?category=ELECTRONICS" class="text-gray-400 hover:text-white transition-colors">電子產品</a></li>
              <li><a routerLink="/products?category=FASHION" class="text-gray-400 hover:text-white transition-colors">時尚服飾</a></li>
              <li><a routerLink="/products?category=HOME_LIVING" class="text-gray-400 hover:text-white transition-colors">居家生活</a></li>
            </ul>
          </div>

          <!-- Customer Service -->
          <div class="space-y-4">
            <h3 class="text-lg font-semibold">客戶服務</h3>
            <ul class="space-y-2 text-sm">
              <li><a href="#" class="text-gray-400 hover:text-white transition-colors">聯絡我們</a></li>
              <li><a href="#" class="text-gray-400 hover:text-white transition-colors">常見問題</a></li>
              <li><a href="#" class="text-gray-400 hover:text-white transition-colors">退換貨政策</a></li>
              <li><a href="#" class="text-gray-400 hover:text-white transition-colors">配送資訊</a></li>
            </ul>
          </div>

          <!-- Contact Info -->
          <div class="space-y-4">
            <h3 class="text-lg font-semibold">聯絡資訊</h3>
            <div class="space-y-2 text-sm text-gray-400">
              <div class="flex items-center space-x-2">
                <i class="pi pi-phone"></i>
                <span>0800-123-456</span>
              </div>
              <div class="flex items-center space-x-2">
                <i class="pi pi-envelope"></i>
                <span>service&#64;shopping.com</span>
              </div>
              <div class="flex items-center space-x-2">
                <i class="pi pi-map-marker"></i>
                <span>台北市信義區信義路五段7號</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom Bar -->
        <div class="border-t border-gray-800 mt-8 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p class="text-gray-400 text-sm">
            © 2024 購物商城. All rights reserved.
          </p>
          <div class="flex space-x-6 mt-4 md:mt-0">
            <a href="#" class="text-gray-400 hover:text-white text-sm transition-colors">隱私政策</a>
            <a href="#" class="text-gray-400 hover:text-white text-sm transition-colors">服務條款</a>
            <a href="#" class="text-gray-400 hover:text-white text-sm transition-colors">Cookie 政策</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: []
})
export class FooterComponent {}