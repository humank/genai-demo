#!/usr/bin/env python3
"""
Fix broken links in markdown files based on link check report
"""

import os
import re
import json
import glob
from pathlib import Path

def load_broken_links():
    """Load broken links from the report"""
    report_path = "build/reports/documentation-quality/advanced-link-check.json"
    if not os.path.exists(report_path):
        print("❌ Link check report not found. Please run link checker first.")
        return []
    
    with open(report_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    broken_links = []
    files_data = data.get('files', {})
    
    for file_path, file_info in files_data.items():
        if file_info.get('brokenLinks', 0) > 0:
            for link in file_info.get('links', []):
                if not link.get('valid', True):
                    broken_links.append({
                        'file': file_path,
                        'link': link['url'],
                        'line': link.get('line', 0),
                        'reason': link.get('details', {}).get('error', 'Unknown error')
                    })
    
    return broken_links

def find_actual_file(broken_link):
    """Try to find the actual location of a broken link"""
    # Remove leading ./ and ../
    clean_link = broken_link.lstrip('./').lstrip('../')
    
    # Common patterns to fix
    fixes = {
        # Double path issues
        'reports-summaries/project-management/reports-summaries/project-management/': 'reports-summaries/project-management/',
        'docs/reports/reports-summaries/': 'reports-summaries/',
        'diagrams/reports-summaries/': 'reports-summaries/',
        'reports/reports-summaries/': 'reports-summaries/',
        
        # Moved files
        'docs/observability/production-observability-testing-guide.md': 'docs/viewpoints/operational/production-observability-testing-guide.md',
        'docs/architecture/ddd-guide.md': 'docs/design/ddd-guide.md',
        'docs/architecture/event-driven-design.md': 'docs/design/event-driven-architecture.md',
        
        # Missing ADR files - create placeholder or remove reference
        'ADR-004-spring-boot-profiles.md': None,
        'ADR-006-multi-region-architecture.md': None,
        'ADR-007-container-orchestration.md': None,
        'ADR-008-database-strategy.md': None,
        'ADR-009-event-streaming-platform.md': None,
        'ADR-010-observability-integration.md': None,
        'ADR-014-gitops-platform.md': None,
        'ADR-015-automated-rollback.md': None,
        
        # Diagram files
        'diagrams/plantuml/pricing-sequence-diagram.svg': 'docs/diagrams/plantuml/pricing-sequence-diagram.svg',
        'diagrams/plantuml/delivery-sequence-diagram.svg': 'docs/diagrams/plantuml/delivery-sequence-diagram.svg',
    }
    
    # Apply direct fixes
    for pattern, replacement in fixes.items():
        if pattern in clean_link:
            if replacement is None:
                return None  # Mark for removal
            return clean_link.replace(pattern, replacement)
    
    # Try to find file by name
    filename = os.path.basename(clean_link)
    if filename:
        # Search for the file in the project
        for root, dirs, files in os.walk('.'):
            # Skip node_modules and build directories
            if 'node_modules' in root or 'build' in root:
                continue
            if filename in files:
                found_path = os.path.join(root, filename).lstrip('./')
                return found_path
    
    return None

def fix_link_in_file(file_path, old_link, new_link):
    """Fix a specific link in a file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Escape special regex characters in the old link
        escaped_old = re.escape(old_link)
        
        # Try different markdown link patterns
        patterns = [
            f'\\[([^\\]]+)\\]\\({escaped_old}\\)',  # [text](link)
            f'\\[([^\\]]+)\\]\\({re.escape("./" + old_link)}\\)',  # [text](./link)
            f'\\[([^\\]]+)\\]\\({re.escape("../" + old_link)}\\)',  # [text](../link)
        ]
        
        updated = False
        for pattern in patterns:
            if new_link is None:
                # Remove the link entirely
                new_content = re.sub(pattern, r'\\1', content)
            else:
                # Replace with new link
                new_content = re.sub(pattern, f'[\\1]({new_link})', content)
            
            if new_content != content:
                content = new_content
                updated = True
                break
        
        if updated:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        
    except Exception as e:
        print(f"❌ Error fixing link in {file_path}: {e}")
    
    return False

def create_missing_files():
    """Create missing files that are commonly referenced"""
    missing_files = {
        'docs/development/getting-started.md': '''# Getting Started

This guide helps you get started with the project development.

## Prerequisites

- Java 21
- Node.js 18+
- Docker
- AWS CLI

## Setup

1. Clone the repository
2. Run `./gradlew build`
3. Start the application with `docker-compose up`

For more details, see [DEVELOPER_QUICKSTART.md](../DEVELOPER_QUICKSTART.md).
''',
        'docs/development/coding-standards.md': '''# Coding Standards

Please refer to our comprehensive coding standards in the steering rules:

- [Development Standards](../.kiro/steering/development-standards.md)
- [Code Review Standards](../.kiro/steering/code-review-standards.md)
- [Security Standards](../.kiro/steering/security-standards.md)
''',
        'docs/development/testing-guide.md': '''# Testing Guide

Please refer to our comprehensive testing guidelines:

- [Development Standards - Testing](../.kiro/steering/development-standards.md#testing-standards)
- [Test Performance Standards](../.kiro/steering/test-performance-standards.md)
- [Performance Standards](../.kiro/steering/performance-standards.md)
''',
        'docs/development/documentation-guide.md': '''# Documentation Guide

Please refer to our documentation standards:

- [Translation Guide](../.kiro/steering/translation-guide.md)
- [Documentation Maintenance Guide](../DOCUMENTATION_MAINTENANCE_GUIDE.md)
- [Reports Organization Standards](../.kiro/steering/reports-organization-standards.md)
''',
        'docs/deployment/kubernetes-guide.md': '''# Kubernetes Deployment Guide

For Kubernetes deployment, please refer to:

- [AWS EKS Architecture](aws-eks-architecture.md)
- [Production Deployment Checklist](production-deployment-checklist.md)
- [Observability Deployment](observability-deployment.md)
''',
    }
    
    for file_path, content in missing_files.items():
        if not os.path.exists(file_path):
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Created missing file: {file_path}")

def main():
    print("🔧 Starting broken link fix process...")
    
    # Create missing files first
    create_missing_files()
    
    # Load broken links
    broken_links = load_broken_links()
    if not broken_links:
        print("❌ No broken links data found.")
        return
    
    print(f"📊 Found {len(broken_links)} broken links to fix")
    
    fixed_count = 0
    removed_count = 0
    not_found_count = 0
    
    for broken in broken_links:
        file_path = broken['file']
        old_link = broken['link']
        
        # Skip external links
        if old_link.startswith('http'):
            continue
        
        # Find the correct path
        new_link = find_actual_file(old_link)
        
        if new_link is None:
            # Remove the link
            if fix_link_in_file(file_path, old_link, None):
                removed_count += 1
                print(f"🗑️  Removed broken link: {old_link} from {file_path}")
        elif new_link != old_link and os.path.exists(new_link):
            # Fix the link
            if fix_link_in_file(file_path, old_link, new_link):
                fixed_count += 1
                print(f"✅ Fixed: {old_link} → {new_link} in {file_path}")
        else:
            not_found_count += 1
            print(f"❓ Could not find: {old_link} in {file_path}")
    
    print(f"\n📊 Fix Summary:")
    print(f"✅ Fixed links: {fixed_count}")
    print(f"🗑️  Removed links: {removed_count}")
    print(f"❓ Not found: {not_found_count}")
    print(f"📝 Total processed: {len(broken_links)}")

if __name__ == "__main__":
    main()