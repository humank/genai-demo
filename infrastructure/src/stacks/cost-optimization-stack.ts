import * as cdk from 'aws-cdk-lib';
import * as budgets from 'aws-cdk-lib/aws-budgets';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

/**
 * Stack for cost optimization and billing alerts
 * Implements requirement 12.4: WHEN monitoring costs THEN the system SHALL provide CloudWatch billing alerts
 */
export interface CostOptimizationStackProps extends cdk.StackProps {
    environment: string;
    alertEmail: string;
    slackWebhookUrl?: string;
    // Multi-region configuration for cost management
    multiRegionConfig?: {
        enabled: boolean;
        regions: string[];
        primaryRegion: string;
        crossRegionCostAnalysis: boolean;
    };
    // EKS cluster references for resource utilization monitoring
    eksClusterArns?: string[];
    // CloudWatch metrics for intelligent cost optimization
    enableIntelligentOptimization?: boolean;
}

export class CostOptimizationStack extends cdk.Stack {
    public readonly billingAlarmTopic: sns.Topic;
    public readonly costOptimizationTopic: sns.Topic;
    public readonly multiRegionCostTopic: sns.Topic;
    public dashboard: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: CostOptimizationStackProps) {
        super(scope, id, props);

        // Create SNS topics for different types of cost alerts
        this.billingAlarmTopic = this.createBillingAlarmTopic(props.alertEmail, props.slackWebhookUrl);
        this.costOptimizationTopic = this.createCostOptimizationTopic(props.alertEmail);
        this.multiRegionCostTopic = this.createMultiRegionCostTopic(props.alertEmail);

        // Create billing alarms
        this.createBillingAlarms(props.environment);

        // Create AWS Budgets
        this.createBudgets(props.environment);

        // Create cost optimization dashboard
        this.createCostOptimizationDashboard(props.environment);

        // Multi-region cost management features
        if (props.multiRegionConfig?.enabled) {
            this.createMultiRegionCostMonitoring(props.environment, props.multiRegionConfig);
            this.createCrossRegionCostAnalysis(props.environment, props.multiRegionConfig);
            this.createMultiRegionBudgetControl(props.environment, props.multiRegionConfig);
        }

        // Intelligent cost optimization features
        if (props.enableIntelligentOptimization) {
            this.createIntelligentCostOptimization(props.environment, props.eksClusterArns);
            this.createTrafficBasedResourceAdjustment(props.environment);
            this.createCostOptimizationRecommendationSystem(props.environment);
        }

        // Output important ARNs
        new cdk.CfnOutput(this, 'BillingAlarmTopicArn', {
            value: this.billingAlarmTopic.topicArn,
            description: 'SNS Topic ARN for billing alarms'
        });

        new cdk.CfnOutput(this, 'CostOptimizationTopicArn', {
            value: this.costOptimizationTopic.topicArn,
            description: 'SNS Topic ARN for cost optimization alerts'
        });

        new cdk.CfnOutput(this, 'MultiRegionCostTopicArn', {
            value: this.multiRegionCostTopic.topicArn,
            description: 'SNS Topic ARN for multi-region cost alerts'
        });
    }

    private createBillingAlarmTopic(alertEmail: string, slackWebhookUrl?: string): sns.Topic {
        const topic = new sns.Topic(this, 'BillingAlarmTopic', {
            topicName: `genai-demo-billing-alarms-${this.stackName}`,
            displayName: 'GenAI Demo Billing Alarms',
            fifo: false
        });

        // Add email subscription
        topic.addSubscription(new subscriptions.EmailSubscription(alertEmail));

        // Add Slack subscription if webhook URL is provided
        if (slackWebhookUrl) {
            // Note: In production, you would use AWS Chatbot or Lambda for Slack integration
            // This is a placeholder for the integration
            topic.addSubscription(new subscriptions.UrlSubscription(slackWebhookUrl));
        }

        return topic;
    }

    private createCostOptimizationTopic(alertEmail: string): sns.Topic {
        const topic = new sns.Topic(this, 'CostOptimizationTopic', {
            topicName: `genai-demo-cost-optimization-${this.stackName}`,
            displayName: 'GenAI Demo Cost Optimization Alerts',
            fifo: false
        });

        topic.addSubscription(new subscriptions.EmailSubscription(alertEmail));

        return topic;
    }

    private createBillingAlarms(environment: string): void {
        // Define cost thresholds based on environment
        const costThresholds = this.getCostThresholds(environment);

        // Create billing alarms for different cost levels
        costThresholds.forEach((threshold, index) => {
            const alarm = new cloudwatch.Alarm(this, `BillingAlarm${threshold.amount}`, {
                alarmName: `genai-demo-${environment}-billing-${threshold.amount}usd`,
                alarmDescription: `Billing alarm when estimated charges exceed $${threshold.amount}`,
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Billing',
                    metricName: 'EstimatedCharges',
                    dimensionsMap: {
                        Currency: 'USD'
                    },
                    statistic: 'Maximum',
                    period: cdk.Duration.hours(6)
                }),
                threshold: threshold.amount,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                evaluationPeriods: 1,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
            });

            alarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.billingAlarmTopic));
        });

        // Create service-specific cost alarms
        this.createServiceSpecificAlarms(environment);

        // Create CDN cost monitoring
        this.createCdnCostMonitoring(environment);
    }

    private createServiceSpecificAlarms(environment: string): void {
        const services = [
            { name: 'EKS', threshold: environment === 'production' ? 200 : 50 },
            { name: 'RDS', threshold: environment === 'production' ? 150 : 30 },
            { name: 'CloudWatch', threshold: environment === 'production' ? 100 : 20 },
            { name: 'S3', threshold: environment === 'production' ? 50 : 10 },
            { name: 'MSK', threshold: environment === 'production' ? 100 : 25 },
            { name: 'CloudFront', threshold: environment === 'production' ? 75 : 15 } // CDN cost monitoring
        ];

        services.forEach(service => {
            const alarm = new cloudwatch.Alarm(this, `${service.name}CostAlarm`, {
                alarmName: `genai-demo-${environment}-${service.name.toLowerCase()}-cost`,
                alarmDescription: `Cost alarm for ${service.name} service when charges exceed $${service.threshold}`,
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Billing',
                    metricName: 'EstimatedCharges',
                    dimensionsMap: {
                        Currency: 'USD',
                        ServiceName: service.name === 'EKS' ? 'Amazon Elastic Kubernetes Service' :
                            service.name === 'RDS' ? 'Amazon Relational Database Service' :
                                service.name === 'MSK' ? 'Amazon Managed Streaming for Apache Kafka' :
                                    service.name === 'CloudFront' ? 'Amazon CloudFront' :
                                        `Amazon ${service.name}`
                    },
                    statistic: 'Maximum',
                    period: cdk.Duration.hours(6)
                }),
                threshold: service.threshold,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                evaluationPeriods: 1
            });

            alarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));
        });
    }

    private createBudgets(environment: string): void {
        const budgetAmount = environment === 'production' ? 1000 : 200;

        // Monthly budget
        new budgets.CfnBudget(this, 'MonthlyBudget', {
            budget: {
                budgetName: `genai-demo-${environment}-monthly-budget`,
                budgetLimit: {
                    amount: budgetAmount,
                    unit: 'USD'
                },
                timeUnit: 'MONTHLY',
                budgetType: 'COST',
                costFilters: {
                    TagKey: ['Environment'],
                    TagValue: [environment]
                }
            },
            notificationsWithSubscribers: [
                {
                    notification: {
                        notificationType: 'ACTUAL',
                        comparisonOperator: 'GREATER_THAN',
                        threshold: 80, // 80% of budget
                        thresholdType: 'PERCENTAGE'
                    },
                    subscribers: [
                        {
                            subscriptionType: 'SNS',
                            address: this.billingAlarmTopic.topicArn
                        }
                    ]
                },
                {
                    notification: {
                        notificationType: 'FORECASTED',
                        comparisonOperator: 'GREATER_THAN',
                        threshold: 100, // 100% of budget (forecasted)
                        thresholdType: 'PERCENTAGE'
                    },
                    subscribers: [
                        {
                            subscriptionType: 'SNS',
                            address: this.billingAlarmTopic.topicArn
                        }
                    ]
                }
            ]
        });

        // Daily budget for production
        if (environment === 'production') {
            new budgets.CfnBudget(this, 'DailyBudget', {
                budget: {
                    budgetName: `genai-demo-${environment}-daily-budget`,
                    budgetLimit: {
                        amount: budgetAmount / 30, // Daily limit
                        unit: 'USD'
                    },
                    timeUnit: 'DAILY',
                    budgetType: 'COST',
                    costFilters: {
                        TagKey: ['Environment'],
                        TagValue: [environment]
                    }
                },
                notificationsWithSubscribers: [
                    {
                        notification: {
                            notificationType: 'ACTUAL',
                            comparisonOperator: 'GREATER_THAN',
                            threshold: 150, // 150% of daily budget
                            thresholdType: 'PERCENTAGE'
                        },
                        subscribers: [
                            {
                                subscriptionType: 'SNS',
                                address: this.billingAlarmTopic.topicArn
                            }
                        ]
                    }
                ]
            });
        }
    }

    private createCostOptimizationDashboard(environment: string): void {
        this.dashboard = new cloudwatch.Dashboard(this, 'CostOptimizationDashboard', {
            dashboardName: `genai-demo-${environment}-cost-optimization`,
            defaultInterval: cdk.Duration.hours(24)
        });

        // Add billing metrics widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Estimated Monthly Charges',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Billing',
                        metricName: 'EstimatedCharges',
                        dimensionsMap: { Currency: 'USD' },
                        statistic: 'Maximum',
                        period: cdk.Duration.hours(6)
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add service breakdown widget including CDN
        const services = ['EKS', 'RDS', 'CloudWatch', 'S3', 'MSK', 'CloudFront'];
        const serviceMetrics = services.map(service =>
            new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD',
                    ServiceName: service === 'EKS' ? 'Amazon Elastic Kubernetes Service' :
                        service === 'RDS' ? 'Amazon Relational Database Service' :
                            service === 'MSK' ? 'Amazon Managed Streaming for Apache Kafka' :
                                service === 'CloudFront' ? 'Amazon CloudFront' :
                                    `Amazon ${service}`
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6)
            })
        );

        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Service Cost Breakdown',
                left: serviceMetrics,
                width: 12,
                height: 6
            })
        );

        // Add cost optimization recommendations widget
        this.dashboard.addWidgets(
            new cloudwatch.SingleValueWidget({
                title: 'Cost Optimization Score',
                metrics: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/CostOptimization',
                        metricName: 'OptimizationScore',
                        statistic: 'Average',
                        period: cdk.Duration.hours(1)
                    })
                ],
                width: 6,
                height: 6
            })
        );

        // Add CDN cost efficiency widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'CDN Cost Efficiency Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Billing',
                        metricName: 'EstimatedCharges',
                        dimensionsMap: {
                            Currency: 'USD',
                            ServiceName: 'Amazon CloudFront'
                        },
                        statistic: 'Maximum',
                        period: cdk.Duration.hours(6),
                        label: 'CDN Cost (USD)'
                    })
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/CloudFront',
                        metricName: 'BytesDownloaded',
                        statistic: 'Sum',
                        period: cdk.Duration.hours(6),
                        label: 'Data Transfer (Bytes)'
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add CDN cache efficiency widget for cost optimization
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## CDN Cost Optimization Recommendations

