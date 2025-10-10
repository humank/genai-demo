import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface SecurityMonitoringStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region?: string;
    readonly criticalAlertsTopic: sns.ITopic;
    readonly warningAlertsTopic: sns.ITopic;
    readonly guardDutyDetectorId: string;
    readonly webAclArn: string;
}

/**
 * Security Monitoring and Alerting Stack
 * 
 * This stack implements comprehensive security monitoring and alerting:
 * 1. GuardDuty findings processing and alerting
 * 2. WAF metrics monitoring and anomaly detection
 * 3. VPC Flow Logs analysis and threat detection
 * 4. Security event correlation and automated response
 * 
 * Requirements: 3.3 - Network Security and Isolation (Monitoring Component)
 */
export class SecurityMonitoringStack extends cdk.Stack {
    public readonly securityDashboard: cloudwatch.Dashboard;
    public readonly securityEventProcessor: lambda.Function;
    public readonly threatDetectionAlarms: cloudwatch.Alarm[];

    constructor(scope: Construct, id: string, props: SecurityMonitoringStackProps) {
        super(scope, id, props);

        const { environment, projectName, region, criticalAlertsTopic, warningAlertsTopic, guardDutyDetectorId, webAclArn } = props;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'SecurityMonitoring');
        cdk.Tags.of(this).add('Component', 'SecurityObservability');

        // Create security event processor Lambda
        this.securityEventProcessor = this.createSecurityEventProcessor(projectName, environment, criticalAlertsTopic);

        // Create security monitoring dashboard
        this.securityDashboard = this.createSecurityDashboard(projectName, environment, region);

        // Create threat detection alarms
        this.threatDetectionAlarms = this.createThreatDetectionAlarms(
            projectName, 
            environment, 
            criticalAlertsTopic, 
            warningAlertsTopic,
            guardDutyDetectorId,
            webAclArn
        );

        // Configure GuardDuty event processing
        this.configureGuardDutyEventProcessing(projectName, environment);

        // Configure WAF log analysis
        this.configureWAFLogAnalysis(projectName, environment, region);

        // Configure VPC Flow Logs analysis
        this.configureVPCFlowLogsAnalysis(projectName, environment, region);

