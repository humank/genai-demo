# Development Toolchain

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Engineering Management & DevOps Team

## Overview

This document provides comprehensive information about the development toolchain used by the engineering team, including tool selection rationale, setup guides, best practices, and cost analysis.

## Toolchain Architecture

```text
┌─────────────────────────────────────────────────────────┐
│              Development Toolchain Stack                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Developer Workstation                                  │
│  ┌───────────────────────────────────────────────┐     │
│  │ IDE: IntelliJ IDEA / VS Code                  │     │
│  │ Version Control: Git + GitHub Desktop         │     │
│  │ Local Runtime: Docker Desktop                 │     │
│  │ Database Tools: DataGrip, pgAdmin             │     │
│  │ API Testing: Postman, Insomnia                │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  Source Control & Collaboration                         │
│  ┌───────────────────────────────────────────────┐     │
│  │ GitHub Enterprise                             │     │
│  │ - Repository hosting                          │     │
│  │ - Code review (Pull Requests)                 │     │
│  │ - Issue tracking                              │     │
│  │ - GitHub Actions (CI/CD)                      │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  Build & Package Management                             │
│  ┌───────────────────────────────────────────────┐     │
│  │ Backend: Gradle 8.x, Maven Central            │     │
│  │ Frontend: npm/pnpm, Node.js 20                │     │
│  │ Containers: Docker, Docker Compose            │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  CI/CD Pipeline                                         │
│  ┌───────────────────────────────────────────────┐     │
│  │ GitHub Actions (Build, Test, Deploy)          │     │
│  │ AWS CodePipeline (Production deployment)      │     │
│  │ ArgoCD (GitOps for Kubernetes)                │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  Quality & Security                                     │
│  ┌───────────────────────────────────────────────┐     │
│  │ SonarQube (Code quality)                      │     │
│  │ Snyk (Security scanning)                      │     │
│  │ Dependabot (Dependency updates)               │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  Monitoring & Observability                             │
│  ┌───────────────────────────────────────────────┐     │
│  │ CloudWatch (Logs, Metrics)                    │     │
│  │ X-Ray (Distributed tracing)                   │     │
│  │ Grafana (Dashboards)                          │     │
│  └───────────────────────────────────────────────┘     │
│                        ↓                                │
│  Collaboration & Communication                          │
│  ┌───────────────────────────────────────────────┐     │
│  │ Slack (Team communication)                    │     │
│  │ Jira (Project management)                     │     │
│  │ Confluence (Documentation)                    │     │
│  │ Miro (Diagramming)                            │     │
│  └───────────────────────────────────────────────┘     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Development Tools

### Integrated Development Environments (IDEs)

#### IntelliJ IDEA Ultimate

**Purpose**: Primary IDE for backend Java development

**License**: Commercial (JetBrains)  
**Cost**: $16.90/user/month (annual subscription)  
**Users**: All backend engineers (7 people)

**Key Features**:

- Best-in-class Java and Spring Boot support
- Intelligent code completion and refactoring
- Built-in database tools
- Excellent debugging capabilities
- Git integration
- Plugin ecosystem

**Setup Guide**:

1. Download from [JetBrains website](https://www.jetbrains.com/idea/)
2. Install and activate with company license
3. Install required plugins:
   - Lombok Plugin
   - Spring Boot Assistant
   - AWS Toolkit
   - SonarLint
   - CheckStyle-IDEA
4. Import code style settings from repository
5. Configure JDK 21
6. Set up Gradle integration

**Best Practices**:

- Use keyboard shortcuts for productivity
- Enable auto-import for dependencies
- Configure code inspections
- Use Live Templates for common patterns
- Regularly update to latest version

---

#### Visual Studio Code

**Purpose**: Primary IDE for frontend development, secondary for backend

**License**: Free (MIT)  
**Cost**: $0  
**Users**: All frontend engineers (4 people), DevOps (2 people)

**Key Features**:

- Excellent TypeScript/JavaScript support
- Rich extension marketplace
- Integrated terminal
- Git integration
- Remote development capabilities
- Lightweight and fast

**Setup Guide**:

1. Download from [VS Code website](https://code.visualstudio.com/)
2. Install recommended extensions:
   - ESLint
   - Prettier
   - TypeScript and JavaScript Language Features
   - React Developer Tools
   - Angular Language Service
   - GitLens
   - Docker
   - AWS Toolkit
3. Configure settings.json from repository
4. Set up workspace settings
5. Configure debugging for Node.js

**Recommended Extensions**:

```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "eamodio.gitlens",
    "ms-azuretools.vscode-docker",
    "amazonwebservices.aws-toolkit-vscode",
    "bradlc.vscode-tailwindcss",
    "dsznajder.es7-react-js-snippets",
    "angular.ng-template"
  ]
}
```

---

#### DataGrip

**Purpose**: Database IDE for all database operations

**License**: Commercial (JetBrains)  
**Cost**: $8.90/user/month (annual subscription)  
**Users**: Backend engineers (7 people), QA (3 people)

**Key Features**:

- Support for PostgreSQL, Redis, and other databases
- Intelligent SQL editor with auto-completion
- Query execution and result visualization
- Database schema management
- Data export/import tools
- Query performance analysis

**Setup Guide**:

1. Download from [JetBrains website](https://www.jetbrains.com/datagrip/)
2. Configure database connections:
   - Local PostgreSQL (development)
   - Staging database (read-only)
   - Production database (read-only, restricted access)
3. Import SQL code style from repository
4. Set up query history and favorites

## Version Control Tools

### Git

**Purpose**: Distributed version control system

**License**: Free (GPL)  
**Cost**: $0  
**Users**: All engineers (18 people)

**Setup Guide**:

1. Install Git from [git-scm.com](https://git-scm.com/)
2. Configure user information:

   ```bash
   git config --global user.name "Your Name"
   git config --global user.email "your.email@company.com"
   ```

3. Set up SSH keys for GitHub
4. Configure Git aliases from repository

**Git Workflow**:

- Main branch: `main` (production)
- Development branch: `develop`
- Feature branches: `feature/{ticket-number}-{description}`
- Hotfix branches: `hotfix/{ticket-number}-{description}`
- Release branches: `release/{version}`

**Commit Message Convention**:

```text
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

