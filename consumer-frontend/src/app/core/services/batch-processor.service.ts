import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { ObservabilityConfigService } from '../config/observability.config';
import { ApiService } from './api.service';
import { ObservabilityEvent } from './observability.service';

export interface BatchConfig {
  maxBatchSize: number;        // 預設: 50 個事件
  maxWaitTime: number;         // 預設: 30 秒 (毫秒)
  retryAttempts: number;       // 預設: 3 次
  backoffMultiplier: number;   // 預設: 2
  enableOfflineStorage: boolean; // 預設: true
  storageKey: string;          // localStorage key
  maxStorageSize: number;      // 最大儲存大小 (事件數量)
}

export interface BatchProcessorStatus {
  isProcessing: boolean;
  queueLength: number;
  lastProcessedAt?: Date;
  totalProcessed: number;
  totalFailed: number;
  isOnline: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BatchProcessorService {
  private config: BatchConfig;
  private eventQueue: ObservabilityEvent[] = [];
  private isProcessing = false;
  private destroy$ = new Subject<void>();
  private flushTimer?: any;
  private isOnline = navigator.onLine;
  
  // 統計資訊
  private totalProcessed = 0;
  private totalFailed = 0;
  private lastProcessedAt?: Date;
  
  // 狀態 Observable
  private statusSubject = new BehaviorSubject<BatchProcessorStatus>(this.getStatus());
  public status$: Observable<BatchProcessorStatus> = this.statusSubject.asObservable();

  constructor(
    private apiService: ApiService,
    private configService: ObservabilityConfigService
  ) {
    this.config = this.getDefaultConfig();
    this.initializeNetworkListeners();
    this.startPeriodicFlush();
    this.loadOfflineEvents();
  }

  /**
   * 配置批次處理器
   */
  configure(config: Partial<BatchConfig>): void {
    this.config = { ...this.config, ...config };
    this.restartPeriodicFlush();
  }

  /**
   * 添加事件到批次佇列
   */
  addEvent(event: ObservabilityEvent): void {
    this.eventQueue.push(event);
    this.updateStatus();

    // 如果達到批次大小，立即處理
    if (this.eventQueue.length >= this.config.maxBatchSize) {
      this.processBatch();
    }

    // 如果離線，儲存到本地儲存
    if (!this.isOnline && this.config.enableOfflineStorage) {
      this.saveToOfflineStorage();
    }
  }

  /**
   * 手動觸發批次處理
   */
  async processBatch(): Promise<void> {
    if (this.isProcessing || this.eventQueue.length === 0) {
      return;
    }

    this.isProcessing = true;
    this.updateStatus();

    const eventsToProcess = this.eventQueue.splice(0, this.config.maxBatchSize);
    
    try {
      await this.sendEventsWithRetry(eventsToProcess);
      this.totalProcessed += eventsToProcess.length;
      this.lastProcessedAt = new Date();
      
      // 清除已成功處理的離線事件
      if (this.config.enableOfflineStorage) {
        this.clearProcessedOfflineEvents(eventsToProcess.length);
      }
      
    } catch (error) {
      console.error('Batch processing failed after all retries:', error);
      this.totalFailed += eventsToProcess.length;
      
      // 如果在線但發送失敗，將事件放回佇列前端
      if (this.isOnline) {
        this.eventQueue.unshift(...eventsToProcess);
      } else {
        // 如果離線，儲存到本地儲存
        this.saveEventsToOfflineStorage(eventsToProcess);
      }
    } finally {
      this.isProcessing = false;
      this.updateStatus();
    }
  }

  /**
   * 強制刷新所有待處理事件
   */
  async flush(): Promise<void> {
    while (this.eventQueue.length > 0 && !this.isProcessing) {
      await this.processBatch();
    }
  }

  /**
   * 獲取當前狀態
   */
  getStatus(): BatchProcessorStatus {
    return {
      isProcessing: this.isProcessing,
      queueLength: this.eventQueue.length,
      lastProcessedAt: this.lastProcessedAt,
      totalProcessed: this.totalProcessed,
      totalFailed: this.totalFailed,
      isOnline: this.isOnline
    };
  }

  /**
   * 清空佇列 (用於測試)
   */
  clearQueue(): void {
    this.eventQueue = [];
    this.updateStatus();
  }

  /**
   * 重置統計資訊 (用於測試)
   */
  resetStats(): void {
    this.totalProcessed = 0;
    this.totalFailed = 0;
    this.lastProcessedAt = undefined;
    this.updateStatus();
  }

  /**
   * 銷毀服務
   */
  destroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }
  }

  private async sendEventsWithRetry(events: ObservabilityEvent[]): Promise<void> {
    let lastError: any;
    
    for (let attempt = 1; attempt <= this.config.retryAttempts; attempt++) {
      try {
        await this.sendEvents(events);
        return; // 成功，退出重試循環
      } catch (error) {
        lastError = error;
        console.warn(`Batch send attempt ${attempt} failed:`, error);
        
        if (attempt < this.config.retryAttempts) {
          // 指數退避延遲
          const delay = Math.pow(this.config.backoffMultiplier, attempt - 1) * 1000;
          await this.delay(delay);
        }
      }
    }
    
    throw lastError;
  }

  private async sendEvents(events: ObservabilityEvent[]): Promise<void> {
    if (!this.isOnline) {
      throw new Error('Network is offline');
    }

    try {
      // 使用配置服務獲取 API 端點
      const endpoints = this.configService.getApiEndpoints();
      await this.apiService.post(endpoints.analytics, events).toPromise();
    } catch (error) {
      // 檢查是否為網路錯誤
      if (this.isNetworkError(error)) {
        this.isOnline = false;
        this.updateStatus();
      }
      throw error;
    }
  }

  private isNetworkError(error: any): boolean {
    return error?.status === 0 || 
           error?.name === 'NetworkError' ||
           error?.message?.includes('Network');
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private initializeNetworkListeners(): void {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.updateStatus();
      this.processOfflineEvents();
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
      this.updateStatus();
    });
  }

  private startPeriodicFlush(): void {
    this.flushTimer = setInterval(() => {
      if (this.eventQueue.length > 0 && !this.isProcessing) {
        this.processBatch();
      }
    }, this.config.maxWaitTime);
  }

  private restartPeriodicFlush(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }
    this.startPeriodicFlush();
  }

  private updateStatus(): void {
    this.statusSubject.next(this.getStatus());
  }

  private getDefaultConfig(): BatchConfig {
    return {
      maxBatchSize: 50,
      maxWaitTime: 30000, // 30 seconds
      retryAttempts: 3,
      backoffMultiplier: 2,
      enableOfflineStorage: true,
      storageKey: 'observability_offline_events',
      maxStorageSize: 1000 // 最多儲存 1000 個事件
    };
  }

  // 離線儲存相關方法
  private saveToOfflineStorage(): void {
    if (!this.config.enableOfflineStorage) return;

    try {
      const existingEvents = this.getOfflineEvents();
      const allEvents = [...existingEvents, ...this.eventQueue];
      
      // 限制儲存大小
      const eventsToStore = allEvents.slice(-this.config.maxStorageSize);
      
      localStorage.setItem(this.config.storageKey, JSON.stringify(eventsToStore));
    } catch (error) {
      console.warn('Failed to save events to offline storage:', error);
    }
  }

  private saveEventsToOfflineStorage(events: ObservabilityEvent[]): void {
    if (!this.config.enableOfflineStorage) return;

    try {
      const existingEvents = this.getOfflineEvents();
      const allEvents = [...existingEvents, ...events];
      
      // 限制儲存大小
      const eventsToStore = allEvents.slice(-this.config.maxStorageSize);
      
      localStorage.setItem(this.config.storageKey, JSON.stringify(eventsToStore));
    } catch (error) {
      console.warn('Failed to save events to offline storage:', error);
    }
  }

  private getOfflineEvents(): ObservabilityEvent[] {
    if (!this.config.enableOfflineStorage) return [];

    try {
      const stored = localStorage.getItem(this.config.storageKey);
      return stored ? JSON.parse(stored) : [];
    } catch (error) {
      console.warn('Failed to load offline events:', error);
      return [];
    }
  }

  private loadOfflineEvents(): void {
    if (!this.config.enableOfflineStorage) return;

    const offlineEvents = this.getOfflineEvents();
    if (offlineEvents.length > 0) {
      this.eventQueue.unshift(...offlineEvents);
      this.updateStatus();
      
      // 清除已載入的離線事件
      localStorage.removeItem(this.config.storageKey);
    }
  }

  private clearProcessedOfflineEvents(processedCount: number): void {
    if (!this.config.enableOfflineStorage) return;

    try {
      const remainingEvents = this.getOfflineEvents().slice(processedCount);
      
      if (remainingEvents.length > 0) {
        localStorage.setItem(this.config.storageKey, JSON.stringify(remainingEvents));
      } else {
        localStorage.removeItem(this.config.storageKey);
      }
    } catch (error) {
      console.warn('Failed to clear processed offline events:', error);
    }
  }

  private async processOfflineEvents(): Promise<void> {
    // 當網路恢復時，處理離線事件
    if (this.isOnline && this.eventQueue.length > 0) {
      await this.flush();
    }
  }

  /**
   * 獲取離線儲存大小 (用於監控)
   */
  getOfflineStorageSize(): number {
    return this.getOfflineEvents().length;
  }

  /**
   * 清除所有離線儲存 (用於測試或重置)
   */
  clearOfflineStorage(): void {
    if (this.config.enableOfflineStorage) {
      localStorage.removeItem(this.config.storageKey);
    }
  }
}