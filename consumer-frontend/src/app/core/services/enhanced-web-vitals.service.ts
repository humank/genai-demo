import { Injectable } from '@angular/core';
import { getCLS, getFCP, getFID, getLCP, getTTFB, onCLS, onFCP, onFID, onLCP, onTTFB } from 'web-vitals';
import { ObservabilityConfigService } from '../config/observability.config';
import { ObservabilityService } from './observability.service';

export interface WebVitalsMetric {
    name: string;
    value: number;
    rating: 'good' | 'needs-improvement' | 'poor';
    delta: number;
    id: string;
    navigationType: string;
}

export interface WebVitalsReport {
    lcp?: WebVitalsMetric;
    fid?: WebVitalsMetric;
    cls?: WebVitalsMetric;
    fcp?: WebVitalsMetric;
    ttfb?: WebVitalsMetric;
    timestamp: number;
    page: string;
    userAgent: string;
}

@Injectable({
    providedIn: 'root'
})
export class EnhancedWebVitalsService {
    private metrics: Map<string, WebVitalsMetric> = new Map();
    private isInitialized = false;

    constructor(
        private observabilityService: ObservabilityService,
        private configService: ObservabilityConfigService
    ) {
        if (this.configService.isPerformanceMonitoringEnabled()) {
            this.initializeWebVitals();
        }
    }

    /**
     * 初始化 Web Vitals 監控
     */
    private initializeWebVitals(): void {
        if (this.isInitialized || typeof window === 'undefined') {
            return;
        }

        this.isInitialized = true;

        // 監控 LCP (Largest Contentful Paint)
        onLCP((metric) => {
            this.handleMetric('LCP', metric);
        });

        // 監控 FID (First Input Delay)
        onFID((metric) => {
            this.handleMetric('FID', metric);
        });

        // 監控 CLS (Cumulative Layout Shift)
        onCLS((metric) => {
            this.handleMetric('CLS', metric);
        });

        // 監控 FCP (First Contentful Paint)
        onFCP((metric) => {
            this.handleMetric('FCP', metric);
        });

        // 監控 TTFB (Time to First Byte)
        onTTFB((metric) => {
            this.handleMetric('TTFB', metric);
        });

        console.log('Enhanced Web Vitals monitoring initialized');
    }

    /**
     * 處理 Web Vitals 指標
     */
    private handleMetric(name: string, metric: any): void {
        const webVitalsMetric: WebVitalsMetric = {
            name,
            value: metric.value,
            rating: metric.rating,
            delta: metric.delta,
            id: metric.id,
            navigationType: metric.navigationType || 'unknown'
        };

        this.metrics.set(name, webVitalsMetric);

        // 發送到可觀測性服務
        this.sendMetricToObservability(webVitalsMetric);

        // 如果是開發環境，輸出到控制台
        if (this.configService.isDevelopment()) {
            console.log(`Web Vitals - ${name}:`, webVitalsMetric);
        }
    }

    /**
     * 發送指標到可觀測性服務
     */
    private sendMetricToObservability(metric: WebVitalsMetric): void {
        this.observabilityService.trackPerformanceMetric({
            metricType: metric.name.toLowerCase(),
            value: metric.value,
            page: window.location.pathname,
            metadata: {
                rating: metric.rating,
                delta: metric.delta,
                id: metric.id,
                navigationType: metric.navigationType,
                timestamp: Date.now(),
                userAgent: navigator.userAgent,
                viewport: {
                    width: window.innerWidth,
                    height: window.innerHeight
                },
                connection: this.getConnectionInfo()
            }
        });
    }

    /**
     * 獲取網路連線資訊
     */
    private getConnectionInfo(): any {
        const connection = (navigator as any).connection;
        if (!connection) return null;

        return {
            effectiveType: connection.effectiveType,
            downlink: connection.downlink,
            rtt: connection.rtt,
            saveData: connection.saveData
        };
    }

    /**
     * 手動獲取當前所有 Web Vitals 指標
     */
    public async getCurrentMetrics(): Promise<WebVitalsReport> {
        const report: WebVitalsReport = {
            timestamp: Date.now(),
            page: window.location.pathname,
            userAgent: navigator.userAgent
        };

        try {
            // 獲取 LCP
            getLCP((metric) => {
                report.lcp = {
                    name: 'LCP',
                    value: metric.value,
                    rating: metric.rating,
                    delta: metric.delta,
                    id: metric.id,
                    navigationType: metric.navigationType || 'unknown'
                };
            });

            // 獲取 FID (只有在有用戶互動時才有值)
            getFID((metric) => {
                report.fid = {
                    name: 'FID',
                    value: metric.value,
                    rating: metric.rating,
                    delta: metric.delta,
                    id: metric.id,
                    navigationType: metric.navigationType || 'unknown'
                };
            });

            // 獲取 CLS
            getCLS((metric) => {
                report.cls = {
                    name: 'CLS',
                    value: metric.value,
                    rating: metric.rating,
                    delta: metric.delta,
                    id: metric.id,
                    navigationType: metric.navigationType || 'unknown'
                };
            });

            // 獲取 FCP
            getFCP((metric) => {
                report.fcp = {
                    name: 'FCP',
                    value: metric.value,
                    rating: metric.rating,
                    delta: metric.delta,
                    id: metric.id,
                    navigationType: metric.navigationType || 'unknown'
                };
            });

            // 獲取 TTFB
            getTTFB((metric) => {
                report.ttfb = {
                    name: 'TTFB',
                    value: metric.value,
                    rating: metric.rating,
                    delta: metric.delta,
                    id: metric.id,
                    navigationType: metric.navigationType || 'unknown'
                };
            });

        } catch (error) {
            console.warn('Error getting Web Vitals metrics:', error);
        }

        return report;
    }

