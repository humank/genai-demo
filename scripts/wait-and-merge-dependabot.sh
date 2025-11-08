#!/bin/bash
#
# Wait for Dependabot PRs to finish rebasing and testing, then merge them
#
# Usage: ./scripts/wait-and-merge-dependabot.sh
#

set -e

REPO="humank/genai-demo"
MAX_WAIT_MINUTES=30
CHECK_INTERVAL_SECONDS=60

echo "🔄 Waiting for Dependabot PRs to rebase and pass checks..."
echo "Will check every $CHECK_INTERVAL_SECONDS seconds for up to $MAX_WAIT_MINUTES minutes"
echo ""

ELAPSED=0
while [ $ELAPSED -lt $((MAX_WAIT_MINUTES * 60)) ]; do
    echo "⏱️  Elapsed: $((ELAPSED / 60)) minutes"
    
    # Check how many PRs are ready to merge
    READY_COUNT=$(gh pr list --repo "$REPO" --author "app/dependabot" --state open --json number,statusCheckRollup --limit 50 | \
        jq '[.[] | select(.statusCheckRollup != null and .statusCheckRollup != []) | 
             select([.statusCheckRollup[] | select(.status == "IN_PROGRESS" or .status == "QUEUED")] | length == 0) |
             select([.statusCheckRollup[] | select(.conclusion == "FAILURE" and (.name | contains("Staging") | not))] | length == 0)] | length')
    
    echo "✅ $READY_COUNT PRs are ready to merge"
    
    if [ "$READY_COUNT" -gt 0 ]; then
        echo ""
        echo "🎉 Found $READY_COUNT PRs ready to merge!"
        echo "Starting merge process..."
        echo ""
        ./scripts/merge-dependabot-prs.sh
        exit 0
    fi
    
    echo "⏳ Waiting $CHECK_INTERVAL_SECONDS seconds before next check..."
    echo ""
    sleep $CHECK_INTERVAL_SECONDS
    ELAPSED=$((ELAPSED + CHECK_INTERVAL_SECONDS))
done

echo "⏰ Timeout reached after $MAX_WAIT_MINUTES minutes"
echo "Some PRs may still be processing. Run this script again or merge manually."
exit 1
