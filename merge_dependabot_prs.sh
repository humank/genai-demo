#!/bin/bash

# Batch merge Dependabot PRs
# This script merges all mergeable Dependabot PRs

set -e

echo "🤖 Starting batch merge of Dependabot PRs..."

# Get all mergeable PR numbers
MERGEABLE_PRS=$(gh pr list --state open --json number,mergeable,mergeStateStatus --jq '.[] | select(.mergeable == "MERGEABLE" and .mergeStateStatus == "CLEAN") | .number' | sort -n)

if [ -z "$MERGEABLE_PRS" ]; then
    echo "❌ No mergeable PRs found"
    exit 1
fi

echo "📋 Found mergeable PRs: $(echo $MERGEABLE_PRS | tr '\n' ' ')"

# Counter for tracking progress
TOTAL_PRS=$(echo "$MERGEABLE_PRS" | wc -l)
CURRENT=0
SUCCESSFUL=0
FAILED=0

echo "🚀 Starting to merge $TOTAL_PRS PRs..."

# Merge each PR
for PR_NUMBER in $MERGEABLE_PRS; do
    CURRENT=$((CURRENT + 1))
    echo ""
    echo "[$CURRENT/$TOTAL_PRS] Processing PR #$PR_NUMBER..."
    
    # Get PR title for better logging
    PR_TITLE=$(gh pr view $PR_NUMBER --json title --jq '.title')
    echo "  📝 Title: $PR_TITLE"
    
    # Attempt to merge
    if gh pr merge $PR_NUMBER --squash --delete-branch; then
        echo "  ✅ Successfully merged PR #$PR_NUMBER"
        SUCCESSFUL=$((SUCCESSFUL + 1))
        
        # Small delay to avoid rate limiting
        sleep 2
    else
        echo "  ❌ Failed to merge PR #$PR_NUMBER"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "🎉 Batch merge completed!"
echo "📊 Summary:"
echo "  - Total PRs processed: $TOTAL_PRS"
echo "  - Successfully merged: $SUCCESSFUL"
echo "  - Failed to merge: $FAILED"

if [ $FAILED -eq 0 ]; then
    echo "✅ All PRs merged successfully!"
    exit 0
else
    echo "⚠️  Some PRs failed to merge. Please check manually."
    exit 1
fi