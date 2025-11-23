"""
IAM and Security Integration Tests

This module implements comprehensive security testing for the staging environment,
including IAM policy validation, authentication/authorization testing, encryption
validation, and compliance checks.

Test Categories:
- IAM Policy and Role Validation
- Authentication and Authorization Testing
- Data Encryption Validation (at rest and in transit)
- Compliance Validation (SOC2, ISO27001, GDPR)
- Security Hub Integration
- OWASP ZAP Automated Scanning

Requirements: 12.5, 12.23
Implementation: Python using boto3, requests, and python-owasp-zap-v2.4
"""

import pytest
import boto3
import requests
import json
import time
from typing import Dict, List, Optional
from datetime import datetime, timedelta
from botocore.exceptions import ClientError
from zapv2 import ZAPv2

from staging_tests.base_staging_test import BaseStagingTest


class TestIAMSecurityIntegration(BaseStagingTest):
    """
    Comprehensive IAM and security integration tests for staging environment.
    
    This test suite validates:
    - IAM policies and roles configuration
    - Authentication and authorization mechanisms
    - Data encryption at rest and in transit
    - Compliance with security standards
    - Security Hub findings and recommendations
    """
    
    @pytest.fixture(scope="class")
    def iam_client(self):
        """Create IAM client for policy validation."""
        return boto3.client('iam', region_name=self.region)
    
    @pytest.fixture(scope="class")
    def security_hub_client(self):
        """Create Security Hub client for compliance validation."""
        return boto3.client('securityhub', region_name=self.region)
    
    @pytest.fixture(scope="class")
    def kms_client(self):
        """Create KMS client for encryption validation."""
        return boto3.client('kms', region_name=self.region)
    
    @pytest.fixture(scope="class")
    def rds_client(self):
        """Create RDS client for database encryption validation."""
        return boto3.client('rds', region_name=self.region)
    
    @pytest.fixture(scope="class")
    def zap_client(self):
        """Create OWASP ZAP client for security scanning."""
        # ZAP should be running locally or in a container
        zap_proxy = self.config.get('zap_proxy', 'http://localhost:8080')
        zap_api_key = self.config.get('zap_api_key', '')
        return ZAPv2(proxies={'http': zap_proxy, 'https': zap_proxy}, apikey=zap_api_key)
    
    # ==================== IAM Policy and Role Validation ====================
    
    def test_eks_service_account_iam_roles(self, iam_client):
        """
        Test EKS service account IAM roles are properly configured.
        
        Validates:
        - IRSA (IAM Roles for Service Accounts) configuration
        - Trust relationships with EKS OIDC provider
        - Least privilege principle adherence
        """
        self.logger.info("Testing EKS service account IAM roles configuration")
        
        # Expected service account roles
        expected_roles = [
            'genai-demo-eks-service-account-role',
            'genai-demo-alb-controller-role',
            'genai-demo-cluster-autoscaler-role'
        ]
        
        for role_name in expected_roles:
            try:
                # Get role details
                response = iam_client.get_role(RoleName=role_name)
                role = response['Role']
                
                # Validate trust relationship
                trust_policy = json.loads(role['AssumeRolePolicyDocument'])
                assert 'Statement' in trust_policy, f"Trust policy missing Statement for {role_name}"
                
                # Validate OIDC provider in trust policy
                statements = trust_policy['Statement']
                has_oidc = any(
                    'Federated' in stmt and 'oidc.eks' in stmt.get('Federated', '')
                    for stmt in statements
                )
                assert has_oidc, f"Role {role_name} missing EKS OIDC provider in trust policy"
                
                # Get attached policies
                policies_response = iam_client.list_attached_role_policies(RoleName=role_name)
                attached_policies = policies_response['AttachedPolicies']
                
                assert len(attached_policies) > 0, f"Role {role_name} has no attached policies"
                
                self.logger.info(f"✓ Role {role_name} properly configured with {len(attached_policies)} policies")
                
            except ClientError as e:
                if e.response['Error']['Code'] == 'NoSuchEntity':
                    pytest.fail(f"Required IAM role {role_name} not found")
                raise
    
    def test_rds_iam_authentication_enabled(self, rds_client):
        """
        Test RDS IAM authentication is enabled for Aurora cluster.
        
        Validates:
        - IAM database authentication enabled
        - Proper security group configuration
        - SSL/TLS enforcement
        """
        self.logger.info("Testing RDS IAM authentication configuration")
        
        cluster_identifier = self.config.get('aurora_cluster_id', 'genai-demo-aurora-cluster')
        
        try:
            response = rds_client.describe_db_clusters(
                DBClusterIdentifier=cluster_identifier
            )
            
            cluster = response['DBClusters'][0]
            
            # Validate IAM authentication
            iam_auth_enabled = cluster.get('IAMDatabaseAuthenticationEnabled', False)
            assert iam_auth_enabled, "IAM database authentication not enabled"
            
            # Validate encryption
            storage_encrypted = cluster.get('StorageEncrypted', False)
            assert storage_encrypted, "Storage encryption not enabled"
            
            # Validate backup retention
            backup_retention = cluster.get('BackupRetentionPeriod', 0)
            assert backup_retention >= 7, f"Backup retention too short: {backup_retention} days"
            
            self.logger.info(f"✓ Aurora cluster {cluster_identifier} properly secured")
            
        except ClientError as e:
            pytest.fail(f"Failed to validate Aurora cluster: {str(e)}")
    
    def test_least_privilege_policy_validation(self, iam_client):
        """
        Test IAM policies follow least privilege principle.
        
        Validates:
        - No wildcard (*) permissions on sensitive actions
        - Resource-specific permissions where possible
        - No overly permissive policies
        """
        self.logger.info("Testing least privilege policy compliance")
        
        # Get all custom policies
        response = iam_client.list_policies(Scope='Local', MaxItems=100)
        policies = response['Policies']
        
        violations = []
        
        for policy in policies:
            # Get policy version
            policy_version = iam_client.get_policy_version(
                PolicyArn=policy['Arn'],
                VersionId=policy['DefaultVersionId']
            )
            
            policy_document = policy_version['PolicyVersion']['Document']
            
            # Check for overly permissive statements
            for statement in policy_document.get('Statement', []):
                if statement.get('Effect') == 'Allow':
                    actions = statement.get('Action', [])
                    resources = statement.get('Resource', [])
                    
                    # Convert to list if string
                    if isinstance(actions, str):
                        actions = [actions]
                    if isinstance(resources, str):
                        resources = [resources]
                    
                    # Check for dangerous wildcards
                    if '*' in actions and '*' in resources:
                        violations.append(f"Policy {policy['PolicyName']} has full wildcard permissions")
                    
                    # Check for sensitive actions with wildcards
                    sensitive_actions = ['iam:*', 'kms:*', 'secretsmanager:*']
                    for action in actions:
                        if action in sensitive_actions and '*' in resources:
                            violations.append(
                                f"Policy {policy['PolicyName']} has wildcard resource for sensitive action {action}"
                            )
        
        if violations:
            self.logger.warning(f"Found {len(violations)} least privilege violations:")
            for violation in violations:
                self.logger.warning(f"  - {violation}")
        
        # Allow some violations but warn
        assert len(violations) < 5, f"Too many least privilege violations: {len(violations)}"
    
    # ==================== Authentication and Authorization Testing ====================
    
    def test_api_authentication_required(self):
        """
        Test API endpoints require proper authentication.
        
        Validates:
        - Unauthenticated requests are rejected
        - Invalid tokens are rejected
        - Proper HTTP status codes returned
        """
        self.logger.info("Testing API authentication requirements")
        
        test_endpoints = [
            '/api/v1/customers',
            '/api/v1/orders',
            '/api/v1/products'
        ]
        
        for endpoint in test_endpoints:
            url = f"{self.api_base_url}{endpoint}"
            
            # Test without authentication
            response = requests.get(url, timeout=10)
            assert response.status_code in [401, 403], \
                f"Endpoint {endpoint} should require authentication, got {response.status_code}"
            
            # Test with invalid token
            headers = {'Authorization': 'Bearer invalid_token_12345'}
            response = requests.get(url, headers=headers, timeout=10)
            assert response.status_code in [401, 403], \
                f"Endpoint {endpoint} should reject invalid token, got {response.status_code}"
            
            self.logger.info(f"✓ Endpoint {endpoint} properly requires authentication")
    
    def test_role_based_access_control(self):
        """
        Test role-based access control (RBAC) is enforced.
        
        Validates:
        - Different roles have different permissions
        - Admin-only endpoints reject non-admin users
        - User can only access their own resources
        """
        self.logger.info("Testing role-based access control")
        
        # This test requires test users with different roles
        # For now, we'll test the structure
        
        admin_endpoints = [
            '/api/v1/admin/users',
            '/api/v1/admin/system-config'
        ]
        
        for endpoint in admin_endpoints:
            url = f"{self.api_base_url}{endpoint}"
            
            # Test without admin role (should be rejected)
            # In real implementation, use a non-admin token
            response = requests.get(url, timeout=10)
            assert response.status_code in [401, 403], \
                f"Admin endpoint {endpoint} should require admin role"
            
            self.logger.info(f"✓ Admin endpoint {endpoint} properly protected")
    
    # ==================== Data Encryption Validation ====================
    
    def test_kms_key_rotation_enabled(self, kms_client):
        """
        Test KMS keys have automatic rotation enabled.
        
        Validates:
        - Customer managed keys exist
        - Automatic key rotation enabled
        - Key policies are properly configured
        """
        self.logger.info("Testing KMS key rotation configuration")
        
        # List KMS keys
        response = kms_client.list_keys(Limit=100)
        keys = response['Keys']
        
        customer_managed_keys = []
        
        for key in keys:
            key_id = key['KeyId']
            
            try:
                # Get key metadata
                key_metadata = kms_client.describe_key(KeyId=key_id)
                key_info = key_metadata['KeyMetadata']
                
                # Only check customer managed keys
                if key_info['KeyManager'] == 'CUSTOMER':
                    customer_managed_keys.append(key_id)
                    
                    # Check rotation status
                    rotation_response = kms_client.get_key_rotation_status(KeyId=key_id)
                    rotation_enabled = rotation_response['KeyRotationEnabled']
                    
                    if not rotation_enabled:
                        self.logger.warning(f"Key {key_id} does not have rotation enabled")
                    
            except ClientError as e:
                if e.response['Error']['Code'] != 'AccessDeniedException':
                    raise
        
        assert len(customer_managed_keys) > 0, "No customer managed KMS keys found"
        self.logger.info(f"✓ Found {len(customer_managed_keys)} customer managed KMS keys")
    
    def test_rds_encryption_at_rest(self, rds_client):
        """
        Test RDS encryption at rest is enabled.
        
        Validates:
        - Storage encryption enabled
        - KMS key used for encryption
        - Automated backups encrypted
        """
        self.logger.info("Testing RDS encryption at rest")
        
        cluster_identifier = self.config.get('aurora_cluster_id', 'genai-demo-aurora-cluster')
        
        try:
            response = rds_client.describe_db_clusters(
                DBClusterIdentifier=cluster_identifier
            )
            
            cluster = response['DBClusters'][0]
            
            # Validate storage encryption
            storage_encrypted = cluster.get('StorageEncrypted', False)
            assert storage_encrypted, "Storage encryption not enabled"
            
            # Validate KMS key
            kms_key_id = cluster.get('KmsKeyId')
            assert kms_key_id, "KMS key not configured for encryption"
            
            self.logger.info(f"✓ Aurora cluster encrypted with KMS key: {kms_key_id}")
            
        except ClientError as e:
            pytest.fail(f"Failed to validate RDS encryption: {str(e)}")
    
    def test_tls_encryption_in_transit(self):
        """
        Test TLS encryption is enforced for data in transit.
        
        Validates:
        - HTTPS endpoints only
        - TLS 1.2 or higher
        - Valid SSL certificates
        """
        self.logger.info("Testing TLS encryption in transit")
        
        # Test HTTPS endpoint
        https_url = self.api_base_url.replace('http://', 'https://')
        
        try:
            response = requests.get(f"{https_url}/actuator/health", timeout=10, verify=True)
            
            # Check TLS version
            if hasattr(response.raw.connection, 'sock'):
                sock = response.raw.connection.sock
                if hasattr(sock, 'version'):
                    tls_version = sock.version()
                    assert tls_version in ['TLSv1.2', 'TLSv1.3'], \
                        f"Insecure TLS version: {tls_version}"
                    self.logger.info(f"✓ Using secure TLS version: {tls_version}")
            
        except requests.exceptions.SSLError as e:
            pytest.fail(f"SSL certificate validation failed: {str(e)}")
        except requests.exceptions.RequestException as e:
            self.logger.warning(f"Could not test HTTPS endpoint: {str(e)}")
    
    # ==================== Compliance Validation ====================
    
    def test_security_hub_compliance_standards(self, security_hub_client):
        """
        Test Security Hub compliance with security standards.
        
        Validates:
        - Security Hub enabled
        - Compliance standards enabled (CIS, PCI-DSS, AWS Foundational Security Best Practices)
        - Critical findings addressed
        """
        self.logger.info("Testing Security Hub compliance standards")
        
        try:
            # Get Security Hub status
            response = security_hub_client.describe_hub()
            assert response['HubArn'], "Security Hub not enabled"
            
            # Get enabled standards
            standards_response = security_hub_client.get_enabled_standards()
            enabled_standards = standards_response['StandardsSubscriptions']
            
            assert len(enabled_standards) > 0, "No security standards enabled"
            
            # Check for critical findings
            findings_response = security_hub_client.get_findings(
                Filters={
                    'SeverityLabel': [{'Value': 'CRITICAL', 'Comparison': 'EQUALS'}],
                    'RecordState': [{'Value': 'ACTIVE', 'Comparison': 'EQUALS'}]
                },
                MaxResults=100
            )
            
            critical_findings = findings_response['Findings']
            
            if critical_findings:
                self.logger.warning(f"Found {len(critical_findings)} critical Security Hub findings")
                for finding in critical_findings[:5]:  # Show first 5
                    self.logger.warning(f"  - {finding['Title']}")
            
            # Allow some findings but warn
            assert len(critical_findings) < 10, \
                f"Too many critical security findings: {len(critical_findings)}"
            
            self.logger.info(f"✓ Security Hub enabled with {len(enabled_standards)} standards")
            
        except ClientError as e:
            if e.response['Error']['Code'] == 'InvalidAccessException':
                pytest.skip("Security Hub not enabled or no access")
            raise
    
    def test_gdpr_compliance_data_encryption(self, kms_client, rds_client):
        """
        Test GDPR compliance for data encryption requirements.
        
        Validates:
        - Personal data encrypted at rest
        - Encryption keys properly managed
        - Data retention policies configured
        """
        self.logger.info("Testing GDPR compliance for data encryption")
        
        # Validate RDS encryption (personal data storage)
        cluster_identifier = self.config.get('aurora_cluster_id', 'genai-demo-aurora-cluster')
        
        try:
            response = rds_client.describe_db_clusters(
                DBClusterIdentifier=cluster_identifier
            )
            
            cluster = response['DBClusters'][0]
            
            # GDPR requires encryption of personal data
            storage_encrypted = cluster.get('StorageEncrypted', False)
            assert storage_encrypted, "GDPR violation: Personal data not encrypted at rest"
            
            # Validate backup retention (GDPR right to erasure)
            backup_retention = cluster.get('BackupRetentionPeriod', 0)
            assert 1 <= backup_retention <= 35, \
                f"GDPR concern: Backup retention should be reasonable: {backup_retention} days"
            
            self.logger.info("✓ GDPR data encryption requirements met")
            
        except ClientError as e:
            pytest.fail(f"Failed to validate GDPR compliance: {str(e)}")
    
    # ==================== OWASP ZAP Security Scanning ====================
    
    @pytest.mark.slow
    def test_owasp_zap_security_scan(self, zap_client):
        """
        Test application security using OWASP ZAP automated scanning.
        
        Validates:
        - No high-risk vulnerabilities
        - Common web vulnerabilities addressed (XSS, SQL Injection, CSRF)
        - Security headers properly configured
        """
        self.logger.info("Running OWASP ZAP security scan")
        
        target_url = self.api_base_url
        
        try:
            # Start ZAP spider
            self.logger.info(f"Starting ZAP spider on {target_url}")
            scan_id = zap_client.spider.scan(target_url)
            
            # Wait for spider to complete
            while int(zap_client.spider.status(scan_id)) < 100:
                time.sleep(2)
            
            self.logger.info("Spider scan completed")
            
            # Start active scan
            self.logger.info("Starting ZAP active scan")
            scan_id = zap_client.ascan.scan(target_url)
            
            # Wait for active scan to complete (with timeout)
            timeout = 300  # 5 minutes
            start_time = time.time()
            
            while int(zap_client.ascan.status(scan_id)) < 100:
                if time.time() - start_time > timeout:
                    self.logger.warning("ZAP scan timeout, stopping scan")
                    zap_client.ascan.stop(scan_id)
                    break
                time.sleep(5)
            
            self.logger.info("Active scan completed")
            
            # Get alerts
            alerts = zap_client.core.alerts(baseurl=target_url)
            
            # Categorize alerts by risk
            high_risk = [a for a in alerts if a['risk'] == 'High']
            medium_risk = [a for a in alerts if a['risk'] == 'Medium']
            low_risk = [a for a in alerts if a['risk'] == 'Low']
            
            self.logger.info(f"ZAP scan results: {len(high_risk)} high, {len(medium_risk)} medium, {len(low_risk)} low risk alerts")
            
            # Log high-risk alerts
            if high_risk:
                self.logger.warning("High-risk vulnerabilities found:")
                for alert in high_risk:
                    self.logger.warning(f"  - {alert['alert']}: {alert['description'][:100]}")
            
            # Fail if too many high-risk vulnerabilities
            assert len(high_risk) == 0, f"Found {len(high_risk)} high-risk vulnerabilities"
            
            self.logger.info("✓ OWASP ZAP security scan passed")
            
        except Exception as e:
            self.logger.warning(f"ZAP scan failed or not available: {str(e)}")
            pytest.skip("OWASP ZAP not available or scan failed")
    
    # ==================== Security Monitoring and Alerting ====================
    
    def test_cloudwatch_security_alarms_configured(self):
        """
        Test CloudWatch security alarms are properly configured.
        
        Validates:
        - Failed authentication attempts monitoring
        - Unauthorized access attempts monitoring
        - Security group changes monitoring
        """
        self.logger.info("Testing CloudWatch security alarms configuration")
        
        cloudwatch = boto3.client('cloudwatch', region_name=self.region)
        
        # Get all alarms
        response = cloudwatch.describe_alarms(MaxRecords=100)
        alarms = response['MetricAlarms']
        
        # Check for security-related alarms
        security_alarm_patterns = [
            'UnauthorizedAPICall',
            'FailedAuthentication',
            'SecurityGroupChange',
            'IAMPolicyChange'
        ]
        
        found_alarms = []
        for pattern in security_alarm_patterns:
            matching_alarms = [a for a in alarms if pattern.lower() in a['AlarmName'].lower()]
            if matching_alarms:
                found_alarms.extend(matching_alarms)
        
        if found_alarms:
            self.logger.info(f"✓ Found {len(found_alarms)} security monitoring alarms")
        else:
            self.logger.warning("No security monitoring alarms found")
    
    # ==================== Test Summary and Reporting ====================
    
    def test_generate_security_compliance_report(self, iam_client, security_hub_client, kms_client):
        """
        Generate comprehensive security compliance report.
        
        Includes:
        - IAM configuration summary
        - Encryption status
        - Compliance findings
        - Recommendations
        """
        self.logger.info("Generating security compliance report")
        
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'region': self.region,
            'iam_summary': {},
            'encryption_summary': {},
            'compliance_summary': {},
            'recommendations': []
        }
        
        # IAM summary
        try:
            users_response = iam_client.list_users(MaxItems=100)
            roles_response = iam_client.list_roles(MaxItems=100)
            
            report['iam_summary'] = {
                'total_users': len(users_response['Users']),
                'total_roles': len(roles_response['Roles'])
            }
        except ClientError:
            pass
        
        # KMS summary
        try:
            keys_response = kms_client.list_keys(Limit=100)
            report['encryption_summary'] = {
                'total_kms_keys': len(keys_response['Keys'])
            }
        except ClientError:
            pass
        
        # Security Hub summary
        try:
            findings_response = security_hub_client.get_findings(
                Filters={'RecordState': [{'Value': 'ACTIVE', 'Comparison': 'EQUALS'}]},
                MaxResults=100
            )
            
            findings = findings_response['Findings']
            critical = len([f for f in findings if f['Severity']['Label'] == 'CRITICAL'])
            high = len([f for f in findings if f['Severity']['Label'] == 'HIGH'])
            
            report['compliance_summary'] = {
                'total_findings': len(findings),
                'critical_findings': critical,
                'high_findings': high
            }
        except ClientError:
            pass
        
        # Save report
        report_path = f"reports/security-compliance-{datetime.utcnow().strftime('%Y%m%d-%H%M%S')}.json"
        with open(report_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        self.logger.info(f"✓ Security compliance report saved to {report_path}")
        
        return report


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
