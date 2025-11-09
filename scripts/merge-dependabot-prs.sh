#!/bin/bash

# Dependabot PR Merge Script
# This script helps merge Dependabot PRs in phases based on risk level

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if gh CLI is installed
check_gh_cli() {
    if ! command -v gh &> /dev/null; then
        print_error "GitHub CLI (gh) is not installed"
        print_status "Install it from: https://cli.github.com/"
        exit 1
    fi
    
    # Check if authenticated
    if ! gh auth status &> /dev/null; then
        print_error "GitHub CLI is not authenticated"
        print_status "Run: gh auth login"
        exit 1
    fi
    
    print_success "GitHub CLI is ready"
}

# Function to merge a PR
merge_pr() {
    local pr_number=$1
    local description=$2
    
    print_status "Merging PR #${pr_number}: ${description}"
    
    if gh pr merge "$pr_number" --squash --auto; then
        print_success "PR #${pr_number} merged successfully"
        return 0
    else
        print_error "Failed to merge PR #${pr_number}"
        return 1
    fi
}

# Function to check PR status
check_pr_status() {
    local pr_number=$1
    
    local status=$(gh pr view "$pr_number" --json state,statusCheckRollup --jq '.state')
    
    if [ "$status" != "OPEN" ]; then
        print_warning "PR #${pr_number} is not open (status: ${status})"
        return 1
    fi
    
    return 0
}

# Main script
main() {
    echo "================================================"
    echo "  Dependabot PR Merge Script"
    echo "================================================"
    echo ""
    
    check_gh_cli
    
    # Ask user which phase to execute
    echo ""
    echo "Select merge phase:"
    echo "  1) Phase 1: Security Updates (HIGH PRIORITY)"
    echo "  2) Phase 2: Low-Risk Updates"
    echo "  3) Phase 3: Test Major Updates (Manual)"
    echo "  4) Phase 4: Bulk Merge Remaining"
    echo "  5) Dry Run (Show what would be merged)"
    echo "  6) Exit"
    echo ""
    read -p "Enter choice [1-6]: " choice
    
    case $choice in
        1)
            echo ""
            print_status "=== Phase 1: Security Updates ==="
            echo ""
            
            merge_pr 109 "Jinja2 3.1.2 → 3.1.6 (Security)"
            merge_pr 108 "Requests 2.31.0 → 2.32.4 (Security)"
            merge_pr 106 "urllib3 2.1.0 → 2.5.0 (Security)"
            merge_pr 121 "AWS SDK BOM 2.21.29 → 2.36.2"
            
            print_success "Phase 1 complete!"
            ;;
            
        2)
            echo ""
            print_status "=== Phase 2: Low-Risk Updates ==="
            echo ""
            
            print_status "Merging GitHub Actions updates..."
            merge_pr 76 "actions/checkout 4 → 5"
            merge_pr 75 "actions/github-script 7 → 8"
            merge_pr 74 "dorny/test-reporter 1 → 2"
            
            print_status "Merging infrastructure updates..."
            merge_pr 123 "aws-cdk 2.1029.2 → 2.1031.0"
            merge_pr 115 "cdk-nag 2.37.34 → 2.37.55"
            
            print_status "Merging minor dependency updates..."
            merge_pr 120 "vite 5.4.20 → 5.4.21"
            merge_pr 114 "zod 4.1.8 → 4.1.12"
            merge_pr 91 "jest 30.1.3 → 30.2.0"
            merge_pr 78 "next 14.2.32 → 14.2.33"
            
            print_success "Phase 2 complete!"
            ;;
            
        3)
            echo ""
            print_warning "=== Phase 3: Major Updates (Requires Testing) ==="
            echo ""
            print_warning "These PRs require manual testing before merging:"
            echo ""
            echo "  PR #119: Node 24 → 25 (Consumer Frontend)"
            echo "  PR #118: Node 24 → 25 (CMC Frontend)"
            echo "  PR #97:  Eclipse Temurin 21 → 25 (Docker)"
            echo "  PR #96:  PrimeNG 18.0.2 → 20.2.0"
            echo "  PR #88:  Zone.js 0.14.10 → 0.15.1"
            echo ""
            print_status "To test a PR:"
            echo "  1. gh pr checkout <PR_NUMBER>"
            echo "  2. Run tests: ./gradlew test"
            echo "  3. Test manually in staging"
            echo "  4. gh pr merge <PR_NUMBER> --squash"
            echo ""
            ;;
            
        4)
            echo ""
            print_status "=== Phase 4: Bulk Merge Remaining ==="
            echo ""
            print_warning "This will merge all remaining low-risk PRs"
            read -p "Are you sure? (yes/no): " confirm
            
            if [ "$confirm" != "yes" ]; then
                print_status "Cancelled"
                exit 0
            fi
            
            print_status "Merging frontend dependencies..."
            merge_pr 122 "tailwindcss 3.4.17 → 4.1.16"
            merge_pr 117 "playwright updates"
            merge_pr 103 "typescript 5.5.4 → 5.9.3 (consumer)"
            merge_pr 101 "typescript 5.6.3 → 5.9.3 (infra)"
            merge_pr 99 "jasmine-core 5.10.0 → 5.12.0"
            merge_pr 83 "@types/node 24.3.1 → 24.5.2"
            merge_pr 80 "@tanstack/react-query-devtools"
            merge_pr 79 "lucide-react 0.424.0 → 0.544.0"
            
            print_status "Merging backend dependencies..."
            merge_pr 107 "black 23.12.1 → 24.3.0"
            merge_pr 84 "httpcore5 5.2.4 → 5.3.6"
            merge_pr 82 "httpclient5-fluent 5.3.1 → 5.5.1"
            merge_pr 81 "allure-java-commons 2.22.1 → 2.30.0"
            merge_pr 77 "mockito-core 5.8.0 → 5.20.0"
            
            print_success "Phase 4 complete!"
            ;;
            
        5)
            echo ""
            print_status "=== Dry Run: Showing PR Status ==="
            echo ""
            
            gh pr list --limit 50 --json number,title,state,statusCheckRollup \
                --jq '.[] | "PR #\(.number): \(.title) - \(.state)"'
            
            echo ""
            print_status "No PRs were merged (dry run mode)"
            ;;
            
        6)
            print_status "Exiting..."
            exit 0
            ;;
            
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
    
    echo ""
    echo "================================================"
    print_success "Script completed successfully!"
    echo "================================================"
}

# Run main function
main
