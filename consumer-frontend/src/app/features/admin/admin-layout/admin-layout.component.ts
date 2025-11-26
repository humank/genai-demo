import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
// import { SidebarModule } from 'primeng/sidebar'; // TODO: Fix import path for PrimeNG 20.x

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ButtonModule,
    // SidebarModule, // Temporarily disabled
    MenuModule
  ],
  template: `
    <div class="admin-layout">
      <!-- Admin Header -->
      <header class="admin-header bg-white shadow-sm border-b border-gray-200 px-6 py-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <h1 class="text-xl font-bold text-gray-900">管理控制台</h1>
            <nav class="hidden md:flex items-center gap-6">
              <a 
                routerLink="/admin/dashboard" 
                routerLinkActive="active"
                class="nav-link px-3 py-2 rounded-md text-sm font-medium transition-colors">
                即時分析儀表板
              </a>
              <a 
                routerLink="/admin/system-health" 
                routerLinkActive="active"
                class="nav-link px-3 py-2 rounded-md text-sm font-medium transition-colors">
                系統健康監控
              </a>
            </nav>
          </div>
          <div class="flex items-center gap-3">
            <p-button 
              icon="pi pi-home" 
              [text]="true"
              routerLink="/home"
              pTooltip="返回首頁">
            </p-button>
          </div>
        </div>
      </header>

      <!-- Mobile Navigation -->
      <div class="md:hidden bg-white border-b border-gray-200 px-6 py-3">
        <nav class="flex gap-4">
          <a 
            routerLink="/admin/dashboard" 
            routerLinkActive="active"
            class="nav-link px-3 py-2 rounded-md text-sm font-medium transition-colors">
            儀表板
          </a>
          <a 
            routerLink="/admin/system-health" 
            routerLinkActive="active"
            class="nav-link px-3 py-2 rounded-md text-sm font-medium transition-colors">
            系統監控
          </a>
        </nav>
      </div>

      <!-- Main Content -->
      <main class="admin-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-layout {
      min-height: 100vh;
      background-color: #f8fafc;
    }

    .admin-header {
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .nav-link {
      color: #6b7280;
      text-decoration: none;
    }

    .nav-link:hover {
      color: #374151;
      background-color: #f3f4f6;
    }

    .nav-link.active {
      color: #667eea;
      background-color: #eef2ff;
    }

    .admin-content {
      min-height: calc(100vh - 80px);
    }

    @media (max-width: 768px) {
      .admin-content {
        min-height: calc(100vh - 120px);
      }
    }
  `]
})
export class AdminLayoutComponent { }