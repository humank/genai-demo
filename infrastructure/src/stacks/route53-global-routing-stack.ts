import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface Route53GlobalRoutingStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain: string;
    readonly hostedZone: route53.IHostedZone;
    readonly certificate: certificatemanager.ICertificate;
    readonly regions: {
        primary: {
            region: string;
            loadBalancer: elbv2.IApplicationLoadBalancer;
            weight?: number;
        };
        secondary?: {
            region: string;
            loadBalancer: elbv2.IApplicationLoadBalancer;
            weight?: number;
        };
        tertiary?: {
            region: string;
            loadBalancer: elbv2.IApplicationLoadBalancer;
            weight?: number;
        };
    };
    readonly monitoringConfig?: {
        healthCheckInterval?: number;
        failureThreshold?: number;
        enableABTesting?: boolean;
        enableGeolocationRouting?: boolean;
    };
}

export class Route53GlobalRoutingStack extends cdk.Stack {
    public readonly healthChecks: Map<string, route53.CfnHealthCheck>;
    public readonly geolocationRecords: Map<string, route53.CfnRecordSet>;
    public readonly weightedRecords: Map<string, route53.CfnRecordSet>;
    public readonly latencyRecords: Map<string, route53.CfnRecordSet>;
    public readonly globalMonitoring: cloudwatch.Dashboard;
    public readonly alertingTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: Route53GlobalRoutingStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            domain,
            hostedZone,
            certificate,
            regions,
            monitoringConfig = {}
        } = props;

        // Initialize collections
        this.healthChecks = new Map();
        this.geolocationRecords = new Map();
        this.weightedRecords = new Map();
        this.latencyRecords = new Map();

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Route53GlobalRouting',
            RoutingType: 'Active-Active-MultiRegion'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get configuration with defaults
        // ✅ OPTIMIZATION: Reduced intervals for RTO < 2min target
        // Optimized: 10s interval × 2 failures = 20s detection time (saves 70s vs previous 90s)
        const config = {
            healthCheckInterval: monitoringConfig.healthCheckInterval || 10, // Changed from 30 to 10
            failureThreshold: monitoringConfig.failureThreshold || 2, // Changed from 3 to 2
            enableABTesting: monitoringConfig.enableABTesting || true,
            enableGeolocationRouting: monitoringConfig.enableGeolocationRouting || true
        };

        // Create SNS topic for global routing alerts
        this.alertingTopic = this.createGlobalAlertingTopic(projectName, environment);

        // Create health checks for all regions
        this.createHealthChecks(regions, domain, projectName, environment, config);

        // Create geolocation-based routing records
        if (config.enableGeolocationRouting) {
            this.createGeolocationRouting(
                domain,
                hostedZone,
                regions,
                projectName,
                environment
            );
        }

        // Create weighted routing for A/B testing
        if (config.enableABTesting) {
            this.createWeightedRouting(
                domain,
                hostedZone,
                regions,
                projectName,
                environment
            );
        }

        // Create latency-based routing for optimal performance
        this.createLatencyBasedRouting(
            domain,
            hostedZone,
            regions,
            projectName,
            environment
        );

        // Create global monitoring dashboard
        this.globalMonitoring = this.createGlobalMonitoringDashboard(
            projectName,
            environment,
            regions,
            config
        );

        // Create comprehensive alerting
        this.createGlobalAlerting(projectName, environment, regions, config);

        // Create outputs
        this.createOutputs(projectName, environment, domain, regions);
    }

    private createGlobalAlertingTopic(projectName: string, environment: string): sns.Topic {
        return new sns.Topic(this, 'GlobalRoutingAlertsTopic', {
            topicName: `${projectName}-${environment}-global-routing-alerts`,
            displayName: `Global Routing Alerts for ${projectName} ${environment}`
        });
    }

    private createHealthChecks(
        regions: Route53GlobalRoutingStackProps['regions'],
        domain: string,
        projectName: string,
        environment: string,
        config: any
    ): void {
        // Create health check for primary region
        const primaryHealthCheck = this.createRegionHealthCheck(
            'Primary',
            `api.${domain}`,
            regions.primary.region,
            projectName,
            environment,
            config
        );
        this.healthChecks.set('primary', primaryHealthCheck);

        // Create health check for secondary region if exists
        if (regions.secondary) {
            const secondaryHealthCheck = this.createRegionHealthCheck(
                'Secondary',
                `api-${regions.secondary.region}.${domain}`,
                regions.secondary.region,
                projectName,
                environment,
                config
            );
            this.healthChecks.set('secondary', secondaryHealthCheck);
        }

        // Create health check for tertiary region if exists
        if (regions.tertiary) {
            const tertiaryHealthCheck = this.createRegionHealthCheck(
                'Tertiary',
                `api-${regions.tertiary.region}.${domain}`,
                regions.tertiary.region,
                projectName,
                environment,
                config
            );
            this.healthChecks.set('tertiary', tertiaryHealthCheck);
        }
    }

    private createRegionHealthCheck(
        regionType: string,
        fqdn: string,
        region: string,
        projectName: string,
        environment: string,
        config: any
    ): route53.CfnHealthCheck {
        const healthCheck = new route53.CfnHealthCheck(this, `${regionType}HealthCheck`, {
            healthCheckConfig: {
                type: 'HTTPS',
                fullyQualifiedDomainName: fqdn,
                port: 443,
                resourcePath: '/actuator/health',
                requestInterval: config.healthCheckInterval,
                failureThreshold: config.failureThreshold,
                measureLatency: true,
                enableSni: true,
                regions: [
                    'us-east-1',
                    'us-west-1',
                    'us-west-2',
                    'eu-west-1',
                    'ap-southeast-1',
                    'ap-southeast-2',
                    'ap-northeast-1'
                ]
            },
            healthCheckTags: [
                {
                    key: 'Name',
                    value: `${projectName}-${environment}-${regionType.toLowerCase()}-health-check`
                },
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'Region',
                    value: region
                },
                {
                    key: 'Service',
                    value: 'GlobalRouting'
                }
            ]
        });

        return healthCheck;
    }

    private createGeolocationRouting(
        domain: string,
        hostedZone: route53.IHostedZone,
        regions: Route53GlobalRoutingStackProps['regions'],
        projectName: string,
        environment: string
    ): void {
        const recordName = `api-geo.${domain}`;

        // Primary region - North America
        const primaryGeoRecord = this.createGeolocationRecord(
            'PrimaryGeoRecord',
            recordName,
            hostedZone,
            regions.primary.loadBalancer,
            'NA', // North America
            projectName,
            environment,
            this.healthChecks.get('primary')
        );
        this.geolocationRecords.set('primary', primaryGeoRecord);

        // Secondary region - Europe (if exists)
        if (regions.secondary && this.healthChecks.get('secondary')) {
            const secondaryGeoRecord = this.createGeolocationRecord(
                'SecondaryGeoRecord',
                recordName,
                hostedZone,
                regions.secondary.loadBalancer,
                'EU', // Europe
                projectName,
                environment,
                this.healthChecks.get('secondary')
            );
            this.geolocationRecords.set('secondary', secondaryGeoRecord);
        }

        // Tertiary region - Asia Pacific (if exists)
        if (regions.tertiary && this.healthChecks.get('tertiary')) {
            const tertiaryGeoRecord = this.createGeolocationRecord(
                'TertiaryGeoRecord',
                recordName,
                hostedZone,
                regions.tertiary.loadBalancer,
                'AS', // Asia
                projectName,
                environment,
                this.healthChecks.get('tertiary')
            );
            this.geolocationRecords.set('tertiary', tertiaryGeoRecord);
        }

        // Default record for unmatched locations (routes to primary)
        const defaultGeoRecord = this.createGeolocationRecord(
            'DefaultGeoRecord',
            recordName,
            hostedZone,
            regions.primary.loadBalancer,
            '*', // Default
            projectName,
            environment,
            this.healthChecks.get('primary')
        );
        this.geolocationRecords.set('default', defaultGeoRecord);
    }

    private createGeolocationRecord(
        recordId: string,
        recordName: string,
        hostedZone: route53.IHostedZone,
        loadBalancer: elbv2.IApplicationLoadBalancer,
        geoLocation: string,
        projectName: string,
        environment: string,
        healthCheck?: route53.CfnHealthCheck
    ): route53.CfnRecordSet {
        const record = new route53.ARecord(this, recordId, {
            zone: hostedZone,
            recordName: recordName,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
            comment: `Geolocation routing record for ${geoLocation}`,
            ttl: cdk.Duration.seconds(60)
        });

        const cfnRecord = record.node.defaultChild as route53.CfnRecordSet;
        cfnRecord.setIdentifier = `${projectName}-${environment}-geo-${geoLocation.toLowerCase()}`;
        
        if (geoLocation === '*') {
            cfnRecord.geoLocation = { continentCode: '*' };
        } else {
            cfnRecord.geoLocation = { continentCode: geoLocation };
        }

        if (healthCheck) {
            cfnRecord.healthCheckId = healthCheck.attrHealthCheckId;
        }

        return cfnRecord;
    }

    private createWeightedRouting(
        domain: string,
        hostedZone: route53.IHostedZone,
        regions: Route53GlobalRoutingStackProps['regions'],
        projectName: string,
        environment: string
    ): void {
        const recordName = `api-weighted.${domain}`;

        // Primary region weighted record
        const primaryWeight = regions.primary.weight || 70;
        const primaryWeightedRecord = this.createWeightedRecord(
            'PrimaryWeightedRecord',
            recordName,
            hostedZone,
            regions.primary.loadBalancer,
            primaryWeight,
            projectName,
            environment,
            'primary',
            this.healthChecks.get('primary')
        );
        this.weightedRecords.set('primary', primaryWeightedRecord);

        // Secondary region weighted record (if exists)
        if (regions.secondary && this.healthChecks.get('secondary')) {
            const secondaryWeight = regions.secondary.weight || 20;
            const secondaryWeightedRecord = this.createWeightedRecord(
                'SecondaryWeightedRecord',
                recordName,
                hostedZone,
                regions.secondary.loadBalancer,
                secondaryWeight,
                projectName,
                environment,
                'secondary',
                this.healthChecks.get('secondary')
            );
            this.weightedRecords.set('secondary', secondaryWeightedRecord);
        }

        // Tertiary region weighted record (if exists)
        if (regions.tertiary && this.healthChecks.get('tertiary')) {
            const tertiaryWeight = regions.tertiary.weight || 10;
            const tertiaryWeightedRecord = this.createWeightedRecord(
                'TertiaryWeightedRecord',
                recordName,
                hostedZone,
                regions.tertiary.loadBalancer,
                tertiaryWeight,
                projectName,
                environment,
                'tertiary',
                this.healthChecks.get('tertiary')
            );
            this.weightedRecords.set('tertiary', tertiaryWeightedRecord);
        }
    }

    private createWeightedRecord(
        recordId: string,
        recordName: string,
        hostedZone: route53.IHostedZone,
        loadBalancer: elbv2.IApplicationLoadBalancer,
        weight: number,
        projectName: string,
        environment: string,
        regionType: string,
        healthCheck?: route53.CfnHealthCheck
    ): route53.CfnRecordSet {
        const record = new route53.ARecord(this, recordId, {
            zone: hostedZone,
            recordName: recordName,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
            comment: `Weighted routing record for ${regionType} region (weight: ${weight})`,
            ttl: cdk.Duration.seconds(60)
        });

        const cfnRecord = record.node.defaultChild as route53.CfnRecordSet;
        cfnRecord.setIdentifier = `${projectName}-${environment}-weighted-${regionType}`;
        cfnRecord.weight = weight;

        if (healthCheck) {
            cfnRecord.healthCheckId = healthCheck.attrHealthCheckId;
        }

        return cfnRecord;
    }

    private createLatencyBasedRouting(
        domain: string,
        hostedZone: route53.IHostedZone,
        regions: Route53GlobalRoutingStackProps['regions'],
        projectName: string,
        environment: string
    ): void {
        const recordName = `api-latency.${domain}`;

        // Primary region latency record
        const primaryLatencyRecord = this.createLatencyRecord(
            'PrimaryLatencyRecord',
            recordName,
            hostedZone,
            regions.primary.loadBalancer,
            regions.primary.region,
            projectName,
            environment,
            'primary',
            this.healthChecks.get('primary')
        );
        this.latencyRecords.set('primary', primaryLatencyRecord);

        // Secondary region latency record (if exists)
        if (regions.secondary && this.healthChecks.get('secondary')) {
            const secondaryLatencyRecord = this.createLatencyRecord(
                'SecondaryLatencyRecord',
                recordName,
                hostedZone,
                regions.secondary.loadBalancer,
                regions.secondary.region,
                projectName,
                environment,
                'secondary',
                this.healthChecks.get('secondary')
            );
            this.latencyRecords.set('secondary', secondaryLatencyRecord);
        }

        // Tertiary region latency record (if exists)
        if (regions.tertiary && this.healthChecks.get('tertiary')) {
            const tertiaryLatencyRecord = this.createLatencyRecord(
                'TertiaryLatencyRecord',
                recordName,
                hostedZone,
                regions.tertiary.loadBalancer,
                regions.tertiary.region,
                projectName,
                environment,
                'tertiary',
                this.healthChecks.get('tertiary')
            );
            this.latencyRecords.set('tertiary', tertiaryLatencyRecord);
        }
    }

    private createLatencyRecord(
        recordId: string,
        recordName: string,
        hostedZone: route53.IHostedZone,
        loadBalancer: elbv2.IApplicationLoadBalancer,
        region: string,
        projectName: string,
        environment: string,
        regionType: string,
        healthCheck?: route53.CfnHealthCheck
    ): route53.CfnRecordSet {
        const record = new route53.ARecord(this, recordId, {
            zone: hostedZone,
            recordName: recordName,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
            comment: `Latency-based routing record for ${regionType} region (${region})`,
            ttl: cdk.Duration.seconds(300)
        });

        const cfnRecord = record.node.defaultChild as route53.CfnRecordSet;
        cfnRecord.setIdentifier = `${projectName}-${environment}-latency-${regionType}`;
        cfnRecord.region = region;

        if (healthCheck) {
            cfnRecord.healthCheckId = healthCheck.attrHealthCheckId;
        }

        return cfnRecord;
    }

    private createGlobalMonitoringDashboard(
        projectName: string,
        environment: string,
        regions: Route53GlobalRoutingStackProps['regions'],
        config: any
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'GlobalRoutingMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-global-routing-monitoring`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# Global Route 53 Routing Monitoring Dashboard\n\n**Environment:** ${environment}\n**Routing Types:** Geolocation, Weighted (A/B Testing), Latency-based\n**Health Check Interval:** ${config.healthCheckInterval} seconds\n**Failure Threshold:** ${config.failureThreshold} failures\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 4
            })
        );

        // Add health check status widgets for all regions
        const healthCheckWidgets = this.createHealthCheckWidgets(regions);
        dashboard.addWidgets(...healthCheckWidgets);

        // Add DNS query metrics
        const dnsQueryWidgets = this.createDnsQueryWidgets(projectName, environment);
        dashboard.addWidgets(...dnsQueryWidgets);

        // Add latency comparison widgets
        const latencyWidgets = this.createLatencyComparisonWidgets(regions);
        dashboard.addWidgets(...latencyWidgets);

        // Add traffic distribution widgets
        const trafficWidgets = this.createTrafficDistributionWidgets(projectName, environment);
        dashboard.addWidgets(...trafficWidgets);

        return dashboard;
    }

    private createHealthCheckWidgets(
        regions: Route53GlobalRoutingStackProps['regions']
    ): cloudwatch.IWidget[] {
        const widgets: cloudwatch.IWidget[] = [];

        // Primary region health check
        const primaryHealthCheck = this.healthChecks.get('primary');
        if (primaryHealthCheck) {
            widgets.push(
                new cloudwatch.GraphWidget({
                    title: 'Primary Region Health Check Status',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/Route53',
                            metricName: 'HealthCheckStatus',
                            dimensionsMap: {
                                HealthCheckId: primaryHealthCheck.attrHealthCheckId
                            },
                            statistic: 'Average',
                            period: cdk.Duration.minutes(1),
                            label: `Primary (${regions.primary.region})`
                        })
                    ],
                    width: 8,
                    height: 6
                })
            );
        }

        // Secondary region health check
        const secondaryHealthCheck = this.healthChecks.get('secondary');
        if (secondaryHealthCheck && regions.secondary) {
            widgets.push(
                new cloudwatch.GraphWidget({
                    title: 'Secondary Region Health Check Status',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/Route53',
                            metricName: 'HealthCheckStatus',
                            dimensionsMap: {
                                HealthCheckId: secondaryHealthCheck.attrHealthCheckId
                            },
                            statistic: 'Average',
                            period: cdk.Duration.minutes(1),
                            label: `Secondary (${regions.secondary.region})`
                        })
                    ],
                    width: 8,
                    height: 6
                })
            );
        }

        // Tertiary region health check
        const tertiaryHealthCheck = this.healthChecks.get('tertiary');
        if (tertiaryHealthCheck && regions.tertiary) {
            widgets.push(
                new cloudwatch.GraphWidget({
                    title: 'Tertiary Region Health Check Status',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/Route53',
                            metricName: 'HealthCheckStatus',
                            dimensionsMap: {
                                HealthCheckId: tertiaryHealthCheck.attrHealthCheckId
                            },
                            statistic: 'Average',
                            period: cdk.Duration.minutes(1),
                            label: `Tertiary (${regions.tertiary.region})`
                        })
                    ],
                    width: 8,
                    height: 6
                })
            );
        }

        return widgets;
    }

    private createDnsQueryWidgets(projectName: string, environment: string): cloudwatch.IWidget[] {
        return [
            new cloudwatch.GraphWidget({
                title: 'DNS Query Count by Routing Type',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Route53',
                        metricName: 'QueryCount',
                        dimensionsMap: {
                            HostedZoneId: 'Z123456789', // This would be dynamically set
                            RecordName: `api-geo.${projectName}-${environment}.com`
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Geolocation Queries'
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/Route53',
                        metricName: 'QueryCount',
                        dimensionsMap: {
                            HostedZoneId: 'Z123456789', // This would be dynamically set
                            RecordName: `api-weighted.${projectName}-${environment}.com`
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Weighted Queries (A/B Testing)'
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/Route53',
                        metricName: 'QueryCount',
                        dimensionsMap: {
                            HostedZoneId: 'Z123456789', // This would be dynamically set
                            RecordName: `api-latency.${projectName}-${environment}.com`
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Latency-based Queries'
                    })
                ],
                width: 12,
                height: 6
            })
        ];
    }

    private createLatencyComparisonWidgets(
        regions: Route53GlobalRoutingStackProps['regions']
    ): cloudwatch.IWidget[] {
        const widgets: cloudwatch.IWidget[] = [];

        // Health check latency comparison
        const latencyMetrics: cloudwatch.Metric[] = [];

        const primaryHealthCheck = this.healthChecks.get('primary');
        if (primaryHealthCheck) {
            latencyMetrics.push(
                new cloudwatch.Metric({
                    namespace: 'AWS/Route53',
                    metricName: 'ConnectionTime',
                    dimensionsMap: {
                        HealthCheckId: primaryHealthCheck.attrHealthCheckId
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(1),
                    label: `Primary (${regions.primary.region})`
                })
            );
        }

        const secondaryHealthCheck = this.healthChecks.get('secondary');
        if (secondaryHealthCheck && regions.secondary) {
            latencyMetrics.push(
                new cloudwatch.Metric({
                    namespace: 'AWS/Route53',
                    metricName: 'ConnectionTime',
                    dimensionsMap: {
                        HealthCheckId: secondaryHealthCheck.attrHealthCheckId
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(1),
                    label: `Secondary (${regions.secondary.region})`
                })
            );
        }

        const tertiaryHealthCheck = this.healthChecks.get('tertiary');
        if (tertiaryHealthCheck && regions.tertiary) {
            latencyMetrics.push(
                new cloudwatch.Metric({
                    namespace: 'AWS/Route53',
                    metricName: 'ConnectionTime',
                    dimensionsMap: {
                        HealthCheckId: tertiaryHealthCheck.attrHealthCheckId
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(1),
                    label: `Tertiary (${regions.tertiary.region})`
                })
            );
        }

        if (latencyMetrics.length > 0) {
            widgets.push(
                new cloudwatch.GraphWidget({
                    title: 'Health Check Response Times Comparison',
                    left: latencyMetrics,
                    width: 24,
                    height: 6
                })
            );
        }

        return widgets;
    }

    private createTrafficDistributionWidgets(projectName: string, environment: string): cloudwatch.IWidget[] {
        return [
            new cloudwatch.GraphWidget({
                title: 'Traffic Distribution by Region',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/ApplicationELB',
                        metricName: 'RequestCount',
                        dimensionsMap: {
                            LoadBalancer: 'app/primary-alb/1234567890123456'
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Primary Region Requests'
                    })
                ],
                width: 12,
                height: 6
            }),
            new cloudwatch.GraphWidget({
                title: 'Global Response Time P95',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/ApplicationELB',
                        metricName: 'TargetResponseTime',
                        dimensionsMap: {
                            LoadBalancer: 'app/primary-alb/1234567890123456'
                        },
                        statistic: 'p95',
                        period: cdk.Duration.minutes(5),
                        label: 'Primary Region P95'
                    })
                ],
                width: 12,
                height: 6
            })
        ];
    }

    private createGlobalAlerting(
        projectName: string,
        environment: string,
        regions: Route53GlobalRoutingStackProps['regions'],
        config: any
    ): void {
        // Create alarms for each region's health check
        this.createRegionHealthCheckAlarms('Primary', regions.primary.region, projectName, environment, config);

        if (regions.secondary) {
            this.createRegionHealthCheckAlarms('Secondary', regions.secondary.region, projectName, environment, config);
        }

        if (regions.tertiary) {
            this.createRegionHealthCheckAlarms('Tertiary', regions.tertiary.region, projectName, environment, config);
        }

        // Create composite alarm for global system health
        this.createGlobalSystemHealthAlarm(projectName, environment, regions);

        // Create DNS query rate alarm
        this.createDnsQueryRateAlarm(projectName, environment);
    }

    private createRegionHealthCheckAlarms(
        regionType: string,
        region: string,
        projectName: string,
        environment: string,
        config: any
    ): void {
        const healthCheck = this.healthChecks.get(regionType.toLowerCase());
        if (!healthCheck) return;

        // Health check failure alarm
        const healthCheckAlarm = new cloudwatch.Alarm(this, `${regionType}HealthCheckFailureAlarm`, {
            alarmName: `${projectName}-${environment}-${regionType.toLowerCase()}-health-check-failure`,
            alarmDescription: `${regionType} region (${region}) health check failure - triggers traffic rerouting`,
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Route53',
                metricName: 'HealthCheckStatus',
                dimensionsMap: {
                    HealthCheckId: healthCheck.attrHealthCheckId
                },
                statistic: 'Minimum',
                period: cdk.Duration.minutes(1)
            }),
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: config.failureThreshold,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        healthCheckAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: this.alertingTopic.topicArn })
        });

        // High latency alarm
        const latencyAlarm = new cloudwatch.Alarm(this, `${regionType}HighLatencyAlarm`, {
            alarmName: `${projectName}-${environment}-${regionType.toLowerCase()}-high-latency`,
            alarmDescription: `${regionType} region (${region}) experiencing high latency`,
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Route53',
                metricName: 'ConnectionTime',
                dimensionsMap: {
                    HealthCheckId: healthCheck.attrHealthCheckId
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 2000, // 2 seconds
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        latencyAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: this.alertingTopic.topicArn })
        });
    }

    private createGlobalSystemHealthAlarm(
        projectName: string,
        environment: string,
        regions: Route53GlobalRoutingStackProps['regions']
    ): void {
        // This would be a composite alarm that triggers when multiple regions are down
        // For now, we'll create a simple alarm based on primary region
        const primaryHealthCheck = this.healthChecks.get('primary');
        if (!primaryHealthCheck) return;

        const globalHealthAlarm = new cloudwatch.Alarm(this, 'GlobalSystemHealthAlarm', {
            alarmName: `${projectName}-${environment}-global-system-health`,
            alarmDescription: 'Global system health - critical when primary region is down',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Route53',
                metricName: 'HealthCheckStatus',
                dimensionsMap: {
                    HealthCheckId: primaryHealthCheck.attrHealthCheckId
                },
                statistic: 'Minimum',
                period: cdk.Duration.minutes(1)
            }),
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        globalHealthAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: this.alertingTopic.topicArn })
        });
    }

    private createDnsQueryRateAlarm(projectName: string, environment: string): void {
        // Alarm for unusually high DNS query rates (potential DDoS)
        const highQueryRateAlarm = new cloudwatch.Alarm(this, 'HighDnsQueryRateAlarm', {
            alarmName: `${projectName}-${environment}-high-dns-query-rate`,
            alarmDescription: 'Unusually high DNS query rate detected',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Route53',
                metricName: 'QueryCount',
                statistic: 'Sum',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 10000, // 10k queries per 5 minutes
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        highQueryRateAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: this.alertingTopic.topicArn })
        });
    }

    private createOutputs(
        projectName: string,
        environment: string,
        domain: string,
        regions: Route53GlobalRoutingStackProps['regions']
    ): void {
        // Health Check outputs
        this.healthChecks.forEach((healthCheck, regionType) => {
            new cdk.CfnOutput(this, `${regionType.charAt(0).toUpperCase() + regionType.slice(1)}HealthCheckId`, {
                value: healthCheck.attrHealthCheckId,
                description: `${regionType} region Route 53 health check ID`,
                exportName: `${projectName}-${environment}-${regionType}-health-check-id`
            });
        });

        // DNS endpoint outputs
        new cdk.CfnOutput(this, 'GeolocationDnsName', {
            value: `api-geo.${domain}`,
            description: 'Geolocation-based routing DNS name for API endpoint',
            exportName: `${projectName}-${environment}-geolocation-dns-name`
        });

        new cdk.CfnOutput(this, 'WeightedDnsName', {
            value: `api-weighted.${domain}`,
            description: 'Weighted routing DNS name for A/B testing',
            exportName: `${projectName}-${environment}-weighted-dns-name`
        });

        new cdk.CfnOutput(this, 'LatencyBasedDnsName', {
            value: `api-latency.${domain}`,
            description: 'Latency-based routing DNS name for optimal performance',
            exportName: `${projectName}-${environment}-latency-dns-name`
        });

        // Monitoring outputs
        new cdk.CfnOutput(this, 'GlobalMonitoringDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.globalMonitoring.dashboardName}`,
            description: 'Global routing monitoring dashboard URL',
            exportName: `${projectName}-${environment}-global-monitoring-dashboard-url`
        });

        new cdk.CfnOutput(this, 'GlobalAlertingTopicArn', {
            value: this.alertingTopic.topicArn,
            description: 'SNS topic ARN for global routing alerts',
            exportName: `${projectName}-${environment}-global-alerting-topic-arn`
        });

        // Configuration outputs
        new cdk.CfnOutput(this, 'GlobalRoutingConfiguration', {
            value: JSON.stringify({
                primaryRegion: regions.primary.region,
                secondaryRegion: regions.secondary?.region || 'N/A',
                tertiaryRegion: regions.tertiary?.region || 'N/A',
                healthCheckInterval: '30 seconds',
                failureThreshold: 3,
                routingTypes: ['geolocation', 'weighted', 'latency-based'],
                abTestingEnabled: true,
                geolocationEnabled: true
            }),
            description: 'Global routing configuration parameters',
            exportName: `${projectName}-${environment}-global-routing-configuration`
        });

        // Traffic distribution outputs
        const primaryWeight = regions.primary.weight || 70;
        const secondaryWeight = regions.secondary?.weight || 20;
        const tertiaryWeight = regions.tertiary?.weight || 10;

        new cdk.CfnOutput(this, 'TrafficDistribution', {
            value: JSON.stringify({
                primary: `${primaryWeight}%`,
                secondary: regions.secondary ? `${secondaryWeight}%` : '0%',
                tertiary: regions.tertiary ? `${tertiaryWeight}%` : '0%'
            }),
            description: 'Current traffic distribution weights for A/B testing',
            exportName: `${projectName}-${environment}-traffic-distribution`
        });
    }
}