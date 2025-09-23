import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { ApiMonitoringService } from '../../../core/services/api-monitoring.service';
import { RealTimeAnalyticsService } from '../../../core/services/real-time-analytics.service';
import { SystemHealthComponent } from './system-health.component';

describe('SystemHealthComponent', () => {
    let component: SystemHealthComponent;
    let fixture: ComponentFixture<SystemHealthComponent>;
    let mockApiMonitoring: jasmine.SpyObj<ApiMonitoringService>;
    let mockRealTimeAnalytics: jasmine.SpyObj<RealTimeAnalyticsService>;
    let mockMessageService: jasmine.SpyObj<MessageService>;

    beforeEach(async () => {
        mockApiMonitoring = jasmine.createSpyObj('ApiMonitoringService', [
            'getSystemMetrics',
            'getApiEndpoints'
        ]);

        mockRealTimeAnalytics = jasmine.createSpyObj('RealTimeAnalyticsService', [
            'subscribe'
        ], {
            messages$: of()
        });

        mockMessageService = jasmine.createSpyObj('MessageService', [
            'add'
        ]);

        await TestBed.configureTestingModule({
            imports: [SystemHealthComponent],
            providers: [
                { provide: ApiMonitoringService, useValue: mockApiMonitoring },
                { provide: RealTimeAnalyticsService, useValue: mockRealTimeAnalytics },
                { provide: MessageService, useValue: mockMessageService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(SystemHealthComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should initialize monitoring on init', () => {
        component.ngOnInit();

        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('system-health');
        expect(mockRealTimeAnalytics.subscribe).toHaveBeenCalledWith('api-performance');
    });

    it('should calculate status correctly', () => {
        expect(component['calculateStatus'](50, 80)).toBe('healthy');
        expect(component['calculateStatus'](70, 80)).toBe('warning');
        expect(component['calculateStatus'](85, 80)).toBe('critical');
    });

    it('should format metric values correctly', () => {
        expect(component.formatMetricValue(75.5, '%')).toBe('75.5%');
        expect(component.formatMetricValue(1500, 'ms')).toBe('1500.0ms');
        expect(component.formatMetricValue(1234, '')).toBe('1,234');
    });

    it('should get correct status colors', () => {
        expect(component.getStatusColor('healthy')).toBe('bg-green-500');
        expect(component.getStatusColor('warning')).toBe('bg-yellow-500');
        expect(component.getStatusColor('critical')).toBe('bg-red-500');
        expect(component.getStatusColor('unknown')).toBe('bg-gray-500');
    });

    it('should calculate progress value correctly', () => {
        const metric = {
            name: 'CPU',
            value: 60,
            threshold: 80,
            status: 'healthy' as const,
            unit: '%'
        };

        expect(component.getProgressValue(metric)).toBe(75); // (60/80) * 100
    });

    it('should get correct method severity', () => {
        expect(component.getMethodSeverity('GET')).toBe('info');
        expect(component.getMethodSeverity('POST')).toBe('success');
        expect(component.getMethodSeverity('PUT')).toBe('warning');
        expect(component.getMethodSeverity('DELETE')).toBe('danger');
    });

    it('should get correct status severity', () => {
        expect(component.getStatusSeverity('healthy')).toBe('success');
        expect(component.getStatusSeverity('warning')).toBe('warning');
        expect(component.getStatusSeverity('critical')).toBe('danger');
    });

    it('should detect unacknowledged alerts correctly', () => {
        component.alerts = [
            {
                id: '1',
                type: 'system',
                severity: 'error',
                message: 'Test alert 1',
                timestamp: new Date(),
                acknowledged: false
            },
            {
                id: '2',
                type: 'performance',
                severity: 'warning',
                message: 'Test alert 2',
                timestamp: new Date(),
                acknowledged: true
            }
        ];

        expect(component.hasUnacknowledgedAlerts()).toBe(true);
        expect(component.getUnacknowledgedCount()).toBe(1);
    });

    it('should acknowledge alert correctly', () => {
        component.alerts = [
            {
                id: 'test-alert',
                type: 'system',
                severity: 'error',
                message: 'Test alert',
                timestamp: new Date(),
                acknowledged: false
            }
        ];

        component.acknowledgeAlert('test-alert');

        expect(component.alerts[0].acknowledged).toBe(true);
        expect(mockMessageService.add).toHaveBeenCalledWith({
            severity: 'success',
            summary: '警報已確認',
            detail: '警報已標記為已讀',
            life: 3000
        });
    });

    it('should acknowledge all alerts correctly', () => {
        component.alerts = [
            {
                id: '1',
                type: 'system',
                severity: 'error',
                message: 'Test alert 1',
                timestamp: new Date(),
                acknowledged: false
            },
            {
                id: '2',
                type: 'performance',
                severity: 'warning',
                message: 'Test alert 2',
                timestamp: new Date(),
                acknowledged: false
            }
        ];

        component.acknowledgeAllAlerts();

        expect(component.alerts.every(alert => alert.acknowledged)).toBe(true);
        expect(mockMessageService.add).toHaveBeenCalledWith({
            severity: 'success',
            summary: '所有警報已確認',
            detail: '所有警報已標記為已讀',
            life: 3000
        });
    });

    it('should clear acknowledged alerts correctly', () => {
        component.alerts = [
            {
                id: '1',
                type: 'system',
                severity: 'error',
                message: 'Test alert 1',
                timestamp: new Date(),
                acknowledged: true
            },
            {
                id: '2',
                type: 'performance',
                severity: 'warning',
                message: 'Test alert 2',
                timestamp: new Date(),
                acknowledged: false
            }
        ];

        component.clearAcknowledgedAlerts();

        expect(component.alerts.length).toBe(1);
        expect(component.alerts[0].id).toBe('2');
        expect(mockMessageService.add).toHaveBeenCalledWith({
            severity: 'info',
            summary: '警報已清除',
            detail: '已清除 1 個已讀警報',
            life: 3000
        });
    });

    it('should handle health messages correctly', () => {
        const mockMessage = {
            type: 'system_metrics',
            data: {
                cpu: 75,
                memory: 60
            }
        };

        component['handleHealthMessage'](mockMessage);

        expect(component.systemMetrics[0].value).toBe(75);
        expect(component.systemMetrics[1].value).toBe(60);
    });

    it('should add new alert correctly', () => {
        const initialAlertsCount = component.alerts.length;

        component['addAlert']({
            type: 'system',
            severity: 'error',
            message: 'Test alert message'
        });

        expect(component.alerts.length).toBe(initialAlertsCount + 1);
        expect(component.alerts[0].message).toBe('Test alert message');
        expect(component.alerts[0].acknowledged).toBe(false);
        expect(mockMessageService.add).toHaveBeenCalled();
    });

    it('should check for recent alerts correctly', () => {
        const recentAlert = {
            id: '1',
            type: 'system' as const,
            severity: 'error' as const,
            message: 'CPU usage high',
            timestamp: new Date(),
            acknowledged: false
        };

        const oldAlert = {
            id: '2',
            type: 'system' as const,
            severity: 'error' as const,
            message: 'Memory usage high',
            timestamp: new Date(Date.now() - 10 * 60 * 1000), // 10 minutes ago
            acknowledged: false
        };

        component.alerts = [recentAlert, oldAlert];

        expect(component['hasRecentAlert']('CPU')).toBe(true);
        expect(component['hasRecentAlert']('Memory')).toBe(false);
    });

    it('should format alert time correctly', () => {
        const testDate = new Date('2024-01-01T12:30:45');
        const formattedTime = component.formatAlertTime(testDate);

        expect(formattedTime).toContain('2024');
        expect(formattedTime).toContain('12:30:45');
    });

    it('should cleanup on destroy', () => {
        spyOn(component['destroy$'], 'next');
        spyOn(component['destroy$'], 'complete');

        component.ngOnDestroy();

        expect(component['destroy$'].next).toHaveBeenCalled();
        expect(component['destroy$'].complete).toHaveBeenCalled();
    });
});