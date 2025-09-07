import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as rds from 'aws-cdk-lib/aws-rds';
import { Construct } from 'constructs';

export interface GenAIDemoInfrastructureStackProps extends cdk.StackProps {
  readonly environment: string;
  readonly projectName: string;
  readonly domain?: string;
}

export class GenAIDemoInfrastructureStack extends cdk.Stack {
  public readonly vpc: ec2.Vpc;
  public readonly eksCluster: eks.Cluster;
  public readonly rdsDatabase: rds.DatabaseInstance;
  public readonly mskCluster: msk.CfnCluster;
  public readonly loadBalancer: elbv2.ApplicationLoadBalancer;

  constructor(scope: Construct, id: string, props: GenAIDemoInfrastructureStackProps) {
    super(scope, id, props);

    // Get context values
    const environment = props.environment || this.node.tryGetContext('genai-demo:environment') || 'development';
    const projectName = props.projectName || this.node.tryGetContext('genai-demo:project-name') || 'genai-demo';
    const domain = props.domain || this.node.tryGetContext('genai-demo:domain');

    // Tags for all resources
    const commonTags = {
      Project: projectName,
      Environment: environment,
      ManagedBy: 'AWS-CDK',
      Component: 'Infrastructure'
    };

    // Apply tags to the stack
    Object.entries(commonTags).forEach(([key, value]) => {
      cdk.Tags.of(this).add(key, value);
    });

    // Core Infrastructure Components (to be implemented in subsequent tasks)
    this.vpc = this.createVPC(projectName, environment);

    // Placeholder for future infrastructure components
    // These will be implemented in subsequent tasks:
    // - this.eksCluster = this.createEKSCluster(this.vpc, projectName, environment);
    // - this.rdsDatabase = this.createRDSDatabase(this.vpc, projectName, environment);
    // - this.mskCluster = this.createMSKCluster(this.vpc, projectName, environment);
    // - this.loadBalancer = this.createApplicationLoadBalancer(this.vpc, projectName, environment);

    // Output important values
    new cdk.CfnOutput(this, 'VpcId', {
      value: this.vpc.vpcId,
      description: 'VPC ID for the GenAI Demo infrastructure',
      exportName: `${projectName}-${environment}-vpc-id`
    });

    new cdk.CfnOutput(this, 'Environment', {
      value: environment,
      description: 'Deployment environment'
    });

    new cdk.CfnOutput(this, 'ProjectName', {
      value: projectName,
      description: 'Project name'
    });
  }

  private createVPC(projectName: string, environment: string): ec2.Vpc {
    return new ec2.Vpc(this, 'GenAIDemoVPC', {
      vpcName: `${projectName}-${environment}-vpc`,
      maxAzs: 3,
      cidr: '10.0.0.0/16',
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
          cidrMask: 24,
          name: 'Database',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        }
      ],
      enableDnsHostnames: true,
      enableDnsSupport: true,
      natGateways: environment === 'production' ? 3 : 1
    });
  }

  // Placeholder methods for future implementation
  // These will be implemented in subsequent tasks

  /*
  private createEKSCluster(vpc: ec2.Vpc, projectName: string, environment: string): eks.Cluster {
    // To be implemented in task 6
  }

  private createRDSDatabase(vpc: ec2.Vpc, projectName: string, environment: string): rds.DatabaseInstance {
    // To be implemented in task 7
  }

  private createMSKCluster(vpc: ec2.Vpc, projectName: string, environment: string): msk.Cluster {
    // To be implemented in task 8
  }

  private createApplicationLoadBalancer(vpc: ec2.Vpc, projectName: string, environment: string): elbv2.ApplicationLoadBalancer {
    // To be implemented in task 5
  }
  */
}
