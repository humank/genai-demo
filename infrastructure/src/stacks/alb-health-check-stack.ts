import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as elbv2Targets from 'aws-cdk-lib/aws-elasticloadbalancingv2-targets';
import { Construct } from 'constructs';

export interface ALBHealthCheckStackProps extends cdk.StackProps {
    vpc: ec2.IVpc;
    eksCluster: any; // EKS cluster reference
    environment: string;
}

/**
 * Application Load Balancer Health Check Stack
 * 
 * Configures ALB with health checks for EKS applications
 * 
 * Health Check Flow:
 * 1. ALB sends HTTP GET to /actuator/health
 * 2. Application responds with 200 OK if healthy
 * 3. ALB marks target as healthy/unhealthy based on response
 * 4. ALB routes traffic only to healthy targets
 * 
 * This is the THIRD layer of health checks:
 * - Layer 1: Spring Boot health indicators (application internal state)
 * - Layer 2: Kubernetes probes (container/pod health)
 * - Layer 3: ALB health checks (load balancer routing decisions)
 */
export class ALBHealthCheckStack extends cdk.Stack {
    public readonly alb: elbv2.ApplicationLoadBalancer;
    public readonly backendTargetGroup: elbv2.ApplicationTargetGroup;
    public readonly frontendTargetGroup: elbv2.ApplicationTargetGroup;

    constructor(scope: Construct, id: string, props: ALBHealthCheckStackProps) {
        super(scope, id, props);

        const { vpc, environment } = props;

        // Create Application Load Balancer
        this.alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
            vpc,
            internetFacing: true,
            loadBalancerName: `genai-demo-${environment}-alb`,
            deletionProtection: environment === 'production', // Enable for production
        });

        // Create Backend Target Group with Health Check
        this.backendTargetGroup = this.createBackendTargetGroup(vpc, environment);

        // Create Frontend Target Groups with Health Check
        this.frontendTargetGroup = this.createFrontendTargetGroup(vpc, environment);

        // Create Listeners
        this.createListeners();

        // Outputs
        this.createOutputs(environment);
    }

    /**
     * Create Backend API Target Group with Health Check
     */
    private createBackendTargetGroup(vpc: ec2.IVpc, environment: string): elbv2.ApplicationTargetGroup {
        const targetGroup = new elbv2.ApplicationTargetGroup(this, 'BackendTargetGroup', {
            vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP, // For EKS pods
            targetGroupName: `genai-demo-${environment}-backend-tg`,
            deregistrationDelay: cdk.Duration.seconds(30), // Faster deregistration for faster deployments
            
            // Health Check Configuration
            healthCheck: {
                enabled: true,
                path: '/actuator/health', // Spring Boot Actuator health endpoint
                protocol: elbv2.Protocol.HTTP,
                port: '8080',
                
                // Timing Configuration
                interval: cdk.Duration.seconds(30), // Check every 30 seconds
                timeout: cdk.Duration.seconds(5),   // Timeout after 5 seconds
                
                // Threshold Configuration
                healthyThresholdCount: 2,   // 2 consecutive successes = healthy
                unhealthyThresholdCount: 2, // 2 consecutive failures = unhealthy
                
                // Success Criteria
                healthyHttpCodes: '200',    // Only 200 is considered healthy
            },
        });

        // Add tags for identification
        cdk.Tags.of(targetGroup).add('Environment', environment);
        cdk.Tags.of(targetGroup).add('Application', 'backend-api');
        cdk.Tags.of(targetGroup).add('HealthCheckPath', '/actuator/health');

        return targetGroup;
    }

    /**
     * Create Frontend Target Group with Health Check
     */
    private createFrontendTargetGroup(vpc: ec2.IVpc, environment: string): elbv2.ApplicationTargetGroup {
        const targetGroup = new elbv2.ApplicationTargetGroup(this, 'FrontendTargetGroup', {
            vpc,
            port: 3000, // Next.js default port
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            targetGroupName: `genai-demo-${environment}-frontend-tg`,
            deregistrationDelay: cdk.Duration.seconds(30),
            
            // Health Check Configuration
            healthCheck: {
                enabled: true,
                path: '/health', // Frontend health endpoint
                protocol: elbv2.Protocol.HTTP,
                port: '3000',
                
                // Timing Configuration
                interval: cdk.Duration.seconds(30),
                timeout: cdk.Duration.seconds(5),
                
                // Threshold Configuration
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 2,
                
                // Success Criteria
                healthyHttpCodes: '200',
            },
        });

        cdk.Tags.of(targetGroup).add('Environment', environment);
        cdk.Tags.of(targetGroup).add('Application', 'frontend');
        cdk.Tags.of(targetGroup).add('HealthCheckPath', '/health');

        return targetGroup;
    }

    /**
     * Create ALB Listeners
     */
    private createListeners(): void {
        // HTTP Listener (redirect to HTTPS in production)
        const httpListener = this.alb.addListener('HttpListener', {
            port: 80,
            protocol: elbv2.ApplicationProtocol.HTTP,
            defaultAction: elbv2.ListenerAction.redirect({
                protocol: 'HTTPS',
                port: '443',
                permanent: true,
            }),
        });

        // HTTPS Listener (requires certificate - configure separately)
        // Uncomment when certificate is available
        /*
        const httpsListener = this.alb.addListener('HttpsListener', {
            port: 443,
            protocol: elbv2.ApplicationProtocol.HTTPS,
            certificates: [certificate],
            defaultAction: elbv2.ListenerAction.forward([this.frontendTargetGroup]),
        });

        // Add routing rules
        httpsListener.addTargetGroups('BackendRule', {
            targetGroups: [this.backendTargetGroup],
            priority: 10,
            conditions: [
                elbv2.ListenerCondition.pathPatterns(['/api/*']),
            ],
        });
        */
    }

    /**
     * Create CloudFormation Outputs
     */
    private createOutputs(environment: string): void {
        new cdk.CfnOutput(this, 'ALBDnsName', {
            value: this.alb.loadBalancerDnsName,
            description: 'ALB DNS Name',
            exportName: `${environment}-alb-dns-name`,
        });

        new cdk.CfnOutput(this, 'BackendTargetGroupArn', {
            value: this.backendTargetGroup.targetGroupArn,
            description: 'Backend Target Group ARN',
            exportName: `${environment}-backend-target-group-arn`,
        });

        new cdk.CfnOutput(this, 'FrontendTargetGroupArn', {
            value: this.frontendTargetGroup.targetGroupArn,
            description: 'Frontend Target Group ARN',
            exportName: `${environment}-frontend-target-group-arn`,
        });

        // Output health check configuration for reference
        new cdk.CfnOutput(this, 'HealthCheckConfiguration', {
            value: JSON.stringify({
                backend: {
                    path: '/actuator/health',
                    port: 8080,
                    interval: 30,
                    timeout: 5,
                    healthyThreshold: 2,
                    unhealthyThreshold: 2,
                },
                frontend: {
                    path: '/health',
                    port: 3000,
                    interval: 30,
                    timeout: 5,
                    healthyThreshold: 2,
                    unhealthyThreshold: 2,
                },
            }),
            description: 'ALB Health Check Configuration',
        });
    }
}
