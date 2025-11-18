import * as cdk from 'aws-cdk-lib';
import * as synthetics from 'aws-cdk-lib/aws-synthetics';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface CloudWatchSyntheticsMonitoringProps {
    /**
     * Environment name for resource naming
     */
    environment: string;

    /**
     * API endpoint base URL to monitor
     */
    apiEndpoint: string;

    /**
     * SNS topic for critical alerts
     */
    criticalAlertTopic?: sns.Topic;

    /**
     * SNS topic for warning alerts
     */
    warningAlertTopic?: sns.Topic;

    /**
     * Enable detailed monitoring (default: true)
     */
    enableDetailedMonitoring?: boolean;

    /**
     * Canary execution frequency in minutes (default: 1)
     */
    executionFrequencyMinutes?: number;

    /**
     * Critical business process endpoints to monitor
     */
    criticalEndpoints?: {
        path: string;
        method: string;
        expectedStatusCode: number;
        maxLatencyMs: number;
    }[];
}

/**
 * CloudWatch Synthetics Monitoring Construct
 * 
 * Implements proactive monitoring with:
 * - Automated end-to-end functional tests for new deployments
 * - API endpoint health monitoring with 1-minute detection
 * - Critical business process failure analysis
 * 
 * Requirements: 13.13, 13.14, 13.15
 */
export class CloudWatchSyntheticsMonitoring extends Construct {
    public readonly canaries: synthetics.Canary[];
    public readonly artifactsBucket: s3.Bucket;
    public readonly canaryRole: iam.Role;
    public readonly alarms: cloudwatch.Alarm[];

    constructor(scope: Construct, id: string, props: CloudWatchSyntheticsMonitoringProps) {
        super(scope, id);

        this.canaries = [];
        this.alarms = [];

        const environment = props.environment;
        const enableDetailedMonitoring = props.enableDetailedMonitoring ?? true;
        const executionFrequencyMinutes = props.executionFrequencyMinutes ?? 1;

        // Create S3 bucket for canary artifacts
        this.artifactsBucket = new s3.Bucket(this, 'CanaryArtifactsBucket', {
            bucketName: `${environment}-synthetics-canary-artifacts-${cdk.Aws.ACCOUNT_ID}`,
            encryption: s3.BucketEncryption.S3_MANAGED,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            autoDeleteObjects: true,
            lifecycleRules: [
                {
                    id: 'DeleteOldArtifacts',
                    enabled: true,
                    expiration: cdk.Duration.days(30),
                },
            ],
        });

        // Create IAM role for canaries
        this.canaryRole = new iam.Role(this, 'CanaryRole', {
            roleName: `${environment}-synthetics-canary-role`,
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            description: 'IAM role for CloudWatch Synthetics canaries',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchSyntheticsFullAccess'),
            ],
        });

        // Grant S3 permissions to canary role
        this.artifactsBucket.grantReadWrite(this.canaryRole);

