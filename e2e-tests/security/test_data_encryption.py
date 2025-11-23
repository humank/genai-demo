#!/usr/bin/env python3
"""
Data Encryption Validation Test

This script validates data encryption across all regions in Active-Active architecture.

Test Categories:
1. Data at rest encryption (RDS, S3, EBS)
2. Data in transit encryption (TLS/SSL)
3. Key management validation (KMS)
4. Certificate validation
"""

import asyncio
import logging
import ssl
import socket
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime

import boto3
import aiohttp
from cryptography import x509
from cryptography.hazmat.backends import default_backend

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


@dataclass
class EncryptionTestConfig:
    """Configuration for encryption tests"""
    regions: List[str] = field(default_factory=lambda: ["us-east-1", "us-west-2", "eu-west-1"])
    api_endpoints: Dict[str, str] = field(default_factory=lambda: {
        "us-east-1": "https://api-us-east-1.example.com",
        "us-west-2": "https://api-us-west-2.example.com",
        "eu-west-1": "https://api-eu-west-1.example.com"
    })
    min_tls_version: str = "TLSv1.3"
    required_cipher_suites: List[str] = field(default_factory=lambda: [
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256"
    ])


@dataclass
class EncryptionTestResult:
    """Result of encryption test"""
    test_name: str
    success: bool
    region: str
    details: Dict = field(default_factory=dict)
    issues: List[str] = field(default_factory=list)
    error_message: Optional[str] = None


