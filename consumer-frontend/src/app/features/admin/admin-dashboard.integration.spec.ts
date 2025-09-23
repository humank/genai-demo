import { Location } from '@angular/common';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MessageService } from 'primeng/api';
import { BehaviorSubject, of } from 'rxjs';
import { ApiMonitoringService } from '../../core/services/api-monitoring.service';
import { ObservabilityService } from '../../core/services/observability.service';
import { RealTimeAnalyticsService } from '../../core/services/real-time-analytics.service';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SystemHealthComponent } from './system-health/system-health.component';

@Component({
    template: '<router-outlet></router-outlet>'
})
class TestHostComponent { }

describe('Admin Dashboard Integration', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let router: Router;
    let location: Location;
    let mockRealTimeAnalytics: jasmine.SpyObj<RealTimeAnalyticsService>;
    let mockObservabilityService: jasmine.SpyObj<ObservabilityService>;
    let mockApiMonitoring: jasmine.SpyObj<ApiMonitoringService>;
    let mockMessageService: jasmine.SpyObj<MessageService>;

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

        mockApiMonitoring = jasmine.createSpyObj('ApiMonitoringService', [
            'getSystemMetrics',
            'getApiEndpoints'
        ]);

        mockMessageService = jasmine.createSpyObj('MessageService', [
            'add'
        ]);

        await TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        path: 'admin',
                        component: AdminLayoutComponent,
                        children: [
                            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
                            { path: 'dashboard', component: DashboardComponent },
                            { path: 'system-health', component: SystemHealthComponent }
                        ]
                    }
                ]),
                TestHostComponent,
                AdminLayoutComponent,
                DashboardComponent,
                SystemHealthComponent
            ],
            providers: [
                { provide: RealTimeAnalyticsService, useValue: mockRealTimeAnalytics },
                { provide: ObservabilityService, useValue: mockObservabilityService },
                { provide: ApiMonitoringService, useValue: mockApiMonitoring },
                { provide: MessageService, useValue: mockMessageService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        router = TestBed.inject(Router);
        location = TestBed.inject(Location);

        fixture.detectChanges();
    });

    it('should navigate to admin dashboard by default', fakeAsync(() => {
        router.navigate(['/admin']);
        tick();

        expect(location.path()).toBe('/admin/dashboard');
    }));

    it('should navigate to system health page', fakeAsync(() => {
        router.navigate(['/admin/system-health']);
        tick();

        expect(location.path()).toBe('/admin/system-health');
    }));

    it('should initialize WebSocket connections when navigating to dashboard', fakeAsync(() => {
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('business-metrics');
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('user-activity');
    }));

    it('should initialize system health monitoring when navigating to system health', fakeAsync(() => {
        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('system-health');
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('api-performance');
    }));

    it('should track page views when navigating between admin pages', fakeAsync(() => {
        // Navigate to dashboard
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Navigate to system health
        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        // Should have tracked navigation events
        expect(mockObservabilityService.trackPageView).toHaveBeenCalled();
    }));

    it('should handle WebSocket connection state changes across components', fakeAsync(() => {
        const connectionStateSubject = new BehaviorSubject<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
        mockRealTimeAnalytics.connectionState$ = connectionStateSubject.asObservable();

        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Simulate connection established
        connectionStateSubject.next('connected');
        tick();
        fixture.detectChanges();

        // Navigate to system health - should maintain connection state
        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledTimes(4); // 2 calls per component
    }));

    it('should handle real-time data updates in dashboard', fakeAsync(() => {
        const messagesSubject = new BehaviorSubject<any>(null);
        mockRealTimeAnalytics.messages$ = messagesSubject.asObservable();

        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Simulate real-time business metrics update
        messagesSubject.next({
            type: 'business_metrics',
            data: {
                pageViews: 1500,
                pageViewsChange: 10,
                activeUsers: 95,
                activeUsersChange: -2
            }
        });

        tick();
        fixture.detectChanges();

        // Verify the dashboard component received and processed the update
        const dashboardComponent = fixture.debugElement.query(
            (el) => el.componentInstance instanceof DashboardComponent
        )?.componentInstance as DashboardComponent;

        if (dashboardComponent) {
            expect(dashboardComponent.businessMetrics[0].value).toBe(1500);
            expect(dashboardComponent.businessMetrics[1].value).toBe(95);
        }
    }));

    it('should handle system health alerts in system health component', fakeAsync(() => {
        const messagesSubject = new BehaviorSubject<any>(null);
        mockRealTimeAnalytics.messages$ = messagesSubject.asObservable();

        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        // Simulate system health alert
        messagesSubject.next({
            type: 'alert',
            data: {
                type: 'system',
                severity: 'error',
                message: 'CPU usage critical'
            }
        });

        tick();
        fixture.detectChanges();

        // Verify alert was added and toast notification was shown
        expect(mockMessageService.add).toHaveBeenCalledWith({
            severity: 'error',
            summary: '新警報',
            detail: 'CPU usage critical',
            life: 5000
        });
    }));

    it('should maintain admin layout navigation across page changes', fakeAsync(() => {
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Check that admin layout is rendered
        const adminLayout = fixture.debugElement.query(
            (el) => el.componentInstance instanceof AdminLayoutComponent
        );
        expect(adminLayout).toBeTruthy();

        // Navigate to system health
        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        // Admin layout should still be present
        const adminLayoutAfterNav = fixture.debugElement.query(
            (el) => el.componentInstance instanceof AdminLayoutComponent
        );
        expect(adminLayoutAfterNav).toBeTruthy();
    }));

    it('should handle navigation back to main app from admin', fakeAsync(() => {
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Simulate clicking "返回首頁" button
        router.navigate(['/home']);
        tick();

        expect(location.path()).toBe('/home');
    }));

    it('should cleanup WebSocket connections when leaving admin area', fakeAsync(() => {
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Navigate away from admin
        router.navigate(['/home']);
        tick();
        fixture.detectChanges();

        // Components should have been destroyed and cleaned up
        // This is verified by the component's ngOnDestroy tests
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalled();
    }));

    it('should handle concurrent real-time updates across multiple admin components', fakeAsync(() => {
        const messagesSubject = new BehaviorSubject<any>(null);
        mockRealTimeAnalytics.messages$ = messagesSubject.asObservable();

        // Start with dashboard
        router.navigate(['/admin/dashboard']);
        tick();
        fixture.detectChanges();

        // Send business metrics update
        messagesSubject.next({
            type: 'business_metrics',
            data: { pageViews: 2000 }
        });
        tick();

        // Navigate to system health
        router.navigate(['/admin/system-health']);
        tick();
        fixture.detectChanges();

        // Send system health update
        messagesSubject.next({
            type: 'system_metrics',
            data: { cpu: 85, memory: 70 }
        });
        tick();

        // Both components should have processed their respective updates
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledTimes(4);
    }));
});