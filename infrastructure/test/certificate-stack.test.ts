import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { CertificateStack } from '../src/stacks/certificate-stack';

describe('Certificate Stack', () => {
    describe('Without Domain', () => {
        let app: cdk.App;
        let stack: CertificateStack;
        let template: Template;

        beforeEach(() => {
            app = new cdk.App();
            stack = new CertificateStack(app, 'TestCertificateStack', {
                environment: 'test',
                projectName: 'genai-demo-test',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });
            template = Template.fromStack(stack);
        });

        test('No certificates are created when domain is not provided', () => {
            template.resourceCountIs('AWS::CertificateManager::Certificate', 0);
            template.resourceCountIs('AWS::SNS::Topic', 0);
            template.resourceCountIs('AWS::CloudWatch::Alarm', 0);
        });

        test('No outputs are created when domain is not provided', () => {
            expect(Object.keys(template.toJSON().Outputs || {})).toHaveLength(0);
        });
    });

    describe('With Domain', () => {
        let app: cdk.App;
        let stack: CertificateStack;
        let template: Template;

        beforeEach(() => {
            app = new cdk.App({
                context: {
                    'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                        'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                        'Name': 'kimkao.io.'
                    }
                }
            });
            stack = new CertificateStack(app, 'TestCertificateStackWithDomain', {
                environment: 'test',
                projectName: 'genai-demo-test',
                domain: 'test.kimkao.io',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });
            template = Template.fromStack(stack);
        });

        test('ACM certificates are created with DNS validation', () => {
            // Main certificate
            template.hasResourceProperties('AWS::CertificateManager::Certificate', {
                DomainName: 'test.kimkao.io',
                ValidationMethod: 'DNS',
                SubjectAlternativeNames: [
                    'api.test.kimkao.io',
                    'cmc.test.kimkao.io',
                    'shop.test.kimkao.io',
                    'grafana.test.kimkao.io',
                    'logs.test.kimkao.io'
                ]
            });

            // Wildcard certificate
            template.hasResourceProperties('AWS::CertificateManager::Certificate', {
                DomainName: '*.kimkao.io',
                ValidationMethod: 'DNS',
                SubjectAlternativeNames: ['kimkao.io']
            });
        });

        test('Certificate validation monitoring is configured', () => {
            // SNS topic for certificate alerts
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-test-test-certificate-alerts',
                DisplayName: 'Certificate Alerts for genai-demo-test test'
            });

            // CloudWatch alarms for certificate expiration
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-test-certificate-validation-status',
                AlarmDescription: 'Monitor certificate validation status',
                MetricName: 'DaysToExpiry',
                Namespace: 'AWS/CertificateManager',
                Threshold: 30,
                ComparisonOperator: 'LessThanThreshold'
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-test-wildcard-certificate-validation-status',
                AlarmDescription: 'Monitor wildcard certificate validation status',
                MetricName: 'DaysToExpiry',
                Namespace: 'AWS/CertificateManager',
                Threshold: 30,
                ComparisonOperator: 'LessThanThreshold'
            });
        });

        test('Certificate ARNs are exported for Kubernetes Ingress use', () => {
            template.hasOutput('CertificateArn', {
                Description: 'ACM Certificate ARN for domain - use in Kubernetes Ingress annotations',
                Export: {
                    Name: 'genai-demo-test-test-certificate-arn'
                }
            });

            template.hasOutput('WildcardCertificateArn', {
                Description: 'ACM Wildcard Certificate ARN for domain - use in Kubernetes Ingress annotations',
                Export: {
                    Name: 'genai-demo-test-test-wildcard-certificate-arn'
                }
            });
        });

        test('Hosted Zone ID is exported for cross-stack references', () => {
            template.hasOutput('HostedZoneId', {
                Description: 'Route 53 Hosted Zone ID for cross-stack DNS management',
                Export: {
                    Name: 'genai-demo-test-test-hosted-zone-id'
                }
            });

            template.hasOutput('HostedZoneName', {
                Description: 'Route 53 Hosted Zone Name for cross-stack DNS management',
                Export: {
                    Name: 'genai-demo-test-test-hosted-zone-name'
                }
            });
        });

        test('Certificate validation status monitoring outputs are created', () => {
            template.hasOutput('CertMonitoringAlertsTopicArnOutput', {
                Description: 'SNS topic ARN for certificate alerts',
                Export: {
                    Name: 'genai-demo-test-test-certificate-alerts-topic'
                }
            });

            template.hasOutput('CertMonitoringValidationAlarmOutput', {
                Description: 'CloudWatch alarm name for certificate validation monitoring',
                Export: {
                    Name: 'genai-demo-test-test-certificate-validation-alarm'
                }
            });

            template.hasOutput('WildcardCertMonitoringValidationAlarmOutput', {
                Description: 'CloudWatch alarm name for wildcard certificate validation monitoring',
                Export: {
                    Name: 'genai-demo-test-test-wildcard-certificate-validation-alarm'
                }
            });
        });

        test('Common tags are applied to certificate resources', () => {
            // Check that SNS topic has the required tags (among others)
            const snsTopics = template.findResources('AWS::SNS::Topic');
            expect(Object.keys(snsTopics)).toHaveLength(1);

            const topic = Object.values(snsTopics)[0] as any;
            const tags = topic.Properties.Tags;
            const tagMap = tags.reduce((acc: any, tag: any) => {
                acc[tag.Key] = tag.Value;
                return acc;
            }, {});

            expect(tagMap.Environment).toBe('test');
            expect(tagMap.Project).toBe('genai-demo-test');
            expect(tagMap.Service).toBe('Certificate-Monitoring');
        });
    });
});