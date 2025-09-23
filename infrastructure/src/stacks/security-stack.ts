import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import { Construct } from 'constructs';

export interface SecurityStackProps extends cdk.StackProps {
    // Add any specific props here
}

export class SecurityStack extends cdk.Stack {
    public readonly kmsKey: kms.Key;
    public readonly applicationRole: iam.Role;

    constructor(scope: Construct, id: string, props?: SecurityStackProps) {
        super(scope, id, props);

        // Create KMS Key for encryption
        this.kmsKey = new kms.Key(this, 'ApplicationKey', {
            description: 'KMS key for GenAI Demo application encryption',
            enableKeyRotation: true,
            removalPolicy: cdk.RemovalPolicy.DESTROY, // For demo purposes
        });

        // Create application role
        this.applicationRole = new iam.Role(this, 'ApplicationRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            description: 'IAM role for GenAI Demo application',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
            ],
        });

        // Grant the application role access to the KMS key
        this.kmsKey.grantEncryptDecrypt(this.applicationRole);

        // Outputs
        new cdk.CfnOutput(this, 'KMSKeyId', {
            value: this.kmsKey.keyId,
            exportName: `${this.stackName}-KMSKeyId`,
        });

        new cdk.CfnOutput(this, 'ApplicationRoleArn', {
            value: this.applicationRole.roleArn,
            exportName: `${this.stackName}-ApplicationRoleArn`,
        });
    }
}