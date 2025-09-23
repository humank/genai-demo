import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, firstValueFrom, fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ResilientHttpService } from './resilient-http.service';

export interface OfflineManagerConfig {
  enableOfflineMode: boolean;
  syncInterval: number;
  maxRetryAttempts: number;
  retryDelay: number;
  enableNotifications: boolean;
  storageQuotaWarningThreshold: number; // MB
}

export interface OfflineStatus {
  isOnline: boolean;
  isOfflineModeEnabled: boolean;
  lastSyncAt?: Date;
  pendingSyncItems: number;
  storageUsage: number; // MB
  syncInProgress: boolean;
  lastError?: string;
}

export interface SyncItem {
  id: string;
  type: 'analytics' | 'user_action' | 'performance' | 'error';
  data: any;
  timestamp: number;
  retryCount: number;
  priority: 'high' | 'medium' | 'low';
}

@Injectable({
  providedIn: 'root'
})
export class OfflineManagerService {
  private config: OfflineManagerConfig;
  private isOnline = navigator.onLine;
  private isOfflineModeEnabled = true;
  private syncInProgress = false;
  private lastSyncAt?: Date;
  private lastError?: string;
  
  // 同步佇列
  private syncQueue: SyncItem[] = [];
  private syncTimer?: any;
  
  // 狀態 Observable
  private statusSubject = new BehaviorSubject<OfflineStatus>(this.getStatus());
  public status$: Observable<OfflineStatus> = this.statusSubject.asObservable();
  
  // 銷毀 Subject
  private destroy$ = new Subject<void>();

  constructor(private resilientHttpService: ResilientHttpService) {
    this.config = this.getDefaultConfig();
    this.initializeNetworkMonitoring();
    this.loadSyncQueue();
    this.startPeriodicSync();
    this.monitorStorageUsage();
  }

  /**
   * 配置離線管理器
   */
  configure(config: Partial<OfflineManagerConfig>): void {
    this.config = { ...this.config, ...config };
    this.restartPeriodicSync();
  }

  /**
   * 啟用/停用離線模式
   */
  setOfflineModeEnabled(enabled: boolean): void {
    this.isOfflineModeEnabled = enabled;
    this.updateStatus();
    
    if (enabled && this.isOnline) {
      // 如果重新啟用且在線，立即同步
      this.syncNow();
    }
  }

  /**
   * 添加項目到同步佇列
   */
  addToSyncQueue(item: Omit<SyncItem, 'id' | 'timestamp' | 'retryCount'>): void {
    if (!this.isOfflineModeEnabled) {
      return;
    }

    const syncItem: SyncItem = {
      ...item,
      id: this.generateSyncId(),
      timestamp: Date.now(),
      retryCount: 0
    };

    this.syncQueue.push(syncItem);
    this.saveSyncQueue();
    this.updateStatus();

    // 如果在線且不在同步中，立即嘗試同步高優先級項目
    if (this.isOnline && !this.syncInProgress && item.priority === 'high') {
      this.syncNow();
    }
  }

  /**
   * 手動觸發同步
   */
  async syncNow(): Promise<void> {
    if (!this.isOnline || this.syncInProgress || this.syncQueue.length === 0) {
      return;
    }

    this.syncInProgress = true;
    this.lastError = undefined;
    this.updateStatus();

    try {
      await this.processSyncQueue();
      this.lastSyncAt = new Date();
    } catch (error) {
      this.lastError = error instanceof Error ? error.message : 'Unknown sync error';
      console.error('Sync failed:', error);
    } finally {
      this.syncInProgress = false;
      this.updateStatus();
    }
  }

  /**
   * 清除同步佇列
   */
  clearSyncQueue(): void {
    this.syncQueue = [];
    this.saveSyncQueue();
    this.updateStatus();
  }

  /**
   * 獲取特定類型的待同步項目數量
   */
  getPendingSyncCount(type?: SyncItem['type']): number {
    if (type) {
      return this.syncQueue.filter(item => item.type === type).length;
    }
    return this.syncQueue.length;
  }

