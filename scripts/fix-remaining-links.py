#!/usr/bin/env python3
"""
Fix remaining broken links after initial fix
"""

import os
import re

def fix_specific_links():
    """Fix specific known broken links"""
    
    fixes = [
        # Fix relative path issues in development docs
        {
            'file': 'docs/development/coding-standards.md',
            'old': '../.kiro/steering/development-standards.md',
            'new': '../../.kiro/steering/development-standards.md'
        },
        {
            'file': 'docs/development/coding-standards.md',
            'old': '../.kiro/steering/code-review-standards.md',
            'new': '../../.kiro/steering/code-review-standards.md'
        },
        {
            'file': 'docs/development/coding-standards.md',
            'old': '../.kiro/steering/security-standards.md',
            'new': '../../.kiro/steering/security-standards.md'
        },
        {
            'file': 'docs/development/documentation-guide.md',
            'old': '../.kiro/steering/translation-guide.md',
            'new': '../../.kiro/steering/translation-guide.md'
        },
        {
            'file': 'docs/development/documentation-guide.md',
            'old': '../.kiro/steering/reports-organization-standards.md',
            'new': '../../.kiro/steering/reports-organization-standards.md'
        },
        {
            'file': 'docs/development/getting-started.md',
            'old': '../DEVELOPER_QUICKSTART.md',
            'new': '../../DEVELOPER_QUICKSTART.md'
        },
        
        # Fix docs/README.md links
        {
            'file': 'docs/README.md',
            'old': 'reports-summaries/general/generation-report.md',
            'new': '../reports-summaries/general/generation-report.md'
        },
        {
            'file': 'docs/README.md',
            'old': 'reports-summaries/project-management/project-summary-2025.md',
            'new': '../reports-summaries/project-management/project-summary-2025.md'
        },
        {
            'file': 'docs/README.md',
            'old': 'docs/deployment/kubernetes-guide.md',
            'new': 'deployment/kubernetes-guide.md'
        },
        {
            'file': 'docs/README.md',
            'old': 'docs/en/observability/production-observability-testing-guide.md',
            'new': 'en/viewpoints/operational/production-observability-testing-guide.md'
        },
        
        # Fix architecture docs
        {
            'file': 'docs/architecture/adr/README.md',
            'old': 'reports-summaries/architecture-design/ADR-SUMMARY.md',
            'new': '../../../reports-summaries/architecture-design/ADR-SUMMARY.md'
        },
        {
            'file': 'docs/architecture/mcp-integration-importance.md',
            'old': 'docs/en/infrastructure/docs/MCP_INTEGRATION_GUIDE.md',
            'new': '../en/infrastructure/docs/MCP_INTEGRATION_GUIDE.md'
        },
        {
            'file': 'docs/architecture/mcp-quick-reference.md',
            'old': 'reports-summaries/infrastructure/executive-summary.md',
            'new': '../../reports-summaries/infrastructure/executive-summary.md'
        },
        {
            'file': 'docs/architecture/mcp-quick-reference.md',
            'old': 'docs/en/infrastructure/docs/MCP_INTEGRATION_GUIDE.md',
            'new': '../en/infrastructure/docs/MCP_INTEGRATION_GUIDE.md'
        },
        {
            'file': 'docs/architecture/overview.md',
            'old': 'reports-summaries/project-management/REFACTORING_SUMMARY.md',
            'new': '../../reports-summaries/project-management/REFACTORING_SUMMARY.md'
        },
        {
            'file': 'docs/architecture/overview.md',
            'old': 'docs/en/reports/test-fixes-complete-2025.md',
            'new': '../en/reports/test-fixes-complete-2025.md'
        },
        
        # Fix deployment docs
        {
            'file': 'docs/deployment/README.md',
            'old': 'docs/deployment/kubernetes-guide.md',
            'new': 'kubernetes-guide.md'
        },
        {
            'file': 'docs/deployment/docker-guide.md',
            'old': 'docs/api/API_VERSIONING_STRATEGY.md',
            'new': '../api/API_VERSIONING_STRATEGY.md'
        },
        {
            'file': 'docs/deployment/kubernetes-guide.md',
            'old': 'aws-eks-architecture.md',
            'new': 'aws-eks-architecture.md'
        },
        
        # Fix development README
        {
            'file': 'docs/development/README.md',
            'old': 'docs/development/getting-started.md',
            'new': 'getting-started.md'
        },
        {
            'file': 'docs/development/README.md',
            'old': 'docs/development/coding-standards.md',
            'new': 'coding-standards.md'
        },
        {
            'file': 'docs/development/README.md',
            'old': 'docs/development/testing-guide.md',
            'new': 'testing-guide.md'
        },
        {
            'file': 'docs/development/README.md',
            'old': 'docs/development/documentation-guide.md',
            'new': 'documentation-guide.md'
        },
        
        # Fix observability architecture
        {
            'file': 'docs/architecture/observability-architecture.md',
            'old': 'docs/diagrams/aws_infrastructure.mmd',
            'new': '../diagrams/aws_infrastructure.mmd'
        },
        {
            'file': 'docs/architecture/observability-architecture.md',
            'old': 'docs/diagrams/multi_environment.mmd',
            'new': '../diagrams/multi_environment.mmd'
        },
        {
            'file': 'docs/architecture/observability-architecture.md',
            'old': 'docs/diagrams/ddd_architecture.mmd',
            'new': '../diagrams/ddd_architecture.mmd'
        },
        {
            'file': 'docs/architecture/observability-architecture.md',
            'old': 'docs/diagrams/hexagonal_architecture.mmd',
            'new': '../diagrams/hexagonal_architecture.mmd'
        },
    ]
    
    fixed_count = 0
    
    for fix in fixes:
        file_path = fix['file']
        old_link = fix['old']
        new_link = fix['new']
        
        if not os.path.exists(file_path):
            print(f"❌ File not found: {file_path}")
            continue
            
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Replace the link
            if old_link in content:
                new_content = content.replace(old_link, new_link)
                
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                
                fixed_count += 1
                print(f"✅ Fixed: {old_link} → {new_link} in {file_path}")
            else:
                print(f"❓ Link not found: {old_link} in {file_path}")
                
        except Exception as e:
            print(f"❌ Error fixing {file_path}: {e}")
    
    return fixed_count

