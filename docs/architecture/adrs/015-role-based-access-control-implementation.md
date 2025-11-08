---
adr_number: 015
title: "Role-Based Access Control (RBAC) Implementation"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 054]
affected_viewpoints: ["functional", "development"]
affected_perspectives: ["security", "evolution"]
---

# ADR-015: Role-Based Access Control (RBAC) Implementation

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a flexible, scalable authorization system that can:

- Control access to resources based on user roles
- Support fine-grained permissions for different operations
- Enable hierarchical role structures
- Allow dynamic permission assignment without code changes
- Support multi-tenant scenarios (customers, sellers, admins)
- Integrate seamlessly with JWT authentication
- Provide audit trails for authorization decisions

### Business Context

**Business Drivers**:

- Multiple user types with different access levels (customers, sellers, admins, support)
- Need for granular control over sensitive operations (refunds, price changes)
- Regulatory compliance (data access controls, audit trails)
- Business flexibility (add new roles without code deployment)
- Expected growth from 10K to 1M+ users

**Constraints**:

- Must integrate with existing JWT authentication (ADR-014)
- Performance: Authorization check < 5ms
- Must support role hierarchy (admin inherits all permissions)
- Budget: No additional licensing costs

### Technical Context

**Current State**:

- Spring Boot 3.4.5 with Spring Security
- JWT-based authentication implemented (ADR-014)
- Microservices architecture
- PostgreSQL database

**Requirements**:

- Role-based access control with permissions
- Support for role hierarchy
- Dynamic role and permission management
- Method-level security annotations
- URL-based access control
- Audit logging for authorization decisions

## Decision Drivers

1. **Flexibility**: Easy to add new roles and permissions
2. **Performance**: Fast authorization checks (< 5ms)
3. **Granularity**: Support both coarse and fine-grained permissions
4. **Maintainability**: Centralized permission management
5. **Security**: Principle of least privilege
6. **Scalability**: Support millions of users
7. **Standards**: Use industry-standard patterns
8. **Cost**: No licensing fees

## Considered Options

### Option 1: RBAC with Permission-Based Model

**Description**: Roles contain permissions, permissions grant access to resources

**Pros**:

- ✅ Flexible and granular control
- ✅ Easy to understand and maintain
- ✅ Supports role hierarchy
- ✅ Dynamic permission assignment
- ✅ Excellent Spring Security integration
- ✅ Scales well (permissions in JWT)
- ✅ Industry standard pattern
- ✅ No additional costs

**Cons**:

- ⚠️ Requires careful permission design
- ⚠️ JWT token size increases with permissions
- ⚠️ Permission changes require token refresh

**Cost**: $0 (built into Spring Security)

**Risk**: **Low** - Proven pattern

### Option 2: Attribute-Based Access Control (ABAC)

**Description**: Access based on attributes (user, resource, environment)

**Pros**:

- ✅ Very flexible and dynamic
- ✅ Context-aware decisions
- ✅ Fine-grained control

**Cons**:

- ❌ Complex to implement and maintain
- ❌ Performance overhead (policy evaluation)
- ❌ Harder to understand and debug
- ❌ Requires policy engine
- ❌ Overkill for our requirements

**Cost**: $2,000/month (policy engine like OPA)

**Risk**: **High** - Complexity, performance

### Option 3: Simple Role-Only Model

**Description**: Access control based only on roles, no separate permissions

**Pros**:

- ✅ Very simple to implement
- ✅ Fast performance
- ✅ Easy to understand

**Cons**:

- ❌ Not flexible enough
- ❌ Role explosion (need many roles)
- ❌ Hard to maintain
- ❌ Cannot handle fine-grained permissions

**Cost**: $0

**Risk**: **Medium** - Inflexibility

### Option 4: External Authorization Service (Authz)

**Description**: Delegate authorization to external service

**Pros**:

- ✅ Centralized authorization
- ✅ Advanced features

**Cons**:

- ❌ Network latency on every request
- ❌ Additional infrastructure
- ❌ Single point of failure
- ❌ Licensing costs

**Cost**: $1,000/month

**Risk**: **Medium** - Dependency, latency

## Decision Outcome

