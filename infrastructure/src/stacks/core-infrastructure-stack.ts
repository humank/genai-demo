import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { Construct } from 'constructs';

export interface CoreInfrastructureStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    securityGroups: {
        alb: ec2.SecurityGroup;
        app: ec2.SecurityGroup;
        database: ec2.SecurityGroup;
    };
    kmsKey: kms.Key;
    environment: string;
    projectName: string;
    region?: string;
    isPrimaryRegion?: boolean;
}

export class CoreInfrastructureStack extends cdk.Stack {
    public readonly loadBalancer: elbv2.ApplicationLoadBalancer;
    public readonly crossRegionTargetGroup: elbv2.ApplicationTargetGroup;
    public readonly primaryTargetGroup: elbv2.ApplicationTargetGroup;
    public readonly secondaryTargetGroup: elbv2.ApplicationTargetGroup;
    public readonly region: string;
    public readonly isPrimaryRegion: boolean;

    constructor(scope: Construct, id: string, props: CoreInfrastructureStackProps) {
        super(scope, id, props);

        const { environment, projectName, region, isPrimaryRegion = true } = props;
        
        // Store region and primary region flag for cross-region configuration
        this.region = region || 'ap-east-2';
        this.isPrimaryRegion = isPrimaryRegion;

        // Apply common tags for cross-region identification
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'CoreInfrastructure');
        cdk.Tags.of(this).add('Region', this.region);
        cdk.Tags.of(this).add('RegionType', isPrimaryRegion ? 'Primary' : 'Secondary');
        cdk.Tags.of(this).add('CrossRegionEnabled', 'true');

        // Create enhanced Application Load Balancer with cross-region support
        this.loadBalancer = this.createCrossRegionALB(props, projectName, environment);

