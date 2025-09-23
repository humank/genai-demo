export const environment = {
  production: true,
  apiUrl: 'https://api.genai-demo.com',
  appName: '電商購物平台',
  version: '1.0.0',
  observability: {
    enabled: true,
    debug: false,
    batchSize: 50, // 生產環境使用較大的批次大小
    flushInterval: 30000, // 30 秒
    retryAttempts: 3,
    enableOfflineStorage: true,
    maxStorageSize: 1000,
    performanceMonitoring: {
      enabled: true,
      sampleRate: 0.1 // 生產環境 10% 採樣以減少負載
    },
    features: {
      userBehaviorTracking: true,
      performanceMetrics: true,
      businessEvents: true,
      errorTracking: true,
      apiTracking: true
    }
  }
};