import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as sns from 'aws-cdk-lib/aws-sns';
import { MSKStack } from '../lib/stacks/msk-stack';

/**
 * 測試 MSK Stack 中的可觀測性 Topics 配置
 */
describe('MSK Observability Topics Configuration', () => {
    let app: cdk.App;
    let stack: MSKStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        
        // 創建必要的依賴資源
        const vpc = ec2.Vpc.fromLookup(app, 'TestVpc', { isDefault: true });
        const securityGroup = new ec2.SecurityGroup(app, 'TestSecurityGroup', { vpc });
        const kmsKey = new kms.Key(app, 'TestKmsKey');
        const alertingTopic = new sns.Topic(app, 'TestAlertingTopic');

        // 創建 MSK Stack
        stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup: securityGroup,
            kmsKey,
            alertingTopic
        });

        template = Template.fromStack(stack);
    });

    test('should include observability topics in domain events topics output', () => {
        // 檢查是否包含可觀測性 topics 的輸出
        template.hasOutput('DomainEventsTopics', {
            Value: {
                'Fn::Join': [
                    ',',
                    [
                        'genai-demo.test.customer.created',
                        'genai-demo.test.customer.updated',
                        'genai-demo.test.order.created',
                        'genai-demo.test.order.confirmed',
                        'genai-demo.test.order.cancelled',
                        'genai-demo.test.payment.processed',
                        'genai-demo.test.payment.failed',
                        'genai-demo.test.inventory.reserved',
                        'genai-demo.test.inventory.released',
                        'genai-demo.test.product.created',
                        'genai-demo.test.product.updated',
                        'genai-demo.test.observability.user.behavior',
                        'genai-demo.test.observability.performance.metrics',
                        'genai-demo.test.observability.business.analytics'
                    ]
                ]
            }
        });
    });

    test('should include observability DLQ topics output', () => {
        // 檢查是否包含 DLQ topics 的輸出
        template.hasOutput('ObservabilityDLQTopics', {
            Value: {
                'Fn::Join': [
                    ',',
                    [
                        'genai-demo.test.observability.user.behavior.dlq',
                        'genai-demo.test.observability.performance.metrics.dlq',
                        'genai-demo.test.observability.business.analytics.dlq'
                    ]
                ]
            }
        });
    });

    test('should include observability topics configuration output', () => {
        // 檢查是否包含可觀測性 topics 配置的輸出
        template.hasOutput('ObservabilityTopicsConfig', {
            Value: {
                'Fn::Sub': JSON.stringify({
                    'user-behavior': 'genai-demo.test.observability.user.behavior',
                    'performance-metrics': 'genai-demo.test.observability.performance.metrics',
                    'business-analytics': 'genai-demo.test.observability.business.analytics',
                    'user-behavior-dlq': 'genai-demo.test.observability.user.behavior.dlq',
                    'performance-metrics-dlq': 'genai-demo.test.observability.performance.metrics.dlq',
                    'business-analytics-dlq': 'genai-demo.test.observability.business.analytics.dlq'
                })
            }
        });
    });

    test('should include Spring Boot observability configuration output', () => {
        // 檢查是否包含 Spring Boot 可觀測性配置的輸出
        template.hasOutput('SpringBootObservabilityConfig', {
            Value: {
                'Fn::Sub': JSON.stringify({
                    'genai-demo.domain-events.topic.prefix': 'genai-demo.test',
                    'genai-demo.domain-events.topic.observability.user-behavior': 'genai-demo.test.observability.user.behavior',
                    'genai-demo.domain-events.topic.observability.performance-metrics': 'genai-demo.test.observability.performance.metrics',
                    'genai-demo.domain-events.topic.observability.business-analytics': 'genai-demo.test.observability.business.analytics',
                    'genai-demo.domain-events.publishing.dlq.enabled': 'true',
                    'genai-demo.domain-events.publishing.dlq.topic-suffix': '.dlq',
                    'genai-demo.events.publisher': 'kafka',
                    'genai-demo.events.async': 'true'
                })
            }
        });
    });

    test('should create MSK cluster with proper configuration', () => {
        // 檢查 MSK 叢集是否被創建
        template.hasResourceProperties('AWS::MSK::Cluster', {
            ClusterName: 'genai-demo-test-msk',
            KafkaVersion: '2.8.1'
        });
    });

    test('should create MSK configuration with proper settings', () => {
        // 檢查 MSK 配置是否被創建
        template.hasResourceProperties('AWS::MSK::Configuration', {
            Name: 'genai-demo-test-msk-config',
            Description: 'MSK configuration for genai-demo test environment - optimized for domain events'
        });
    });
});