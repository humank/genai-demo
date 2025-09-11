#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import 'source-map-support/register';
import {
    CrossRegionObservabilityStack,
    MSKCrossRegionStack,
    MSKStack,
    MultiRegionStack,
    NetworkStack,
    RdsStack,
    SecurityStack
} from './lib/stacks';

const app = new cdk.App();

// Test configuration
const environment = 'production';
const projectName = 'genai-demo';
const primaryRegion = 'ap-east-2';
const secondaryRegion = 'ap-northeast-1';

console.log('Testing multi-region infrastructure components...');

// Test Primary Region - Core Components Only
console.log('Creating primary region core components...');

const primaryNetworkStack = new NetworkStack(app, `${projectName}-${environment}-primary-network-test`, {
    environment,
    projectName,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

const primarySecurityStack = new SecurityStack(app, `${projectName}-${environment}-primary-security-test`, {
    environment,
    projectName,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

const primaryRdsStack = new RdsStack(app, `${projectName}-${environment}-primary-rds-test`, {
    environment,
    projectName,
    vpc: primaryNetworkStack.vpc,
    rdsSecurityGroup: primaryNetworkStack.rdsSecurityGroup,
    region: primaryRegion,
    regionType: 'primary',
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

const primaryMskStack = new MSKStack(app, `${projectName}-${environment}-primary-msk-test`, {
    environment,
    projectName,
    vpc: primaryNetworkStack.vpc,
    mskSecurityGroup: primaryNetworkStack.mskSecurityGroup,
    kmsKey: primarySecurityStack.kmsKey,
    alertingTopic: primarySecurityStack.criticalAlertsTopic,
    region: primaryRegion,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

// Test Secondary Region - Core Components Only
console.log('Creating secondary region core components...');

const secondaryNetworkStack = new NetworkStack(app, `${projectName}-${environment}-secondary-network-test`, {
    environment: `${environment}-dr`,
    projectName,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: secondaryRegion
    }
});

const secondarySecurityStack = new SecurityStack(app, `${projectName}-${environment}-secondary-security-test`, {
    environment: `${environment}-dr`,
    projectName,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: secondaryRegion
    }
});

const secondaryRdsStack = new RdsStack(app, `${projectName}-${environment}-secondary-rds-test`, {
    environment: `${environment}-dr`,
    projectName,
    vpc: secondaryNetworkStack.vpc,
    rdsSecurityGroup: secondaryNetworkStack.rdsSecurityGroup,
    region: secondaryRegion,
    regionType: 'secondary',
    globalClusterIdentifier: primaryRdsStack.globalCluster?.ref,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: secondaryRegion
    }
});

const secondaryMskStack = new MSKStack(app, `${projectName}-${environment}-secondary-msk-test`, {
    environment: `${environment}-dr`,
    projectName,
    vpc: secondaryNetworkStack.vpc,
    mskSecurityGroup: secondaryNetworkStack.mskSecurityGroup,
    kmsKey: secondarySecurityStack.kmsKey,
    alertingTopic: secondarySecurityStack.criticalAlertsTopic,
    region: secondaryRegion,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: secondaryRegion
    }
});

// Test Cross-Region Components
console.log('Creating cross-region components...');

const primaryMskCrossRegionStack = new MSKCrossRegionStack(app, `${projectName}-${environment}-primary-msk-cross-region-test`, {
    environment,
    projectName,
    vpc: primaryNetworkStack.vpc,
    primaryMskCluster: primaryMskStack.mskCluster,
    secondaryMskCluster: secondaryMskStack.mskCluster,
    primaryRegion,
    secondaryRegion,
    regionType: 'primary',
    alertingTopic: primarySecurityStack.criticalAlertsTopic,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

const primaryCrossRegionObservabilityStack = new CrossRegionObservabilityStack(app, `${projectName}-${environment}-primary-cross-region-observability-test`, {
    environment,
    projectName,
    primaryRegion,
    secondaryRegion,
    regionType: 'primary',
    kmsKey: primarySecurityStack.kmsKey,
    alertingTopic: primarySecurityStack.criticalAlertsTopic,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

const multiRegionStack = new MultiRegionStack(app, `${projectName}-${environment}-multi-region-test`, {
    environment,
    projectName,
    regionType: 'primary',
    primaryRegion,
    secondaryRegion,
    primaryVpc: primaryNetworkStack.vpc,
    secondaryVpc: secondaryNetworkStack.vpc,
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: primaryRegion
    }
});

// Set up dependencies
primarySecurityStack.addDependency(primaryNetworkStack);
primaryRdsStack.addDependency(primaryNetworkStack);
primaryRdsStack.addDependency(primarySecurityStack);
primaryMskStack.addDependency(primaryNetworkStack);
primaryMskStack.addDependency(primarySecurityStack);

secondarySecurityStack.addDependency(secondaryNetworkStack);
secondaryRdsStack.addDependency(secondaryNetworkStack);
secondaryRdsStack.addDependency(secondarySecurityStack);
secondaryRdsStack.addDependency(primaryRdsStack);
secondaryMskStack.addDependency(secondaryNetworkStack);
secondaryMskStack.addDependency(secondarySecurityStack);

primaryMskCrossRegionStack.addDependency(primaryMskStack);
primaryMskCrossRegionStack.addDependency(secondaryMskStack);
primaryCrossRegionObservabilityStack.addDependency(primarySecurityStack);
multiRegionStack.addDependency(primaryNetworkStack);
multiRegionStack.addDependency(secondaryNetworkStack);

console.log('Multi-region infrastructure test configuration complete.');
console.log('Core components: Network, Security, RDS (Aurora Global), MSK, Cross-Region Replication');