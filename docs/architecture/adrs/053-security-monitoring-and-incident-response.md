# ADR-053: Security Monitoring and Incident Response

**Status**: Accepted  
**Date**: 2024-11-19  
**Decision Makers**: Security Team, Operations Team

---

## Context

The GenAI Demo platform requires comprehensive security monitoring and incident response capabilities to detect, respond to, and recover from security incidents.

### Business Context

- **Compliance**: PCI DSS, GDPR, SOC 2 requirements
- **Customer Trust**: Protect customer data and privacy
- **Business Continuity**: Minimize impact of security incidents
- **Regulatory**: Meet security audit requirements

### Technical Context

- **Current State**: Basic CloudWatch logging
- **Target State**: Comprehensive security monitoring
- **Constraints**: AWS-based infrastructure
- **Scale**: 10,000+ requests/minute

---

## Decision Drivers

1. **Detection Speed**: Identify threats in real-time
2. **Response Time**: Minimize incident response time
3. **Compliance**: Meet regulatory requirements
4. **Cost**: Balance security with budget
5. **Integration**: Work with existing AWS services

---

## Considered Options

### Option 1: AWS Native Security Services

**Components**:
- AWS GuardDuty (threat detection)
- AWS Security Hub (centralized security)
- AWS CloudTrail (audit logging)
- Amazon Detective (investigation)

**Pros**:
- Native AWS integration
- Managed services
- Automatic updates
- Pay-as-you-go pricing

**Cons**:
- Limited customization
- AWS-specific
- Multiple services to manage

**Cost**: ~$500/month  
**Risk**: Low

### Option 2: Third-Party SIEM (Splunk/Datadog)

**Pros**:
- Advanced analytics
- Custom dashboards
- Multi-cloud support
- Rich ecosystem

**Cons**:
- High cost
- Complex setup
- Vendor lock-in

**Cost**: ~$5,000/month  
**Risk**: Medium

### Option 3: Open Source (ELK Stack + Wazuh)

**Pros**:
- Full control
- No licensing costs
- Customizable

**Cons**:
- High operational overhead
- Self-managed
- Requires expertise

**Cost**: Infrastructure only (~$1,000/month)  
**Risk**: High

---

## Decision Outcome

**Chosen Option**: AWS Native Security Services (Option 1) + Custom Monitoring

### Rationale

1. **Cost-Effective**: Best balance of features and cost
2. **AWS Integration**: Seamless integration with existing infrastructure
3. **Managed Services**: Reduced operational overhead
4. **Compliance**: Built-in compliance reporting
5. **Scalability**: Automatic scaling with usage

---

## Implementation

### Security Monitoring Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Security Monitoring                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  GuardDuty   │  │Security Hub  │  │  CloudTrail  │ │
│  │   Threats    │  │  Findings    │  │  Audit Logs  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                  │                  │          │
│         └──────────────────┼──────────────────┘          │
│                            │                             │
│                    ┌───────▼────────┐                   │
│                    │  EventBridge   │                   │
│                    │   Rules        │                   │
│                    └───────┬────────┘                   │
│                            │                             │
│         ┌──────────────────┼──────────────────┐         │
│         │                  │                  │         │
│    ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐  │
│    │  SNS    │      │  Lambda   │     │ Security  │  │
│    │ Alerts  │      │ Response  │     │   Team    │  │
│    └─────────┘      └───────────┘     └───────────┘  │
└─────────────────────────────────────────────────────────┘
```

### GuardDuty Configuration

```typescript
// infrastructure/src/constructs/security-monitoring.ts
import * as guardduty from 'aws-cdk-lib/aws-guardduty';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';

export class SecurityMonitoring extends Construct {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    // Enable GuardDuty
    const detector = new guardduty.CfnDetector(this, 'Detector', {
      enable: true,
      findingPublishingFrequency: 'FIFTEEN_MINUTES',
    });

    // Create EventBridge rule for high severity findings
    const rule = new events.Rule(this, 'HighSeverityFindings', {
      eventPattern: {
        source: ['aws.guardduty'],
        detailType: ['GuardDuty Finding'],
        detail: {
          severity: [7, 8, 9], // High and Critical
        },
      },
    });

    // Send alerts to SNS
    rule.addTarget(new targets.SnsTopic(alertTopic));

    // Trigger automated response
    rule.addTarget(new targets.LambdaFunction(responseFunction));
  }
}
```

### Security Hub Configuration

```typescript
// Enable Security Hub
const securityHub = new securityhub.CfnHub(this, 'SecurityHub', {
  enableDefaultStandards: true,
  controlFindingGenerator: 'SECURITY_CONTROL',
});

