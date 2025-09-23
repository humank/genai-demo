import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'app-page-loader',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="page-loader" [class.fade-out]="fadeOut">
      <div class="page-loader-content">
        <div class="loader-logo">
          <i class="pi pi-shopping-cart"></i>
        </div>
        <div class="loader-text">{{ loadingText }}</div>
        <div class="loader-progress">
          <div class="loader-progress-bar" [style.width.%]="progress"></div>
        </div>
        <div class="loader-dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .page-loader {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: var(--z-index-modal);
      animation: fadeIn var(--duration-300) ease-out;
    }

    .page-loader.fade-out {
      animation: fadeOut var(--duration-500) ease-in forwards;
    }

    .page-loader-content {
      text-align: center;
      animation: scaleIn var(--duration-700) var(--easing-bounce);
    }

    .loader-logo {
      width: 80px;
      height: 80px;
      margin: 0 auto var(--spacing-6);
      background: var(--gradient-primary);
      border-radius: var(--radius-2xl);
      display: flex;
      align-items: center;
      justify-content: center;
      animation: pulse 2s ease-in-out infinite;
      box-shadow: var(--shadow-xl);
    }

    .loader-logo i {
      font-size: var(--font-size-2xl);
      color: white;
    }

    .loader-text {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      color: var(--color-gray-700);
      margin-bottom: var(--spacing-4);
      animation: fadeInUp var(--duration-500) ease-out 0.2s both;
    }

    .loader-progress {
      width: 200px;
      height: 4px;
      background: var(--color-gray-200);
      border-radius: var(--radius-full);
      overflow: hidden;
      margin: 0 auto var(--spacing-6);
      animation: fadeInUp var(--duration-500) ease-out 0.4s both;
    }

    .loader-progress-bar {
      height: 100%;
      background: var(--gradient-primary);
      border-radius: var(--radius-full);
      transition: width var(--duration-300) ease-out;
      position: relative;
    }

    .loader-progress-bar::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(90deg,
        transparent 0%,
        rgba(255, 255, 255, 0.3) 50%,
        transparent 100%);
      animation: shimmer 2s infinite;
    }

    .loader-dots {
      display: flex;
      justify-content: center;
      gap: var(--spacing-2);
      animation: fadeInUp var(--duration-500) ease-out 0.6s both;
    }

    .loader-dots .dot {
      width: 8px;
      height: 8px;
      background: var(--color-primary-500);
      border-radius: var(--radius-full);
      animation: dotsBounce 1.4s ease-in-out infinite both;
    }

    .loader-dots .dot:nth-child(1) {
      animation-delay: -0.32s;
    }

    .loader-dots .dot:nth-child(2) {
      animation-delay: -0.16s;
    }

    .loader-dots .dot:nth-child(3) {
      animation-delay: 0s;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes fadeOut {
      from { opacity: 1; }
      to { opacity: 0; }
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

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes pulse {
      0%, 100% {
        transform: scale(1);
      }
      50% {
        transform: scale(1.05);
      }
    }

    @keyframes shimmer {
      0% {
        transform: translateX(-100%);
      }
      100% {
        transform: translateX(100%);
      }
    }

    @keyframes dotsBounce {
      0%, 80%, 100% {
        transform: scale(0);
      }
      40% {
        transform: scale(1);
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .page-loader,
      .page-loader-content,
      .loader-logo,
      .loader-text,
      .loader-progress,
      .loader-dots,
      .loader-progress-bar::after,
      .loader-dots .dot {
        animation: none;
      }
    }
  `]
})
export class PageLoaderComponent implements OnInit {
    @Input() loadingText = '載入中...';
    @Input() progress = 0;
    @Input() fadeOut = false;

    ngOnInit() {
        // Simulate loading progress
        this.simulateProgress();
    }

    private simulateProgress() {
        const interval = setInterval(() => {
            this.progress += Math.random() * 15;
            if (this.progress >= 100) {
                this.progress = 100;
                clearInterval(interval);
            }
        }, 200);
    }
}