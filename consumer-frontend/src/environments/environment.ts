export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  appName: '電商購物平台',
  version: '1.0.0',
  observability: {
    enabled: true,
    debug: true,
    batchSize: 20, // 開發環境使用較小的批次大小
    flushInterval: 10000, // 10 秒，開發環境更頻繁刷新
    retryAttempts: 2,
    enableOfflineStorage: true,
    maxStorageSize: 500,
    performanceMonitoring: {
      enabled: true,
      sampleRate: 1.0 // 開發環境 100% 採樣
    },
    features: {
      userBehaviorTracking: true, // ✅ Works independently
      performanceMetrics: true,   // ✅ Works independently  
      businessEvents: true,       // ✅ Works with basic backend API
      errorTracking: true,        // ✅ Works independently
      apiTracking: true          // ✅ Works with basic backend API
    },
    // Development mode notices
    notices: {
      websocketDisabled: true,    // WebSocket features use mock data
      analyticsPartial: true,     // Analytics API partially implemented
      realTimeDisabled: true      // Real-time features use mock data
    }
  }
};