// Enable standards
new securityhub.CfnStandard(this, 'CISStandard', {
  standardsArn: 'arn:aws:securityhub:region::standards/cis-aws-foundations-benchmark/v/1.4.0',
});

new securityhub.CfnStandard(this, 'PCIDSSStandard', {
  standardsArn: 'arn:aws:securityhub:region::standards/pci-dss/v/3.2.1',
});
```

### Automated Incident Response

```java
@Component
public class SecurityIncidentHandler {
    
    @EventListener
    public void handleSecurityFinding(SecurityFindingEvent event) {
        SecurityFinding finding = event.getFinding();
        
        // Log incident
        securityLogger.error("Security finding detected",
            kv("findingId", finding.getId()),
            kv("severity", finding.getSeverity()),
            kv("type", finding.getType()),
            kv("resource", finding.getResource())
        );
        
        // Automated response based on severity
        switch (finding.getSeverity()) {
            case CRITICAL -> handleCriticalFinding(finding);
            case HIGH -> handleHighFinding(finding);
            case MEDIUM -> handleMediumFinding(finding);
            default -> logFinding(finding);
        }
    }
    
    private void handleCriticalFinding(SecurityFinding finding) {
        // 1. Immediate notification
        notificationService.sendCriticalAlert(finding);
        
        // 2. Isolate affected resources
        if (finding.getType().equals("UnauthorizedAccess")) {
            isolateResource(finding.getResource());
        }
        
        // 3. Create incident ticket
        incidentService.createIncident(finding, Priority.CRITICAL);
        
        // 4. Trigger incident response runbook
        runbookService.execute("security-incident-response", finding);
    }
}
```

---

## Incident Response Process

### Detection

```java
@Component
public class ThreatDetectionService {
    
    // Real-time threat detection
    @Scheduled(fixedRate = 60000) // Every minute
    public void detectThreats() {
        // Check for suspicious patterns
        List<SecurityEvent> suspiciousEvents = securityEventRepository
            .findSuspiciousEvents(Instant.now().minus(1, ChronoUnit.MINUTES));
        
        for (SecurityEvent event : suspiciousEvents) {
            analyzeThreat(event);
        }
    }
    
    private void analyzeThreat(SecurityEvent event) {
        ThreatScore score = calculateThreatScore(event);
        
        if (score.isHigh()) {
            triggerIncidentResponse(event, score);
        }
    }
}
```

### Response Automation

```java
@Component
public class AutomatedResponseService {
    
    public void respondToThreat(SecurityFinding finding) {
        ResponseAction action = determineResponseAction(finding);
        
        switch (action) {
            case BLOCK_IP -> blockIpAddress(finding.getSourceIp());
            case REVOKE_CREDENTIALS -> revokeCredentials(finding.getUserId());
            case ISOLATE_INSTANCE -> isolateInstance(finding.getInstanceId());
            case ALERT_ONLY -> sendAlert(finding);
        }
        
        // Log response action
        auditLogger.info("Automated response executed",
            kv("findingId", finding.getId()),
            kv("action", action),
            kv("timestamp", Instant.now())
        );
    }
}
```

---

## Monitoring Dashboards

### Security Metrics

```java
@Component
public class SecurityMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordSecurityEvent(SecurityEvent event) {
        Counter.builder("security.events")
            .tag("type", event.getType())
            .tag("severity", event.getSeverity())
            .tag("source", event.getSource())
            .register(meterRegistry)
            .increment();
    }
    
    public void recordIncidentResponse(String action, Duration responseTime) {
        Timer.builder("security.incident.response.time")
            .tag("action", action)
            .register(meterRegistry)
            .record(responseTime);
    }
}
```

### Alert Rules

```yaml
# Prometheus Alert Rules
groups:
  - name: security_alerts
    rules:
      - alert: HighSecurityEventRate
        expr: rate(security_events_total{severity="high"}[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High rate of security events"
          
      - alert: UnauthorizedAccessAttempt
        expr: security_events_total{type="unauthorized_access"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Unauthorized access attempt detected"
          
      - alert: SlowIncidentResponse
        expr: security_incident_response_time_seconds > 300
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Incident response time exceeds 5 minutes"
```

---

## Success Metrics

- **Detection Time**: < 5 minutes for critical threats
- **Response Time**: < 15 minutes for critical incidents
- **False Positive Rate**: < 5%
- **Incident Resolution**: < 4 hours for critical incidents
- **Compliance**: 100% audit trail coverage

---

## Related Documentation

- [Security Overview](../../perspectives/security/overview.md)
- [Security Standards](.kiro/steering/security-standards.md)
- [Incident Response Runbook](../../operations/runbooks/security-incident-response.md)
- [Monitoring Guide](../../operations/monitoring/README.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Security Team  
**Reviewers**: Operations Team, Compliance Team
