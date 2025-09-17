import { TestBed } from '@angular/core/testing';
import { ObservabilityConfigService } from '../config/observability.config';
import { EnhancedWebVitalsService } from './enhanced-web-vitals.service';
import { ObservabilityService } from './observability.service';

describe('EnhancedWebVitalsService', () => {
    let service: EnhancedWebVitalsService;
    let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
    let mockConfigService: jasmine.SpyObj<ObservabilityConfigService>;

    beforeEach(() => {
        const observabilitySpy = jasmine.createSpyObj('ObservabilityService', ['trackPerformanceMetric']);
        const configSpy = jasmine.createSpyObj('ObservabilityConfigService', [
            'isPerformanceMonitoringEnabled',
            'isDevelopment'
        ]);

        TestBed.configureTestingModule({
            providers: [
                EnhancedWebVitalsService,
                { provide: ObservabilityService, useValue: observabilitySpy },
                { provide: ObservabilityConfigService, useValue: configSpy }
            ]
        });

        service = TestBed.inject(EnhancedWebVitalsService);
        mockObservabilityService = TestBed.inject(ObservabilityService) as jasmine.SpyObj<ObservabilityService>;
        mockConfigService = TestBed.inject(ObservabilityConfigService) as jasmine.SpyObj<ObservabilityConfigService>;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('Metric Rating', () => {
        it('should correctly identify good LCP values', () => {
            expect(service.isGoodMetric('LCP', 2000)).toBe(true);
            expect(service.isGoodMetric('LCP', 2500)).toBe(true);
            expect(service.isGoodMetric('LCP', 3000)).toBe(false);
        });

        it('should correctly identify good FID values', () => {
            expect(service.isGoodMetric('FID', 50)).toBe(true);
            expect(service.isGoodMetric('FID', 100)).toBe(true);
            expect(service.isGoodMetric('FID', 150)).toBe(false);
        });

        it('should correctly identify good CLS values', () => {
            expect(service.isGoodMetric('CLS', 0.05)).toBe(true);
            expect(service.isGoodMetric('CLS', 0.1)).toBe(true);
            expect(service.isGoodMetric('CLS', 0.15)).toBe(false);
        });

        it('should correctly identify poor metrics', () => {
            expect(service.isPoorMetric('LCP', 5000)).toBe(true);
            expect(service.isPoorMetric('FID', 400)).toBe(true);
            expect(service.isPoorMetric('CLS', 0.3)).toBe(true);
        });

        it('should correctly identify metrics that need improvement', () => {
            expect(service.needsImprovement('LCP', 3000)).toBe(true);
            expect(service.needsImprovement('FID', 200)).toBe(true);
            expect(service.needsImprovement('CLS', 0.2)).toBe(true);
        });
    });

    describe('Metric Rating Classification', () => {
        it('should return correct rating for LCP values', () => {
            expect(service.getMetricRating('LCP', 2000)).toBe('good');
            expect(service.getMetricRating('LCP', 3000)).toBe('needs-improvement');
            expect(service.getMetricRating('LCP', 5000)).toBe('poor');
        });

        it('should return correct rating for FID values', () => {
            expect(service.getMetricRating('FID', 50)).toBe('good');
            expect(service.getMetricRating('FID', 200)).toBe('needs-improvement');
            expect(service.getMetricRating('FID', 400)).toBe('poor');
        });

        it('should return correct rating for CLS values', () => {
            expect(service.getMetricRating('CLS', 0.05)).toBe('good');
            expect(service.getMetricRating('CLS', 0.2)).toBe('needs-improvement');
            expect(service.getMetricRating('CLS', 0.3)).toBe('poor');
        });
    });

    describe('Performance Report Generation', () => {
        it('should generate performance report with recommendations', async () => {
            // Mock getCurrentMetrics to return test data
            spyOn(service, 'getCurrentMetrics').and.returnValue(Promise.resolve({
                lcp: { name: 'LCP', value: 5000, rating: 'poor', delta: 0, id: 'test', navigationType: 'navigate' },
                fid: { name: 'FID', value: 400, rating: 'poor', delta: 0, id: 'test', navigationType: 'navigate' },
                cls: { name: 'CLS', value: 0.3, rating: 'poor', delta: 0, id: 'test', navigationType: 'navigate' },
                fcp: { name: 'FCP', value: 3500, rating: 'poor', delta: 0, id: 'test', navigationType: 'navigate' },
                ttfb: { name: 'TTFB', value: 2000, rating: 'poor', delta: 0, id: 'test', navigationType: 'navigate' },
                timestamp: Date.now(),
                page: '/test',
                userAgent: 'test-agent'
            }));

            const report = await service.generatePerformanceReport();

            expect(report.summary.poorMetrics).toBe(5);
            expect(report.summary.overallScore).toBe(0);
            expect(report.recommendations.length).toBeGreaterThan(0);
            expect(report.recommendations).toContain(jasmine.stringMatching(/優化 LCP/));
            expect(report.recommendations).toContain(jasmine.stringMatching(/優化 FID/));
            expect(report.recommendations).toContain(jasmine.stringMatching(/優化 CLS/));
        });

        it('should generate report with good metrics', async () => {
            // Mock getCurrentMetrics to return good test data
            spyOn(service, 'getCurrentMetrics').and.returnValue(Promise.resolve({
                lcp: { name: 'LCP', value: 2000, rating: 'good', delta: 0, id: 'test', navigationType: 'navigate' },
                fid: { name: 'FID', value: 50, rating: 'good', delta: 0, id: 'test', navigationType: 'navigate' },
                cls: { name: 'CLS', value: 0.05, rating: 'good', delta: 0, id: 'test', navigationType: 'navigate' },
                timestamp: Date.now(),
                page: '/test',
                userAgent: 'test-agent'
            }));

            const report = await service.generatePerformanceReport();

            expect(report.summary.goodMetrics).toBe(3);
            expect(report.summary.overallScore).toBe(100);
            expect(report.recommendations.length).toBe(0);
        });
    });

    describe('Metrics Management', () => {
        it('should clear all metrics', () => {
            service.clearMetrics();
            expect(service.getAllMetrics().size).toBe(0);
        });

        it('should get specific metric', () => {
            // This would require mocking internal state
            // For now, just test that the method exists and returns undefined for non-existent metrics
            expect(service.getMetric('NON_EXISTENT')).toBeUndefined();
        });
    });
});