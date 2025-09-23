import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { ErrorHandler, Provider } from '@angular/core';
import { ErrorTrackingInterceptor } from '../interceptors/error-tracking.interceptor';
import { ErrorTrackingService, GlobalErrorHandler } from '../services/error-tracking.service';

/**
 * 錯誤處理配置
 * 提供全域錯誤處理器和 HTTP 錯誤攔截器的配置
 */

export const ERROR_HANDLING_PROVIDERS: Provider[] = [
    // 全域錯誤處理器
    {
        provide: ErrorHandler,
        useClass: GlobalErrorHandler
    },

    // HTTP 錯誤追蹤攔截器
    {
        provide: HTTP_INTERCEPTORS,
        useClass: ErrorTrackingInterceptor,
        multi: true
    },

    // 錯誤追蹤服務
    ErrorTrackingService
];

/**
 * 錯誤處理配置選項
 */
export interface ErrorHandlingConfig {
    // 是否啟用錯誤追蹤
    enableErrorTracking: boolean;

    // 是否啟用控制台錯誤捕獲
    enableConsoleErrorCapture: boolean;

    // 是否啟用未處理的 Promise 拒絕捕獲
    enableUnhandledRejectionCapture: boolean;

    // 錯誤佇列最大大小
    maxErrorQueueSize: number;

    // 錯誤批次處理間隔 (毫秒)
    errorFlushInterval: number;

    // 重試次數
    maxRetryAttempts: number;

    // 是否在開發環境中顯示詳細錯誤
    showDetailedErrorsInDev: boolean;
}

/**
 * 預設錯誤處理配置
 */
export const DEFAULT_ERROR_HANDLING_CONFIG: ErrorHandlingConfig = {
    enableErrorTracking: true,
    enableConsoleErrorCapture: true,
    enableUnhandledRejectionCapture: true,
    maxErrorQueueSize: 100,
    errorFlushInterval: 30000, // 30 seconds
    maxRetryAttempts: 3,
    showDetailedErrorsInDev: true
};

/**
 * 生產環境錯誤處理配置
 */
export const PRODUCTION_ERROR_HANDLING_CONFIG: ErrorHandlingConfig = {
    ...DEFAULT_ERROR_HANDLING_CONFIG,
    showDetailedErrorsInDev: false,
    maxErrorQueueSize: 50,
    errorFlushInterval: 60000 // 1 minute
};

/**
 * 開發環境錯誤處理配置
 */
export const DEVELOPMENT_ERROR_HANDLING_CONFIG: ErrorHandlingConfig = {
    ...DEFAULT_ERROR_HANDLING_CONFIG,
    showDetailedErrorsInDev: true,
    maxErrorQueueSize: 200,
    errorFlushInterval: 10000 // 10 seconds
};