class DataEncryptionTest:
    """Test suite for data encryption validation"""
    
    def __init__(self, config: EncryptionTestConfig):
        self.config = config
        self.ec2_clients = {r: boto3.client('ec2', region_name=r) for r in config.regions}
        self.rds_clients = {r: boto3.client('rds', region_name=r) for r in config.regions}
        self.s3_clients = {r: boto3.client('s3', region_name=r) for r in config.regions}
        self.kms_clients = {r: boto3.client('kms', region_name=r) for r in config.regions}
    
    async def test_rds_encryption_at_rest(self) -> List[EncryptionTestResult]:
        """Test RDS encryption at rest"""
        logger.info("Testing RDS encryption at rest...")
        results = []
        
        for region in self.config.regions:
            try:
                # Check RDS instances
                response = self.rds_clients[region].describe_db_instances()
                
                issues = []
                encrypted_count = 0
                total_count = len(response.get('DBInstances', []))
                
                for db in response.get('DBInstances', []):
                    if not db.get('StorageEncrypted', False):
                        issues.append(f"DB instance {db['DBInstanceIdentifier']} not encrypted")
                    else:
                        encrypted_count += 1
                
                success = len(issues) == 0
                
                results.append(EncryptionTestResult(
                    test_name="rds_encryption_at_rest",
                    success=success,
                    region=region,
                    details={
                        "total_instances": total_count,
                        "encrypted_instances": encrypted_count
                    },
                    issues=issues
                ))
                
            except Exception as e:
                logger.error(f"RDS encryption test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="rds_encryption_at_rest",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def test_s3_encryption_at_rest(self) -> List[EncryptionTestResult]:
        """Test S3 bucket encryption"""
        logger.info("Testing S3 encryption at rest...")
        results = []
        
        for region in self.config.regions:
            try:
                # List buckets in region
                response = self.s3_clients[region].list_buckets()
                
                issues = []
                encrypted_count = 0
                total_count = 0
                
                for bucket in response.get('Buckets', []):
                    bucket_name = bucket['Name']
                    total_count += 1
                    
                    try:
                        # Check encryption configuration
                        self.s3_clients[region].get_bucket_encryption(Bucket=bucket_name)
                        encrypted_count += 1
                    except self.s3_clients[region].exceptions.ServerSideEncryptionConfigurationNotFoundError:
                        issues.append(f"Bucket {bucket_name} has no encryption configuration")
                
                success = len(issues) == 0
                
                results.append(EncryptionTestResult(
                    test_name="s3_encryption_at_rest",
                    success=success,
                    region=region,
                    details={
                        "total_buckets": total_count,
                        "encrypted_buckets": encrypted_count
                    },
                    issues=issues
                ))
                
            except Exception as e:
                logger.error(f"S3 encryption test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="s3_encryption_at_rest",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def test_ebs_encryption(self) -> List[EncryptionTestResult]:
        """Test EBS volume encryption"""
        logger.info("Testing EBS encryption...")
        results = []
        
        for region in self.config.regions:
            try:
                # Check EBS volumes
                response = self.ec2_clients[region].describe_volumes()
                
                issues = []
                encrypted_count = 0
                total_count = len(response.get('Volumes', []))
                
                for volume in response.get('Volumes', []):
                    if not volume.get('Encrypted', False):
                        issues.append(f"EBS volume {volume['VolumeId']} not encrypted")
                    else:
                        encrypted_count += 1
                
                success = len(issues) == 0
                
                results.append(EncryptionTestResult(
                    test_name="ebs_encryption",
                    success=success,
                    region=region,
                    details={
                        "total_volumes": total_count,
                        "encrypted_volumes": encrypted_count
                    },
                    issues=issues
                ))
                
            except Exception as e:
                logger.error(f"EBS encryption test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="ebs_encryption",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def test_tls_encryption_in_transit(self) -> List[EncryptionTestResult]:
        """Test TLS encryption for data in transit"""
        logger.info("Testing TLS encryption in transit...")
        results = []
        
        for region in self.config.regions:
            endpoint = self.config.api_endpoints[region]
            
            try:
                # Parse endpoint
                from urllib.parse import urlparse
                parsed = urlparse(endpoint)
                hostname = parsed.hostname
                port = parsed.port or 443
                
                # Check TLS configuration
                context = ssl.create_default_context()
                
                with socket.create_connection((hostname, port), timeout=10) as sock:
                    with context.wrap_socket(sock, server_hostname=hostname) as ssock:
                        # Get TLS version
                        tls_version = ssock.version()
                        cipher = ssock.cipher()
                        
                        issues = []
                        
                        # Check TLS version
                        if tls_version != self.config.min_tls_version:
                            issues.append(f"TLS version {tls_version} does not meet minimum {self.config.min_tls_version}")
                        
                        # Check cipher suite
                        cipher_name = cipher[0] if cipher else "Unknown"
                        if cipher_name not in self.config.required_cipher_suites:
                            issues.append(f"Cipher suite {cipher_name} not in required list")
                        
                        success = len(issues) == 0
                        
                        results.append(EncryptionTestResult(
                            test_name="tls_encryption_in_transit",
                            success=success,
                            region=region,
                            details={
                                "tls_version": tls_version,
                                "cipher_suite": cipher_name,
                                "endpoint": endpoint
                            },
                            issues=issues
                        ))
                
            except Exception as e:
                logger.error(f"TLS encryption test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="tls_encryption_in_transit",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def test_certificate_validity(self) -> List[EncryptionTestResult]:
        """Test SSL certificate validity"""
        logger.info("Testing SSL certificate validity...")
        results = []
        
        for region in self.config.regions:
            endpoint = self.config.api_endpoints[region]
            
            try:
                from urllib.parse import urlparse
                parsed = urlparse(endpoint)
                hostname = parsed.hostname
                port = parsed.port or 443
                
                # Get certificate
                context = ssl.create_default_context()
                
                with socket.create_connection((hostname, port), timeout=10) as sock:
                    with context.wrap_socket(sock, server_hostname=hostname) as ssock:
                        cert_bin = ssock.getpeercert(binary_form=True)
                        cert = x509.load_der_x509_certificate(cert_bin, default_backend())
                        
                        issues = []
                        
                        # Check expiration
                        now = datetime.utcnow()
                        if cert.not_valid_after < now:
                            issues.append("Certificate has expired")
                        elif (cert.not_valid_after - now).days < 30:
                            issues.append(f"Certificate expires in {(cert.not_valid_after - now).days} days")
                        
                        # Check validity start
                        if cert.not_valid_before > now:
                            issues.append("Certificate not yet valid")
                        
                        success = len(issues) == 0
                        
                        results.append(EncryptionTestResult(
                            test_name="certificate_validity",
                            success=success,
                            region=region,
                            details={
                                "subject": cert.subject.rfc4514_string(),
                                "issuer": cert.issuer.rfc4514_string(),
                                "not_before": cert.not_valid_before.isoformat(),
                                "not_after": cert.not_valid_after.isoformat(),
                                "days_until_expiry": (cert.not_valid_after - now).days
                            },
                            issues=issues
                        ))
                
            except Exception as e:
                logger.error(f"Certificate validity test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="certificate_validity",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def test_kms_key_rotation(self) -> List[EncryptionTestResult]:
        """Test KMS key rotation"""
        logger.info("Testing KMS key rotation...")
        results = []
        
        for region in self.config.regions:
            try:
                # List KMS keys
                response = self.kms_clients[region].list_keys()
                
                issues = []
                rotation_enabled_count = 0
                total_count = 0
                
                for key in response.get('Keys', []):
                    key_id = key['KeyId']
                    total_count += 1
                    
                    try:
                        # Check rotation status
                        rotation_response = self.kms_clients[region].get_key_rotation_status(KeyId=key_id)
                        
                        if not rotation_response.get('KeyRotationEnabled', False):
                            issues.append(f"KMS key {key_id} does not have rotation enabled")
                        else:
                            rotation_enabled_count += 1
                    except:
                        pass  # Skip keys we can't check
                
                success = len(issues) == 0
                
                results.append(EncryptionTestResult(
                    test_name="kms_key_rotation",
                    success=success,
                    region=region,
                    details={
                        "total_keys": total_count,
                        "rotation_enabled": rotation_enabled_count
                    },
                    issues=issues
                ))
                
            except Exception as e:
                logger.error(f"KMS key rotation test failed in {region}: {str(e)}")
                results.append(EncryptionTestResult(
                    test_name="kms_key_rotation",
                    success=False,
                    region=region,
                    error_message=str(e)
                ))
        
        return results
    
    async def run_all_tests(self) -> List[EncryptionTestResult]:
        """Run all encryption tests"""
        logger.info("Starting data encryption test suite")
        
        all_results = []
        
        # Run tests
        all_results.extend(await self.test_rds_encryption_at_rest())
        all_results.extend(await self.test_s3_encryption_at_rest())
        all_results.extend(await self.test_ebs_encryption())
        all_results.extend(await self.test_tls_encryption_in_transit())
        all_results.extend(await self.test_certificate_validity())
        all_results.extend(await self.test_kms_key_rotation())
        
        # Print summary
        self._print_summary(all_results)
        
        return all_results
    
    def _print_summary(self, results: List[EncryptionTestResult]):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Data Encryption Test Results")
        logger.info("="*80)
        
        by_test = {}
        for result in results:
            if result.test_name not in by_test:
                by_test[result.test_name] = []
            by_test[result.test_name].append(result)
        
        for test_name, test_results in by_test.items():
            logger.info(f"\n{test_name}:")
            for result in test_results:
                status = "✓ PASS" if result.success else "✗ FAIL"
                logger.info(f"  {status} - {result.region}")
                
                if result.error_message:
                    logger.info(f"    Error: {result.error_message}")
                elif result.issues:
                    for issue in result.issues:
                        logger.info(f"    Issue: {issue}")
        
        passed = sum(1 for r in results if r.success)
        total = len(results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point"""
    config = EncryptionTestConfig()
    test_suite = DataEncryptionTest(config)
    
    results = await test_suite.run_all_tests()
    
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
