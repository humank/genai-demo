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
}

export class CoreInfrastructureStack extends cdk.Stack {
    public readonly loadBalancer: elbv2.ApplicationLoadBalancer;

    constructor(scope: Construct, id: string, props: CoreInfrastructureStackProps) {
        super(scope, id, props);

        // Create Application Load Balancer
        this.loadBalancer = new elbv2.ApplicationLoadBalancer(this, 'ApplicationLoadBalancer', {
            vpc: props.vpc,
            internetFacing: true,
            securityGroup: props.securityGroups.alb,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PUBLIC,
            },
        });

        // Create a default target group (will be used by applications)
        const targetGroup = new elbv2.ApplicationTargetGroup(this, 'DefaultTargetGroup', {
            vpc: props.vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/health',
                protocol: elbv2.Protocol.HTTP,
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(5),
                interval: cdk.Duration.seconds(30),
            },
        });

        // Create HTTP listener (will redirect to HTTPS in production)
        const httpListener = this.loadBalancer.addListener('HttpListener', {
            port: 80,
            protocol: elbv2.ApplicationProtocol.HTTP,
            defaultTargetGroups: [targetGroup],
        });

        // Outputs
        new cdk.CfnOutput(this, 'LoadBalancerDNS', {
            value: this.loadBalancer.loadBalancerDnsName,
            exportName: `${this.stackName}-LoadBalancerDNS`,
        });

        new cdk.CfnOutput(this, 'LoadBalancerArn', {
            value: this.loadBalancer.loadBalancerArn,
            exportName: `${this.stackName}-LoadBalancerArn`,
        });
    }
}