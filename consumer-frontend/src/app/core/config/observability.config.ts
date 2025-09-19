import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface ObservabilityFeatures {
  userBehaviorTracking: boolean;
  performanceMetrics: boolean;
  businessEvents: boolean;
  errorTracking: boolean;
  apiTracking: boolean;
}

export interface PerformanceMonitoringConfig {
  enabled: boolean;
  sampleRate: number; // 0.0 到 1.0
}

export interface ObservabilityEnvironmentConfig {
  enabled: boolean;
  debug: boolean;
  batchSize: number;
  flushInterval: number;
  retryAttempts: number;
  enableOfflineStorage: boolean;
  maxStorageSize: number;
  performanceMonitoring: PerformanceMonitoringConfig;
  features: ObservabilityFeatures;
}

export interface RuntimeObservabilityConfig extends ObservabilityEnvironmentConfig {
  apiEndpoint: string;
  performanceEndpoint: string;
  errorEndpoint: string;
  sessionTimeout: number;
  enableSampling: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ObservabilityConfigService {
  private config: RuntimeObservabilityConfig;
  private featureFlags: Map<string, boolean> = new Map();

  constructor() {
    this.config = this.buildRuntimeConfig();
    this.initializeFeatureFlags();
  }

  /**
   * 獲取完整的可觀測性配置
   */
  getConfig(): RuntimeObservabilityConfig {
    return { ...this.config };
  }

  /**
   * 獲取批次處理配置
   */
  getBatchConfig() {
    return {
      maxBatchSize: this.config.batchSize,
      maxWaitTime: this.config.flushInterval,
      retryAttempts: this.config.retryAttempts,
      backoffMultiplier: 2,
      enableOfflineStorage: this.config.enableOfflineStorage,
      storageKey: 'observability_offline_events',
      maxStorageSize: this.config.maxStorageSize
    };
  }

  /**
   * 獲取可觀測性服務配置
   */
  getObservabilityServiceConfig() {
    return {
      enabled: this.config.enabled,
      batchSize: this.config.batchSize,
      flushInterval: this.config.flushInterval,
      apiEndpoint: this.config.apiEndpoint,
      debug: this.config.debug
    };
  }

  /**
   * 檢查功能是否啟用
   */
  isFeatureEnabled(feature: keyof ObservabilityFeatures): boolean {
    if (!this.config.enabled) {
      return false;
    }

    return this.config.features[feature] && this.getFeatureFlag(feature);
  }

  /**
   * 檢查效能監控是否啟用
   */
  isPerformanceMonitoringEnabled(): boolean {
    return this.config.enabled &&
      this.config.performanceMonitoring.enabled &&
      this.shouldSample();
  }

  /**
   * 檢查是否應該進行採樣
   */
  shouldSample(): boolean {
    if (!this.config.enableSampling) {
      return true;
    }

    return Math.random() < this.config.performanceMonitoring.sampleRate;
  }

  /**
   * 動態更新配置 (用於 A/B 測試或遠程配置)
   */
  updateConfig(updates: Partial<RuntimeObservabilityConfig>): void {
    this.config = { ...this.config, ...updates };
  }

  /**
   * 設定功能開關
   */
  setFeatureFlag(feature: string, enabled: boolean): void {
    this.featureFlags.set(feature, enabled);
  }

  /**
   * 獲取功能開關狀態
   */
  getFeatureFlag(feature: string): boolean {
    return this.featureFlags.get(feature) ?? true;
  }

  /**
   * 批量設定功能開關
   */
  setFeatureFlags(flags: Record<string, boolean>): void {
    Object.entries(flags).forEach(([feature, enabled]) => {
      this.featureFlags.set(feature, enabled);
    });
  }

  /**
   * 獲取所有功能開關
   */
  getAllFeatureFlags(): Record<string, boolean> {
    const flags: Record<string, boolean> = {};
    this.featureFlags.forEach((value, key) => {
      flags[key] = value;
    });
    return flags;
  }

  /**
   * 檢查是否為開發環境
   */
  isDevelopment(): boolean {
    return !environment.production;
  }

  /**
   * 檢查是否為生產環境
   */
  isProduction(): boolean {
    return environment.production;
  }

  /**
   * 檢查除錯模式是否啟用
   */
  isDebugEnabled(): boolean {
    return this.config.debug;
  }

  /**
   * 獲取 API 端點配置
   */
  getApiEndpoints() {
    return {
      analytics: this.config.apiEndpoint,
      performance: this.config.performanceEndpoint,
      error: this.config.errorEndpoint
    };
  }

  /**
   * 獲取會話配置
   */
  getSessionConfig() {
    return {
      timeout: this.config.sessionTimeout,
      storageKey: 'observability_session_id'
    };
  }

  /**
   * 重置配置為預設值
   */
  resetToDefaults(): void {
    this.config = this.buildRuntimeConfig();
    this.featureFlags.clear();
    this.initializeFeatureFlags();
  }

  /**
   * 驗證配置有效性
   */
  validateConfig(): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (this.config.batchSize <= 0) {
      errors.push('Batch size must be greater than 0');
    }

    if (this.config.flushInterval <= 0) {
      errors.push('Flush interval must be greater than 0');
    }

    if (this.config.retryAttempts < 0) {
      errors.push('Retry attempts cannot be negative');
    }

    if (this.config.performanceMonitoring.sampleRate < 0 ||
      this.config.performanceMonitoring.sampleRate > 1) {
      errors.push('Sample rate must be between 0 and 1');
    }

    if (this.config.maxStorageSize <= 0) {
      errors.push('Max storage size must be greater than 0');
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  private buildRuntimeConfig(): RuntimeObservabilityConfig {
    const envConfig = environment.observability;

    return {
      ...envConfig,
      apiEndpoint: `/api/analytics/events`, // ⚠️ Backend analytics API partially implemented
      performanceEndpoint: `/api/analytics/performance`, // ⚠️ Backend analytics API partially implemented
      errorEndpoint: `/api/monitoring/events`, // ✅ Basic monitoring endpoints available
      sessionTimeout: 30 * 60 * 1000, // 30 分鐘
      enableSampling: environment.production // 只在生產環境啟用採樣
    };
  }

  private initializeFeatureFlags(): void {
    // 預設所有功能都啟用
    const features: (keyof ObservabilityFeatures)[] = [
      'userBehaviorTracking',
      'performanceMetrics',
      'businessEvents',
      'errorTracking',
      'apiTracking'
    ];

    features.forEach(feature => {
      this.featureFlags.set(feature, true);
    });

    // 可以從 localStorage 載入用戶偏好設定
    this.loadUserPreferences();
  }

  private loadUserPreferences(): void {
    try {
      const stored = localStorage.getItem('observability_preferences');
      if (stored) {
        const preferences = JSON.parse(stored);
        Object.entries(preferences).forEach(([key, value]) => {
          if (typeof value === 'boolean') {
            this.featureFlags.set(key, value);
          }
        });
      }
    } catch (error) {
      console.warn('Failed to load observability preferences:', error);
    }
  }

  /**
   * 儲存用戶偏好設定
   */
  saveUserPreferences(): void {
    try {
      const preferences = this.getAllFeatureFlags();
      localStorage.setItem('observability_preferences', JSON.stringify(preferences));
    } catch (error) {
      console.warn('Failed to save observability preferences:', error);
    }
  }

  /**
   * 獲取配置摘要 (用於除錯)
   */
  getConfigSummary(): Record<string, any> {
    return {
      environment: environment.production ? 'production' : 'development',
      enabled: this.config.enabled,
      debug: this.config.debug,
      batchSize: this.config.batchSize,
      flushInterval: this.config.flushInterval,
      performanceMonitoring: this.config.performanceMonitoring,
      features: this.config.features,
      featureFlags: this.getAllFeatureFlags(),
      apiEndpoints: this.getApiEndpoints(),
      validation: this.validateConfig()
    };
  }
}