import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface CertificateStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain?: string;
}

export class CertificateStack extends cdk.Stack {
    public readonly hostedZone?: route53.IHostedZone;
    public readonly certificate?: certificatemanager.Certificate;
    public readonly wildcardCertificate?: certificatemanager.Certificate;

    constructor(scope: Construct, id: string, props: CertificateStackProps) {
        super(scope, id, props);

        const { environment, projectName, domain } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Certificate'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Only create certificates and DNS resources if domain is provided
        if (domain) {
            // Lookup hosted zone
            this.hostedZone = this.lookupHostedZone(domain);

            // Create certificates
            this.certificate = this.createCertificate(domain, this.hostedZone, projectName, environment);
            this.wildcardCertificate = this.createWildcardCertificate(domain, this.hostedZone, projectName, environment);

            // Set up certificate validation monitoring
            this.createCertificateValidationMonitoring(projectName, environment);

            // Create outputs for cross-stack references
            this.createOutputs(projectName, environment, domain);
        }
    }

    private lookupHostedZone(domain: string): route53.IHostedZone {
        // Extract the root domain (e.g., kimkao.io from dev.kimkao.io)
        const domainParts = domain.split('.');
        const rootDomain = domainParts.length > 2 ? domainParts.slice(-2).join('.') : domain;

        return route53.HostedZone.fromLookup(this, 'HostedZone', {
            domainName: rootDomain
        });
    }

    private createCertificate(
        domain: string,
        hostedZone: route53.IHostedZone,
        projectName: string,
        environment: string
    ): certificatemanager.Certificate {
        return new certificatemanager.Certificate(this, 'Certificate', {
            certificateName: `${projectName}-${environment}-certificate`,
            domainName: domain,
            validation: certificatemanager.CertificateValidation.fromDns(hostedZone),
            subjectAlternativeNames: [
                // Add common subdomains for the environment
                `api.${domain}`,
                `cmc.${domain}`,
                `shop.${domain}`,
                `grafana.${domain}`,
                `logs.${domain}`
            ]
        });
    }

    private createWildcardCertificate(
        domain: string,
        hostedZone: route53.IHostedZone,
        projectName: string,
        environment: string
    ): certificatemanager.Certificate {
        // Extract the root domain for wildcard certificate
        const domainParts = domain.split('.');
        const rootDomain = domainParts.length > 2 ? domainParts.slice(-2).join('.') : domain;

        return new certificatemanager.Certificate(this, 'WildcardCertificate', {
            certificateName: `${projectName}-${environment}-wildcard-certificate`,
            domainName: `*.${rootDomain}`,
            validation: certificatemanager.CertificateValidation.fromDns(hostedZone),
            subjectAlternativeNames: [rootDomain] // Include the root domain as well
        });
    }

