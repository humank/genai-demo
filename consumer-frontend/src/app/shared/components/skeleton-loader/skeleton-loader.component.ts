import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
    selector: 'app-skeleton-loader',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="skeleton-container" [ngClass]="containerClass">
      <!-- Product Card Skeleton -->
      <div *ngIf="type === 'product-card'" class="skeleton-product-card">
        <div class="skeleton skeleton-image"></div>
        <div class="skeleton-content">
          <div class="skeleton skeleton-text skeleton-text-lg"></div>
          <div class="skeleton skeleton-text"></div>
          <div class="skeleton skeleton-text skeleton-text-sm"></div>
          <div class="skeleton-actions">
            <div class="skeleton skeleton-button"></div>
            <div class="skeleton skeleton-icon"></div>
          </div>
        </div>
      </div>

      <!-- Product List Skeleton -->
      <div *ngIf="type === 'product-list'" class="skeleton-product-list">
        <div *ngFor="let item of getArray(count)" class="skeleton-product-item">
          <div class="skeleton skeleton-image-sm"></div>
          <div class="skeleton-item-content">
            <div class="skeleton skeleton-text"></div>
            <div class="skeleton skeleton-text skeleton-text-sm"></div>
            <div class="skeleton skeleton-price"></div>
          </div>
        </div>
      </div>

      <!-- Category Card Skeleton -->
      <div *ngIf="type === 'category-card'" class="skeleton-category-card">
        <div class="skeleton skeleton-image skeleton-image-rect"></div>
        <div class="skeleton-content">
          <div class="skeleton skeleton-icon"></div>
          <div class="skeleton skeleton-text"></div>
          <div class="skeleton skeleton-text skeleton-text-sm"></div>
        </div>
      </div>

      <!-- Text Block Skeleton -->
      <div *ngIf="type === 'text-block'" class="skeleton-text-block">
        <div *ngFor="let line of getArray(lines)" 
             class="skeleton skeleton-text"
             [class.skeleton-text-sm]="line === lines">
        </div>
      </div>

      <!-- Avatar Skeleton -->
      <div *ngIf="type === 'avatar'" class="skeleton-avatar-container">
        <div class="skeleton skeleton-avatar" [ngClass]="'skeleton-avatar-' + size"></div>
        <div *ngIf="showName" class="skeleton-avatar-info">
          <div class="skeleton skeleton-text skeleton-text-sm"></div>
          <div class="skeleton skeleton-text skeleton-text-xs"></div>
        </div>
      </div>

      <!-- Button Skeleton -->
      <div *ngIf="type === 'button'" 
           class="skeleton skeleton-button" 
           [ngClass]="'skeleton-button-' + size">
      </div>

      <!-- Custom Skeleton -->
      <div *ngIf="type === 'custom'" [style.width]="width" [style.height]="height" 
           class="skeleton" [ngClass]="customClass">
      </div>
    </div>
  `,
    styles: [`
    .skeleton-container {
      animation: fadeIn var(--duration-300) ease-out;
    }

    .skeleton {
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200px 100%;
      animation: shimmer 1.5s infinite;
      border-radius: var(--radius-md);
      position: relative;
      overflow: hidden;
    }

    .skeleton::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(90deg,
        transparent 0%,
        rgba(255, 255, 255, 0.6) 50%,
        transparent 100%);
      animation: shimmerWave 2s infinite;
    }

    /* Product Card Skeleton */
    .skeleton-product-card {
      background: white;
      border-radius: var(--radius-2xl);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
      animation: scaleIn var(--duration-500) ease-out;
    }

    .skeleton-image {
      aspect-ratio: 1;
      margin-bottom: 0;
    }

    .skeleton-image-rect {
      aspect-ratio: 16/9;
    }

    .skeleton-image-sm {
      width: 60px;
      height: 60px;
      border-radius: var(--radius-lg);
      flex-shrink: 0;
    }

    .skeleton-content {
      padding: var(--spacing-6);
    }

    .skeleton-text {
      height: 1em;
      margin-bottom: var(--spacing-2);
      border-radius: var(--radius-sm);
    }

    .skeleton-text:last-child {
      margin-bottom: 0;
      width: 60%;
    }

    .skeleton-text-xs {
      height: 0.75em;
    }

    .skeleton-text-sm {
      height: 0.875em;
    }

    .skeleton-text-lg {
      height: 1.25em;
    }

    .skeleton-actions {
      display: flex;
      gap: var(--spacing-3);
      margin-top: var(--spacing-4);
    }

    .skeleton-button {
      height: 40px;
      border-radius: var(--radius-lg);
      flex: 1;
    }

    .skeleton-button-sm {
      height: 32px;
    }

    .skeleton-button-lg {
      height: 48px;
    }

    .skeleton-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-lg);
    }

    .skeleton-price {
      height: 1.5em;
      width: 80px;
      margin-top: var(--spacing-2);
    }

    /* Product List Skeleton */
    .skeleton-product-list {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-4);
    }

    .skeleton-product-item {
      display: flex;
      gap: var(--spacing-4);
      padding: var(--spacing-4);
      background: white;
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-sm);
    }

    .skeleton-item-content {
      flex: 1;
    }

    /* Category Card Skeleton */
    .skeleton-category-card {
      background: white;
      border-radius: var(--radius-2xl);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }

    /* Text Block Skeleton */
    .skeleton-text-block {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-2);
    }

    /* Avatar Skeleton */
    .skeleton-avatar-container {
      display: flex;
      align-items: center;
      gap: var(--spacing-3);
    }

    .skeleton-avatar {
      border-radius: var(--radius-full);
    }

    .skeleton-avatar-sm {
      width: 32px;
      height: 32px;
    }

    .skeleton-avatar-md {
      width: 40px;
      height: 40px;
    }

    .skeleton-avatar-lg {
      width: 64px;
      height: 64px;
    }

    .skeleton-avatar-info {
      flex: 1;
    }

    /* Animations */
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes scaleIn {
      from {
        opacity: 0;
        transform: scale(0.95);
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

    @keyframes shimmerWave {
      0% {
        transform: translateX(-100%);
      }
      100% {
        transform: translateX(100%);
      }
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .skeleton-content {
        padding: var(--spacing-4);
      }

      .skeleton-product-item {
        padding: var(--spacing-3);
      }
    }

    /* Accessibility */
    @media (prefers-reduced-motion: reduce) {
      .skeleton,
      .skeleton::after,
      .skeleton-container,
      .skeleton-product-card {
        animation: none;
      }

      .skeleton {
        background: var(--color-gray-200);
      }
    }

    /* Dark Mode Support */
    @media (prefers-color-scheme: dark) {
      .skeleton {
        background: linear-gradient(90deg, #2a2a2a 25%, #3a3a3a 50%, #2a2a2a 75%);
      }

      .skeleton-product-card,
      .skeleton-product-item,
      .skeleton-category-card {
        background: var(--color-gray-800);
      }
    }
  `]
})
export class SkeletonLoaderComponent {
    @Input() type: 'product-card' | 'product-list' | 'category-card' | 'text-block' | 'avatar' | 'button' | 'custom' = 'product-card';
    @Input() count = 1;
    @Input() lines = 3;
    @Input() size: 'sm' | 'md' | 'lg' = 'md';
    @Input() showName = true;
    @Input() width = '100%';
    @Input() height = '20px';
    @Input() containerClass = '';
    @Input() customClass = '';

    getArray(length: number): number[] {
        return Array.from({ length }, (_, i) => i);
    }
}