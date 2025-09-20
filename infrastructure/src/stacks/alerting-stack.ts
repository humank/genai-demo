import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

/**
 * Stack for creating CloudWatch alarms and SNS topics for health check alerting.
 * 
 * Implements requirement 8.3: Set up CloudWatch alarms for critical metrics
 * Implements requirement 8.4: Create SNS topics and subscriptions for alerting
 */
export class AlertingStack extends cdk.Stack {
  public readonly criticalAlertsTopic: sns.Topic;
  public readonly warningAlertsTopic: sns.Topic;
  public readonly infoAlertsTopic: sns.Topic;

  constructor(scope: Construct, id: string, props: cdk.StackProps & {
    environment: string;
    region: string;
    applicationName: string;
    alertingConfig: AlertingConfig;
  }) {
    super(scope, id, props);

    const { environment, region, applicationName, alertingConfig } = props;

    // Create SNS Topics for different alert levels
    this.criticalAlertsTopic = this.createSNSTopic('Critical', environment, alertingConfig.criticalAlerts);
    this.warningAlertsTopic = this.createSNSTopic('Warning', environment, alertingConfig.warningAlerts);
    this.infoAlertsTopic = this.createSNSTopic('Info', environment, alertingConfig.infoAlerts);

    // Create CloudWatch Alarms for health checks
    this.createHealthCheckAlarms(environment, applicationName);

    // Create CloudWatch Alarms for application metrics
    this.createApplicationMetricAlarms(environment, applicationName);

    // Create CloudWatch Alarms for infrastructure metrics
    this.createInfrastructureAlarms(environment, applicationName);

    // Create CloudWatch Dashboard
    this.createHealthCheckDashboard(environment, applicationName);

    // Output important ARNs
    new cdk.CfnOutput(this, 'CriticalAlertsTopicArn', {
      value: this.criticalAlertsTopic.topicArn,
      description: 'ARN of the critical alerts SNS topic',
      exportName: `${environment}-critical-alerts-topic-arn`,
    });

    new cdk.CfnOutput(this, 'WarningAlertsTopicArn', {
      value: this.warningAlertsTopic.topicArn,
      description: 'ARN of the warning alerts SNS topic',
      exportName: `${environment}-warning-alerts-topic-arn`,
    });
  }

  /**
   * Create SNS topic with subscriptions for alerts
   */
  private createSNSTopic(level: string, environment: string, config: AlertSubscriptionConfig): sns.Topic {
    const topic = new sns.Topic(this, `${level}AlertsTopic`, {
      topicName: `genai-demo-${environment}-${level.toLowerCase()}-alerts`,
      displayName: `GenAI Demo ${environment} ${level} Alerts`,
      fifo: false,
    });

    // Add email subscriptions
    if (config.emailAddresses && config.emailAddresses.length > 0) {
      config.emailAddresses.forEach((email, index) => {
        topic.addSubscription(new snsSubscriptions.EmailSubscription(email));
      });
    }

    // Add SMS subscriptions for critical alerts
    if (level === 'Critical' && config.phoneNumbers && config.phoneNumbers.length > 0) {
      config.phoneNumbers.forEach((phone, index) => {
        topic.addSubscription(new snsSubscriptions.SmsSubscription(phone));
      });
    }

    // Add Slack webhook subscription if configured
    if (config.slackWebhookUrl) {
      // Note: In a real implementation, you would use AWS Chatbot or Lambda for Slack integration
      // For now, we'll create a placeholder for the webhook URL
      topic.addSubscription(new snsSubscriptions.UrlSubscription(config.slackWebhookUrl));
    }

    return topic;
  }

