import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './layout/header/header.component';
import { FooterComponent } from './layout/footer/footer.component';

// PrimeNG
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    FooterComponent,
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
    </div>
  `,
  styles: []
})
export class AppComponent {
  title = '電商購物平台';
}