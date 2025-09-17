import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subject, timer } from 'rxjs';
import { filter, map, takeUntil } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AnalyticsUpdateMessage {
  type: string;
  data: any;
  timestamp: string;
}

export interface ConnectionStats {
  connectedSessions: number;
  timestamp: number;
}

/**
 * Real-time analytics service for WebSocket communication.
 * Handles connection management, subscription/unsubscription, and reconnection logic.
 */
@Injectable({
  providedIn: 'root'
})
export class RealTimeAnalyticsService implements OnDestroy {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000; // Start with 1 second
  private maxReconnectInterval = 30000; // Max 30 seconds
  private isConnecting = false;
  private shouldReconnect = true;
  private subscriptions = new Set<string>();

  // Observables for connection state and messages
  private connectionStateSubject = new BehaviorSubject<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected');
  private messageSubject = new Subject<AnalyticsUpdateMessage>();
  private errorSubject = new Subject<Event>();
  private destroy$ = new Subject<void>();

  // Public observables
  public connectionState$ = this.connectionStateSubject.asObservable();
  public messages$ = this.messageSubject.asObservable();
  public errors$ = this.errorSubject.asObservable();

  private readonly wsUrl: string;

  constructor() {
    // Construct WebSocket URL based on environment
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = environment.production ? window.location.host : 'localhost:8080';
    this.wsUrl = `${protocol}//${host}/ws/analytics`;

    // Auto-connect on service initialization
    this.connect();

    // Handle page visibility changes
    document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
  }

  /**
   * Establish WebSocket connection
   */
  connect(): void {
    if (this.isConnecting || (this.socket && this.socket.readyState === WebSocket.OPEN)) {
      return;
    }

    this.isConnecting = true;
    this.connectionStateSubject.next('connecting');

    try {
      this.socket = new WebSocket(this.wsUrl);
      this.setupEventListeners();
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.handleConnectionError();
    }
  }

  /**
   * Close WebSocket connection
   */
  disconnect(): void {
    this.shouldReconnect = false;
    this.reconnectAttempts = 0;
    
    if (this.socket) {
      this.socket.close(1000, 'Client disconnect');
      this.socket = null;
    }
    
    this.connectionStateSubject.next('disconnected');
    this.isConnecting = false;
  }

  /**
   * Subscribe to a specific analytics channel
   */
  subscribe(channel: string): void {
    this.subscriptions.add(channel);
    
    if (this.isConnected()) {
      this.sendMessage({
        action: 'subscribe',
        channel: channel
      });
    }
  }

  /**
   * Unsubscribe from a specific analytics channel
   */
  unsubscribe(channel: string): void {
    this.subscriptions.delete(channel);
    
    if (this.isConnected()) {
      this.sendMessage({
        action: 'unsubscribe',
        channel: channel
      });
    }
  }

  /**
   * Send ping to keep connection alive
   */
  ping(): void {
    if (this.isConnected()) {
      this.sendMessage({ action: 'ping' });
    }
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
  }

  /**
   * Get current connection state
   */
  getConnectionState(): 'connecting' | 'connected' | 'disconnected' | 'error' {
    return this.connectionStateSubject.value;
  }

  /**
   * Get connection statistics (if available from server)
   */
  getConnectionStats(): Observable<ConnectionStats> {
    // Filter messages for connection stats type and map to ConnectionStats
    return this.messages$.pipe(
      takeUntil(this.destroy$),
      // Only process messages that contain connection stats
      filter(message => message.type === 'connection_stats'),
      // Map the message data to ConnectionStats interface
      map(message => ({
        connectedSessions: message.data.connectedSessions || 0,
        timestamp: message.data.timestamp || Date.now()
      }))
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
    document.removeEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
  }

  private setupEventListeners(): void {
    if (!this.socket) return;

    this.socket.onopen = (event) => {
      console.log('WebSocket connection established:', event);
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      this.reconnectInterval = 1000; // Reset reconnect interval
      this.connectionStateSubject.next('connected');

      // Re-subscribe to all channels after reconnection
      this.subscriptions.forEach(channel => {
        this.sendMessage({
          action: 'subscribe',
          channel: channel
        });
      });
    };

    this.socket.onmessage = (event) => {
      try {
        const message: AnalyticsUpdateMessage = JSON.parse(event.data);
        console.log('Received WebSocket message:', message);
        this.messageSubject.next(message);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error, event.data);
      }
    };

    this.socket.onclose = (event) => {
      console.log('WebSocket connection closed:', event.code, event.reason);
      this.isConnecting = false;
      this.socket = null;
      
      if (event.code !== 1000 && this.shouldReconnect) {
        // Unexpected close, attempt to reconnect
        this.connectionStateSubject.next('disconnected');
        this.scheduleReconnect();
      } else {
        this.connectionStateSubject.next('disconnected');
      }
    };

    this.socket.onerror = (event) => {
      console.error('WebSocket error:', event);
      this.errorSubject.next(event);
      this.handleConnectionError();
    };
  }

  private sendMessage(message: any): void {
    if (this.isConnected() && this.socket) {
      try {
        this.socket.send(JSON.stringify(message));
      } catch (error) {
        console.error('Failed to send WebSocket message:', error);
      }
    }
  }

  private handleConnectionError(): void {
    this.isConnecting = false;
    this.connectionStateSubject.next('error');
    
    if (this.shouldReconnect) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached. Giving up.');
      this.connectionStateSubject.next('error');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(
      this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1),
      this.maxReconnectInterval
    );

    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    timer(delay).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.shouldReconnect && !this.isConnected()) {
        this.connect();
      }
    });
  }

  private handleVisibilityChange(): void {
    if (document.hidden) {
      // Page is hidden, reduce activity
      console.log('Page hidden, WebSocket activity reduced');
    } else {
      // Page is visible, ensure connection is active
      console.log('Page visible, checking WebSocket connection');
      if (!this.isConnected() && this.shouldReconnect) {
        this.connect();
      } else if (this.isConnected()) {
        // Send ping to verify connection is still alive
        this.ping();
      }
    }
  }
}