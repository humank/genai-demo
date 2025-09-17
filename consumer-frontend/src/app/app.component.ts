import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from './layout/footer/footer.component';
import { HeaderComponent } from './layout/header/header.component';

// PrimeNG
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    FooterComponent,
    PerformanceMonitorComponent,
    ToastModule
  ],
  template: `
    <div class="min-h-screen flex flex-col bg-gray-50">
      <app-header></app-header>
      
      <main class="flex-1">
        <router-outlet></router-outlet>
      </main>
      
      <app-footer></app-footer>
      
      <!-- Global Toast Messages -->
      <p-toast position="top-right" [life]="5000"></p-toast>
      
      <!-- Performance Monitor (Development Only) -->
      <app-performance-monitor [showMonitor]="true" 
                               [updateInterval]="3000"
                               position="top-right">
      </app-performance-monitor>
    </div>
  `,
  styles: []
})
export class AppComponent {
  title = '電商購物平台';
}