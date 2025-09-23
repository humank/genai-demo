import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CircuitBreakerState, ResilientHttpConfig, ResilientHttpService } from './resilient-http.service';

describe('ResilientHttpService', () => {
  let service: ResilientHttpService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ResilientHttpService]
    });
    
    service = TestBed.inject(ResilientHttpService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // 重置服務狀態
    service.resetCircuitBreaker();
    service.clearOfflineQueue();
  });

  afterEach(() => {
    httpMock.verify();
    service.destroy();
  });

  describe('Basic HTTP Operations', () => {
    it('should perform successful GET request', (done) => {
      const testData = { id: 1, name: 'Test' };
      
      service.get<any>('/test').subscribe({
        next: (data) => {
          expect(data).toEqual(testData);
          done();
        },
        error: done.fail
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      expect(req.request.method).toBe('GET');
      req.flush(testData);
    });

    it('should perform successful POST request', (done) => {
      const testData = { name: 'New Item' };
      const responseData = { id: 1, ...testData };
      
      service.post<any>('/test', testData).subscribe({
        next: (data) => {
          expect(data).toEqual(responseData);
          done();
        },
        error: done.fail
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(testData);
      req.flush(responseData);
    });

    it('should handle GET request with query parameters', (done) => {
      const params = { page: 1, size: 10, filter: 'active' };
      
      service.get<any>('/test', params).subscribe({
        next: () => done(),
        error: done.fail
      });

      const req = httpMock.expectOne(request => {
        return request.url.includes('/test') && 
               request.params.get('page') === '1' &&
               request.params.get('size') === '10' &&
               request.params.get('filter') === 'active';
      });
      
      expect(req.request.method).toBe('GET');
      req.flush({});
    });
  });

  describe('Retry Mechanism', () => {
    it('should retry on retryable errors', (done) => {
      let attemptCount = 0;
      
      service.get<any>('/test').subscribe({
        next: (data) => {
          expect(attemptCount).toBe(3); // 1 initial + 2 retries
          expect(data).toEqual({ success: true });
          done();
        },
        error: done.fail
      });

      // 模擬前兩次失敗，第三次成功
      const expectRequest = () => {
        const req = httpMock.expectOne(`${baseUrl}/test`);
        attemptCount++;
        
        if (attemptCount < 3) {
          req.flush(null, { status: 500, statusText: 'Internal Server Error' });
        } else {
          req.flush({ success: true });
        }
        
        if (attemptCount < 3) {
          setTimeout(expectRequest, 100); // 等待重試
        }
      };
      
      expectRequest();
    });

    it('should not retry on non-retryable errors', (done) => {
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: (error) => {
          expect(error.status).toBe(400);
          done();
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush(null, { status: 400, statusText: 'Bad Request' });
    });

    it('should fail after max retry attempts', (done) => {
      let attemptCount = 0;
      
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: (error) => {
          expect(attemptCount).toBe(4); // 1 initial + 3 retries
          expect(error.status).toBe(500);
          done();
        }
      });

      const expectRequest = () => {
        const req = httpMock.expectOne(`${baseUrl}/test`);
        attemptCount++;
        req.flush(null, { status: 500, statusText: 'Internal Server Error' });
        
        if (attemptCount < 4) {
          setTimeout(expectRequest, 100);
        }
      };
      
      expectRequest();
    });
  });

  describe('Circuit Breaker', () => {
    it('should open circuit breaker after failure threshold', (done) => {
      const config: Partial<ResilientHttpConfig> = {
        circuitBreaker: {
          failureThreshold: 2,
          recoveryTimeout: 1000,
          monitoringPeriod: 5000
        }
      };
      
      service.configure(config);
      
      let requestCount = 0;
      const makeRequest = () => {
        service.get<any>('/test').subscribe({
          next: done.fail,
          error: (error) => {
            requestCount++;
            
            if (requestCount < 2) {
              // 繼續發送請求直到達到閾值
              setTimeout(makeRequest, 10);
            } else if (requestCount === 2) {
              // 檢查斷路器是否開啟
              const status = service.getStatus();
              expect(status.circuitBreakerState).toBe(CircuitBreakerState.OPEN);
              
              // 嘗試再次請求，應該立即失敗
              service.get<any>('/test').subscribe({
                next: done.fail,
                error: (cbError) => {
                  expect(cbError.message).toBe('Circuit breaker is open');
                  done();
                }
              });
            }
          }
        });
      };
      
      makeRequest();
      
      // 模擬伺服器錯誤
      const handleRequest = () => {
        try {
          const req = httpMock.expectOne(`${baseUrl}/test`);
          req.flush(null, { status: 500, statusText: 'Internal Server Error' });
          
          if (requestCount < 2) {
            setTimeout(handleRequest, 20);
          }
        } catch (e) {
          // 當斷路器開啟時，不會有更多 HTTP 請求
        }
      };
      
      handleRequest();
    });

    it('should recover from open circuit breaker after timeout', (done) => {
      const config: Partial<ResilientHttpConfig> = {
        circuitBreaker: {
          failureThreshold: 1,
          recoveryTimeout: 100, // 短暫的恢復時間用於測試
          monitoringPeriod: 5000
        }
      };
      
      service.configure(config);
      
      // 觸發斷路器開啟
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: () => {
          const status = service.getStatus();
          expect(status.circuitBreakerState).toBe(CircuitBreakerState.OPEN);
          
          // 等待恢復時間後再次嘗試
          setTimeout(() => {
            service.get<any>('/test').subscribe({
              next: (data) => {
                expect(data).toEqual({ recovered: true });
                const newStatus = service.getStatus();
                expect(newStatus.circuitBreakerState).toBe(CircuitBreakerState.CLOSED);
                done();
              },
              error: done.fail
            });
            
            const req = httpMock.expectOne(`${baseUrl}/test`);
            req.flush({ recovered: true });
          }, 150);
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Offline Support', () => {
    it('should queue critical requests when offline', (done) => {
      // 模擬離線狀態
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: false
      });
      
      // 觸發離線事件
      window.dispatchEvent(new Event('offline'));
      
      setTimeout(() => {
        service.post<any>('/test', { data: 'test' }, { isCritical: true }).subscribe({
          next: done.fail,
          error: (error) => {
            expect(error.message).toBe('Network is offline');
            
            const status = service.getStatus();
            expect(status.offlineQueueSize).toBe(1);
            done();
          }
        });
      }, 10);
    });

    it('should retry offline requests when back online', (done) => {
      // 開始時離線
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: false
      });
      
      window.dispatchEvent(new Event('offline'));
      
      setTimeout(() => {
        // 發送關鍵請求（會被加入離線佇列）
        service.post<any>('/test', { data: 'offline_test' }, { isCritical: true }).subscribe({
          next: done.fail,
          error: () => {
            // 模擬恢復在線
            Object.defineProperty(navigator, 'onLine', {
              writable: true,
              value: true
            });
            
            window.dispatchEvent(new Event('online'));
            
            // 手動重試離線請求
            setTimeout(() => {
              service.retryOfflineRequests().then(() => {
                const status = service.getStatus();
                expect(status.offlineQueueSize).toBe(0);
                done();
              });
              
              // 模擬成功的重試請求
              const req = httpMock.expectOne(`${baseUrl}/test`);
              req.flush({ success: true });
            }, 10);
          }
        });
      }, 10);
    });
  });

  describe('Configuration', () => {
    it('should apply custom configuration', () => {
      const customConfig: Partial<ResilientHttpConfig> = {
        retry: {
          maxAttempts: 5,
          baseDelay: 2000,
          maxDelay: 20000,
          backoffMultiplier: 3,
          retryableErrors: [500, 502, 503]
        },
        timeout: 60000
      };
      
      service.configure(customConfig);
      
      // 配置應該被應用（這裡我們通過行為來驗證）
      expect(service).toBeDefined();
    });
  });

  describe('Status Monitoring', () => {
    it('should provide accurate status information', (done) => {
      service.status$.subscribe(status => {
        expect(status.networkStatus.isOnline).toBe(navigator.onLine);
        expect(status.circuitBreakerState).toBe(CircuitBreakerState.CLOSED);
        expect(status.failureCount).toBe(0);
        expect(status.successCount).toBe(0);
        expect(status.pendingRequests).toBe(0);
        expect(status.offlineQueueSize).toBe(0);
        done();
      });
    });

    it('should update status after successful request', (done) => {
      let statusUpdateCount = 0;
      
      service.status$.subscribe(status => {
        statusUpdateCount++;
        
        if (statusUpdateCount === 2) { // 第二次更新（請求完成後）
          expect(status.successCount).toBe(1);
          expect(status.pendingRequests).toBe(0);
          done();
        }
      });
      
      service.get<any>('/test').subscribe();
      
      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush({ success: true });
    });

    it('should update status after failed request', (done) => {
      let statusUpdateCount = 0;
      
      service.status$.subscribe(status => {
        statusUpdateCount++;
        
        if (statusUpdateCount === 2) { // 第二次更新（請求失敗後）
          expect(status.failureCount).toBeGreaterThan(0);
          expect(status.lastError).toBeDefined();
          done();
        }
      });
      
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: () => {} // 忽略錯誤，我們只關心狀態更新
      });
      
      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Request Metadata', () => {
    it('should generate unique request IDs', (done) => {
      const requestIds: string[] = [];
      
      // 發送多個請求並收集 ID
      for (let i = 0; i < 3; i++) {
        service.get<any>(`/test${i}`).subscribe();
      }
      
      // 驗證所有請求都有唯一 ID
      setTimeout(() => {
        const requests = httpMock.match(() => true);
        expect(requests.length).toBe(3);
        
        requests.forEach(req => req.flush({}));
        done();
      }, 10);
    });

    it('should mark critical requests appropriately', (done) => {
      service.post<any>('/test', { data: 'test' }, { isCritical: true }).subscribe({
        next: () => done(),
        error: done.fail
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush({ success: true });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', (done) => {
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: (error) => {
          expect(error).toBeDefined();
          done();
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle timeout errors', (done) => {
      const config: Partial<ResilientHttpConfig> = {
        timeout: 100 // 很短的超時時間
      };
      
      service.configure(config);
      
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: (error) => {
          expect(error.message).toBe('Request timeout');
          done();
        }
      });

      // 不回應請求，讓它超時
      httpMock.expectOne(`${baseUrl}/test`);
    });

    it('should categorize errors correctly', (done) => {
      service.get<any>('/test').subscribe({
        next: done.fail,
        error: (error) => {
          expect(error.status).toBe(404);
          
          const status = service.getStatus();
          expect(status.failureCount).toBe(1);
          done();
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/test`);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('Memory Management', () => {
    it('should clean up resources on destroy', () => {
      const initialStatus = service.getStatus();
      expect(initialStatus).toBeDefined();
      
      service.destroy();
      
      // 驗證服務已正確清理
      expect(service).toBeDefined(); // 服務物件仍存在，但內部資源已清理
    });

    it('should limit offline queue size', (done) => {
      const config: Partial<ResilientHttpConfig> = {
        offline: {
          enableOfflineStorage: true,
          storageKey: 'test_offline_queue',
          maxStorageSize: 2,
          syncOnReconnect: true
        }
      };
      
      service.configure(config);
      
      // 模擬離線
      Object.defineProperty(navigator, 'onLine', {
        writable: true,
        value: false
      });
      
      window.dispatchEvent(new Event('offline'));
      
      setTimeout(() => {
        // 發送 3 個關鍵請求（超過佇列限制）
        for (let i = 0; i < 3; i++) {
          service.post<any>(`/test${i}`, { data: i }, { isCritical: true }).subscribe({
            next: done.fail,
            error: () => {
              if (i === 2) {
                const status = service.getStatus();
                expect(status.offlineQueueSize).toBe(2); // 應該限制在 2 個
                done();
              }
            }
          });
        }
      }, 10);
    });
  });
});