        // Add CloudWatch Logs permissions
        this.canaryRole.addToPolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'logs:CreateLogGroup',
                    'logs:CreateLogStream',
                    'logs:PutLogEvents',
                ],
                resources: [`arn:aws:logs:${cdk.Aws.REGION}:${cdk.Aws.ACCOUNT_ID}:log-group:/aws/lambda/cwsyn-*`],
            })
        );

        // Add CloudWatch Metrics permissions
        this.canaryRole.addToPolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: ['cloudwatch:PutMetricData'],
                resources: ['*'],
                conditions: {
                    StringEquals: {
                        'cloudwatch:namespace': 'CloudWatchSynthetics',
                    },
                },
            })
        );

        // Create API health check canary
        const apiHealthCanary = this.createApiHealthCheckCanary(
            environment,
            props.apiEndpoint,
            executionFrequencyMinutes
        );
        this.canaries.push(apiHealthCanary);

        // Create critical business process canaries
        if (props.criticalEndpoints && props.criticalEndpoints.length > 0) {
            props.criticalEndpoints.forEach((endpoint, index) => {
                const canary = this.createBusinessProcessCanary(
                    environment,
                    props.apiEndpoint,
                    endpoint,
                    index,
                    executionFrequencyMinutes
                );
                this.canaries.push(canary);
            });
        }

        // Create end-to-end functional test canary
        const e2eCanary = this.createE2EFunctionalTestCanary(
            environment,
            props.apiEndpoint,
            executionFrequencyMinutes
        );
        this.canaries.push(e2eCanary);

        // Create CloudWatch alarms for canaries
        this.createCanaryAlarms(props);

        // Output canary information
        new cdk.CfnOutput(this, 'CanaryArtifactsBucketName', {
            value: this.artifactsBucket.bucketName,
            description: 'S3 bucket for canary artifacts',
        });

        new cdk.CfnOutput(this, 'CanaryCount', {
            value: this.canaries.length.toString(),
            description: 'Number of active canaries',
        });
    }

    /**
     * Create API health check canary for 1-minute detection
     */
    private createApiHealthCheckCanary(
        environment: string,
        apiEndpoint: string,
        frequencyMinutes: number
    ): synthetics.Canary {
        const canary = new synthetics.Canary(this, 'ApiHealthCheckCanary', {
            canaryName: `${environment}-api-health-check`,
            runtime: synthetics.Runtime.SYNTHETICS_NODEJS_PUPPETEER_6_2,
            test: synthetics.Test.custom({
                code: synthetics.Code.fromInline(`
const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');
const http = require('http');

const apiHealthCheck = async function () {
    const url = '${apiEndpoint}/actuator/health';
    log.info('Checking API health at: ' + url);

    const startTime = Date.now();
    
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https') ? https : http;
        
        const req = protocol.get(url, (res) => {
            const latency = Date.now() - startTime;
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                log.info('Response status: ' + res.statusCode);
                log.info('Response latency: ' + latency + 'ms');
                log.info('Response body: ' + data);

                // Check status code
                if (res.statusCode !== 200) {
                    reject(new Error('API health check failed with status: ' + res.statusCode));
                    return;
                }

                // Check latency (should be < 2000ms)
                if (latency > 2000) {
                    log.warn('High latency detected: ' + latency + 'ms');
                }

                // Parse response
                try {
                    const health = JSON.parse(data);
                    if (health.status !== 'UP') {
                        reject(new Error('API health status is not UP: ' + health.status));
                        return;
                    }
                    
                    log.info('API health check passed');
                    resolve();
                } catch (e) {
                    reject(new Error('Failed to parse health response: ' + e.message));
                }
            });
        });

        req.on('error', (e) => {
            reject(new Error('API health check request failed: ' + e.message));
        });

        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('API health check request timeout'));
        });
    });
};

exports.handler = async () => {
    return await apiHealthCheck();
};
                `),
                handler: 'index.handler',
            }),
            schedule: synthetics.Schedule.rate(cdk.Duration.minutes(frequencyMinutes)),
            role: this.canaryRole,
            artifactsBucketLocation: {
                bucket: this.artifactsBucket,
            },
            environmentVariables: {
                API_ENDPOINT: apiEndpoint,
            },
        });

        return canary;
    }

    /**
     * Create business process monitoring canary
     */
    private createBusinessProcessCanary(
        environment: string,
        apiEndpoint: string,
        endpoint: { path: string; method: string; expectedStatusCode: number; maxLatencyMs: number },
        index: number,
        frequencyMinutes: number
    ): synthetics.Canary {
        const canary = new synthetics.Canary(this, `BusinessProcessCanary${index}`, {
            canaryName: `${environment}-business-process-${index}`,
            runtime: synthetics.Runtime.SYNTHETICS_NODEJS_PUPPETEER_6_2,
            test: synthetics.Test.custom({
                code: synthetics.Code.fromInline(`
const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');
const http = require('http');

const businessProcessCheck = async function () {
    const url = '${apiEndpoint}${endpoint.path}';
    const method = '${endpoint.method}';
    const expectedStatus = ${endpoint.expectedStatusCode};
    const maxLatency = ${endpoint.maxLatencyMs};
    
    log.info('Checking business process: ' + method + ' ' + url);

    const startTime = Date.now();
    
    return new Promise((resolve, reject) => {
        const urlObj = new URL(url);
        const protocol = urlObj.protocol === 'https:' ? https : http;
        
        const options = {
            hostname: urlObj.hostname,
            port: urlObj.port,
            path: urlObj.pathname + urlObj.search,
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'User-Agent': 'CloudWatch-Synthetics'
            }
        };

        const req = protocol.request(options, (res) => {
            const latency = Date.now() - startTime;
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                log.info('Response status: ' + res.statusCode);
                log.info('Response latency: ' + latency + 'ms');

                // Check status code
                if (res.statusCode !== expectedStatus) {
                    reject(new Error('Unexpected status code: ' + res.statusCode + ', expected: ' + expectedStatus));
                    return;
                }

                // Check latency
                if (latency > maxLatency) {
                    reject(new Error('High latency: ' + latency + 'ms, max allowed: ' + maxLatency + 'ms'));
                    return;
                }

                log.info('Business process check passed');
                resolve();
            });
        });

        req.on('error', (e) => {
            reject(new Error('Business process check failed: ' + e.message));
        });

        req.setTimeout(10000, () => {
            req.destroy();
            reject(new Error('Business process check timeout'));
        });

        req.end();
    });
};

exports.handler = async () => {
    return await businessProcessCheck();
};
                `),
                handler: 'index.handler',
            }),
            schedule: synthetics.Schedule.rate(cdk.Duration.minutes(frequencyMinutes)),
            role: this.canaryRole,
            artifactsBucketLocation: {
                bucket: this.artifactsBucket,
            },
            environmentVariables: {
                API_ENDPOINT: apiEndpoint,
                ENDPOINT_PATH: endpoint.path,
                HTTP_METHOD: endpoint.method,
                EXPECTED_STATUS: endpoint.expectedStatusCode.toString(),
                MAX_LATENCY_MS: endpoint.maxLatencyMs.toString(),
            },
        });

        return canary;
    }

    /**
     * Create end-to-end functional test canary
     */
    private createE2EFunctionalTestCanary(
        environment: string,
        apiEndpoint: string,
        frequencyMinutes: number
    ): synthetics.Canary {
        const canary = new synthetics.Canary(this, 'E2EFunctionalTestCanary', {
            canaryName: `${environment}-e2e-functional-test`,
            runtime: synthetics.Runtime.SYNTHETICS_NODEJS_PUPPETEER_6_2,
            test: synthetics.Test.custom({
                code: synthetics.Code.fromInline(`
const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');
const http = require('http');

const e2eFunctionalTest = async function () {
    log.info('Starting end-to-end functional test');

    // Test 1: Health check
    await testHealthCheck();
    
    // Test 2: API readiness
    await testApiReadiness();
    
    // Test 3: Database connectivity
    await testDatabaseConnectivity();
    
    log.info('End-to-end functional test completed successfully');
};

const testHealthCheck = async function () {
    const url = '${apiEndpoint}/actuator/health';
    log.info('Testing health check: ' + url);
    
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https') ? https : http;
        
        const req = protocol.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                if (res.statusCode !== 200) {
                    reject(new Error('Health check failed: ' + res.statusCode));
                    return;
                }
                const health = JSON.parse(data);
                if (health.status !== 'UP') {
                    reject(new Error('Health status not UP: ' + health.status));
                    return;
                }
                log.info('Health check passed');
                resolve();
            });
        });
        
        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('Health check timeout'));
        });
    });
};

const testApiReadiness = async function () {
    const url = '${apiEndpoint}/actuator/health/readiness';
    log.info('Testing API readiness: ' + url);
    
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https') ? https : http;
        
        const req = protocol.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                if (res.statusCode !== 200) {
                    reject(new Error('Readiness check failed: ' + res.statusCode));
                    return;
                }
                const readiness = JSON.parse(data);
                if (readiness.status !== 'UP') {
                    reject(new Error('Readiness status not UP: ' + readiness.status));
                    return;
                }
                log.info('API readiness check passed');
                resolve();
            });
        });
        
        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('Readiness check timeout'));
        });
    });
};

const testDatabaseConnectivity = async function () {
    const url = '${apiEndpoint}/actuator/health/db';
    log.info('Testing database connectivity: ' + url);
    
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https') ? https : http;
        
        const req = protocol.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                if (res.statusCode !== 200) {
                    reject(new Error('Database connectivity check failed: ' + res.statusCode));
                    return;
                }
                const dbHealth = JSON.parse(data);
                if (dbHealth.status !== 'UP') {
                    reject(new Error('Database status not UP: ' + dbHealth.status));
                    return;
                }
                log.info('Database connectivity check passed');
                resolve();
            });
        });
        
        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('Database connectivity check timeout'));
        });
    });
};

exports.handler = async () => {
    return await e2eFunctionalTest();
};
                `),
                handler: 'index.handler',
            }),
            schedule: synthetics.Schedule.rate(cdk.Duration.minutes(frequencyMinutes * 5)), // Run every 5 minutes
            role: this.canaryRole,
            artifactsBucketLocation: {
                bucket: this.artifactsBucket,
            },
            environmentVariables: {
                API_ENDPOINT: apiEndpoint,
            },
        });

        return canary;
    }

    /**
     * Create CloudWatch alarms for canary monitoring
     */
    private createCanaryAlarms(props: CloudWatchSyntheticsMonitoringProps): void {
        this.canaries.forEach((canary, index) => {
            // Create alarm for canary failures
            const failureAlarm = new cloudwatch.Alarm(this, `CanaryFailureAlarm${index}`, {
                alarmName: `${props.environment}-${canary.canaryName}-failures`,
                alarmDescription: `Alert when canary ${canary.canaryName} fails`,
                metric: canary.metricFailed({
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(1),
                }),
                threshold: 1,
                evaluationPeriods: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            });

            if (props.criticalAlertTopic) {
                failureAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(props.criticalAlertTopic));
            }

            this.alarms.push(failureAlarm);

            // Create alarm for high latency
            const latencyAlarm = new cloudwatch.Alarm(this, `CanaryLatencyAlarm${index}`, {
                alarmName: `${props.environment}-${canary.canaryName}-high-latency`,
                alarmDescription: `Alert when canary ${canary.canaryName} has high latency`,
                metric: canary.metricDuration({
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5),
                }),
                threshold: 2000, // 2 seconds
                evaluationPeriods: 2,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            });

            if (props.warningAlertTopic) {
                latencyAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(props.warningAlertTopic));
            }

            this.alarms.push(latencyAlarm);
        });
    }

    /**
     * Add canary metrics to CloudWatch dashboard
     */
    public addToDashboard(dashboard: cloudwatch.Dashboard): void {
        // Add canary success rate widget
        const successRateWidget = new cloudwatch.GraphWidget({
            title: 'Synthetics Canary Success Rate',
            left: this.canaries.map((canary) =>
                canary.metricSuccessPercent({
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5),
                    label: canary.canaryName,
                })
            ),
            width: 12,
            height: 6,
        });

        // Add canary duration widget
        const durationWidget = new cloudwatch.GraphWidget({
            title: 'Synthetics Canary Duration',
            left: this.canaries.map((canary) =>
                canary.metricDuration({
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5),
                    label: canary.canaryName,
                })
            ),
            width: 12,
            height: 6,
        });

        // Add canary failure widget
        const failureWidget = new cloudwatch.GraphWidget({
            title: 'Synthetics Canary Failures',
            left: this.canaries.map((canary) =>
                canary.metricFailed({
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5),
                    label: canary.canaryName,
                })
            ),
            width: 12,
            height: 6,
        });

        dashboard.addWidgets(successRateWidget, durationWidget);
        dashboard.addWidgets(failureWidget);
    }
}
