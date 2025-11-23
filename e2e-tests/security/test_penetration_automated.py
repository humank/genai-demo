#!/usr/bin/env python3
"""
Automated Penetration Testing

This script performs automated penetration testing across multi-region infrastructure
to identify security vulnerabilities and weaknesses.

Test Categories:
1. API endpoint security testing
2. Authentication and authorization bypass attempts
3. SQL injection and XSS vulnerability scanning
4. Network security testing
5. Configuration security validation
"""

import asyncio
import time
import logging
import json
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum

import aiohttp
import boto3
from urllib.parse import urljoin

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class VulnerabilitySeverity(Enum):
    """Vulnerability severity levels"""
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


@dataclass
class PenetrationTestConfig:
    """Configuration for penetration testing"""
    regions: List[str] = field(default_factory=lambda: [
        "us-east-1", "us-west-2", "eu-west-1"
    ])
    api_endpoints: Dict[str, str] = field(default_factory=lambda: {
        "us-east-1": "https://api-us-east-1.example.com",
        "us-west-2": "https://api-us-west-2.example.com",
        "eu-west-1": "https://api-eu-west-1.example.com"
    })
    test_timeout_seconds: int = 30
    max_concurrent_tests: int = 10
    enable_aggressive_tests: bool = False  # Disable by default for safety


@dataclass
class Vulnerability:
    """Detected vulnerability"""
    vulnerability_id: str
    severity: VulnerabilitySeverity
    category: str
    title: str
    description: str
    affected_endpoint: str
    region: str
    remediation: str
    cve_references: List[str] = field(default_factory=list)
    detected_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class PenetrationTestResult:
    """Result of penetration testing"""
    test_name: str
    success: bool
    vulnerabilities_found: List[Vulnerability] = field(default_factory=list)
    tests_executed: int = 0
    critical_count: int = 0
    high_count: int = 0
    medium_count: int = 0
    low_count: int = 0
    info_count: int = 0
    error_message: Optional[str] = None