  /**
   * 獲取當前狀態
   */
  getStatus(): OfflineStatus {
    return {
      isOnline: this.isOnline,
      isOfflineModeEnabled: this.isOfflineModeEnabled,
      lastSyncAt: this.lastSyncAt,
      pendingSyncItems: this.syncQueue.length,
      storageUsage: this.calculateStorageUsage(),
      syncInProgress: this.syncInProgress,
      lastError: this.lastError
    };
  }

  /**
   * 獲取儲存使用情況詳情
   */
  getStorageDetails(): { [key: string]: number } {
    const details: { [key: string]: number } = {};
    
    try {
      // 計算各種儲存的大小
      details['syncQueue'] = this.calculateItemSize(this.syncQueue);
      details['resilientHttp'] = this.calculateItemSize(
        JSON.parse(localStorage.getItem('resilient_http_offline_queue') || '[]')
      );
      details['observabilityEvents'] = this.calculateItemSize(
        JSON.parse(localStorage.getItem('observability_offline_events') || '[]')
      );
      
      // 計算總大小
      details['total'] = Object.values(details).reduce((sum, size) => sum + size, 0);
      
    } catch (error) {
      console.warn('Failed to calculate storage details:', error);
    }
    
    return details;
  }

  /**
   * 清理過期的同步項目
   */
  cleanupExpiredItems(maxAge: number = 7 * 24 * 60 * 60 * 1000): void {
    const now = Date.now();
    const initialCount = this.syncQueue.length;
    
    this.syncQueue = this.syncQueue.filter(item => {
      return (now - item.timestamp) < maxAge;
    });
    
    if (this.syncQueue.length !== initialCount) {
      this.saveSyncQueue();
      this.updateStatus();
      console.log(`Cleaned up ${initialCount - this.syncQueue.length} expired sync items`);
    }
  }

