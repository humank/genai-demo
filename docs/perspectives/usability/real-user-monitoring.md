# Real User Monitoring (RUM)

> **Last Updated**: 2025-11-17  
> **Status**: 🚧 In Progress

## Overview

Real User Monitoring (RUM) provides visibility into actual user experiences by collecting performance and interaction data from real users in production. This document describes the implementation of CloudWatch RUM for both consumer and management frontends.

## Why Real User Monitoring

### Traditional Monitoring Limitations

**Synthetic Monitoring** (Lighthouse, WebPageTest):
- Tests from specific locations
- Uses simulated network conditions
- Doesn't reflect real user diversity
- Limited to test scenarios

**Server-Side Monitoring** (CloudWatch, X-Ray):
- Measures backend performance only
- Doesn't capture client-side rendering
- Misses JavaScript execution time
- No visibility into user interactions

### RUM Advantages

**Real User Data**:
- Actual user devices and networks
- Real-world performance metrics
- Geographic distribution insights
- Device and browser diversity

**User Experience Focus**:
- Core Web Vitals measurement
- User journey tracking
- Error impact analysis
- Session replay capabilities

## CloudWatch RUM Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                     User Browser                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Frontend Application (Angular/Next.js)                │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │  CloudWatch RUM Web Client                       │  │ │
│  │  │  - Performance monitoring                        │  │ │
│  │  │  - Error tracking                                │  │ │
│  │  │  - Session recording                             │  │ │
│  │  │  - Custom events                                 │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  AWS CloudWatch RUM                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Data Collection & Processing                          │ │
│  │  - Performance metrics aggregation                     │ │
│  │  - Error categorization                                │ │
│  │  │  - Session analysis                                 │ │
│  │  - Geographic insights                                 │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              CloudWatch Dashboards & Alarms                  │
│  - Core Web Vitals dashboard                                │
│  - User journey analytics                                    │
│  - Error tracking dashboard                                  │
│  - Performance alerts                                        │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Client-Side Collection**
   - RUM web client embedded in application
   - Automatic performance metric collection
   - Custom event tracking
   - Error and exception capture

2. **Data Transmission**
   - Batched data transmission to CloudWatch
   - Secure HTTPS communication
   - Automatic retry on failure
   - Minimal performance impact

3. **Server-Side Processing**
   - Metric aggregation and analysis
   - Geographic and device segmentation
   - Session reconstruction
   - Anomaly detection

4. **Visualization and Alerting**
   - Real-time dashboards
   - Custom metric queries
   - Automated alerting
   - Integration with other AWS services

## Implementation

### Consumer Frontend (Angular)

#### 1. Install CloudWatch RUM Web Client

```bash
cd consumer-frontend
npm install --save aws-rum-web
```

#### 2. Configure RUM in Angular Application

```typescript
// src/app/app.module.ts
import { AwsRum, AwsRumConfig } from 'aws-rum-web';

export class AppModule {
  constructor() {
    this.initializeRUM();
  }

  private initializeRUM(): void {
    try {
      const config: AwsRumConfig = {
        sessionSampleRate: 1, // 100% sampling for initial phase
        identityPoolId: 'ap-northeast-1:XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX',
        endpoint: 'https://dataplane.rum.ap-northeast-1.amazonaws.com',
        telemetries: ['performance', 'errors', 'http'],
        allowCookies: true,
        enableXRay: true
      };

      const APPLICATION_ID = 'consumer-frontend-prod';
      const APPLICATION_VERSION = '1.0.0';
      const APPLICATION_REGION = 'ap-northeast-1';

      const awsRum: AwsRum = new AwsRum(
        APPLICATION_ID,
        APPLICATION_VERSION,
        APPLICATION_REGION,
        config
      );

      // Record custom page view events
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe((event: NavigationEnd) => {
        awsRum.recordPageView(event.urlAfterRedirects);
      });

    } catch (error) {
      // Ignore RUM initialization errors to prevent app failure
      console.error('Failed to initialize CloudWatch RUM:', error);
    }
  }
}
```

#### 3. Track Custom Events