**Chosen Option**: **RBAC with Permission-Based Model**

### Rationale

RBAC with permissions was selected for the following reasons:

1. **Flexibility**: Roles can be composed of permissions, easy to add new permissions
2. **Performance**: Permissions included in JWT, no database lookup needed
3. **Granularity**: Supports both coarse (roles) and fine-grained (permissions) control
4. **Standards-Based**: Industry-standard pattern, well-understood
5. **Spring Security**: Excellent integration with @PreAuthorize, @Secured
6. **Scalability**: Permissions cached in JWT, scales horizontally
7. **Cost-Effective**: No additional licensing or infrastructure
8. **Maintainability**: Clear separation of roles and permissions

**Authorization Model**:

```mermaid
graph LR
    N1["User"]
    N2["Roles"]
    N1 --> N2
    N3["Permissions"]
    N2 --> N3
    N4["Resources"]
    N3 --> N4
```

**Role Hierarchy**:

```text
SUPER_ADMIN (all permissions)
  ├── ADMIN (manage users, orders, products)
  ├── SELLER (manage own products, orders)
  └── CUSTOMER (create orders, manage profile)
      └── GUEST (browse products only)
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to implement RBAC annotations | Training, code examples, documentation |
| Security Team | Positive | Fine-grained access control | Regular permission audits |
| Operations Team | Low | Monitor authorization failures | Dashboards, alerts |
| End Users | None | Transparent to users | N/A |
| Business | Positive | Flexible role management | Admin UI for role management |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All API endpoints (authorization required)
- All microservices (permission checks)
- Database schema (roles and permissions tables)
- JWT tokens (include permissions)
- Admin UI (role management)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Permission explosion | Medium | Medium | Regular permission review, consolidation |
| JWT token size | Medium | Low | Limit permissions per role, use permission groups |
| Permission changes delay | Medium | Low | Short token expiration (15 min), force refresh for critical changes |
| Misconfigured permissions | Low | High | Automated tests, permission audits, principle of least privilege |
| Performance degradation | Low | Medium | Cache permission checks, optimize queries |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Database Schema and Core Model (Week 1)

- [x] Create `roles` table (id, name, description, hierarchy_level)
- [x] Create `permissions` table (id, name, resource, action, description)
- [x] Create `role_permissions` junction table
- [x] Create `user_roles` junction table
- [x] Implement Role and Permission entities
- [x] Implement RoleRepository and PermissionRepository
- [x] Seed initial roles and permissions

### Phase 2: Spring Security Integration (Week 2)

- [x] Implement custom UserDetailsService with roles/permissions
- [x] Include permissions in JWT claims
- [x] Implement permission-based authorization
- [x] Add @PreAuthorize annotations to endpoints
- [x] Implement custom PermissionEvaluator
- [x] Add method-level security

### Phase 3: Role Management API (Week 3)

- [x] Implement Role CRUD endpoints (admin only)
- [x] Implement Permission CRUD endpoints (admin only)
- [x] Implement assign/revoke role to user
- [x] Implement assign/revoke permission to role
- [x] Add role hierarchy management
- [x] Add audit logging for role changes

### Phase 4: Testing and Documentation (Week 4)

- [x] Unit tests for authorization logic
- [x] Integration tests for RBAC
- [x] Security tests (unauthorized access attempts)
- [x] Performance tests (authorization overhead)
- [x] Documentation and examples
- [x] Admin UI for role management

### Rollback Strategy

**Trigger Conditions**:

- Authorization failures > 5%
- Performance degradation > 10ms per request
- Security vulnerabilities discovered
- Data corruption in roles/permissions

**Rollback Steps**:

1. Revert to simple role-only authorization
2. Investigate and fix RBAC implementation
3. Re-deploy with fixes
4. Gradually re-enable permission-based authorization

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Authorization check latency < 5ms (95th percentile)
- ✅ Zero unauthorized access incidents
- ✅ Authorization failure rate < 0.1% (excluding legitimate denials)
- ✅ Role management operations < 100ms
- ✅ Permission audit trail 100% complete

### Monitoring Plan

**CloudWatch Metrics**:

- `authz.check.time` (histogram)
- `authz.denied` (count, by permission)
- `authz.granted` (count, by permission)
- `authz.error` (count)
- `role.assigned` (count)
- `role.revoked` (count)

**Alerts**:

- Authorization check latency > 10ms for 5 minutes
- Authorization error rate > 1% for 5 minutes
- Suspicious authorization patterns (privilege escalation attempts)
- Role changes outside business hours

**Security Monitoring**:

- Failed authorization attempts per user
- Permission usage patterns
- Role assignment changes
- Privilege escalation attempts

**Review Schedule**:

- Daily: Check authorization metrics
- Weekly: Review denied access logs
- Monthly: Permission audit and cleanup
- Quarterly: Role hierarchy review

## Consequences

### Positive Consequences

- ✅ **Flexible Access Control**: Easy to add new roles and permissions
- ✅ **Fine-Grained Security**: Control access at resource and action level
- ✅ **Performance**: Fast authorization (< 5ms)
- ✅ **Scalability**: Permissions in JWT, no database lookup
- ✅ **Maintainability**: Clear separation of roles and permissions
- ✅ **Audit Trail**: Complete history of authorization decisions
- ✅ **Standards-Based**: Industry-standard RBAC pattern
- ✅ **Cost-Effective**: No additional licensing

### Negative Consequences

- ⚠️ **JWT Token Size**: Permissions increase token size (mitigated by permission groups)
- ⚠️ **Permission Management**: Requires careful design and maintenance
- ⚠️ **Permission Changes**: Require token refresh (15-min delay)
- ⚠️ **Complexity**: More complex than simple role-only model

### Technical Debt

**Identified Debt**:

1. No permission groups implemented (acceptable initially)
2. Manual permission audit process (acceptable for now)
3. No dynamic permission loading (acceptable with short token expiration)

**Debt Repayment Plan**:

- **Q2 2026**: Implement permission groups to reduce JWT size
- **Q3 2026**: Automate permission audit and cleanup
- **Q4 2026**: Implement dynamic permission loading for long-lived sessions

## Related Decisions

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Authentication integration
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Access control integration
- [ADR-009: RESTful API Design with OpenAPI 3.0](009-restful-api-design-with-openapi.md) - API authorization

## Notes

### Permission Naming Convention

Format: `{resource}:{action}:{scope}`

Examples:

- `order:create:any` - Create any order
- `order:read:own` - Read own orders only
- `order:update:any` - Update any order
- `order:delete:own` - Delete own orders only
- `product:create:own` - Create own products (sellers)
- `user:manage:any` - Manage any user (admins)

### Role Definitions

```yaml
roles:
  SUPER_ADMIN:
    description: "System administrator with all permissions"
    permissions: ["*:*:*"]
    hierarchy_level: 100
    
  ADMIN:
    description: "Platform administrator"
    permissions:

      - "user:*:any"
      - "order:*:any"
      - "product:*:any"
      - "report:read:any"

    hierarchy_level: 90
    
  SELLER:
    description: "Product seller"
    permissions:

      - "product:create:own"
      - "product:update:own"
      - "product:read:any"
      - "order:read:own"
      - "order:update:own"

    hierarchy_level: 50
    
  CUSTOMER:
    description: "Regular customer"
    permissions:

      - "order:create:own"
      - "order:read:own"
      - "order:cancel:own"
      - "profile:update:own"
      - "product:read:any"

    hierarchy_level: 10
    
  GUEST:
    description: "Unauthenticated user"
    permissions:

      - "product:read:any"

    hierarchy_level: 0
```

### Spring Security Configuration

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }
}

// Usage in controllers
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    @PreAuthorize("hasPermission(null, 'order:create:own')")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        // Implementation
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Order', 'read')")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        // Implementation
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Order', 'update')")
    public ResponseEntity<Order> updateOrder(@PathVariable String id, @RequestBody UpdateOrderRequest request) {
        // Implementation
    }
}
```

### Database Schema

```sql
CREATE TABLE roles (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    hierarchy_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id VARCHAR(50) NOT NULL,
    permission_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id VARCHAR(50) NOT NULL,
    role_id VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(50),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
