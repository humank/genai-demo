# Diagrams

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This directory contains all architectural diagrams for the project.

## Structure

```
diagrams/
├── viewpoints/      # Diagrams organized by viewpoint
├── perspectives/    # Diagrams organized by perspective
├── generated/       # Auto-generated diagrams from PlantUML
├── mermaid/         # Standalone Mermaid diagrams
└── excalidraw/      # Excalidraw source files
```

## Diagram Types

### Mermaid Diagrams (Inline)
Most diagrams are now embedded directly in documentation as Mermaid code blocks. See:
- [Deployment Overview](../viewpoints/deployment/overview.md)
- [Functional Overview](../viewpoints/functional/overview.md)
- [Security Overview](../perspectives/security/README.md)
- [Performance Overview](../perspectives/performance/README.md)

### PlantUML Diagrams (Generated)
Complex UML diagrams are generated from PlantUML sources:
- Source: `diagrams/viewpoints/` and `diagrams/perspectives/`
- Generated: `diagrams/generated/`
- Format: PNG (primary), SVG (secondary)

## Generation

Generate diagrams using:

```bash
./scripts/generate-diagrams.sh --format=png
```

## Standards

For diagram standards, see:

**Note**: We now use Mermaid for most diagrams (20+ diagrams added in Phase 3).
