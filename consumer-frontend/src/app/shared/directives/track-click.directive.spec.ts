import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { ObservabilityService } from '../../core/services/observability.service';
import { TrackClickDirective } from './track-click.directive';

@Component({
  template: `
    <button trackClick="test_button" [trackClickData]="{ category: 'test' }">Test Button</button>
    <a href="/products/123" trackClick="product_link" data-product-id="PROD-123">Product Link</a>
    <button class="add-to-cart" trackClick>Add to Cart</button>
    <button [trackClickEnabled]="false" trackClick="disabled_button">Disabled Tracking</button>
  `,
  standalone: true,
  imports: [TrackClickDirective]
})
class TestComponent {}

describe('TrackClickDirective', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
  let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;

  beforeEach(async () => {
    const observabilitySpy = jasmine.createSpyObj('ObservabilityService', [
      'trackUserAction', 
      'trackBusinessEvent',
      'getSessionId'
    ]);
    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', [
      'isFeatureEnabled'
    ]);

    await TestBed.configureTestingModule({
      imports: [TestComponent],
      providers: [
        { provide: ObservabilityService, useValue: observabilitySpy },
        { provide: ObservabilityConfigService, useValue: configSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
    mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;

    // 預設啟用追蹤
    mockConfigService.isFeatureEnabled.and.returnValue(true);
    mockObservabilityService.getSessionId.and.returnValue('session-123');

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should track click events with custom event name', () => {
    const button = fixture.debugElement.query(By.css('button[trackClick="test_button"]'));
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_button', jasmine.objectContaining({
      category: 'test',
      elementType: 'button',
      elementText: 'Test Button'
    }));
  });

  it('should generate event name when not provided', () => {
    const button = fixture.debugElement.query(By.css('.add-to-cart'));
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('click_add-to-cart', jasmine.any(Object));
  });

  it('should track business events for product links', () => {
    const link = fixture.debugElement.query(By.css('a[href="/products/123"]'));
    
    link.nativeElement.click();

    expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
      type: 'product_view',
      data: jasmine.objectContaining({
        productId: 'PROD-123',
        linkText: 'Product Link'
      }),
      timestamp: jasmine.any(Number),
      sessionId: 'session-123'
    });
  });

  it('should track business events for add to cart buttons', () => {
    const button = fixture.debugElement.query(By.css('.add-to-cart'));
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
      type: 'cart_add',
      data: jasmine.objectContaining({
        buttonText: 'Add to Cart'
      }),
      timestamp: jasmine.any(Number),
      sessionId: 'session-123'
    });
  });

  it('should not track when feature is disabled', () => {
    mockConfigService.isFeatureEnabled.and.returnValue(false);
    
    const button = fixture.debugElement.query(By.css('button[trackClick="test_button"]'));
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).not.toHaveBeenCalled();
  });

  it('should not track when trackClickEnabled is false', () => {
    const button = fixture.debugElement.query(By.css('button[trackClick="disabled_button"]'));
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).not.toHaveBeenCalled();
  });

  it('should collect click position and viewport information', () => {
    const button = fixture.debugElement.query(By.css('button[trackClick="test_button"]'));
    
    // 模擬點擊事件
    const clickEvent = new MouseEvent('click', {
      clientX: 100,
      clientY: 200
    });
    
    button.nativeElement.dispatchEvent(clickEvent);

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_button', jasmine.objectContaining({
      clickPosition: { x: 100, y: 200 },
      viewport: jasmine.objectContaining({
        width: jasmine.any(Number),
        height: jasmine.any(Number)
      })
    }));
  });

  it('should extract product ID from data attributes', () => {
    const link = fixture.debugElement.query(By.css('a[data-product-id="PROD-123"]'));
    
    link.nativeElement.click();

    expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
      data: jasmine.objectContaining({
        productId: 'PROD-123'
      })
    }));
  });

  it('should include page and URL information', () => {
    const button = fixture.debugElement.query(By.css('button[trackClick="test_button"]'));
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_button', jasmine.objectContaining({
      page: jasmine.any(String),
      url: jasmine.any(String)
    }));
  });

  it('should handle elements without ID or class', () => {
    // 創建一個沒有 ID 或 class 的元素
    const plainButton = document.createElement('button');
    plainButton.textContent = 'Plain Button';
    plainButton.setAttribute('trackClick', '');
    
    fixture.nativeElement.appendChild(plainButton);
    fixture.detectChanges();

    plainButton.click();

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('click_button', jasmine.any(Object));
  });

  it('should limit element text length', () => {
    const longText = 'A'.repeat(200);
    const button = fixture.debugElement.query(By.css('button[trackClick="test_button"]'));
    button.nativeElement.textContent = longText;
    
    button.nativeElement.click();

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_button', jasmine.objectContaining({
      elementText: longText.substring(0, 100)
    }));
  });
});