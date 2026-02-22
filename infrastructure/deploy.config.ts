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
        account: '584518143473',
        // domain: 'dev.kimkao.io' — kimkao.io 域名已停用，待取得新域名後再啟用
    },
    staging: {
        environment: 'staging',
        region: 'ap-east-2',
        account: '584518143473',
        // domain: 'staging.kimkao.io' — kimkao.io 域名已停用
    },
    production: {
        environment: 'production',
        region: 'ap-east-2',
        account: '584518143473',
        // domain: 'kimkao.io' — kimkao.io 域名已停用
    },
    'production-dr': {
        environment: 'production-dr',
        region: 'ap-northeast-1',
        account: '584518143473',
        // domain: 'dr.kimkao.io' — kimkao.io 域名已停用
    }
};

export function getDeploymentConfig(environment: string): DeploymentConfig {
    const config = deploymentConfigs[environment];
    if (!config) {
        throw new Error(`No deployment configuration found for environment: ${environment}`);
    }
    return config;
}
