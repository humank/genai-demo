import {
    debounce,
    deepClone,
    extractFilterType,
    extractProductIdFromElement,
    extractProductIdFromUrl,
    extractSearchQuery,
    generateEventId,
    generateSessionId,
    generateTraceId,
    getElementPosition,
    getElementText,
    getScrollPosition,
    getViewportInfo,
    isAddToCartButton,
    isAdvertisementBanner,
    isElementVisible,
    isFilterButton,
    isProductCard,
    isProductLink,
    isRecommendationSection,
    isSearchButton,
    safeJsonStringify,
    throttle
} from './tracking-utils';

describe('TrackingUtils', () => {
  
  describe('ID Generation', () => {
    it('should generate unique event IDs', () => {
      const id1 = generateEventId();
      const id2 = generateEventId();
      
      expect(id1).toMatch(/^event-\d+-[a-z0-9]+$/);
      expect(id2).toMatch(/^event-\d+-[a-z0-9]+$/);
      expect(id1).not.toBe(id2);
    });

    it('should generate unique trace IDs', () => {
      const id1 = generateTraceId();
      const id2 = generateTraceId();
      
      expect(id1).toMatch(/^trace-\d+-[a-z0-9]+$/);
      expect(id2).toMatch(/^trace-\d+-[a-z0-9]+$/);
      expect(id1).not.toBe(id2);
    });

    it('should generate unique session IDs', () => {
      const id1 = generateSessionId();
      const id2 = generateSessionId();
      
      expect(id1).toMatch(/^session-\d+-[a-z0-9]+$/);
      expect(id2).toMatch(/^session-\d+-[a-z0-9]+$/);
      expect(id1).not.toBe(id2);
    });
  });

  describe('Product ID Extraction', () => {
    it('should extract product ID from URL', () => {
      expect(extractProductIdFromUrl('/products/PROD-123')).toBe('PROD-123');
      expect(extractProductIdFromUrl('/products/PROD-123?details=true')).toBe('PROD-123');
      expect(extractProductIdFromUrl('/products/PROD-123/reviews')).toBe('PROD-123');
      expect(extractProductIdFromUrl('/categories/electronics')).toBeNull();
    });

    it('should extract product ID from element attributes', () => {
      const element = document.createElement('div');
      element.setAttribute('data-product-id', 'PROD-456');
      
      expect(extractProductIdFromElement(element)).toBe('PROD-456');
    });

    it('should extract product ID from parent element', () => {
      const parent = document.createElement('div');
      parent.setAttribute('data-product-id', 'PROD-789');
      
      const child = document.createElement('button');
      parent.appendChild(child);
      
      expect(extractProductIdFromElement(child)).toBe('PROD-789');
    });

    it('should extract product ID from link href', () => {
      const link = document.createElement('a');
      link.href = '/products/PROD-999';
      
      expect(extractProductIdFromElement(link)).toBe('PROD-999');
    });
  });

  describe('Element Information', () => {
    it('should get element text with length limit', () => {
      const element = document.createElement('div');
      element.textContent = 'This is a test element';
      
      expect(getElementText(element)).toBe('This is a test element');
      expect(getElementText(element, 10)).toBe('This is a ');
    });

    it('should get element position', () => {
      const element = document.createElement('div');
      document.body.appendChild(element);
      
      // Mock getBoundingClientRect
      spyOn(element, 'getBoundingClientRect').and.returnValue({
        top: 100,
        left: 50,
        width: 200,
        height: 150,
        right: 250,
        bottom: 250
      } as DOMRect);
      
      const position = getElementPosition(element);
      
      expect(position.top).toBe(100);
      expect(position.left).toBe(50);
      expect(position.width).toBe(200);
      expect(position.height).toBe(150);
      
      document.body.removeChild(element);
    });

    it('should get viewport info', () => {
      Object.defineProperty(window, 'innerWidth', { value: 1920, writable: true });
      Object.defineProperty(window, 'innerHeight', { value: 1080, writable: true });
      
      const viewport = getViewportInfo();
      
      expect(viewport.width).toBe(1920);
      expect(viewport.height).toBe(1080);
    });

    it('should get scroll position', () => {
      Object.defineProperty(window, 'scrollX', { value: 100, writable: true });
      Object.defineProperty(window, 'scrollY', { value: 200, writable: true });
      
      const scroll = getScrollPosition();
      
      expect(scroll.x).toBe(100);
      expect(scroll.y).toBe(200);
    });
  });

  describe('Element Type Detection', () => {
    it('should detect add to cart buttons', () => {
      const button1 = document.createElement('button');
      button1.textContent = '加入購物車';
      expect(isAddToCartButton(button1)).toBe(true);

      const button2 = document.createElement('button');
      button2.textContent = 'Add to Cart';
      expect(isAddToCartButton(button2)).toBe(true);

      const button3 = document.createElement('button');
      button3.className = 'add-to-cart';
      expect(isAddToCartButton(button3)).toBe(true);

      const button4 = document.createElement('button');
      button4.textContent = 'Buy Now';
      expect(isAddToCartButton(button4)).toBe(false);
    });

    it('should detect product links', () => {
      const link1 = document.createElement('a');
      link1.href = '/products/123';
      expect(isProductLink(link1)).toBe(true);

      const link2 = document.createElement('a');
      link2.className = 'product-link';
      expect(isProductLink(link2)).toBe(true);

      const link3 = document.createElement('a');
      link3.href = '/categories/electronics';
      expect(isProductLink(link3)).toBe(false);
    });

    it('should detect search buttons', () => {
      const button1 = document.createElement('button');
      button1.textContent = '搜尋';
      expect(isSearchButton(button1)).toBe(true);

      const button2 = document.createElement('button');
      button2.textContent = 'Search';
      expect(isSearchButton(button2)).toBe(true);

      const button3 = document.createElement('button');
      button3.className = 'search-btn';
      expect(isSearchButton(button3)).toBe(true);
    });

    it('should detect filter buttons', () => {
      const button1 = document.createElement('button');
      button1.className = 'filter-btn';
      expect(isFilterButton(button1)).toBe(true);

      const button2 = document.createElement('button');
      button2.textContent = '篩選';
      expect(isFilterButton(button2)).toBe(true);

      const button3 = document.createElement('button');
      button3.textContent = 'Sort';
      expect(isFilterButton(button3)).toBe(false);
    });

    it('should detect product cards', () => {
      const card1 = document.createElement('div');
      card1.className = 'product-card';
      expect(isProductCard(card1)).toBe(true);

      const card2 = document.createElement('div');
      card2.className = 'product-item';
      expect(isProductCard(card2)).toBe(true);

      const card3 = document.createElement('div');
      card3.className = 'news-card';
      expect(isProductCard(card3)).toBe(false);
    });

    it('should detect advertisement banners', () => {
      const banner1 = document.createElement('div');
      banner1.className = 'banner';
      expect(isAdvertisementBanner(banner1)).toBe(true);

      const banner2 = document.createElement('div');
      banner2.id = 'ad-banner';
      expect(isAdvertisementBanner(banner2)).toBe(true);

      const banner3 = document.createElement('div');
      banner3.className = 'content';
      expect(isAdvertisementBanner(banner3)).toBe(false);
    });

    it('should detect recommendation sections', () => {
      const section1 = document.createElement('div');
      section1.className = 'recommendation';
      expect(isRecommendationSection(section1)).toBe(true);

      const section2 = document.createElement('div');
      section2.id = 'recommend-products';
      expect(isRecommendationSection(section2)).toBe(true);

      const section3 = document.createElement('div');
      section3.className = 'main-content';
      expect(isRecommendationSection(section3)).toBe(false);
    });
  });

  describe('Search Query Extraction', () => {
    beforeEach(() => {
      document.body.innerHTML = '';
    });

    it('should extract search query from input', () => {
      const input = document.createElement('input');
      input.type = 'search';
      input.value = 'laptop';
      document.body.appendChild(input);

      expect(extractSearchQuery()).toBe('laptop');
    });

    it('should return null when no search query found', () => {
      expect(extractSearchQuery()).toBeNull();
    });
  });

  describe('Filter Type Extraction', () => {
    it('should extract filter type from element class', () => {
      const element1 = document.createElement('button');
      element1.className = 'category-filter';
      expect(extractFilterType(element1)).toBe('category');

      const element2 = document.createElement('button');
      element2.className = 'price-range';
      expect(extractFilterType(element2)).toBe('price');

      const element3 = document.createElement('button');
      element3.className = 'unknown-filter';
      expect(extractFilterType(element3)).toBe('unknown');
    });
  });

  describe('Element Visibility', () => {
    it('should check if element is visible', () => {
      const element = document.createElement('div');
      document.body.appendChild(element);

      // Mock getBoundingClientRect for visible element
      spyOn(element, 'getBoundingClientRect').and.returnValue({
        top: 100,
        left: 100,
        width: 200,
        height: 200,
        right: 300,
        bottom: 300
      } as DOMRect);

      Object.defineProperty(window, 'innerWidth', { value: 1920, writable: true });
      Object.defineProperty(window, 'innerHeight', { value: 1080, writable: true });

      expect(isElementVisible(element)).toBe(true);

      document.body.removeChild(element);
    });

    it('should detect invisible element', () => {
      const element = document.createElement('div');
      document.body.appendChild(element);

      // Mock getBoundingClientRect for invisible element
      spyOn(element, 'getBoundingClientRect').and.returnValue({
        top: -200,
        left: 100,
        width: 200,
        height: 200,
        right: 300,
        bottom: 0
      } as DOMRect);

      expect(isElementVisible(element)).toBe(false);

      document.body.removeChild(element);
    });
  });

  describe('Utility Functions', () => {
    it('should throttle function calls', (done) => {
      let callCount = 0;
      const throttledFn = throttle(() => callCount++, 100);

      throttledFn();
      throttledFn();
      throttledFn();

      expect(callCount).toBe(1);

      setTimeout(() => {
        throttledFn();
        expect(callCount).toBe(2);
        done();
      }, 150);
    });

    it('should debounce function calls', (done) => {
      let callCount = 0;
      const debouncedFn = debounce(() => callCount++, 100);

      debouncedFn();
      debouncedFn();
      debouncedFn();

      expect(callCount).toBe(0);

      setTimeout(() => {
        expect(callCount).toBe(1);
        done();
      }, 150);
    });

    it('should deep clone objects', () => {
      const original = {
        a: 1,
        b: {
          c: 2,
          d: [3, 4, { e: 5 }]
        },
        f: new Date('2023-01-01')
      };

      const cloned = deepClone(original);

      expect(cloned).toEqual(original);
      expect(cloned).not.toBe(original);
      expect(cloned.b).not.toBe(original.b);
      expect(cloned.b.d).not.toBe(original.b.d);
      expect(cloned.f).not.toBe(original.f);
    });

    it('should safely stringify JSON', () => {
      const obj = { a: 1, b: 'test' };
      expect(safeJsonStringify(obj)).toBe('{"a":1,"b":"test"}');

      // Test circular reference
      const circular: any = { a: 1 };
      circular.self = circular;
      
      const result = safeJsonStringify(circular);
      expect(result).toContain('[Circular Reference]');
    });

    it('should handle errors in JSON stringify', () => {
      const error = new Error('Test error');
      const result = safeJsonStringify(error);
      
      expect(result).toContain('Test error');
      expect(result).toContain('name');
      expect(result).toContain('message');
    });
  });
});