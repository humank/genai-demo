import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ApplicationConfig } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { ERROR_HANDLING_PROVIDERS } from './core/config/error-handler.config';
import { ObservabilityTraceInterceptor } from './core/interceptors/observability-trace.interceptor';

// PrimeNG
import { MessageService } from 'primeng/api';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    MessageService,
    // 註冊可觀測性追蹤攔截器
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ObservabilityTraceInterceptor,
      multi: true
    },
    // 錯誤處理配置
    ...ERROR_HANDLING_PROVIDERS
  ]
};