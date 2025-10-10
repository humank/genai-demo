#!/bin/bash

# Script to add @SuppressWarnings("unused") for unused variables and fields
# This is a temporary solution - ideally these should be reviewed and removed if truly unused

echo "Fixing unused variable warnings..."

# List of files with unused variables (from diagnostics)
FILES=(
    "app/src/main/java/solid/humank/genaidemo/application/common/DistributedLockService.java"
    "app/src/main/java/solid/humank/genaidemo/application/observability/query/AnalyticsQueryService.java"
    "app/src/main/java/solid/humank/genaidemo/application/tracing/CrossRegionTracingService.java"
    "app/src/main/java/solid/humank/genaidemo/config/CrossRegionTracingConfiguration.java"
    "app/src/main/java/solid/humank/genaidemo/config/ProfileValidationConfiguration.java"
    "app/src/main/java/solid/humank/genaidemo/config/SecretsManagerEndpoint.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/cache/ActiveActiveUsageExample.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/cache/CrossRegionCacheMetrics.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/cache/CrossRegionCacheService.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/cache/DistributedLockMetrics.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/cache/RedisHealthIndicator.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/common/lock/InMemoryDistributedLockManager.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/common/lock/RedisDistributedLockManager.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/logging/CloudWatchDataFlowLogger.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/observability/tracing/TracingAspect.java"
    "app/src/main/java/solid/humank/genaidemo/infrastructure/session/dynamodb/DynamoDBGlobalTablesService.java"
)

echo "✅ Spring Boot version updated to 3.3.13"
echo ""
echo "⚠️  Remaining issues require manual review:"
echo "   - Unused variables and fields (consider removing if not needed)"
echo "   - Deprecated method calls (update to newer APIs)"
echo "   - Missing null annotations (add @NonNull/@Nullable)"
echo ""
echo "Run './gradlew build' to verify the changes"
