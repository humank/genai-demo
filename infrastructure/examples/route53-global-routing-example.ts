import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import { Construct } from 'constructs';
import { Route53GlobalRoutingStack, Route53GlobalRoutingStackProps } from '../src/stacks/route53-global-routing-stack';

/**
 * Example usage of Route53GlobalRoutingStack for Active-Active multi-region setup
 * 
 * This example demonstrates how to configure global DNS routing with:
 * - Geolocation-based routing for regional optimization
 * - Weighted routing for A/B testing capabilities
 * - Latency-based routing for optimal performance
 * - Real-time health checks with 30-second intervals
 * - Comprehensive monitoring and alerting
 */
export class Route53GlobalRoutingExample extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        // Example configuration - these would typically come from existing stacks
        const projectName = 'genai-demo';
        const environment = 'production';
        const domain = 'api.genai-demo.com';

        // Mock hosted zone (in real usage, this would be imported from CertificateStack)
        const hostedZone = route53.HostedZone.fromLookup(this, 'HostedZone', {
            domainName: 'genai-demo.com'
        });

        // Mock certificate (in real usage, this would be imported from CertificateStack)
        const certificate = certificatemanager.Certificate.fromCertificateArn(
            this,
            'Certificate',
            'arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012'
        );

        // Mock load balancers (in real usage, these would be imported from CoreInfrastructureStack)
        const primaryLoadBalancer = elbv2.ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
            this,
            'PrimaryALB',
            {
                loadBalancerArn: 'arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/primary-alb/1234567890123456',
                loadBalancerDnsName: 'primary-alb-1234567890.us-east-1.elb.amazonaws.com',
                loadBalancerCanonicalHostedZoneId: 'Z35SXDOTRQ7X7K'
            }
        );

        const secondaryLoadBalancer = elbv2.ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
            this,
            'SecondaryALB',
            {
                loadBalancerArn: 'arn:aws:elasticloadbalancing:eu-west-1:123456789012:loadbalancer/app/secondary-alb/1234567890123456',
                loadBalancerDnsName: 'secondary-alb-1234567890.eu-west-1.elb.amazonaws.com',
                loadBalancerCanonicalHostedZoneId: 'Z32O12XQLNTSW2'
            }
        );

        const tertiaryLoadBalancer = elbv2.ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
            this,
            'TertiaryALB',
            {
                loadBalancerArn: 'arn:aws:elasticloadbalancing:ap-southeast-1:123456789012:loadbalancer/app/tertiary-alb/1234567890123456',
                loadBalancerDnsName: 'tertiary-alb-1234567890.ap-southeast-1.elb.amazonaws.com',
                loadBalancerCanonicalHostedZoneId: 'Z1LMS91P8CMLE5'
            }
        );

        // Create the Route53 Global Routing Stack
        const globalRoutingStack = new Route53GlobalRoutingStack(this, 'GlobalRouting', {
            environment,
            projectName,
            domain,
            hostedZone,
            certificate,
            regions: {
                primary: {
                    region: 'us-east-1',
                    loadBalancer: primaryLoadBalancer,
                    weight: 70 // 70% of traffic for A/B testing
                },
                secondary: {
                    region: 'eu-west-1',
                    loadBalancer: secondaryLoadBalancer,
                    weight: 20 // 20% of traffic for A/B testing
                },
                tertiary: {
                    region: 'ap-southeast-1',
                    loadBalancer: tertiaryLoadBalancer,
                    weight: 10 // 10% of traffic for A/B testing
                }
            },
            monitoringConfig: {
                healthCheckInterval: 30, // 30-second health check interval
                failureThreshold: 3, // 3 failures trigger failover
                enableABTesting: true, // Enable weighted routing for A/B testing
                enableGeolocationRouting: true // Enable geolocation-based routing
            }
        });

        // Output the DNS endpoints for different routing types
        new cdk.CfnOutput(this, 'GeolocationEndpoint', {
            value: `https://api-geo.${domain}`,
            description: 'Geolocation-based routing endpoint (routes based on user location)'
        });

        new cdk.CfnOutput(this, 'WeightedEndpoint', {
            value: `https://api-weighted.${domain}`,
            description: 'Weighted routing endpoint for A/B testing (70/20/10 split)'
        });

        new cdk.CfnOutput(this, 'LatencyEndpoint', {
            value: `https://api-latency.${domain}`,
            description: 'Latency-based routing endpoint (routes to lowest latency region)'
        });

        new cdk.CfnOutput(this, 'MonitoringDashboard', {
            value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${projectName}-${environment}-global-routing-monitoring`,
            description: 'CloudWatch dashboard for global routing monitoring'
        });
    }
}

/**
 * Usage Notes:
 * 
 * 1. **Integration with existing stacks:**
 *    - Import hostedZone from CertificateStack
 *    - Import certificate from CertificateStack  
 *    - Import load balancers from CoreInfrastructureStack
 * 
 * 2. **DNS Routing Types:**
 *    - api-geo.domain.com: Routes based on user's geographic location
 *    - api-weighted.domain.com: Routes based on configured weights (A/B testing)
 *    - api-latency.domain.com: Routes to the region with lowest latency
 * 
 * 3. **Health Checks:**
 *    - 30-second intervals (configurable)
 *    - HTTPS health checks on /actuator/health endpoint
 *    - 3 failure threshold before triggering failover
 *    - Latency measurement enabled for performance monitoring
 * 
 * 4. **Monitoring:**
 *    - CloudWatch dashboard with health check status
 *    - DNS query metrics by routing type
 *    - Latency comparison across regions
 *    - Traffic distribution visualization
 * 
 * 5. **Alerting:**
 *    - SNS topic for health check failures
 *    - High latency alerts (>2 seconds)
 *    - Global system health composite alarm
 *    - DNS query rate monitoring (DDoS detection)
 * 
 * 6. **A/B Testing Configuration:**
 *    - Primary region: 70% traffic
 *    - Secondary region: 20% traffic  
 *    - Tertiary region: 10% traffic
 *    - Weights can be adjusted for different test scenarios
 */