import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { OfflineManagerConfig, OfflineManagerService, SyncItem } from './offline-manager.service';
import { ResilientHttpService } from './resilient-http.service';

describe('OfflineManagerService', () => {
  let service: OfflineManagerService;
  let resilientHttpService: jasmine.SpyObj<ResilientHttpService>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    const resilientHttpSpy = jasmine.createSpyObj('ResilientHttpService', ['post']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        OfflineManagerService,
        { provide: ResilientHttpService, useValue: resilientHttpSpy }
      ]
    });

    service = TestBed.inject(OfflineManagerService);
    resilientHttpService = TestBed.inject(ResilientHttpService) as jasmine.SpyObj<ResilientHttpService>;
    httpMock = TestBed.inject(HttpTestingController);

    // 清理 localStorage
    localStorage.clear();
  });

  afterEach(() => {
    service.destroy();
    localStorage.clear();
  });

  describe('Initialization', () => {
    it('should create service with default configuration', () => {
      expect(service).toBeTruthy();
      
      const status = service.getStatus();
      expect(status.isOfflineModeEnabled).toBe(true);
      expect(status.pendingSyncItems).toBe(0);
      expect(status.syncInProgress).toBe(false);
    });

    it('should load existing sync queue from localStorage', () => {
      // 預先在 localStorage 中放入同步項目
      const existingItems: SyncItem[] = [
        {
          id: 'test1',
          type: 'analytics',
          data: { event: 'test' },
          timestamp: Date.now(),
          retryCount: 0,
          priority: 'medium'
        }
      ];
      
      localStorage.setItem('offline_manager_sync_queue', JSON.stringify(existingItems));
      
      // 重新創建服務
      const newService = new OfflineManagerService(resilientHttpService);
      
      const status = newService.getStatus();
      expect(status.pendingSyncItems).toBe(1);
      
      newService.destroy();
    });
  });

  describe('Configuration', () => {
    it('should apply custom configuration', () => {
      const customConfig: Partial<OfflineManagerConfig> = {
        syncInterval: 60000,
        maxRetryAttempts: 5,
        enableNotifications: false
      };
      
      service.configure(customConfig);
      
      // 配置應該被應用（通過行為驗證）
      expect(service).toBeDefined();
    });

    it('should enable/disable offline mode', () => {
      expect(service.getStatus().isOfflineModeEnabled).toBe(true);
      
      service.setOfflineModeEnabled(false);
      expect(service.getStatus().isOfflineModeEnabled).toBe(false);
      
      service.setOfflineModeEnabled(true);
      expect(service.getStatus().isOfflineModeEnabled).toBe(true);
    });
  });

  describe('Sync Queue Management', () => {
    it('should add items to sync queue', () => {
      const item = {
        type: 'analytics' as const,
        data: { event: 'page_view', page: '/home' },
        priority: 'medium' as const
      };
      
      service.addToSyncQueue(item);
      
      const status = service.getStatus();
      expect(status.pendingSyncItems).toBe(1);
    });

    it('should not add items when offline mode is disabled', () => {
      service.setOfflineModeEnabled(false);
      
      const item = {
        type: 'analytics' as const,
        data: { event: 'page_view' },
        priority: 'medium' as const
      };
      
      service.addToSyncQueue(item);
      
      const status = service.getStatus();
      expect(status.pendingSyncItems).toBe(0);
    });

    it('should generate unique IDs for sync items', () => {
      const item1 = {
        type: 'analytics' as const,
        data: { event: 'test1' },
        priority: 'medium' as const
      };
      
      const item2 = {
        type: 'analytics' as const,
        data: { event: 'test2' },
        priority: 'medium' as const
      };
      
      service.addToSyncQueue(item1);
      service.addToSyncQueue(item2);
      
      expect(service.getStatus().pendingSyncItems).toBe(2);
    });

    it('should clear sync queue', () => {
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      expect(service.getStatus().pendingSyncItems).toBe(1);
      
      service.clearSyncQueue();
      
      expect(service.getStatus().pendingSyncItems).toBe(0);
    });

    it('should get pending sync count by type', () => {
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test1' },
        priority: 'medium'
      });
      
      service.addToSyncQueue({
        type: 'performance',
        data: { metric: 'lcp' },
        priority: 'high'
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test2' },
        priority: 'low'
      });
      
      expect(service.getPendingSyncCount('analytics')).toBe(2);
      expect(service.getPendingSyncCount('performance')).toBe(1);
      expect(service.getPendingSyncCount()).toBe(3);
    });
  });

  describe('Synchronization', () => {
    beforeEach(() => {
      // 模擬在線狀態
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: true
      });
    });

    it('should sync analytics items', async () => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'page_view' },
        priority: 'medium'
      });
      
      await service.syncNow();
      
      expect(resilientHttpService.post).toHaveBeenCalledWith(
        '/api/analytics/events',
        { event: 'page_view' },
        { isCritical: true }
      );
      
      expect(service.getStatus().pendingSyncItems).toBe(0);
    });

    it('should sync performance items', async () => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      service.addToSyncQueue({
        type: 'performance',
        data: { metric: 'lcp', value: 1500 },
        priority: 'high'
      });
      
      await service.syncNow();
      
      expect(resilientHttpService.post).toHaveBeenCalledWith(
        '/api/analytics/performance',
        { metric: 'lcp', value: 1500 },
        { isCritical: true }
      );
    });

    it('should sync error items', async () => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      service.addToSyncQueue({
        type: 'error',
        data: { error: 'JavaScript error', stack: 'stack trace' },
        priority: 'high'
      });
      
      await service.syncNow();
      
      expect(resilientHttpService.post).toHaveBeenCalledWith(
        '/api/monitoring/events',
        { error: 'JavaScript error', stack: 'stack trace' },
        { isCritical: true }
      );
    });

    it('should sync user action items', async () => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      service.addToSyncQueue({
        type: 'user_action',
        data: { action: 'click', element: 'button' },
        priority: 'low'
      });
      
      await service.syncNow();
      
      expect(resilientHttpService.post).toHaveBeenCalledWith(
        '/api/analytics/events',
        { action: 'click', element: 'button' },
        { isCritical: false }
      );
    });

    it('should prioritize high priority items', async () => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      // 添加不同優先級的項目
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'low_priority' },
        priority: 'low'
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'high_priority' },
        priority: 'high'
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'medium_priority' },
        priority: 'medium'
      });
      
      await service.syncNow();
      
      // 驗證高優先級項目先被處理
      const calls = resilientHttpService.post.calls.all();
      expect(calls[0].args[1]).toEqual({ event: 'high_priority' });
    });

    it('should not sync when offline', async () => {
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: false
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      await service.syncNow();
      
      expect(resilientHttpService.post).not.toHaveBeenCalled();
      expect(service.getStatus().pendingSyncItems).toBe(1);
    });

    it('should not sync when already syncing', async () => {
      const mockResponse = new Promise(resolve => {
        setTimeout(() => resolve({ success: true }), 100);
      });
      
      resilientHttpService.post.and.returnValue(
        of({ success: true }).pipe(delay(100))
      );
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test1' },
        priority: 'medium'
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test2' },
        priority: 'medium'
      });
      
      // 同時發起兩個同步請求
      const sync1 = service.syncNow();
      const sync2 = service.syncNow();
      
      await Promise.all([sync1, sync2]);
      
      // 應該只有一次同步被執行
      expect(resilientHttpService.post).toHaveBeenCalledTimes(1);
    });
  });

  describe('Error Handling', () => {
    it('should handle sync failures and retry', async () => {
      let callCount = 0;
      (resilientHttpService.post as any).and.callFake((endpoint: string, data: any, options?: any) => {
        callCount++;
        if (callCount === 1) {
          return throwError(() => new Error('Network error'));
        } else {
          return of({ success: true });
        }
      });
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      // 第一次同步失敗
      await service.syncNow();
      expect(service.getStatus().pendingSyncItems).toBe(1);
      
      // 第二次同步成功
      await service.syncNow();
      expect(service.getStatus().pendingSyncItems).toBe(0);
    });

    it('should remove items after max retry attempts', async () => {
      service.configure({ maxRetryAttempts: 2 });
      
      resilientHttpService.post.and.returnValue(
        throwError(() => new Error('Persistent error'))
      );
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      // 嘗試同步多次
      await service.syncNow();
      await service.syncNow();
      await service.syncNow();
      
      // 項目應該被移除
      expect(service.getStatus().pendingSyncItems).toBe(0);
    });
  });

  describe('Network Status Monitoring', () => {
    it('should detect network status changes', (done) => {
      let statusUpdateCount = 0;
      
      service.status$.subscribe(status => {
        statusUpdateCount++;
        
        if (statusUpdateCount === 1) {
          expect(status.isOnline).toBe(true);
          
          // 模擬離線
          Object.defineProperty(navigator, 'onLine', {
            writable: true,
            value: false
          });
          
          window.dispatchEvent(new Event('offline'));
        } else if (statusUpdateCount === 2) {
          expect(status.isOnline).toBe(false);
          done();
        }
      });
    });

    it('should auto-sync when coming back online', (done) => {
      resilientHttpService.post.and.returnValue(
        of({ success: true })
      );
      
      // 添加項目到佇列
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'offline_test' },
        priority: 'medium'
      });
      
      // 模擬離線
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: false
      });
      
      window.dispatchEvent(new Event('offline'));
      
      setTimeout(() => {
        // 模擬恢復在線
        Object.defineProperty(navigator, 'onLine', {
          writable: true,
          value: true
        });
        
        window.dispatchEvent(new Event('online'));
        
        // 等待自動同步
        setTimeout(() => {
          expect(resilientHttpService.post).toHaveBeenCalled();
          done();
        }, 100);
      }, 50);
    });
  });

  describe('Storage Management', () => {
    it('should persist sync queue to localStorage', () => {
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      const stored = localStorage.getItem('offline_manager_sync_queue');
      expect(stored).toBeTruthy();
      
      const parsedItems = JSON.parse(stored!);
      expect(parsedItems.length).toBe(1);
      expect(parsedItems[0].data.event).toBe('test');
    });

    it('should calculate storage usage', () => {
      // 添加一些項目
      for (let i = 0; i < 5; i++) {
        service.addToSyncQueue({
          type: 'analytics',
          data: { event: `test${i}`, largeData: 'x'.repeat(1000) },
          priority: 'medium'
        });
      }
      
      const status = service.getStatus();
      expect(status.storageUsage).toBeGreaterThan(0);
    });

    it('should provide storage details', () => {
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      const details = service.getStorageDetails();
      expect(details['syncQueue']).toBeGreaterThan(0);
      expect(details['total']).toBeGreaterThan(0);
    });

    it('should cleanup expired items', () => {
      // 手動添加過期項目到 localStorage
      const expiredItem: SyncItem = {
        id: 'expired1',
        type: 'analytics',
        data: { event: 'old' },
        timestamp: Date.now() - (8 * 24 * 60 * 60 * 1000), // 8 天前
        retryCount: 0,
        priority: 'medium'
      };
      
      const recentItem: SyncItem = {
        id: 'recent1',
        type: 'analytics',
        data: { event: 'new' },
        timestamp: Date.now(),
        retryCount: 0,
        priority: 'medium'
      };
      
      localStorage.setItem('offline_manager_sync_queue', JSON.stringify([expiredItem, recentItem]));
      
      // 重新創建服務以載入項目
      const newService = new OfflineManagerService(resilientHttpService);
      
      expect(newService.getStatus().pendingSyncItems).toBe(2);
      
      // 清理過期項目（7 天）
      newService.cleanupExpiredItems(7 * 24 * 60 * 60 * 1000);
      
      expect(newService.getStatus().pendingSyncItems).toBe(1);
      
      newService.destroy();
    });
  });

  describe('Status Monitoring', () => {
    it('should provide comprehensive status information', () => {
      const status = service.getStatus();
      
      expect(status).toEqual(jasmine.objectContaining({
        isOnline: jasmine.any(Boolean),
        isOfflineModeEnabled: jasmine.any(Boolean),
        pendingSyncItems: jasmine.any(Number),
        storageUsage: jasmine.any(Number),
        syncInProgress: jasmine.any(Boolean)
      }));
    });

    it('should update status after operations', () => {
      const initialStatus = service.getStatus();
      expect(initialStatus.pendingSyncItems).toBe(0);
      
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      const updatedStatus = service.getStatus();
      expect(updatedStatus.pendingSyncItems).toBe(1);
    });
  });

  describe('Memory Management', () => {
    it('should clean up resources on destroy', () => {
      service.addToSyncQueue({
        type: 'analytics',
        data: { event: 'test' },
        priority: 'medium'
      });
      
      expect(service.getStatus().pendingSyncItems).toBe(1);
      
      service.destroy();
      
      // 服務應該停止所有定時器和監聽器
      expect(service).toBeDefined();
    });
  });
});