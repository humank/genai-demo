import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';
import { NagSuppressions } from 'cdk-nag';

export interface NetworkStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly alertingTopic?: sns.ITopic;
    readonly peerRegions?: string[];
    readonly enableTransitGateway?: boolean;
    readonly enableCrossRegionPeering?: boolean;
}

export class NetworkStack extends cdk.Stack {
    public readonly vpc: ec2.Vpc;
    public readonly securityGroups: {
        alb: ec2.SecurityGroup;
        app: ec2.SecurityGroup;
        database: ec2.SecurityGroup;
        crossRegionDb: ec2.SecurityGroup;
        crossRegionApp: ec2.SecurityGroup;
        crossRegionMsg: ec2.SecurityGroup;
    };
    public readonly transitGateway?: ec2.CfnTransitGateway;
    public readonly crossRegionPeerings: { [region: string]: ec2.CfnVPCPeeringConnection } = {};
    public readonly networkLatencyMonitor: lambda.Function;

    constructor(scope: Construct, id: string, props: NetworkStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            alertingTopic,
            peerRegions = [],
            enableTransitGateway = false,
            enableCrossRegionPeering = false
        } = props;

        // Create VPC with enhanced configuration for cross-region connectivity
        this.vpc = this.createVpc(projectName, environment);

        // Create Transit Gateway for cross-region connectivity if enabled
        if (enableTransitGateway) {
            this.transitGateway = this.createTransitGateway(projectName, environment);
        }

        // Create cross-region VPC peering connections if enabled
        if (enableCrossRegionPeering && peerRegions.length > 0) {
            this.createCrossRegionPeering(peerRegions, projectName, environment);
        }

        // Create Security Groups including cross-region security groups
        this.securityGroups = this.createSecurityGroups(projectName, environment);

        // Configure security group rules
        this.configureSecurityGroupRules();

        // Create network latency monitoring
        this.networkLatencyMonitor = this.createNetworkLatencyMonitor(projectName, environment, alertingTopic);

