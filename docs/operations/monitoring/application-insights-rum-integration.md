# Application Insights Real User Monitoring (RUM) Integration Guide

**Last Updated**: 2025-10-22  
**Status**: Active  
**Owner**: Frontend Team + DevOps Team

## Overview

This guide provides comprehensive instructions for integrating AWS CloudWatch RUM (Real User Monitoring) into the GenAI Demo frontend applications to enable:

- **Real User Monitoring (RUM)**: Track actual user interactions and performance
- **JavaScript Error Tracking**: Capture and analyze frontend errors with full context
- **Core Web Vitals Monitoring**: Monitor LCP, FID, and CLS metrics
- **HTTP Request Tracking**: Monitor API calls and their performance
- **Session Recording**: Understand user behavior and issues

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend Applications                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Consumer Frontend   │    │    CMC Frontend          │  │
│  │  (Angular 18)        │    │    (Next.js 14)          │  │
│  │                      │    │                          │  │
│  │  - RUM Web Client    │    │  - RUM Web Client        │  │
│  │  - Error Tracking    │    │  - Error Tracking        │  │
│  │  - Core Web Vitals   │    │  - Core Web Vitals       │  │
│  └──────────────────────┘    └──────────────────────────┘  │
│           │                            │                     │
│           └────────────────┬───────────┘                     │
│                            ▼                                 │
├─────────────────────────────────────────────────────────────┤
│              AWS CloudWatch RUM Service                      │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  App Monitor         │    │  Cognito Identity Pool   │  │
│  │  - Consumer          │    │  - Unauthenticated       │  │
│  │  - CMC               │    │  - IAM Roles             │  │
│  └──────────────────────┘    └──────────────────────────┘  │
│           │                            │                     │
│           └────────────────┬───────────┘                     │
│                            ▼                                 │
├─────────────────────────────────────────────────────────────┤
│              CloudWatch Metrics & Dashboards                 │
│  - Page Load Performance                                     │
│  - JavaScript Errors                                         │
│  - Core Web Vitals (LCP, FID, CLS)                          │
│  - HTTP Request Performance                                  │
│  - Session Analytics                                         │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

### Infrastructure Setup

1. **Deploy Observability Stack** with RUM enabled:
   ```bash
   cd infrastructure
   npm run cdk deploy ObservabilityStack
   ```

2. **Retrieve RUM Configuration** from CDK outputs:
   ```bash
   aws cloudformation describe-stacks \
     --stack-name ObservabilityStack \
     --query 'Stacks[0].Outputs' \
     --output json
   ```

   You'll need:
   - `ConsumerRumAppMonitorId`
   - `ConsumerRumIdentityPoolId`
   - `CmcRumAppMonitorId`
   - `CmcRumIdentityPoolId`

### NPM Packages

Install the AWS RUM Web Client:

```bash
npm install --save aws-rum-web
```

## Consumer Frontend Integration (Angular 18)

### Step 1: Create RUM Service

