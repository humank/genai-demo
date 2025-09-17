import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private readonly SESSION_KEY = 'observability_session_id';
  private readonly TRACE_KEY = 'observability_trace_id';
  private sessionId: string;
  private currentTraceId: string;

  constructor() {
    this.sessionId = this.getOrCreateSessionId();
    this.currentTraceId = this.generateTraceId();
  }

  /**
   * 獲取當前會話 ID
   */
  getSessionId(): string {
    return this.sessionId;
  }

  /**
   * 獲取當前追蹤 ID
   */
  getCurrentTraceId(): string {
    return this.currentTraceId;
  }

  /**
   * 生成新的追蹤 ID (用於新的請求)
   */
  generateNewTraceId(): string {
    this.currentTraceId = this.generateTraceId();
    return this.currentTraceId;
  }

  /**
   * 設定追蹤 ID (用於特定場景)
   */
  setTraceId(traceId: string): void {
    this.currentTraceId = traceId;
  }

  /**
   * 重新生成會話 ID (用於用戶登入/登出)
   */
  regenerateSession(): string {
    this.sessionId = this.generateSessionId();
    this.storeSessionId(this.sessionId);
    this.currentTraceId = this.generateTraceId();
    return this.sessionId;
  }

  /**
   * 清除會話資料
   */
  clearSession(): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.removeItem(this.SESSION_KEY);
      sessionStorage.removeItem(this.TRACE_KEY);
    }
    this.sessionId = this.generateSessionId();
    this.currentTraceId = this.generateTraceId();
  }

  /**
   * 檢查會話是否有效
   */
  isSessionValid(): boolean {
    return !!this.sessionId && this.sessionId.length > 0;
  }

  private getOrCreateSessionId(): string {
    if (typeof window === 'undefined' || !window.sessionStorage) {
      return this.generateSessionId();
    }

    let sessionId = sessionStorage.getItem(this.SESSION_KEY);
    
    if (!sessionId) {
      sessionId = this.generateSessionId();
      this.storeSessionId(sessionId);
    }

    return sessionId;
  }

  private storeSessionId(sessionId: string): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.setItem(this.SESSION_KEY, sessionId);
    }
  }

  private generateSessionId(): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substr(2, 9);
    return `session-${timestamp}-${random}`;
  }

  private generateTraceId(): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substr(2, 9);
    return `trace-${timestamp}-${random}`;
  }

  /**
   * 獲取會話統計資訊 (用於除錯)
   */
  getSessionInfo(): {
    sessionId: string;
    currentTraceId: string;
    sessionAge: number;
    isValid: boolean;
  } {
    const sessionTimestamp = this.extractTimestampFromId(this.sessionId);
    const sessionAge = sessionTimestamp ? Date.now() - sessionTimestamp : 0;

    return {
      sessionId: this.sessionId,
      currentTraceId: this.currentTraceId,
      sessionAge,
      isValid: this.isSessionValid()
    };
  }

  private extractTimestampFromId(id: string): number | null {
    const match = id.match(/-(\\d+)-/);
    return match ? parseInt(match[1], 10) : null;
  }
}