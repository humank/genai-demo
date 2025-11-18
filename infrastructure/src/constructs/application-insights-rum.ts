import * as cdk from 'aws-cdk-lib';
import * as rum from 'aws-cdk-lib/aws-rum';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import { Construct } from 'constructs';

export interface ApplicationInsightsRumProps {
  /**
   * Name of the RUM application monitor
   */
  readonly appMonitorName: string;

  /**
   * Domain where the application is hosted
   */
  readonly domain: string;

  /**
   * Enable session recording
   * @default true
   */
  readonly enableSessionRecording?: boolean;

  /**
   * Enable X-Ray tracing integration
   * @default true
   */
  readonly enableXRay?: boolean;

  /**
   * Sample rate for RUM events (0.0 to 1.0)
   * @default 0.1 (10%)
   */
  readonly sessionSampleRate?: number;

  /**
   * Tags to apply to resources
   */
  readonly tags?: { [key: string]: string };
}

/**
 * Construct for AWS CloudWatch RUM (Real User Monitoring)
 * 
 * This construct creates:
 * - CloudWatch RUM App Monitor for frontend monitoring
 * - Cognito Identity Pool for unauthenticated access
 * - IAM roles for RUM data ingestion
 * - Configuration for Core Web Vitals and custom metrics
 */
export class ApplicationInsightsRum extends Construct {
  /**
   * The CloudWatch RUM App Monitor
   */
  public readonly appMonitor: rum.CfnAppMonitor;

  /**
   * The Cognito Identity Pool for RUM
   */
  public readonly identityPool: cognito.CfnIdentityPool;

  /**
   * The IAM role for unauthenticated RUM access
   */
  public readonly unauthenticatedRole: iam.Role;

  constructor(scope: Construct, id: string, props: ApplicationInsightsRumProps) {
    super(scope, id);

    const enableSessionRecording = props.enableSessionRecording ?? true;
    const enableXRay = props.enableXRay ?? true;
    const sessionSampleRate = props.sessionSampleRate ?? 0.1;

    // Create Cognito Identity Pool for unauthenticated access
    this.identityPool = new cognito.CfnIdentityPool(this, 'RumIdentityPool', {
      identityPoolName: `${props.appMonitorName}-rum-identity-pool`,
      allowUnauthenticatedIdentities: true,
    });

    // Create IAM role for unauthenticated users
    this.unauthenticatedRole = new iam.Role(this, 'RumUnauthRole', {
      assumedBy: new iam.FederatedPrincipal(
        'cognito-identity.amazonaws.com',
        {
          StringEquals: {
            'cognito-identity.amazonaws.com:aud': this.identityPool.ref,
          },
          'ForAnyValue:StringLike': {
            'cognito-identity.amazonaws.com:amr': 'unauthenticated',
          },
        },
        'sts:AssumeRoleWithWebIdentity'
      ),
      description: 'Unauthenticated role for CloudWatch RUM',
      inlinePolicies: {
        RumPutEvents: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'rum:PutRumEvents',
              ],
              resources: [
                `arn:aws:rum:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:appmonitor/${props.appMonitorName}`,
              ],
            }),
          ],
        }),
      },
    });

    // Attach the unauthenticated role to the identity pool
    new cognito.CfnIdentityPoolRoleAttachment(this, 'RumIdentityPoolRoleAttachment', {
      identityPoolId: this.identityPool.ref,
      roles: {
        unauthenticated: this.unauthenticatedRole.roleArn,
      },
    });

    // Create CloudWatch RUM App Monitor
    this.appMonitor = new rum.CfnAppMonitor(this, 'AppMonitor', {
      name: props.appMonitorName,
      domain: props.domain,
      
      // App Monitor Configuration
      appMonitorConfiguration: {
        // Allow cookies for session tracking
        allowCookies: true,
        
        // Enable session recording
        enableXRay: enableXRay,
        
        // Session sample rate (10% by default for cost optimization)
        sessionSampleRate: sessionSampleRate,
        
        // Telemetry configuration
        telemetries: [
          'errors',      // JavaScript errors
          'performance', // Core Web Vitals and performance metrics
          'http',        // HTTP requests
        ],
        
        // Identity pool configuration
        identityPoolId: this.identityPool.ref,
        guestRoleArn: this.unauthenticatedRole.roleArn,
        
        // Excluded pages (optional - can be configured later)
        excludedPages: [],
        
        // Included pages (optional - can be configured later)
        includedPages: [],
        
        // Favorite pages for detailed monitoring
        favoritePages: [
          '/',
          '/products',
          '/cart',
          '/checkout',
          '/orders',
        ],
        
        // Session recording configuration
        ...(enableSessionRecording && {
          sessionSampleRate: sessionSampleRate,
        }),
      },
      
      // Custom events configuration
      customEvents: {
        status: 'ENABLED',
      },
      
      // Tags
      tags: props.tags ? Object.entries(props.tags).map(([key, value]) => ({
        key,
        value,
      })) : undefined,
    });

    // Output the App Monitor ID and configuration
    new cdk.CfnOutput(this, 'AppMonitorId', {
      value: this.appMonitor.ref,
      description: 'CloudWatch RUM App Monitor ID',
      exportName: `${cdk.Stack.of(this).stackName}-RumAppMonitorId`,
    });

    new cdk.CfnOutput(this, 'IdentityPoolId', {
      value: this.identityPool.ref,
      description: 'Cognito Identity Pool ID for RUM',
      exportName: `${cdk.Stack.of(this).stackName}-RumIdentityPoolId`,
    });

    new cdk.CfnOutput(this, 'GuestRoleArn', {
      value: this.unauthenticatedRole.roleArn,
      description: 'IAM Role ARN for unauthenticated RUM access',
      exportName: `${cdk.Stack.of(this).stackName}-RumGuestRoleArn`,
    });

    // Add tags to all resources
    if (props.tags) {
      Object.entries(props.tags).forEach(([key, value]) => {
        cdk.Tags.of(this).add(key, value);
      });
    }
  }

  /**
   * Get the RUM configuration for frontend integration
   */
  public getRumConfig(): {
    appMonitorId: string;
    identityPoolId: string;
    region: string;
    guestRoleArn: string;
  } {
    return {
      appMonitorId: this.appMonitor.ref,
      identityPoolId: this.identityPool.ref,
      region: cdk.Stack.of(this).region,
      guestRoleArn: this.unauthenticatedRole.roleArn,
    };
  }
}
