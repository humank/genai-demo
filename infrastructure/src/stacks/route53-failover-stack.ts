import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface Route53FailoverStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain: string;
    readonly hostedZone: route53.IHostedZone;
    readonly primaryLoadBalancer: elbv2.IApplicationLoadBalancer;
    readonly secondaryLoadBalancer?: elbv2.IApplicationLoadBalancer;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
}

export class Route53FailoverStack extends cdk.Stack {
    public readonly primaryHealthCheck?: route53.CfnHealthCheck;
    public readonly secondaryHealthCheck?: route53.CfnHealthCheck;
    public readonly primaryFailoverRecord: route53.CfnRecordSet;
    public readonly secondaryFailoverRecord?: route53.CfnRecordSet;
    public readonly failoverMonitoring: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: Route53FailoverStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            domain,
            hostedZone,
            primaryLoadBalancer,
            secondaryLoadBalancer,
            primaryRegion,
            secondaryRegion
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Route53Failover',
            PrimaryRegion: primaryRegion,
            SecondaryRegion: secondaryRegion
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get multi-region configuration
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Create health checks for both regions
        this.primaryHealthCheck = this.createHealthCheck(
            'Primary',
            `api.${domain}`,
            projectName,
            environment,
            multiRegionConfig
        );

        if (secondaryLoadBalancer) {
            this.secondaryHealthCheck = this.createHealthCheck(
                'Secondary',
                `api-dr.${domain}`,
                projectName,
                environment,
                multiRegionConfig
            );
        }

        // Create failover DNS records
        this.primaryFailoverRecord = this.createFailoverRecord(
            'Primary',
            `api.${domain}`,
            hostedZone,
            primaryLoadBalancer,
            projectName,
            environment,
            this.primaryHealthCheck
        );

        if (secondaryLoadBalancer && this.secondaryHealthCheck) {
            this.secondaryFailoverRecord = this.createFailoverRecord(
                'Secondary',
                `api.${domain}`,
                hostedZone,
                secondaryLoadBalancer,
                projectName,
                environment,
                this.secondaryHealthCheck
            );
        }

        // Create latency-based routing records for optimal performance
        this.createLatencyBasedRouting(
            domain,
            hostedZone,
            primaryLoadBalancer,
            secondaryLoadBalancer,
            primaryRegion,
            secondaryRegion,
            projectName,
            environment
        );

        // Create monitoring dashboard for failover status
        this.failoverMonitoring = this.createFailoverMonitoring(
            projectName,
            environment,
            multiRegionConfig
        );

        // Create alerting for failover events
        this.createFailoverAlerting(projectName, environment, multiRegionConfig);

