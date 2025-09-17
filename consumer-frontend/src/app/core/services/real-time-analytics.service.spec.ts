import { TestBed } from '@angular/core/testing';
import { AnalyticsUpdateMessage, RealTimeAnalyticsService } from './real-time-analytics.service';

// Mock WebSocket
class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  constructor(public url: string) {
    // Simulate connection opening after a short delay
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN;
      if (this.onopen) {
        this.onopen(new Event('open'));
      }
    }, 10);
  }

  send(data: string): void {
    // Mock send implementation
    console.log('Mock WebSocket send:', data);
  }

  close(code?: number, reason?: string): void {
    this.readyState = MockWebSocket.CLOSED;
    if (this.onclose) {
      const closeEvent = new CloseEvent('close', { code: code || 1000, reason: reason || '' });
      this.onclose(closeEvent);
    }
  }

  // Helper method to simulate receiving a message
  simulateMessage(data: any): void {
    if (this.onmessage) {
      const messageEvent = new MessageEvent('message', {
        data: JSON.stringify(data)
      });
      this.onmessage(messageEvent);
    }
  }

  // Helper method to simulate an error
  simulateError(): void {
    if (this.onerror) {
      this.onerror(new Event('error'));
    }
  }
}

describe('RealTimeAnalyticsService', () => {
  let service: RealTimeAnalyticsService;
  let mockWebSocket: MockWebSocket;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    
    // Mock the global WebSocket
    (globalThis as any).WebSocket = MockWebSocket;
    
    service = TestBed.inject(RealTimeAnalyticsService);
  });

  afterEach(() => {
    service.ngOnDestroy();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should establish WebSocket connection on initialization', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        expect(service.isConnected()).toBe(true);
        done();
      }
    });
  });

  it('should handle connection state changes', (done) => {
    const states: string[] = [];
    
    service.connectionState$.subscribe(state => {
      states.push(state);
      
      if (states.length >= 2) {
        expect(states).toContain('connecting');
        expect(states).toContain('connected');
        done();
      }
    });
  });

  it('should send subscription message when subscribing to channel', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        // Mock the send method to capture sent messages
        const sendSpy = spyOn(service as any, 'sendMessage').and.callThrough();
        
        service.subscribe('analytics');
        
        expect(sendSpy).toHaveBeenCalledWith({
          action: 'subscribe',
          channel: 'analytics'
        });
        done();
      }
    });
  });

  it('should send unsubscription message when unsubscribing from channel', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        // First subscribe
        service.subscribe('analytics');
        
        // Mock the send method to capture sent messages
        const sendSpy = spyOn(service as any, 'sendMessage').and.callThrough();
        
        service.unsubscribe('analytics');
        
        expect(sendSpy).toHaveBeenCalledWith({
          action: 'unsubscribe',
          channel: 'analytics'
        });
        done();
      }
    });
  });

  it('should handle incoming messages', (done) => {
    const testMessage: AnalyticsUpdateMessage = {
      type: 'user_behavior_update',
      data: { eventType: 'page_view', sessionId: 'test-session' },
      timestamp: new Date().toISOString()
    };

    service.messages$.subscribe(message => {
      expect(message).toEqual(testMessage);
      done();
    });

    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        // Access the private socket to simulate message
        const socket = (service as any).socket as MockWebSocket;
        socket.simulateMessage(testMessage);
      }
    });
  });

  it('should send ping message', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        const sendSpy = spyOn(service as any, 'sendMessage').and.callThrough();
        
        service.ping();
        
        expect(sendSpy).toHaveBeenCalledWith({ action: 'ping' });
        done();
      }
    });
  });

  it('should handle connection errors', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'error') {
        expect(service.isConnected()).toBe(false);
        done();
      }
    });

    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        // Simulate an error
        const socket = (service as any).socket as MockWebSocket;
        socket.simulateError();
      }
    });
  });

  it('should disconnect properly', (done) => {
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        service.disconnect();
        expect(service.isConnected()).toBe(false);
        expect(service.getConnectionState()).toBe('disconnected');
        done();
      }
    });
  });

  it('should re-subscribe to channels after reconnection', (done) => {
    let connectionCount = 0;
    
    service.connectionState$.subscribe(state => {
      if (state === 'connected') {
        connectionCount++;
        
        if (connectionCount === 1) {
          // First connection - subscribe to a channel
          service.subscribe('test-channel');
          
          // Simulate connection loss and reconnection
          const socket = (service as any).socket as MockWebSocket;
          socket.close();
          
          // The service should automatically reconnect
        } else if (connectionCount === 2) {
          // Second connection - verify re-subscription
          const sendSpy = spyOn(service as any, 'sendMessage').and.callThrough();
          
          // Trigger re-subscription logic
          setTimeout(() => {
            expect(sendSpy).toHaveBeenCalledWith({
              action: 'subscribe',
              channel: 'test-channel'
            });
            done();
          }, 50);
        }
      }
    });
  });

  it('should handle visibility changes', () => {
    const connectSpy = spyOn(service, 'connect').and.callThrough();
    const pingSpy = spyOn(service, 'ping').and.callThrough();

    // Simulate page becoming hidden
    Object.defineProperty(document, 'hidden', { value: true, configurable: true });
    (service as any).handleVisibilityChange();

    // Simulate page becoming visible
    Object.defineProperty(document, 'hidden', { value: false, configurable: true });
    (service as any).handleVisibilityChange();

    // Should attempt to ping if connected, or connect if not connected
    expect(connectSpy).toHaveBeenCalled();
  });
});