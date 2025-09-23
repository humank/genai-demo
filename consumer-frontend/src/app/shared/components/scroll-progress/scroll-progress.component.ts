import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { SmoothScrollService } from '../../services/smooth-scroll.service';

@Component({
    selector: 'app-scroll-progress',
    standalone: true,
    imports: [CommonModule],
    template: `
    <!-- Linear Progress Bar -->
    <div *ngIf="type === 'bar'" class="scroll-progress-bar-container" [class]="position">
      <div class="scroll-progress-bar" [style.width.%]="progress"></div>
    </div>

    <!-- Circular Progress Indicator -->
    <div *ngIf="type === 'circle'" class="scroll-progress-circle-container" [class]="position">
      <button class="scroll-to-top-btn" 
              [class.visible]="progress > 10"
              (click)="scrollToTop()"
              title="回到頂部">
        <svg class="progress-ring" width="48" height="48">
          <circle class="progress-ring-bg" 
                  cx="24" cy="24" r="20" 
                  fill="transparent" 
                  stroke="currentColor" 
                  stroke-width="2" 
                  opacity="0.2"/>
          <circle class="progress-ring-fill" 
                  cx="24" cy="24" r="20" 
                  fill="transparent" 
                  stroke="currentColor" 
                  stroke-width="2"
                  [style.stroke-dasharray]="circumference"
                  [style.stroke-dashoffset]="offset"/>
        </svg>
        <i class="pi pi-chevron-up progress-icon"></i>
      </button>
    </div>

    <!-- Reading Progress (for articles) -->
    <div *ngIf="type === 'reading'" class="reading-progress-container">
      <div class="reading-progress-content">
        <div class="reading-time">
          <i class="pi pi-clock"></i>
          <span>{{ estimatedReadingTime }} 分鐘閱讀</span>
        </div>
        <div class="reading-progress">
          <div class="reading-progress-bar" [style.width.%]="progress"></div>
        </div>
        <div class="reading-percentage">{{ Math.round(progress) }}%</div>
      </div>
    </div>
  `,
    styles: [`
    /* Linear Progress Bar */
    .scroll-progress-bar-container {
      position: fixed;
      z-index: var(--z-index-header);
      height: 3px;
      background: rgba(0, 0, 0, 0.1);
      backdrop-filter: blur(10px);
    }

    .scroll-progress-bar-container.top {
      top: 0;
      left: 0;
      right: 0;
    }

    .scroll-progress-bar-container.bottom {
      bottom: 0;
      left: 0;
      right: 0;
    }

    .scroll-progress-bar {
      height: 100%;
      background: linear-gradient(90deg, var(--color-primary-500), var(--color-primary-600));
      transition: width 0.1s ease-out;
      position: relative;
    }

    .scroll-progress-bar::after {
      content: '';
      position: absolute;
      top: 0;
      right: 0;
      width: 20px;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3));
      animation: shimmer 2s infinite;
    }

    /* Circular Progress Indicator */
    .scroll-progress-circle-container {
      position: fixed;
      z-index: var(--z-index-fab);
    }

    .scroll-progress-circle-container.bottom-right {
      bottom: var(--spacing-6);
      right: var(--spacing-6);
    }

    .scroll-progress-circle-container.bottom-left {
      bottom: var(--spacing-6);
      left: var(--spacing-6);
    }

    .scroll-to-top-btn {
      width: 48px;
      height: 48px;
      border: none;
      border-radius: var(--radius-full);
      background: white;
      box-shadow: var(--shadow-lg);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      transition: all var(--transition-normal);
      opacity: 0;
      transform: translateY(20px);
      pointer-events: none;
    }

    .scroll-to-top-btn.visible {
      opacity: 1;
      transform: translateY(0);
      pointer-events: auto;
    }

    .scroll-to-top-btn:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-xl);
    }

    .progress-ring {
      position: absolute;
      top: 0;
      left: 0;
      transform: rotate(-90deg);
      color: var(--color-primary-500);
    }

    .progress-ring-fill {
      transition: stroke-dashoffset 0.1s ease-out;
    }

    .progress-icon {
      font-size: var(--font-size-base);
      color: var(--color-primary-600);
      z-index: 1;
    }

    /* Reading Progress */
    .reading-progress-container {
      position: fixed;
      top: var(--spacing-20);
      right: var(--spacing-6);
      z-index: var(--z-index-overlay);
      background: white;
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-lg);
      padding: var(--spacing-4);
      min-width: 200px;
      backdrop-filter: blur(10px);
    }

    .reading-progress-content {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-2);
    }

    .reading-time {
      display: flex;
      align-items: center;
      gap: var(--spacing-2);
      font-size: var(--font-size-sm);
      color: var(--color-gray-600);
    }

    .reading-progress {
      height: 4px;
      background: var(--color-gray-200);
      border-radius: var(--radius-full);
      overflow: hidden;
    }

    .reading-progress-bar {
      height: 100%;
      background: var(--gradient-primary);
      border-radius: var(--radius-full);
      transition: width 0.1s ease-out;
    }

    .reading-percentage {
      text-align: center;
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-primary-600);
    }

    /* Animations */
    @keyframes shimmer {
      0% {
        transform: translateX(-100%);
      }
      100% {
        transform: translateX(100%);
      }
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .scroll-progress-circle-container.bottom-right {
        bottom: var(--spacing-4);
        right: var(--spacing-4);
      }

      .reading-progress-container {
        top: var(--spacing-16);
        right: var(--spacing-4);
        left: var(--spacing-4);
        min-width: auto;
      }
    }

    /* Accessibility */
    @media (prefers-reduced-motion: reduce) {
      .scroll-progress-bar,
      .progress-ring-fill,
      .reading-progress-bar,
      .scroll-to-top-btn {
        transition: none;
      }

      .scroll-progress-bar::after {
        animation: none;
      }
    }

    /* Dark Mode Support */
    @media (prefers-color-scheme: dark) {
      .scroll-to-top-btn {
        background: var(--color-gray-800);
        color: var(--color-gray-100);
      }

      .reading-progress-container {
        background: var(--color-gray-800);
        color: var(--color-gray-100);
      }

      .reading-time {
        color: var(--color-gray-300);
      }
    }
  `]
})
export class ScrollProgressComponent implements OnInit, OnDestroy {
    @Input() type: 'bar' | 'circle' | 'reading' = 'bar';
    @Input() position: 'top' | 'bottom' | 'bottom-right' | 'bottom-left' = 'top';
    @Input() estimatedReadingTime = 5;

    progress = 0;
    circumference = 2 * Math.PI * 20; // radius = 20
    offset = this.circumference;
    Math = Math;

    private scrollListener?: () => void;

    constructor(private smoothScrollService: SmoothScrollService) { }

    ngOnInit() {
        this.setupScrollListener();
    }

    ngOnDestroy() {
        if (this.scrollListener) {
            this.smoothScrollService.removeScrollListener(this.scrollListener);
        }
    }

    private setupScrollListener() {
        this.scrollListener = () => {
            this.updateProgress();
        };

        this.smoothScrollService.addScrollListener(this.scrollListener);

        // Listen for custom scroll progress events
        window.addEventListener('scrollProgress', (event: any) => {
            this.progress = event.detail.progress;
            this.updateCircularProgress();
        });
    }

    private updateProgress() {
        const scrollTop = window.pageYOffset;
        const docHeight = document.documentElement.scrollHeight - window.innerHeight;
        this.progress = Math.max(0, Math.min(100, (scrollTop / docHeight) * 100));

        this.updateCircularProgress();
    }

    private updateCircularProgress() {
        this.offset = this.circumference - (this.progress / 100) * this.circumference;
    }

    scrollToTop() {
        this.smoothScrollService.scrollToTop(800);
    }
}