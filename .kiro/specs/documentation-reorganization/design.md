# Design Document

## Overview

This design document describes the approach for reorganizing the `docs/` directory structure to eliminate redundancy and align with the Rozanski & Woods architecture methodology. The consolidation will merge overlapping directories into the canonical `viewpoints/` and `perspectives/` structure.

## Architecture

### Current Structure (Before)

```
docs/
├── viewpoints/           # Core - Rozanski & Woods viewpoints
├── perspectives/         # Core - Quality perspectives
├── architecture/         # Core - ADRs and patterns
├── diagrams/            # Core - Diagram resources
├── api/                 # Core - API documentation
├── templates/           # Core - Document templates
├── operations/          # OVERLAPS with viewpoints/operational/
├── development/         # OVERLAPS with viewpoints/development/
├── infrastructure/      # OVERLAPS with viewpoints/deployment/
├── getting-started/     # Should be in viewpoints/development/
├── examples/            # Should be in viewpoints/development/
├── generated/           # DUPLICATE of diagrams/generated/
└── [other files]        # Root-level files (README, etc.)
```

### Target Structure (After)

```
docs/
├── viewpoints/           # Core - All 7 viewpoints
│   ├── functional/
│   ├── information/
│   ├── concurrency/
│   ├── development/      # Consolidated from docs/development/
│   │   ├── getting-started/
│   │   ├── examples/
│   │   ├── coding-standards/
│   │   ├── testing/
│   │   └── workflows/
│   ├── deployment/       # Consolidated from docs/infrastructure/
│   │   └── infrastructure/
│   ├── operational/      # Consolidated from docs/operations/
│   │   ├── deployment/
│   │   ├── monitoring/
│   │   ├── runbooks/
│   │   ├── troubleshooting/
│   │   └── maintenance/
│   └── context/
├── perspectives/         # Core - All 8 perspectives
├── architecture/         # Core - ADRs and patterns
├── diagrams/            # Core - All diagrams
│   └── generated/       # Single location for generated diagrams
├── api/                 # Core - API documentation
├── templates/           # Core - Document templates
├── feedback-forms/      # Stakeholder feedback
├── reports/             # Quality reports
├── archive/             # Archived content
└── [root files]         # README.md, STYLE-GUIDE.md, etc.
```

## Components and Interfaces

### Consolidation Process

1. **Operations Consolidation**
   - Source: `docs/operations/`
   - Target: `docs/viewpoints/operational/`
   - Subdirectories: deployment, monitoring, runbooks, troubleshooting, maintenance

2. **Development Consolidation**
   - Source: `docs/development/`
   - Target: `docs/viewpoints/development/`
   - Subdirectories: coding-standards, testing, workflows, examples, setup

3. **Infrastructure Consolidation**
   - Source: `docs/infrastructure/`
   - Target: `docs/viewpoints/deployment/infrastructure/`

4. **Auxiliary Consolidation**
   - `docs/getting-started/` → `docs/viewpoints/development/getting-started/`
   - `docs/examples/` → `docs/viewpoints/development/examples/`
   - `docs/generated/` → Verify content in `docs/diagrams/generated/`, then delete

### Cross-Reference Update Strategy

All links in `docs/README.md` will be updated to reflect new paths:
- `operations/` → `viewpoints/operational/`
- `development/` → `viewpoints/development/`
- `infrastructure/` → `viewpoints/deployment/infrastructure/`

## Data Models

### File Movement Mapping

| Source Path | Target Path |
|-------------|-------------|
| `docs/operations/deployment/` | `docs/viewpoints/operational/deployment/` |
| `docs/operations/monitoring/` | `docs/viewpoints/operational/monitoring/` |
| `docs/operations/runbooks/` | `docs/viewpoints/operational/runbooks/` |
| `docs/operations/troubleshooting/` | `docs/viewpoints/operational/troubleshooting/` |
| `docs/operations/maintenance/` | `docs/viewpoints/operational/maintenance/` |
| `docs/development/coding-standards/` | `docs/viewpoints/development/coding-standards/` |
| `docs/development/testing/` | `docs/viewpoints/development/testing/` |
| `docs/development/workflows/` | `docs/viewpoints/development/workflows/` |
| `docs/development/examples/` | `docs/viewpoints/development/examples/` |
| `docs/development/setup/` | `docs/viewpoints/development/setup/` |
| `docs/infrastructure/*.md` | `docs/viewpoints/deployment/infrastructure/` |
| `docs/getting-started/` | `docs/viewpoints/development/getting-started/` |
| `docs/examples/` | `docs/viewpoints/development/examples/` |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Content Preservation After Move
*For any* file moved from a source directory to a target directory, the file content at the target location should be identical to the original content at the source location.
**Validates: Requirements 1.1, 2.1, 3.1, 4.1, 4.2**

### Property 2: Source Directory Deletion
*For any* consolidation operation, after completion the source directory should no longer exist in the file system.
**Validates: Requirements 1.3, 2.3, 3.2, 4.4**

### Property 3: Link Validity After Update
*For any* internal link in `docs/README.md`, the link target should resolve to an existing file or directory in the documentation structure.
**Validates: Requirements 1.4, 2.4, 3.3, 5.1, 5.3**

### Property 4: No References to Deleted Directories
*For any* reference in `docs/README.md`, the reference should not point to any of the deleted directories (`operations/`, `development/`, `infrastructure/`, `getting-started/`, `examples/`, `generated/`).
**Validates: Requirements 5.2**

### Property 5: Generated Diagrams Single Location
*For any* generated diagram file, it should exist only in `docs/diagrams/generated/` and not in any other `generated/` directory.
**Validates: Requirements 4.3, 4.4**

## Error Handling

- **File Conflict**: If a file with the same name exists in both source and target, merge content or rename with suffix
- **Missing Target Directory**: Create target directory if it doesn't exist before moving files
- **Broken Links**: Log all broken links found during validation for manual review

## Testing Strategy

### Property-Based Testing

Use pytest with hypothesis for property-based testing:
- Test file content preservation after moves
- Test directory deletion verification
- Test link validity in README.md
- Test no references to deleted directories

### Unit Testing

- Test individual file move operations
- Test directory deletion operations
- Test link extraction and validation
- Test README.md update operations
