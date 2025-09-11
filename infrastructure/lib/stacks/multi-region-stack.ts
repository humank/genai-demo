import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface MultiRegionStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly regionType: 'primary' | 'secondary';
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly domain?: string;
    readonly primaryVpc?: ec2.IVpc;
    readonly secondaryVpc?: ec2.IVpc;
    readonly hostedZone?: route53.IHostedZone;
}

export class MultiRegionStack extends cdk.Stack {
    public readonly vpcPeeringConnection?: ec2.CfnVPCPeeringConnection;
    public readonly healthCheck?: route53.CfnHealthCheck;
    public readonly crossRegionCertificate?: certificatemanager.Certificate;
    public readonly failoverRecord?: route53.CfnRecordSet;

    constructor(scope: Construct, id: string, props: MultiRegionStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            regionType,
            primaryRegion,
            secondaryRegion,
            domain,
            primaryVpc,
            secondaryVpc,
            hostedZone
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'MultiRegion',
            RegionType: regionType,
            PrimaryRegion: primaryRegion,
            SecondaryRegion: secondaryRegion
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get multi-region configuration
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};
        const regionConfig = this.node.tryGetContext('genai-demo:regions') || {};

        // Only proceed if multi-region is enabled and we're in production
        if (multiRegionConfig['enable-dr'] && environment === 'production') {
            // Set up cross-region VPC peering if both VPCs are available
            if (multiRegionConfig['enable-cross-region-peering'] && primaryVpc && secondaryVpc && regionType === 'primary') {
                this.vpcPeeringConnection = this.createVpcPeeringConnection(
                    primaryVpc,
                    secondaryVpc,
                    primaryRegion,
                    secondaryRegion,
                    projectName,
                    environment
                );
            }

            // Set up Route 53 health checks and failover routing
            if (domain && hostedZone) {
                this.healthCheck = this.createHealthCheck(
                    domain,
                    regionType,
                    projectName,
                    environment,
                    multiRegionConfig
                );

                this.failoverRecord = this.createFailoverRecord(
                    domain,
                    hostedZone,
                    regionType,
                    projectName,
                    environment,
                    this.healthCheck
                );
            }

            // Set up cross-region certificate replication
            if (domain && regionType === 'secondary') {
                this.crossRegionCertificate = this.createCrossRegionCertificate(
                    domain,
                    hostedZone,
                    projectName,
                    environment
                );
            }

            // Create monitoring and alerting for multi-region setup
            this.createMultiRegionMonitoring(projectName, environment, regionType, multiRegionConfig);

            // Create outputs for cross-stack references
            this.createOutputs(projectName, environment, regionType, domain);
        }
    }

    private createVpcPeeringConnection(
        primaryVpc: ec2.IVpc,
        secondaryVpc: ec2.IVpc,
        primaryRegion: string,
        secondaryRegion: string,
        projectName: string,
        environment: string
    ): ec2.CfnVPCPeeringConnection {
        // Create VPC Peering Connection from primary to secondary region
        const peeringConnection = new ec2.CfnVPCPeeringConnection(this, 'CrossRegionVPCPeering', {
            vpcId: primaryVpc.vpcId,
            peerVpcId: secondaryVpc.vpcId,
            peerRegion: secondaryRegion,
            tags: [
                {
                    key: 'Name',
                    value: `${projectName}-${environment}-cross-region-peering`
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
                    key: 'PrimaryRegion',
                    value: primaryRegion
                },
                {
                    key: 'SecondaryRegion',
                    value: secondaryRegion
                }
            ]
        });

        return peeringConnection;
    }

    private createHealthCheck(
        domain: string,
        regionType: 'primary' | 'secondary',
        projectName: string,
        environment: string,
        multiRegionConfig: any
    ): route53.CfnHealthCheck {
        const healthCheckInterval = multiRegionConfig['health-check-interval'] || 30;
        const failureThreshold = multiRegionConfig['health-check-failure-threshold'] || 3;

        // Create health check for the region's endpoint
        const healthCheck = new route53.CfnHealthCheck(this, `${regionType}HealthCheck`, {
            type: 'HTTPS',
            resourcePath: '/actuator/health',
            fullyQualifiedDomainName: `${regionType === 'primary' ? 'api' : 'api-dr'}.${domain}`,
            port: 443,
            requestInterval: healthCheckInterval,
            failureThreshold: failureThreshold,
            measureLatency: true,
            enableSni: true,
            tags: [
                {
                    key: 'Name',
                    value: `${projectName}-${environment}-${regionType}-health-check`
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
                    key: 'RegionType',
                    value: regionType
                }
            ]
        });

        return healthCheck;
    }

    private createFailoverRecord(
        domain: string,
        hostedZone: route53.IHostedZone,
        regionType: 'primary' | 'secondary',
        projectName: string,
        environment: string,
        healthCheck?: route53.CfnHealthCheck
    ): route53.CfnRecordSet {
        // Create failover routing policy record
        const recordName = `api.${domain}`;
        const recordType = regionType === 'primary' ? 'PRIMARY' : 'SECONDARY';

        const failoverRecord = new route53.CfnRecordSet(this, `${regionType}FailoverRecord`, {
            hostedZoneId: hostedZone.hostedZoneId,
            name: recordName,
            type: 'A',
            setIdentifier: `${projectName}-${environment}-${regionType}`,
            failover: recordType,
            ttl: '60', // Short TTL for faster failover
            resourceRecords: [
                // This will be updated with actual ALB IP addresses during deployment
                regionType === 'primary' ? '1.2.3.4' : '5.6.7.8'
            ],
            ...(healthCheck && regionType === 'primary' && {
                healthCheckId: healthCheck.attrHealthCheckId
            })
        });

        return failoverRecord;
    }

    private createCrossRegionCertificate(
        domain: string,
        hostedZone?: route53.IHostedZone,
        projectName?: string,
        environment?: string
    ): certificatemanager.Certificate {
        // Create certificate in secondary region for cross-region replication
        const certificate = new certificatemanager.Certificate(this, 'CrossRegionCertificate', {
            certificateName: `${projectName}-${environment}-dr-certificate`,
            domainName: domain,
            validation: hostedZone
                ? certificatemanager.CertificateValidation.fromDns(hostedZone)
                : certificatemanager.CertificateValidation.fromEmail(),
            subjectAlternativeNames: [
                `api-dr.${domain}`,
                `cmc-dr.${domain}`,
                `shop-dr.${domain}`,
                `grafana-dr.${domain}`,
                `logs-dr.${domain}`
            ]
        });

        // Add tags
        cdk.Tags.of(certificate).add('Name', `${projectName}-${environment}-dr-certificate`);
        cdk.Tags.of(certificate).add('Environment', environment || 'production');
        cdk.Tags.of(certificate).add('Project', projectName || 'genai-demo');
        cdk.Tags.of(certificate).add('RegionType', 'secondary');

        return certificate;
    }

    private createMultiRegionMonitoring(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        multiRegionConfig: any
    ): void {
        // Create SNS topic for multi-region alerts
        const multiRegionAlertsTopic = new sns.Topic(this, 'MultiRegionAlertsTopic', {
            topicName: `${projectName}-${environment}-${regionType}-multi-region-alerts`,
            displayName: `Multi-Region Alerts for ${projectName} ${environment} (${regionType})`
        });

        // Create CloudWatch alarms for multi-region monitoring
        if (this.healthCheck) {
            const healthCheckAlarm = new cloudwatch.Alarm(this, 'HealthCheckAlarm', {
                alarmName: `${projectName}-${environment}-${regionType}-health-check-failure`,
                alarmDescription: `Health check failure alarm for ${regionType} region`,
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Route53',
                    metricName: 'HealthCheckStatus',
                    dimensionsMap: {
                        HealthCheckId: this.healthCheck.attrHealthCheckId
                    },
                    statistic: 'Minimum',
                    period: cdk.Duration.minutes(1)
                }),
                threshold: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
                evaluationPeriods: multiRegionConfig['health-check-failure-threshold'] || 3,
                treatMissingData: cloudwatch.TreatMissingData.BREACHING
            });

            // Add SNS action to the alarm
            healthCheckAlarm.addAlarmAction({
                bind: () => ({ alarmActionArn: multiRegionAlertsTopic.topicArn })
            });
        }

        // Create VPC Peering Connection monitoring
        if (this.vpcPeeringConnection) {
            const peeringConnectionAlarm = new cloudwatch.Alarm(this, 'VPCPeeringConnectionAlarm', {
                alarmName: `${projectName}-${environment}-vpc-peering-connection-status`,
                alarmDescription: 'VPC Peering Connection status alarm',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/VPC',
                    metricName: 'PeeringConnectionStatus',
                    dimensionsMap: {
                        VpcPeeringConnectionId: this.vpcPeeringConnection.ref
                    },
                    statistic: 'Maximum',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
                evaluationPeriods: 2,
                treatMissingData: cloudwatch.TreatMissingData.BREACHING
            });

            peeringConnectionAlarm.addAlarmAction({
                bind: () => ({ alarmActionArn: multiRegionAlertsTopic.topicArn })
            });
        }

        // Add tags to monitoring resources
        cdk.Tags.of(multiRegionAlertsTopic).add('Name', `${projectName}-${environment}-${regionType}-multi-region-alerts`);
        cdk.Tags.of(multiRegionAlertsTopic).add('Environment', environment);
        cdk.Tags.of(multiRegionAlertsTopic).add('Project', projectName);
        cdk.Tags.of(multiRegionAlertsTopic).add('RegionType', regionType);
    }

    private createOutputs(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        domain?: string
    ): void {
        // VPC Peering Connection outputs
        if (this.vpcPeeringConnection) {
            new cdk.CfnOutput(this, 'VPCPeeringConnectionId', {
                value: this.vpcPeeringConnection.ref,
                description: 'Cross-region VPC Peering Connection ID',
                exportName: `${projectName}-${environment}-vpc-peering-connection-id`
            });
        }

        // Health Check outputs
        if (this.healthCheck) {
            new cdk.CfnOutput(this, 'HealthCheckId', {
                value: this.healthCheck.attrHealthCheckId,
                description: `Route 53 Health Check ID for ${regionType} region`,
                exportName: `${projectName}-${environment}-${regionType}-health-check-id`
            });
        }

        // Cross-region certificate outputs
        if (this.crossRegionCertificate) {
            new cdk.CfnOutput(this, 'CrossRegionCertificateArn', {
                value: this.crossRegionCertificate.certificateArn,
                description: 'Cross-region certificate ARN for DR deployment',
                exportName: `${projectName}-${environment}-dr-certificate-arn`
            });
        }

        // Failover record outputs
        if (this.failoverRecord && domain) {
            new cdk.CfnOutput(this, 'FailoverRecordName', {
                value: `api.${domain}`,
                description: `Failover DNS record name for ${regionType} region`,
                exportName: `${projectName}-${environment}-${regionType}-failover-record-name`
            });
        }

        // Multi-region configuration outputs
        new cdk.CfnOutput(this, 'RegionType', {
            value: regionType,
            description: 'Region type (primary or secondary)',
            exportName: `${projectName}-${environment}-${regionType}-region-type`
        });

        new cdk.CfnOutput(this, 'MultiRegionEnabled', {
            value: 'true',
            description: 'Multi-region disaster recovery enabled',
            exportName: `${projectName}-${environment}-multi-region-enabled`
        });
    }
}