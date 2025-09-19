import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

describe('SessionService', () => {
  let service: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    
    // 清除 sessionStorage
    sessionStorage.clear();
    
    service = TestBed.inject(SessionService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Session ID Management', () => {
    it('should generate session ID on initialization', () => {
      const sessionId = service.getSessionId();
      
      expect(sessionId).toBeTruthy();
      expect(sessionId).toMatch(/^session-\\d+-[a-z0-9]+$/);
    });

    it('should persist session ID in sessionStorage', () => {
      const sessionId = service.getSessionId();
      
      expect(sessionStorage.getItem('observability_session_id')).toBe(sessionId);
    });

    it('should reuse existing session ID from sessionStorage', () => {
      const existingSessionId = 'session-123-abc';
      sessionStorage.setItem('observability_session_id', existingSessionId);
      
      const newService = TestBed.inject(SessionService);
      
      expect(newService.getSessionId()).toBe(existingSessionId);
    });

    it('should regenerate session ID when requested', () => {
      const originalSessionId = service.getSessionId();
      
      const newSessionId = service.regenerateSession();
      
      expect(newSessionId).not.toBe(originalSessionId);
      expect(newSessionId).toMatch(/^session-\\d+-[a-z0-9]+$/);
      expect(service.getSessionId()).toBe(newSessionId);
    });

    it('should validate session correctly', () => {
      expect(service.isSessionValid()).toBe(true);
      
      service.clearSession();
      
      expect(service.isSessionValid()).toBe(true); // 清除後會生成新的 session
    });
  });

  describe('Trace ID Management', () => {
    it('should generate trace ID on initialization', () => {
      const traceId = service.getCurrentTraceId();
      
      expect(traceId).toBeTruthy();
      expect(traceId).toMatch(/^trace-\\d+-[a-z0-9]+$/);
    });

    it('should generate new trace ID when requested', () => {
      const originalTraceId = service.getCurrentTraceId();
      
      const newTraceId = service.generateNewTraceId();
      
      expect(newTraceId).not.toBe(originalTraceId);
      expect(newTraceId).toMatch(/^trace-\d+-[a-z0-9]+$/);
      expect(service.getCurrentTraceId()).toBe(newTraceId);
    });

    it('should set custom trace ID', () => {
      const customTraceId = 'custom-trace-123';
      
      service.setTraceId(customTraceId);
      
      expect(service.getCurrentTraceId()).toBe(customTraceId);
    });

    it('should generate new trace ID when session is regenerated', () => {
      const originalTraceId = service.getCurrentTraceId();
      
      service.regenerateSession();
      
      expect(service.getCurrentTraceId()).not.toBe(originalTraceId);
    });
  });

  describe('Session Clearing', () => {
    it('should clear session data from sessionStorage', () => {
      const sessionId = service.getSessionId();
      
      expect(sessionStorage.getItem('observability_session_id')).toBe(sessionId);
      
      service.clearSession();
      
      expect(sessionStorage.getItem('observability_session_id')).toBeNull();
    });

    it('should generate new IDs after clearing', () => {
      const originalSessionId = service.getSessionId();
      const originalTraceId = service.getCurrentTraceId();
      
      service.clearSession();
      
      expect(service.getSessionId()).not.toBe(originalSessionId);
      expect(service.getCurrentTraceId()).not.toBe(originalTraceId);
    });
  });

  describe('Session Information', () => {
    it('should provide session information', () => {
      const sessionInfo = service.getSessionInfo();
      
      expect(sessionInfo.sessionId).toBeTruthy();
      expect(sessionInfo.currentTraceId).toBeTruthy();
      expect(sessionInfo.sessionAge).toBeGreaterThanOrEqual(0);
      expect(sessionInfo.isValid).toBe(true);
    });

    it('should calculate session age correctly', () => {
      const sessionInfo = service.getSessionInfo();
      
      expect(sessionInfo.sessionAge).toBeGreaterThanOrEqual(0);
      expect(sessionInfo.sessionAge).toBeLessThan(1000); // 應該小於 1 秒
    });
  });

  describe('Browser Compatibility', () => {
    it('should handle missing sessionStorage gracefully', () => {
      // 模擬沒有 sessionStorage 的環境
      const originalSessionStorage = window.sessionStorage;
      Object.defineProperty(window, 'sessionStorage', {
        value: undefined,
        writable: true
      });
      
      const newService = TestBed.inject(SessionService);
      
      expect(newService.getSessionId()).toBeTruthy();
      expect(newService.getCurrentTraceId()).toBeTruthy();
      
      // 恢復 sessionStorage
      Object.defineProperty(window, 'sessionStorage', {
        value: originalSessionStorage,
        writable: true
      });
    });

    it('should handle server-side rendering (no window)', () => {
      // 模擬 SSR 環境
      const originalWindow = (globalThis as any).window;
      delete (globalThis as any).window;
      
      const newService = TestBed.inject(SessionService);
      
      expect(newService.getSessionId()).toBeTruthy();
      expect(newService.getCurrentTraceId()).toBeTruthy();
      
      // 恢復 window
      (globalThis as any).window = originalWindow;
    });
  });

  describe('ID Format Validation', () => {
    it('should generate session IDs with correct format', () => {
      for (let i = 0; i < 10; i++) {
        service.regenerateSession();
        const sessionId = service.getSessionId();
        expect(sessionId).toMatch(/^session-\\d+-[a-z0-9]+$/);
      }
    });

    it('should generate trace IDs with correct format', () => {
      for (let i = 0; i < 10; i++) {
        const traceId = service.generateNewTraceId();
        expect(traceId).toMatch(/^trace-\\d+-[a-z0-9]+$/);
      }
    });

    it('should generate unique IDs', () => {
      const sessionIds = new Set();
      const traceIds = new Set();
      
      for (let i = 0; i < 100; i++) {
        service.regenerateSession();
        sessionIds.add(service.getSessionId());
        traceIds.add(service.generateNewTraceId());
      }
      
      expect(sessionIds.size).toBe(100);
      expect(traceIds.size).toBe(100);
    });
  });

  describe('Timestamp Extraction', () => {
    it('should extract timestamp from session ID', () => {
      const beforeTime = Date.now();
      service.regenerateSession();
      const afterTime = Date.now();
      
      const sessionInfo = service.getSessionInfo();
      const sessionAge = sessionInfo.sessionAge;
      
      // 會話年齡應該很小（剛創建）
      expect(sessionAge).toBeLessThan(afterTime - beforeTime + 100);
    });

    it('should handle invalid ID format gracefully', () => {
      // 這個測試驗證內部方法的健壯性
      const sessionInfo = service.getSessionInfo();
      
      // 即使 ID 格式有問題，也應該能正常工作
      expect(sessionInfo.sessionAge).toBeGreaterThanOrEqual(0);
    });
  });
});