Create `src/app/services/rum.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { AwsRum, AwsRumConfig } from 'aws-rum-web';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RumService {
  private awsRum: AwsRum | null = null;

  constructor() {
    this.initializeRum();
  }

  private initializeRum(): void {
    if (!environment.rum.enabled) {
      console.log('RUM is disabled in this environment');
      return;
    }

    try {
      const config: AwsRumConfig = {
        sessionSampleRate: environment.rum.sessionSampleRate,
        identityPoolId: environment.rum.identityPoolId,
        endpoint: `https://dataplane.rum.${environment.rum.region}.amazonaws.com`,
        telemetries: ['errors', 'performance', 'http'],
        allowCookies: true,
        enableXRay: true,
      };

      this.awsRum = new AwsRum(
        environment.rum.applicationId,
        environment.rum.applicationVersion,
        environment.rum.region,
        config
      );

      console.log('AWS RUM initialized successfully');
    } catch (error) {
      console.error('Failed to initialize AWS RUM:', error);
    }
  }

  /**
   * Record a custom event
   */
  recordEvent(eventType: string, eventData: Record<string, any>): void {
    if (this.awsRum) {
      this.awsRum.recordEvent(eventType, eventData);
    }
  }

  /**
   * Record an error
   */
  recordError(error: Error): void {
    if (this.awsRum) {
      this.awsRum.recordError(error);
    }
  }

  /**
   * Add session attributes
   */
  addSessionAttributes(attributes: Record<string, string>): void {
    if (this.awsRum) {
      this.awsRum.addSessionAttributes(attributes);
    }
  }

  /**
   * Record page view
   */
  recordPageView(pageId: string): void {
    if (this.awsRum) {
      this.awsRum.recordPageView(pageId);
    }
  }
}
```

### Step 2: Update Environment Configuration

Update `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  rum: {
    enabled: true,
    applicationId: 'CONSUMER_RUM_APP_MONITOR_ID', // From CDK output
    applicationVersion: '1.0.0',
    region: 'ap-northeast-1',
    identityPoolId: 'CONSUMER_RUM_IDENTITY_POOL_ID', // From CDK output
    sessionSampleRate: 0.5, // 50% sampling in development
  },
};
```

Update `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.genai-demo.com',
  rum: {
    enabled: true,
    applicationId: 'CONSUMER_RUM_APP_MONITOR_ID', // From CDK output
    applicationVersion: '1.0.0',
    region: 'ap-northeast-1',
    identityPoolId: 'CONSUMER_RUM_IDENTITY_POOL_ID', // From CDK output
    sessionSampleRate: 0.1, // 10% sampling in production
  },
};
```

### Step 3: Initialize RUM in App Component

Update `src/app/app.component.ts`:

```typescript
import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RumService } from './services/rum.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'consumer-frontend';

  constructor(
    private router: Router,
    private rumService: RumService
  ) {}

  ngOnInit(): void {
    // Track page views
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.rumService.recordPageView(event.urlAfterRedirects);
      });

    // Add session attributes
    this.rumService.addSessionAttributes({
      userType: 'consumer',
      appVersion: '1.0.0',
    });
  }
}
```

### Step 4: Create Global Error Handler

Create `src/app/services/global-error-handler.service.ts`:

```typescript
import { ErrorHandler, Injectable } from '@angular/core';
import { RumService } from './rum.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private rumService: RumService) {}

  handleError(error: Error): void {
    // Log to console
    console.error('Global error:', error);

    // Record error in RUM
    this.rumService.recordError(error);

    // You can also send to your backend logging service here
  }
}
```

Register in `src/app/app.module.ts`:

```typescript
import { ErrorHandler, NgModule } from '@angular/core';
import { GlobalErrorHandler } from './services/global-error-handler.service';

@NgModule({
  // ... other configuration
  providers: [
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler,
    },
  ],
})
export class AppModule {}
```

### Step 5: Track Custom Events

Example usage in components:

```typescript
import { Component } from '@angular/core';
import { RumService } from '../services/rum.service';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
})
export class ProductListComponent {
  constructor(private rumService: RumService) {}

  onProductClick(productId: string): void {
    // Record custom event
    this.rumService.recordEvent('product_clicked', {
      productId,
      timestamp: new Date().toISOString(),
    });
  }

  onAddToCart(productId: string): void {
    this.rumService.recordEvent('add_to_cart', {
      productId,
      source: 'product_list',
    });
  }
}
```

## CMC Frontend Integration (Next.js 14)

### Step 1: Create RUM Utility

Create `src/lib/rum.ts`:

```typescript
import { AwsRum, AwsRumConfig } from 'aws-rum-web';

let awsRum: AwsRum | null = null;

export interface RumConfig {
  enabled: boolean;
  applicationId: string;
  applicationVersion: string;
  region: string;
  identityPoolId: string;
  sessionSampleRate: number;
}

export function initializeRum(config: RumConfig): void {
  if (!config.enabled || typeof window === 'undefined') {
    return;
  }

  if (awsRum) {
    console.log('RUM already initialized');
    return;
  }

  try {
    const rumConfig: AwsRumConfig = {
      sessionSampleRate: config.sessionSampleRate,
      identityPoolId: config.identityPoolId,
      endpoint: `https://dataplane.rum.${config.region}.amazonaws.com`,
      telemetries: ['errors', 'performance', 'http'],
      allowCookies: true,
      enableXRay: true,
    };

    awsRum = new AwsRum(
      config.applicationId,
      config.applicationVersion,
      config.region,
      rumConfig
    );

    console.log('AWS RUM initialized successfully');
  } catch (error) {
    console.error('Failed to initialize AWS RUM:', error);
  }
}

export function recordEvent(eventType: string, eventData: Record<string, any>): void {
  if (awsRum) {
    awsRum.recordEvent(eventType, eventData);
  }
}

export function recordError(error: Error): void {
  if (awsRum) {
    awsRum.recordError(error);
  }
}