```typescript
// src/app/services/analytics.service.ts
import { Injectable } from '@angular/core';
import { AwsRum } from 'aws-rum-web';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private awsRum: AwsRum;

  constructor() {
    this.awsRum = (window as any).awsRum;
  }

  // Track product view
  trackProductView(productId: string, productName: string): void {
    this.awsRum?.recordEvent('product_view', {
      productId,
      productName,
      timestamp: new Date().toISOString()
    });
  }

  // Track add to cart
  trackAddToCart(productId: string, quantity: number, price: number): void {
    this.awsRum?.recordEvent('add_to_cart', {
      productId,
      quantity,
      price,
      timestamp: new Date().toISOString()
    });
  }

  // Track checkout step
  trackCheckoutStep(step: string, stepNumber: number): void {
    this.awsRum?.recordEvent('checkout_step', {
      step,
      stepNumber,
      timestamp: new Date().toISOString()
    });
  }

  // Track purchase
  trackPurchase(orderId: string, totalAmount: number, itemCount: number): void {
    this.awsRum?.recordEvent('purchase', {
      orderId,
      totalAmount,
      itemCount,
      timestamp: new Date().toISOString()
    });
  }

  // Track error
  trackError(errorType: string, errorMessage: string, context?: any): void {
    this.awsRum?.recordError(new Error(errorMessage), {
      errorType,
      context,
      timestamp: new Date().toISOString()
    });
  }
}
```

### Management Frontend (Next.js)

#### 1. Install CloudWatch RUM Web Client

```bash
cd cmc-frontend
npm install --save aws-rum-web
```

#### 2. Configure RUM in Next.js Application

```typescript
// src/lib/rum.ts
import { AwsRum, AwsRumConfig } from 'aws-rum-web';

let awsRum: AwsRum | null = null;

export function initializeRUM(): void {
  if (typeof window === 'undefined' || awsRum) {
    return; // Skip on server-side or if already initialized
  }

  try {
    const config: AwsRumConfig = {
      sessionSampleRate: 1,
      identityPoolId: 'ap-northeast-1:XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX',
      endpoint: 'https://dataplane.rum.ap-northeast-1.amazonaws.com',
      telemetries: ['performance', 'errors', 'http'],
      allowCookies: true,
      enableXRay: true
    };

    const APPLICATION_ID = 'cmc-frontend-prod';
    const APPLICATION_VERSION = '1.0.0';
    const APPLICATION_REGION = 'ap-northeast-1';

    awsRum = new AwsRum(
      APPLICATION_ID,
      APPLICATION_VERSION,
      APPLICATION_REGION,
      config
    );

    // Make available globally
    (window as any).awsRum = awsRum;

  } catch (error) {
    console.error('Failed to initialize CloudWatch RUM:', error);
  }
}

export function getAwsRum(): AwsRum | null {
  return awsRum;
}
```

```typescript
// src/app/layout.tsx
'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';
import { initializeRUM, getAwsRum } from '@/lib/rum';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  useEffect(() => {
    initializeRUM();
  }, []);

  useEffect(() => {
    const awsRum = getAwsRum();
    if (awsRum && pathname) {
      awsRum.recordPageView(pathname);
    }
  }, [pathname]);

  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
```

#### 3. Track Custom Events

```typescript
// src/lib/analytics.ts
import { getAwsRum } from './rum';

export const analytics = {
  // Track dashboard view
  trackDashboardView(dashboardType: string): void {
    const awsRum = getAwsRum();
    awsRum?.recordEvent('dashboard_view', {
      dashboardType,
      timestamp: new Date().toISOString()
    });
  },

  // Track order management action
  trackOrderAction(action: string, orderId: string): void {
    const awsRum = getAwsRum();
    awsRum?.recordEvent('order_action', {
      action,
      orderId,
      timestamp: new Date().toISOString()
    });
  },

  // Track bulk operation
  trackBulkOperation(operation: string, itemCount: number, duration: number): void {
    const awsRum = getAwsRum();
    awsRum?.recordEvent('bulk_operation', {
      operation,
      itemCount,
      duration,
      timestamp: new Date().toISOString()
    });
  },

  // Track report generation
  trackReportGeneration(reportType: string, duration: number): void {
    const awsRum = getAwsRum();
    awsRum?.recordEvent('report_generation', {
      reportType,
      duration,
      timestamp: new Date().toISOString()
    });
  }
};
```

## Core Web Vitals Monitoring

### Metrics Tracked

#### 1. Largest Contentful Paint (LCP)

**Definition**: Time until the largest content element is rendered

**Target**: ≤ 2.5 seconds

**Optimization Strategies**:
- Optimize server response time
- Implement resource preloading
- Optimize images and videos
- Use CDN for static assets
- Implement lazy loading

#### 2. First Input Delay (FID)

**Definition**: Time from first user interaction to browser response

**Target**: ≤ 100 milliseconds

**Optimization Strategies**:
- Minimize JavaScript execution time
- Break up long tasks
- Use web workers for heavy computations
- Implement code splitting
- Defer non-critical JavaScript

#### 3. Cumulative Layout Shift (CLS)

**Definition**: Sum of all unexpected layout shifts

**Target**: ≤ 0.1

**Optimization Strategies**:
- Set explicit dimensions for images and videos
- Reserve space for dynamic content
- Avoid inserting content above existing content
- Use CSS transforms for animations
- Preload fonts