---

### GitHub Enterprise

**Purpose**: Repository hosting, code review, CI/CD

**License**: Commercial  
**Cost**: $21/user/month  
**Users**: All engineers (18 people)

**Key Features**:

- Unlimited private repositories
- Pull request reviews
- GitHub Actions (CI/CD)
- Issue tracking
- Project boards
- Security scanning (Dependabot, CodeQL)
- Team management

**Setup Guide**:

1. Create GitHub account with company email
2. Join organization
3. Set up 2FA (required)
4. Configure SSH keys
5. Install GitHub CLI (optional)

**Branch Protection Rules**:

- Require pull request reviews (2 approvals)
- Require status checks to pass
- Require branches to be up to date
- Require signed commits (optional)
- Restrict who can push to main

---

### GitHub Desktop / GitKraken

**Purpose**: GUI Git client (optional)

**License**: Free (GitHub Desktop) / Commercial (GitKraken)  
**Cost**: $0 (GitHub Desktop), $4.95/user/month (GitKraken)  
**Users**: Optional for all engineers

**Use Cases**:

- Visual diff and merge conflict resolution
- Easier branch management for beginners
- Visual commit history
- Simplified Git operations

## Build & Package Management

### Gradle

**Purpose**: Build automation for backend Java projects

**License**: Free (Apache 2.0)  
**Cost**: $0  
**Users**: All backend engineers (7 people)

**Version**: 8.x

**Key Features**:

- Dependency management
- Multi-project builds
- Incremental builds
- Build caching
- Plugin ecosystem

**Setup Guide**:

1. Install via SDKMAN:

   ```bash
   sdk install gradle 8.5
   ```

2. Verify installation:

   ```bash
   gradle --version
   ```

3. Configure Gradle wrapper in project
4. Set up Gradle daemon for faster builds

**Common Commands**:

```bash
./gradlew clean build          # Clean and build
./gradlew test                 # Run tests
./gradlew bootRun              # Run Spring Boot app
./gradlew dependencyUpdates    # Check for updates
```

---

### npm / pnpm

**Purpose**: Package management for frontend projects

**License**: Free  
**Cost**: $0  
**Users**: All frontend engineers (4 people)

**Versions**:

- Node.js: 20.x LTS
- npm: 10.x
- pnpm: 8.x (preferred for monorepos)

**Setup Guide**:

