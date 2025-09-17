import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { ObservabilityService } from '../../core/services/observability.service';

@Directive({
  selector: '[trackClick]',
  standalone: true
})
export class TrackClickDirective implements OnInit, OnDestroy {
  @Input() trackClick: string = ''; // 事件名稱
  @Input() trackClickData: Record<string, any> = {}; // 額外數據
  @Input() trackClickCategory: string = 'user_interaction'; // 事件分類
  @Input() trackClickEnabled: boolean = true; // 是否啟用追蹤

  private clickListener?: () => void;

  constructor(
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private observabilityService: ObservabilityService,
    private configService: ObservabilityConfigService
  ) {}

  ngOnInit(): void {
    if (this.shouldTrack()) {
      this.setupClickTracking();
    }
  }

  ngOnDestroy(): void {
    if (this.clickListener) {
      this.clickListener();
    }
  }

  private shouldTrack(): boolean {
    return this.trackClickEnabled && 
           this.configService.isFeatureEnabled('userBehaviorTracking');
  }

  private setupClickTracking(): void {
    const element = this.elementRef.nativeElement;
    
    this.clickListener = this.renderer.listen(element, 'click', (event: MouseEvent) => {
      this.handleClick(event);
    });
  }

  private handleClick(event: MouseEvent): void {
    const element = this.elementRef.nativeElement;
    const eventName = this.trackClick || this.generateEventName(element);
    
    // 收集點擊上下文資訊
    const clickData = {
      ...this.trackClickData,
      category: this.trackClickCategory,
      elementType: element.tagName.toLowerCase(),
      elementId: element.id || null,
      elementClass: element.className || null,
      elementText: this.getElementText(element),
      clickPosition: {
        x: event.clientX,
        y: event.clientY
      },
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      },
      timestamp: Date.now(),
      page: window.location.pathname,
      url: window.location.href
    };

    // 追蹤點擊事件
    this.observabilityService.trackUserAction(eventName, clickData);

    // 如果是業務相關的點擊，也追蹤業務事件
    this.trackBusinessClick(eventName, clickData);
  }

  private generateEventName(element: HTMLElement): string {
    // 根據元素屬性生成事件名稱
    if (element.id) {
      return `click_${element.id}`;
    }
    
    if (element.className) {
      const firstClass = element.className.split(' ')[0];
      return `click_${firstClass}`;
    }
    
    return `click_${element.tagName.toLowerCase()}`;
  }

  private getElementText(element: HTMLElement): string {
    // 獲取元素的文本內容，限制長度
    const text = element.textContent || element.innerText || '';
    return text.trim().substring(0, 100);
  }

  private trackBusinessClick(eventName: string, clickData: any): void {
    // 檢查是否為業務相關的點擊
    const businessEvents = this.identifyBusinessEvent(eventName, clickData);
    
    businessEvents.forEach(businessEvent => {
      this.observabilityService.trackBusinessEvent(businessEvent);
    });
  }

  private identifyBusinessEvent(eventName: string, clickData: any): any[] {
    const businessEvents: any[] = [];
    const element = this.elementRef.nativeElement;
    
    // 加入購物車按鈕
    if (this.isAddToCartButton(element, eventName)) {
      businessEvents.push({
        type: 'cart_add',
        data: {
          productId: this.extractProductId(element),
          buttonText: clickData.elementText,
          ...clickData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    // 商品連結點擊
    if (this.isProductLink(element, eventName)) {
      businessEvents.push({
        type: 'product_view',
        data: {
          productId: this.extractProductId(element),
          linkText: clickData.elementText,
          ...clickData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    // 搜尋按鈕
    if (this.isSearchButton(element, eventName)) {
      const searchQuery = this.extractSearchQuery();
      if (searchQuery) {
        businessEvents.push({
          type: 'search',
          data: {
            query: searchQuery,
            searchType: 'button_click',
            ...clickData
          },
          timestamp: Date.now(),
          sessionId: this.observabilityService.getSessionId()
        });
      }
    }
    
    // 篩選器點擊
    if (this.isFilterButton(element, eventName)) {
      businessEvents.push({
        type: 'filter_apply',
        data: {
          filterType: this.extractFilterType(element),
          filterValue: clickData.elementText,
          ...clickData
        },
        timestamp: Date.now(),
        sessionId: this.observabilityService.getSessionId()
      });
    }
    
    return businessEvents;
  }

  private isAddToCartButton(element: HTMLElement, eventName: string): boolean {
    const text = element.textContent?.toLowerCase() || '';
    const className = element.className.toLowerCase();
    const id = element.id.toLowerCase();
    
    return text.includes('加入購物車') ||
           text.includes('add to cart') ||
           className.includes('add-to-cart') ||
           className.includes('cart-add') ||
           id.includes('add-cart') ||
           eventName.includes('cart');
  }

  private isProductLink(element: HTMLElement, eventName: string): boolean {
    const href = (element as HTMLAnchorElement).href || '';
    const className = element.className.toLowerCase();
    
    return href.includes('/products/') ||
           className.includes('product-link') ||
           className.includes('product-card') ||
           eventName.includes('product');
  }

  private isSearchButton(element: HTMLElement, eventName: string): boolean {
    const text = element.textContent?.toLowerCase() || '';
    const className = element.className.toLowerCase();
    const type = (element as HTMLInputElement).type;
    
    return text.includes('搜尋') ||
           text.includes('search') ||
           className.includes('search') ||
           type === 'submit' && this.isInSearchForm(element) ||
           eventName.includes('search');
  }

  private isFilterButton(element: HTMLElement, eventName: string): boolean {
    const className = element.className.toLowerCase();
    const text = element.textContent?.toLowerCase() || '';
    
    return className.includes('filter') ||
           className.includes('category') ||
           className.includes('sort') ||
           text.includes('篩選') ||
           text.includes('分類') ||
           text.includes('排序') ||
           eventName.includes('filter');
  }

  private extractProductId(element: HTMLElement): string | null {
    // 嘗試從各種屬性中提取產品 ID
    const productId = element.getAttribute('data-product-id') ||
                     element.getAttribute('data-id') ||
                     element.closest('[data-product-id]')?.getAttribute('data-product-id') ||
                     element.closest('[data-id]')?.getAttribute('data-id');
    
    if (productId) {
      return productId;
    }
    
    // 從 URL 中提取產品 ID
    const href = (element as HTMLAnchorElement).href;
    if (href) {
      const match = href.match(/\/products\/([^\/\?]+)/);
      return match ? match[1] : null;
    }
    
    return null;
  }

  private extractSearchQuery(): string | null {
    // 尋找附近的搜尋輸入框
    const searchInputs = document.querySelectorAll('input[type="search"], input[name*="search"], input[placeholder*="搜尋"], input[placeholder*="search"]');
    
    for (const input of Array.from(searchInputs)) {
      const value = (input as HTMLInputElement).value.trim();
      if (value) {
        return value;
      }
    }
    
    return null;
  }

  private extractFilterType(element: HTMLElement): string {
    const className = element.className.toLowerCase();
    
    if (className.includes('category')) return 'category';
    if (className.includes('price')) return 'price';
    if (className.includes('brand')) return 'brand';
    if (className.includes('rating')) return 'rating';
    if (className.includes('sort')) return 'sort';
    
    return 'unknown';
  }

  private isInSearchForm(element: HTMLElement): boolean {
    const form = element.closest('form');
    if (!form) return false;
    
    const formClass = form.className.toLowerCase();
    const formId = form.id.toLowerCase();
    
    return formClass.includes('search') || formId.includes('search');
  }
}