export function addSessionAttributes(attributes: Record<string, string>): void {
  if (awsRum) {
    awsRum.addSessionAttributes(attributes);
  }
}

export function recordPageView(pageId: string): void {
  if (awsRum) {
    awsRum.recordPageView(pageId);
  }
}

export function getRumInstance(): AwsRum | null {
  return awsRum;
}
```

### Step 2: Create RUM Provider Component

Create `src/components/RumProvider.tsx`:

```typescript
'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';
import { initializeRum, recordPageView, addSessionAttributes } from '@/lib/rum';

const rumConfig = {
  enabled: process.env.NEXT_PUBLIC_RUM_ENABLED === 'true',
  applicationId: process.env.NEXT_PUBLIC_RUM_APP_MONITOR_ID || '',
  applicationVersion: process.env.NEXT_PUBLIC_APP_VERSION || '1.0.0',
  region: process.env.NEXT_PUBLIC_RUM_REGION || 'ap-northeast-1',
  identityPoolId: process.env.NEXT_PUBLIC_RUM_IDENTITY_POOL_ID || '',
  sessionSampleRate: parseFloat(process.env.NEXT_PUBLIC_RUM_SAMPLE_RATE || '0.1'),
};

export function RumProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  useEffect(() => {
    // Initialize RUM on mount
    initializeRum(rumConfig);

    // Add session attributes
    addSessionAttributes({
      userType: 'admin',
      appVersion: rumConfig.applicationVersion,
    });
  }, []);

  useEffect(() => {
    // Record page view on route change
    if (pathname) {
      recordPageView(pathname);
    }
  }, [pathname]);

  return <>{children}</>;
}
```

### Step 3: Update Root Layout

Update `src/app/layout.tsx`:

```typescript
import { RumProvider } from '@/components/RumProvider';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <RumProvider>
          {children}
        </RumProvider>
      </body>
    </html>
  );
}
```

### Step 4: Create Environment Variables

Create `.env.local`:

```bash
# AWS RUM Configuration
NEXT_PUBLIC_RUM_ENABLED=true
NEXT_PUBLIC_RUM_APP_MONITOR_ID=CMC_RUM_APP_MONITOR_ID
NEXT_PUBLIC_RUM_IDENTITY_POOL_ID=CMC_RUM_IDENTITY_POOL_ID
NEXT_PUBLIC_RUM_REGION=ap-northeast-1
NEXT_PUBLIC_RUM_SAMPLE_RATE=0.5
NEXT_PUBLIC_APP_VERSION=1.0.0
```

Create `.env.production`:

```bash
# AWS RUM Configuration
NEXT_PUBLIC_RUM_ENABLED=true
NEXT_PUBLIC_RUM_APP_MONITOR_ID=CMC_RUM_APP_MONITOR_ID
NEXT_PUBLIC_RUM_IDENTITY_POOL_ID=CMC_RUM_IDENTITY_POOL_ID
NEXT_PUBLIC_RUM_REGION=ap-northeast-1
NEXT_PUBLIC_RUM_SAMPLE_RATE=0.2
NEXT_PUBLIC_APP_VERSION=1.0.0
```

### Step 5: Create Error Boundary

Create `src/components/ErrorBoundary.tsx`:

```typescript
'use client';

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { recordError } from '@/lib/rum';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Error caught by boundary:', error, errorInfo);
    
    // Record error in RUM
    recordError(error);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <h2>Something went wrong</h2>
          <p>We're sorry for the inconvenience. Please try refreshing the page.</p>
        </div>
      );
    }

    return this.props.children;
  }
}
```

### Step 6: Track Custom Events

Example usage in components:

```typescript
'use client';

import { recordEvent } from '@/lib/rum';

