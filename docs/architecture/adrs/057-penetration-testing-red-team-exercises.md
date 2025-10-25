---
adr_number: 057
title: "Penetration Testing and Red Team Exercises"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [053, 054, 055, 056]
affected_viewpoints: ["operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-057: Penetration Testing and Red Team Exercises

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires comprehensive security testing to:

- Identify security vulnerabilities before attackers do
- Validate security controls effectiveness
- Test incident response capabilities
- Comply with security standards (PCI-DSS, ISO 27001)
- Improve security posture continuously
- Prepare for real-world attacks

Taiwan's cyber security environment presents unique challenges:
- Sophisticated APT attacks from state-sponsored actors
- High-value e-commerce platform as attractive target
- Need to test defenses against advanced threats
- Regulatory requirements for security testing
- Limited internal security expertise

### Business Context

**Business Drivers**:
- Protect customer data and business operations
- Maintain platform reputation and trust
- Comply with PCI-DSS requirement 11.3 (penetration testing)
- Validate security investments
- Improve incident response readiness

**Constraints**:
- Must not disrupt production services
- Cannot expose real customer data
- Must comply with legal and ethical guidelines
- Budget: $50,000/year for external testing
- Internal team capacity: 2 security engineers

### Technical Context

**Current State**:
- No regular penetration testing
- No red team exercises
- Ad-hoc security assessments
- No vulnerability validation process
- Limited security testing expertise
- No attack simulation framework

**Requirements**:
- Regular penetration testing (quarterly)
- Annual red team exercises
- Automated security testing
- Vulnerability validation
- Incident response testing
- Compliance-ready reporting

## Decision Drivers

1. **Coverage**: Comprehensive testing of all attack vectors
2. **Frequency**: Regular testing to catch new vulnerabilities
3. **Realism**: Simulate real-world attack scenarios
4. **Compliance**: Meet PCI-DSS and ISO 27001 requirements
5. **Cost**: Optimize testing budget
6. **Expertise**: Leverage external expertise
7. **Safety**: Minimize risk to production systems
8. **Learning**: Improve team security skills

## Considered Options

### Option 1: Hybrid Penetration Testing Program (Recommended)

**Description**: Combination of external penetration testing, internal security testing, and red team exercises

**Components**:
- **Quarterly External Penetration Testing**: Professional security firm
- **Semi-Annual Internal Testing**: Internal security team
- **Annual Red Team Exercise**: Simulate APT attack
- **Continuous Automated Testing**: DAST, SAST tools
- **Bug Bounty Program**: Crowdsourced security testing
- **Post-Major-Update Testing**: Test after significant changes

**Pros**:
- ✅ Comprehensive coverage (external + internal + automated)
- ✅ Regular testing cadence
- ✅ Real-world attack simulation
- ✅ Cost-effective ($50K/year)
- ✅ Compliance-ready
- ✅ Continuous improvement
- ✅ Team skill development

**Cons**:
- ⚠️ Requires coordination
- ⚠️ Internal team capacity needed
- ⚠️ Potential for service disruption

**Cost**: $50,000/year ($30K external, $10K red team, $10K tools/bounty)

**Risk**: **Low** - Industry best practices

### Option 2: External Penetration Testing Only

**Description**: Rely solely on external security firms

**Pros**:
- ✅ Professional expertise
- ✅ Compliance-ready reports
- ✅ No internal capacity needed

**Cons**:
- ❌ Limited testing frequency
- ❌ No continuous testing
- ❌ No team skill development
- ❌ Higher cost for frequent testing

**Cost**: $80,000/year (quarterly external testing)

**Risk**: **Medium** - Gaps between tests

### Option 3: Internal Testing Only

**Description**: Build internal security testing capability

**Pros**:
- ✅ Continuous testing capability
- ✅ Deep system knowledge
- ✅ Lower long-term cost

**Cons**:
- ❌ Limited expertise
- ❌ Potential bias
- ❌ Compliance concerns
- ❌ High initial investment

**Cost**: $100,000/year (2 security engineers)

**Risk**: **High** - Insufficient expertise

## Decision Outcome

**Chosen Option**: **Hybrid Penetration Testing Program (Option 1)**

### Rationale

Hybrid penetration testing program was selected for the following reasons:

1. **Comprehensive**: Combines external expertise with internal capability
2. **Cost-Effective**: $50K/year vs $80K+ for external-only
3. **Continuous**: Regular testing plus automated scanning
4. **Compliance**: Meets PCI-DSS and ISO 27001 requirements
5. **Realistic**: Red team exercises simulate real attacks
6. **Skill Development**: Internal team learns from external experts
7. **Flexible**: Can adjust testing focus based on threats

### Penetration Testing Scope

**Testing Categories**:

**1. Web Application Testing**
- OWASP Top 10 vulnerabilities
- Authentication and authorization
- Session management
- Input validation
- Business logic flaws
- API security

**2. API Endpoint Testing**
- REST API security
- GraphQL security (if applicable)
- Authentication bypass
- Authorization flaws
- Rate limiting bypass
- Data exposure

**3. Infrastructure Testing**
- Network segmentation
- Firewall rules
- Cloud configuration
- Container security
- Kubernetes security
- Database security

**4. Social Engineering**
- Phishing campaigns
- Pretexting
- Physical security (optional)
- Insider threat simulation

**Testing Methodology**:
- **Black Box**: No prior knowledge (external attacker perspective)
- **Gray Box**: Limited knowledge (compromised user perspective)
- **White Box**: Full knowledge (insider threat perspective)

### Quarterly External Penetration Testing

**Testing Schedule**:
- **Q1**: Web application and API testing
- **Q2**: Infrastructure and cloud security testing
- **Q3**: Full-scope testing (web + infrastructure)
- **Q4**: Post-major-update testing

**Testing Process**:

**Week 1: Planning and Scoping**
- Define testing scope and objectives
- Identify critical assets and systems
- Establish rules of engagement
- Set up testing environment
- Coordinate with operations team

**Week 2-3: Testing Execution**
- Reconnaissance and information gathering
- Vulnerability identification
- Exploitation attempts
- Privilege escalation testing
- Lateral movement testing
- Data exfiltration simulation

**Week 4: Reporting and Remediation**
- Detailed findings report
- Risk prioritization
- Remediation recommendations
- Executive summary
- Remediation validation (optional)

**Vendor Selection Criteria**:
- CREST or OSCP certified testers
- Experience with e-commerce platforms
- Understanding of Taiwan cyber threats
- PCI-DSS testing experience
- Good communication and reporting

**Implementation**:
```yaml
# Penetration Testing Checklist
penetration_testing:
  frequency: quarterly
  vendor: "Professional Security Firm"
  scope:
    - web_applications
    - api_endpoints
    - infrastructure
    - cloud_configuration
  
  methodology:
    - black_box
    - gray_box
    - white_box
  
  deliverables:
    - detailed_findings_report
    - executive_summary
    - remediation_recommendations
    - retest_report
  
  cost_per_test: $7,500
  annual_cost: $30,000
```

### Semi-Annual Internal Testing

**Internal Testing Focus**:
- Validate external testing findings
- Test new features and changes
- Continuous security assessment
- Team skill development

**Testing Tools**:
- **Burp Suite Professional**: Web application testing
- **OWASP ZAP**: Automated scanning
- **Metasploit**: Exploitation framework
- **Nmap**: Network scanning
- **Nikto**: Web server scanning

**Internal Testing Process**:
```bash
# Automated security scanning
#!/bin/bash

# Web application scanning with OWASP ZAP
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://staging.example.com \
  -r zap-report.html

# API security testing
docker run -t owasp/zap2docker-stable zap-api-scan.py \
  -t https://api.staging.example.com/openapi.json \
  -f openapi \
  -r api-report.html

# Network scanning
nmap -sV -sC -oA nmap-scan staging.example.com

# SSL/TLS testing
testssl.sh --full staging.example.com

# Container security scanning
trivy image app:latest --severity HIGH,CRITICAL
```

### Annual Red Team Exercise

**Red Team Objectives**:
- Simulate Advanced Persistent Threat (APT) attack
- Test detection and response capabilities
- Identify security gaps
- Validate incident response procedures
- Improve security awareness

**Attack Scenarios**:

**Scenario 1: External Breach**
- Initial access via phishing or vulnerability exploitation
- Establish persistence
- Privilege escalation
- Lateral movement
- Data exfiltration
- Cover tracks

**Scenario 2: Insider Threat**
- Compromised employee account
- Abuse of legitimate access
- Data theft
- Sabotage attempts

**Scenario 3: Supply Chain Attack**
- Compromised third-party dependency
- Malicious code injection
- Backdoor installation

**Red Team Exercise Process**:

**Phase 1: Planning (Week 1-2)**
- Define objectives and scope
- Establish rules of engagement
- Identify target systems
- Coordinate with blue team (limited disclosure)
- Set up command and control infrastructure

**Phase 2: Execution (Week 3-6)**
- Reconnaissance and intelligence gathering
- Initial access attempts
- Establish foothold
- Privilege escalation
- Lateral movement
- Achieve objectives (data exfiltration, etc.)
- Maintain persistence

**Phase 3: Reporting (Week 7-8)**
- Detailed attack timeline
- Techniques, tactics, and procedures (TTPs)
- Detection gaps identified
- Blue team performance analysis
- Recommendations for improvement

**Implementation**:
```yaml
# Red Team Exercise Plan
red_team_exercise:
  frequency: annual
  duration: 6_weeks
  team: "External Red Team + Internal Security"
  
  objectives:
    - test_detection_capabilities
    - validate_incident_response
    - identify_security_gaps
    - improve_security_awareness
  
  scenarios:
    - external_breach
    - insider_threat
    - supply_chain_attack
  
  rules_of_engagement:
    - no_production_data_exfiltration
    - no_service_disruption
    - coordinate_with_operations
    - stop_on_critical_issues
  
  cost: $10,000
```

### Continuous Automated Testing

**DAST (Dynamic Application Security Testing)**:
```yaml
# GitHub Actions - DAST Scanning
name: DAST Scan

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  dast-scan:
    runs-on: ubuntu-latest
    steps:
      - name: OWASP ZAP Scan
        uses: zaproxy/action-full-scan@v0.4.0
        with:
          target: 'https://staging.example.com'
          rules_file_name: '.zap/rules.tsv'
          cmd_options: '-a'
      
      - name: Upload SARIF results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: results.sarif
```

**SAST (Static Application Security Testing)**:
```yaml
# GitHub Actions - SAST Scanning
name: SAST Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  sast-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Semgrep
        uses: returntocorp/semgrep-action@v1
        with:
          config: >-
            p/security-audit
            p/owasp-top-ten
            p/java
      
      - name: Run SonarQube
        uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

### Bug Bounty Program

**Program Structure**:
- **Platform**: HackerOne or Bugcrowd
- **Scope**: Production web application and APIs
- **Budget**: $10,000/year
- **Rewards**: $100-$5,000 per vulnerability

**Reward Tiers**:
| Severity | Reward Range | Examples |
|----------|--------------|----------|
| Critical | $2,000-$5,000 | RCE, SQL Injection, Authentication bypass |
| High | $500-$2,000 | XSS, CSRF, Authorization bypass |
| Medium | $200-$500 | Information disclosure, IDOR |
| Low | $100-$200 | Security misconfiguration |

**Program Rules**:
```markdown
# Bug Bounty Program Rules

## In Scope
- *.example.com (production)
- api.example.com
- admin.example.com

## Out of Scope
- staging.example.com
- dev.example.com
- Third-party services

## Prohibited Activities
- DDoS attacks
- Social engineering
- Physical attacks
- Spam or phishing
- Accessing other users' data

## Reporting Requirements
- Detailed vulnerability description
- Steps to reproduce
- Proof of concept
- Impact assessment
```

### Post-Major-Update Testing

**Testing Triggers**:
- Major feature releases
- Infrastructure changes
- Security control updates
- Third-party integration changes

**Testing Process**:
1. Identify changes and potential security impacts
2. Conduct focused penetration testing
3. Validate security controls
4. Test for regression vulnerabilities
5. Document findings and remediation

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Vulnerability remediation work | Prioritization, support |
| Operations Team | Medium | Testing coordination, potential disruption | Scheduling, communication |
| Security Team | High | Testing execution, remediation validation | Training, tools |
| End Users | None | Transparent testing (staging environment) | N/A |
| Compliance Team | Positive | PCI-DSS compliance evidence | Regular reporting |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All applications and services
- Infrastructure and cloud configuration
- Security controls and policies
- Incident response procedures
- Team security awareness

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Service disruption during testing | Low | High | Test in staging, coordinate with ops |
| False sense of security | Medium | Medium | Regular testing, multiple methodologies |
| Vulnerability disclosure | Low | Critical | Responsible disclosure policy, NDA |
| Testing cost overrun | Medium | Low | Fixed-price contracts, budget monitoring |
| Internal team capacity | Medium | Medium | External support, training |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Program Setup (Month 1)

- [ ] Define testing scope and objectives
- [ ] Select external penetration testing vendor
- [ ] Procure testing tools
- [ ] Set up bug bounty program
- [ ] Create testing procedures and runbooks

### Phase 2: Initial Testing (Month 2-3)

- [ ] Conduct first external penetration test
- [ ] Set up automated security scanning
- [ ] Train internal security team
- [ ] Launch bug bounty program
- [ ] Document findings and remediation

### Phase 3: Regular Testing (Month 4-12)

- [ ] Quarterly external penetration testing
- [ ] Semi-annual internal testing
- [ ] Continuous automated scanning
- [ ] Bug bounty program management
- [ ] Remediation tracking

### Phase 4: Red Team Exercise (Month 12)

- [ ] Plan red team exercise
- [ ] Execute attack scenarios
- [ ] Analyze blue team response
- [ ] Document lessons learned
- [ ] Implement improvements

### Rollback Strategy

**Not Applicable** - Testing is non-destructive and conducted in controlled manner

**Safety Measures**:
- Test in staging environment first
- Coordinate with operations team
- Have rollback plan for any changes
- Monitor systems during testing
- Stop testing if issues detected

## Monitoring and Success Criteria

### Success Metrics

- ✅ Quarterly penetration testing: 100% completion
- ✅ Critical vulnerabilities: Remediated within 24 hours
- ✅ High vulnerabilities: Remediated within 7 days
- ✅ Red team exercise: Annual completion
- ✅ Bug bounty submissions: > 10 per year
- ✅ PCI-DSS compliance: 100%
- ✅ Zero successful real attacks

### Monitoring Plan

**Tracking Metrics**:
- `security.testing.completed` (count by type)
- `security.vulnerabilities.found` (count by severity)
- `security.vulnerabilities.remediated` (count by severity)
- `security.remediation.time` (histogram)
- `security.testing.cost` (dollars)

**Reporting**:
- Monthly: Vulnerability remediation status
- Quarterly: Penetration testing results
- Annual: Red team exercise report
- Annual: Security posture assessment

**Review Schedule**:
- Monthly: Vulnerability remediation review
- Quarterly: Testing program effectiveness
- Annual: Program budget and scope review

## Consequences

### Positive Consequences

- ✅ **Proactive Security**: Find vulnerabilities before attackers
- ✅ **Compliance**: Meet PCI-DSS requirement 11.3
- ✅ **Validation**: Validate security controls effectiveness
- ✅ **Improvement**: Continuous security improvement
- ✅ **Readiness**: Improved incident response capabilities
- ✅ **Awareness**: Enhanced team security awareness

### Negative Consequences

- ⚠️ **Cost**: $50,000/year ongoing expense
- ⚠️ **Effort**: Remediation work for development team
- ⚠️ **Coordination**: Requires coordination with multiple teams
- ⚠️ **Potential Disruption**: Risk of service disruption during testing
- ⚠️ **False Positives**: Some findings may not be exploitable

### Technical Debt

**Identified Debt**:
1. Manual testing coordination (acceptable initially)
2. Limited internal testing capability
3. No automated remediation validation
4. Basic bug bounty program

**Debt Repayment Plan**:
- **Q2 2026**: Automate testing coordination and scheduling
- **Q3 2026**: Build internal red team capability
- **Q4 2026**: Implement automated remediation validation
- **2027**: Expand bug bounty program with private researchers

## Related Decisions

- [ADR-053: Security Monitoring and Incident Response](053-security-monitoring-incident-response.md) - Incident response testing
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection testing
- [ADR-055: Vulnerability Management and Patching Strategy](055-vulnerability-management-patching-strategy.md) - Vulnerability remediation
- [ADR-056: Network Segmentation and Isolation Strategy](056-network-segmentation-isolation-strategy.md) - Network security testing

## Notes

### PCI-DSS Requirement 11.3

**Requirement**: Implement a methodology for penetration testing that includes:
- External and internal penetration testing at least annually
- Testing after significant infrastructure or application upgrades
- Segmentation and scope-reduction controls testing
- Application-layer and network-layer penetration tests
- Testing to validate any segmentation and scope-reduction controls

### OWASP Testing Guide

Follow OWASP Testing Guide v4.2 methodology:
1. Information Gathering
2. Configuration and Deployment Management Testing
3. Identity Management Testing
4. Authentication Testing
5. Authorization Testing
6. Session Management Testing
7. Input Validation Testing
8. Error Handling Testing
9. Cryptography Testing
10. Business Logic Testing
11. Client-Side Testing

### Red Team vs Penetration Testing

**Penetration Testing**:
- Focused on finding vulnerabilities
- Time-boxed engagement
- Comprehensive reporting
- Known to blue team

**Red Team Exercise**:
- Focused on achieving objectives
- Extended engagement
- Simulates real attack
- Limited blue team knowledge

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
