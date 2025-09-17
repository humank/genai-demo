import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Toast, ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container" [class]="'toast-container-' + getPosition(toasts$ | async)">
      <div *ngFor="let toast of toasts$ | async; trackBy: trackByToastId"
           class="toast"
           [class]="getToastClasses(toast)"
           [attr.data-toast-id]="toast.id">
        
        <div class="toast-icon" [class]="'toast-icon-' + toast.type">
          <i [class]="toast.icon || getToastIcon(toast.type)"></i>
        </div>
        
        <div class="toast-content">
          <div class="toast-title">{{ toast.title }}</div>
          <div class="toast-message" *ngIf="toast.message">{{ toast.message }}</div>
          
          <!-- Action Button -->
          <button *ngIf="toast.action" 
                  class="toast-action"
                  (click)="handleAction(toast)">
            {{ toast.action.label }}
          </button>
        </div>
        
        <button class="toast-close" 
                *ngIf="toast.dismissible"
                (click)="dismiss(toast.id)"
                title="關閉">
          <i class="pi pi-times"></i>
        </button>
        
        <!-- Progress Bar for timed toasts -->
        <div *ngIf="toast.duration && toast.duration > 0" 
             class="toast-progress"
             [style.animation-duration]="toast.duration + 'ms'">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      z-index: var(--z-index-toast);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-3);
      max-width: 400px;
      pointer-events: none;
    }

    /* Position variants */
    .toast-container-top-right {
      top: var(--spacing-6);
      right: var(--spacing-6);
    }

    .toast-container-top-left {
      top: var(--spacing-6);
      left: var(--spacing-6);
    }

    .toast-container-bottom-right {
      bottom: var(--spacing-6);
      right: var(--spacing-6);
      flex-direction: column-reverse;
    }

    .toast-container-bottom-left {
      bottom: var(--spacing-6);
      left: var(--spacing-6);
      flex-direction: column-reverse;
    }

    .toast-container-top-center {
      top: var(--spacing-6);
      left: 50%;
      transform: translateX(-50%);
    }

    .toast-container-bottom-center {
      bottom: var(--spacing-6);
      left: 50%;
      transform: translateX(-50%);
      flex-direction: column-reverse;
    }

    .toast {
      pointer-events: auto;
      background: white;
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-xl);
      border: 1px solid var(--color-gray-200);
      padding: var(--spacing-4);
      display: flex;
      align-items: flex-start;
      gap: var(--spacing-3);
      position: relative;
      overflow: hidden;
      min-width: 300px;
      backdrop-filter: blur(10px);
    }

    /* Toast type styles */
    .toast-success {
      border-left: 4px solid var(--color-success-500);
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.05) 0%, white 100%);
    }

    .toast-error {
      border-left: 4px solid var(--color-error-500);
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.05) 0%, white 100%);
    }

    .toast-warning {
      border-left: 4px solid var(--color-warning-500);
      background: linear-gradient(135deg, rgba(245, 158, 11, 0.05) 0%, white 100%);
    }

    .toast-info {
      border-left: 4px solid var(--color-info-500);
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, white 100%);
    }

    /* Animation classes */
    .toast-animation-slide {
      animation: slideInRight var(--duration-300) var(--easing-ease-out);
    }

    .toast-animation-fade {
      animation: fadeIn var(--duration-300) var(--easing-ease-out);
    }

    .toast-animation-bounce {
      animation: bounceIn var(--duration-500) var(--easing-bounce);
    }

    .toast-animation-zoom {
      animation: zoomIn var(--duration-300) var(--easing-ease-out);
    }

    .toast-animation-shake {
      animation: slideInRight var(--duration-300) var(--easing-ease-out),
                 shake var(--duration-500) var(--easing-ease-out) 0.3s;
    }

    .toast-exit {
      animation: slideOutRight var(--duration-200) var(--easing-ease-in) forwards;
    }

    .toast-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .toast-icon-success {
      color: var(--color-success-500);
      background: var(--color-success-50);
    }

    .toast-icon-error {
      color: var(--color-error-500);
      background: var(--color-error-50);
    }

    .toast-icon-warning {
      color: var(--color-warning-500);
      background: var(--color-warning-50);
    }

    .toast-icon-info {
      color: var(--color-info-500);
      background: var(--color-info-50);
    }

    .toast-icon i {
      font-size: var(--font-size-lg);
    }

    .toast-content {
      flex: 1;
      min-width: 0;
    }

    .toast-title {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-semibold);
      color: var(--color-gray-900);
      margin-bottom: var(--spacing-1);
      line-height: var(--line-height-tight);
    }

    .toast-message {
      font-size: var(--font-size-sm);
      color: var(--color-gray-600);
      line-height: var(--line-height-relaxed);
      margin-bottom: var(--spacing-2);
    }

    .toast-action {
      background: var(--color-primary-500);
      color: white;
      border: none;
      border-radius: var(--radius-md);
      padding: var(--spacing-1) var(--spacing-3);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .toast-action:hover {
      background: var(--color-primary-600);
      transform: translateY(-1px);
    }

    .toast-close {
      width: 24px;
      height: 24px;
      border: none;
      background: none;
      color: var(--color-gray-400);
      cursor: pointer;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition-fast);
      flex-shrink: 0;
    }

    .toast-close:hover {
      color: var(--color-gray-600);
      background: var(--color-gray-100);
    }

    .toast-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      background: var(--color-primary-500);
      animation: progressShrink linear;
      transform-origin: left;
    }

    /* Keyframe animations */
    @keyframes slideInRight {
      0% {
        transform: translateX(100%);
        opacity: 0;
      }
      100% {
        transform: translateX(0);
        opacity: 1;
      }
    }

    @keyframes slideOutRight {
      0% {
        transform: translateX(0);
        opacity: 1;
      }
      100% {
        transform: translateX(100%);
        opacity: 0;
      }
    }

    @keyframes fadeIn {
      0% { opacity: 0; }
      100% { opacity: 1; }
    }

    @keyframes bounceIn {
      0% {
        opacity: 0;
        transform: scale(0.3);
      }
      50% {
        opacity: 1;
        transform: scale(1.05);
      }
      70% {
        transform: scale(0.9);
      }
      100% {
        opacity: 1;
        transform: scale(1);
      }
    }

    @keyframes zoomIn {
      0% {
        opacity: 0;
        transform: scale(0.8);
      }
      100% {
        opacity: 1;
        transform: scale(1);
      }
    }

    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
      20%, 40%, 60%, 80% { transform: translateX(4px); }
    }

    @keyframes progressShrink {
      0% { transform: scaleX(1); }
      100% { transform: scaleX(0); }
    }

    /* Responsive design */
    @media (max-width: 768px) {
      .toast-container {
        max-width: none;
        left: var(--spacing-4) !important;
        right: var(--spacing-4) !important;
        transform: none !important;
      }

      .toast {
        min-width: auto;
        padding: var(--spacing-3);
      }

      .toast-icon {
        width: 32px;
        height: 32px;
      }
    }

    /* Accessibility */
    @media (prefers-reduced-motion: reduce) {
      .toast,
      .toast-exit,
      .toast-progress {
        animation: none;
      }

      .toast {
        transform: none;
        opacity: 1;
      }
    }

    /* Dark mode support */
    @media (prefers-color-scheme: dark) {
      .toast {
        background: var(--color-gray-800);
        border-color: var(--color-gray-700);
        color: var(--color-gray-100);
      }

      .toast-title {
        color: var(--color-gray-100);
      }

      .toast-message {
        color: var(--color-gray-300);
      }

      .toast-close:hover {
        background: var(--color-gray-700);
      }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts$: Observable<Toast[]>;

  constructor(private toastService: ToastService) {
    this.toasts$ = this.toastService.toasts$;
  }

  ngOnInit(): void { }

  ngOnDestroy(): void { }

  trackByToastId(index: number, toast: Toast): string {
    return toast.id;
  }

  getToastIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      'success': 'pi pi-check-circle',
      'error': 'pi pi-times-circle',
      'warning': 'pi pi-exclamation-triangle',
      'info': 'pi pi-info-circle'
    };
    return iconMap[type] || 'pi pi-info-circle';
  }

  getToastClasses(toast: Toast): string {
    const classes = [`toast-${toast.type}`];

    if (toast.animation) {
      classes.push(`toast-animation-${toast.animation}`);
    }

    return classes.join(' ');
  }

  getPosition(toasts: Toast[] | null): string {
    if (!toasts || toasts.length === 0) return 'top-right';
    return toasts[0].position || 'top-right';
  }

  handleAction(toast: Toast): void {
    if (toast.action) {
      toast.action.handler();
      this.dismiss(toast.id);
    }
  }

  dismiss(id: string): void {
    this.toastService.dismiss(id);
  }
}