  /**
   * 銷毀服務
   */
  destroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    
    if (this.syncTimer) {
      clearInterval(this.syncTimer);
    }
  }

  private async processSyncQueue(): Promise<void> {
    // 按優先級排序
    const sortedQueue = [...this.syncQueue].sort((a, b) => {
      const priorityOrder = { high: 3, medium: 2, low: 1 };
      return priorityOrder[b.priority] - priorityOrder[a.priority];
    });

    const itemsToProcess = sortedQueue.slice(0, 10); // 一次處理最多 10 個項目
    const successfulItems: string[] = [];

    for (const item of itemsToProcess) {
      try {
        await this.syncItem(item);
        successfulItems.push(item.id);
      } catch (error) {
        console.warn(`Failed to sync item ${item.id}:`, error);
        
        // 增加重試次數
        item.retryCount++;
        
        // 如果重試次數超過限制，移除項目
        if (item.retryCount >= this.config.maxRetryAttempts) {
          console.error(`Removing item ${item.id} after ${item.retryCount} failed attempts`);
          successfulItems.push(item.id); // 標記為要移除
        }
      }
    }

    // 移除成功同步的項目
    if (successfulItems.length > 0) {
      this.syncQueue = this.syncQueue.filter(item => !successfulItems.includes(item.id));
      this.saveSyncQueue();
    }
  }

  private async syncItem(item: SyncItem): Promise<void> {
    switch (item.type) {
      case 'analytics':
        await firstValueFrom(this.resilientHttpService.post('/api/analytics/events', item.data, { isCritical: true }));
        break;
        
      case 'performance':
        await firstValueFrom(this.resilientHttpService.post('/api/analytics/performance', item.data, { isCritical: true }));
        break;
        
      case 'error':
        await firstValueFrom(this.resilientHttpService.post('/api/monitoring/events', item.data, { isCritical: true }));
        break;
        
      case 'user_action':
        await this.resilientHttpService.post('/api/analytics/events', item.data, { isCritical: false }).toPromise();
        break;
        
      default:
        throw new Error(`Unknown sync item type: ${item.type}`);
    }
  }

  private initializeNetworkMonitoring(): void {
    // 監聽網路狀態變化
    const online$ = fromEvent(window, 'online');
    const offline$ = fromEvent(window, 'offline');
    
    merge(online$, offline$).pipe(
      debounceTime(1000), // 防抖動
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const wasOnline = this.isOnline;
      this.isOnline = navigator.onLine;
      
      if (!wasOnline && this.isOnline) {
        // 從離線恢復到在線
        console.log('Network restored, starting sync...');
        this.showNotification('網路已恢復，正在同步資料...', 'info');
        this.syncNow();
      } else if (wasOnline && !this.isOnline) {
        // 從在線變為離線
        console.log('Network lost, enabling offline mode...');
        this.showNotification('網路連線中斷，已啟用離線模式', 'warning');
      }
      
      this.updateStatus();
    });
  }

  private startPeriodicSync(): void {
    this.syncTimer = setInterval(() => {
      if (this.isOnline && this.isOfflineModeEnabled && this.syncQueue.length > 0) {
        this.syncNow();
      }
    }, this.config.syncInterval);
  }

  private restartPeriodicSync(): void {
    if (this.syncTimer) {
      clearInterval(this.syncTimer);
    }
    this.startPeriodicSync();
  }

  private monitorStorageUsage(): void {
    setInterval(() => {
      const usage = this.calculateStorageUsage();
      
      if (usage > this.config.storageQuotaWarningThreshold) {
        console.warn(`Storage usage (${usage.toFixed(2)}MB) exceeds warning threshold`);
        this.showNotification(
          `儲存空間使用量過高 (${usage.toFixed(1)}MB)，建議清理資料`,
          'warning'
        );
        
        // 自動清理過期項目
        this.cleanupExpiredItems();
      }
    }, 60000); // 每分鐘檢查一次
  }

  private calculateStorageUsage(): number {
    try {
      let totalSize = 0;
      
      // 計算 localStorage 使用量
      for (let key in localStorage) {
        if (localStorage.hasOwnProperty(key)) {
          totalSize += localStorage[key].length;
        }
      }
      
      // 轉換為 MB
      return totalSize / (1024 * 1024);
    } catch (error) {
      console.warn('Failed to calculate storage usage:', error);
      return 0;
    }
  }

  private calculateItemSize(item: any): number {
    try {
      return JSON.stringify(item).length / (1024 * 1024); // MB
    } catch (error) {
      return 0;
    }
  }

  private saveSyncQueue(): void {
    try {
      localStorage.setItem('offline_manager_sync_queue', JSON.stringify(this.syncQueue));
    } catch (error) {
      console.error('Failed to save sync queue:', error);
    }
  }

  private loadSyncQueue(): void {
    try {
      const stored = localStorage.getItem('offline_manager_sync_queue');
      if (stored) {
        this.syncQueue = JSON.parse(stored);
        this.updateStatus();
      }
    } catch (error) {
      console.warn('Failed to load sync queue:', error);
      this.syncQueue = [];
    }
  }

  private updateStatus(): void {
    this.statusSubject.next(this.getStatus());
  }

  private generateSyncId(): string {
    return `sync_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private showNotification(message: string, type: 'info' | 'warning' | 'error'): void {
    if (!this.config.enableNotifications) {
      return;
    }

    // 簡單的通知實現，可以後續整合更完整的通知系統
    console.log(`[${type.toUpperCase()}] ${message}`);
    
    // 如果瀏覽器支援通知 API
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('電商應用', {
        body: message,
        icon: '/favicon.ico'
      });
    }
  }

  private getDefaultConfig(): OfflineManagerConfig {
    return {
      enableOfflineMode: true,
      syncInterval: 30000, // 30 seconds
      maxRetryAttempts: 3,
      retryDelay: 5000, // 5 seconds
      enableNotifications: true,
      storageQuotaWarningThreshold: 10 // 10 MB
    };
  }
}