        // Create outputs
        this.createOutputs(projectName, environment, domain);
    }

    private createHealthCheck(
        regionType: 'Primary' | 'Secondary',
        fqdn: string,
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): route53.CfnHealthCheck {
        const healthCheckInterval = multiRegionConfig['health-check-interval'] || 30;
        const failureThreshold = multiRegionConfig['health-check-failure-threshold'] || 3;

        const healthCheck = new route53.CfnHealthCheck(this, `${regionType}HealthCheck`, {
            healthCheckConfig: {
                type: 'HTTPS',
                fullyQualifiedDomainName: fqdn,
                port: 443,
                resourcePath: '/actuator/health',
                requestInterval: healthCheckInterval,
                failureThreshold: failureThreshold
            }
        });

        return healthCheck;
    }

    private createFailoverRecord(
        regionType: 'Primary' | 'Secondary',
        recordName: string,
        hostedZone: route53.IHostedZone,
        loadBalancer: elbv2.IApplicationLoadBalancer,
        projectName: string,
        environment: string,
        healthCheck?: route53.CfnHealthCheck
    ): route53.CfnRecordSet {
        const failoverType = regionType === 'Primary' ? 'PRIMARY' : 'SECONDARY';
        const setIdentifier = `${projectName}-${environment}-${regionType.toLowerCase()}-failover`;

        // Create alias record for failover routing
        const failoverRecord = new route53.ARecord(this, `${regionType}FailoverRecord`, {
            zone: hostedZone,
            recordName: recordName,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
            comment: `${regionType} failover record for ${recordName}`,
            ttl: cdk.Duration.seconds(60) // Short TTL for faster failover
        });

        // Create the underlying CfnRecordSet for advanced failover configuration
        const cfnRecord = failoverRecord.node.defaultChild as route53.CfnRecordSet;
        cfnRecord.setIdentifier = setIdentifier;
        cfnRecord.failover = failoverType;

        // Only add health check to primary record if available
        if (regionType === 'Primary' && healthCheck) {
            cfnRecord.healthCheckId = healthCheck.attrHealthCheckId;
        }

        return cfnRecord;
    }

    private createLatencyBasedRouting(
        domain: string,
        hostedZone: route53.IHostedZone,
        primaryLoadBalancer: elbv2.IApplicationLoadBalancer,
        secondaryLoadBalancer: elbv2.IApplicationLoadBalancer | undefined,
        primaryRegion: string,
        secondaryRegion: string,
        projectName: string,
        environment: string
    ): void {
        // Create latency-based routing for optimal performance when both regions are healthy
        const latencyRecordName = `api-latency.${domain}`;

        // Primary region latency record
        const primaryLatencyRecord = new route53.ARecord(this, 'PrimaryLatencyRecord', {
            zone: hostedZone,
            recordName: latencyRecordName,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(primaryLoadBalancer)),
            comment: `Primary region latency-based routing record`,
            ttl: cdk.Duration.seconds(300)
        });

        const primaryCfnLatencyRecord = primaryLatencyRecord.node.defaultChild as route53.CfnRecordSet;
        primaryCfnLatencyRecord.setIdentifier = `${projectName}-${environment}-primary-latency`;
        primaryCfnLatencyRecord.region = primaryRegion;
        if (this.primaryHealthCheck) {
            primaryCfnLatencyRecord.healthCheckId = this.primaryHealthCheck.attrHealthCheckId;
        }

        // Secondary region latency record (if available)
        if (secondaryLoadBalancer && this.secondaryHealthCheck) {
            const secondaryLatencyRecord = new route53.ARecord(this, 'SecondaryLatencyRecord', {
                zone: hostedZone,
                recordName: latencyRecordName,
                target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(secondaryLoadBalancer)),
                comment: `Secondary region latency-based routing record`,
                ttl: cdk.Duration.seconds(300)
            });

            const secondaryCfnLatencyRecord = secondaryLatencyRecord.node.defaultChild as route53.CfnRecordSet;
            secondaryCfnLatencyRecord.setIdentifier = `${projectName}-${environment}-secondary-latency`;
            secondaryCfnLatencyRecord.region = secondaryRegion;
            secondaryCfnLatencyRecord.healthCheckId = this.secondaryHealthCheck.attrHealthCheckId;
        }
    }

    private createFailoverMonitoring(
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'FailoverMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-failover-monitoring`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# Route 53 Failover Monitoring Dashboard\n\n**Environment:** ${environment}\n**RTO Target:** ${multiRegionConfig['failover-rto-minutes'] || 1} minutes\n**RPO Target:** ${multiRegionConfig['failover-rpo-minutes'] || 0} minutes\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 4
            })
        );

        // Add health check status widgets
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Primary Region Health Check Status',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Route53',
                        metricName: 'HealthCheckStatus',
                        dimensionsMap: {
                            HealthCheckId: this.primaryHealthCheck?.attrHealthCheckId || 'N/A'
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(1)
                    })
                ],
                width: 12,
                height: 6
            })
        );

        if (this.secondaryHealthCheck) {
            dashboard.addWidgets(
                new cloudwatch.GraphWidget({
                    title: 'Secondary Region Health Check Status',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/Route53',
                            metricName: 'HealthCheckStatus',
                            dimensionsMap: {
                                HealthCheckId: this.secondaryHealthCheck.attrHealthCheckId
                            },
                            statistic: 'Average',
                            period: cdk.Duration.minutes(1)
                        })
                    ],
                    width: 12,
                    height: 6
                })
            );
        }

        // Add health check latency widgets
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Health Check Response Times',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Route53',
                        metricName: 'ConnectionTime',
                        dimensionsMap: {
                            HealthCheckId: this.primaryHealthCheck?.attrHealthCheckId || 'N/A'
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(1),
                        label: 'Primary Region'
                    })
                ],
                ...(this.secondaryHealthCheck && {
                    right: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/Route53',
                            metricName: 'ConnectionTime',
                            dimensionsMap: {
                                HealthCheckId: this.secondaryHealthCheck.attrHealthCheckId
                            },
                            statistic: 'Average',
                            period: cdk.Duration.minutes(1),
                            label: 'Secondary Region'
                        })
                    ]
                }),
                width: 24,
                height: 6
            })
        );

        return dashboard;
    }

    private createFailoverAlerting(
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): void {
        // Create SNS topic for failover alerts
        const failoverAlertsTopic = new sns.Topic(this, 'FailoverAlertsTopic', {
            topicName: `${projectName}-${environment}-failover-alerts`,
            displayName: `Failover Alerts for ${projectName} ${environment}`
        });

        // Primary region health check failure alarm
        const primaryHealthCheckAlarm = new cloudwatch.Alarm(this, 'PrimaryHealthCheckFailureAlarm', {
            alarmName: `${projectName}-${environment}-primary-health-check-failure`,
            alarmDescription: 'Primary region health check failure - triggers failover',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Route53',
                metricName: 'HealthCheckStatus',
                dimensionsMap: {
                    HealthCheckId: this.primaryHealthCheck?.attrHealthCheckId || 'N/A'
                },
                statistic: 'Minimum',
                period: cdk.Duration.minutes(1)
            }),
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: multiRegionConfig['health-check-failure-threshold'] || 3,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        primaryHealthCheckAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: failoverAlertsTopic.topicArn })
        });

        // Secondary region health check failure alarm (if exists)
        if (this.secondaryHealthCheck) {
            const secondaryHealthCheckAlarm = new cloudwatch.Alarm(this, 'SecondaryHealthCheckFailureAlarm', {
                alarmName: `${projectName}-${environment}-secondary-health-check-failure`,
                alarmDescription: 'Secondary region health check failure - both regions down',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Route53',
                    metricName: 'HealthCheckStatus',
                    dimensionsMap: {
                        HealthCheckId: this.secondaryHealthCheck.attrHealthCheckId
                    },
                    statistic: 'Minimum',
                    period: cdk.Duration.minutes(1)
                }),
                threshold: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
                evaluationPeriods: multiRegionConfig['health-check-failure-threshold'] || 3,
                treatMissingData: cloudwatch.TreatMissingData.BREACHING
            });

            secondaryHealthCheckAlarm.addAlarmAction({
                bind: () => ({ alarmActionArn: failoverAlertsTopic.topicArn })
            });
        }

        // Add tags to alerting resources
        cdk.Tags.of(failoverAlertsTopic).add('Name', `${projectName}-${environment}-failover-alerts`);
        cdk.Tags.of(failoverAlertsTopic).add('Environment', environment);
        cdk.Tags.of(failoverAlertsTopic).add('Project', projectName);
        cdk.Tags.of(failoverAlertsTopic).add('Service', 'FailoverAlerting');
    }

    private createOutputs(projectName: string, environment: string, domain: string): void {
        // Health Check outputs
        if (this.primaryHealthCheck) {
            new cdk.CfnOutput(this, 'PrimaryHealthCheckId', {
                value: this.primaryHealthCheck.attrHealthCheckId,
                description: 'Primary region Route 53 health check ID',
                exportName: `${projectName}-${environment}-primary-health-check-id`
            });
        }

        if (this.secondaryHealthCheck) {
            new cdk.CfnOutput(this, 'SecondaryHealthCheckId', {
                value: this.secondaryHealthCheck.attrHealthCheckId,
                description: 'Secondary region Route 53 health check ID',
                exportName: `${projectName}-${environment}-secondary-health-check-id`
            });
        }

        // Failover DNS record outputs
        new cdk.CfnOutput(this, 'FailoverDnsName', {
            value: `api.${domain}`,
            description: 'Failover DNS name for API endpoint',
            exportName: `${projectName}-${environment}-failover-dns-name`
        });

        new cdk.CfnOutput(this, 'LatencyBasedDnsName', {
            value: `api-latency.${domain}`,
            description: 'Latency-based routing DNS name for API endpoint',
            exportName: `${projectName}-${environment}-latency-dns-name`
        });

        // Monitoring outputs
        new cdk.CfnOutput(this, 'FailoverMonitoringDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.failoverMonitoring.dashboardName}`,
            description: 'Failover monitoring dashboard URL',
            exportName: `${projectName}-${environment}-failover-monitoring-dashboard-url`
        });

        // Configuration outputs
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};
        new cdk.CfnOutput(this, 'FailoverConfiguration', {
            value: JSON.stringify({
                rto: `${multiRegionConfig['failover-rto-minutes'] || 1} minutes`,
                rpo: `${multiRegionConfig['failover-rpo-minutes'] || 0} minutes`,
                healthCheckInterval: `${multiRegionConfig['health-check-interval'] || 30} seconds`,
                failureThreshold: multiRegionConfig['health-check-failure-threshold'] || 3
            }),
            description: 'Failover configuration parameters',
            exportName: `${projectName}-${environment}-failover-configuration`
        });
    }
}