        // Create cross-region target groups with intelligent routing
        // Primary target group for local region traffic
        this.primaryTargetGroup = new elbv2.ApplicationTargetGroup(this, 'PrimaryTargetGroup', {
            targetGroupName: `${projectName}-${environment}-pri-${this.region}`,
            vpc: props.vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/health',
                port: '8080',
                protocol: elbv2.Protocol.HTTP,
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(10),
                interval: cdk.Duration.seconds(30)
            }
        });

        // Secondary target group for failover scenarios
        this.secondaryTargetGroup = new elbv2.ApplicationTargetGroup(this, 'SecondaryTargetGroup', {
            targetGroupName: `${projectName}-${environment}-sec-${this.region}`,
            vpc: props.vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/health',
                port: '8080',
                protocol: elbv2.Protocol.HTTP,
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(10),
                interval: cdk.Duration.seconds(30)
            }
        });

        // Cross-region target group for inter-region traffic
        this.crossRegionTargetGroup = new elbv2.ApplicationTargetGroup(this, 'CrossRegionTargetGroup', {
            targetGroupName: `${projectName}-${environment}-xr-${this.region}`,
            vpc: props.vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/health',
                port: '8080',
                protocol: elbv2.Protocol.HTTP,
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(10),
                interval: cdk.Duration.seconds(30)
            }
        });

        // Configure cross-region routing with intelligent load balancing
        this.configureCrossRegionRouting(props, projectName, environment);

        // Integrate monitoring systems for regional capacity monitoring
        this.integrateMonitoringSystems(props, projectName, environment);

        // Configure traffic overflow alerting mechanisms
        this.configureTrafficOverflowAlerting(props, projectName, environment);

        // Outputs
        new cdk.CfnOutput(this, 'LoadBalancerDNS', {
            value: this.loadBalancer.loadBalancerDnsName,
            exportName: `${this.stackName}-LoadBalancerDNS`,
        });

        new cdk.CfnOutput(this, 'LoadBalancerArn', {
            value: this.loadBalancer.loadBalancerArn,
            exportName: `${this.stackName}-LoadBalancerArn`,
        });

        // Cross-region specific outputs
        new cdk.CfnOutput(this, 'CrossRegionTargetGroupArn', {
            value: this.crossRegionTargetGroup.targetGroupArn,
            exportName: `${this.stackName}-CrossRegionTargetGroupArn`,
        });

        new cdk.CfnOutput(this, 'PrimaryTargetGroupArn', {
            value: this.primaryTargetGroup.targetGroupArn,
            exportName: `${this.stackName}-PrimaryTargetGroupArn`,
        });

        new cdk.CfnOutput(this, 'SecondaryTargetGroupArn', {
            value: this.secondaryTargetGroup.targetGroupArn,
            exportName: `${this.stackName}-SecondaryTargetGroupArn`,
        });

        new cdk.CfnOutput(this, 'RegionType', {
            value: this.isPrimaryRegion ? 'Primary' : 'Secondary',
            exportName: `${this.stackName}-RegionType`,
        });
    }

    /**
     * Create enhanced ALB with cross-region support
     */
    private createCrossRegionALB(props: CoreInfrastructureStackProps, projectName: string, environment: string): elbv2.ApplicationLoadBalancer {
        const loadBalancer = new elbv2.ApplicationLoadBalancer(this, 'ApplicationLoadBalancer', {
            loadBalancerName: `${projectName}-${environment}-alb-${this.region}`,
            vpc: props.vpc,
            internetFacing: true,
            securityGroup: props.securityGroups.alb,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PUBLIC,
            },
            // Enable deletion protection for production
            deletionProtection: environment === 'production',
            // Enable access logs for monitoring
            // accessLogs: {
            //     bucket: accessLogsBucket,
            //     prefix: `alb-logs/${this.region}`,
            // },
        });

        // Add tags for cross-region identification
        cdk.Tags.of(loadBalancer).add('CrossRegionEnabled', 'true');
        cdk.Tags.of(loadBalancer).add('RegionType', this.isPrimaryRegion ? 'Primary' : 'Secondary');
        cdk.Tags.of(loadBalancer).add('TrafficDistribution', 'Intelligent');

        return loadBalancer;
    }



    /**
     * Configure cross-region routing with intelligent load balancing
     */
    private configureCrossRegionRouting(props: CoreInfrastructureStackProps, projectName: string, environment: string): void {
        // Create HTTP listener with intelligent routing rules
        const httpListener = this.loadBalancer.addListener('HttpListener', {
            port: 80,
            protocol: elbv2.ApplicationProtocol.HTTP,
            // Default action will be overridden by rules
            defaultAction: elbv2.ListenerAction.fixedResponse(503, {
                contentType: 'text/plain',
                messageBody: 'Service temporarily unavailable - cross-region routing in progress',
            }),
        });

        // Rule 1: Route based on regional load (primary region gets 70% traffic)
        httpListener.addAction('RegionalLoadBasedRouting', {
            priority: 10,
            conditions: [
                elbv2.ListenerCondition.httpHeader('X-Region-Preference', ['local']),
            ],
            action: elbv2.ListenerAction.weightedForward([
                {
                    targetGroup: this.primaryTargetGroup,
                    weight: this.isPrimaryRegion ? 80 : 20, // Primary region handles more local traffic
                },
                {
                    targetGroup: this.secondaryTargetGroup,
                    weight: this.isPrimaryRegion ? 20 : 80,
                },
            ]),
        });

        // Rule 2: Route based on capacity monitoring (intelligent overflow)
        httpListener.addAction('CapacityBasedRouting', {
            priority: 20,
            conditions: [
                elbv2.ListenerCondition.httpHeader('X-Capacity-Check', ['true']),
            ],
            action: elbv2.ListenerAction.weightedForward([
                {
                    targetGroup: this.primaryTargetGroup,
                    weight: 60, // Balanced distribution for capacity checks
                },
                {
                    targetGroup: this.crossRegionTargetGroup,
                    weight: 40,
                },
            ]),
        });

        // Rule 3: Route based on latency sensitivity
        httpListener.addAction('LatencySensitiveRouting', {
            priority: 30,
            conditions: [
                elbv2.ListenerCondition.httpHeader('X-Latency-Sensitive', ['true']),
            ],
            action: elbv2.ListenerAction.forward([this.primaryTargetGroup]), // Always route to local region for latency-sensitive requests
        });

        // Rule 4: Default intelligent routing based on regional load
        httpListener.addAction('DefaultIntelligentRouting', {
            priority: 100, // Lowest priority (default)
            conditions: [
                elbv2.ListenerCondition.pathPatterns(['*']), // Match all paths
            ],
            action: elbv2.ListenerAction.weightedForward([
                {
                    targetGroup: this.primaryTargetGroup,
                    weight: this.isPrimaryRegion ? 70 : 30, // Primary region gets more traffic
                },
                {
                    targetGroup: this.secondaryTargetGroup,
                    weight: this.isPrimaryRegion ? 20 : 50,
                },
                {
                    targetGroup: this.crossRegionTargetGroup,
                    weight: 10, // Small percentage for cross-region testing
                },
            ]),
        });

        // Create HTTPS listener for production (if certificates are available)
        if (environment === 'production') {
            const httpsListener = this.loadBalancer.addListener('HttpsListener', {
                port: 443,
                protocol: elbv2.ApplicationProtocol.HTTPS,
                // certificateArns: [certificateArn], // Would be provided in production
                defaultAction: elbv2.ListenerAction.redirect({
                    protocol: 'HTTPS',
                    port: '443',
                    permanent: true,
                }),
            });

            // Add the same routing rules to HTTPS listener
            httpsListener.addAction('HttpsRegionalLoadBasedRouting', {
                priority: 10,
                conditions: [
                    elbv2.ListenerCondition.httpHeader('X-Region-Preference', ['local']),
                ],
                action: elbv2.ListenerAction.weightedForward([
                    {
                        targetGroup: this.primaryTargetGroup,
                        weight: this.isPrimaryRegion ? 80 : 20,
                    },
                    {
                        targetGroup: this.secondaryTargetGroup,
                        weight: this.isPrimaryRegion ? 20 : 80,
                    },
                ]),
            });
        }
    }

    /**
     * Integrate monitoring systems for regional capacity monitoring
     */
    private integrateMonitoringSystems(props: CoreInfrastructureStackProps, projectName: string, environment: string): void {
        // Create CloudWatch custom metrics for regional capacity monitoring
        const capacityMetricFilter = new cdk.aws_logs.MetricFilter(this, 'RegionalCapacityMetricFilter', {
            logGroup: new cdk.aws_logs.LogGroup(this, 'ALBAccessLogGroup', {
                logGroupName: `/aws/applicationloadbalancer/${projectName}-${environment}-${this.region}`,
                retention: cdk.aws_logs.RetentionDays.ONE_WEEK,
                removalPolicy: cdk.RemovalPolicy.DESTROY,
            }),
            metricNamespace: 'CrossRegion/LoadBalancer',
            metricName: 'RegionalCapacityUtilization',
            filterPattern: cdk.aws_logs.FilterPattern.exists('$.responseTime'),
            metricValue: '1',
            defaultValue: 0,
        });

        // Create CloudWatch dashboard for cross-region monitoring
        const dashboard = new cdk.aws_cloudwatch.Dashboard(this, 'CrossRegionDashboard', {
            dashboardName: `${projectName}-${environment}-cross-region-${this.region}`,
            widgets: [
                [
                    new cdk.aws_cloudwatch.GraphWidget({
                        title: 'Regional Load Distribution',
                        left: [
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'AWS/ApplicationELB',
                                metricName: 'RequestCount',
                                dimensionsMap: {
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                    TargetGroup: this.primaryTargetGroup.targetGroupFullName,
                                },
                                statistic: 'Sum',
                                period: cdk.Duration.minutes(5),
                            }),
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'AWS/ApplicationELB',
                                metricName: 'RequestCount',
                                dimensionsMap: {
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                    TargetGroup: this.secondaryTargetGroup.targetGroupFullName,
                                },
                                statistic: 'Sum',
                                period: cdk.Duration.minutes(5),
                            }),
                        ],
                        width: 12,
                        height: 6,
                    }),
                ],
                [
                    new cdk.aws_cloudwatch.GraphWidget({
                        title: 'Target Group Health Status',
                        left: [
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'AWS/ApplicationELB',
                                metricName: 'HealthyHostCount',
                                dimensionsMap: {
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                    TargetGroup: this.primaryTargetGroup.targetGroupFullName,
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(1),
                            }),
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'AWS/ApplicationELB',
                                metricName: 'UnHealthyHostCount',
                                dimensionsMap: {
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                    TargetGroup: this.primaryTargetGroup.targetGroupFullName,
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(1),
                            }),
                        ],
                        width: 12,
                        height: 6,
                    }),
                ],
                [
                    new cdk.aws_cloudwatch.SingleValueWidget({
                        title: 'Regional Capacity Utilization',
                        metrics: [
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'CrossRegion/LoadBalancer',
                                metricName: 'RegionalCapacityUtilization',
                                dimensionsMap: {
                                    Region: this.region,
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(5),
                            }),
                        ],
                        width: 6,
                        height: 6,
                    }),
                    new cdk.aws_cloudwatch.SingleValueWidget({
                        title: 'Cross-Region Response Time P95',
                        metrics: [
                            new cdk.aws_cloudwatch.Metric({
                                namespace: 'AWS/ApplicationELB',
                                metricName: 'TargetResponseTime',
                                dimensionsMap: {
                                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                                },
                                statistic: 'p95',
                                period: cdk.Duration.minutes(5),
                            }),
                        ],
                        width: 6,
                        height: 6,
                    }),
                ],
            ],
        });
    }

    /**
     * Configure traffic overflow alerting mechanisms
     */
    private configureTrafficOverflowAlerting(props: CoreInfrastructureStackProps, projectName: string, environment: string): void {
        // Create SNS topic for traffic overflow alerts
        const alertTopic = new cdk.aws_sns.Topic(this, 'TrafficOverflowAlertTopic', {
            topicName: `${projectName}-${environment}-traffic-overflow-${this.region}`,
            displayName: 'Cross-Region Traffic Overflow Alerts',
        });

        // Create CloudWatch alarms for traffic overflow detection
        
        // Alarm 1: High request count indicating potential overflow
        const highRequestCountAlarm = new cdk.aws_cloudwatch.Alarm(this, 'HighRequestCountAlarm', {
            alarmName: `${projectName}-${environment}-high-request-count-${this.region}`,
            alarmDescription: 'High request count detected - potential traffic overflow',
            metric: new cdk.aws_cloudwatch.Metric({
                namespace: 'AWS/ApplicationELB',
                metricName: 'RequestCount',
                dimensionsMap: {
                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: this.isPrimaryRegion ? 10000 : 5000, // Primary region handles more traffic
            evaluationPeriods: 2,
            comparisonOperator: cdk.aws_cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cdk.aws_cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm 2: High response time indicating capacity issues
        const highResponseTimeAlarm = new cdk.aws_cloudwatch.Alarm(this, 'HighResponseTimeAlarm', {
            alarmName: `${projectName}-${environment}-high-response-time-${this.region}`,
            alarmDescription: 'High response time detected - capacity overflow',
            metric: new cdk.aws_cloudwatch.Metric({
                namespace: 'AWS/ApplicationELB',
                metricName: 'TargetResponseTime',
                dimensionsMap: {
                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 2.0, // 2 seconds threshold
            evaluationPeriods: 3,
            comparisonOperator: cdk.aws_cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cdk.aws_cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm 3: Low healthy host count indicating capacity issues
        const lowHealthyHostCountAlarm = new cdk.aws_cloudwatch.Alarm(this, 'LowHealthyHostCountAlarm', {
            alarmName: `${projectName}-${environment}-low-healthy-hosts-${this.region}`,
            alarmDescription: 'Low healthy host count - potential capacity overflow',
            metric: new cdk.aws_cloudwatch.Metric({
                namespace: 'AWS/ApplicationELB',
                metricName: 'HealthyHostCount',
                dimensionsMap: {
                    LoadBalancer: this.loadBalancer.loadBalancerFullName,
                    TargetGroup: this.primaryTargetGroup.targetGroupFullName,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(1),
            }),
            threshold: 2, // Minimum healthy hosts
            evaluationPeriods: 2,
            comparisonOperator: cdk.aws_cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treatMissingData: cdk.aws_cloudwatch.TreatMissingData.BREACHING,
        });

        // Alarm 4: High error rate indicating system stress
        const highErrorRateAlarm = new cdk.aws_cloudwatch.Alarm(this, 'HighErrorRateAlarm', {
            alarmName: `${projectName}-${environment}-high-error-rate-${this.region}`,
            alarmDescription: 'High error rate detected - system overflow',
            metric: new cdk.aws_cloudwatch.MathExpression({
                expression: '(m1/m2)*100',
                usingMetrics: {
                    m1: new cdk.aws_cloudwatch.Metric({
                        namespace: 'AWS/ApplicationELB',
                        metricName: 'HTTPCode_Target_5XX_Count',
                        dimensionsMap: {
                            LoadBalancer: this.loadBalancer.loadBalancerFullName,
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                    m2: new cdk.aws_cloudwatch.Metric({
                        namespace: 'AWS/ApplicationELB',
                        metricName: 'RequestCount',
                        dimensionsMap: {
                            LoadBalancer: this.loadBalancer.loadBalancerFullName,
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                },
                period: cdk.Duration.minutes(5),
            }),
            threshold: 5.0, // 5% error rate threshold
            evaluationPeriods: 2,
            comparisonOperator: cdk.aws_cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cdk.aws_cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Add SNS actions to all alarms
        const alarms = [highRequestCountAlarm, highResponseTimeAlarm, lowHealthyHostCountAlarm, highErrorRateAlarm];
        alarms.forEach(alarm => {
            alarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(alertTopic));
            alarm.addOkAction(new cdk.aws_cloudwatch_actions.SnsAction(alertTopic));
        });

        // Create composite alarm for overall traffic overflow detection
        const trafficOverflowCompositeAlarm = new cdk.aws_cloudwatch.CompositeAlarm(this, 'TrafficOverflowCompositeAlarm', {
            alarmDescription: 'Composite alarm for traffic overflow detection across multiple metrics',
            alarmRule: cdk.aws_cloudwatch.AlarmRule.anyOf(
                cdk.aws_cloudwatch.AlarmRule.fromAlarm(highRequestCountAlarm, cdk.aws_cloudwatch.AlarmState.ALARM),
                cdk.aws_cloudwatch.AlarmRule.allOf(
                    cdk.aws_cloudwatch.AlarmRule.fromAlarm(highResponseTimeAlarm, cdk.aws_cloudwatch.AlarmState.ALARM),
                    cdk.aws_cloudwatch.AlarmRule.fromAlarm(lowHealthyHostCountAlarm, cdk.aws_cloudwatch.AlarmState.ALARM)
                ),
                cdk.aws_cloudwatch.AlarmRule.fromAlarm(highErrorRateAlarm, cdk.aws_cloudwatch.AlarmState.ALARM)
            ),
            actionsEnabled: true,
        });

        trafficOverflowCompositeAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(alertTopic));

        // Output the SNS topic ARN for integration with other systems
        new cdk.CfnOutput(this, 'TrafficOverflowAlertTopicArn', {
            value: alertTopic.topicArn,
            exportName: `${this.stackName}-TrafficOverflowAlertTopicArn`,
        });
    }
}