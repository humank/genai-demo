---
adr_number: 024
title: "Monorepo vs Multi-Repo Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [2, 7, 18, 19]
affected_viewpoints: ["development", "deployment"]
affected_perspectives: ["evolution", "development-resource"]
decision_makers: ["Architecture Team", "Development Team", "DevOps Team"]
---

# ADR-024: Monorepo vs Multi-Repo Strategy

## Status

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Development Team, DevOps Team

## Context

### Problem Statement

The Enterprise E-Commerce Platform consists of multiple components (backend services, frontend applications, infrastructure code, shared libraries) that need to be organized in a version control repository structure. We need to decide between:

- **Monorepo**: Single repository containing all code
- **Multi-Repo**: Separate repositories for each component
- **Hybrid**: Combination of both approaches

This decision impacts:

- Code sharing and reusability
- Build and deployment pipelines
- Team collaboration and ownership
- Dependency management
- Release coordination
- Developer experience

### Business Context

**Business Drivers**:

- Fast feature delivery requiring cross-component changes
- Code reuse across multiple services and applications
- Consistent coding standards and tooling
- Simplified dependency management
- Atomic commits across multiple components
- Reduced context switching for developers

**Business Constraints**:

- Team size: 15-20 developers across multiple teams
- Multiple programming languages (Java, TypeScript, Python)
- Multiple deployment targets (backend services, frontends, infrastructure)
- Need for independent service deployment
- CI/CD pipeline performance requirements

**Business Requirements**:

- Support rapid feature development
- Enable code sharing across components
- Maintain clear ownership boundaries
- Support independent service scaling
- Enable efficient code reviews

### Technical Context

**Current Architecture**:

- Backend: Spring Boot microservices (Java 21)
- Frontend: Next.js (CMC), Angular (Consumer)
- Infrastructure: AWS CDK (TypeScript)
- Shared libraries: Domain models, utilities
- 13 bounded contexts with potential for independent services

**Technical Constraints**:

- Multiple build tools (Gradle, npm, Maven)
- Multiple deployment pipelines
- Need for selective builds (only changed components)
- Git repository size and performance
- CI/CD pipeline execution time
- Developer machine performance

**Dependencies**:

- ADR-002: Hexagonal Architecture (affects code organization)
- ADR-007: AWS CDK for Infrastructure (infrastructure code location)
- ADR-018: Container Orchestration with EKS (deployment strategy)
- ADR-019: Progressive Deployment Strategy (deployment independence)

## Decision Drivers

- **Code Sharing**: Maximize code reuse across services and applications
- **Developer Experience**: Minimize context switching and repository management overhead
- **Build Performance**: Ensure fast CI/CD pipelines with selective builds
- **Team Autonomy**: Enable teams to work independently without blocking
- **Dependency Management**: Simplify dependency updates and version management
- **Atomic Changes**: Support cross-component changes in single commit
- **Tooling**: Leverage modern monorepo tools (Nx, Turborepo, Bazel)

## Considered Options

### Option 1: Monorepo with Selective Builds

**Description**:
Single repository containing all code (backend, frontend, infrastructure, shared libraries) with build tools that support selective builds based on changed files.

**Pros** ✅:

- **Atomic Changes**: Cross-component changes in single commit and PR
- **Code Sharing**: Easy to share code across services without versioning overhead
- **Consistent Tooling**: Single set of linters, formatters, and CI/CD configuration
- **Simplified Dependency Management**: All dependencies in one place, easier to update
- **Better Refactoring**: IDE support for cross-component refactoring
- **Single Source of Truth**: All code in one place, easier to search and navigate
- **Simplified Onboarding**: New developers clone one repository
- **Unified CI/CD**: Single pipeline configuration for all components

**Cons** ❌:

- **Repository Size**: Large repository can slow down Git operations
- **Build Complexity**: Requires sophisticated build tools for selective builds
- **Access Control**: Harder to restrict access to specific components
- **CI/CD Performance**: Risk of slow pipelines without proper optimization
- **Merge Conflicts**: Higher potential for conflicts with many developers
- **Tooling Investment**: Requires investment in monorepo tooling (Nx, Turborepo)

**Cost**:

- **Implementation Cost**: 4 person-weeks (setup monorepo tools, migrate code, configure CI/CD)
- **Tooling Cost**: $0 (open source tools)
- **Maintenance Cost**: 1 person-day/month (monorepo tool updates, optimization)
- **Total Cost of Ownership (3 years)**: ~$25,000 (labor)

**Risk**: Medium

**Risk Description**: Build performance degradation, Git performance issues with large repository

**Effort**: Medium

**Effort Description**: Moderate setup effort, requires learning monorepo tools

### Option 2: Multi-Repo with Shared Libraries

**Description**:
Separate repositories for each major component (backend, CMC frontend, consumer frontend, infrastructure) with shared libraries published as packages.

**Pros** ✅:

- **Clear Ownership**: Each repository has clear team ownership
- **Independent Deployment**: Services can be deployed independently
- **Smaller Repositories**: Faster Git operations
- **Access Control**: Easy to restrict access per repository
- **Simpler CI/CD**: Each repository has independent pipeline
- **Flexible Tooling**: Each repository can use different tools
- **Proven Pattern**: Well-understood approach used by many organizations

**Cons** ❌:

- **Cross-Repo Changes**: Difficult to make atomic changes across repositories
- **Dependency Hell**: Complex dependency management with versioned packages
- **Code Duplication**: Tendency to duplicate code instead of sharing
- **Inconsistent Tooling**: Different linters, formatters per repository
- **Context Switching**: Developers need to switch between repositories
- **Complicated Refactoring**: Cross-repo refactoring requires multiple PRs
- **Version Coordination**: Difficult to coordinate versions across repositories
- **Onboarding Overhead**: New developers need to clone multiple repositories

**Cost**:

- **Implementation Cost**: 2 person-weeks (split existing code, setup shared libraries)
- **Publishing Cost**: $0 (use GitHub Packages or npm registry)
- **Maintenance Cost**: 2 person-days/month (coordinate versions, publish packages)
- **Total Cost of Ownership (3 years)**: ~$35,000 (labor + coordination overhead)

**Risk**: Medium

**Risk Description**: Dependency management complexity, version coordination challenges

**Effort**: Low

**Effort Description**: Simple to implement, well-understood pattern

### Option 3: Hybrid Approach (Monorepo for Backend, Separate Repos for Frontends)

**Description**:
Monorepo for backend services and shared libraries, separate repositories for frontend applications and infrastructure.

**Pros** ✅:

- **Backend Code Sharing**: Easy sharing across backend services
- **Frontend Independence**: Frontends can evolve independently
- **Balanced Complexity**: Simpler than full monorepo, better than full multi-repo
- **Team Alignment**: Backend team uses monorepo, frontend teams use separate repos
- **Flexible Deployment**: Frontends deploy independently, backend services coordinated

**Cons** ❌:

- **Partial Benefits**: Doesn't get full benefits of either approach
- **Cross-Boundary Changes**: Still difficult to make changes across backend/frontend
- **Inconsistent Experience**: Different workflows for backend vs frontend developers
- **Shared Library Complexity**: Still need to publish shared libraries for frontends
- **Tooling Overhead**: Need to maintain both monorepo and multi-repo tooling

**Cost**:

- **Implementation Cost**: 3 person-weeks
- **Maintenance Cost**: 1.5 person-days/month
- **Total Cost of Ownership (3 years)**: ~$30,000

**Risk**: Medium

**Risk Description**: Complexity of maintaining both approaches

**Effort**: Medium

**Effort Description**: Moderate effort to set up and maintain

## Decision Outcome

**Chosen Option**: Option 1 - Monorepo with Selective Builds

**Rationale**:
We chose monorepo with selective builds as our repository strategy. This decision prioritizes developer experience, code sharing, and atomic changes over repository independence:

1. **Atomic Cross-Component Changes**: E-commerce features often require changes across backend services, frontends, and infrastructure. Monorepo enables atomic commits and PRs, reducing coordination overhead and deployment risks.

2. **Code Sharing Without Versioning**: Shared domain models, utilities, and types can be imported directly without publishing packages. This eliminates dependency hell and version coordination challenges.

