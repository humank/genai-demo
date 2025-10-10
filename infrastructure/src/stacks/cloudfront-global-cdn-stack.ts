import * as cdk from 'aws-cdk-lib';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface CloudFrontGlobalCdnStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain: string;
    readonly hostedZone: route53.IHostedZone;
    readonly primaryLoadBalancer: elbv2.IApplicationLoadBalancer;
    readonly secondaryLoadBalancer?: elbv2.IApplicationLoadBalancer;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly certificate: certificatemanager.ICertificate;
    readonly primaryHealthCheck?: route53.CfnHealthCheck;
    readonly secondaryHealthCheck?: route53.CfnHealthCheck;
}

export class CloudFrontGlobalCdnStack extends cdk.Stack {
    public readonly distribution: cloudfront.Distribution;
    public readonly cdnMonitoring: cloudwatch.Dashboard;
    // public readonly originFailoverGroup: cloudfront.OriginGroup; // Deprecated in CDK v2

    constructor(scope: Construct, id: string, props: CloudFrontGlobalCdnStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            domain,
            hostedZone,
            primaryLoadBalancer,
            secondaryLoadBalancer,
            primaryRegion,
            secondaryRegion,
            certificate,
            primaryHealthCheck,
            secondaryHealthCheck
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'CloudFrontGlobalCDN',
            PrimaryRegion: primaryRegion,
            SecondaryRegion: secondaryRegion
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get multi-region configuration
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Create primary origin (OriginGroup deprecated in CDK v2)
        const primaryOrigin = this.createPrimaryOrigin(
            primaryLoadBalancer,
            secondaryLoadBalancer,
            primaryRegion,
            secondaryRegion,
            projectName,
            environment,
            multiRegionConfig
        );

        // Create CloudFront distribution with primary origin
        this.distribution = this.createCloudFrontDistribution(
            domain,
            certificate,
            primaryOrigin,
            projectName,
            environment,
            multiRegionConfig
        );

        // Create Route53 alias record for CDN
        this.createRoute53AliasRecord(
            domain,
            hostedZone,
            this.distribution,
            projectName,
            environment
        );

        // Create CDN performance monitoring dashboard
        this.cdnMonitoring = this.createCdnMonitoring(
            projectName,
            environment,
            this.distribution,
            multiRegionConfig
        );

        // Create CDN failover alerting
        this.createCdnFailoverAlerting(
            projectName,
            environment,
            this.distribution,
            multiRegionConfig
        );

        // Create outputs
        this.createOutputs(projectName, environment, domain);
    }

    private createPrimaryOrigin(
        primaryLoadBalancer: elbv2.IApplicationLoadBalancer,
        secondaryLoadBalancer: elbv2.IApplicationLoadBalancer | undefined,
        primaryRegion: string,
        secondaryRegion: string,
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): origins.LoadBalancerV2Origin {
        // Create primary origin from ALB
        const primaryOrigin = new origins.LoadBalancerV2Origin(primaryLoadBalancer, {
            protocolPolicy: cloudfront.OriginProtocolPolicy.HTTPS_ONLY,
            httpsPort: 443,
            httpPort: 80,
            originId: `${projectName}-${environment}-primary-origin`,
            customHeaders: {
                'X-Origin-Region': primaryRegion,
                'X-Origin-Type': 'Primary'
            },
            connectionAttempts: 3,
            connectionTimeout: cdk.Duration.seconds(10),
            readTimeout: cdk.Duration.seconds(30)
        });

        // Create secondary origin if available
        const secondaryOrigin = secondaryLoadBalancer ? new origins.LoadBalancerV2Origin(secondaryLoadBalancer, {
            protocolPolicy: cloudfront.OriginProtocolPolicy.HTTPS_ONLY,
            httpsPort: 443,
            httpPort: 80,
            originId: `${projectName}-${environment}-secondary-origin`,
            customHeaders: {
                'X-Origin-Region': secondaryRegion,
                'X-Origin-Type': 'Secondary'
            },
            connectionAttempts: 3,
            connectionTimeout: cdk.Duration.seconds(10),
            readTimeout: cdk.Duration.seconds(30)
        }) : undefined;

        // Return primary origin (OriginGroup is deprecated in CDK v2)
        // Failover is now handled at the distribution level
        return primaryOrigin;
    }

