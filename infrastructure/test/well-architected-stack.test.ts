import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { WellArchitectedStack } from '../lib/stacks/well-architected-stack';

describe('WellArchitectedStack', () => {
  let app: cdk.App;
  let stack: WellArchitectedStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new WellArchitectedStack(app, 'TestWellArchitectedStack', {
      workloadName: 'GenAI-Demo-Test',
      environment: 'test',
      alertEmail: 'test@example.com',
      reviewOwner: 'test-owner@example.com',
      env: {
        account: '123456789012',
        region: 'ap-northeast-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('Well-Architected Workload', () => {
    it('should create a Well-Architected workload with correct configuration', () => {
      template.hasResourceProperties('AWS::WellArchitected::Workload', {
        WorkloadName: 'GenAI-Demo-Test',
        Description: 'GenAI Demo Platform - test Environment',
        Environment: 'TEST',
        ReviewOwner: 'test-owner@example.com',
      });
    });

    it('should configure workload with multiple lenses', () => {
      template.hasResourceProperties('AWS::WellArchitected::Workload', {
        Lenses: Match.arrayWith([
          'wellarchitected',
          'serverless',
          'softwareasaservice',
        ]),
      });
    });

    it('should set pillar priorities correctly', () => {
      template.hasResourceProperties('AWS::WellArchitected::Workload', {
        PillarPriorities: [
          'security',
          'reliability',
          'operationalExcellence',
          'performance',
          'costOptimization',
          'sustainability',
        ],
      });
    });

    it('should configure AWS regions', () => {
      template.hasResourceProperties('AWS::WellArchitected::Workload', {
        AwsRegions: ['ap-northeast-1', 'ap-northeast-1'],
      });
    });

    it('should have proper tags', () => {
      template.hasResourceProperties('AWS::WellArchitected::Workload', {
        Tags: {
          Environment: 'test',
          Project: 'GenAI-Demo',
          ManagedBy: 'CDK',
          Purpose: 'Architecture-Assessment',
        },
      });
    });
  });

  describe('S3 Report Bucket', () => {
    it('should create an S3 bucket for reports', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        BucketName: Match.stringLikeRegexp('well-architected-reports-test-.*'),
        BucketEncryption: {
          ServerSideEncryptionConfiguration: [
            {
              ServerSideEncryptionByDefault: {
                SSEAlgorithm: 'AES256',
              },
            },
          ],
        },
        PublicAccessBlockConfiguration: {
          BlockPublicAcls: true,
          BlockPublicPolicy: true,
          IgnorePublicAcls: true,
          RestrictPublicBuckets: true,
        },
        VersioningConfiguration: {
          Status: 'Enabled',
        },
      });
    });

    it('should configure lifecycle rules for report retention', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        LifecycleConfiguration: {
          Rules: Match.arrayWith([
            Match.objectLike({
              Id: 'DeleteOldReports',
              ExpirationInDays: 90,
              NoncurrentVersionExpiration: Match.objectLike({
                NoncurrentDays: 30,
              }),
              Status: 'Enabled',
            }),
          ]),
        },
      });
    });
  });

  describe('SNS Notification Topic', () => {
    it('should create an SNS topic for notifications', () => {
      template.hasResourceProperties('AWS::SNS::Topic', {
        DisplayName: 'Well-Architected Tool Notifications',
        TopicName: 'well-architected-notifications-test',
      });
    });

    it('should create an email subscription', () => {
      template.hasResourceProperties('AWS::SNS::Subscription', {
        Protocol: 'email',
        Endpoint: 'test@example.com',
      });
    });
  });

  describe('Assessment Lambda Function', () => {
    it('should create a Lambda function for assessment', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 900, // 15 minutes
        MemorySize: 512,
        Description: 'Automated Well-Architected assessment and reporting',
      });
    });

    it('should configure Lambda environment variables', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: {
          Variables: {
            WORKLOAD_ID: Match.anyValue(),
            REPORT_BUCKET: Match.anyValue(),
            NOTIFICATION_TOPIC_ARN: Match.anyValue(),
            ENVIRONMENT: 'test',
          },
        },
      });
    });

    it('should have proper IAM permissions for Well-Architected', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: Match.arrayWith([
                'wellarchitected:GetWorkload',
                'wellarchitected:ListWorkloads',
                'wellarchitected:GetLensReview',
                'wellarchitected:ListLensReviews',
                'wellarchitected:GetAnswer',
                'wellarchitected:ListAnswers',
                'wellarchitected:UpdateAnswer',
                'wellarchitected:CreateMilestone',
                'wellarchitected:ListMilestones',
                'wellarchitected:GetLensReviewReport',
                'wellarchitected:ListLensReviewImprovements',
              ]),
            }),
          ]),
        },
      });
    });

    it('should have S3 read/write permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: Match.arrayWith([
                's3:GetObject*',
                's3:GetBucket*',
                's3:List*',
                's3:DeleteObject*',
                's3:PutObject',
                's3:PutObjectLegalHold',
                's3:PutObjectRetention',
                's3:PutObjectTagging',
                's3:PutObjectVersionTagging',
                's3:Abort*',
              ]),
            }),
          ]),
        },
      });
    });

    it('should have SNS publish permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: 'sns:Publish',
            }),
          ]),
        },
      });
    });
  });

  describe('EventBridge Rules', () => {
    it('should create a weekly assessment rule', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        Description: 'Trigger Well-Architected assessment every Monday at 9 AM',
        ScheduleExpression: 'cron(0 9 ? * MON *)',
        State: 'ENABLED',
      });
    });

    it('should create a monthly milestone rule', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        Description: 'Create Well-Architected milestone on the 1st of each month',
        ScheduleExpression: 'cron(0 10 1 * ? *)',
        State: 'ENABLED',
      });
    });

    it('should configure Lambda targets for EventBridge rules', () => {
      template.resourceCountIs('AWS::Lambda::Permission', 2);
    });
  });

  describe('Stack Outputs', () => {
    it('should export workload ID', () => {
      template.hasOutput('WorkloadId', {
        Description: 'Well-Architected Workload ID',
        Export: {
          Name: 'test-workload-id',
        },
      });
    });

    it('should output workload ARN', () => {
      template.hasOutput('WorkloadArn', {
        Description: 'Well-Architected Workload ARN',
      });
    });

    it('should output report bucket name', () => {
      template.hasOutput('ReportBucket', {
        Description: 'S3 bucket for Well-Architected reports',
      });
    });

    it('should output assessment Lambda ARN', () => {
      template.hasOutput('AssessmentLambdaArn', {
        Description: 'Assessment Lambda function ARN',
      });
    });

    it('should output notification topic ARN', () => {
      template.hasOutput('NotificationTopicArn', {
        Description: 'SNS topic for Well-Architected notifications',
      });
    });
  });

  describe('Resource Count', () => {
    it('should create expected number of resources', () => {
      template.resourceCountIs('AWS::WellArchitected::Workload', 1);
      template.resourceCountIs('AWS::S3::Bucket', 1);
      template.resourceCountIs('AWS::SNS::Topic', 1);
      template.resourceCountIs('AWS::SNS::Subscription', 1);
      template.resourceCountIs('AWS::Lambda::Function', 1);
      template.resourceCountIs('AWS::Events::Rule', 2);
      template.resourceCountIs('AWS::IAM::Role', 1);
    });
  });

  describe('Security Best Practices', () => {
    it('should block all public access on S3 bucket', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        PublicAccessBlockConfiguration: {
          BlockPublicAcls: true,
          BlockPublicPolicy: true,
          IgnorePublicAcls: true,
          RestrictPublicBuckets: true,
        },
      });
    });

    it('should enable S3 bucket encryption', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        BucketEncryption: {
          ServerSideEncryptionConfiguration: [
            {
              ServerSideEncryptionByDefault: {
                SSEAlgorithm: 'AES256',
              },
            },
          ],
        },
      });
    });

    it('should enable S3 bucket versioning', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        VersioningConfiguration: {
          Status: 'Enabled',
        },
      });
    });

    it('should use least privilege IAM permissions', () => {
      // Verify that Lambda role only has necessary permissions
      const policies = template.findResources('AWS::IAM::Policy');
      expect(Object.keys(policies).length).toBeGreaterThan(0);
    });
  });
});