3. **Consistent Tooling**: Single set of linters (ESLint, Checkstyle), formatters (Prettier, Google Java Format), and CI/CD configuration ensures consistent code quality across all components.

4. **Better Refactoring**: IDE support for cross-component refactoring enables safe large-scale changes. Renaming a domain model automatically updates all usages across services and frontends.

5. **Simplified Dependency Management**: All dependencies in root package.json and build.gradle. Updating a dependency (e.g., Spring Boot version) is a single commit affecting all services.

6. **Developer Experience**: New developers clone one repository and have access to all code. No context switching between repositories, no need to manage multiple Git remotes.

7. **Modern Tooling**: Tools like Nx and Turborepo provide sophisticated build caching and selective builds, addressing traditional monorepo performance concerns.

8. **Team Size**: With 15-20 developers, monorepo is manageable. Large companies (Google, Facebook, Microsoft) successfully use monorepos with thousands of developers.

**Key Factors in Decision**:

1. **Atomic Changes**: Cross-component features are common in e-commerce (e.g., new payment method requires backend, frontend, and infrastructure changes)
2. **Code Sharing**: Domain models, types, and utilities shared across 13 bounded contexts
3. **Team Collaboration**: Small team benefits from unified codebase and tooling
4. **Modern Tooling**: Nx and Turborepo solve traditional monorepo performance issues

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Development Team | High | New workflow, learning monorepo tools | Training sessions, documentation, pair programming |
| DevOps Team | High | New CI/CD pipeline with selective builds | Gradual migration, performance monitoring |
| Frontend Team | Medium | Share repository with backend team | Clear ownership boundaries, CODEOWNERS file |
| Backend Team | Low | Similar to current workflow | Minimal changes needed |
| New Hires | Low | Simpler onboarding (one repository) | Updated onboarding documentation |

### Impact Radius Assessment

**Selected Impact Radius**: Enterprise

**Impact Description**:

- **Enterprise**: Changes affect entire development workflow
  - All developers must adopt monorepo workflow
  - All CI/CD pipelines must support selective builds
  - All teams must follow monorepo conventions
  - All documentation must be updated

### Affected Components

- **All Source Code**: Migrated to monorepo structure
- **CI/CD Pipelines**: Rewritten to support selective builds
- **Build Tools**: Nx or Turborepo integrated
- **Developer Machines**: Monorepo tools installed
- **Documentation**: Updated for monorepo workflow
- **Git Hooks**: Updated for monorepo structure

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Git performance degradation | Medium | Medium | Use Git LFS for large files, shallow clones | DevOps Team |
| CI/CD pipeline slowdown | Medium | High | Implement build caching, selective builds | DevOps Team |
| Merge conflict increase | Medium | Medium | Clear ownership boundaries, frequent integration | Development Team |
| Learning curve for monorepo tools | High | Low | Training sessions, documentation | Tech Lead |
| Repository size growth | Low | Medium | Regular cleanup, Git LFS for binaries | DevOps Team |

**Overall Risk Level**: Medium

**Risk Mitigation Plan**:

- Implement Nx or Turborepo for build caching and selective builds
- Use Git LFS for large binary files (images, fonts)
- Configure shallow clones for CI/CD pipelines
- Establish clear ownership boundaries with CODEOWNERS file
- Monitor CI/CD pipeline performance and optimize
- Provide comprehensive training on monorepo workflow

## Implementation Plan

### Phase 1: Monorepo Setup (Timeline: Week 1)

**Objectives**:

- Set up monorepo structure
- Configure build tools
- Migrate existing code

**Tasks**:

- [ ] Create monorepo structure (apps/, libs/, tools/, docs/)
- [ ] Install and configure Nx or Turborepo
- [ ] Migrate backend services to monorepo
- [ ] Migrate frontend applications to monorepo
- [ ] Migrate infrastructure code to monorepo
- [ ] Configure workspace dependencies
- [ ] Set up CODEOWNERS file for ownership boundaries

**Deliverables**:

- Monorepo structure with all code migrated
- Build tools configured
- CODEOWNERS file established

**Success Criteria**:

- All code accessible in single repository
- Build tools working for all components
- Clear ownership boundaries defined

### Phase 2: CI/CD Pipeline (Timeline: Week 2)

