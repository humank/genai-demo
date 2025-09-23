import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject, throwError, timer } from 'rxjs';
import { catchError, finalize, retry, switchMap, takeUntil, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface RetryConfig {
  maxAttempts: number;
  baseDelay: number;
  maxDelay: number;
  backoffMultiplier: number;
  retryableErrors: number[];
}

export interface CircuitBreakerConfig {
  failureThreshold: number;
  recoveryTimeout: number;
  monitoringPeriod: number;
}

export interface OfflineConfig {
  enableOfflineStorage: boolean;
  storageKey: string;
  maxStorageSize: number;
  syncOnReconnect: boolean;
}

export interface ResilientHttpConfig {
  retry: RetryConfig;
  circuitBreaker: CircuitBreakerConfig;
  offline: OfflineConfig;
  timeout: number;
}

export interface RequestMetadata {
  id: string;
  url: string;
  method: string;
  timestamp: number;
  retryCount: number;
  isCritical: boolean;
}

export interface FailedRequest {
  metadata: RequestMetadata;
  data?: any;
  params?: any;
  error: any;
  timestamp: number;
}

export enum CircuitBreakerState {
  CLOSED = 'CLOSED',
  OPEN = 'OPEN',
  HALF_OPEN = 'HALF_OPEN'
}

export interface NetworkStatus {
  isOnline: boolean;
  lastOnlineAt?: Date;
  lastOfflineAt?: Date;
  connectionQuality: 'good' | 'poor' | 'offline';
}

export interface ResilientHttpStatus {
  networkStatus: NetworkStatus;
  circuitBreakerState: CircuitBreakerState;
  failureCount: number;
  successCount: number;
  pendingRequests: number;
  offlineQueueSize: number;
  lastError?: any;
}

@Injectable({
  providedIn: 'root'
})
export class ResilientHttpService {
  private readonly baseUrl = environment.apiUrl;
  private config: ResilientHttpConfig;
  
  // Circuit Breaker 狀態
  private circuitBreakerState = CircuitBreakerState.CLOSED;
  private failureCount = 0;
  private successCount = 0;
  private lastFailureTime = 0;
  private nextAttemptTime = 0;
  
  // 網路狀態
  private isOnline = navigator.onLine;
  private connectionQuality: 'good' | 'poor' | 'offline' = 'good';
  private lastOnlineAt?: Date;
  private lastOfflineAt?: Date;
  
  // 離線佇列
  private offlineQueue: FailedRequest[] = [];
  private pendingRequests = 0;
  private lastError?: any;
  
  // 狀態 Observable
  private statusSubject = new BehaviorSubject<ResilientHttpStatus>(this.getStatus());
  public status$: Observable<ResilientHttpStatus> = this.statusSubject.asObservable();
  
  // 銷毀 Subject
  private destroy$ = new Subject<void>();

  constructor(private http: HttpClient) {
    this.config = this.getDefaultConfig();
    this.initializeNetworkMonitoring();
    this.loadOfflineQueue();
    this.startConnectionQualityMonitoring();
  }

  /**
   * 配置韌性 HTTP 服務
   */
  configure(config: Partial<ResilientHttpConfig>): void {
    this.config = {
      ...this.config,
      ...config,
      retry: { ...this.config.retry, ...config.retry },
      circuitBreaker: { ...this.config.circuitBreaker, ...config.circuitBreaker },
      offline: { ...this.config.offline, ...config.offline }
    };
  }

  /**
   * 韌性 GET 請求
   */
  get<T>(endpoint: string, params?: any, options?: { isCritical?: boolean }): Observable<T> {
    const metadata: RequestMetadata = {
      id: this.generateRequestId(),
      url: endpoint,
      method: 'GET',
      timestamp: Date.now(),
      retryCount: 0,
      isCritical: options?.isCritical || false
    };

    return this.executeResilientRequest<T>(() => {
      let httpParams = new HttpParams();
      
      if (params) {
        Object.keys(params).forEach(key => {
          if (params[key] !== null && params[key] !== undefined) {
            httpParams = httpParams.set(key, params[key].toString());
          }
        });
      }

      return this.http.get<T>(`${this.baseUrl}${endpoint}`, { params: httpParams });
    }, metadata, undefined, params);
  }

  /**
   * 韌性 POST 請求
   */
  post<T>(endpoint: string, data: any, options?: { isCritical?: boolean }): Observable<T> {
    const metadata: RequestMetadata = {
      id: this.generateRequestId(),
      url: endpoint,
      method: 'POST',
      timestamp: Date.now(),
      retryCount: 0,
      isCritical: options?.isCritical || false
    };

    return this.executeResilientRequest<T>(() => {
      return this.http.post<T>(`${this.baseUrl}${endpoint}`, data);
    }, metadata, data);
  }

  /**
   * 韌性 PUT 請求
   */
  put<T>(endpoint: string, data: any, options?: { isCritical?: boolean }): Observable<T> {
    const metadata: RequestMetadata = {
      id: this.generateRequestId(),
      url: endpoint,
      method: 'PUT',
      timestamp: Date.now(),
      retryCount: 0,
      isCritical: options?.isCritical || false
    };

    return this.executeResilientRequest<T>(() => {
      return this.http.put<T>(`${this.baseUrl}${endpoint}`, data);
    }, metadata, data);
  }

  /**
   * 韌性 DELETE 請求
   */
  delete<T>(endpoint: string, options?: { isCritical?: boolean }): Observable<T> {
    const metadata: RequestMetadata = {
      id: this.generateRequestId(),
      url: endpoint,
      method: 'DELETE',
      timestamp: Date.now(),
      retryCount: 0,
      isCritical: options?.isCritical || false
    };

    return this.executeResilientRequest<T>(() => {
      return this.http.delete<T>(`${this.baseUrl}${endpoint}`);
    }, metadata);
  }

  /**
   * 韌性 PATCH 請求
   */
  patch<T>(endpoint: string, data: any, options?: { isCritical?: boolean }): Observable<T> {
    const metadata: RequestMetadata = {
      id: this.generateRequestId(),
      url: endpoint,
      method: 'PATCH',
      timestamp: Date.now(),
      retryCount: 0,
      isCritical: options?.isCritical || false
    };

    return this.executeResilientRequest<T>(() => {
      return this.http.patch<T>(`${this.baseUrl}${endpoint}`, data);
    }, metadata, data);
  }

  /**
   * 手動重試離線佇列中的請求
   */
  async retryOfflineRequests(): Promise<void> {
    if (!this.isOnline || this.offlineQueue.length === 0) {
      return;
    }

    const requestsToRetry = [...this.offlineQueue];
    this.offlineQueue = [];
    this.saveOfflineQueue();

    for (const failedRequest of requestsToRetry) {
      try {
        await this.retryFailedRequest(failedRequest);
      } catch (error) {
        console.warn('Failed to retry offline request:', error);
        // 如果重試失敗，重新加入佇列
        this.addToOfflineQueue(failedRequest);
      }
    }

    this.updateStatus();
  }

  /**
   * 清除離線佇列
   */
  clearOfflineQueue(): void {
    this.offlineQueue = [];
    this.saveOfflineQueue();
    this.updateStatus();
  }

  /**
   * 獲取當前狀態
   */
  getStatus(): ResilientHttpStatus {
    return {
      networkStatus: {
        isOnline: this.isOnline,
        lastOnlineAt: this.lastOnlineAt,
        lastOfflineAt: this.lastOfflineAt,
        connectionQuality: this.connectionQuality
      },
      circuitBreakerState: this.circuitBreakerState,
      failureCount: this.failureCount,
      successCount: this.successCount,
      pendingRequests: this.pendingRequests,
      offlineQueueSize: this.offlineQueue.length,
      lastError: this.lastError
    };
  }

  /**
   * 重置斷路器狀態
   */
  resetCircuitBreaker(): void {
    this.circuitBreakerState = CircuitBreakerState.CLOSED;
    this.failureCount = 0;
    this.lastFailureTime = 0;
    this.nextAttemptTime = 0;
    this.updateStatus();
  }

  /**
   * 銷毀服務
   */
  destroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private executeResilientRequest<T>(
    requestFn: () => Observable<T>,
    metadata: RequestMetadata,
    data?: any,
    params?: any
  ): Observable<T> {
    // 檢查斷路器狀態
    if (!this.canExecuteRequest()) {
      return this.handleCircuitBreakerOpen<T>(metadata, data, params);
    }

    // 檢查網路狀態
    if (!this.isOnline) {
      return this.handleOfflineRequest<T>(metadata, data, params);
    }

    this.pendingRequests++;
    this.updateStatus();

    return requestFn().pipe(
      // 超時處理
      switchMap(response => {
        return timer(this.config.timeout).pipe(
          switchMap(() => throwError(() => new Error('Request timeout'))),
          takeUntil(of(response).pipe(tap(() => {})))
        );
      }),
      
      // 重試機制
      retry({
        count: this.config.retry.maxAttempts,
        delay: (error, retryCount) => {
          metadata.retryCount = retryCount;
          
          if (!this.isRetryableError(error)) {
            throw error;
          }

          const delay = Math.min(
            this.config.retry.baseDelay * Math.pow(this.config.retry.backoffMultiplier, retryCount - 1),
            this.config.retry.maxDelay
          );

          console.warn(`Request ${metadata.id} failed, retrying in ${delay}ms (attempt ${retryCount}/${this.config.retry.maxAttempts}):`, error);
          
          return timer(delay);
        }
      }),
      
      // 成功處理
      tap(() => {
        this.onRequestSuccess();
      }),
      
      // 錯誤處理
      catchError(error => {
        this.onRequestFailure(error, metadata, data, params);
        return throwError(() => error);
      }),
      
      // 清理
      finalize(() => {
        this.pendingRequests--;
        this.updateStatus();
      }),
      
      takeUntil(this.destroy$)
    );
  }

  private canExecuteRequest(): boolean {
    const now = Date.now();

    switch (this.circuitBreakerState) {
      case CircuitBreakerState.CLOSED:
        return true;
        
      case CircuitBreakerState.OPEN:
        if (now >= this.nextAttemptTime) {
          this.circuitBreakerState = CircuitBreakerState.HALF_OPEN;
          this.updateStatus();
          return true;
        }
        return false;
        
      case CircuitBreakerState.HALF_OPEN:
        return true;
        
      default:
        return false;
    }
  }

  private handleCircuitBreakerOpen<T>(
    metadata: RequestMetadata,
    data?: any,
    params?: any
  ): Observable<T> {
    const error = new Error('Circuit breaker is open');
    
    // 如果是關鍵請求，加入離線佇列
    if (metadata.isCritical && this.config.offline.enableOfflineStorage) {
      this.addToOfflineQueue({
        metadata,
        data,
        params,
        error,
        timestamp: Date.now()
      });
    }

    return throwError(() => error);
  }

  private handleOfflineRequest<T>(
    metadata: RequestMetadata,
    data?: any,
    params?: any
  ): Observable<T> {
    const error = new Error('Network is offline');
    
    // 加入離線佇列
    if (this.config.offline.enableOfflineStorage) {
      this.addToOfflineQueue({
        metadata,
        data,
        params,
        error,
        timestamp: Date.now()
      });
    }

    return throwError(() => error);
  }

  private onRequestSuccess(): void {
    this.successCount++;
    this.lastError = undefined;

    // 斷路器狀態管理
    if (this.circuitBreakerState === CircuitBreakerState.HALF_OPEN) {
      this.circuitBreakerState = CircuitBreakerState.CLOSED;
      this.failureCount = 0;
    }

    this.updateStatus();
  }

  private onRequestFailure(error: any, metadata: RequestMetadata, data?: any, params?: any): void {
    this.failureCount++;
    this.lastError = error;
    this.lastFailureTime = Date.now();

    // 檢查是否需要開啟斷路器
    if (this.failureCount >= this.config.circuitBreaker.failureThreshold) {
      this.circuitBreakerState = CircuitBreakerState.OPEN;
      this.nextAttemptTime = Date.now() + this.config.circuitBreaker.recoveryTimeout;
    }

    // 如果是網路錯誤，更新網路狀態
    if (this.isNetworkError(error)) {
      this.isOnline = false;
      this.lastOfflineAt = new Date();
      this.connectionQuality = 'offline';
    }

    // 如果是關鍵請求，加入離線佇列
    if (metadata.isCritical && this.config.offline.enableOfflineStorage) {
      this.addToOfflineQueue({
        metadata,
        data,
        params,
        error,
        timestamp: Date.now()
      });
    }

    this.updateStatus();
  }

  private isRetryableError(error: any): boolean {
    if (error?.status) {
      return this.config.retry.retryableErrors.includes(error.status);
    }
    
    // 網路錯誤通常是可重試的
    return this.isNetworkError(error);
  }

  private isNetworkError(error: any): boolean {
    return error?.status === 0 || 
           error?.name === 'NetworkError' ||
           error?.message?.includes('Network') ||
           error?.message?.includes('timeout') ||
           error?.message?.includes('offline');
  }

  private addToOfflineQueue(failedRequest: FailedRequest): void {
    // 限制佇列大小
    if (this.offlineQueue.length >= this.config.offline.maxStorageSize) {
      this.offlineQueue.shift(); // 移除最舊的請求
    }

    this.offlineQueue.push(failedRequest);
    this.saveOfflineQueue();
  }

  private async retryFailedRequest(failedRequest: FailedRequest): Promise<any> {
    const { metadata, data, params } = failedRequest;
    
    // 重新建立請求
    const requestFn = () => {
      const url = `${this.baseUrl}${metadata.url}`;
      
      switch (metadata.method.toLowerCase()) {
        case 'get':
          let httpParams = new HttpParams();
          if (params) {
            Object.keys(params).forEach(key => {
              if (params[key] !== null && params[key] !== undefined) {
                httpParams = httpParams.set(key, params[key].toString());
              }
            });
          }
          return this.http.get(url, { params: httpParams });
          
        case 'post':
          return this.http.post(url, data);
          
        case 'put':
          return this.http.put(url, data);
          
        case 'delete':
          return this.http.delete(url);
          
        case 'patch':
          return this.http.patch(url, data);
          
        default:
          throw new Error(`Unsupported method: ${metadata.method}`);
      }
    };

    return requestFn().toPromise();
  }

  private initializeNetworkMonitoring(): void {
    // 監聽網路狀態變化
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.lastOnlineAt = new Date();
      this.connectionQuality = 'good';
      this.updateStatus();
      
      // 自動重試離線請求
      if (this.config.offline.syncOnReconnect) {
        this.retryOfflineRequests();
      }
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
      this.lastOfflineAt = new Date();
      this.connectionQuality = 'offline';
      this.updateStatus();
    });
  }

  private startConnectionQualityMonitoring(): void {
    // 定期檢查連線品質
    setInterval(() => {
      if (this.isOnline) {
        this.checkConnectionQuality();
      }
    }, this.config.circuitBreaker.monitoringPeriod);
  }

  private checkConnectionQuality(): void {
    // 基於最近的錯誤率判斷連線品質
    const recentFailureRate = this.failureCount / (this.failureCount + this.successCount);
    
    if (recentFailureRate > 0.5) {
      this.connectionQuality = 'poor';
    } else if (recentFailureRate > 0.2) {
      this.connectionQuality = 'poor';
    } else {
      this.connectionQuality = 'good';
    }
    
    this.updateStatus();
  }

  private saveOfflineQueue(): void {
    if (!this.config.offline.enableOfflineStorage) return;

    try {
      localStorage.setItem(this.config.offline.storageKey, JSON.stringify(this.offlineQueue));
    } catch (error) {
      console.warn('Failed to save offline queue:', error);
    }
  }

  private loadOfflineQueue(): void {
    if (!this.config.offline.enableOfflineStorage) return;

    try {
      const stored = localStorage.getItem(this.config.offline.storageKey);
      if (stored) {
        this.offlineQueue = JSON.parse(stored);
        this.updateStatus();
      }
    } catch (error) {
      console.warn('Failed to load offline queue:', error);
      this.offlineQueue = [];
    }
  }

  private updateStatus(): void {
    this.statusSubject.next(this.getStatus());
  }

  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private getDefaultConfig(): ResilientHttpConfig {
    return {
      retry: {
        maxAttempts: 3,
        baseDelay: 1000,
        maxDelay: 10000,
        backoffMultiplier: 2,
        retryableErrors: [0, 408, 429, 500, 502, 503, 504]
      },
      circuitBreaker: {
        failureThreshold: 5,
        recoveryTimeout: 30000, // 30 seconds
        monitoringPeriod: 60000  // 1 minute
      },
      offline: {
        enableOfflineStorage: true,
        storageKey: 'resilient_http_offline_queue',
        maxStorageSize: 100,
        syncOnReconnect: true
      },
      timeout: 30000 // 30 seconds
    };
  }
}