### Monitoring Dashboard

```typescript
// CloudWatch Dashboard Configuration (CDK)
import * as cw from 'aws-cdk-lib/aws-cloudwatch';

const rumDashboard = new cw.Dashboard(this, 'RUMDashboard', {
  dashboardName: 'enterprise-ecommerce-rum',
  widgets: [
    [
      // Core Web Vitals
      new cw.GraphWidget({
        title: 'Core Web Vitals - LCP',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'LargestContentfulPaint',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'p95'
          })
        ],
        leftYAxis: {
          label: 'Time (ms)',
          showUnits: false
        }
      }),
      new cw.GraphWidget({
        title: 'Core Web Vitals - FID',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'FirstInputDelay',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'p95'
          })
        ]
      }),
      new cw.GraphWidget({
        title: 'Core Web Vitals - CLS',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'CumulativeLayoutShift',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'p95'
          })
        ]
      })
    ],
    [
      // Page Load Performance
      new cw.GraphWidget({
        title: 'Page Load Time',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'PageLoadTime',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'p95'
          })
        ]
      }),
      // Error Rate
      new cw.GraphWidget({
        title: 'JavaScript Error Rate',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'JsErrorCount',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'Sum'
          })
        ]
      }),
      // Session Count
      new cw.GraphWidget({
        title: 'Active Sessions',
        left: [
          new cw.Metric({
            namespace: 'AWS/RUM',
            metricName: 'SessionCount',
            dimensionsMap: {
              application_name: 'consumer-frontend-prod'
            },
            statistic: 'Sum'
          })
        ]
      })
    ]
  ]
});
```

## Performance Alerts

### Critical Alerts

```typescript
// LCP Alert - Critical
new cw.Alarm(this, 'LCPCriticalAlarm', {
  metric: new cw.Metric({
    namespace: 'AWS/RUM',
    metricName: 'LargestContentfulPaint',
    dimensionsMap: {
      application_name: 'consumer-frontend-prod'
    },
    statistic: 'p95'
  }),
  threshold: 4000, // 4 seconds
  evaluationPeriods: 2,
  datapointsToAlarm: 2,
  comparisonOperator: cw.ComparisonOperator.GREATER_THAN_THRESHOLD,
  alarmDescription: 'LCP exceeds 4 seconds (critical)',
  actionsEnabled: true
});

// FID Alert - Critical
new cw.Alarm(this, 'FIDCriticalAlarm', {
  metric: new cw.Metric({
    namespace: 'AWS/RUM',
    metricName: 'FirstInputDelay',
    dimensionsMap: {
      application_name: 'consumer-frontend-prod'
    },
    statistic: 'p95'
  }),
  threshold: 300, // 300ms
  evaluationPeriods: 2,
  datapointsToAlarm: 2,
  comparisonOperator: cw.ComparisonOperator.GREATER_THAN_THRESHOLD,
  alarmDescription: 'FID exceeds 300ms (critical)',
  actionsEnabled: true
});

// Error Rate Alert
new cw.Alarm(this, 'ErrorRateAlarm', {
  metric: new cw.Metric({
    namespace: 'AWS/RUM',
    metricName: 'JsErrorCount',
    dimensionsMap: {
      application_name: 'consumer-frontend-prod'
    },
    statistic: 'Sum'
  }),
  threshold: 100, // 100 errors per period
  evaluationPeriods: 1,
  comparisonOperator: cw.ComparisonOperator.GREATER_THAN_THRESHOLD,
  alarmDescription: 'High JavaScript error rate detected',
  actionsEnabled: true
});
```

## User Journey Tracking

### Critical Journeys

#### Consumer Journey: Product Purchase

```typescript
// Track journey steps
export class PurchaseJourneyTracker {
  private awsRum: AwsRum;
  private journeyId: string;

  constructor() {
    this.awsRum = (window as any).awsRum;
    this.journeyId = this.generateJourneyId();
  }

  // Step 1: Product Search
  trackProductSearch(query: string, resultCount: number): void {
    this.awsRum?.recordEvent('journey_step', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      step: 'product_search',
      stepNumber: 1,
      query,
      resultCount,
      timestamp: new Date().toISOString()
    });
  }

  // Step 2: Product View
  trackProductView(productId: string): void {
    this.awsRum?.recordEvent('journey_step', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      step: 'product_view',
      stepNumber: 2,
      productId,
      timestamp: new Date().toISOString()
    });
  }

  // Step 3: Add to Cart
  trackAddToCart(productId: string, quantity: number): void {
    this.awsRum?.recordEvent('journey_step', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      step: 'add_to_cart',
      stepNumber: 3,
      productId,
      quantity,
      timestamp: new Date().toISOString()
    });
  }

  // Step 4: Checkout
  trackCheckout(): void {
    this.awsRum?.recordEvent('journey_step', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      step: 'checkout',
      stepNumber: 4,
      timestamp: new Date().toISOString()
    });
  }

  // Step 5: Purchase Complete
  trackPurchaseComplete(orderId: string, totalAmount: number): void {
    this.awsRum?.recordEvent('journey_step', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      step: 'purchase_complete',
      stepNumber: 5,
      orderId,
      totalAmount,
      timestamp: new Date().toISOString()
    });
  }

  // Track journey abandonment
  trackJourneyAbandonment(lastStep: string, reason?: string): void {
    this.awsRum?.recordEvent('journey_abandonment', {
      journeyId: this.journeyId,
      journeyName: 'product_purchase',
      lastStep,
      reason,
      timestamp: new Date().toISOString()
    });
  }

  private generateJourneyId(): string {
    return `journey_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}
