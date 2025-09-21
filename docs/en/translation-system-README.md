
# Automated Translation System

## Overview

This project implements an automated translation system based on Rozanski & Woods methodology, ensuring consistency and professional terminology accuracy between Chinese and English documentation.

## System Components

### 1. Kiro Hook Configuration

**File**: `.kiro/hooks/md-docs-translation.kiro.hook`

Automatically monitors changes to:
- `README.md`
- `docs/**/*.md`

**Excluded files**:
- `docs/en/**/*.md` (English versions)
- `docs/.terminology.json` (Terminology dictionary)
- System files (`node_modules`, `.git`, `.kiro`)

### 2. Professional Terminology Dictionary

**File**: `docs/.terminology.json`

Contains terminology categories:
- **Rozanski & Woods Viewpoints**: Functional, Information, Concurrency, etc.
- **Rozanski & Woods Perspectives**: Security, Performance, Availability, etc.
- **DDD Strategic Patterns**: Bounded Context, Context Mapping, etc.
- **DDD Tactical Patterns**: Aggregate Root, Value Object, Domain Event, etc.
- **Event Storming**: Big Picture, Process Level, Design Level, etc.
- **Hexagonal Architecture**: Port, Adapter, Application Core, etc.
- **Infrastructure as Code**: AWS CDK, Multi-Stack Architecture, etc.
- **Testing Terminology**: TDD, BDD, Unit Test, etc.
- **Observability**: Monitoring, Logging, Tracing, etc.
- **Quality Attributes**: Performance, Scalability, Availability, etc.
- **Architectural Patterns**: Layered Architecture, Microservices, Event-Driven, etc.
- **Development Practices**: Agile Development, DevOps, Continuous Integration, etc.
- **Cloud Native**: Containerization, Kubernetes, Service Mesh, etc.
- **AI-Assisted Development**: MCP, Code Generation, etc.

### 3. Translation Quality Check Mechanism

**Script**: `scripts/check-translation-quality.sh`

**Checks**:
- Translation completeness
- Terminology consistency
- Internal link validity
- Directory structure consistency
- Terminology dictionary validation

**Usage**:
```bash
./scripts/check-translation-quality.sh
```

### 4. GitHub Actions Automation

**Workflow**: `.github/workflows/translation-quality-check.yml`

**Triggers**:
- Push commits with documentation changes
- Pull requests with documentation changes

**Checks**:
- Translation quality verification
- Terminology dictionary validation
- Hook configuration verification
- Translation report generation

## File Organization Rules

### Chinese → English File Mapping

| Chinese File | English File |
|-------------|-------------|
| `README.md` | `docs/en/PROJECT_README.md` |
| `docs/viewpoints/**/*.md` | `docs/en/viewpoints/**/*.md` |
| `docs/perspectives/**/*.md` | `docs/en/perspectives/**/*.md` |
| `docs/diagrams/**/*.md` | `docs/en/diagrams/**/*.md` |
| `docs/templates/**/*.md` | `docs/en/templates/**/*.md` |

### Directory Structure

```
docs/
├── viewpoints/           # Seven architectural viewpoints
│   ├── functional/       # Functional Viewpoint
│   ├── information/      # Information Viewpoint
│   ├── concurrency/      # Concurrency Viewpoint
│   ├── development/      # Development Viewpoint
│   ├── deployment/       # Deployment Viewpoint
│   └── operational/      # Operational Viewpoint
├── perspectives/         # Eight architectural perspectives
│   ├── security/         # Security Perspective
│   ├── performance/      # Performance Perspective
│   ├── availability/     # Availability Perspective
│   ├── evolution/        # Evolution Perspective
│   ├── usability/        # Usability Perspective
│   ├── regulation/       # Regulation Perspective
│   ├── location/         # Location Perspective
│   └── cost/            # Cost Perspective
├── diagrams/            # Diagram resources
├── templates/           # Document templates
├── .terminology.json    # Terminology dictionary
└── en/                  # English versions (auto-generated)
    └── [Corresponding English file structure]
```

## Translation Rules

### Terminology Translation Standards