export function ProductCard({ product }: { product: Product }) {
  const handleClick = () => {
    recordEvent('product_viewed', {
      productId: product.id,
      productName: product.name,
      timestamp: new Date().toISOString(),
    });
  };

  return (
    <div onClick={handleClick}>
      {/* Product card content */}
    </div>
  );
}
```

## Core Web Vitals Monitoring

The RUM client automatically collects Core Web Vitals:

### Largest Contentful Paint (LCP)
- **Target**: < 2.5 seconds
- **Measures**: Loading performance
- **Optimization**: Optimize images, reduce server response time, use CDN

### First Input Delay (FID)
- **Target**: < 100 milliseconds
- **Measures**: Interactivity
- **Optimization**: Minimize JavaScript execution, code splitting, lazy loading

### Cumulative Layout Shift (CLS)
- **Target**: < 0.1
- **Measures**: Visual stability
- **Optimization**: Set image dimensions, avoid dynamic content injection

## Monitoring and Alerts

### CloudWatch Dashboard

Access the RUM dashboard:
1. Go to AWS CloudWatch Console
2. Navigate to "Dashboards"
3. Select "GenAI-Demo-{environment}"
4. View RUM widgets:
   - Page Load Performance
   - JavaScript Errors
   - Core Web Vitals
   - HTTP Request Performance
   - Session Count

### CloudWatch Alarms

Configured alarms:
- **High JavaScript Error Rate**: > 10 errors in 5 minutes
- **Poor LCP**: > 2.5 seconds (P75)
- **Poor FID**: > 100ms (P75)

### SNS Notifications

Alerts are sent to:
- **Critical Alerts**: JavaScript errors
- **Warning Alerts**: Core Web Vitals degradation

## Troubleshooting

### RUM Not Initializing

**Problem**: RUM client fails to initialize

**Solutions**:
1. Check environment variables are set correctly
2. Verify IAM permissions for Cognito Identity Pool
3. Check browser console for errors
4. Ensure RUM is enabled in environment config

### No Data in CloudWatch

**Problem**: No RUM data appearing in CloudWatch

**Solutions**:
1. Verify App Monitor ID is correct
2. Check session sample rate (increase for testing)
3. Ensure cookies are enabled in browser
4. Check network tab for RUM API calls
5. Verify CORS configuration

### High Data Costs

**Problem**: RUM costs are higher than expected

**Solutions**:
1. Reduce session sample rate in production
2. Exclude non-critical pages from monitoring
3. Implement custom sampling logic
4. Review and optimize telemetry configuration

## Best Practices

### 1. Sampling Strategy

- **Development**: 50-100% sampling for testing
- **Staging**: 20-50% sampling for validation
- **Production**: 10-20% sampling for cost optimization

### 2. Custom Events

- Track business-critical user actions
- Include relevant context in event data
- Use consistent event naming conventions
- Avoid tracking PII (Personally Identifiable Information)

### 3. Error Tracking

- Implement global error handlers
- Add context to errors (user ID, page, action)
- Don't log sensitive information
- Use error boundaries in React/Angular

### 4. Performance Optimization

- Initialize RUM asynchronously
- Use code splitting to reduce bundle size
- Lazy load RUM for non-critical pages
- Monitor RUM client performance impact

### 5. Privacy and Compliance

- Respect user privacy preferences
- Implement cookie consent
- Anonymize user data
- Follow GDPR/CCPA guidelines

## Security Considerations

### 1. IAM Permissions

The Cognito Identity Pool has minimal permissions:
- Only `rum:PutRumEvents` action
- Scoped to specific App Monitor ARN
- Unauthenticated access only for RUM

### 2. Data Protection

- No PII should be sent to RUM
- Session data is encrypted in transit
- Data retention follows AWS RUM policies
- Access controlled via IAM

### 3. CORS Configuration

Ensure your domain is configured in the App Monitor:
```typescript
// In CDK construct
domain: 'consumer.genai-demo.com'
```

## Cost Optimization

### Estimated Costs

Based on AWS RUM pricing:
- **Events**: $1.00 per 100,000 events
- **Sessions**: $0.50 per 1,000 sessions

### Cost Reduction Strategies

1. **Adjust Sample Rate**: Lower in production
2. **Exclude Pages**: Don't monitor admin/internal pages
3. **Filter Events**: Only track critical events
4. **Session Limits**: Set maximum session duration

### Example Cost Calculation

For 10,000 daily active users with 10% sampling:
- Sessions: 1,000/day × 30 days = 30,000 sessions/month
- Cost: 30 × $0.50 = $15/month

## References

- [AWS CloudWatch RUM Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-RUM.html)
- [AWS RUM Web Client GitHub](https://github.com/aws-observability/aws-rum-web)
- [Core Web Vitals](https://web.dev/vitals/)
- [Angular Error Handling](https://angular.io/api/core/ErrorHandler)
- [Next.js Error Handling](https://nextjs.org/docs/advanced-features/error-handling)

## Support

For issues or questions:
- **DevOps Team**: devops@genai-demo.com
- **Frontend Team**: frontend@genai-demo.com
- **AWS Support**: Open a support ticket

---

**Document Version**: 1.0  
**Last Reviewed**: 2025-10-22  
**Next Review**: 2025-11-22