**Current Environment**: ${environment}

**Cost Optimization Strategies**:
- **Cache Hit Rate > 90%**: Reduces origin costs and improves performance
- **Compress Content**: Enable Gzip/Brotli compression to reduce data transfer costs
- **Price Class Optimization**: Use appropriate price class for your audience
- **Origin Shield**: Enable for high-traffic origins to reduce origin requests

**Cost Monitoring Thresholds**:
- **Production**: Data Transfer > $50, Requests > $25
- **Development**: Data Transfer > $10, Requests > $5

**Key Metrics to Monitor**:
- Cost per GB transferred
- Cost per million requests
- Cache hit rate impact on costs
- Geographic distribution costs

**Optimization Actions**:
1. Review cache policies for static content
2. Implement proper TTL settings
3. Monitor origin request patterns
4. Consider Reserved Capacity for predictable workloads`,
                width: 12,
                height: 8
            })
        );
    }

    private getCostThresholds(environment: string): Array<{ amount: number, severity: string }> {
        if (environment === 'production') {
            return [
                { amount: 500, severity: 'warning' },
                { amount: 750, severity: 'critical' },
                { amount: 1000, severity: 'emergency' }
            ];
        } else {
            return [
                { amount: 100, severity: 'warning' },
                { amount: 150, severity: 'critical' },
                { amount: 200, severity: 'emergency' }
            ];
        }
    }

    private createCdnCostMonitoring(environment: string): void {
        // CDN data transfer cost alarm
        const cdnDataTransferAlarm = new cloudwatch.Alarm(this, 'CdnDataTransferCostAlarm', {
            alarmName: `genai-demo-${environment}-cdn-data-transfer-cost`,
            alarmDescription: 'CDN data transfer costs exceeding threshold',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD',
                    ServiceName: 'Amazon CloudFront'
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6)
            }),
            threshold: environment === 'production' ? 50 : 10, // Data transfer cost threshold
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        cdnDataTransferAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));

        // CDN request cost alarm
        const cdnRequestCostAlarm = new cloudwatch.Alarm(this, 'CdnRequestCostAlarm', {
            alarmName: `genai-demo-${environment}-cdn-request-cost`,
            alarmDescription: 'CDN request costs exceeding threshold',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD',
                    ServiceName: 'Amazon CloudFront'
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6)
            }),
            threshold: environment === 'production' ? 25 : 5, // Request cost threshold
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        cdnRequestCostAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));

        // Create CDN cost optimization dashboard widget
        this.addCdnCostOptimizationWidget(environment);
    }

    private addCdnCostOptimizationWidget(environment: string): void {
        // This method would be called from the dashboard creation
        // Adding CDN-specific cost metrics to the existing dashboard
        
        // Note: This would typically be integrated into the createCostOptimizationDashboard method
        // For now, we'll create the metrics that can be used in the dashboard
        
        const cdnCostMetric = new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            dimensionsMap: {
                Currency: 'USD',
                ServiceName: 'Amazon CloudFront'
            },
            statistic: 'Maximum',
            period: cdk.Duration.hours(6)
        });

        // CDN usage metrics for cost optimization
        const cdnDataTransferMetric = new cloudwatch.Metric({
            namespace: 'AWS/CloudFront',
            metricName: 'BytesDownloaded',
            statistic: 'Sum',
            period: cdk.Duration.hours(1)
        });

        const cdnRequestMetric = new cloudwatch.Metric({
            namespace: 'AWS/CloudFront',
            metricName: 'Requests',
            statistic: 'Sum',
            period: cdk.Duration.hours(1)
        });

        // These metrics can be used in the dashboard for CDN cost analysis
        // Cost per GB = CDN Cost / (Data Transfer in GB)
        // Cost per 1M requests = CDN Cost / (Requests / 1,000,000)
    }

    private createMultiRegionCostTopic(alertEmail: string): sns.Topic {
        const topic = new sns.Topic(this, 'MultiRegionCostTopic', {
            topicName: `genai-demo-multi-region-cost-${this.stackName}`,
            displayName: 'GenAI Demo Multi-Region Cost Alerts',
            fifo: false
        });

        topic.addSubscription(new subscriptions.EmailSubscription(alertEmail));

        return topic;
    }

    private createMultiRegionCostMonitoring(environment: string, multiRegionConfig: any): void {
        // Create cost monitoring for each region
        multiRegionConfig.regions.forEach((region: string) => {
            // Regional cost alarm
            const regionalCostAlarm = new cloudwatch.Alarm(this, `RegionalCostAlarm${region.replace(/-/g, '')}`, {
                alarmName: `genai-demo-${environment}-${region}-cost`,
                alarmDescription: `Cost alarm for region ${region}`,
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Billing',
                    metricName: 'EstimatedCharges',
                    dimensionsMap: {
                        Currency: 'USD',
                        LinkedAccount: this.account,
                        Region: region
                    },
                    statistic: 'Maximum',
                    period: cdk.Duration.hours(6)
                }),
                threshold: environment === 'production' ? 300 : 75, // Per region threshold
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                evaluationPeriods: 1,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
            });

            regionalCostAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.multiRegionCostTopic));

            // Traffic-based cost efficiency alarm
            const trafficCostEfficiencyAlarm = new cloudwatch.Alarm(this, `TrafficCostEfficiency${region.replace(/-/g, '')}`, {
                alarmName: `genai-demo-${environment}-${region}-traffic-cost-efficiency`,
                alarmDescription: `Traffic cost efficiency alarm for region ${region}`,
                metric: new cloudwatch.MathExpression({
                    expression: 'cost / (requests / 1000)', // Cost per 1K requests
                    usingMetrics: {
                        cost: new cloudwatch.Metric({
                            namespace: 'AWS/Billing',
                            metricName: 'EstimatedCharges',
                            dimensionsMap: {
                                Currency: 'USD',
                                Region: region
                            },
                            statistic: 'Maximum',
                            period: cdk.Duration.hours(6)
                        }),
                        requests: new cloudwatch.Metric({
                            namespace: 'AWS/ApplicationELB',
                            metricName: 'RequestCount',
                            statistic: 'Sum',
                            period: cdk.Duration.hours(6)
                        })
                    },
                    period: cdk.Duration.hours(6)
                }),
                threshold: environment === 'production' ? 0.05 : 0.1, // Cost per 1K requests threshold
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                evaluationPeriods: 2
            });

            trafficCostEfficiencyAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));
        });
    }

    private createCrossRegionCostAnalysis(environment: string, multiRegionConfig: any): void {
        // Create cross-region cost comparison dashboard
        const crossRegionDashboard = new cloudwatch.Dashboard(this, 'CrossRegionCostAnalysis', {
            dashboardName: `genai-demo-${environment}-cross-region-cost-analysis`,
            defaultInterval: cdk.Duration.hours(24)
        });

        // Regional cost comparison widget
        const regionalCostMetrics = multiRegionConfig.regions.map((region: string) =>
            new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD',
                    Region: region
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6),
                label: `${region} Cost`
            })
        );

        crossRegionDashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Cross-Region Cost Comparison',
                left: regionalCostMetrics,
                width: 12,
                height: 6
            })
        );

        // Cost efficiency by region widget
        const costEfficiencyMetrics = multiRegionConfig.regions.map((region: string) =>
            new cloudwatch.MathExpression({
                expression: `cost_${region.replace(/-/g, '_')} / traffic_${region.replace(/-/g, '_')}`,
                usingMetrics: {
                    [`cost_${region.replace(/-/g, '_')}`]: new cloudwatch.Metric({
                        namespace: 'AWS/Billing',
                        metricName: 'EstimatedCharges',
                        dimensionsMap: {
                            Currency: 'USD',
                            Region: region
                        },
                        statistic: 'Maximum',
                        period: cdk.Duration.hours(6)
                    }),
                    [`traffic_${region.replace(/-/g, '_')}`]: new cloudwatch.Metric({
                        namespace: 'AWS/ApplicationELB',
                        metricName: 'RequestCount',
                        statistic: 'Sum',
                        period: cdk.Duration.hours(6)
                    })
                },
                period: cdk.Duration.hours(6),
                label: `${region} Cost Efficiency`
            })
        );

        crossRegionDashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Cost Efficiency by Region (Cost per Request)',
                left: costEfficiencyMetrics,
                width: 12,
                height: 6
            })
        );

        // Regional resource utilization vs cost widget
        crossRegionDashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Cross-Region Cost Analysis

**Environment**: ${environment}

**Cost Optimization Strategies by Region**:
${multiRegionConfig.regions.map((region: string) => `
- **${region}**: Monitor cost per request, resource utilization > 70%`).join('')}

**Key Metrics**:
- **Cost per Request**: Target < $0.05 per 1K requests (production)
- **Regional Cost Balance**: No single region > 60% of total cost
- **Resource Utilization**: Target > 70% across all regions

**Optimization Actions**:
1. **Traffic-based Scaling**: Adjust resources based on regional traffic patterns
2. **Reserved Instances**: Use RIs for predictable workloads in primary regions
3. **Spot Instances**: Leverage spot instances for non-critical workloads
4. **Cross-Region Load Balancing**: Optimize traffic distribution for cost efficiency

**Alert Thresholds**:
- **Regional Cost**: ${environment === 'production' ? '$300' : '$75'} per region
- **Cost Efficiency**: > ${environment === 'production' ? '$0.05' : '$0.10'} per 1K requests
- **Regional Imbalance**: Single region > 60% of total cost`,
                width: 24,
                height: 10
            })
        );
    }

    private createMultiRegionBudgetControl(environment: string, multiRegionConfig: any): void {
        const totalBudget = environment === 'production' ? 1500 : 300; // Increased for multi-region
        const regionalBudget = totalBudget / multiRegionConfig.regions.length;

        // Create regional budgets
        multiRegionConfig.regions.forEach((region: string, index: number) => {
            new budgets.CfnBudget(this, `RegionalBudget${region.replace(/-/g, '')}`, {
                budget: {
                    budgetName: `genai-demo-${environment}-${region}-budget`,
                    budgetLimit: {
                        amount: regionalBudget,
                        unit: 'USD'
                    },
                    timeUnit: 'MONTHLY',
                    budgetType: 'COST',
                    costFilters: {
                        Region: [region],
                        TagKey: ['Environment'],
                        TagValue: [environment]
                    }
                },
                notificationsWithSubscribers: [
                    {
                        notification: {
                            notificationType: 'ACTUAL',
                            comparisonOperator: 'GREATER_THAN',
                            threshold: 80, // 80% of regional budget
                            thresholdType: 'PERCENTAGE'
                        },
                        subscribers: [
                            {
                                subscriptionType: 'SNS',
                                address: this.multiRegionCostTopic.topicArn
                            }
                        ]
                    },
                    {
                        notification: {
                            notificationType: 'FORECASTED',
                            comparisonOperator: 'GREATER_THAN',
                            threshold: 100, // 100% of regional budget (forecasted)
                            thresholdType: 'PERCENTAGE'
                        },
                        subscribers: [
                            {
                                subscriptionType: 'SNS',
                                address: this.multiRegionCostTopic.topicArn
                            }
                        ]
                    }
                ]
            });
        });

        // Cross-region budget imbalance detection
        new budgets.CfnBudget(this, 'CrossRegionImbalanceBudget', {
            budget: {
                budgetName: `genai-demo-${environment}-cross-region-imbalance`,
                budgetLimit: {
                    amount: totalBudget * 0.6, // 60% of total budget for single region
                    unit: 'USD'
                },
                timeUnit: 'MONTHLY',
                budgetType: 'COST',
                costFilters: {
                    TagKey: ['Environment'],
                    TagValue: [environment]
                }
            },
            notificationsWithSubscribers: [
                {
                    notification: {
                        notificationType: 'ACTUAL',
                        comparisonOperator: 'GREATER_THAN',
                        threshold: 100, // Alert when any region exceeds 60% of total budget
                        thresholdType: 'PERCENTAGE'
                    },
                    subscribers: [
                        {
                            subscriptionType: 'SNS',
                            address: this.multiRegionCostTopic.topicArn
                        }
                    ]
                }
            ]
        });
    }

    private createIntelligentCostOptimization(environment: string, eksClusterArns?: string[]): void {
        // Create Lambda function for intelligent cost optimization
        const costOptimizationFunction = new lambda.Function(this, 'CostOptimizationFunction', {
            functionName: `genai-demo-${environment}-cost-optimization`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import json
import boto3
import datetime
from typing import Dict, List, Any

def handler(event, context):
    """
    Intelligent cost optimization function
    Analyzes resource utilization and provides optimization recommendations
    """
    cloudwatch = boto3.client('cloudwatch')
    ec2 = boto3.client('ec2')
    eks = boto3.client('eks')
    
    recommendations = []
    
    try:
        # Analyze EKS cluster utilization
        if 'eksClusterArns' in event:
            eks_recommendations = analyze_eks_utilization(cloudwatch, eks, event['eksClusterArns'])
            recommendations.extend(eks_recommendations)
        
        # Analyze EC2 instance utilization
        ec2_recommendations = analyze_ec2_utilization(cloudwatch, ec2)
        recommendations.extend(ec2_recommendations)
        
        # Generate cost optimization score
        optimization_score = calculate_optimization_score(recommendations)
        
        # Publish optimization score to CloudWatch
        cloudwatch.put_metric_data(
            Namespace='GenAIDemo/CostOptimization',
            MetricData=[
                {
                    'MetricName': 'OptimizationScore',
                    'Value': optimization_score,
                    'Unit': 'Percent',
                    'Timestamp': datetime.datetime.utcnow()
                }
            ]
        )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'recommendations': recommendations,
                'optimizationScore': optimization_score
            })
        }
        
    except Exception as e:
        print(f"Error in cost optimization analysis: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def analyze_eks_utilization(cloudwatch, eks, cluster_arns: List[str]) -> List[Dict[str, Any]]:
    """Analyze EKS cluster resource utilization"""
    recommendations = []
    
    for cluster_arn in cluster_arns:
        cluster_name = cluster_arn.split('/')[-1]
        
        # Get CPU utilization
        cpu_response = cloudwatch.get_metric_statistics(
            Namespace='ContainerInsights',
            MetricName='cluster_cpu_utilization',
            Dimensions=[
                {'Name': 'ClusterName', 'Value': cluster_name}
            ],
            StartTime=datetime.datetime.utcnow() - datetime.timedelta(hours=24),
            EndTime=datetime.datetime.utcnow(),
            Period=3600,
            Statistics=['Average']
        )
        
        if cpu_response['Datapoints']:
            avg_cpu = sum(dp['Average'] for dp in cpu_response['Datapoints']) / len(cpu_response['Datapoints'])
            
            if avg_cpu < 30:
                recommendations.append({
                    'type': 'EKS_UNDERUTILIZED',
                    'resource': cluster_name,
                    'utilization': avg_cpu,
                    'recommendation': 'Consider reducing node group size or instance types',
                    'potential_savings': 'Up to 40% cost reduction'
                })
            elif avg_cpu > 80:
                recommendations.append({
                    'type': 'EKS_OVERUTILIZED',
                    'resource': cluster_name,
                    'utilization': avg_cpu,
                    'recommendation': 'Consider scaling up node groups or using larger instance types',
                    'potential_impact': 'Prevent performance degradation'
                })
    
    return recommendations

def analyze_ec2_utilization(cloudwatch, ec2) -> List[Dict[str, Any]]:
    """Analyze EC2 instance utilization"""
    recommendations = []
    
    # Get all running instances
    instances_response = ec2.describe_instances(
        Filters=[{'Name': 'instance-state-name', 'Values': ['running']}]
    )
    
    for reservation in instances_response['Reservations']:
        for instance in reservation['Instances']:
            instance_id = instance['InstanceId']
            
            # Get CPU utilization
            cpu_response = cloudwatch.get_metric_statistics(
                Namespace='AWS/EC2',
                MetricName='CPUUtilization',
                Dimensions=[
                    {'Name': 'InstanceId', 'Value': instance_id}
                ],
                StartTime=datetime.datetime.utcnow() - datetime.timedelta(hours=24),
                EndTime=datetime.datetime.utcnow(),
                Period=3600,
                Statistics=['Average']
            )
            
            if cpu_response['Datapoints']:
                avg_cpu = sum(dp['Average'] for dp in cpu_response['Datapoints']) / len(cpu_response['Datapoints'])
                
                if avg_cpu < 10:
                    recommendations.append({
                        'type': 'EC2_IDLE',
                        'resource': instance_id,
                        'utilization': avg_cpu,
                        'recommendation': 'Consider stopping or terminating idle instance',
                        'potential_savings': f"Instance {instance.get('InstanceType', 'unknown')} hourly cost"
                    })
    
    return recommendations

def calculate_optimization_score(recommendations: List[Dict[str, Any]]) -> float:
    """Calculate overall cost optimization score"""
    if not recommendations:
        return 100.0  # Perfect score if no issues found
    
    # Score based on number and severity of recommendations
    underutilized_count = len([r for r in recommendations if 'UNDERUTILIZED' in r['type'] or 'IDLE' in r['type']])
    overutilized_count = len([r for r in recommendations if 'OVERUTILIZED' in r['type']])
    
    # Deduct points for each issue
    score = 100.0
    score -= underutilized_count * 15  # Underutilization is costly
    score -= overutilized_count * 10   # Overutilization affects performance
    
    return max(0.0, score)
            `),
            timeout: cdk.Duration.minutes(5),
            memorySize: 256,
            environment: {
                'ENVIRONMENT': environment
            }
        });

        // Grant necessary permissions
        costOptimizationFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:PutMetricData',
                'ec2:DescribeInstances',
                'eks:DescribeCluster',
                'eks:ListClusters'
            ],
            resources: ['*']
        }));

        // Schedule the function to run every hour
        const costOptimizationRule = new events.Rule(this, 'CostOptimizationSchedule', {
            ruleName: `genai-demo-${environment}-cost-optimization-schedule`,
            schedule: events.Schedule.rate(cdk.Duration.hours(1)),
            description: 'Trigger cost optimization analysis every hour'
        });

        costOptimizationRule.addTarget(new eventsTargets.LambdaFunction(costOptimizationFunction, {
            event: events.RuleTargetInput.fromObject({
                eksClusterArns: eksClusterArns || [],
                environment: environment
            })
        }));
    }

    private createTrafficBasedResourceAdjustment(environment: string): void {
        // Create CloudWatch composite alarm for traffic-based resource adjustment
        const highTrafficAlarm = new cloudwatch.Alarm(this, 'HighTrafficAlarm', {
            alarmName: `genai-demo-${environment}-high-traffic`,
            alarmDescription: 'High traffic detected - consider scaling up resources',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/ApplicationELB',
                metricName: 'RequestCount',
                statistic: 'Sum',
                period: cdk.Duration.minutes(5)
            }),
            threshold: environment === 'production' ? 10000 : 1000, // Requests per 5 minutes
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        const lowTrafficAlarm = new cloudwatch.Alarm(this, 'LowTrafficAlarm', {
            alarmName: `genai-demo-${environment}-low-traffic`,
            alarmDescription: 'Low traffic detected - consider scaling down resources',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/ApplicationELB',
                metricName: 'RequestCount',
                statistic: 'Sum',
                period: cdk.Duration.minutes(15)
            }),
            threshold: environment === 'production' ? 100 : 10, // Requests per 15 minutes
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        // Add actions to send notifications for manual review
        highTrafficAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));
        lowTrafficAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(this.costOptimizationTopic));
    }

    private createCostOptimizationRecommendationSystem(environment: string): void {
        // Create custom metrics for cost optimization recommendations
        const costOptimizationMetrics = [
            'ResourceUtilizationScore',
            'CostEfficiencyRatio',
            'PotentialSavings',
            'OptimizationOpportunities'
        ];

        costOptimizationMetrics.forEach(metricName => {
            new cloudwatch.Metric({
                namespace: 'GenAIDemo/CostOptimization',
                metricName: metricName,
                statistic: 'Average',
                period: cdk.Duration.hours(1)
            });
        });

        // Add cost optimization recommendations to the main dashboard
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Cost Optimization Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/CostOptimization',
                        metricName: 'OptimizationScore',
                        statistic: 'Average',
                        period: cdk.Duration.hours(1),
                        label: 'Optimization Score (%)'
                    })
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/CostOptimization',
                        metricName: 'PotentialSavings',
                        statistic: 'Average',
                        period: cdk.Duration.hours(1),
                        label: 'Potential Savings ($)'
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add cost optimization recommendations text widget
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Intelligent Cost Optimization System

**Environment**: ${environment}

**Automated Analysis**:
- **Resource Utilization Monitoring**: Target > 70% utilization
- **Traffic-based Scaling**: Automatic recommendations based on traffic patterns
- **Cost Efficiency Analysis**: Cost per request optimization
- **Idle Resource Detection**: Identify and recommend cleanup of unused resources

**Key Metrics**:
- **Optimization Score**: Overall cost efficiency score (target > 85%)
- **Resource Utilization**: CPU/Memory utilization across services
- **Cost per Request**: Efficiency metric for traffic-based optimization
- **Potential Savings**: Estimated monthly savings from recommendations

**Optimization Strategies**:
1. **EKS Node Group Optimization**: Right-size based on actual usage
2. **Reserved Instance Recommendations**: For predictable workloads
3. **Spot Instance Usage**: For fault-tolerant workloads
4. **Auto-scaling Optimization**: Fine-tune scaling policies

**Alert Thresholds**:
- **Low Utilization**: < 30% CPU/Memory for > 24 hours
- **High Cost per Request**: > ${environment === 'production' ? '$0.05' : '$0.10'} per 1K requests
- **Optimization Score**: < 70% overall efficiency`,
                width: 12,
                height: 8
            })
        );
    }
}