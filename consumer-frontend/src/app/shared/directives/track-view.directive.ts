import { Directive, ElementRef, Input, NgZone, OnDestroy, OnInit } from '@angular/core';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { ObservabilityService } from '../../core/services/observability.service';

@Directive({
  selector: '[trackView]',
  standalone: true
})
export class TrackViewDirective implements OnInit, OnDestroy {
  @Input() trackView: string = ''; // 事件名稱
  @Input() trackViewData: Record<string, any> = {}; // 額外數據
  @Input() trackViewThreshold: number = 0.5; // 可見性閾值 (0-1)
  @Input() trackViewDelay: number = 1000; // 延遲時間 (毫秒)
  @Input() trackViewOnce: boolean = true; // 是否只追蹤一次
  @Input() trackViewEnabled: boolean = true; // 是否啟用追蹤

  private intersectionObserver?: IntersectionObserver;
  private viewTimer?: number;
  private hasBeenTracked = false;
  private viewStartTime?: number;

  constructor(
    private elementRef: ElementRef,
    private ngZone: NgZone,
    private observabilityService: ObservabilityService,
    private configService: ObservabilityConfigService
  ) {}

  ngOnInit(): void {
    if (this.shouldTrack()) {
      this.setupViewTracking();
    }
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private shouldTrack(): boolean {
    return this.trackViewEnabled && 
           this.configService.isFeatureEnabled('userBehaviorTracking') &&
           (!this.trackViewOnce || !this.hasBeenTracked);
  }

  private setupViewTracking(): void {
    if (!('IntersectionObserver' in window)) {
      // Fallback for browsers without IntersectionObserver
      this.setupFallbackTracking();
      return;
    }

    this.intersectionObserver = new IntersectionObserver(
      (entries) => this.handleIntersection(entries),
      {
        threshold: this.trackViewThreshold,
        rootMargin: '0px'
      }
    );

    this.intersectionObserver.observe(this.elementRef.nativeElement);
  }

  private setupFallbackTracking(): void {
    // 簡單的 fallback：檢查元素是否在視窗中
    const checkVisibility = () => {
      if (this.isElementVisible()) {
        this.handleElementVisible();
      }
    };

    // 定期檢查可見性
    const intervalId = setInterval(checkVisibility, 1000);
    
    // 清理函數
    this.cleanup = () => {
      clearInterval(intervalId);
    };
  }

  private handleIntersection(entries: IntersectionObserverEntry[]): void {
    this.ngZone.run(() => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          this.handleElementVisible();
        } else {
          this.handleElementHidden();
        }
      });
    });
  }

  private handleElementVisible(): void {
    if (this.hasBeenTracked && this.trackViewOnce) {
      return;
    }

    this.viewStartTime = Date.now();

    // 設定延遲追蹤
    if (this.trackViewDelay > 0) {
      this.viewTimer = window.setTimeout(() => {
        this.trackViewEvent();
      }, this.trackViewDelay);
    } else {
      this.trackViewEvent();
    }
  }

  private handleElementHidden(): void {
    // 清除延遲追蹤
    if (this.viewTimer) {
      clearTimeout(this.viewTimer);
      this.viewTimer = undefined;
    }

    // 如果已經開始追蹤，記錄觀看時間
    if (this.viewStartTime) {
      const viewDuration = Date.now() - this.viewStartTime;
      this.trackViewEnd(viewDuration);
      this.viewStartTime = undefined;
    }
  }

  private trackViewEvent(): void {
    const element = this.elementRef.nativeElement;
    const eventName = this.trackView || this.generateEventName(element);
    
    // 收集可見性上下文資訊
    const viewData = {
      ...this.trackViewData,
      elementType: element.tagName.toLowerCase(),
      elementId: element.id || null,
      elementClass: element.className || null,
      elementText: this.getElementText(element),
      elementPosition: this.getElementPosition(element),
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      },
      scrollPosition: {
        x: window.scrollX,
        y: window.scrollY
      },
      threshold: this.trackViewThreshold,
      delay: this.trackViewDelay,
      timestamp: Date.now(),
      page: window.location.pathname,
      url: window.location.href
    };

    // 追蹤可見性事件
    this.observabilityService.trackUserAction(eventName, viewData);

    // 如果是業務相關的元素，也追蹤業務事件
    this.trackBusinessView(eventName, viewData);

    this.hasBeenTracked = true;
  }

  private trackViewEnd(duration: number): void {
    const element = this.elementRef.nativeElement;
    const eventName = `${this.trackView || this.generateEventName(element)}_end`;
    
    const viewEndData = {
      ...this.trackViewData,
      viewDuration: duration,
      elementType: element.tagName.toLowerCase(),
      elementId: element.id || null,
      timestamp: Date.now(),
      page: window.location.pathname
    };

    this.observabilityService.trackUserAction(eventName, viewEndData);
  }

  private generateEventName(element: HTMLElement): string {
    // 根據元素屬性生成事件名稱
    if (element.id) {
      return `view_${element.id}`;
    }
    
    if (element.className) {
      const firstClass = element.className.split(' ')[0];
      return `view_${firstClass}`;
    }
    
    return `view_${element.tagName.toLowerCase()}`;
  }

  private getElementText(element: HTMLElement): string {
    // 獲取元素的文本內容，限制長度
    const text = element.textContent || element.innerText || '';
    return text.trim().substring(0, 100);
  }

  private getElementPosition(element: HTMLElement): { top: number; left: number; width: number; height: number } {
    const rect = element.getBoundingClientRect();
    return {
      top: rect.top + window.scrollY,
      left: rect.left + window.scrollX,
      width: rect.width,
      height: rect.height
    };
  }

  private isElementVisible(): boolean {
    const element = this.elementRef.nativeElement;
    const rect = element.getBoundingClientRect();
    const windowHeight = window.innerHeight || document.documentElement.clientHeight;
    const windowWidth = window.innerWidth || document.documentElement.clientWidth;

    // 檢查元素是否在視窗範圍內
    const isInViewport = rect.top < windowHeight && 
                        rect.bottom > 0 && 
                        rect.left < windowWidth && 
                        rect.right > 0;

    if (!isInViewport) {
      return false;
    }

    // 計算可見面積比例
    const visibleHeight = Math.min(rect.bottom, windowHeight) - Math.max(rect.top, 0);
    const visibleWidth = Math.min(rect.right, windowWidth) - Math.max(rect.left, 0);
    const visibleArea = visibleHeight * visibleWidth;
    const totalArea = rect.width * rect.height;

    return totalArea > 0 && (visibleArea / totalArea) >= this.trackViewThreshold;
  }

  private trackBusinessView(eventName: string, viewData: any): void {
    // 檢查是否為業務相關的元素可見性
    const businessEvents = this.identifyBusinessViewEvent(eventName, viewData);
    
    businessEvents.forEach(businessEvent => {
      this.observabilityService.trackBusinessEvent(businessEvent);
    });
  }

  private identifyBusinessViewEvent(eventName: string, viewData: any): any[] {
    const businessEvents: any[] = [];
    const element = this.elementRef.nativeElement;
    
    // 商品卡片可見
    if (this.isProductCard(element, eventName)) {
      businessEvents.push({
        type: 'product_view',
        data: {
          productId: this.extractProductId(element),
          viewType: 'card_impression',
          position: viewData.elementPosition,
          ...viewData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    // 廣告橫幅可見
    if (this.isAdvertisementBanner(element, eventName)) {
      businessEvents.push({
        type: 'advertisement_view',
        data: {
          bannerId: this.extractBannerId(element),
          bannerType: this.extractBannerType(element),
          position: viewData.elementPosition,
          ...viewData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    // 推薦區塊可見
    if (this.isRecommendationSection(element, eventName)) {
      businessEvents.push({
        type: 'recommendation_view',
        data: {
          sectionType: this.extractSectionType(element),
          position: viewData.elementPosition,
          ...viewData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    return businessEvents;
  }

  private isProductCard(element: HTMLElement, eventName: string): boolean {
    const className = element.className.toLowerCase();
    
    return className.includes('product-card') ||
           className.includes('product-item') ||
           className.includes('product-tile') ||
           eventName.includes('product');
  }

  private isAdvertisementBanner(element: HTMLElement, eventName: string): boolean {
    const className = element.className.toLowerCase();
    const id = element.id.toLowerCase();
    
    return className.includes('banner') ||
           className.includes('advertisement') ||
           className.includes('promo') ||
           id.includes('banner') ||
           id.includes('ad') ||
           eventName.includes('banner');
  }

  private isRecommendationSection(element: HTMLElement, eventName: string): boolean {
    const className = element.className.toLowerCase();
    const id = element.id.toLowerCase();
    
    return className.includes('recommendation') ||
           className.includes('suggested') ||
           className.includes('related') ||
           id.includes('recommend') ||
           eventName.includes('recommendation');
  }

  private extractProductId(element: HTMLElement): string | null {
    return element.getAttribute('data-product-id') ||
           element.getAttribute('data-id') ||
           element.closest('[data-product-id]')?.getAttribute('data-product-id') ||
           null;
  }

  private extractBannerId(element: HTMLElement): string | null {
    return element.getAttribute('data-banner-id') ||
           element.getAttribute('data-ad-id') ||
           element.id ||
           null;
  }

  private extractBannerType(element: HTMLElement): string {
    const className = element.className.toLowerCase();
    
    if (className.includes('hero')) return 'hero';
    if (className.includes('sidebar')) return 'sidebar';
    if (className.includes('footer')) return 'footer';
    if (className.includes('header')) return 'header';
    
    return 'unknown';
  }

  private extractSectionType(element: HTMLElement): string {
    const className = element.className.toLowerCase();
    
    if (className.includes('related')) return 'related_products';
    if (className.includes('suggested')) return 'suggested_products';
    if (className.includes('trending')) return 'trending_products';
    if (className.includes('popular')) return 'popular_products';
    
    return 'unknown';
  }

  private cleanup(): void {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
    
    if (this.viewTimer) {
      clearTimeout(this.viewTimer);
    }
  }
}