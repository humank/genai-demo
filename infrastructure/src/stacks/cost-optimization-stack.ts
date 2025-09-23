import * as cdk from 'aws-cdk-lib';
import * as budgets from 'aws-cdk-lib/aws-budgets';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

/**
 * Stack for cost optimization and billing alerts
 * Implements requirement 12.4: WHEN monitoring costs THEN the system SHALL provide CloudWatch billing alerts
 */
export class CostOptimizationStack extends cdk.Stack {
    public readonly billingAlarmTopic: sns.Topic;
    public readonly costOptimizationTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: cdk.StackProps & {
        environment: string;
        alertEmail: string;
        slackWebhookUrl?: string;
    }) {
        super(scope, id, props);

        // Create SNS topics for different types of cost alerts
        this.billingAlarmTopic = this.createBillingAlarmTopic(props.alertEmail, props.slackWebhookUrl);
        this.costOptimizationTopic = this.createCostOptimizationTopic(props.alertEmail);

        // Create billing alarms
        this.createBillingAlarms(props.environment);

        // Create AWS Budgets
        this.createBudgets(props.environment);

        // Create cost optimization dashboard
        this.createCostOptimizationDashboard(props.environment);

        // Output important ARNs
        new cdk.CfnOutput(this, 'BillingAlarmTopicArn', {
            value: this.billingAlarmTopic.topicArn,
            description: 'SNS Topic ARN for billing alarms'
        });

        new cdk.CfnOutput(this, 'CostOptimizationTopicArn', {
            value: this.costOptimizationTopic.topicArn,
            description: 'SNS Topic ARN for cost optimization alerts'
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
    }

    private createServiceSpecificAlarms(environment: string): void {
        const services = [
            { name: 'EKS', threshold: environment === 'production' ? 200 : 50 },
            { name: 'RDS', threshold: environment === 'production' ? 150 : 30 },
            { name: 'CloudWatch', threshold: environment === 'production' ? 100 : 20 },
            { name: 'S3', threshold: environment === 'production' ? 50 : 10 },
            { name: 'MSK', threshold: environment === 'production' ? 100 : 25 }
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
        const dashboard = new cloudwatch.Dashboard(this, 'CostOptimizationDashboard', {
            dashboardName: `genai-demo-${environment}-cost-optimization`,
            defaultInterval: cdk.Duration.hours(24)
        });

        // Add billing metrics widget
        dashboard.addWidgets(
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

        // Add service breakdown widget
        const services = ['EKS', 'RDS', 'CloudWatch', 'S3', 'MSK'];
        const serviceMetrics = services.map(service =>
            new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD',
                    ServiceName: service === 'EKS' ? 'Amazon Elastic Kubernetes Service' :
                        service === 'RDS' ? 'Amazon Relational Database Service' :
                            service === 'MSK' ? 'Amazon Managed Streaming for Apache Kafka' :
                                `Amazon ${service}`
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6)
            })
        );

        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Service Cost Breakdown',
                left: serviceMetrics,
                width: 12,
                height: 6
            })
        );

        // Add cost optimization recommendations widget
        dashboard.addWidgets(
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
}