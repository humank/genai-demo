import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export interface NetworkStackProps extends cdk.StackProps {
    // Add any specific props here
}

export class NetworkStack extends cdk.Stack {
    public readonly vpc: ec2.Vpc;
    public readonly securityGroups: {
        alb: ec2.SecurityGroup;
        app: ec2.SecurityGroup;
        database: ec2.SecurityGroup;
    };

    constructor(scope: Construct, id: string, props?: NetworkStackProps) {
        super(scope, id, props);

        // Create VPC
        this.vpc = new ec2.Vpc(this, 'VPC', {
            maxAzs: 2,
            ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16'),
            natGateways: 1,
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
                    cidrMask: 28,
                    name: 'Database',
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                },
            ],
        });

        // Create Security Groups
        this.securityGroups = {
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
        };

        // Configure security group rules
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

        // Outputs
        new cdk.CfnOutput(this, 'VpcId', {
            value: this.vpc.vpcId,
            exportName: `${this.stackName}-VpcId`,
        });

        new cdk.CfnOutput(this, 'ALBSecurityGroupId', {
            value: this.securityGroups.alb.securityGroupId,
            exportName: `${this.stackName}-ALBSecurityGroupId`,
        });
    }
}