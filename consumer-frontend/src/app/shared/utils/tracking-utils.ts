/**
 * 可觀測性追蹤工具函數
 */

/**
 * 生成唯一的事件 ID
 */
export function generateEventId(): string {
  return `event-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 生成唯一的追蹤 ID
 */
export function generateTraceId(): string {
  return `trace-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 生成唯一的會話 ID
 */
export function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 從 URL 中提取產品 ID
 */
export function extractProductIdFromUrl(url: string): string | null {
  const match = url.match(/\/products\/([^\/\?]+)/);
  return match ? match[1] : null;
}

/**
 * 從元素中提取產品 ID
 */
export function extractProductIdFromElement(element: HTMLElement): string | null {
  // 嘗試從各種屬性中提取產品 ID
  const productId = element.getAttribute('data-product-id') ||
                   element.getAttribute('data-id') ||
                   element.closest('[data-product-id]')?.getAttribute('data-product-id') ||
                   element.closest('[data-id]')?.getAttribute('data-id');
  
  if (productId) {
    return productId;
  }
  
  // 從連結 URL 中提取產品 ID
  const href = (element as HTMLAnchorElement).href;
  if (href) {
    return extractProductIdFromUrl(href);
  }
  
  return null;
}

/**
 * 獲取元素的文本內容（限制長度）
 */
export function getElementText(element: HTMLElement, maxLength: number = 100): string {
  const text = element.textContent || element.innerText || '';
  return text.trim().substring(0, maxLength);
}

/**
 * 獲取元素的位置資訊
 */
export function getElementPosition(element: HTMLElement): {
  top: number;
  left: number;
  width: number;
  height: number;
} {
  const rect = element.getBoundingClientRect();
  return {
    top: rect.top + window.scrollY,
    left: rect.left + window.scrollX,
    width: rect.width,
    height: rect.height
  };
}

/**
 * 獲取視窗資訊
 */
export function getViewportInfo(): {
  width: number;
  height: number;
} {
  return {
    width: window.innerWidth,
    height: window.innerHeight
  };
}

/**
 * 獲取滾動位置
 */
export function getScrollPosition(): {
  x: number;
  y: number;
} {
  return {
    x: window.scrollX,
    y: window.scrollY
  };
}

/**
 * 檢查元素是否為加入購物車按鈕
 */
export function isAddToCartButton(element: HTMLElement): boolean {
  const text = element.textContent?.toLowerCase() || '';
  const className = element.className.toLowerCase();
  const id = element.id.toLowerCase();
  
  return text.includes('加入購物車') ||
         text.includes('add to cart') ||
         className.includes('add-to-cart') ||
         className.includes('cart-add') ||
         id.includes('add-cart');
}

/**
 * 檢查元素是否為產品連結
 */
export function isProductLink(element: HTMLElement): boolean {
  const href = (element as HTMLAnchorElement).href || '';
  const className = element.className.toLowerCase();
  
  return href.includes('/products/') ||
         className.includes('product-link') ||
         className.includes('product-card');
}

/**
 * 檢查元素是否為搜尋按鈕
 */
export function isSearchButton(element: HTMLElement): boolean {
  const text = element.textContent?.toLowerCase() || '';
  const className = element.className.toLowerCase();
  const type = (element as HTMLInputElement).type;
  
  return text.includes('搜尋') ||
         text.includes('search') ||
         className.includes('search') ||
         (type === 'submit' && isInSearchForm(element));
}

/**
 * 檢查元素是否在搜尋表單中
 */
export function isInSearchForm(element: HTMLElement): boolean {
  const form = element.closest('form');
  if (!form) return false;
  
  const formClass = form.className.toLowerCase();
  const formId = form.id.toLowerCase();
  
  return formClass.includes('search') || formId.includes('search');
}

/**
 * 檢查元素是否為篩選按鈕
 */
export function isFilterButton(element: HTMLElement): boolean {
  const className = element.className.toLowerCase();
  const text = element.textContent?.toLowerCase() || '';
  
  return className.includes('filter') ||
         className.includes('category') ||
         className.includes('sort') ||
         text.includes('篩選') ||
         text.includes('分類') ||
         text.includes('排序');
}

/**
 * 檢查元素是否為產品卡片
 */
export function isProductCard(element: HTMLElement): boolean {
  const className = element.className.toLowerCase();
  
  return className.includes('product-card') ||
         className.includes('product-item') ||
         className.includes('product-tile');
}

/**
 * 檢查元素是否為廣告橫幅
 */
export function isAdvertisementBanner(element: HTMLElement): boolean {
  const className = element.className.toLowerCase();
  const id = element.id.toLowerCase();
  
  return className.includes('banner') ||
         className.includes('advertisement') ||
         className.includes('promo') ||
         id.includes('banner') ||
         id.includes('ad');
}

/**
 * 檢查元素是否為推薦區塊
 */
export function isRecommendationSection(element: HTMLElement): boolean {
  const className = element.className.toLowerCase();
  const id = element.id.toLowerCase();
  
  return className.includes('recommendation') ||
         className.includes('suggested') ||
         className.includes('related') ||
         id.includes('recommend');
}

/**
 * 從搜尋輸入框中提取搜尋查詢
 */
export function extractSearchQuery(): string | null {
  const searchInputs = document.querySelectorAll(
    'input[type="search"], input[name*="search"], input[placeholder*="搜尋"], input[placeholder*="search"]'
  );
  
  for (const input of Array.from(searchInputs)) {
    const value = (input as HTMLInputElement).value.trim();
    if (value) {
      return value;
    }
  }
  
  return null;
}

/**
 * 提取篩選類型
 */
export function extractFilterType(element: HTMLElement): string {
  const className = element.className.toLowerCase();
  
  if (className.includes('category')) return 'category';
  if (className.includes('price')) return 'price';
  if (className.includes('brand')) return 'brand';
  if (className.includes('rating')) return 'rating';
  if (className.includes('sort')) return 'sort';
  
  return 'unknown';
}

/**
 * 提取橫幅 ID
 */
export function extractBannerId(element: HTMLElement): string | null {
  return element.getAttribute('data-banner-id') ||
         element.getAttribute('data-ad-id') ||
         element.id ||
         null;
}

/**
 * 提取橫幅類型
 */
export function extractBannerType(element: HTMLElement): string {
  const className = element.className.toLowerCase();
  
  if (className.includes('hero')) return 'hero';
  if (className.includes('sidebar')) return 'sidebar';
  if (className.includes('footer')) return 'footer';
  if (className.includes('header')) return 'header';
  
  return 'unknown';
}

/**
 * 提取區塊類型
 */
export function extractSectionType(element: HTMLElement): string {
  const className = element.className.toLowerCase();
  
  if (className.includes('related')) return 'related_products';
  if (className.includes('suggested')) return 'suggested_products';
  if (className.includes('trending')) return 'trending_products';
  if (className.includes('popular')) return 'popular_products';
  
  return 'unknown';
}

/**
 * 檢查元素是否在視窗中可見
 */
export function isElementVisible(element: HTMLElement, threshold: number = 0.5): boolean {
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

  return totalArea > 0 && (visibleArea / totalArea) >= threshold;
}

/**
 * 節流函數
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: number | undefined;
  let lastExecTime = 0;
  
  return (...args: Parameters<T>) => {
    const currentTime = Date.now();
    
    if (currentTime - lastExecTime > delay) {
      func(...args);
      lastExecTime = currentTime;
    } else {
      clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => {
        func(...args);
        lastExecTime = Date.now();
      }, delay - (currentTime - lastExecTime));
    }
  };
}

/**
 * 防抖函數
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: number | undefined;
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = window.setTimeout(() => func(...args), delay);
  };
}

/**
 * 深度複製對象
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as any;
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as any;
  }
  
  if (typeof obj === 'object') {
    const cloned = {} as any;
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        cloned[key] = deepClone(obj[key]);
      }
    }
    return cloned;
  }
  
  return obj;
}

/**
 * 安全的 JSON 序列化
 */
export function safeJsonStringify(obj: any, maxDepth: number = 10): string {
  const seen = new WeakSet();
  
  const replacer = (key: string, value: any, depth: number = 0): any => {
    if (depth > maxDepth) {
      return '[Max Depth Reached]';
    }
    
    if (value === null || typeof value !== 'object') {
      return value;
    }
    
    if (seen.has(value)) {
      return '[Circular Reference]';
    }
    
    seen.add(value);
    
    if (value instanceof Error) {
      return {
        name: value.name,
        message: value.message,
        stack: value.stack
      };
    }
    
    if (value instanceof Date) {
      return value.toISOString();
    }
    
    if (typeof value === 'function') {
      return '[Function]';
    }
    
    return value;
  };
  
  try {
    return JSON.stringify(obj, (key, value) => replacer(key, value));
  } catch (error) {
    return JSON.stringify({ error: 'Serialization failed', message: (error as Error).message });
  }
}