export interface EnvironmentConfig {
    environment: string;
    region: string;
    stackPrefix: string;
    tags: Record<string, string>;
    vpc: {
        cidr: string;
        maxAzs: number;
        enableNatGateway: boolean;
        enableVpnGateway: boolean;
    };
    security: {
        enableCloudTrail: boolean;
        enableGuardDuty: boolean;
        enableSecurityHub: boolean;
    };
    observability: {
        enableXRay: boolean;
        enableCloudWatch: boolean;
        logRetentionDays: number;
    };
}

export function getEnvironmentConfig(environment: string, region: string): EnvironmentConfig {
    const baseConfig = {
        environment,
        region,
        stackPrefix: `genai-demo-${environment}-`,
        tags: {
            Project: 'genai-demo',
            Environment: environment,
            Region: region,
            ManagedBy: 'CDK',
        },
    };

    switch (environment) {
        case 'production':
            return {
                ...baseConfig,
                vpc: {
                    cidr: '10.0.0.0/16',
                    maxAzs: 3,
                    enableNatGateway: true,
                    enableVpnGateway: false,
                },
                security: {
                    enableCloudTrail: true,
                    enableGuardDuty: true,
                    enableSecurityHub: true,
                },
                observability: {
                    enableXRay: true,
                    enableCloudWatch: true,
                    logRetentionDays: 90,
                },
            };

        case 'staging':
            return {
                ...baseConfig,
                vpc: {
                    cidr: '10.1.0.0/16',
                    maxAzs: 2,
                    enableNatGateway: true,
                    enableVpnGateway: false,
                },
                security: {
                    enableCloudTrail: true,
                    enableGuardDuty: true,
                    enableSecurityHub: false,
                },
                observability: {
                    enableXRay: true,
                    enableCloudWatch: true,
                    logRetentionDays: 30,
                },
            };

        case 'development':
        default:
            return {
                ...baseConfig,
                vpc: {
                    cidr: '10.2.0.0/16',
                    maxAzs: 2,
                    enableNatGateway: false,
                    enableVpnGateway: false,
                },
                security: {
                    enableCloudTrail: false,
                    enableGuardDuty: false,
                    enableSecurityHub: false,
                },
                observability: {
                    enableXRay: false,
                    enableCloudWatch: true,
                    logRetentionDays: 7,
                },
            };
    }
}