    private createCertificateValidationMonitoring(projectName: string, environment: string): void {
        if (!this.certificate || !this.wildcardCertificate) {
            return;
        }

        // Create SNS topic for certificate alerts
        const certMonitoringTopic = new sns.Topic(this, 'CertMonitoringAlertsTopic', {
            topicName: `${projectName}-${environment}-certificate-alerts`,
            displayName: `Certificate Alerts for ${projectName} ${environment}`
        });

        // CloudWatch alarm for certificate validation status
        const certificateValidationAlarm = new cloudwatch.Alarm(this, 'CertMonitoringValidationAlarm', {
            alarmName: `${projectName}-${environment}-certificate-validation-status`,
            alarmDescription: 'Monitor certificate validation status',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/CertificateManager',
                metricName: 'DaysToExpiry',
                dimensionsMap: {
                    CertificateArn: this.certificate.certificateArn
                },
                statistic: 'Minimum',
                period: cdk.Duration.hours(1)
            }),
            threshold: 30, // Alert when certificate expires in 30 days
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        // Add SNS action to the alarm
        certificateValidationAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: certMonitoringTopic.topicArn })
        });

        // CloudWatch alarm for wildcard certificate validation status
        const wildcardCertificateValidationAlarm = new cloudwatch.Alarm(this, 'WildcardCertMonitoringValidationAlarm', {
            alarmName: `${projectName}-${environment}-wildcard-certificate-validation-status`,
            alarmDescription: 'Monitor wildcard certificate validation status',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/CertificateManager',
                metricName: 'DaysToExpiry',
                dimensionsMap: {
                    CertificateArn: this.wildcardCertificate.certificateArn
                },
                statistic: 'Minimum',
                period: cdk.Duration.hours(1)
            }),
            threshold: 30, // Alert when certificate expires in 30 days
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        // Add SNS action to the wildcard certificate alarm
        wildcardCertificateValidationAlarm.addAlarmAction({
            bind: () => ({ alarmActionArn: certMonitoringTopic.topicArn })
        });

        // Add tags to monitoring resources
        cdk.Tags.of(certMonitoringTopic).add('Name', `${projectName}-${environment}-certificate-alerts`);
        cdk.Tags.of(certMonitoringTopic).add('Environment', environment);
        cdk.Tags.of(certMonitoringTopic).add('Project', projectName);
        cdk.Tags.of(certMonitoringTopic).add('Service', 'Certificate-Monitoring');

        cdk.Tags.of(certificateValidationAlarm).add('Name', `${projectName}-${environment}-certificate-validation-alarm`);
        cdk.Tags.of(certificateValidationAlarm).add('Environment', environment);
        cdk.Tags.of(certificateValidationAlarm).add('Project', projectName);
        cdk.Tags.of(certificateValidationAlarm).add('Service', 'Certificate-Monitoring');

        cdk.Tags.of(wildcardCertificateValidationAlarm).add('Name', `${projectName}-${environment}-wildcard-certificate-validation-alarm`);
        cdk.Tags.of(wildcardCertificateValidationAlarm).add('Environment', environment);
        cdk.Tags.of(wildcardCertificateValidationAlarm).add('Project', projectName);
        cdk.Tags.of(wildcardCertificateValidationAlarm).add('Service', 'Certificate-Monitoring');
    }

    private createOutputs(projectName: string, environment: string, domain: string): void {
        if (!this.certificate || !this.wildcardCertificate || !this.hostedZone) {
            return;
        }

        // Certificate ARNs for Kubernetes Ingress use
        new cdk.CfnOutput(this, 'CertificateArn', {
            value: this.certificate.certificateArn,
            description: 'ACM Certificate ARN for domain - use in Kubernetes Ingress annotations',
            exportName: `${projectName}-${environment}-certificate-arn`
        });

        new cdk.CfnOutput(this, 'WildcardCertificateArn', {
            value: this.wildcardCertificate.certificateArn,
            description: 'ACM Wildcard Certificate ARN for domain - use in Kubernetes Ingress annotations',
            exportName: `${projectName}-${environment}-wildcard-certificate-arn`
        });

        // Hosted Zone ID for cross-stack references
        new cdk.CfnOutput(this, 'HostedZoneId', {
            value: this.hostedZone.hostedZoneId,
            description: 'Route 53 Hosted Zone ID for cross-stack DNS management',
            exportName: `${projectName}-${environment}-hosted-zone-id`
        });

        new cdk.CfnOutput(this, 'HostedZoneName', {
            value: this.hostedZone.zoneName,
            description: 'Route 53 Hosted Zone Name for cross-stack DNS management',
            exportName: `${projectName}-${environment}-hosted-zone-name`
        });

        // Domain configuration
        new cdk.CfnOutput(this, 'Domain', {
            value: domain,
            description: 'Primary domain name',
            exportName: `${projectName}-${environment}-domain`
        });

        // Certificate validation method
        new cdk.CfnOutput(this, 'CertificateValidationMethod', {
            value: 'DNS',
            description: 'Certificate validation method used',
            exportName: `${projectName}-${environment}-certificate-validation-method`
        });

        // Certificate monitoring outputs
        const certMonitoringTopic = this.node.findChild('CertMonitoringAlertsTopic') as sns.Topic;
        const certificateValidationAlarm = this.node.findChild('CertMonitoringValidationAlarm') as cloudwatch.Alarm;
        const wildcardCertificateValidationAlarm = this.node.findChild('WildcardCertMonitoringValidationAlarm') as cloudwatch.Alarm;

        if (certMonitoringTopic) {
            new cdk.CfnOutput(this, 'CertMonitoringAlertsTopicArnOutput', {
                value: certMonitoringTopic.topicArn,
                description: 'SNS topic ARN for certificate alerts',
                exportName: `${projectName}-${environment}-certificate-alerts-topic`
            });
        }

        if (certificateValidationAlarm) {
            new cdk.CfnOutput(this, 'CertMonitoringValidationAlarmOutput', {
                value: certificateValidationAlarm.alarmName,
                description: 'CloudWatch alarm name for certificate validation monitoring',
                exportName: `${projectName}-${environment}-certificate-validation-alarm`
            });
        }

        if (wildcardCertificateValidationAlarm) {
            new cdk.CfnOutput(this, 'WildcardCertMonitoringValidationAlarmOutput', {
                value: wildcardCertificateValidationAlarm.alarmName,
                description: 'CloudWatch alarm name for wildcard certificate validation monitoring',
                exportName: `${projectName}-${environment}-wildcard-certificate-validation-alarm`
            });
        }
    }
}