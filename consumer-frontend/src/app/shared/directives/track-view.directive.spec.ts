import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { ObservabilityService } from '../../core/services/observability.service';
import { TrackViewDirective } from './track-view.directive';

@Component({
  template: `
    <div trackView="test_section" [trackViewData]="{ category: 'test' }" style="height: 200px;">Test Section</div>
    <div class="product-card" trackView data-product-id="PROD-123" style="height: 150px;">Product Card</div>
    <div trackView [trackViewThreshold]="0.8" [trackViewDelay]="2000" style="height: 100px;">High Threshold</div>
    <div [trackViewEnabled]="false" trackView="disabled_section" style="height: 100px;">Disabled Tracking</div>
  `,
  standalone: true,
  imports: [TrackViewDirective]
})
class TestComponent {}

describe('TrackViewDirective', () => {
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

  it('should track view events with custom event name', fakeAsync(() => {
    // 模擬 IntersectionObserver
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      // 立即觸發可見性事件
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('div[trackView="test_section"]')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    // 重新初始化組件以使用 mock
    fixture.detectChanges();
    tick(1000); // 等待預設延遲

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_section', jasmine.objectContaining({
      category: 'test',
      elementType: 'div',
      elementText: 'Test Section'
    }));
  }));

  it('should generate event name when not provided', fakeAsync(() => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('.product-card')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();
    tick(1000);

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('view_product-card', jasmine.any(Object));
  }));

  it('should track business events for product cards', fakeAsync(() => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('.product-card')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();
    tick(1000);

    expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith({
      type: 'product_view',
      data: jasmine.objectContaining({
        productId: 'PROD-123',
        viewType: 'card_impression'
      }),
      timestamp: jasmine.any(Number),
      sessionId: 'session-123'
    });
  }));

  it('should respect custom threshold and delay', fakeAsync(() => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback, options) => {
      expect(options.threshold).toBe(0.8);
      
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('div[trackViewThreshold]')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();
    
    // 在延遲時間之前不應該追蹤
    tick(1000);
    expect(mockObservabilityService.trackUserAction).not.toHaveBeenCalled();
    
    // 在延遲時間之後應該追蹤
    tick(1000);
    expect(mockObservabilityService.trackUserAction).toHaveBeenCalled();
  }));

  it('should not track when feature is disabled', () => {
    mockConfigService.isFeatureEnabled.and.returnValue(false);
    
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver');
    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();

    expect(mockIntersectionObserver).not.toHaveBeenCalled();
  });

  it('should not track when trackViewEnabled is false', () => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver');
    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();

    // 檢查是否為 disabled 元素創建了 observer
    const calls = mockIntersectionObserver.calls.all();
    expect(calls.length).toBeLessThan(4); // 應該少於總元素數量
  });

  it('should track view end when element becomes hidden', fakeAsync(() => {
    let intersectionCallback: any;
    
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      intersectionCallback = callback;
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();

    const element = fixture.debugElement.query(By.css('div[trackView="test_section"]')).nativeElement;

    // 模擬元素變為可見
    intersectionCallback([{ isIntersecting: true, target: element }]);
    tick(1000);

    // 模擬元素變為不可見
    intersectionCallback([{ isIntersecting: false, target: element }]);

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_section_end', jasmine.objectContaining({
      viewDuration: jasmine.any(Number)
    }));
  }));

  it('should handle browsers without IntersectionObserver', () => {
    // 移除 IntersectionObserver 支援
    delete (window as any).IntersectionObserver;

    expect(() => {
      fixture.detectChanges();
    }).not.toThrow();
  });

  it('should collect element position and viewport information', fakeAsync(() => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('div[trackView="test_section"]')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();
    tick(1000);

    expect(mockObservabilityService.trackUserAction).toHaveBeenCalledWith('test_section', jasmine.objectContaining({
      elementPosition: jasmine.objectContaining({
        top: jasmine.any(Number),
        left: jasmine.any(Number),
        width: jasmine.any(Number),
        height: jasmine.any(Number)
      }),
      viewport: jasmine.objectContaining({
        width: jasmine.any(Number),
        height: jasmine.any(Number)
      }),
      scrollPosition: jasmine.objectContaining({
        x: jasmine.any(Number),
        y: jasmine.any(Number)
      })
    }));
  }));

  it('should only track once when trackViewOnce is true', fakeAsync(() => {
    let intersectionCallback: any;
    
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      intersectionCallback = callback;
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();

    const element = fixture.debugElement.query(By.css('div[trackView="test_section"]')).nativeElement;

    // 第一次可見
    intersectionCallback([{ isIntersecting: true, target: element }]);
    tick(1000);

    // 變為不可見
    intersectionCallback([{ isIntersecting: false, target: element }]);

    // 再次可見
    intersectionCallback([{ isIntersecting: true, target: element }]);
    tick(1000);

    // 應該只追蹤一次 view 事件（不包括 view_end 事件）
    const viewCalls = mockObservabilityService.trackUserAction.calls.all().filter(call => 
      call.args[0] === 'test_section'
    );
    expect(viewCalls.length).toBe(1);
  }));

  it('should extract product ID from data attributes', fakeAsync(() => {
    const mockIntersectionObserver = jasmine.createSpy('IntersectionObserver').and.callFake((callback) => {
      setTimeout(() => {
        callback([{ isIntersecting: true, target: fixture.debugElement.query(By.css('[data-product-id="PROD-123"]')).nativeElement }]);
      }, 0);
      
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).IntersectionObserver = mockIntersectionObserver;

    fixture.detectChanges();
    tick(1000);

    expect(mockObservabilityService.trackBusinessEvent).toHaveBeenCalledWith(jasmine.objectContaining({
      data: jasmine.objectContaining({
        productId: 'PROD-123'
      })
    }));
  }));
});