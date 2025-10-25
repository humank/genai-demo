---
adr_number: 052
title: "Authentication Security Hardening"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 015, 053]
affected_viewpoints: ["functional", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-052: Authentication Security Hardening

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires comprehensive authentication security hardening to protect against:

- Brute force attacks and credential stuffing
- Weak password vulnerabilities
- Account takeover attempts
- Session hijacking and token theft
- Insider threats and privilege escalation
- Compliance violations (password policies)

Taiwan's cyber security environment presents additional challenges:
- Frequent targeted attacks from state-sponsored actors
- High-value e-commerce platform as attractive target
- Need for defense-in-depth authentication security
- Regulatory compliance requirements (Taiwan Personal Data Protection Act)

### Business Context

**Business Drivers**:
- Protect customer accounts and sensitive data
- Maintain customer trust and platform reputation
- Comply with security regulations and standards
- Prevent financial losses from account compromise
- Support 100K+ user accounts with varying risk profiles

**Constraints**:
- Must balance security with user experience
- Cannot require complex authentication for all users
- Must support legacy password migration
- Budget: $2,000/month for MFA services

### Technical Context

**Current State**:
- JWT-based authentication (ADR-014)
- Basic password hashing with BCrypt
- No multi-factor authentication
- No account lockout mechanism
- No anomalous login detection

**Requirements**:
- Strong password policy enforcement
- Multi-factor authentication (MFA) support
- Account protection mechanisms (lockout, anomaly detection)
- Secure password storage and rotation
- Session timeout and management
- Audit trail for authentication events

## Decision Drivers

1. **Security**: Prevent unauthorized access and account compromise
2. **Compliance**: Meet regulatory requirements (GDPR, Taiwan PDPA)
3. **User Experience**: Balance security with usability
4. **Scalability**: Support 100K+ users without performance degradation
5. **Cost**: Minimize operational costs while maintaining security
6. **Flexibility**: Support different security levels for different user types
7. **Auditability**: Complete audit trail for compliance
8. **Recovery**: Enable secure account recovery mechanisms

## Considered Options

### Option 1: Comprehensive Security Hardening (Recommended)

**Description**: Multi-layered authentication security with password policy, MFA, account protection, and monitoring

**Components**:
- Strong password policy (12+ chars, complexity, history, rotation)
- Multi-factor authentication (TOTP, SMS backup)
- Account lockout (5 failed attempts = 15-min lockout)
- Anomalous login detection (location, device, time)
- Session timeout (30 minutes idle, 8 hours absolute)
- Password storage (BCrypt cost factor 12)
- Mandatory MFA for admin accounts

**Pros**:
- ✅ Defense-in-depth security approach
- ✅ Protects against multiple attack vectors
- ✅ Compliance with security standards
- ✅ Flexible security levels per user type
- ✅ Comprehensive audit trail
- ✅ Industry best practices
- ✅ Supports risk-based authentication

**Cons**:
- ⚠️ Increased implementation complexity
- ⚠️ Higher operational overhead
- ⚠️ Potential user friction (MFA, password requirements)
- ⚠️ SMS costs for MFA backup

**Cost**: $2,000/month (SMS MFA, monitoring tools)

**Risk**: **Low** - Proven security practices

### Option 2: Basic Security with Password Policy Only

**Description**: Minimal security hardening with password policy and basic lockout

**Pros**:
- ✅ Simple to implement
- ✅ Low operational overhead
- ✅ Minimal user friction
- ✅ Low cost

**Cons**:
- ❌ Insufficient protection against sophisticated attacks
- ❌ No MFA protection
- ❌ Limited anomaly detection
- ❌ Compliance gaps
- ❌ Higher risk of account compromise

**Cost**: $0

**Risk**: **High** - Inadequate security for e-commerce platform

### Option 3: Third-Party Authentication Service (Auth0, Okta)

**Description**: Delegate authentication security to managed service

**Pros**:
- ✅ Managed security features
- ✅ Professional support
- ✅ Advanced features (adaptive MFA, risk scoring)
- ✅ Compliance certifications

**Cons**:
- ❌ High cost ($3,000-5,000/month)
- ❌ Vendor lock-in
- ❌ Less control over security policies
- ❌ Data privacy concerns
- ❌ External dependency

**Cost**: $4,000/month

**Risk**: **Medium** - Vendor dependency

## Decision Outcome

**Chosen Option**: **Comprehensive Security Hardening (Option 1)**

### Rationale

Comprehensive security hardening was selected for the following reasons:

1. **Defense-in-Depth**: Multiple security layers protect against various attack vectors
2. **Compliance**: Meets regulatory requirements for password security and MFA
3. **Risk Mitigation**: Taiwan's cyber threat environment requires robust authentication security
4. **Cost-Effective**: $2,000/month is reasonable for 100K+ users
5. **Flexibility**: Risk-based authentication adapts to user behavior
6. **Control**: Full control over security policies and implementation
7. **Scalability**: Designed to scale with user growth

### Password Policy

**Requirements**:
- Minimum 12 characters (industry best practice)
- Complexity requirements:
  - At least 1 uppercase letter
  - At least 1 lowercase letter
  - At least 1 number
  - At least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
- Password history: No reuse of last 5 passwords
- Mandatory password change every 90 days
- No common passwords (check against breach database)
- No personal information in password (name, email, birthdate)

**Implementation**:
```java
public class PasswordPolicy {
    private static final int MIN_LENGTH = 12;
    private static final int PASSWORD_HISTORY_SIZE = 5;
    private static final int PASSWORD_EXPIRY_DAYS = 90;
    
    public ValidationResult validate(String password, User user) {
        // Length check
        if (password.length() < MIN_LENGTH) {
            return ValidationResult.fail("Password must be at least 12 characters");
        }
        
        // Complexity checks
        if (!hasUppercase(password)) {
            return ValidationResult.fail("Password must contain uppercase letter");
        }
        if (!hasLowercase(password)) {
            return ValidationResult.fail("Password must contain lowercase letter");
        }
        if (!hasDigit(password)) {
            return ValidationResult.fail("Password must contain number");
        }
        if (!hasSpecialChar(password)) {
            return ValidationResult.fail("Password must contain special character");
        }
        
        // History check
        if (isInPasswordHistory(password, user)) {
            return ValidationResult.fail("Cannot reuse last 5 passwords");
        }
        
        // Common password check
        if (isCommonPassword(password)) {
            return ValidationResult.fail("Password is too common");
        }
        
        return ValidationResult.success();
    }
}
```

### Multi-Factor Authentication (MFA)

**Primary Method**: Time-based One-Time Password (TOTP)
- Standard: RFC 6238
- Apps: Google Authenticator, Authy, Microsoft Authenticator
- Code validity: 30 seconds
- Code length: 6 digits

**Backup Method**: SMS One-Time Password
- Used when TOTP unavailable
- Code validity: 5 minutes
- Rate limited: 3 codes per hour
- Cost: $0.01 per SMS

**MFA Requirements**:
- Mandatory for admin accounts
- Optional but recommended for customer accounts
- Mandatory for high-value transactions (> $1,000)
- Mandatory after password reset
- Remember device for 30 days (optional)

**Implementation**:
```java
@Service
public class MfaService {
    
    public boolean verifyTotp(String userId, String code) {
        String secret = getUserTotpSecret(userId);
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        
        // Check current code and adjacent time windows (±1)
        long currentTime = System.currentTimeMillis() / 30000;
        for (long time = currentTime - 1; time <= currentTime + 1; time++) {
            String expectedCode = totp.generateOneTimePasswordString(secret, time);
            if (code.equals(expectedCode)) {
                return true;
            }
        }
        return false;
    }
    
    public void sendSmsCode(String userId, String phoneNumber) {
        // Rate limiting check
        if (hasExceededSmsRateLimit(userId)) {
            throw new RateLimitExceededException("Too many SMS requests");
        }
        
        // Generate 6-digit code
        String code = generateRandomCode(6);
        
        // Store code with 5-minute expiration
        storeVerificationCode(userId, code, Duration.ofMinutes(5));
        
        // Send SMS
        smsService.send(phoneNumber, "Your verification code: " + code);
    }
}
```

### Account Protection

**Account Lockout**:
- Trigger: 5 failed login attempts within 15 minutes
- Lockout duration: 15 minutes
- Unlock methods: Time expiration, email verification, admin unlock
- Notification: Email sent to user on lockout

**Anomalous Login Detection**:
- Location-based: Login from new country/city
- Device-based: Login from new device/browser
- Time-based: Login at unusual time (3 AM local time)
- Velocity-based: Multiple logins from different locations
- Action: Require MFA verification, send notification email

**Session Management**:
- Idle timeout: 30 minutes of inactivity
- Absolute timeout: 8 hours maximum session
- Concurrent sessions: Maximum 3 active sessions per user
- Session termination: Logout all sessions on password change

**Implementation**:
```java
@Service
public class AccountProtectionService {
    
    public void recordFailedLogin(String userId, String ipAddress) {
        FailedLoginAttempt attempt = new FailedLoginAttempt(userId, ipAddress, Instant.now());
        failedLoginRepository.save(attempt);
        
        // Check if account should be locked
        long recentFailures = countRecentFailures(userId, Duration.ofMinutes(15));
        if (recentFailures >= 5) {
            lockAccount(userId, Duration.ofMinutes(15));
            notificationService.sendAccountLockoutEmail(userId);
        }
    }
    
    public boolean isAnomalousLogin(String userId, LoginContext context) {
        UserLoginHistory history = getUserLoginHistory(userId);
        
        // Check location
        if (!history.hasLoginFromCountry(context.getCountry())) {
            return true; // New country
        }
        
        // Check device
        if (!history.hasLoginFromDevice(context.getDeviceFingerprint())) {
            return true; // New device
        }
        
        // Check time
        if (isUnusualLoginTime(context.getLocalTime())) {
            return true; // Unusual time (3-6 AM)
        }
        
        // Check velocity
        if (hasRecentLoginFromDifferentLocation(userId, context, Duration.ofMinutes(30))) {
            return true; // Impossible travel
        }
        
        return false;
    }
}
```

### Password Storage

**Hashing Algorithm**: BCrypt with cost factor 12
- Industry standard for password hashing
- Adaptive cost factor (increases with hardware improvements)
- Built-in salt generation
- Resistant to rainbow table attacks

**Alternative**: Argon2 (future consideration)
- Winner of Password Hashing Competition (2015)
- Memory-hard algorithm (resistant to GPU attacks)
- Configurable memory, time, and parallelism parameters

**Implementation**:
```java
@Service
public class PasswordService {
    
    private static final int BCRYPT_COST_FACTOR = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST_FACTOR);
    
    public String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }
    
    public void rotatePassword(String userId, String newPassword) {
        // Validate new password
        ValidationResult validation = passwordPolicy.validate(newPassword, getUser(userId));
        if (!validation.isValid()) {
            throw new InvalidPasswordException(validation.getMessage());
        }
        
        // Hash new password
        String hashedPassword = hashPassword(newPassword);
        
        // Update password and history
        updateUserPassword(userId, hashedPassword);
        addToPasswordHistory(userId, hashedPassword);
        
        // Invalidate all sessions
        sessionService.invalidateAllSessions(userId);
        
        // Send notification
        notificationService.sendPasswordChangedEmail(userId);
    }
}
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| End Users | Medium | Stronger password requirements, MFA setup | Clear instructions, gradual rollout |
| Admin Users | High | Mandatory MFA, stricter policies | Training, support documentation |
| Development Team | High | Implement security features | Training, code examples, testing tools |
| Operations Team | Medium | Monitor security events, handle lockouts | Runbooks, automated alerts |
| Security Team | Positive | Enhanced security posture | Regular audits, penetration testing |
| Customer Support | Medium | Handle lockout and MFA issues | Training, support scripts |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All authentication endpoints
- User registration and login flows
- Password reset functionality
- Session management
- Admin interfaces
- Mobile applications
- API authentication

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| User friction from MFA | High | Medium | Optional for customers, clear benefits communication |
| Account lockout false positives | Medium | Medium | Email unlock, admin override, reasonable thresholds |
| SMS delivery failures | Medium | Low | TOTP as primary, SMS as backup, retry mechanism |
| Password policy too strict | Medium | Low | User education, password strength meter |
| Implementation bugs | Low | High | Comprehensive testing, gradual rollout, monitoring |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Password Policy (Week 1-2)

- [ ] Implement password validation service
- [ ] Add password complexity checks
- [ ] Implement password history tracking
- [ ] Add password expiration mechanism
- [ ] Create password strength meter (frontend)
- [ ] Add common password blacklist
- [ ] Implement password rotation reminders

### Phase 2: Account Protection (Week 3-4)

- [ ] Implement failed login tracking
- [ ] Add account lockout mechanism
- [ ] Create anomalous login detection
- [ ] Implement session timeout
- [ ] Add concurrent session management
- [ ] Create account lockout notifications
- [ ] Add admin unlock functionality

### Phase 3: Multi-Factor Authentication (Week 5-6)

- [ ] Implement TOTP generation and verification
- [ ] Add MFA enrollment flow
- [ ] Implement SMS OTP backup
- [ ] Add device remember functionality
- [ ] Create MFA recovery codes
- [ ] Implement MFA enforcement for admins
- [ ] Add MFA status to user profile

### Phase 4: Integration and Testing (Week 7-8)

- [ ] Integrate with existing authentication (ADR-014)
- [ ] Update frontend for password policy
- [ ] Add MFA setup UI
- [ ] Security testing (penetration testing)
- [ ] Load testing (account lockout scenarios)
- [ ] User acceptance testing
- [ ] Documentation and training

### Rollback Strategy

**Trigger Conditions**:
- Critical security vulnerability in implementation
- Excessive user lockouts (> 5% of login attempts)
- MFA service failures (> 1% failure rate)
- Performance degradation (> 100ms authentication overhead)

**Rollback Steps**:
1. Disable MFA enforcement (make optional)
2. Relax password policy temporarily
3. Disable account lockout
4. Investigate and fix issues
5. Gradually re-enable features

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Password policy compliance: 100% of new passwords
- ✅ MFA adoption: > 30% of customers, 100% of admins
- ✅ Account lockout rate: < 1% of login attempts
- ✅ False positive lockouts: < 0.1%
- ✅ Anomalous login detection: > 95% accuracy
- ✅ Authentication overhead: < 50ms
- ✅ Zero account compromise incidents

### Monitoring Plan

**CloudWatch Metrics**:
- `auth.password.validation.failure` (count by reason)
- `auth.account.lockout` (count)
- `auth.mfa.enrollment` (count)
- `auth.mfa.verification.success` (count)
- `auth.mfa.verification.failure` (count)
- `auth.anomalous.login.detected` (count)
- `auth.session.timeout` (count)

**Alerts**:
- Account lockout rate > 2% for 10 minutes
- MFA verification failure rate > 5% for 10 minutes
- Anomalous login detection spike (> 100 in 5 minutes)
- Password policy bypass attempts
- Suspicious authentication patterns

**Security Monitoring**:
- Failed login attempts per IP/user
- Account lockout patterns
- MFA bypass attempts
- Password reset abuse
- Session hijacking attempts

**Review Schedule**:
- Daily: Check authentication security metrics
- Weekly: Review anomalous login detections
- Monthly: Password policy effectiveness review
- Quarterly: Security audit and penetration testing

## Consequences

### Positive Consequences

- ✅ **Enhanced Security**: Multi-layered protection against account compromise
- ✅ **Compliance**: Meets regulatory password and MFA requirements
- ✅ **Auditability**: Complete audit trail for authentication events
- ✅ **Flexibility**: Risk-based authentication adapts to user behavior
- ✅ **User Protection**: Proactive detection and prevention of account takeover
- ✅ **Reputation**: Demonstrates commitment to security
- ✅ **Cost-Effective**: $2,000/month for comprehensive security

### Negative Consequences

- ⚠️ **User Friction**: Stronger password requirements may frustrate some users
- ⚠️ **Support Overhead**: Increased support requests for lockouts and MFA issues
- ⚠️ **Implementation Complexity**: Multiple security features to implement and maintain
- ⚠️ **Operational Overhead**: Monitoring and responding to security events
- ⚠️ **SMS Costs**: $0.01 per SMS for MFA backup

### Technical Debt

**Identified Debt**:
1. BCrypt cost factor may need increase in future (currently 12)
2. SMS MFA is less secure than TOTP (acceptable as backup)
3. No biometric authentication support (future enhancement)
4. No adaptive authentication (risk scoring)

**Debt Repayment Plan**:
- **Q2 2026**: Implement adaptive authentication with risk scoring
- **Q3 2026**: Add biometric authentication support (WebAuthn)
- **Q4 2026**: Migrate to Argon2 for password hashing
- **2027**: Implement passwordless authentication options

## Related Decisions

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Base authentication mechanism
- [ADR-015: Role-Based Access Control (RBAC) Implementation](015-role-based-access-control-implementation.md) - Authorization integration
- [ADR-053: Security Monitoring and Incident Response](053-security-monitoring-incident-response.md) - Security monitoring integration
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection integration

## Notes

### Password Policy Configuration

```yaml
# application.yml
security:
  password:
    min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special-char: true
    history-size: 5
    expiry-days: 90
    common-password-check: true
```

### MFA Configuration

```yaml
# application.yml
security:
  mfa:
    totp:
      enabled: true
      issuer: "E-Commerce Platform"
      period: 30
      digits: 6
    sms:
      enabled: true
      validity-minutes: 5
      rate-limit: 3
      cost-per-sms: 0.01
    enforcement:
      admin-mandatory: true
      customer-optional: true
      high-value-transaction: true
```

### Account Protection Configuration

```yaml
# application.yml
security:
  account-protection:
    lockout:
      max-attempts: 5
      window-minutes: 15
      duration-minutes: 15
    session:
      idle-timeout-minutes: 30
      absolute-timeout-hours: 8
      max-concurrent-sessions: 3
    anomaly-detection:
      enabled: true
      check-location: true
      check-device: true
      check-time: true
      check-velocity: true
```

### Gradual Rollout Plan

**Phase 1 (Week 1-2)**: Password policy for new users
**Phase 2 (Week 3-4)**: Password policy for existing users (grace period)
**Phase 3 (Week 5-6)**: Account lockout enabled
**Phase 4 (Week 7-8)**: MFA optional for customers
**Phase 5 (Week 9-10)**: MFA mandatory for admins
**Phase 6 (Week 11-12)**: Anomalous login detection enabled

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