  /**
   * Create CloudWatch alarms for health check metrics
   */
  private createHealthCheckAlarms(environment: string, applicationName: string): void {
    const namespace = `GenAIDemo/${environment}`;

    // Database Health Check Alarm
    new cloudwatch.Alarm(this, 'DatabaseHealthAlarm', {
      alarmName: `${applicationName}-${environment}-database-health`,
      alarmDescription: 'Database health check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'database_health_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    // Kafka Health Check Alarm (for production)
    if (environment === 'production') {
      new cloudwatch.Alarm(this, 'KafkaHealthAlarm', {
        alarmName: `${applicationName}-${environment}-kafka-health`,
        alarmDescription: 'Kafka health check is failing',
        metric: new cloudwatch.Metric({
          namespace: namespace,
          metricName: 'health.check.status',
          dimensionsMap: {
            indicator: 'kafka_health_indicator',
          },
          statistic: 'Average',
        }),
        threshold: 0.5,
        comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
        evaluationPeriods: 3,
        datapointsToAlarm: 2,
        treatMissingData: cloudwatch.TreatMissingData.BREACHING,
      });
    }

    // System Resources Health Alarm
    new cloudwatch.Alarm(this, 'SystemResourcesHealthAlarm', {
      alarmName: `${applicationName}-${environment}-system-resources-health`,
      alarmDescription: 'System resources health check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'system_resources_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    // Application Readiness Alarm
    new cloudwatch.Alarm(this, 'ApplicationReadinessAlarm', {
      alarmName: `${applicationName}-${environment}-application-readiness`,
      alarmDescription: 'Application readiness check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'application_readiness_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });
  }

  /**
   * Create CloudWatch alarms for application metrics
   */
  private createApplicationMetricAlarms(environment: string, applicationName: string): void {
    const namespace = `GenAIDemo/${environment}`;

    // High Error Rate Alarm
    new cloudwatch.Alarm(this, 'HighErrorRateAlarm', {
      alarmName: `${applicationName}-${environment}-high-error-rate`,
      alarmDescription: 'Application error rate is too high (>5% in 5 minutes)',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'http.server.requests',
        dimensionsMap: {
          status: '5xx',
        },
        statistic: 'Sum',
      }),
      threshold: 10, // More than 10 errors in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // High Response Time Alarm
    new cloudwatch.Alarm(this, 'HighResponseTimeAlarm', {
      alarmName: `${applicationName}-${environment}-high-response-time`,
      alarmDescription: 'Application response time is too high (>2s average in 5 minutes)',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'http.server.requests',
        statistic: 'Average',
      }),
      threshold: 2000, // 2 seconds in milliseconds
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Health Check Failures Alarm
    new cloudwatch.Alarm(this, 'HealthCheckFailuresAlarm', {
      alarmName: `${applicationName}-${environment}-health-check-failures`,
      alarmDescription: 'Health check failures are increasing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.executions',
        statistic: 'Sum',
      }),
      threshold: 5, // More than 5 failed health checks in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Recovery Attempts Alarm
    new cloudwatch.Alarm(this, 'RecoveryAttemptsAlarm', {
      alarmName: `${applicationName}-${environment}-recovery-attempts`,
      alarmDescription: 'Automated recovery attempts are being triggered frequently',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.recovery.attempts',
        statistic: 'Sum',
      }),
      threshold: 3, // More than 3 recovery attempts in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });
  }

  /**
   * Create CloudWatch alarms for infrastructure metrics
   */
  private createInfrastructureAlarms(environment: string, applicationName: string): void {
    // Memory Usage Alarm
    new cloudwatch.Alarm(this, 'HighMemoryUsageAlarm', {
      alarmName: `${applicationName}-${environment}-high-memory-usage`,
      alarmDescription: 'Memory usage is too high (>85%)',
      metric: new cloudwatch.Metric({
        namespace: `GenAIDemo/${environment}`,
        metricName: 'health.system.memory.usage',
        statistic: 'Average',
      }),
      threshold: 85, // 85% memory usage
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Database Connection Failures Alarm
    new cloudwatch.Alarm(this, 'DatabaseConnectionFailuresAlarm', {
      alarmName: `${applicationName}-${environment}-database-connection-failures`,
      alarmDescription: 'Database connection failures detected',
      metric: new cloudwatch.Metric({
        namespace: `GenAIDemo/${environment}`,
        metricName: 'health.check.errors',
        dimensionsMap: {
          indicator: 'database_health_indicator',
        },
        statistic: 'Sum',
      }),
      threshold: 1, // Any database connection failure
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });
  }

  /**
   * Create CloudWatch dashboard for health monitoring
   */
  private createHealthCheckDashboard(environment: string, applicationName: string): void {
    const dashboard = new cloudwatch.Dashboard(this, 'HealthCheckDashboard', {
      dashboardName: `${applicationName}-${environment}-health-monitoring`,
    });

    const namespace = `GenAIDemo/${environment}`;

    // Health Status Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Health Check Status',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'database_health_indicator' },
            label: 'Database Health',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'application_readiness_indicator' },
            label: 'Application Readiness',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'system_resources_indicator' },
            label: 'System Resources',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Health Check Duration Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Health Check Response Times',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.duration',
            statistic: 'Average',
            label: 'Average Duration',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.duration',
            statistic: 'Maximum',
            label: 'Max Duration',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Recovery Attempts Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Automated Recovery Activity',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.attempts',
            statistic: 'Sum',
            label: 'Recovery Attempts',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.successful',
            statistic: 'Sum',
            label: 'Successful Recoveries',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.failed',
            statistic: 'Sum',
            label: 'Failed Recoveries',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // System Resources Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'System Resources',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.system.memory.usage',
            statistic: 'Average',
            label: 'Memory Usage %',
          }),
        ],
        width: 12,
        height: 6,
      })
    );
  }
}

/**
 * Configuration for alerting subscriptions
 */
export interface AlertSubscriptionConfig {
  emailAddresses?: string[];
  phoneNumbers?: string[];
  slackWebhookUrl?: string;
}

/**
 * Configuration for alerting stack
 */
export interface AlertingConfig {
  criticalAlerts: AlertSubscriptionConfig;
  warningAlerts: AlertSubscriptionConfig;
  infoAlerts: AlertSubscriptionConfig;
}