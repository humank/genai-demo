#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { DeploymentMonitoringStack } from '../src/stacks/deployment-monitoring-stack';

/**
 * Example: Deployment Monitoring Stack Integration
 * 
 * This example demonstrates how to integrate the Deployment Monitoring Stack
 * with your existing infrastructure.
 */

const app = new cdk.App();

// Example 1: Basic deployment monitoring
const basicMonitoring = new DeploymentMonitoringStack(app, 'BasicDeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'development',
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: process.env.CDK_DEFAULT_REGION || 'ap-east-2',
    },
});

// Example 2: Production deployment monitoring with multi-region
const productionMonitoring = new DeploymentMonitoringStack(app, 'ProductionDeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2', 'eu-west-1'],
        primaryRegion: 'us-east-1',
    },
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: 'us-east-1',
    },
});

// Example 3: Integration with existing alerting infrastructure
// Note: This assumes you have an existing AlertingStack deployed
/*
import { AlertingStack } from '../src/stacks/alerting-stack';

const alerting = new AlertingStack(app, 'Alerting', {
    environment: 'production',
    applicationName: 'genai-demo',
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: 'us-east-1',
    },
});

const integratedMonitoring = new DeploymentMonitoringStack(app, 'IntegratedDeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: alerting.alertTopic, // Reuse existing alert topic
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: 'us-east-1',
    },
});
*/

// Example 4: Custom configuration for staging environment
const stagingMonitoring = new DeploymentMonitoringStack(app, 'StagingDeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'staging',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2'],
        primaryRegion: 'us-east-1',
    },
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: 'us-east-1',
    },
});

// Add tags to all stacks
cdk.Tags.of(app).add('Project', 'GenAI-Demo');
cdk.Tags.of(app).add('ManagedBy', 'CDK');
cdk.Tags.of(app).add('Component', 'DeploymentMonitoring');

app.synth();

/**
 * Deployment Instructions:
 * 
 * 1. Deploy basic monitoring:
 *    cdk deploy BasicDeploymentMonitoring
 * 
 * 2. Deploy production monitoring:
 *    cdk deploy ProductionDeploymentMonitoring
 * 
 * 3. Deploy staging monitoring:
 *    cdk deploy StagingDeploymentMonitoring
 * 
 * 4. Subscribe to alerts:
 *    aws sns subscribe \
 *      --topic-arn $(aws cloudformation describe-stacks \
 *        --stack-name ProductionDeploymentMonitoring \
 *        --query 'Stacks[0].Outputs[?OutputKey==`DeploymentAlertTopicArn`].OutputValue' \
 *        --output text) \
 *      --protocol email \
 *      --notification-endpoint your-email@example.com
 * 
 * 5. Access dashboard:
 *    aws cloudformation describe-stacks \
 *      --stack-name ProductionDeploymentMonitoring \
 *      --query 'Stacks[0].Outputs[?OutputKey==`DeploymentDashboardUrl`].OutputValue' \
 *      --output text
 */
