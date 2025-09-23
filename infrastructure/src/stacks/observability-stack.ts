import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface ObservabilityStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    kmsKey: kms.Key;
}

export class ObservabilityStack extends cdk.Stack {
    public readonly logGroup: logs.LogGroup;
    public readonly dashboard: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
        super(scope, id, props);

        // Create CloudWatch Log Group
        this.logGroup = new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/genai-demo/application`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create CloudWatch Dashboard
        this.dashboard = new cloudwatch.Dashboard(this, 'ApplicationDashboard', {
            dashboardName: `GenAI-Demo-${this.stackName}`,
        });

        // Add basic widgets to dashboard
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# GenAI Demo Monitoring Dashboard\n\nEnvironment: ${this.stackName}`,
                width: 24,
                height: 2,
            })
        );

        // Outputs
        new cdk.CfnOutput(this, 'LogGroupName', {
            value: this.logGroup.logGroupName,
            exportName: `${this.stackName}-LogGroupName`,
        });

        new cdk.CfnOutput(this, 'DashboardURL', {
            value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.dashboard.dashboardName}`,
            exportName: `${this.stackName}-DashboardURL`,
        });
    }
}