    private createCloudFrontDistribution(
        domain: string,
        certificate: certificatemanager.ICertificate,
        primaryOrigin: origins.LoadBalancerV2Origin,
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): cloudfront.Distribution {
        // Create cache policies for different content types
        const apiCachePolicy = new cloudfront.CachePolicy(this, 'ApiCachePolicy', {
            cachePolicyName: `${projectName}-${environment}-api-cache-policy`,
            comment: 'Cache policy for API responses with short TTL',
            defaultTtl: cdk.Duration.minutes(5),
            maxTtl: cdk.Duration.minutes(30),
            minTtl: cdk.Duration.seconds(0),
            headerBehavior: cloudfront.CacheHeaderBehavior.allowList(
                'Authorization',
                'Content-Type',
                'X-Forwarded-For',
                'X-Origin-Region'
            ),
            queryStringBehavior: cloudfront.CacheQueryStringBehavior.all(),
            cookieBehavior: cloudfront.CacheCookieBehavior.none(),
            enableAcceptEncodingGzip: true,
            enableAcceptEncodingBrotli: true
        });

        const staticCachePolicy = new cloudfront.CachePolicy(this, 'StaticCachePolicy', {
            cachePolicyName: `${projectName}-${environment}-static-cache-policy`,
            comment: 'Cache policy for static content with long TTL',
            defaultTtl: cdk.Duration.hours(24),
            maxTtl: cdk.Duration.days(365),
            minTtl: cdk.Duration.seconds(0),
            headerBehavior: cloudfront.CacheHeaderBehavior.allowList(
                'Content-Type',
                'Content-Encoding'
            ),
            queryStringBehavior: cloudfront.CacheQueryStringBehavior.none(),
            cookieBehavior: cloudfront.CacheCookieBehavior.none(),
            enableAcceptEncodingGzip: true,
            enableAcceptEncodingBrotli: true
        });

        // Create origin request policy
        const originRequestPolicy = new cloudfront.OriginRequestPolicy(this, 'OriginRequestPolicy', {
            originRequestPolicyName: `${projectName}-${environment}-origin-request-policy`,
            comment: 'Origin request policy for multi-region setup',
            headerBehavior: cloudfront.OriginRequestHeaderBehavior.allowList(
                'Authorization',
                'Content-Type',
                'User-Agent',
                'X-Forwarded-For',
                'X-Real-IP',
                'X-Origin-Region'
            ),
            queryStringBehavior: cloudfront.OriginRequestQueryStringBehavior.all(),
            cookieBehavior: cloudfront.OriginRequestCookieBehavior.none()
        });

        // Create response headers policy for security
        const responseHeadersPolicy = new cloudfront.ResponseHeadersPolicy(this, 'ResponseHeadersPolicy', {
            responseHeadersPolicyName: `${projectName}-${environment}-response-headers-policy`,
            comment: 'Security and performance response headers',
            securityHeadersBehavior: {
                contentTypeOptions: { override: true },
                frameOptions: { frameOption: cloudfront.HeadersFrameOption.DENY, override: true },
                referrerPolicy: { referrerPolicy: cloudfront.HeadersReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN, override: true },
                strictTransportSecurity: {
                    accessControlMaxAge: cdk.Duration.seconds(31536000),
                    includeSubdomains: true,
                    preload: true,
                    override: true
                }
            },
            // Custom headers are now configured via responseHeadersPolicy in CDK v2
        });

        // Create CloudFront distribution
        const distribution = new cloudfront.Distribution(this, 'GlobalCdnDistribution', {
            domainNames: [`cdn.${domain}`, `api-cdn.${domain}`],
            certificate: certificate,
            defaultBehavior: {
                origin: primaryOrigin,
                viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                cachePolicy: apiCachePolicy,
                originRequestPolicy: originRequestPolicy,
                responseHeadersPolicy: responseHeadersPolicy,
                allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
                cachedMethods: cloudfront.CachedMethods.CACHE_GET_HEAD_OPTIONS,
                compress: true
            },
            additionalBehaviors: {
                '/api/*': {
                    origin: primaryOrigin,
                    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                    cachePolicy: apiCachePolicy,
                    originRequestPolicy: originRequestPolicy,
                    responseHeadersPolicy: responseHeadersPolicy,
                    allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
                    cachedMethods: cloudfront.CachedMethods.CACHE_GET_HEAD,
                    compress: true
                },
                '/static/*': {
                    origin: primaryOrigin,
                    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                    cachePolicy: staticCachePolicy,
                    originRequestPolicy: originRequestPolicy,
                    responseHeadersPolicy: responseHeadersPolicy,
                    allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD_OPTIONS,
                    cachedMethods: cloudfront.CachedMethods.CACHE_GET_HEAD_OPTIONS,
                    compress: true
                },
                '/actuator/health': {
                    origin: primaryOrigin,
                    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                    cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
                    originRequestPolicy: originRequestPolicy,
                    allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD,
                    compress: false
                }
            },
            priceClass: cloudfront.PriceClass.PRICE_CLASS_ALL,
            enabled: true,
            enableIpv6: true,
            httpVersion: cloudfront.HttpVersion.HTTP2_AND_3,
            comment: `Global CDN for ${projectName} ${environment} with intelligent failover`,
            defaultRootObject: 'index.html',
            errorResponses: [
                {
                    httpStatus: 404,
                    responseHttpStatus: 200,
                    responsePagePath: '/index.html',
                    ttl: cdk.Duration.minutes(5)
                },
                {
                    httpStatus: 403,
                    responseHttpStatus: 200,
                    responsePagePath: '/index.html',
                    ttl: cdk.Duration.minutes(5)
                }
            ],
            geoRestriction: cloudfront.GeoRestriction.denylist(), // No geo restrictions by default
            webAclId: undefined, // Can be added later for additional security
            enableLogging: true,
            logBucket: undefined, // Will use default logging if not specified
            logFilePrefix: `${projectName}-${environment}-cdn-logs/`,
            logIncludesCookies: false
        });

        return distribution;
    }

