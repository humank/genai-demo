import * as cdk from 'aws-cdk-lib';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface KmsStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region: string;
}

/**
 * KMS Stack for managing encryption keys using AWS managed keys
 * 
 * This stack provides references to AWS managed KMS keys for:
 * - RDS/Aurora encryption
 * - Secrets Manager encryption
 * - Multi-region key management
 */
export class KmsStack extends cdk.Stack {
    public readonly rdsKey: kms.IKey;
    public readonly secretsManagerKey: kms.IKey;
    public readonly s3Key: kms.IKey;
    public readonly efsKey: kms.IKey;

    constructor(scope: Construct, id: string, props: KmsStackProps) {
        super(scope, id, props);

        const { environment, projectName, region } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Encryption',
            Service: 'KMS'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Reference AWS managed KMS keys
        this.rdsKey = this.getRdsKey();
        this.secretsManagerKey = this.getSecretsManagerKey();
        this.s3Key = this.getS3Key();
        this.efsKey = this.getEfsKey();

        // Store key information in Parameter Store for cross-stack references
        this.createParameterStoreParameters(projectName, environment, region);

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment);
    }

    /**
     * Get AWS managed RDS KMS key
     */
    private getRdsKey(): kms.IKey {
        return kms.Alias.fromAliasName(this, 'RdsKey', 'alias/aws/rds');
    }

    /**
     * Get AWS managed Secrets Manager KMS key
     */
    private getSecretsManagerKey(): kms.IKey {
        return kms.Alias.fromAliasName(this, 'SecretsManagerKey', 'alias/aws/secretsmanager');
    }

    /**
     * Get AWS managed S3 KMS key
     */
    private getS3Key(): kms.IKey {
        return kms.Alias.fromAliasName(this, 'S3Key', 'alias/aws/s3');
    }

    /**
     * Get AWS managed EFS KMS key
     */
    private getEfsKey(): kms.IKey {
        return kms.Alias.fromAliasName(this, 'EfsKey', 'alias/aws/elasticfilesystem');
    }

    /**
     * Store KMS key information in Parameter Store
     */
    private createParameterStoreParameters(projectName: string, environment: string, region: string): void {
        const parameterPrefix = `/genai-demo/${environment}/${region}/kms`;

        // RDS key ARN parameter
        new ssm.StringParameter(this, 'RdsKeyArnParameter', {
            parameterName: `${parameterPrefix}/rds-key-arn`,
            stringValue: this.rdsKey.keyArn,
            description: `AWS managed RDS KMS key ARN for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Secrets Manager key ARN parameter
        new ssm.StringParameter(this, 'SecretsManagerKeyArnParameter', {
            parameterName: `${parameterPrefix}/secrets-manager-key-arn`,
            stringValue: this.secretsManagerKey.keyArn,
            description: `AWS managed Secrets Manager KMS key ARN for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // S3 key ARN parameter
        new ssm.StringParameter(this, 'S3KeyArnParameter', {
            parameterName: `${parameterPrefix}/s3-key-arn`,
            stringValue: this.s3Key.keyArn,
            description: `AWS managed S3 KMS key ARN for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // EFS key ARN parameter
        new ssm.StringParameter(this, 'EfsKeyArnParameter', {
            parameterName: `${parameterPrefix}/efs-key-arn`,
            stringValue: this.efsKey.keyArn,
            description: `AWS managed EFS KMS key ARN for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Multi-region support information
        new ssm.StringParameter(this, 'MultiRegionSupportParameter', {
            parameterName: `${parameterPrefix}/multi-region-support`,
            stringValue: JSON.stringify({
                primaryRegion: region,
                supportedRegions: ['ap-east-2', 'ap-northeast-1'], // Hong Kong and Tokyo
                keyType: 'aws-managed',
                autoRotation: true,
                crossRegionReplication: true
            }),
            description: `Multi-region KMS configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });
    }

    /**
     * Create CloudFormation outputs for cross-stack references
     */
    private createOutputs(projectName: string, environment: string): void {
        // RDS key outputs
        new cdk.CfnOutput(this, 'RdsKeyArn', {
            value: this.rdsKey.keyArn,
            description: 'AWS managed RDS KMS key ARN',
            exportName: `${projectName}-${environment}-rds-key-arn`
        });

        new cdk.CfnOutput(this, 'RdsKeyId', {
            value: this.rdsKey.keyId,
            description: 'AWS managed RDS KMS key ID',
            exportName: `${projectName}-${environment}-rds-key-id`
        });

        // Secrets Manager key outputs
        new cdk.CfnOutput(this, 'SecretsManagerKeyArn', {
            value: this.secretsManagerKey.keyArn,
            description: 'AWS managed Secrets Manager KMS key ARN',
            exportName: `${projectName}-${environment}-secrets-manager-key-arn`
        });

        new cdk.CfnOutput(this, 'SecretsManagerKeyId', {
            value: this.secretsManagerKey.keyId,
            description: 'AWS managed Secrets Manager KMS key ID',
            exportName: `${projectName}-${environment}-secrets-manager-key-id`
        });

        // S3 key outputs
        new cdk.CfnOutput(this, 'S3KeyArn', {
            value: this.s3Key.keyArn,
            description: 'AWS managed S3 KMS key ARN',
            exportName: `${projectName}-${environment}-s3-key-arn`
        });

        // EFS key outputs
        new cdk.CfnOutput(this, 'EfsKeyArn', {
            value: this.efsKey.keyArn,
            description: 'AWS managed EFS KMS key ARN',
            exportName: `${projectName}-${environment}-efs-key-arn`
        });
    }
}