import { TestBed } from '@angular/core/testing';
import { ObservabilityConfigService, ObservabilityFeatures } from './observability.config';

describe('ObservabilityConfigService', () => {
  let service: ObservabilityConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    
    // 清除 localStorage
    localStorage.clear();
    
    service = TestBed.inject(ObservabilityConfigService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Configuration Loading', () => {
    it('should load configuration from environment', () => {
      const config = service.getConfig();
      
      expect(config.enabled).toBeDefined();
      expect(config.batchSize).toBeGreaterThan(0);
      expect(config.flushInterval).toBeGreaterThan(0);
      expect(config.apiEndpoint).toBeTruthy();
      expect(config.performanceEndpoint).toBeTruthy();
      expect(config.errorEndpoint).toBeTruthy();
    });

    it('should provide batch configuration', () => {
      const batchConfig = service.getBatchConfig();
      
      expect(batchConfig.maxBatchSize).toBeGreaterThan(0);
      expect(batchConfig.maxWaitTime).toBeGreaterThan(0);
      expect(batchConfig.retryAttempts).toBeGreaterThanOrEqual(0);
      expect(batchConfig.backoffMultiplier).toBe(2);
      expect(batchConfig.storageKey).toBe('observability_offline_events');
    });

    it('should provide observability service configuration', () => {
      const serviceConfig = service.getObservabilityServiceConfig();
      
      expect(serviceConfig.enabled).toBeDefined();
      expect(serviceConfig.batchSize).toBeGreaterThan(0);
      expect(serviceConfig.flushInterval).toBeGreaterThan(0);
      expect(serviceConfig.apiEndpoint).toBeTruthy();
      expect(serviceConfig.debug).toBeDefined();
    });
  });

  describe('Feature Flags', () => {
    it('should check if features are enabled', () => {
      const features: (keyof ObservabilityFeatures)[] = [
        'userBehaviorTracking',
        'performanceMetrics',
        'businessEvents',
        'errorTracking',
        'apiTracking'
      ];

      features.forEach(feature => {
        expect(service.isFeatureEnabled(feature)).toBeDefined();
      });
    });

    it('should set and get feature flags', () => {
      service.setFeatureFlag('testFeature', true);
      expect(service.getFeatureFlag('testFeature')).toBe(true);

      service.setFeatureFlag('testFeature', false);
      expect(service.getFeatureFlag('testFeature')).toBe(false);
    });

    it('should set multiple feature flags', () => {
      const flags = {
        'feature1': true,
        'feature2': false,
        'feature3': true
      };

      service.setFeatureFlags(flags);

      expect(service.getFeatureFlag('feature1')).toBe(true);
      expect(service.getFeatureFlag('feature2')).toBe(false);
      expect(service.getFeatureFlag('feature3')).toBe(true);
    });

    it('should get all feature flags', () => {
      service.setFeatureFlag('test1', true);
      service.setFeatureFlag('test2', false);

      const allFlags = service.getAllFeatureFlags();

      expect(allFlags['test1']).toBe(true);
      expect(allFlags['test2']).toBe(false);
    });

    it('should return false for features when observability is disabled', () => {
      service.updateConfig({ enabled: false });

      expect(service.isFeatureEnabled('userBehaviorTracking')).toBe(false);
      expect(service.isFeatureEnabled('performanceMetrics')).toBe(false);
    });
  });

  describe('Performance Monitoring', () => {
    it('should check if performance monitoring is enabled', () => {
      const isEnabled = service.isPerformanceMonitoringEnabled();
      expect(typeof isEnabled).toBe('boolean');
    });

    it('should handle sampling correctly', () => {
      // 測試 100% 採樣率
      service.updateConfig({ 
        performanceMonitoring: { enabled: true, sampleRate: 1.0 },
        enableSampling: true
      });

      let sampledCount = 0;
      for (let i = 0; i < 100; i++) {
        if (service.shouldSample()) {
          sampledCount++;
        }
      }

      expect(sampledCount).toBe(100);
    });

    it('should handle 0% sampling rate', () => {
      service.updateConfig({ 
        performanceMonitoring: { enabled: true, sampleRate: 0.0 },
        enableSampling: true
      });

      let sampledCount = 0;
      for (let i = 0; i < 100; i++) {
        if (service.shouldSample()) {
          sampledCount++;
        }
      }

      expect(sampledCount).toBe(0);
    });

    it('should always sample when sampling is disabled', () => {
      service.updateConfig({ enableSampling: false });

      expect(service.shouldSample()).toBe(true);
    });
  });

  describe('Configuration Updates', () => {
    it('should update configuration dynamically', () => {
      const originalBatchSize = service.getConfig().batchSize;
      const newBatchSize = originalBatchSize + 10;

      service.updateConfig({ batchSize: newBatchSize });

      expect(service.getConfig().batchSize).toBe(newBatchSize);
    });

    it('should reset to defaults', () => {
      service.updateConfig({ batchSize: 999 });
      service.setFeatureFlag('testFlag', false);

      service.resetToDefaults();

      expect(service.getConfig().batchSize).not.toBe(999);
      expect(service.getFeatureFlag('testFlag')).toBe(true); // 預設為 true
    });
  });

  describe('Environment Detection', () => {
    it('should detect development/production environment', () => {
      const isDev = service.isDevelopment();
      const isProd = service.isProduction();

      expect(typeof isDev).toBe('boolean');
      expect(typeof isProd).toBe('boolean');
      expect(isDev).toBe(!isProd);
    });

    it('should check debug mode', () => {
      const isDebug = service.isDebugEnabled();
      expect(typeof isDebug).toBe('boolean');
    });
  });

  describe('API Endpoints', () => {
    it('should provide API endpoints', () => {
      const endpoints = service.getApiEndpoints();

      expect(endpoints.analytics).toBeTruthy();
      expect(endpoints.performance).toBeTruthy();
      expect(endpoints.error).toBeTruthy();
    });

    it('should provide session configuration', () => {
      const sessionConfig = service.getSessionConfig();

      expect(sessionConfig.timeout).toBeGreaterThan(0);
      expect(sessionConfig.storageKey).toBe('observability_session_id');
    });
  });

  describe('Configuration Validation', () => {
    it('should validate valid configuration', () => {
      const validation = service.validateConfig();

      expect(validation.isValid).toBe(true);
      expect(validation.errors.length).toBe(0);
    });

    it('should detect invalid batch size', () => {
      service.updateConfig({ batchSize: 0 });

      const validation = service.validateConfig();

      expect(validation.isValid).toBe(false);
      expect(validation.errors).toContain('Batch size must be greater than 0');
    });

    it('should detect invalid flush interval', () => {
      service.updateConfig({ flushInterval: -1 });

      const validation = service.validateConfig();

      expect(validation.isValid).toBe(false);
      expect(validation.errors).toContain('Flush interval must be greater than 0');
    });

    it('should detect invalid retry attempts', () => {
      service.updateConfig({ retryAttempts: -1 });

      const validation = service.validateConfig();

      expect(validation.isValid).toBe(false);
      expect(validation.errors).toContain('Retry attempts cannot be negative');
    });

    it('should detect invalid sample rate', () => {
      service.updateConfig({ 
        performanceMonitoring: { enabled: true, sampleRate: 1.5 }
      });

      const validation = service.validateConfig();

      expect(validation.isValid).toBe(false);
      expect(validation.errors).toContain('Sample rate must be between 0 and 1');
    });

    it('should detect invalid storage size', () => {
      service.updateConfig({ maxStorageSize: 0 });

      const validation = service.validateConfig();

      expect(validation.isValid).toBe(false);
      expect(validation.errors).toContain('Max storage size must be greater than 0');
    });
  });

  describe('User Preferences', () => {
    it('should save and load user preferences', () => {
      service.setFeatureFlag('userPreference1', false);
      service.setFeatureFlag('userPreference2', true);

      service.saveUserPreferences();

      // 創建新的服務實例來測試載入
      const newService = TestBed.inject(ObservabilityConfigService);

      expect(newService.getFeatureFlag('userPreference1')).toBe(false);
      expect(newService.getFeatureFlag('userPreference2')).toBe(true);
    });

    it('should handle corrupted preferences gracefully', () => {
      localStorage.setItem('observability_preferences', 'invalid json');

      // 應該不會拋出錯誤
      expect(() => {
        TestBed.inject(ObservabilityConfigService);
      }).not.toThrow();
    });

    it('should handle localStorage errors gracefully', () => {
      // 模擬 localStorage 錯誤
      spyOn(localStorage, 'setItem').and.throwError('Storage error');

      expect(() => {
        service.saveUserPreferences();
      }).not.toThrow();
    });
  });

  describe('Configuration Summary', () => {
    it('should provide configuration summary', () => {
      const summary = service.getConfigSummary();

      expect(summary['environment']).toBeDefined();
      expect(summary['enabled']).toBeDefined();
      expect(summary['debug']).toBeDefined();
      expect(summary['batchSize']).toBeDefined();
      expect(summary['flushInterval']).toBeDefined();
      expect(summary['performanceMonitoring']).toBeDefined();
      expect(summary['features']).toBeDefined();
      expect(summary['featureFlags']).toBeDefined();
      expect(summary['apiEndpoints']).toBeDefined();
      expect(summary['validation']).toBeDefined();
    });

    it('should include validation results in summary', () => {
      service.updateConfig({ batchSize: -1 });

      const summary = service.getConfigSummary();

      expect(summary['validation'].isValid).toBe(false);
      expect(summary['validation'].errors.length).toBeGreaterThan(0);
    });
  });
});