```

## Error Tracking and Analysis

### Error Categories

1. **JavaScript Errors**
   - Syntax errors
   - Runtime errors
   - Promise rejections
   - Network errors

2. **HTTP Errors**
   - 4xx client errors
   - 5xx server errors
   - Timeout errors
   - Network failures

3. **Resource Errors**
   - Image load failures
   - Script load failures
   - CSS load failures
   - Font load failures

### Error Context Collection

```typescript
// Enhanced error tracking
export class ErrorTracker {
  private awsRum: AwsRum;

  constructor() {
    this.awsRum = (window as any).awsRum;
    this.setupGlobalErrorHandlers();
  }

  private setupGlobalErrorHandlers(): void {
    // Catch unhandled errors
    window.addEventListener('error', (event) => {
      this.trackError({
        type: 'javascript_error',
        message: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        stack: event.error?.stack,
        userAgent: navigator.userAgent,
        url: window.location.href
      });
    });

    // Catch unhandled promise rejections
    window.addEventListener('unhandledrejection', (event) => {
      this.trackError({
        type: 'promise_rejection',
        message: event.reason?.message || 'Unhandled promise rejection',
        stack: event.reason?.stack,
        userAgent: navigator.userAgent,
        url: window.location.href
      });
    });
  }

  trackError(errorContext: any): void {
    this.awsRum?.recordError(new Error(errorContext.message), errorContext);
  }
}
```

## Privacy and Compliance

### Data Collection Policy

**Collected Data**:
- Performance metrics (anonymous)
- Error messages (sanitized)
- User journey events (anonymous)
- Device and browser information
- Geographic location (country/region level)

**NOT Collected**:
- Personally Identifiable Information (PII)
- Form input values
- Authentication tokens
- Payment information
- Sensitive user data

### GDPR Compliance

```typescript
// Cookie consent integration
export class RUMConsentManager {
  private awsRum: AwsRum;

  constructor() {
    this.awsRum = (window as any).awsRum;
  }

  // Enable RUM after consent
  enableRUM(): void {
    const config: AwsRumConfig = {
      // ... config
      allowCookies: true
    };
    // Initialize RUM
  }

  // Disable RUM if consent withdrawn
  disableRUM(): void {
    this.awsRum?.disable();
    // Clear RUM cookies
    this.clearRUMCookies();
  }

  private clearRUMCookies(): void {
    document.cookie.split(";").forEach((c) => {
      if (c.trim().startsWith('cwr_')) {
        document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
      }
    });
  }
}
```

## Performance Impact

### RUM Client Overhead

**Bundle Size**:
- Minified: ~50KB
- Gzipped: ~15KB

**Performance Impact**:
- Page load time increase: < 50ms
- Memory usage: < 5MB
- CPU usage: < 1%
- Network bandwidth: < 10KB per session

### Optimization Strategies

1. **Lazy Loading**
   - Load RUM client after critical content
   - Use async/defer for script loading

2. **Sampling**
   - Start with 100% sampling
   - Reduce to 10-20% after baseline established
   - Always sample error events

3. **Batching**
   - Batch events before transmission
   - Reduce network requests
   - Minimize performance impact

## Related Documentation

- [Overview](overview.md) - Usability Perspective overview
- [Accessibility Compliance](accessibility-compliance.md) - WCAG guidelines
- [User Journey Optimization](user-journey-optimization.md) - Journey mapping
- [Performance Perspective](../performance/README.md) - Performance optimization

## References

- [AWS CloudWatch RUM Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-RUM.html)
- [Core Web Vitals](https://web.dev/vitals/)
- [Web Performance Working Group](https://www.w3.org/webperf/)

---

**Implementation Status**: 🚧 Specification Complete - Implementation Pending  
**Target Completion**: 2025-12-01