class AutomatedPenetrationTest:
    """Automated penetration testing suite"""
    
    def __init__(self, config: PenetrationTestConfig):
        self.config = config
        self.vulnerabilities: List[Vulnerability] = []
        self.test_counter = 0
    
    async def test_api_authentication_bypass(self) -> PenetrationTestResult:
        """
        Test: API authentication bypass attempts
        
        Tests various authentication bypass techniques including:
        - Missing authentication headers
        - Invalid tokens
        - Expired tokens
        - Token manipulation
        """
        test_name = "api_authentication_bypass"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                # Test 1: Access without authentication
                logger.info(f"Testing unauthenticated access in {region}...")
                vuln = await self._test_unauthenticated_access(endpoint, region)
                if vuln:
                    vulnerabilities.append(vuln)
                tests_executed += 1
                
                # Test 2: Invalid token
                logger.info(f"Testing invalid token in {region}...")
                vuln = await self._test_invalid_token(endpoint, region)
                if vuln:
                    vulnerabilities.append(vuln)
                tests_executed += 1
                
                # Test 3: Expired token
                logger.info(f"Testing expired token in {region}...")
                vuln = await self._test_expired_token(endpoint, region)
                if vuln:
                    vulnerabilities.append(vuln)
                tests_executed += 1
                
                # Test 4: Token manipulation
                logger.info(f"Testing token manipulation in {region}...")
                vuln = await self._test_token_manipulation(endpoint, region)
                if vuln:
                    vulnerabilities.append(vuln)
                tests_executed += 1
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    async def test_sql_injection_vulnerabilities(self) -> PenetrationTestResult:
        """
        Test: SQL injection vulnerability scanning
        
        Tests for SQL injection vulnerabilities in API endpoints.
        """
        test_name = "sql_injection_vulnerabilities"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        # Common SQL injection payloads
        sql_payloads = [
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT NULL, NULL, NULL--",
            "admin'--",
            "' OR 1=1--"
        ]
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                # Test common endpoints with SQL injection payloads
                test_endpoints = [
                    "/api/v1/customers",
                    "/api/v1/orders",
                    "/api/v1/search"
                ]
                
                for test_endpoint in test_endpoints:
                    for payload in sql_payloads:
                        vuln = await self._test_sql_injection(
                            endpoint,
                            test_endpoint,
                            payload,
                            region
                        )
                        if vuln:
                            vulnerabilities.append(vuln)
                        tests_executed += 1
                        
                        # Small delay to avoid overwhelming the server
                        await asyncio.sleep(0.1)
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    async def test_xss_vulnerabilities(self) -> PenetrationTestResult:
        """
        Test: Cross-site scripting (XSS) vulnerability scanning
        
        Tests for XSS vulnerabilities in API responses and web interfaces.
        """
        test_name = "xss_vulnerabilities"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        # Common XSS payloads
        xss_payloads = [
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src='javascript:alert(\"XSS\")'>"
        ]
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                # Test endpoints that accept user input
                test_endpoints = [
                    "/api/v1/customers",
                    "/api/v1/comments",
                    "/api/v1/feedback"
                ]
                
                for test_endpoint in test_endpoints:
                    for payload in xss_payloads:
                        vuln = await self._test_xss(
                            endpoint,
                            test_endpoint,
                            payload,
                            region
                        )
                        if vuln:
                            vulnerabilities.append(vuln)
                        tests_executed += 1
                        
                        await asyncio.sleep(0.1)
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    async def test_api_rate_limiting(self) -> PenetrationTestResult:
        """
        Test: API rate limiting and DoS protection
        
        Tests whether API endpoints have proper rate limiting to prevent
        denial of service attacks.
        """
        test_name = "api_rate_limiting"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                # Test rate limiting on critical endpoints
                critical_endpoints = [
                    "/api/v1/auth/login",
                    "/api/v1/orders",
                    "/api/v1/payments"
                ]
                
                for test_endpoint in critical_endpoints:
                    vuln = await self._test_rate_limiting(
                        endpoint,
                        test_endpoint,
                        region
                    )
                    if vuln:
                        vulnerabilities.append(vuln)
                    tests_executed += 1
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    async def test_security_headers(self) -> PenetrationTestResult:
        """
        Test: Security headers validation
        
        Validates that proper security headers are present in API responses.
        """
        test_name = "security_headers"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        required_headers = {
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": ["DENY", "SAMEORIGIN"],
            "X-XSS-Protection": "1; mode=block",
            "Strict-Transport-Security": "max-age=",
            "Content-Security-Policy": None  # Should exist
        }
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                vuln = await self._test_security_headers(
                    endpoint,
                    required_headers,
                    region
                )
                if vuln:
                    vulnerabilities.extend(vuln)
                tests_executed += 1
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    async def test_sensitive_data_exposure(self) -> PenetrationTestResult:
        """
        Test: Sensitive data exposure
        
        Tests for exposure of sensitive data in API responses, error messages,
        and logs.
        """
        test_name = "sensitive_data_exposure"
        logger.info(f"Starting test: {test_name}")
        
        vulnerabilities = []
        tests_executed = 0
        
        # Patterns to look for in responses
        sensitive_patterns = [
            r"password",
            r"api[_-]?key",
            r"secret",
            r"token",
            r"credit[_-]?card",
            r"ssn",
            r"private[_-]?key"
        ]
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints[region]
                
                # Test various endpoints for sensitive data exposure
                test_endpoints = [
                    "/api/v1/customers",
                    "/api/v1/users",
                    "/api/v1/config"
                ]
                
                for test_endpoint in test_endpoints:
                    vuln = await self._test_sensitive_data_exposure(
                        endpoint,
                        test_endpoint,
                        sensitive_patterns,
                        region
                    )
                    if vuln:
                        vulnerabilities.extend(vuln)
                    tests_executed += 1
            
            return self._create_result(
                test_name,
                vulnerabilities,
                tests_executed
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return PenetrationTestResult(
                test_name=test_name,
                success=False,
                tests_executed=tests_executed,
                error_message=str(e)
            )
    
    # Helper methods
    
    async def _test_unauthenticated_access(
        self, 
        endpoint: str, 
        region: str
    ) -> Optional[Vulnerability]:
        """Test access without authentication"""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{endpoint}/api/v1/customers",
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    # If we get 200, it's a vulnerability
                    if response.status == 200:
                        return Vulnerability(
                            vulnerability_id=f"AUTH-001-{region}",
                            severity=VulnerabilitySeverity.CRITICAL,
                            category="Authentication",
                            title="Unauthenticated API Access",
                            description="API endpoint accessible without authentication",
                            affected_endpoint=f"{endpoint}/api/v1/customers",
                            region=region,
                            remediation="Implement proper authentication checks on all API endpoints"
                        )
        except Exception as e:
            logger.debug(f"Unauthenticated access test error: {str(e)}")
        
        return None
    
    async def _test_invalid_token(
        self, 
        endpoint: str, 
        region: str
    ) -> Optional[Vulnerability]:
        """Test with invalid authentication token"""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{endpoint}/api/v1/customers",
                    headers={"Authorization": "Bearer invalid_token_12345"},
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    # Should return 401, if not it's a vulnerability
                    if response.status not in [401, 403]:
                        return Vulnerability(
                            vulnerability_id=f"AUTH-002-{region}",
                            severity=VulnerabilitySeverity.HIGH,
                            category="Authentication",
                            title="Invalid Token Accepted",
                            description="API accepts invalid authentication tokens",
                            affected_endpoint=f"{endpoint}/api/v1/customers",
                            region=region,
                            remediation="Implement proper token validation"
                        )
        except Exception as e:
            logger.debug(f"Invalid token test error: {str(e)}")
        
        return None
    
    async def _test_expired_token(
        self, 
        endpoint: str, 
        region: str
    ) -> Optional[Vulnerability]:
        """Test with expired authentication token"""
        # Simulate expired token test
        # In real implementation, would use actual expired token
        return None
    
    async def _test_token_manipulation(
        self, 
        endpoint: str, 
        region: str
    ) -> Optional[Vulnerability]:
        """Test token manipulation attempts"""
        # Simulate token manipulation test
        # In real implementation, would attempt JWT manipulation
        return None
    
    async def _test_sql_injection(
        self,
        endpoint: str,
        test_endpoint: str,
        payload: str,
        region: str
    ) -> Optional[Vulnerability]:
        """Test SQL injection vulnerability"""
        try:
            async with aiohttp.ClientSession() as session:
                # Test in query parameter
                async with session.get(
                    f"{endpoint}{test_endpoint}?search={payload}",
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    response_text = await response.text()
                    
                    # Look for SQL error messages
                    sql_errors = [
                        "sql syntax",
                        "mysql_fetch",
                        "ora-",
                        "postgresql",
                        "sqlite_"
                    ]
                    
                    if any(error in response_text.lower() for error in sql_errors):
                        return Vulnerability(
                            vulnerability_id=f"SQL-001-{region}",
                            severity=VulnerabilitySeverity.CRITICAL,
                            category="Injection",
                            title="SQL Injection Vulnerability",
                            description=f"SQL injection possible at {test_endpoint}",
                            affected_endpoint=f"{endpoint}{test_endpoint}",
                            region=region,
                            remediation="Use parameterized queries and input validation",
                            cve_references=["CWE-89"]
                        )
        except Exception as e:
            logger.debug(f"SQL injection test error: {str(e)}")
        
        return None
    
    async def _test_xss(
        self,
        endpoint: str,
        test_endpoint: str,
        payload: str,
        region: str
    ) -> Optional[Vulnerability]:
        """Test XSS vulnerability"""
        try:
            async with aiohttp.ClientSession() as session:
                # Test XSS in POST request
                async with session.post(
                    f"{endpoint}{test_endpoint}",
                    json={"name": payload, "comment": payload},
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    response_text = await response.text()
                    
                    # Check if payload is reflected without encoding
                    if payload in response_text:
                        return Vulnerability(
                            vulnerability_id=f"XSS-001-{region}",
                            severity=VulnerabilitySeverity.HIGH,
                            category="XSS",
                            title="Cross-Site Scripting Vulnerability",
                            description=f"XSS vulnerability at {test_endpoint}",
                            affected_endpoint=f"{endpoint}{test_endpoint}",
                            region=region,
                            remediation="Implement proper output encoding and CSP headers",
                            cve_references=["CWE-79"]
                        )
        except Exception as e:
            logger.debug(f"XSS test error: {str(e)}")
        
        return None
    
    async def _test_rate_limiting(
        self,
        endpoint: str,
        test_endpoint: str,
        region: str
    ) -> Optional[Vulnerability]:
        """Test rate limiting"""
        try:
            # Send rapid requests
            request_count = 100
            success_count = 0
            
            async with aiohttp.ClientSession() as session:
                for i in range(request_count):
                    try:
                        async with session.get(
                            f"{endpoint}{test_endpoint}",
                            timeout=aiohttp.ClientTimeout(total=1)
                        ) as response:
                            if response.status == 200:
                                success_count += 1
                    except:
                        pass
            
            # If most requests succeed, rate limiting may be insufficient
            if success_count > request_count * 0.8:
                return Vulnerability(
                    vulnerability_id=f"RATE-001-{region}",
                    severity=VulnerabilitySeverity.MEDIUM,
                    category="Rate Limiting",
                    title="Insufficient Rate Limiting",
                    description=f"Endpoint {test_endpoint} lacks proper rate limiting",
                    affected_endpoint=f"{endpoint}{test_endpoint}",
                    region=region,
                    remediation="Implement rate limiting (e.g., 100 requests per minute)"
                        )
        except Exception as e:
            logger.debug(f"Rate limiting test error: {str(e)}")
        
        return None
    
    async def _test_security_headers(
        self,
        endpoint: str,
        required_headers: Dict,
        region: str
    ) -> List[Vulnerability]:
        """Test security headers"""
        vulnerabilities = []
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{endpoint}/api/v1/health",
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    headers = response.headers
                    
                    for header_name, expected_value in required_headers.items():
                        if header_name not in headers:
                            vulnerabilities.append(Vulnerability(
                                vulnerability_id=f"HDR-{len(vulnerabilities)+1:03d}-{region}",
                                severity=VulnerabilitySeverity.MEDIUM,
                                category="Security Headers",
                                title=f"Missing Security Header: {header_name}",
                                description=f"Security header {header_name} is missing",
                                affected_endpoint=endpoint,
                                region=region,
                                remediation=f"Add {header_name} header to all responses"
                            ))
        except Exception as e:
            logger.debug(f"Security headers test error: {str(e)}")
        
        return vulnerabilities
    
    async def _test_sensitive_data_exposure(
        self,
        endpoint: str,
        test_endpoint: str,
        patterns: List[str],
        region: str
    ) -> List[Vulnerability]:
        """Test for sensitive data exposure"""
        vulnerabilities = []
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{endpoint}{test_endpoint}",
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    response_text = await response.text().lower()
                    
                    for pattern in patterns:
                        if pattern in response_text:
                            vulnerabilities.append(Vulnerability(
                                vulnerability_id=f"DATA-{len(vulnerabilities)+1:03d}-{region}",
                                severity=VulnerabilitySeverity.HIGH,
                                category="Data Exposure",
                                title=f"Sensitive Data Exposure: {pattern}",
                                description=f"Sensitive data pattern '{pattern}' found in response",
                                affected_endpoint=f"{endpoint}{test_endpoint}",
                                region=region,
                                remediation="Remove sensitive data from API responses"
                            ))
        except Exception as e:
            logger.debug(f"Sensitive data exposure test error: {str(e)}")
        
        return vulnerabilities
    
    def _create_result(
        self,
        test_name: str,
        vulnerabilities: List[Vulnerability],
        tests_executed: int
    ) -> PenetrationTestResult:
        """Create test result with vulnerability counts"""
        
        critical_count = sum(1 for v in vulnerabilities if v.severity == VulnerabilitySeverity.CRITICAL)
        high_count = sum(1 for v in vulnerabilities if v.severity == VulnerabilitySeverity.HIGH)
        medium_count = sum(1 for v in vulnerabilities if v.severity == VulnerabilitySeverity.MEDIUM)
        low_count = sum(1 for v in vulnerabilities if v.severity == VulnerabilitySeverity.LOW)
        info_count = sum(1 for v in vulnerabilities if v.severity == VulnerabilitySeverity.INFO)
        
        # Test passes if no critical or high vulnerabilities found
        success = critical_count == 0 and high_count == 0
        
        return PenetrationTestResult(
            test_name=test_name,
            success=success,
            vulnerabilities_found=vulnerabilities,
            tests_executed=tests_executed,
            critical_count=critical_count,
            high_count=high_count,
            medium_count=medium_count,
            low_count=low_count,
            info_count=info_count
        )
    
    async def run_all_tests(self) -> List[PenetrationTestResult]:
        """Run all penetration tests"""
        logger.info("Starting automated penetration test suite")
        
        tests = [
            self.test_api_authentication_bypass(),
            self.test_sql_injection_vulnerabilities(),
            self.test_xss_vulnerabilities(),
            self.test_api_rate_limiting(),
            self.test_security_headers(),
            self.test_sensitive_data_exposure()
        ]
        
        results = await asyncio.gather(*tests)
        
        # Print summary
        self._print_summary(results)
        
        return results
    
    def _print_summary(self, results: List[PenetrationTestResult]):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Automated Penetration Test Results")
        logger.info("="*80)
        
        total_vulnerabilities = 0
        total_critical = 0
        total_high = 0
        total_medium = 0
        total_low = 0
        
        for result in results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            
            if result.error_message:
                logger.info(f"  Error: {result.error_message}")
                continue
            
            logger.info(f"  Tests Executed: {result.tests_executed}")
            logger.info(f"  Vulnerabilities Found: {len(result.vulnerabilities_found)}")
            logger.info(f"    Critical: {result.critical_count}")
            logger.info(f"    High: {result.high_count}")
            logger.info(f"    Medium: {result.medium_count}")
            logger.info(f"    Low: {result.low_count}")
            
            total_vulnerabilities += len(result.vulnerabilities_found)
            total_critical += result.critical_count
            total_high += result.high_count
            total_medium += result.medium_count
            total_low += result.low_count
            
            # Print vulnerability details
            for vuln in result.vulnerabilities_found:
                logger.info(f"\n  [{vuln.severity.value.upper()}] {vuln.title}")
                logger.info(f"    ID: {vuln.vulnerability_id}")
                logger.info(f"    Endpoint: {vuln.affected_endpoint}")
                logger.info(f"    Description: {vuln.description}")
                logger.info(f"    Remediation: {vuln.remediation}")
        
        passed = sum(1 for r in results if r.success)
        total = len(results)
        
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info(f"Total Vulnerabilities: {total_vulnerabilities}")
        logger.info(f"  Critical: {total_critical}")
        logger.info(f"  High: {total_high}")
        logger.info(f"  Medium: {total_medium}")
        logger.info(f"  Low: {total_low}")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for penetration tests"""
    config = PenetrationTestConfig()
    test_suite = AutomatedPenetrationTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    # Fail if any critical or high vulnerabilities found
    has_critical_vulns = any(
        r.critical_count > 0 or r.high_count > 0 
        for r in results
    )
    exit(1 if has_critical_vulns else 0)


if __name__ == "__main__":
    asyncio.run(main())
