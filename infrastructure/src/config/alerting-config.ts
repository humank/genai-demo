import { AlertingConfig } from '../stacks/alerting-stack';

/**
 * Alerting configuration for different environments
 */
export const alertingConfigs: Record<string, AlertingConfig> = {
  development: {
    criticalAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
      // No SMS or Slack for development
    },
    warningAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
    },
    infoAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
    },
  },

  staging: {
    criticalAlerts: {
      emailAddresses: [
        'dev-team@company.com',
        'qa-team@company.com',
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/STAGING/WEBHOOK',
    },
    warningAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/STAGING/WEBHOOK',
    },
    infoAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
    },
  },

  production: {
    criticalAlerts: {
      emailAddresses: [
        'sre-team@company.com',
        'dev-team@company.com',
        'ops-team@company.com',
      ],
      phoneNumbers: [
        '+1234567890', // On-call engineer
        '+1234567891', // Backup on-call
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/PRODUCTION/WEBHOOK',
    },
    warningAlerts: {
      emailAddresses: [
        'sre-team@company.com',
        'dev-team@company.com',
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/PRODUCTION/WEBHOOK',
    },
    infoAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
    },
  },

  'production-dr': {
    criticalAlerts: {
      emailAddresses: [
        'sre-team@company.com',
        'dev-team@company.com',
        'ops-team@company.com',
      ],
      phoneNumbers: [
        '+1234567890', // On-call engineer
        '+1234567891', // Backup on-call
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/PRODUCTION/WEBHOOK',
    },
    warningAlerts: {
      emailAddresses: [
        'sre-team@company.com',
        'dev-team@company.com',
      ],
      slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/PRODUCTION/WEBHOOK',
    },
    infoAlerts: {
      emailAddresses: [
        'dev-team@company.com',
      ],
    },
  },
};

/**
 * Get alerting configuration for a specific environment
 */
export function getAlertingConfig(environment: string): AlertingConfig {
  const config = alertingConfigs[environment];
  if (!config) {
    throw new Error(`No alerting configuration found for environment: ${environment}`);
  }
  return config;
}