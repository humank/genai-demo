import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { ConfigInsightsStack } from '../src/stacks/config-insights-stack';

describe('ConfigInsightsStack', () => {
  let app: cdk.App;
  let stack: ConfigInsightsStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new ConfigInsightsStack(app, 'TestConfigInsightsStack', {
      alertEmail: 'test@example.com',
      env: {
        account: '123456789012',
        region: 'ap-northeast-1',
      },
    });
    template = Template.fromStack(stack);
  });

  test('should create SNS topic for configuration alerts', () => {
    template.hasResourceProperties('AWS::SNS::Topic', {
      DisplayName: 'GenAIDemo Config Alerts',
      TopicName: 'genai-demo-config-alerts',
    });
  });

  test('should create email subscription for alerts', () => {
    template.hasResourceProperties('AWS::SNS::Subscription', {
      Protocol: 'email',
      Endpoint: 'test@example.com',
    });
  });

  test('should create S3 bucket for configuration history', () => {
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

  test('should create configuration recorder', () => {
    template.hasResourceProperties('AWS::Config::ConfigurationRecorder', {
      Name: 'genai-demo-config-recorder',
      RecordingGroup: {
        AllSupported: true,
        IncludeGlobalResourceTypes: true,
      },
    });
  });

  test('should create EBS encryption rule', () => {
    template.hasResourceProperties('AWS::Config::ConfigRule', {
      ConfigRuleName: 'ebs-encryption-enabled',
    });
  });

  test('should create S3 encryption rule', () => {
    template.hasResourceProperties('AWS::Config::ConfigRule', {
      ConfigRuleName: 's3-bucket-encryption-enabled',
    });
  });

  test('should create RDS encryption rule', () => {
    template.hasResourceProperties('AWS::Config::ConfigRule', {
      ConfigRuleName: 'rds-storage-encrypted',
    });
  });

  test('should create Lambda function for drift monitoring', () => {
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'genai-demo-config-drift-monitor',
      Runtime: 'python3.11',
      Handler: 'index.handler',
    });
  });

  test('should create CloudWatch dashboard for Config insights', () => {
    template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
      DashboardName: 'GenAIDemo-Config-Insights',
    });
  });
});
