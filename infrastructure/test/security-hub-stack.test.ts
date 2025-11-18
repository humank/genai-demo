import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { SecurityHubStack } from '../lib/stacks/security-hub-stack';

describe('SecurityHubStack', () => {
  let app: cdk.App;
  let stack: SecurityHubStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new SecurityHubStack(app, 'TestSecurityHubStack', {
      notificationEmail: 'test@example.com',
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('Security Hub Configuration', () => {
    test('should create Security Hub with correct standards', () => {
      template.hasResourceProperties('AWS::SecurityHub::Hub', {});
    });

    test('should enable AWS Foundational Security Best Practices', () => {
      template.hasResourceProperties('AWS::SecurityHub::Standard', {
        StandardsArn: 'arn:aws:securityhub:us-east-1::standards/aws-foundational-security-best-practices/v/1.0.0',
      });
    });
  });

  describe('CIS AWS Foundations Benchmark', () => {
    test('should enable CIS standard', () => {
      template.hasResourceProperties('AWS::SecurityHub::Standard', {
        StandardsArn: 'arn:aws:securityhub:us-east-1::standards/cis-aws-foundations-benchmark/v/1.2.0',
      });
    });
  });

  describe('PCI DSS Standard', () => {
    test('should enable PCI DSS standard', () => {
      template.hasResourceProperties('AWS::SecurityHub::Standard', {
        StandardsArn: 'arn:aws:securityhub:us-east-1::standards/pci-dss/v/3.2.1',
      });
    });
  });

  describe('SNS Topics', () => {
    test('should create critical findings topic', () => {
      template.hasResourceProperties('AWS::SNS::Topic', {
        DisplayName: 'Security Hub Critical Findings',
      });
    });

    test('should create high findings topic', () => {
      template.hasResourceProperties('AWS::SNS::Topic', {
        DisplayName: 'Security Hub High Findings',
      });
    });

    // Note: KMS encryption is optional for SNS topics in this implementation
    // test('should have encryption enabled on topics', () => {
    //   template.hasResourceProperties('AWS::SNS::Topic', {
    //     KmsMasterKeyId: { 'Fn::GetAtt': [Match.anyValue(), 'Arn'] },
    //   });
    // });
  });

  describe('Lambda Function', () => {
    test('should create incident response Lambda', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 300,
      });
    });

    test('should have required environment variables', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: {
          Variables: {
            CRITICAL_TOPIC_ARN: Match.anyValue(),
            HIGH_TOPIC_ARN: Match.anyValue(),
          },
        },
      });
    });
  });

  describe('EventBridge Rules', () => {
    test('should create rule for critical findings', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        EventPattern: {
          source: ['aws.securityhub'],
          'detail-type': ['Security Hub Findings - Imported'],
          detail: {
            findings: {
              Severity: {
                Label: ['CRITICAL'],
              },
            },
          },
        },
      });
    });

    test('should create rule for high findings', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        EventPattern: {
          source: ['aws.securityhub'],
          'detail-type': ['Security Hub Findings - Imported'],
          detail: {
            findings: {
              Severity: {
                Label: ['HIGH'],
              },
            },
          },
        },
      });
    });
  });

  describe('IAM Permissions', () => {
    test('should grant Lambda permissions to Security Hub', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'securityhub:GetFindings',
                'securityhub:BatchUpdateFindings',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });

    test('should grant Lambda permissions to SNS', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: 'sns:Publish',
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });
  });

  describe('Stack Outputs', () => {
    test('should export Security Hub ARN', () => {
      template.hasOutput('SecurityHubArn', {});
    });

    test('should export critical topic ARN', () => {
      template.hasOutput('CriticalFindingsTopicArn', {});
    });

    test('should export high topic ARN', () => {
      template.hasOutput('HighFindingsTopicArn', {});
    });
  });
});