    /**
     * 獲取特定指標
     */
    public getMetric(name: string): WebVitalsMetric | undefined {
        return this.metrics.get(name.toUpperCase());
    }

    /**
     * 獲取所有已收集的指標
     */
    public getAllMetrics(): Map<string, WebVitalsMetric> {
        return new Map(this.metrics);
    }

    /**
     * 清除所有指標
     */
    public clearMetrics(): void {
        this.metrics.clear();
    }

    /**
     * 檢查指標是否符合良好標準
     */
    public isGoodMetric(name: string, value: number): boolean {
        switch (name.toUpperCase()) {
            case 'LCP':
                return value <= 2500;
            case 'FID':
                return value <= 100;
            case 'CLS':
                return value <= 0.1;
            case 'FCP':
                return value <= 1800;
            case 'TTFB':
                return value <= 800;
            default:
                return false;
        }
    }

    /**
     * 檢查指標是否需要改善
     */
    public needsImprovement(name: string, value: number): boolean {
        switch (name.toUpperCase()) {
            case 'LCP':
                return value > 2500 && value <= 4000;
            case 'FID':
                return value > 100 && value <= 300;
            case 'CLS':
                return value > 0.1 && value <= 0.25;
            case 'FCP':
                return value > 1800 && value <= 3000;
            case 'TTFB':
                return value > 800 && value <= 1800;
            default:
                return false;
        }
    }

    /**
     * 檢查指標是否表現不佳
     */
    public isPoorMetric(name: string, value: number): boolean {
        switch (name.toUpperCase()) {
            case 'LCP':
                return value > 4000;
            case 'FID':
                return value > 300;
            case 'CLS':
                return value > 0.25;
            case 'FCP':
                return value > 3000;
            case 'TTFB':
                return value > 1800;
            default:
                return false;
        }
    }

    /**
     * 獲取指標評級
     */
    public getMetricRating(name: string, value: number): 'good' | 'needs-improvement' | 'poor' {
        if (this.isGoodMetric(name, value)) {
            return 'good';
        } else if (this.needsImprovement(name, value)) {
            return 'needs-improvement';
        } else {
            return 'poor';
        }
    }

    /**
     * 生成效能報告
     */
    public async generatePerformanceReport(): Promise<{
        summary: any;
        details: WebVitalsReport;
        recommendations: string[];
    }> {
        const details = await this.getCurrentMetrics();
        const recommendations: string[] = [];

        // 分析指標並提供建議
        if (details.lcp && details.lcp.value > 2500) {
            recommendations.push('優化 LCP: 考慮優化圖片載入、減少伺服器響應時間');
        }

        if (details.fid && details.fid.value > 100) {
            recommendations.push('優化 FID: 減少 JavaScript 執行時間、優化事件處理器');
        }

        if (details.cls && details.cls.value > 0.1) {
            recommendations.push('優化 CLS: 為圖片和廣告設定尺寸、避免動態插入內容');
        }

        if (details.fcp && details.fcp.value > 1800) {
            recommendations.push('優化 FCP: 優化關鍵資源載入、減少阻塞渲染的資源');
        }

        if (details.ttfb && details.ttfb.value > 800) {
            recommendations.push('優化 TTFB: 優化伺服器響應時間、使用 CDN');
        }

        const summary = {
            totalMetrics: Object.keys(details).filter(key => key !== 'timestamp' && key !== 'page' && key !== 'userAgent').length,
            goodMetrics: 0,
            needsImprovementMetrics: 0,
            poorMetrics: 0,
            overallScore: 0
        };

        // 計算總體評分
        let totalScore = 0;
        let metricCount = 0;

        ['lcp', 'fid', 'cls', 'fcp', 'ttfb'].forEach(metricName => {
            const metric = (details as any)[metricName];
            if (metric) {
                metricCount++;
                const rating = this.getMetricRating(metricName, metric.value);
                if (rating === 'good') {
                    summary.goodMetrics++;
                    totalScore += 100;
                } else if (rating === 'needs-improvement') {
                    summary.needsImprovementMetrics++;
                    totalScore += 50;
                } else {
                    summary.poorMetrics++;
                    totalScore += 0;
                }
            }
        });

        summary.overallScore = metricCount > 0 ? Math.round(totalScore / metricCount) : 0;

        return {
            summary,
            details,
            recommendations
        };
    }
}