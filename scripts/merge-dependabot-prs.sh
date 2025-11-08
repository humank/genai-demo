#!/bin/bash
#
# Merge Dependabot PRs that pass CI checks
#
# Usage: ./scripts/merge-dependabot-prs.sh [--dry-run]
#

set -e

DRY_RUN=false
if [ "$1" == "--dry-run" ]; then
    DRY_RUN=true
    echo "🔍 DRY RUN MODE - No PRs will be merged"
    echo ""
fi

REPO="humank/genai-demo"

echo "📋 Fetching open Dependabot PRs..."
echo ""

# Get all open Dependabot PRs
PRS=$(gh pr list --repo "$REPO" --author "app/dependabot" --state open --json number,title,statusCheckRollup --limit 100)

# Count total PRs
TOTAL=$(echo "$PRS" | jq '. | length')
echo "Found $TOTAL Dependabot PRs"
echo ""

if [ "$TOTAL" -eq 0 ]; then
    echo "✅ No Dependabot PRs to process"
    exit 0
fi

MERGED=0
SKIPPED=0
FAILED=0

# Process each PR
echo "$PRS" | jq -c '.[]' | while read -r pr; do
    PR_NUMBER=$(echo "$pr" | jq -r '.number')
    PR_TITLE=$(echo "$pr" | jq -r '.title')
    
    echo "---"
    echo "PR #$PR_NUMBER: $PR_TITLE"
    
    # Check if all required checks passed
    CHECKS=$(echo "$pr" | jq -r '.statusCheckRollup')
    
    if [ "$CHECKS" == "null" ] || [ "$CHECKS" == "[]" ]; then
        echo "⏳ No checks completed yet, skipping"
        ((SKIPPED++))
        continue
    fi
    
    # Check for any failures (excluding staging tests which are skipped)
    FAILURES=$(echo "$CHECKS" | jq '[.[] | select(.conclusion == "FAILURE" and (.name | contains("Staging") | not))] | length')
    
    if [ "$FAILURES" -gt 0 ]; then
        echo "❌ Has $FAILURES failing checks, skipping"
        ((FAILED++))
        continue
    fi
    
    # Check if all non-staging checks are successful
    SUCCESS=$(echo "$CHECKS" | jq '[.[] | select(.name | contains("Staging") | not) | select(.conclusion == "SUCCESS")] | length')
    PENDING=$(echo "$CHECKS" | jq '[.[] | select(.status == "IN_PROGRESS" or .status == "QUEUED")] | length')
    
    if [ "$PENDING" -gt 0 ]; then
        echo "⏳ Has $PENDING pending checks, skipping"
        ((SKIPPED++))
        continue
    fi
    
    if [ "$SUCCESS" -eq 0 ]; then
        echo "⚠️  No successful checks found, skipping"
        ((SKIPPED++))
        continue
    fi
    
    # Merge the PR
    if [ "$DRY_RUN" == "true" ]; then
        echo "🔍 [DRY RUN] Would merge PR #$PR_NUMBER"
        ((MERGED++))
    else
        echo "✅ Merging PR #$PR_NUMBER..."
        if gh pr merge "$PR_NUMBER" --repo "$REPO" --squash --auto; then
            echo "✅ Successfully merged PR #$PR_NUMBER"
            ((MERGED++))
        else
            echo "❌ Failed to merge PR #$PR_NUMBER"
            ((FAILED++))
        fi
    fi
    
    # Rate limiting
    sleep 2
done

echo ""
echo "================================"
echo "📊 Summary"
echo "================================"
echo "Total PRs: $TOTAL"
echo "Merged: $MERGED"
echo "Skipped: $SKIPPED"
echo "Failed: $FAILED"
echo ""

if [ "$DRY_RUN" == "true" ]; then
    echo "🔍 This was a dry run. Run without --dry-run to actually merge PRs."
fi
