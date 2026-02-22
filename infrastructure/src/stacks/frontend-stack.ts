import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export interface FrontendStackProps extends cdk.StackProps {
    environment: string;
    projectName: string;
    region: string;
}

/**
 * 前端基礎設施 Stack
 *
 * 建立 ECR 儲存庫供 Consumer 與 CMC 前端 Docker 映像使用，
 * 並設定映像生命週期策略與跨帳號存取權限。
 */
export class FrontendStack extends cdk.Stack {
    public readonly consumerRepo: ecr.Repository;
    public readonly cmcRepo: ecr.Repository;

    constructor(scope: Construct, id: string, props: FrontendStackProps) {
        super(scope, id, props);

        const { environment, projectName } = props;

        // Consumer Frontend ECR Repository
        this.consumerRepo = new ecr.Repository(this, 'ConsumerFrontendRepo', {
            repositoryName: `${projectName}/consumer-frontend`,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY,
            emptyOnDelete: environment !== 'production',
            imageScanOnPush: true,
            imageTagMutability: ecr.TagMutability.MUTABLE,
            lifecycleRules: [
                {
                    description: '保留最近 20 個映像',
                    maxImageCount: 20,
                    rulePriority: 1,
                    tagStatus: ecr.TagStatus.ANY,
                },
            ],
        });

        // CMC Frontend ECR Repository
        this.cmcRepo = new ecr.Repository(this, 'CmcFrontendRepo', {
            repositoryName: `${projectName}/cmc-frontend`,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY,
            emptyOnDelete: environment !== 'production',
            imageScanOnPush: true,
            imageTagMutability: ecr.TagMutability.MUTABLE,
            lifecycleRules: [
                {
                    description: '保留最近 20 個映像',
                    maxImageCount: 20,
                    rulePriority: 1,
                    tagStatus: ecr.TagStatus.ANY,
                },
            ],
        });

        // 允許 EKS 節點角色拉取映像
        const eksPullPolicy = new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'ecr:GetDownloadUrlForLayer',
                'ecr:BatchGetImage',
                'ecr:BatchCheckLayerAvailability',
            ],
            principals: [new iam.ServicePrincipal('eks.amazonaws.com')],
        });

        this.consumerRepo.addToResourcePolicy(eksPullPolicy);
        this.cmcRepo.addToResourcePolicy(eksPullPolicy);

        // Tags
        cdk.Tags.of(this).add('Component', 'Frontend');
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);

        // Outputs
        new cdk.CfnOutput(this, 'ConsumerFrontendRepoUri', {
            value: this.consumerRepo.repositoryUri,
            exportName: `${this.stackName}-ConsumerRepoUri`,
            description: 'Consumer Frontend ECR Repository URI',
        });

        new cdk.CfnOutput(this, 'ConsumerFrontendRepoArn', {
            value: this.consumerRepo.repositoryArn,
            exportName: `${this.stackName}-ConsumerRepoArn`,
        });

        new cdk.CfnOutput(this, 'CmcFrontendRepoUri', {
            value: this.cmcRepo.repositoryUri,
            exportName: `${this.stackName}-CmcRepoUri`,
            description: 'CMC Frontend ECR Repository URI',
        });

        new cdk.CfnOutput(this, 'CmcFrontendRepoArn', {
            value: this.cmcRepo.repositoryArn,
            exportName: `${this.stackName}-CmcRepoArn`,
        });
    }
}
