export interface DeploymentConfig {
    environment: string;
    region: string;
    account?: string;
    domain?: string;
    certificateArn?: string;
    hostedZoneId?: string;
}

export const deploymentConfigs: Record<string, DeploymentConfig> = {
    development: {
        environment: 'development',
        region: 'ap-east-2',
        domain: 'dev.kimkao.io'
    },
    staging: {
        environment: 'staging',
        region: 'ap-east-2',
        domain: 'staging.kimkao.io'
    },
    production: {
        environment: 'production',
        region: 'ap-east-2',
        domain: 'kimkao.io'
    },
    'production-dr': {
        environment: 'production-dr',
        region: 'ap-northeast-1',
        domain: 'dr.kimkao.io'
    }
};

export function getDeploymentConfig(environment: string): DeploymentConfig {
    const config = deploymentConfigs[environment];
    if (!config) {
        throw new Error(`No deployment configuration found for environment: ${environment}`);
    }
    return config;
}