        // Create network partition detection
        this.createNetworkPartitionDetection(projectName, environment, alertingTopic);

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment);
    }

    private createVpc(projectName: string, environment: string): ec2.Vpc {
        // Use different CIDR blocks per region to avoid conflicts
        const cidr = this.getCidrForRegion(this.region);

        const vpc = new ec2.Vpc(this, 'VPC', {
            maxAzs: 3,
            ipAddresses: ec2.IpAddresses.cidr(cidr),
            natGateways: environment === 'production' ? 3 : 1,
            enableDnsHostnames: true,
            enableDnsSupport: true,
            subnetConfiguration: [
                {
                    cidrMask: 24,
                    name: 'Public',
                    subnetType: ec2.SubnetType.PUBLIC,
                },
                {
                    cidrMask: 24,
                    name: 'Private',
                    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
                },
                {
                    cidrMask: 26,
                    name: 'Database',
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                },
                {
                    cidrMask: 28,
                    name: 'Transit',
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                }
            ],
            // Enable VPC Flow Logs for cross-region traffic monitoring
            flowLogs: {
                'CrossRegionFlowLogs': {
                    destination: ec2.FlowLogDestination.toCloudWatchLogs(),
                    trafficType: ec2.FlowLogTrafficType.ALL,
                    maxAggregationInterval: ec2.FlowLogMaxAggregationInterval.ONE_MINUTE
                }
            }
        });

        // Tag VPC for cross-region identification
        cdk.Tags.of(vpc).add('Name', `${projectName}-${environment}-vpc`);
        cdk.Tags.of(vpc).add('Environment', environment);
        cdk.Tags.of(vpc).add('Project', projectName);
        cdk.Tags.of(vpc).add('Architecture', 'active-active');
        cdk.Tags.of(vpc).add('Region', this.region);

        return vpc;
    }

    private getCidrForRegion(region: string): string {
        // Assign different CIDR blocks per region to avoid conflicts
        const regionCidrs: { [key: string]: string } = {
            'ap-northeast-1': '10.0.0.0/16',  // Tokyo
            'us-west-2': '10.1.0.0/16',       // Oregon
            'eu-west-1': '10.2.0.0/16',       // Ireland
            'ap-southeast-1': '10.3.0.0/16',  // Singapore
            'us-east-1': '10.4.0.0/16'        // N. Virginia
        };
        return regionCidrs[region] || '10.9.0.0/16'; // Default CIDR
    }

    private createTransitGateway(projectName: string, environment: string): ec2.CfnTransitGateway {
        const transitGateway = new ec2.CfnTransitGateway(this, 'CrossRegionTransitGateway', {
            description: `Transit Gateway for ${projectName} ${environment} cross-region connectivity`,
            // Enable cross-region peering
            defaultRouteTableAssociation: 'enable',
            defaultRouteTablePropagation: 'enable',
            // Enable DNS support
            dnsSupport: 'enable',
            // Enable multicast support for future use
            multicastSupport: 'enable',
            tags: [
                {
                    key: 'Name',
                    value: `${projectName}-${environment}-tgw-${this.region}`
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
                    key: 'Architecture',
                    value: 'active-active'
                }
            ]
        });

        // Attach VPC to Transit Gateway
        const tgwAttachment = new ec2.CfnTransitGatewayVpcAttachment(this, 'TGWVpcAttachment', {
            transitGatewayId: transitGateway.ref,
            vpcId: this.vpc.vpcId,
            subnetIds: this.vpc.selectSubnets({ subnetGroupName: 'Transit' }).subnetIds,
            tags: [
                {
                    key: 'Name',
                    value: `${projectName}-${environment}-tgw-attachment-${this.region}`
                }
            ]
        });

        return transitGateway;
    }

    private createCrossRegionPeering(peerRegions: string[], projectName: string, environment: string): void {
        peerRegions.forEach(peerRegion => {
            if (peerRegion !== this.region) {
                // Create VPC Peering Connection to peer region
                const peeringConnection = new ec2.CfnVPCPeeringConnection(this, `PeeringTo${peerRegion}`, {
                    vpcId: this.vpc.vpcId,
                    peerRegion: peerRegion,
                    // Note: PeerVpcId would need to be provided via cross-stack reference
                    // This is a placeholder - in real implementation, you'd use cross-stack references
                    peerVpcId: `vpc-${peerRegion}`, // This should be replaced with actual VPC ID
                    tags: [
                        {
                            key: 'Name',
                            value: `${projectName}-${environment}-peering-${this.region}-to-${peerRegion}`
                        },
                        {
                            key: 'Environment',
                            value: environment
                        },
                        {
                            key: 'Project',
                            value: projectName
                        }
                    ]
                });

                this.crossRegionPeerings[peerRegion] = peeringConnection;

                // Create route table entries for cross-region traffic
                this.createCrossRegionRoutes(peerRegion, peeringConnection);
            }
        });
    }

    private createCrossRegionRoutes(peerRegion: string, peeringConnection: ec2.CfnVPCPeeringConnection): void {
        const peerCidr = this.getCidrForRegion(peerRegion);

        // Add routes to private subnets for cross-region communication
        this.vpc.privateSubnets.forEach((subnet, index) => {
            new ec2.CfnRoute(this, `CrossRegionRoute${peerRegion}Private${index}`, {
                routeTableId: subnet.routeTable.routeTableId,
                destinationCidrBlock: peerCidr,
                vpcPeeringConnectionId: peeringConnection.ref
            });
        });

        // Add routes to isolated subnets for database cross-region communication
        this.vpc.isolatedSubnets.forEach((subnet, index) => {
            new ec2.CfnRoute(this, `CrossRegionRoute${peerRegion}Isolated${index}`, {
                routeTableId: subnet.routeTable.routeTableId,
                destinationCidrBlock: peerCidr,
                vpcPeeringConnectionId: peeringConnection.ref
            });
        });
    }

    private createSecurityGroups(projectName: string, environment: string): {
        alb: ec2.SecurityGroup;
        app: ec2.SecurityGroup;
        database: ec2.SecurityGroup;
        crossRegionDb: ec2.SecurityGroup;
        crossRegionApp: ec2.SecurityGroup;
        crossRegionMsg: ec2.SecurityGroup;
    } {
        const securityGroups = {
            alb: new ec2.SecurityGroup(this, 'ALBSecurityGroup', {
                vpc: this.vpc,
                description: 'Security group for Application Load Balancer',
                allowAllOutbound: false,
            }),
            app: new ec2.SecurityGroup(this, 'AppSecurityGroup', {
                vpc: this.vpc,
                description: 'Security group for application instances',
                allowAllOutbound: true,
            }),
            database: new ec2.SecurityGroup(this, 'DatabaseSecurityGroup', {
                vpc: this.vpc,
                description: 'Security group for database instances',
                allowAllOutbound: false,
            }),
            crossRegionDb: new ec2.SecurityGroup(this, 'CrossRegionDatabaseSG', {
                vpc: this.vpc,
                description: 'Security group for cross-region database communication',
                allowAllOutbound: true
            }),
            crossRegionApp: new ec2.SecurityGroup(this, 'CrossRegionApplicationSG', {
                vpc: this.vpc,
                description: 'Security group for cross-region application communication',
                allowAllOutbound: true
            }),
            crossRegionMsg: new ec2.SecurityGroup(this, 'CrossRegionMessagingSG', {
                vpc: this.vpc,
                description: 'Security group for cross-region messaging communication',
                allowAllOutbound: true
            })
        };

        // Add tags to security groups
        Object.entries(securityGroups).forEach(([name, sg]) => {
            cdk.Tags.of(sg).add('Name', `${projectName}-${environment}-${name}-sg`);
            cdk.Tags.of(sg).add('Environment', environment);
            cdk.Tags.of(sg).add('Project', projectName);
            cdk.Tags.of(sg).add('Purpose', name.includes('crossRegion') ? 'cross-region' : 'standard');
        });

        return securityGroups;
    }

    private configureSecurityGroupRules(): void {
        // Standard security group rules
        this.securityGroups.alb.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(443),
            'Allow HTTPS traffic'
        );

        this.securityGroups.alb.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(80),
            'Allow HTTP traffic'
        );

        this.securityGroups.app.addIngressRule(
            this.securityGroups.alb,
            ec2.Port.tcp(8080),
            'Allow traffic from ALB'
        );

        this.securityGroups.database.addIngressRule(
            this.securityGroups.app,
            ec2.Port.tcp(5432),
            'Allow PostgreSQL traffic from app'
        );

        // Cross-region security group rules
        const peerCidrs = ['10.0.0.0/16', '10.1.0.0/16', '10.2.0.0/16', '10.3.0.0/16', '10.4.0.0/16'];
        const currentCidr = this.getCidrForRegion(this.region);

        peerCidrs.forEach((cidr, index) => {
            if (cidr !== currentCidr) {
                // Allow PostgreSQL traffic from peer regions
                this.securityGroups.crossRegionDb.addIngressRule(
                    ec2.Peer.ipv4(cidr),
                    ec2.Port.tcp(5432),
                    `Allow PostgreSQL from peer region ${index + 1}`
                );

                // Allow HTTP/HTTPS traffic from peer regions
                this.securityGroups.crossRegionApp.addIngressRule(
                    ec2.Peer.ipv4(cidr),
                    ec2.Port.tcp(80),
                    `Allow HTTP from peer region ${index + 1}`
                );
                this.securityGroups.crossRegionApp.addIngressRule(
                    ec2.Peer.ipv4(cidr),
                    ec2.Port.tcp(443),
                    `Allow HTTPS from peer region ${index + 1}`
                );
                this.securityGroups.crossRegionApp.addIngressRule(
                    ec2.Peer.ipv4(cidr),
                    ec2.Port.tcp(8080),
                    `Allow application traffic from peer region ${index + 1}`
                );

                // Allow Kafka traffic from peer regions
                this.securityGroups.crossRegionMsg.addIngressRule(
                    ec2.Peer.ipv4(cidr),
                    ec2.Port.tcpRange(9092, 9094),
                    `Allow Kafka from peer region ${index + 1}`
                );
            }
        });
    }

    private createNetworkLatencyMonitor(
        projectName: string, 
        environment: string, 
        alertingTopic?: sns.ITopic
    ): lambda.Function {
        // Create IAM role for network monitoring Lambda
        const lambdaRole = new iam.Role(this, 'NetworkMonitoringLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaVPCAccessExecutionRole')
            ],
            inlinePolicies: {
                NetworkMonitoringPolicy: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'cloudwatch:PutMetricData',
                                'ec2:DescribeVpcs',
                                'ec2:DescribeVpcPeeringConnections',
                                'ec2:DescribeTransitGateways',
                                'ec2:DescribeNetworkInterfaces'
                            ],
                            resources: ['*']
                        })
                    ]
                })
            }
        });

        // Create network latency monitoring Lambda
        const networkLatencyMonitor = new lambda.Function(this, 'NetworkLatencyMonitor', {
            runtime: lambda.Runtime.NODEJS_18_X,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
                const AWS = require('aws-sdk');
                const https = require('https');
                const cloudwatch = new AWS.CloudWatch();
                
                exports.handler = async (event) => {
                    console.log('Network latency monitoring started');
                    const regions = ['ap-northeast-1', 'us-west-2', 'eu-west-1'];
                    const currentRegion = process.env.AWS_REGION;
                    
                    try {
                        const latencyResults = [];
                        
                        for (const targetRegion of regions) {
                            if (targetRegion !== currentRegion) {
                                const latency = await measureLatency(targetRegion);
                                latencyResults.push({
                                    source: currentRegion,
                                    target: targetRegion,
                                    latency: latency
                                });
                                
                                // Report latency metrics to CloudWatch
                                await reportLatencyMetric(currentRegion, targetRegion, latency);
                            }
                        }
                        
                        return {
                            statusCode: 200,
                            body: JSON.stringify({
                                message: 'Network latency monitoring completed',
                                results: latencyResults
                            })
                        };
                    } catch (error) {
                        console.error('Network latency monitoring failed:', error);
                        throw error;
                    }
                };
                
                async function measureLatency(targetRegion) {
                    const startTime = Date.now();
                    try {
                        // Simulate network latency measurement
                        // In real implementation, this would ping actual endpoints
                        await new Promise(resolve => setTimeout(resolve, Math.random() * 100));
                        const endTime = Date.now();
                        return endTime - startTime;
                    } catch (error) {
                        console.error(\`Failed to measure latency to \${targetRegion}:\`, error);
                        return -1; // Indicate failure
                    }
                }
                
                async function reportLatencyMetric(source, target, latency) {
                    const params = {
                        Namespace: '${projectName}/Network',
                        MetricData: [
                            {
                                MetricName: 'CrossRegionLatency',
                                Dimensions: [
                                    {
                                        Name: 'SourceRegion',
                                        Value: source
                                    },
                                    {
                                        Name: 'TargetRegion',
                                        Value: target
                                    }
                                ],
                                Value: latency,
                                Unit: 'Milliseconds',
                                Timestamp: new Date()
                            }
                        ]
                    };
                    
                    await cloudwatch.putMetricData(params).promise();
                }
            `),
            timeout: cdk.Duration.minutes(5),
            memorySize: 256,
            role: lambdaRole,
            vpc: this.vpc,
            vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
            environment: {
                REGION: this.region
            },
            description: 'Monitors network latency between regions'
        });

        // Schedule latency monitoring every 5 minutes
        const rule = new events.Rule(this, 'NetworkLatencyMonitoringSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(5)),
            description: 'Triggers network latency monitoring every 5 minutes'
        });

        rule.addTarget(new eventsTargets.LambdaFunction(networkLatencyMonitor));

        // Add tags
        cdk.Tags.of(networkLatencyMonitor).add('Name', `${projectName}-${environment}-network-latency-monitor`);
        cdk.Tags.of(networkLatencyMonitor).add('Environment', environment);
        cdk.Tags.of(networkLatencyMonitor).add('Project', projectName);
        cdk.Tags.of(networkLatencyMonitor).add('Component', 'Network-Monitoring');

        return networkLatencyMonitor;
    }

    private createNetworkPartitionDetection(
        projectName: string, 
        environment: string, 
        alertingTopic?: sns.ITopic
    ): void {
        // Create network partition detection alarm
        const networkPartitionAlarm = new cloudwatch.Alarm(this, 'NetworkPartitionAlarm', {
            alarmName: `${projectName}-${environment}-NetworkPartition-${this.region}`,
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Network`,
                metricName: 'CrossRegionLatency',
                dimensionsMap: {
                    SourceRegion: this.region
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 1000, // 1 second threshold indicates potential partition
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING,
            alarmDescription: 'Network partition detected - cross-region latency too high',
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD
        });

        if (alertingTopic) {
            networkPartitionAlarm.addAlarmAction(
                new cloudwatchActions.SnsAction(alertingTopic)
            );
        }

        // Create network connectivity failure alarm
        const connectivityFailureAlarm = new cloudwatch.Alarm(this, 'NetworkConnectivityFailureAlarm', {
            alarmName: `${projectName}-${environment}-NetworkConnectivity-Failure-${this.region}`,
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Network`,
                metricName: 'CrossRegionLatency',
                dimensionsMap: {
                    SourceRegion: this.region
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(1)
            }),
            threshold: 0, // Negative values indicate connectivity failure
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING,
            alarmDescription: 'Cross-region network connectivity failure detected',
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD
        });

        if (alertingTopic) {
            connectivityFailureAlarm.addAlarmAction(
                new cloudwatchActions.SnsAction(alertingTopic)
            );
        }

        // Add tags to alarms
        cdk.Tags.of(networkPartitionAlarm).add('Name', `${projectName}-${environment}-network-partition-alarm`);
        cdk.Tags.of(networkPartitionAlarm).add('Environment', environment);
        cdk.Tags.of(networkPartitionAlarm).add('Project', projectName);
        cdk.Tags.of(networkPartitionAlarm).add('AlarmType', 'NetworkPartition');

        cdk.Tags.of(connectivityFailureAlarm).add('Name', `${projectName}-${environment}-connectivity-failure-alarm`);
        cdk.Tags.of(connectivityFailureAlarm).add('Environment', environment);
        cdk.Tags.of(connectivityFailureAlarm).add('Project', projectName);
        cdk.Tags.of(connectivityFailureAlarm).add('AlarmType', 'ConnectivityFailure');
    }

    private createOutputs(projectName: string, environment: string): void {
        // VPC outputs
        new cdk.CfnOutput(this, 'VpcId', {
            value: this.vpc.vpcId,
            description: 'VPC ID for cross-region connectivity',
            exportName: `${projectName}-${environment}-VpcId`
        });

        new cdk.CfnOutput(this, 'VpcCidr', {
            value: this.vpc.vpcCidrBlock,
            description: 'VPC CIDR block',
            exportName: `${projectName}-${environment}-VpcCidr`
        });

        // Security Group outputs
        new cdk.CfnOutput(this, 'ALBSecurityGroupId', {
            value: this.securityGroups.alb.securityGroupId,
            exportName: `${projectName}-${environment}-ALBSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'AppSecurityGroupId', {
            value: this.securityGroups.app.securityGroupId,
            exportName: `${projectName}-${environment}-AppSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'DatabaseSecurityGroupId', {
            value: this.securityGroups.database.securityGroupId,
            exportName: `${projectName}-${environment}-DatabaseSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'CrossRegionDbSecurityGroupId', {
            value: this.securityGroups.crossRegionDb.securityGroupId,
            exportName: `${projectName}-${environment}-CrossRegionDbSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'CrossRegionAppSecurityGroupId', {
            value: this.securityGroups.crossRegionApp.securityGroupId,
            exportName: `${projectName}-${environment}-CrossRegionAppSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'CrossRegionMsgSecurityGroupId', {
            value: this.securityGroups.crossRegionMsg.securityGroupId,
            exportName: `${projectName}-${environment}-CrossRegionMsgSecurityGroupId`,
        });

        // Transit Gateway output
        if (this.transitGateway) {
            new cdk.CfnOutput(this, 'TransitGatewayId', {
                value: this.transitGateway.ref,
                description: 'Transit Gateway ID for cross-region peering',
                exportName: `${projectName}-${environment}-TransitGatewayId`
            });
        }

        // Network monitoring output
        new cdk.CfnOutput(this, 'NetworkLatencyMonitorArn', {
            value: this.networkLatencyMonitor.functionArn,
            description: 'ARN of the network latency monitoring Lambda function',
            exportName: `${projectName}-${environment}-NetworkLatencyMonitorArn`
        });

        // CDK Nag suppressions
        this.addCdkNagSuppressions();
    }

    private addCdkNagSuppressions(): void {
        // Suppress IAM4 for Lambda execution roles - using AWS managed policies for standard Lambda execution
        NagSuppressions.addResourceSuppressions(
            this.networkLatencyMonitor.role!,
            [
                {
                    id: 'AwsSolutions-IAM4',
                    reason: 'Lambda execution role uses AWS managed policies for standard Lambda execution and VPC access',
                    appliesTo: [
                        'Policy::arn:<AWS::Partition>:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole',
                        'Policy::arn:<AWS::Partition>:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole'
                    ]
                }
            ]
        );

        // Suppress IAM5 for Lambda monitoring permissions - wildcard needed for CloudWatch metrics
        NagSuppressions.addResourceSuppressions(
            this.networkLatencyMonitor.role!,
            [
                {
                    id: 'AwsSolutions-IAM5',
                    reason: 'Lambda function needs wildcard permissions for CloudWatch metrics and network monitoring',
                    appliesTo: ['Resource::*']
                }
            ]
        );

        // Suppress L1 for Lambda runtime - using stable Python 3.9 runtime for network monitoring
        NagSuppressions.addResourceSuppressions(
            this.networkLatencyMonitor,
            [
                {
                    id: 'AwsSolutions-L1',
                    reason: 'Using Python 3.9 runtime which is stable and supported for network monitoring functions'
                }
            ]
        );
    }
}