1. Install Node.js from [nodejs.org](https://nodejs.org/)
2. Install pnpm globally:

   ```bash
   npm install -g pnpm
   ```

3. Verify installation:

   ```bash
   node --version
   pnpm --version
   ```

**Common Commands**:

```bash
pnpm install                   # Install dependencies
pnpm run dev                   # Start dev server
pnpm run build                 # Build for production
pnpm test                      # Run tests
pnpm run lint                  # Run linter
```

---

### Docker Desktop

**Purpose**: Local containerization and development environment

**License**: Free for small businesses  
**Cost**: $0 (< 250 employees)  
**Users**: All engineers (18 people)

**Key Features**:

- Run containers locally
- Docker Compose for multi-container apps
- Kubernetes support (optional)
- Volume management
- Network management

**Setup Guide**:

1. Download from [docker.com](https://www.docker.com/products/docker-desktop/)
2. Install and start Docker Desktop
3. Configure resources (CPU, Memory)
4. Enable Kubernetes (optional)
5. Log in to Docker Hub (optional)

**Common Commands**:

```bash
docker-compose up              # Start services
docker-compose down            # Stop services
docker ps                      # List running containers
docker logs <container>        # View logs
docker exec -it <container> sh # Shell into container
```

## Testing Tools

### Postman

**Purpose**: API development and testing

**License**: Free / Commercial  
**Cost**: $0 (Free tier), $14/user/month (Team tier)  
**Users**: All engineers (18 people)

**Key Features**:

- API request builder
- Collection organization
- Environment variables
- Automated testing
- Mock servers
- API documentation

**Setup Guide**:

1. Download from [postman.com](https://www.postman.com/)
2. Create account with company email
3. Join team workspace
4. Import shared collections
5. Configure environments (local, staging, production)

**Best Practices**:

- Organize requests into collections
- Use environment variables for URLs and tokens
- Write tests for API responses
- Share collections with team
- Keep collections in sync with API changes

---

### JMeter / Gatling

**Purpose**: Performance and load testing

**License**: Free (Apache 2.0)  
**Cost**: $0  
**Users**: QA engineers (3 people), Senior backend engineers

**Use Cases**:

- Load testing
- Stress testing
- Performance benchmarking
- Capacity planning

**Setup Guide (JMeter)**:

1. Download from [jmeter.apache.org](https://jmeter.apache.org/)
2. Extract and run:

   ```bash
   ./bin/jmeter
   ```

3. Install plugins via Plugin Manager
4. Configure test plans

## CI/CD Tools

### GitHub Actions

**Purpose**: Continuous Integration and Deployment

**License**: Included with GitHub Enterprise  
**Cost**: Included in GitHub subscription  
**Users**: All engineers (automated)

**Key Features**:

- Workflow automation
- Matrix builds
- Secrets management
- Artifact storage
- Integration with AWS

**Workflow Examples**:

**Backend CI Workflow**:

```yaml
name: Backend CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      - name: Set up JDK 21

        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle

        run: ./gradlew build

      - name: Run tests

        run: ./gradlew test

      - name: Upload coverage

        uses: codecov/codecov-action@v3
```

**Frontend CI Workflow**:

```yaml
name: Frontend CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      - name: Setup Node.js

        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies

        run: pnpm install

      - name: Lint

        run: pnpm run lint

      - name: Test

        run: pnpm test

      - name: Build

        run: pnpm run build
```

---

### ArgoCD

**Purpose**: GitOps continuous delivery for Kubernetes

**License**: Free (Apache 2.0)  
**Cost**: $0  
**Users**: DevOps engineers (2 people)

**Key Features**:

- Declarative GitOps
- Automated sync
- Health monitoring
- Rollback capabilities
- Multi-cluster support

**Setup Guide**:

1. Install ArgoCD in Kubernetes cluster
2. Configure Git repository connection
3. Create application manifests
4. Set up sync policies
5. Configure notifications

## Quality & Security Tools

### SonarQube

**Purpose**: Code quality and security analysis

**License**: Community (Free) / Commercial  
**Cost**: $0 (Community), $150/month (Developer Edition)  
**Users**: All engineers (automated in CI/CD)

**Key Features**:

- Code smell detection
- Security vulnerability scanning
- Code coverage tracking
- Technical debt measurement
- Quality gates

**Setup Guide**:

1. Access SonarQube server (hosted internally)
2. Configure project in SonarQube
3. Add SonarQube plugin to Gradle/npm
4. Configure quality gates
5. Integrate with GitHub Actions

**Quality Gates**:

- Code coverage > 80%
- No critical/blocker issues
- Technical debt ratio < 5%
- Duplicated code < 3%

---

### Snyk

**Purpose**: Security vulnerability scanning

**License**: Free / Commercial  
**Cost**: $0 (Free tier), $52/user/month (Team tier)  
**Users**: All engineers (automated)

**Key Features**:

- Dependency vulnerability scanning
- Container image scanning
- Infrastructure as Code scanning
- License compliance checking
- Automated fix PRs

**Setup Guide**:

1. Install Snyk CLI:

   ```bash
   npm install -g snyk
   ```

2. Authenticate:

   ```bash
   snyk auth
   ```

3. Test project:

   ```bash
   snyk test
   ```

4. Monitor project:

   ```bash
   snyk monitor
   ```

---

### Dependabot

**Purpose**: Automated dependency updates

**License**: Included with GitHub  
**Cost**: $0  
**Users**: All engineers (automated)

**Configuration** (`.github/dependabot.yml`):

```yaml
version: 2
updates:

  - package-ecosystem: "gradle"

    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    
  - package-ecosystem: "npm"

    directory: "/frontend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
```

## Monitoring & Observability Tools

### AWS CloudWatch

**Purpose**: Logging, metrics, and monitoring

**License**: AWS Service  
**Cost**: Pay-as-you-go (included in AWS costs)  
**Users**: All engineers, Operations team

**Key Features**:

- Log aggregation
- Metrics collection
- Alarms and notifications
- Dashboards
- Log Insights queries

**Setup Guide**:

1. Configure CloudWatch agent on EC2/EKS
2. Set up log groups
3. Create metric filters
4. Configure alarms
5. Build dashboards

---

### AWS X-Ray

**Purpose**: Distributed tracing

**License**: AWS Service  
**Cost**: Pay-as-you-go  
**Users**: Backend engineers, DevOps

**Key Features**:

- Request tracing
- Service map visualization
- Performance analysis
- Error tracking
- Integration with Spring Boot

**Setup Guide**:

1. Add X-Ray SDK to Spring Boot:

   ```gradle
   implementation 'com.amazonaws:aws-xray-recorder-sdk-spring'
   ```

2. Configure X-Ray daemon
3. Add tracing annotations
4. View traces in X-Ray console

---

### Grafana

**Purpose**: Metrics visualization and dashboards

**License**: Free (AGPL) / Commercial  
**Cost**: $0 (self-hosted), $49/month (Grafana Cloud)  
**Users**: All engineers, Operations team

**Key Features**:

- Beautiful dashboards
- Multiple data sources
- Alerting
- Annotations
- Templating

**Setup Guide**:

1. Access Grafana instance (hosted internally)
2. Configure data sources (CloudWatch, Prometheus)
3. Import dashboard templates
4. Create custom dashboards
5. Set up alerts

## Collaboration Tools

### Slack

**Purpose**: Team communication and collaboration

**License**: Commercial  
**Cost**: $8.75/user/month (Pro plan)  
**Users**: All team members (25+ people)

**Key Channels**:

- `#engineering` - General engineering discussions
- `#backend` - Backend team
- `#frontend` - Frontend team
- `#devops` - Infrastructure and deployments
- `#qa` - Quality assurance
- `#incidents` - Production incidents
- `#deployments` - Deployment notifications
- `#github` - GitHub notifications
- `#monitoring` - Monitoring alerts

**Integrations**:

- GitHub (PR notifications, commits)
- Jira (issue updates)
- PagerDuty (incident alerts)
- CloudWatch (alarms)
- ArgoCD (deployment status)

---

### Jira

**Purpose**: Project management and issue tracking

**License**: Commercial  
**Cost**: $7.75/user/month (Standard plan)  
**Users**: All team members (25+ people)

**Key Features**:

- Agile boards (Scrum/Kanban)
- Sprint planning
- Backlog management
- Reporting and dashboards
- Workflow automation

**Project Structure**:

- **ECOM**: Main e-commerce project
- **INFRA**: Infrastructure tasks
- **TECH**: Technical debt and improvements

**Issue Types**:

- Epic
- Story
- Task
- Bug
- Sub-task

---

### Confluence

**Purpose**: Documentation and knowledge management

**License**: Commercial  
**Cost**: $6.05/user/month (Standard plan)  
**Users**: All team members (25+ people)

**Key Spaces**:

- **Engineering** - Technical documentation
- **Product** - Product requirements and roadmap
- **Operations** - Runbooks and procedures
- **Onboarding** - New hire documentation

**Best Practices**:

- Use templates for consistency
- Link to code repositories
- Keep documentation up-to-date
- Use labels for organization
- Regular documentation reviews

---

### Miro

**Purpose**: Collaborative diagramming and brainstorming

**License**: Commercial  
**Cost**: $8/user/month (Team plan)  
**Users**: All engineers (18 people)

**Use Cases**:

- Event storming workshops
- Architecture diagrams
- Sprint planning
- Retrospectives
- Brainstorming sessions

## Tool Cost Summary

### Monthly Cost per Engineer

| Category | Tool | Cost/User/Month | Users | Total/Month |
|----------|------|-----------------|-------|-------------|
| **IDE** | IntelliJ IDEA Ultimate | $16.90 | 7 | $118.30 |
| **IDE** | VS Code | $0 | 11 | $0 |
| **Database** | DataGrip | $8.90 | 10 | $89.00 |
| **Version Control** | GitHub Enterprise | $21.00 | 18 | $378.00 |
| **Project Management** | Jira | $7.75 | 25 | $193.75 |
| **Communication** | Slack | $8.75 | 25 | $218.75 |
| **Documentation** | Confluence | $6.05 | 25 | $151.25 |
| **Diagramming** | Miro | $8.00 | 18 | $144.00 |
| **API Testing** | Postman | $14.00 | 18 | $252.00 |
| **Security** | Snyk | $52.00 | 1 | $52.00 |
| **Monitoring** | Grafana Cloud | $49.00 | 1 | $49.00 |
| **Total** | | | | **$1,646.05/month** |

**Annual Tool Cost**: ~$19,753

**Cost per Engineer per Month**: ~$91.45

**Note**: AWS infrastructure costs (CloudWatch, X-Ray, etc.) are separate and billed based on usage.

## Tool Setup Checklist

### New Engineer Onboarding

**Day 1: Account Setup**

- [ ] Create company email account
- [ ] Set up GitHub account and join organization
- [ ] Set up Slack account and join channels
- [ ] Set up Jira account
- [ ] Set up Confluence account
- [ ] Enable 2FA on all accounts

**Day 1-2: Development Environment**

- [ ] Install IDE (IntelliJ IDEA or VS Code)
- [ ] Install Git and configure
- [ ] Install Docker Desktop
- [ ] Install Node.js and pnpm (frontend)
- [ ] Install Java 21 and Gradle (backend)
- [ ] Clone repositories
- [ ] Set up local database (PostgreSQL)
- [ ] Install Postman and import collections

**Day 3: Additional Tools**

- [ ] Install DataGrip (if needed)
- [ ] Set up AWS CLI and credentials
- [ ] Install kubectl (if needed)
- [ ] Set up Miro account
- [ ] Configure IDE plugins and settings

**Week 1: Verification**

- [ ] Successfully run application locally
- [ ] Create first pull request
- [ ] Run tests locally
- [ ] Access monitoring dashboards
- [ ] Complete tool training

## Best Practices

### Tool Usage Guidelines

1. **Keep Tools Updated**: Regularly update IDEs, CLI tools, and dependencies
2. **Use Company Licenses**: Always use company-provided licenses
3. **Secure Credentials**: Never commit credentials to repositories
4. **Follow Conventions**: Use team-agreed coding styles and configurations
5. **Share Knowledge**: Document tool tips and tricks in Confluence
6. **Report Issues**: Report tool problems to DevOps team
7. **Backup Configurations**: Keep IDE settings in version control

### Security Best Practices

- Enable 2FA on all accounts
- Use SSH keys for Git authentication
- Rotate credentials regularly
- Don't share accounts
- Use password manager for credentials
- Report security incidents immediately

## Related Documentation

- [Overview](overview.md) - Development Resource Perspective overview
- [Team Structure](team-structure.md) - Team organization and roles
- [Required Skills](required-skills.md) - Skill requirements and training

---

**Next Steps**: Complete tool setup using the onboarding checklist and refer to individual tool documentation for detailed usage guides.