def create_missing_mmd_files():
    """Create missing .mmd files that are referenced"""
    
    mmd_files = {
        'docs/diagrams/aws_infrastructure.mmd': '''graph TB
    subgraph "AWS Infrastructure"
        EKS[EKS Cluster]
        RDS[RDS Database]
        S3[S3 Storage]
        CloudWatch[CloudWatch]
        ALB[Application Load Balancer]
    end
    
    ALB --> EKS
    EKS --> RDS
    EKS --> S3
    EKS --> CloudWatch
''',
        'docs/diagrams/multi_environment.mmd': '''graph TB
    subgraph "Development"
        DevApp[App]
        DevDB[H2 Database]
    end
    
    subgraph "Staging"
        StageApp[App]
        StageDB[PostgreSQL]
    end
    
    subgraph "Production"
        ProdApp[App]
        ProdDB[RDS PostgreSQL]
        ProdCache[ElastiCache]
    end
    
    DevApp --> DevDB
    StageApp --> StageDB
    ProdApp --> ProdDB
    ProdApp --> ProdCache
''',
        'docs/diagrams/ddd_architecture.mmd': '''graph TB
    subgraph "Domain Layer"
        Aggregates[Aggregate Roots]
        Entities[Entities]
        ValueObjects[Value Objects]
        DomainEvents[Domain Events]
    end
    
    subgraph "Application Layer"
        ApplicationServices[Application Services]
        CommandHandlers[Command Handlers]
        EventHandlers[Event Handlers]
    end
    
    subgraph "Infrastructure Layer"
        Repositories[Repositories]
        ExternalServices[External Services]
        EventStore[Event Store]
    end
    
    ApplicationServices --> Aggregates
    CommandHandlers --> Aggregates
    EventHandlers --> DomainEvents
    Repositories --> Entities
    ExternalServices --> ApplicationServices
''',
        'docs/diagrams/hexagonal_architecture.mmd': '''graph TB
    subgraph "Core Domain"
        Domain[Domain Logic]
        Ports[Ports/Interfaces]
    end
    
    subgraph "Adapters"
        WebAdapter[Web Adapter]
        DatabaseAdapter[Database Adapter]
        MessageAdapter[Message Adapter]
        ExternalAdapter[External Service Adapter]
    end
    
    WebAdapter --> Ports
    Ports --> Domain
    Domain --> Ports
    Ports --> DatabaseAdapter
    Ports --> MessageAdapter
    Ports --> ExternalAdapter
'''
    }
    
    created_count = 0
    
    for file_path, content in mmd_files.items():
        if not os.path.exists(file_path):
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            created_count += 1
            print(f"✅ Created missing file: {file_path}")
    
    return created_count

def main():
    print("🔧 Fixing remaining broken links...")
    
    # Create missing files
    created = create_missing_mmd_files()
    print(f"📁 Created {created} missing files")
    
    # Fix specific links
    fixed = fix_specific_links()
    print(f"🔗 Fixed {fixed} links")
    
    print(f"\n📊 Summary:")
    print(f"✅ Files created: {created}")
    print(f"🔗 Links fixed: {fixed}")

if __name__ == "__main__":
    main()