**Objectives**:

- Configure selective builds
- Implement build caching
- Set up deployment pipelines

**Tasks**:

- [ ] Configure Nx/Turborepo affected commands
- [ ] Set up build caching (local and remote)
- [ ] Configure CI/CD pipeline for selective builds
- [ ] Implement parallel builds for independent components
- [ ] Set up deployment pipelines per component
- [ ] Configure branch protection rules
- [ ] Test pipeline performance with sample changes

**Deliverables**:

- CI/CD pipeline with selective builds
- Build caching configured
- Deployment pipelines operational

**Success Criteria**:

- Only affected components build on changes
- Build caching reduces build time by 50%+
- Deployment pipelines working for all components

### Phase 3: Developer Tooling (Timeline: Week 3)

**Objectives**:

- Configure developer tools
- Set up IDE integration
- Create documentation

**Tasks**:

- [ ] Configure ESLint, Prettier for monorepo
- [ ] Set up IDE integration (IntelliJ, VS Code)
- [ ] Configure Git hooks (pre-commit, pre-push)
- [ ] Create developer documentation
- [ ] Set up local development scripts
- [ ] Configure debugging for monorepo
- [ ] Create troubleshooting guide

**Deliverables**:

- Developer tools configured
- IDE integration working
- Comprehensive documentation

**Success Criteria**:

- Developers can build and run all components locally
- IDE provides cross-component navigation and refactoring
- Documentation covers common workflows

### Phase 4: Training and Migration (Timeline: Week 4)

**Objectives**:

- Train development team
- Migrate active branches
- Validate workflow

**Tasks**:

- [ ] Conduct training sessions on monorepo workflow
- [ ] Migrate active feature branches to monorepo
- [ ] Validate CI/CD pipeline with real changes
- [ ] Conduct pair programming sessions
- [ ] Gather feedback and address issues
- [ ] Update onboarding documentation
- [ ] Archive old repositories

**Deliverables**:

- Trained development team
- All active work migrated
- Validated workflow

**Success Criteria**:

- All developers comfortable with monorepo workflow
- No blocking issues identified
- Positive feedback from team

### Rollback Strategy

**Trigger Conditions**:

- Git performance degradation > 50% slower
- CI/CD pipeline performance degradation > 100% slower
- Critical blocking issues preventing development
- Team consensus that monorepo is not working

**Rollback Steps**:

1. **Immediate Action**: Freeze monorepo changes, revert to old repositories
2. **Code Extraction**: Extract code back to separate repositories
3. **CI/CD Restoration**: Restore old CI/CD pipelines
4. **Team Communication**: Communicate rollback plan and timeline
5. **Verification**: Confirm all teams can work with old repositories

**Rollback Time**: 1-2 days

**Rollback Testing**: Test rollback procedure in staging environment before production

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| CI/CD Build Time | < 10 minutes | Pipeline execution time | Daily |
| Build Cache Hit Rate | > 80% | Nx/Turborepo cache metrics | Weekly |
| Git Clone Time | < 2 minutes | Developer feedback | Monthly |
| Cross-Component Changes | > 30% of PRs | PR analysis | Monthly |
| Developer Satisfaction | > 4/5 | Survey | Quarterly |

### Monitoring Plan

**Dashboards**:

- **CI/CD Performance Dashboard**: Build times, cache hit rates, pipeline success rates
- **Repository Health Dashboard**: Repository size, Git performance, active branches
- **Developer Experience Dashboard**: Clone times, build times, satisfaction scores

**Alerts**:

- **Warning**: CI/CD build time > 15 minutes (Slack)
- **Warning**: Build cache hit rate < 70% (Slack)
- **Info**: Repository size > 5GB (Email)

**Review Schedule**:

- **Daily**: Quick check of CI/CD performance
- **Weekly**: Detailed review of build metrics and cache performance
- **Monthly**: Developer experience survey and feedback session
- **Quarterly**: Comprehensive review of monorepo strategy

### Key Performance Indicators (KPIs)

- **Productivity KPI**: 20% reduction in time for cross-component changes
- **Quality KPI**: 30% increase in code reuse across components
- **Efficiency KPI**: 50% reduction in dependency management overhead
- **Experience KPI**: 4/5 developer satisfaction score

