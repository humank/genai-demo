import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ObservabilityConfigService } from '../../core/config/observability.config';
import { BatchProcessorService } from '../../core/services/batch-processor.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { PerformanceMonitorComponent } from './performance-monitor.component';

describe('PerformanceMonitorComponent', () => {
  let component: PerformanceMonitorComponent;
  let fixture: ComponentFixture<PerformanceMonitorComponent>;
  let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
  let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;
  let mockBatchProcessor: jasmine.SpyObj<BatchProcessorService>;

  beforeEach(async () => {
    const observabilitySpy = jasmine.createSpyObj('ObservabilityService', ['trackUserAction']);
    const configSpy = jasmine.createSpyObj('ObservabilityConfigService', [
      'isPerformanceMonitoringEnabled',
      'isDevelopment'
    ]);
    const batchProcessorSpy = jasmine.createSpyObj('BatchProcessorService', ['getStatus']);

    await TestBed.configureTestingModule({
      imports: [PerformanceMonitorComponent],
      providers: [
        { provide: ObservabilityService, useValue: observabilitySpy },
        { provide: ObservabilityConfigService, useValue: configSpy },
        { provide: BatchProcessorService, useValue: batchProcessorSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerformanceMonitorComponent);
    component = fixture.componentInstance;
    mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
    mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
    mockBatchProcessor = TestBed.inject(BatchProcessorService) as jasmine.SpyObj<BatchProcessorService>;

    // 預設配置
    mockConfigService.isPerformanceMonitoringEnabled.and.returnValue(true);
    mockConfigService.isDevelopment.and.returnValue(true);
    mockBatchProcessor.getStatus.and.returnValue({
      isProcessing: false,
      queueLength: 0,
      totalProcessed: 10,
      totalFailed: 1,
      isOnline: true
    });

    // Mock performance API
    Object.defineProperty(window, 'performance', {
      value: {
        timing: {
          navigationStart: 1000,
          loadEventEnd: 3000,
          domContentLoadedEventEnd: 2500,
          responseStart: 1200
        },
        getEntriesByType: jasmine.createSpy('getEntriesByType').and.returnValue([
          { name: 'first-paint', startTime: 1500 },
          { name: 'first-contentful-paint', startTime: 1800 }
        ]),
        memory: {
          usedJSHeapSize: 10485760,
          totalJSHeapSize: 20971520,
          jsHeapSizeLimit: 2147483648
        }
      },
      writable: true
    });

    // Mock navigator.connection
    Object.defineProperty(navigator, 'connection', {
      value: {
        type: 'wifi',
        effectiveType: '4g',
        downlink: 10,
        rtt: 50
      },
      writable: true
    });
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize when performance monitoring is enabled', () => {
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.isEnabled).toBe(true);
    expect(component.hasMemoryInfo).toBe(true);
    expect(component.hasNetworkInfo).toBe(true);
  });

  it('should not initialize when performance monitoring is disabled', () => {
    mockConfigService.isPerformanceMonitoringEnabled.and.returnValue(false);
    
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.isEnabled).toBe(false);
  });

  it('should not initialize in production environment', () => {
    mockConfigService.isDevelopment.and.returnValue(false);
    
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.isEnabled).toBe(false);
  });

  it('should collect navigation metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.metrics.pageLoadTime).toBe(2000); // 3000 - 1000
    expect(component.metrics.domContentLoaded).toBe(1500); // 2500 - 1000
    expect(component.metrics.ttfb).toBe(200); // 1200 - 1000
    expect(component.metrics.firstPaint).toBe(1500);
    expect(component.metrics.firstContentfulPaint).toBe(1800);
  });

  it('should collect memory metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.metrics.usedJSHeapSize).toBe(10485760);
    expect(component.metrics.totalJSHeapSize).toBe(20971520);
    expect(component.metrics.jsHeapSizeLimit).toBe(2147483648);
  });

  it('should collect network metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();

    expect(component.metrics.connectionType).toBe('wifi');
    expect(component.metrics.effectiveType).toBe('4g');
    expect(component.metrics.downlink).toBe(10);
    expect(component.metrics.rtt).toBe(50);
  });

  it('should toggle expanded state', () => {
    expect(component.isExpanded).toBe(false);
    
    component.toggleExpanded();
    expect(component.isExpanded).toBe(true);
    
    component.toggleExpanded();
    expect(component.isExpanded).toBe(false);
  });

  it('should refresh metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();
    
    const originalPageLoadTime = component.metrics.pageLoadTime;
    
    // 修改 performance.timing 來模擬新的指標
    (window.performance.timing as any).loadEventEnd = 4000;
    
    component.refreshMetrics();
    
    expect(component.metrics.pageLoadTime).toBe(3000); // 4000 - 1000
    expect(component.metrics.pageLoadTime).not.toBe(originalPageLoadTime);
  });

  it('should clear metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();
    
    expect(Object.keys(component.metrics).length).toBeGreaterThan(0);
    
    component.clearMetrics();
    
    expect(Object.keys(component.metrics).length).toBe(0);
  });

  it('should export metrics', () => {
    component.showMonitor = true;
    component.ngOnInit();
    
    // Mock URL.createObjectURL and document.createElement
    const mockUrl = 'blob:mock-url';
    spyOn(URL, 'createObjectURL').and.returnValue(mockUrl);
    spyOn(URL, 'revokeObjectURL');
    
    const mockAnchor = {
      href: '',
      download: '',
      click: jasmine.createSpy('click')
    };
    spyOn(document, 'createElement').and.returnValue(mockAnchor as any);
    
    component.exportMetrics();
    
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(mockAnchor.href).toBe(mockUrl);
    expect(mockAnchor.download).toMatch(/performance-metrics-\d+\.json/);
    expect(mockAnchor.click).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);
  });

  it('should update batch status periodically', fakeAsync(() => {
    component.showMonitor = true;
    component.updateInterval = 1000; // 1 second for testing
    component.ngOnInit();

    const initialStatus = { ...component.batchStatus };
    
    // 修改 batch processor 狀態
    mockBatchProcessor.getStatus.and.returnValue({
      isProcessing: true,
      queueLength: 5,
      totalProcessed: 15,
      totalFailed: 2,
      isOnline: false
    });

    tick(1000);

    expect(component.batchStatus.isProcessing).toBe(true);
    expect(component.batchStatus.queueLength).toBe(5);
    expect(component.batchStatus.totalProcessed).toBe(15);
    expect(component.batchStatus.totalFailed).toBe(2);
    expect(component.batchStatus.isOnline).toBe(false);
  }));

  it('should format time values correctly', () => {
    expect(component.formatTime(1234.56)).toBe('1235ms');
    expect(component.formatTime(undefined)).toBe('N/A');
    expect(component.formatTime(null as any)).toBe('N/A');
  });

  it('should format CLS values correctly', () => {
    expect(component.formatCLS(0.123456)).toBe('0.123');
    expect(component.formatCLS(undefined)).toBe('N/A');
    expect(component.formatCLS(null as any)).toBe('N/A');
  });

  it('should format memory values correctly', () => {
    expect(component.formatMemory(10485760)).toBe('10MB'); // 10MB
    expect(component.formatMemory(undefined)).toBe('N/A');
    expect(component.formatMemory(null as any)).toBe('N/A');
  });

  it('should format speed values correctly', () => {
    expect(component.formatSpeed(10.5)).toBe('10.5Mbps');
    expect(component.formatSpeed(undefined)).toBe('N/A');
    expect(component.formatSpeed(null as any)).toBe('N/A');
  });

  it('should evaluate LCP scores correctly', () => {
    expect(component.isGoodLCP(2000)).toBe(true);
    expect(component.isGoodLCP(2500)).toBe(true);
    expect(component.isGoodLCP(3000)).toBe(false);
    expect(component.isPoorLCP(3000)).toBe(false);
    expect(component.isPoorLCP(4500)).toBe(true);
  });

  it('should evaluate FID scores correctly', () => {
    expect(component.isGoodFID(50)).toBe(true);
    expect(component.isGoodFID(100)).toBe(true);
    expect(component.isGoodFID(200)).toBe(false);
    expect(component.isPoorFID(200)).toBe(false);
    expect(component.isPoorFID(400)).toBe(true);
  });

  it('should evaluate CLS scores correctly', () => {
    expect(component.isGoodCLS(0.05)).toBe(true);
    expect(component.isGoodCLS(0.1)).toBe(true);
    expect(component.isGoodCLS(0.2)).toBe(false);
    expect(component.isPoorCLS(0.2)).toBe(false);
    expect(component.isPoorCLS(0.3)).toBe(true);
  });

  it('should handle missing performance API gracefully', () => {
    delete (window as any).performance;
    
    expect(() => {
      component.showMonitor = true;
      component.ngOnInit();
    }).not.toThrow();
  });

  it('should handle missing memory info gracefully', () => {
    delete (window.performance as any).memory;
    
    component.showMonitor = true;
    component.ngOnInit();
    
    expect(component.hasMemoryInfo).toBe(false);
    expect(component.metrics.usedJSHeapSize).toBeUndefined();
  });

  it('should handle missing network info gracefully', () => {
    delete (navigator as any).connection;
    
    component.showMonitor = true;
    component.ngOnInit();
    
    expect(component.hasNetworkInfo).toBe(false);
    expect(component.metrics.connectionType).toBeUndefined();
  });

  it('should cleanup on destroy', () => {
    component.showMonitor = true;
    component.ngOnInit();
    
    spyOn(component as any, 'cleanup');
    
    component.ngOnDestroy();
    
    expect((component as any).cleanup).toHaveBeenCalled();
  });

  it('should setup performance observer when available', () => {
    const mockPerformanceObserver = jasmine.createSpy('PerformanceObserver').and.callFake((callback) => {
      return {
        observe: jasmine.createSpy('observe'),
        disconnect: jasmine.createSpy('disconnect')
      };
    });

    (window as any).PerformanceObserver = mockPerformanceObserver;

    component.showMonitor = true;
    component.ngOnInit();

    expect(mockPerformanceObserver).toHaveBeenCalled();
  });

  it('should handle performance observer errors gracefully', () => {
    const mockPerformanceObserver = jasmine.createSpy('PerformanceObserver').and.throwError('Observer error');
    (window as any).PerformanceObserver = mockPerformanceObserver;

    expect(() => {
      component.showMonitor = true;
      component.ngOnInit();
    }).not.toThrow();
  });
});