import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { ObservabilityService } from '../../../core/services/observability.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
    let component: DashboardComponent;
    let fixture: ComponentFixture<DashboardComponent>;
    let mockRealTimeAnalytics: jasmine.SpyObj<RealTimeAnalyticsService>;
    let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;

    beforeEach(async () => {
        const connectionStateSubject = new BehaviorSubject<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected');

        mockRealTimeAnalytics = jasmine.createSpyObj('RealTimeAnalyticsService', [
            'subscribe',
            'disconnect',
            'connect'
        ], {
            connectionState$: connectionStateSubject.asObservable(),
            messages$: of()
        });

        mockObservabilityService = jasmine.createSpyObj('ObservabilityService', [
            'trackPageView',
            'trackUserAction'
        ]);

        await TestBed.configureTestingModule({
            imports: [DashboardComponent],
            providers: [
                { provide: RealTimeAnalyticsService, useValue: mockRealTimeAnalytics },
                { provide: ObservabilityService, useValue: mockObservabilityService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DashboardComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should initialize WebSocket connection on init', () => {
        component.ngOnInit();

        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('business-metrics');
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('user-activity');
    });

    it('should update connection status when WebSocket state changes', () => {
        const connectionStateSubject = new BehaviorSubject<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
        mockRealTimeAnalytics.connectionState$ = connectionStateSubject.asObservable();

        component.ngOnInit();

        expect(component.connectionStatus).toBe('connecting');

        connectionStateSubject.next('connected');
        expect(component.connectionStatus).toBe('connected');
    });

    it('should format metric values correctly', () => {
        expect(component.formatMetricValue(1234, '')).toBe('1,234');
        expect(component.formatMetricValue(3.14159, '%')).toBe('3.1%');
        expect(component.formatMetricValue(156.789, '$')).toBe('$156.79');
    });

    it('should calculate trend correctly', () => {
        expect(component['calculateTrend'](5)).toBe('up');
        expect(component['calculateTrend'](-5)).toBe('down');
        expect(component['calculateTrend'](1)).toBe('stable');
    });

    it('should get correct connection status class', () => {
        component.connectionStatus = 'connected';
        expect(component.getConnectionStatusClass()).toBe('bg-green-500');

        component.connectionStatus = 'error';
        expect(component.getConnectionStatusClass()).toBe('bg-red-500');
    });

    it('should handle reconnect correctly', () => {
        component.reconnect();

        expect(mockRealTimeAnalytics.disconnect).toHaveBeenCalled();

        // Verify connect is called after timeout
        setTimeout(() => {
            expect(mockRealTimeAnalytics.connect).toHaveBeenCalled();
        }, 1100);
    });

    it('should initialize business metrics with default values', () => {
        component.ngOnInit();

        expect(component.businessMetrics.length).toBe(4);
        expect(component.businessMetrics[0].name).toBe('頁面瀏覽量');
        expect(component.businessMetrics[1].name).toBe('活躍用戶');
        expect(component.businessMetrics[2].name).toBe('轉換率');
        expect(component.businessMetrics[3].name).toBe('平均訂單價值');
    });

    it('should format time correctly', () => {
        const testDate = new Date('2024-01-01T12:30:45');
        const formattedTime = component.formatTime(testDate);

        expect(formattedTime).toContain('12:30:45');
    });

    it('should get correct activity severity', () => {
        expect(component.getActivitySeverity('success')).toBe('success');
        expect(component.getActivitySeverity('error')).toBe('danger');
        expect(component.getActivitySeverity('warning')).toBe('warning');
        expect(component.getActivitySeverity('unknown')).toBe('info');
    });

    it('should handle real-time messages correctly', () => {
        const mockMessage = {
            type: 'business_metrics',
            data: {
                pageViews: 1500,
                pageViewsChange: 10,
                activeUsers: 95,
                activeUsersChange: -2
            }
        };

        component['handleRealtimeMessage'](mockMessage);

        expect(component.businessMetrics[0].value).toBe(1500);
        expect(component.businessMetrics[0].change).toBe(10);
        expect(component.businessMetrics[0].trend).toBe('up');

        expect(component.businessMetrics[1].value).toBe(95);
        expect(component.businessMetrics[1].change).toBe(-2);
        expect(component.businessMetrics[1].trend).toBe('down');
    });

    it('should add user activity correctly', () => {
        const mockActivity = {
            userId: 'user123',
            action: '商品點擊',
            page: '/products',
            status: 'success'
        };

        component['addUserActivity'](mockActivity);

        expect(component.recentActivities.length).toBe(1);
        expect(component.recentActivities[0].userId).toBe('user123');
        expect(component.recentActivities[0].action).toBe('商品點擊');
    });

    it('should limit recent activities to 50 items', () => {
        // Add 60 activities
        for (let i = 0; i < 60; i++) {
            component['addUserActivity']({
                userId: `user${i}`,
                action: 'test',
                page: '/test',
                status: 'success'
            });
        }

        expect(component.recentActivities.length).toBe(50);
    });

    it('should cleanup on destroy', () => {
        spyOn(component['destroy$'], 'next');
        spyOn(component['destroy$'], 'complete');

        component.ngOnDestroy();

        expect(component['destroy$'].next).toHaveBeenCalled();
        expect(component['destroy$'].complete).toHaveBeenCalled();
    });
});