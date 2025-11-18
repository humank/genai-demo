import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { CostUsageReportsStack } from '../lib/stacks/cost-usage-reports-stack';

describe('CostUsageReportsStack', () => {
  let app: cdk.App;
  let stack: CostUsageReportsStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new CostUsageReportsStack(app, 'TestCostUsageReportsStack', {
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('S3 Buckets', () => {
    test('should create CUR bucket with correct configuration', () => {
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

    test('should create Athena results bucket', () => {
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

    test('should have lifecycle rules on CUR bucket', () => {
      template.hasResourceProperties('AWS::S3::Bucket', {
        LifecycleConfiguration: {
          Rules: Match.arrayWith([
            Match.objectLike({
              Status: 'Enabled',
            }),
          ]),
        },
      });
    });
  });

  describe('Cost and Usage Report', () => {
    test('should create CUR with hourly granularity', () => {
      template.hasResourceProperties('AWS::CUR::ReportDefinition', {
        ReportName: 'genai-demo-hourly-cost-report',
        TimeUnit: 'HOURLY',
        Format: 'Parquet',
        Compression: 'Parquet',
      });
    });

    test('should include resource details', () => {
      template.hasResourceProperties('AWS::CUR::ReportDefinition', {
        AdditionalSchemaElements: ['RESOURCES'],
      });
    });

    test('should enable Athena integration', () => {
      template.hasResourceProperties('AWS::CUR::ReportDefinition', {
        AdditionalArtifacts: ['ATHENA'],
      });
    });
  });

  describe('Glue Data Catalog', () => {
    test('should create Glue database', () => {
      template.hasResourceProperties('AWS::Glue::Database', {
        DatabaseInput: {
          Name: 'cost_usage_reports',
          Description: Match.stringLikeRegexp('.*cost analysis.*'),
        },
      });
    });

    test('should create Glue crawler', () => {
      template.hasResourceProperties('AWS::Glue::Crawler', {
        Name: 'cost-usage-reports-crawler',
        Schedule: {
          ScheduleExpression: 'cron(0 2 * * ? *)',
        },
      });
    });

    test('should configure crawler schema change policy', () => {
      template.hasResourceProperties('AWS::Glue::Crawler', {
        SchemaChangePolicy: {
          UpdateBehavior: 'UPDATE_IN_DATABASE',
          DeleteBehavior: 'LOG',
        },
      });
    });
  });

  describe('Athena Workgroup', () => {
    test('should create Athena workgroup', () => {
      template.hasResourceProperties('AWS::Athena::WorkGroup', {
        Name: 'cost-analysis-workgroup',
        Description: Match.stringLikeRegexp('.*cost.*analysis.*'),
      });
    });

    test('should configure result location', () => {
      template.hasResourceProperties('AWS::Athena::WorkGroup', {
        WorkGroupConfiguration: {
          ResultConfiguration: {
            EncryptionConfiguration: {
              EncryptionOption: 'SSE_S3',
            },
          },
        },
      });
    });

    test('should enable CloudWatch metrics', () => {
      template.hasResourceProperties('AWS::Athena::WorkGroup', {
        WorkGroupConfiguration: {
          PublishCloudWatchMetricsEnabled: true,
        },
      });
    });
  });

  describe('Lambda Function', () => {
    test('should create cost anomaly detection Lambda', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'cost-anomaly-detection',
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 300,
        MemorySize: 512,
      });
    });

    test('should have required environment variables', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: {
          Variables: {
            ATHENA_WORKGROUP: Match.anyValue(),
            GLUE_DATABASE: Match.anyValue(),
            SNS_TOPIC_ARN: Match.anyValue(),
            ATHENA_OUTPUT_LOCATION: Match.anyValue(),
          },
        },
      });
    });
  });

  describe('SNS Topic', () => {
    test('should create cost alert topic', () => {
      template.hasResourceProperties('AWS::SNS::Topic', {
        TopicName: 'cost-anomaly-alerts',
        DisplayName: 'Cost Anomaly Detection Alerts',
      });
    });
  });

  describe('EventBridge Rule', () => {
    test('should create daily analysis rule', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        Name: 'daily-cost-anomaly-detection',
        ScheduleExpression: 'cron(0 3 * * ? *)',
      });
    });
  });

  describe('CloudWatch Dashboard', () => {
    test('should create cost insights dashboard', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'cost-usage-reports-insights',
      });
    });
  });

  describe('CloudWatch Alarms', () => {
    test('should create cost anomaly alarm', () => {
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'cost-anomaly-detected',
        Threshold: 1,
        ComparisonOperator: 'GreaterThanOrEqualToThreshold',
      });
    });

    test('should create budget overspend risk alarm', () => {
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'budget-overspend-risk-high',
        Threshold: 1,
      });
    });
  });

  describe('IAM Permissions', () => {
    test('should grant Lambda Athena permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'athena:StartQueryExecution',
                'athena:GetQueryExecution',
                'athena:GetQueryResults',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });

    test('should grant Lambda Glue permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'glue:GetDatabase',
                'glue:GetTable',
                'glue:GetPartitions',
              ]),
            }),
          ]),
        },
      });
    });
  });

  describe('Stack Outputs', () => {
    test('should export CUR bucket name', () => {
      template.hasOutput('CURBucketName', {});
    });

    test('should export Glue database name', () => {
      template.hasOutput('GlueDatabaseName', {});
    });

    test('should export Athena workgroup name', () => {
      template.hasOutput('AthenaWorkgroupName', {});
    });

    test('should export cost alert topic ARN', () => {
      template.hasOutput('CostAlertTopicArn', {});
    });
  });
});
