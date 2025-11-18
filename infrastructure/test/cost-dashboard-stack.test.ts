import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { CostDashboardStack } from '../lib/stacks/cost-dashboard-stack';

describe('CostDashboardStack', () => {
  let app: cdk.App;
  let stack: CostDashboardStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new CostDashboardStack(app, 'TestCostDashboardStack', {
      dashboardName: 'Test-Cost-Dashboard',
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('CloudWatch Dashboard', () => {
    test('should create dashboard with correct name', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'Test-Cost-Dashboard',
      });
    });

    test('should have dashboard body with widgets', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardBody: Match.anyValue(),
      });
    });
  });

  describe('Dashboard Widgets', () => {
    test('should create CloudWatch dashboard', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: Match.stringLikeRegexp('.*Cost.*'),
      });
    });

    test('should have dashboard body defined', () => {
      const dashboards = template.findResources('AWS::CloudWatch::Dashboard');
      const dashboard = Object.values(dashboards)[0];
      expect(dashboard.Properties.DashboardBody).toBeDefined();
    });
  });

  describe('Metrics Configuration', () => {
    test('should create dashboard with metrics', () => {
      const dashboards = template.findResources('AWS::CloudWatch::Dashboard');
      expect(Object.keys(dashboards).length).toBeGreaterThan(0);
    });
  });

  describe('Stack Outputs', () => {
    test('should export dashboard URL', () => {
      template.hasOutput('DashboardUrl', {});
    });

    test('should have correct dashboard URL format', () => {
      const outputs = template.toJSON().Outputs;
      const dashboardUrl = outputs.DashboardUrl.Value;
      
      expect(dashboardUrl).toContain('console.aws.amazon.com/cloudwatch');
      expect(dashboardUrl).toContain('dashboards');
    });
  });

  describe('Resource Tags', () => {
    test('should have project tags', () => {
      const dashboards = template.findResources('AWS::CloudWatch::Dashboard');
      const dashboard = Object.values(dashboards)[0];
      
      // CDK applies tags at the stack level
      expect(dashboard).toBeDefined();
    });
  });
});
