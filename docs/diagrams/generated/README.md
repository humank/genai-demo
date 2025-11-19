# Generated Diagrams

> **Status**: 📝 Under Construction  
> **Last Updated**: 2024-11-19

## Overview

This directory contains auto-generated diagrams from PlantUML sources.

## Structure

```
generated/
├── functional/      # Functional viewpoint diagrams (PNG/SVG)
├── information/     # Information viewpoint diagrams (PNG/SVG)
├── deployment/      # Deployment viewpoint diagrams (PNG/SVG)
├── security/        # Security perspective diagrams (PNG/SVG)
├── performance/     # Performance perspective diagrams (PNG/SVG)
└── ...
```

## Generation

Diagrams are generated from PlantUML source files using:

```bash
./scripts/generate-diagrams.sh --format=png
```

## Diagram Standards

- **Primary Format**: PNG (recommended for GitHub)
- **Secondary Format**: SVG (for high-resolution)
- **Source Location**: `docs/diagrams/viewpoints/` and `docs/diagrams/perspectives/`

## Related Documentation

- [Diagram Standards](../../.kiro/steering/diagram-standards.md)
- [Diagram Generation Standards](../../.kiro/steering/diagram-generation-standards.md)

---

**Note**: Many diagrams are now embedded as Mermaid diagrams directly in the documentation. This directory contains PlantUML-generated diagrams.