## Consequences

### Positive Consequences ✅

- **Atomic Changes**: Cross-component features in single PR reduces coordination
- **Code Sharing**: Easy sharing of domain models, utilities, types
- **Consistent Tooling**: Single linting, formatting, testing configuration
- **Better Refactoring**: IDE support for cross-component refactoring
- **Simplified Dependencies**: All dependencies in one place
- **Unified CI/CD**: Single pipeline configuration
- **Easier Onboarding**: New developers clone one repository
- **Better Collaboration**: All code visible to all developers

### Negative Consequences ❌

- **Learning Curve**: Team needs to learn monorepo tools (Mitigation: Training and documentation)
- **Git Performance**: Large repository may slow Git operations (Mitigation: Git LFS, shallow clones)
- **Build Complexity**: Requires sophisticated build tools (Mitigation: Use proven tools like Nx)
- **Access Control**: Harder to restrict access to components (Mitigation: Use CODEOWNERS and branch protection)

### Technical Debt

**Debt Introduced**:

- **Monorepo Tooling Dependency**: Dependent on Nx or Turborepo for build performance
- **Build Configuration Complexity**: Complex build configuration requires maintenance
- **Migration Effort**: Future migration away from monorepo would be significant effort

**Debt Repayment Plan**:

- **Tooling**: Regularly update monorepo tools to latest versions
- **Configuration**: Quarterly review and simplification of build configuration
- **Migration**: Document extraction procedures for potential future migration

### Long-term Implications

This decision establishes monorepo as our standard repository strategy for the next 3-5 years. As the platform evolves:

- Consider splitting monorepo if repository size exceeds 10GB
- Evaluate new monorepo tools as they emerge (Bazel, Rush, etc.)
- Monitor Git performance and optimize as needed
- Reassess strategy if team grows beyond 50 developers

The monorepo provides foundation for future platform evolution, enabling seamless addition of new services and applications without repository management overhead.

## Related Decisions

### Related ADRs

### Affected Viewpoints

- [Development Viewpoint](../../viewpoints/development/README.md) - Repository structure and build process
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - CI/CD pipelines and deployment

### Affected Perspectives

- [Evolution Perspective](../../perspectives/evolution/README.md) - Code evolution and refactoring
- [Development Resource Perspective](../../perspectives/development-resource/README.md) - Developer workflow and tooling

## Notes

### Assumptions

- Team size remains under 50 developers
- Git performance acceptable with repository size < 10GB
- Monorepo tools (Nx, Turborepo) continue to be maintained
- Team willing to learn new workflow and tools
- CI/CD infrastructure can handle selective builds

### Constraints

- Must support multiple programming languages (Java, TypeScript, Python)
- Must support multiple build tools (Gradle, npm)
- Must enable independent service deployment
- Must maintain reasonable CI/CD pipeline performance
- Must work with existing Git hosting (GitHub)

### Open Questions

- Should we use Nx or Turborepo for build orchestration?
- What is optimal repository size before considering split?
- Should we use Git LFS for all binary files?
- How to handle very large files (videos, datasets)?

### Follow-up Actions

- [ ] Evaluate Nx vs Turborepo and make selection - Architecture Team
- [ ] Create monorepo migration plan - DevOps Team
- [ ] Develop training materials for monorepo workflow - Tech Lead
- [ ] Set up build caching infrastructure - DevOps Team
- [ ] Configure CODEOWNERS file - Team Leads
- [ ] Monitor Git and CI/CD performance - DevOps Team

### References

- [Monorepo Tools Comparison](https://monorepo.tools/)
- [Nx Documentation](https://nx.dev/)
- [Turborepo Documentation](https://turbo.build/repo)
- [Google's Monorepo Approach](https://cacm.acm.org/magazines/2016/7/204032-why-google-stores-billions-of-lines-of-code-in-a-single-repository/fulltext)
- [Microsoft's Monorepo Journey](https://devblogs.microsoft.com/engineering-at-microsoft/monorepo-at-microsoft/)
- [Monorepo Best Practices](https://github.com/korfuri/awesome-monorepo)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