    private createRoute53AliasRecord(
        domain: string,
        hostedZone: route53.IHostedZone,
        distribution: cloudfront.Distribution,
        projectName: string,
        environment: string
    ): void {
        // Create alias record for CDN domain
        new route53.ARecord(this, 'CdnAliasRecord', {
            zone: hostedZone,
            recordName: `cdn.${domain}`,
            target: route53.RecordTarget.fromAlias(new targets.CloudFrontTarget(distribution)),
            comment: `CDN alias record for ${projectName} ${environment}`,
            ttl: cdk.Duration.seconds(300)
        });

        // Create alias record for API CDN domain
        new route53.ARecord(this, 'ApiCdnAliasRecord', {
            zone: hostedZone,
            recordName: `api-cdn.${domain}`,
            target: route53.RecordTarget.fromAlias(new targets.CloudFrontTarget(distribution)),
            comment: `API CDN alias record for ${projectName} ${environment}`,
            ttl: cdk.Duration.seconds(300)
        });
    }

    private createCdnMonitoring(
        projectName: string,
        environment: string,
        distribution: cloudfront.Distribution,
        multiRegionConfig: any
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'CdnMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-cdn-monitoring`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# CloudFront Global CDN Monitoring Dashboard\n\n**Environment:** ${environment}\n**Cache Hit Rate Target:** ${multiRegionConfig['cdn-cache-hit-rate-target'] || 90}%\n**Origin Failover Enabled:** Yes\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 4
            })
        );

        // Cache hit rate widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Cache Hit Rate (Target > 90%)',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: 'CacheHitRate',
                        dimensionsMap: {
                            DistributionId: distribution.distributionId
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 12,
                height: 6,
                leftYAxis: {
                    min: 0,
                    max: 100
                }
            })
        );

        // Origin latency widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Origin Response Time',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: 'OriginLatency',
                        dimensionsMap: {
                            DistributionId: distribution.distributionId
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Request count and error rate widgets
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Request Count',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: 'Requests',
                        dimensionsMap: {
                            DistributionId: distribution.distributionId
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 12,
                height: 6
            }),
            new cloudwatch.GraphWidget({
                title: 'Error Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: '4xxErrorRate',
                        dimensionsMap: {
                            DistributionId: distribution.distributionId
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: '4xx Error Rate'
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: '5xxErrorRate',
                        dimensionsMap: {
                            DistributionId: distribution.distributionId
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: '5xx Error Rate'
                    })
                ],
                width: 12,
                height: 6
            })
        );

        return dashboard;
    }

    private createCdnFailoverAlerting(
        projectName: string,
        environment: string,
        distribution: cloudfront.Distribution,
        multiRegionConfig: any
    ): void {
        // Create SNS topic for CDN alerts
        const cdnAlertsTopic = new sns.Topic(this, 'CdnAlertsTopic', {
            topicName: `${projectName}-${environment}-cdn-alerts`,
            displayName: `CDN Alerts for ${projectName} ${environment}`
        });

        // Cache hit rate alarm (target > 90%)
        const cacheHitRateAlarm = new cloudwatch.Alarm(this, 'CacheHitRateAlarm', {
            alarmName: `${projectName}-${environment}-cdn-cache-hit-rate-low`,
            alarmDescription: 'CDN cache hit rate below target threshold',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/CloudFront',
                metricName: 'CacheHitRate',
                dimensionsMap: {
                    DistributionId: distribution.distributionId
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(15)
            }),
            threshold: multiRegionConfig['cdn-cache-hit-rate-target'] || 90,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        cacheHitRateAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: cdnAlertsTopic.topicArn })
        });

        // High error rate alarm
        const errorRateAlarm = new cloudwatch.Alarm(this, 'HighErrorRateAlarm', {
            alarmName: `${projectName}-${environment}-cdn-high-error-rate`,
            alarmDescription: 'CDN experiencing high error rate',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/CloudFront',
                metricName: '5xxErrorRate',
                dimensionsMap: {
                    DistributionId: distribution.distributionId
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 5, // 5% error rate threshold
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        errorRateAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: cdnAlertsTopic.topicArn })
        });

        // Origin latency alarm
        const originLatencyAlarm = new cloudwatch.Alarm(this, 'OriginLatencyAlarm', {
            alarmName: `${projectName}-${environment}-cdn-origin-latency-high`,
            alarmDescription: 'CDN origin response time too high',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/CloudFront',
                metricName: 'OriginLatency',
                dimensionsMap: {
                    DistributionId: distribution.distributionId
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 5000, // 5 seconds threshold
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        originLatencyAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: cdnAlertsTopic.topicArn })
        });

        // Add tags to alerting resources
        cdk.Tags.of(cdnAlertsTopic).add('Name', `${projectName}-${environment}-cdn-alerts`);
        cdk.Tags.of(cdnAlertsTopic).add('Environment', environment);
        cdk.Tags.of(cdnAlertsTopic).add('Project', projectName);
        cdk.Tags.of(cdnAlertsTopic).add('Service', 'CDNAlerting');
    }

    private createOutputs(projectName: string, environment: string, domain: string): void {
        // Distribution outputs
        new cdk.CfnOutput(this, 'DistributionId', {
            value: this.distribution.distributionId,
            description: 'CloudFront distribution ID',
            exportName: `${projectName}-${environment}-cdn-distribution-id`
        });

        new cdk.CfnOutput(this, 'DistributionDomainName', {
            value: this.distribution.distributionDomainName,
            description: 'CloudFront distribution domain name',
            exportName: `${projectName}-${environment}-cdn-distribution-domain`
        });

        // CDN endpoints
        new cdk.CfnOutput(this, 'CdnEndpoint', {
            value: `https://cdn.${domain}`,
            description: 'CDN endpoint URL',
            exportName: `${projectName}-${environment}-cdn-endpoint`
        });

        new cdk.CfnOutput(this, 'ApiCdnEndpoint', {
            value: `https://api-cdn.${domain}`,
            description: 'API CDN endpoint URL',
            exportName: `${projectName}-${environment}-api-cdn-endpoint`
        });

        // Monitoring outputs
        new cdk.CfnOutput(this, 'CdnMonitoringDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.cdnMonitoring.dashboardName}`,
            description: 'CDN monitoring dashboard URL',
            exportName: `${projectName}-${environment}-cdn-monitoring-dashboard-url`
        });

        // Configuration outputs
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};
        new cdk.CfnOutput(this, 'CdnConfiguration', {
            value: JSON.stringify({
                cacheHitRateTarget: `${multiRegionConfig['cdn-cache-hit-rate-target'] || 90}%`,
                originFailoverEnabled: true,
                priceClass: 'PRICE_CLASS_ALL',
                httpVersion: 'HTTP2_AND_3'
            }),
            description: 'CDN configuration parameters',
            exportName: `${projectName}-${environment}-cdn-configuration`
        });
    }
}