**Rozanski & Woods Viewpoints**:
- Architectural Viewpoint → Architectural Viewpoint
- Functional Viewpoint → Functional Viewpoint
- Information Viewpoint → Information Viewpoint
- Concurrency Viewpoint → Concurrency Viewpoint
- Development Viewpoint → Development Viewpoint
- Deployment Viewpoint → Deployment Viewpoint
- Operational Viewpoint → Operational Viewpoint

**Rozanski & Woods Perspectives**:
- Architectural Perspective → Architectural Perspective
- Security Perspective → Security Perspective
- Performance & Scalability Perspective → Performance & Scalability Perspective
- Availability & Resilience Perspective → Availability & Resilience Perspective
- Evolution Perspective → Evolution Perspective
- Usability Perspective → Usability Perspective
- Regulation Perspective → Regulation Perspective
- Location Perspective → Location Perspective
- Cost Perspective → Cost Perspective

**DDD Terminology**:
- Domain-Driven Design → Domain-Driven Design (DDD)
- Bounded Context → Bounded Context
- Aggregate Root → Aggregate Root
- Value Object → Value Object
- Domain Event → Domain Event
- Event Storming → Event Storming

### Format Preservation Rules

**Must remain unchanged**:
- URLs
- File paths
- Code blocks
- Command examples
- Version numbers
- API endpoints
- Configuration keys

**Format preservation**:
- Markdown headers
- Bullet points
- Numbered lists
- Tables
- Code fences
- Blockquotes
- Emphasis (bold/italic)
- Links

## Usage

### Automatic Translation Trigger

1. **Edit Chinese files**: Modify any `docs/**/*.md` or `README.md` file
2. **Hook auto-trigger**: Kiro Hook automatically detects changes and triggers translation
3. **Generate English version**: System creates corresponding English files in `docs/en/` directory

### Manual Quality Check

```bash
# Run complete translation quality check
./scripts/check-translation-quality.sh

# Check translation status of specific files
grep -l "Architectural Viewpoint" docs/**/*.md
```

### Terminology Dictionary Update

1. Edit `docs/.terminology.json`
2. Add or modify terminology mappings
3. Run quality check to verify changes
4. Re-translate related files

## Quality Assurance

### Automated Checks

- **CI/CD Integration**: GitHub Actions automatically runs translation quality checks
- **Pull Request Checks**: Every PR checks translation quality
- **Terminology Consistency**: Automatically verifies consistent terminology usage
- **Link Validity**: Checks internal link validity

### Manual Checks

- **Professional Terminology Review**: Regularly review terminology translation accuracy
- **Context Verification**: Ensure translations are appropriate in specific contexts
- **Cross-document Consistency**: Check consistency of same terms across different documents

## Troubleshooting

### Common Issues

**1. Hook not triggering translation**
- Check if `.kiro/hooks/md-docs-translation.kiro.hook` exists
- Verify file paths match Hook monitoring patterns
- Check if Hook is enabled (`"enabled": true`)

**2. Inconsistent terminology translation**
- Check terminology definitions in `docs/.terminology.json`
- Run `./scripts/check-translation-quality.sh` to find inconsistencies
- Update terminology usage in English files

**3. Translation quality check failures**
- Review detailed error reports
- Check for missing translation files
- Fix broken internal links
- Update terminology usage

### Maintenance Recommendations

1. **Regular terminology dictionary updates**: Add new professional terms as project evolves
2. **Monitor translation quality**: Regularly run quality checks to ensure translation accuracy
3. **Document structure synchronization**: Ensure Chinese and English document structures remain consistent
4. **Team training**: Ensure team members understand how to use the translation system

## Future Improvements

### Planned Features

- **Incremental translation**: Only translate changed parts for improved efficiency
- **Translation memory**: Build translation memory for improved consistency
- **Multi-language support**: Support translation to more languages
- **AI-assisted review**: Use AI to assist with translation quality review

### Technical Improvements

- **Performance optimization**: Optimize translation processing for large files
- **Error recovery**: Improve error handling when translation fails
- **Batch processing**: Support batch translation of multiple files
- **Version control**: Track translation versions and change history

This automated translation system ensures internationalization quality of project documentation while maintaining consistency and accuracy of professional terminology.