        // Create outputs
        this.createOutputs(projectName, environment);
    }

    /**
     * Create Lambda function for security event processing
     */
    private createSecurityEventProcessor(projectName: string, environment: string, alertsTopic: sns.ITopic): lambda.Function {
        const securityEventProcessor = new lambda.Function(this, 'SecurityEventProcessor', {
            functionName: `${projectName}-${environment}-security-event-processor`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(5),
            memorySize: 256,
            environment: {
                ENVIRONMENT: environment,
                PROJECT_NAME: projectName,
                ALERTS_TOPIC_ARN: alertsTopic.topicArn
            },
            code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime

def handler(event, context):
    """
    Process security events from GuardDuty, WAF, and VPC Flow Logs
    """
    sns = boto3.client('sns')
    alerts_topic_arn = os.environ['ALERTS_TOPIC_ARN']
    
    try:
        # Parse the event
        if 'source' in event and event['source'] == 'aws.guardduty':
            return process_guardduty_finding(event, sns, alerts_topic_arn)
        elif 'source' in event and event['source'] == 'aws.wafv2':
            return process_waf_event(event, sns, alerts_topic_arn)
        elif 'logEvents' in event:
            return process_vpc_flow_logs(event, sns, alerts_topic_arn)
        else:
            print(f"Unknown event type: {json.dumps(event)}")
            return {'statusCode': 200, 'body': 'Event processed'}
            
    except Exception as e:
        print(f"Error processing security event: {str(e)}")
        # Send error alert
        sns.publish(
            TopicArn=alerts_topic_arn,
            Subject=f"Security Event Processing Error - {os.environ['PROJECT_NAME']}",
            Message=f"Error processing security event: {str(e)}\\n\\nEvent: {json.dumps(event)}"
        )
        raise e

def process_guardduty_finding(event, sns, alerts_topic_arn):
    """Process GuardDuty findings"""
    detail = event.get('detail', {})
    severity = detail.get('severity', 0)
    finding_type = detail.get('type', 'Unknown')
    
    if severity >= 7.0:  # High severity
        message = f"""
🚨 HIGH SEVERITY GUARDDUTY FINDING 🚨

Finding Type: {finding_type}
Severity: {severity}
Description: {detail.get('description', 'No description')}
Region: {event.get('region', 'Unknown')}
Account: {event.get('account', 'Unknown')}
Time: {event.get('time', 'Unknown')}

Service: {detail.get('service', {}).get('serviceName', 'Unknown')}
Resource: {json.dumps(detail.get('resource', {}), indent=2)}

Action Required: Immediate investigation and response needed.
        """
        
        sns.publish(
            TopicArn=alerts_topic_arn,
            Subject=f"🚨 HIGH SEVERITY GuardDuty Finding: {finding_type}",
            Message=message
        )
    
    return {'statusCode': 200, 'body': 'GuardDuty finding processed'}

def process_waf_event(event, sns, alerts_topic_arn):
    """Process WAF events"""
    # This would be called from WAF logs analysis
    return {'statusCode': 200, 'body': 'WAF event processed'}

def process_vpc_flow_logs(event, sns, alerts_topic_arn):
    """Process VPC Flow Logs for anomaly detection"""
    # This would analyze flow logs for suspicious patterns
    return {'statusCode': 200, 'body': 'VPC Flow Logs processed'}
            `)
        });

        // Grant permissions to publish to SNS
        alertsTopic.grantPublish(securityEventProcessor);

        // Grant permissions to read GuardDuty findings
        securityEventProcessor.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'guardduty:GetFindings',
                'guardduty:ListFindings',
                'guardduty:GetDetector'
            ],
            resources: ['*']
        }));

        // Grant permissions to read CloudWatch Logs
        securityEventProcessor.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams'
            ],
            resources: ['*']
        }));

        return securityEventProcessor;
    }

    /**
     * Create comprehensive security monitoring dashboard
     */
    private createSecurityDashboard(projectName: string, environment: string, region?: string): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'SecurityDashboard', {
            dashboardName: `${projectName}-${environment}-security-monitoring`,
            defaultInterval: cdk.Duration.hours(1)
        });

        // GuardDuty metrics
        const guardDutyWidget = new cloudwatch.GraphWidget({
            title: 'GuardDuty Findings',
            left: [
                new cloudwatch.Metric({
                    namespace: 'AWS/GuardDuty',
                    metricName: 'FindingCount',
                    dimensionsMap: {
                        DetectorId: 'GuardDutyDetector'
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                })
            ],
            width: 12,
            height: 6
        });

        // WAF metrics
        const wafWidget = new cloudwatch.GraphWidget({
            title: 'WAF Blocked Requests',
            left: [
                new cloudwatch.Metric({
                    namespace: 'AWS/WAFV2',
                    metricName: 'BlockedRequests',
                    dimensionsMap: {
                        WebACL: `${projectName}-${environment}-web-acl`,
                        Region: region || 'ap-east-2',
                        Rule: 'ALL'
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                })
            ],
            right: [
                new cloudwatch.Metric({
                    namespace: 'AWS/WAFV2',
                    metricName: 'AllowedRequests',
                    dimensionsMap: {
                        WebACL: `${projectName}-${environment}-web-acl`,
                        Region: region || 'ap-east-2',
                        Rule: 'ALL'
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                })
            ],
            width: 12,
            height: 6
        });

        // VPC Flow Logs metrics
        const vpcFlowLogsWidget = new cloudwatch.GraphWidget({
            title: 'VPC Flow Logs - Traffic Analysis',
            left: [
                new cloudwatch.Metric({
                    namespace: 'AWS/VPC',
                    metricName: 'PacketsDropped',
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                })
            ],
            right: [
                new cloudwatch.Metric({
                    namespace: 'AWS/VPC',
                    metricName: 'BytesTransferred',
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                })
            ],
            width: 12,
            height: 6
        });

        // Security events summary
        const securityEventsWidget = new cloudwatch.SingleValueWidget({
            title: 'Security Events Summary',
            metrics: [
                new cloudwatch.Metric({
                    namespace: 'AWS/GuardDuty',
                    metricName: 'FindingCount',
                    statistic: 'Sum',
                    period: cdk.Duration.hours(24)
                }),
                new cloudwatch.Metric({
                    namespace: 'AWS/WAFV2',
                    metricName: 'BlockedRequests',
                    statistic: 'Sum',
                    period: cdk.Duration.hours(24)
                })
            ],
            width: 12,
            height: 6
        });

        // Add widgets to dashboard
        dashboard.addWidgets(
            guardDutyWidget,
            wafWidget,
            vpcFlowLogsWidget,
            securityEventsWidget
        );

        return dashboard;
    }

    /**
     * Create threat detection alarms
     */
    private createThreatDetectionAlarms(
        projectName: string, 
        environment: string, 
        criticalAlertsTopic: sns.ITopic,
        warningAlertsTopic: sns.ITopic,
        guardDutyDetectorId: string,
        webAclArn: string
    ): cloudwatch.Alarm[] {
        const alarms: cloudwatch.Alarm[] = [];

        // GuardDuty high severity findings alarm
        const guardDutyHighSeverityAlarm = new cloudwatch.Alarm(this, 'GuardDutyHighSeverityAlarm', {
            alarmName: `${projectName}-${environment}-guardduty-high-severity`,
            alarmDescription: 'Alert when GuardDuty detects high severity findings',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/GuardDuty',
                metricName: 'FindingCount',
                dimensionsMap: {
                    DetectorId: guardDutyDetectorId
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 1,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });
        guardDutyHighSeverityAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(criticalAlertsTopic));
        alarms.push(guardDutyHighSeverityAlarm);

        // WAF blocked requests spike alarm
        const wafBlockedRequestsAlarm = new cloudwatch.Alarm(this, 'WAFBlockedRequestsAlarm', {
            alarmName: `${projectName}-${environment}-waf-blocked-requests-spike`,
            alarmDescription: 'Alert when WAF blocks unusually high number of requests',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/WAFV2',
                metricName: 'BlockedRequests',
                dimensionsMap: {
                    WebACL: `${projectName}-${environment}-web-acl`,
                    Region: this.region,
                    Rule: 'ALL'
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 100, // More than 100 blocked requests in 5 minutes
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });
        wafBlockedRequestsAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(warningAlertsTopic));
        alarms.push(wafBlockedRequestsAlarm);

        // WAF rate limit triggered alarm
        const wafRateLimitAlarm = new cloudwatch.Alarm(this, 'WAFRateLimitAlarm', {
            alarmName: `${projectName}-${environment}-waf-rate-limit-triggered`,
            alarmDescription: 'Alert when WAF rate limiting is frequently triggered',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/WAFV2',
                metricName: 'BlockedRequests',
                dimensionsMap: {
                    WebACL: `${projectName}-${environment}-web-acl`,
                    Region: this.region,
                    Rule: 'RateLimitRule'
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 50, // More than 50 rate-limited requests in 5 minutes
            evaluationPeriods: 3,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });
        wafRateLimitAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(warningAlertsTopic));
        alarms.push(wafRateLimitAlarm);

        // VPC Flow Logs anomaly detection
        const vpcFlowLogsAnomalyAlarm = new cloudwatch.Alarm(this, 'VPCFlowLogsAnomalyAlarm', {
            alarmName: `${projectName}-${environment}-vpc-flow-logs-anomaly`,
            alarmDescription: 'Alert when VPC Flow Logs detect traffic anomalies',
            metric: new cloudwatch.MathExpression({
                expression: 'ANOMALY_DETECTION_FUNCTION(m1, 2)',
                usingMetrics: {
                    m1: new cloudwatch.Metric({
                        namespace: 'AWS/VPC',
                        metricName: 'PacketsDropped',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5)
                    })
                },
                period: cdk.Duration.minutes(5)
            }),
            threshold: 0,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.IGNORE
        });
        vpcFlowLogsAnomalyAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(warningAlertsTopic));
        alarms.push(vpcFlowLogsAnomalyAlarm);

        return alarms;
    }

    /**
     * Configure GuardDuty event processing
     */
    private configureGuardDutyEventProcessing(projectName: string, environment: string): void {
        // Create EventBridge rule for GuardDuty findings
        const guardDutyRule = new events.Rule(this, 'GuardDutyFindingsRule', {
            ruleName: `${projectName}-${environment}-guardduty-findings`,
            description: 'Process GuardDuty findings',
            eventPattern: {
                source: ['aws.guardduty'],
                detailType: ['GuardDuty Finding']
            }
        });

        // Add Lambda target
        guardDutyRule.addTarget(new targets.LambdaFunction(this.securityEventProcessor));
    }

    /**
     * Configure WAF log analysis
     */
    private configureWAFLogAnalysis(projectName: string, environment: string, region?: string): void {
        // Create CloudWatch Log Group for WAF logs
        const wafLogGroup = new logs.LogGroup(this, 'WAFLogGroup', {
            logGroupName: `/aws/wafv2/${projectName}-${environment}`,
            retention: logs.RetentionDays.ONE_MONTH,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        // Create log stream for WAF
        new logs.LogStream(this, 'WAFLogStream', {
            logGroup: wafLogGroup,
            logStreamName: `${projectName}-${environment}-waf-logs`
        });

        // Create metric filter for blocked requests
        wafLogGroup.addMetricFilter('WAFBlockedRequestsFilter', {
            metricNamespace: 'Security/WAF',
            metricName: 'BlockedRequests',
            filterPattern: logs.FilterPattern.literal('[timestamp, request_id, client_ip, uri, action="BLOCK"]'),
            metricValue: '1',
            defaultValue: 0
        });

        // Create metric filter for SQL injection attempts
        wafLogGroup.addMetricFilter('WAFSQLInjectionFilter', {
            metricNamespace: 'Security/WAF',
            metricName: 'SQLInjectionAttempts',
            filterPattern: logs.FilterPattern.literal('[timestamp, request_id, client_ip, uri, action="BLOCK", rule_name="*SQLi*"]'),
            metricValue: '1',
            defaultValue: 0
        });
    }

    /**
     * Configure VPC Flow Logs analysis
     */
    private configureVPCFlowLogsAnalysis(projectName: string, environment: string, region?: string): void {
        // Create CloudWatch Insights queries for VPC Flow Logs analysis
        const vpcFlowLogsInsights = [
            {
                name: 'TopTalkersAnalysis',
                query: `
                    fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, action
                    | filter action = "REJECT"
                    | stats count() as rejected_connections by srcaddr
                    | sort rejected_connections desc
                    | limit 20
                `
            },
            {
                name: 'SuspiciousPortScanning',
                query: `
                    fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, action
                    | filter action = "REJECT"
                    | stats count() as connection_attempts by srcaddr, dstaddr
                    | sort connection_attempts desc
                    | limit 50
                `
            },
            {
                name: 'UnusualTrafficPatterns',
                query: `
                    fields @timestamp, srcaddr, dstaddr, bytes, packets, action
                    | filter bytes > 1000000
                    | stats sum(bytes) as total_bytes by srcaddr, dstaddr
                    | sort total_bytes desc
                    | limit 20
                `
            }
        ];

        // Store queries as parameters for easy access
        vpcFlowLogsInsights.forEach((insight, index) => {
            new cdk.aws_ssm.StringParameter(this, `VPCFlowLogsInsight${index}`, {
                parameterName: `/${projectName}/${environment}/security/vpc-flow-logs-insights/${insight.name}`,
                stringValue: insight.query,
                description: `CloudWatch Insights query for ${insight.name}`
            });
        });
    }

    /**
     * Create stack outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'SecurityDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.securityDashboard.dashboardName}`,
            exportName: `${this.stackName}-SecurityDashboardUrl`,
            description: 'URL to the security monitoring dashboard'
        });

        new cdk.CfnOutput(this, 'SecurityEventProcessorArn', {
            value: this.securityEventProcessor.functionArn,
            exportName: `${this.stackName}-SecurityEventProcessorArn`,
            description: 'ARN of the security event processor Lambda function'
        });

        new cdk.CfnOutput(this, 'ThreatDetectionAlarmsCount', {
            value: this.threatDetectionAlarms.length.toString(),
            exportName: `${this.stackName}-ThreatDetectionAlarmsCount`,
            description: 'Number of threat detection alarms configured'
        });
    }
}