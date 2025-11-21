#!/bin/bash

# Java Code Issues Batch Fix Script
# This script fixes common Java code quality issues

echo "🔧 Starting Java code issues fix..."

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter
FIXED=0

echo -e "${YELLOW}Phase 1: Removing unused imports${NC}"

# Fix CrossRegionTracingService
sed -i '' '/import com.amazonaws.xray.entities.Subsegment;/d' \
  app/src/main/java/solid/humank/genaidemo/application/tracing/CrossRegionTracingService.java && \
  echo "✅ Fixed CrossRegionTracingService.java" && ((FIXED++))

# Fix UnifiedDataSourceConfiguration
sed -i '' '/import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;/d' \
  app/src/main/java/solid/humank/genaidemo/config/UnifiedDataSourceConfiguration.java && \
  echo "✅ Fixed UnifiedDataSourceConfiguration.java" && ((FIXED++))

# Fix OptimisticLockingConflictDetector
sed -i '' '/import java.util.Optional;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/common/persistence/OptimisticLockingConflictDetector.java && \
  echo "✅ Fixed OptimisticLockingConflictDetector.java" && ((FIXED++))

# Fix RedisDistributedLockManager
sed -i '' '/import java.time.Instant;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/common/lock/RedisDistributedLockManager.java && \
  echo "✅ Fixed RedisDistributedLockManager.java" && ((FIXED++))

# Fix ReadOnlyOperationAspect
sed -i '' '/import solid.humank.genaidemo.infrastructure.common.persistence.AuroraReadWriteConfiguration.DataSourceType;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/common/persistence/ReadOnlyOperationAspect.java && \
  echo "✅ Fixed ReadOnlyOperationAspect.java" && ((FIXED++))

# Fix CloudWatchMetricsConfig
sed -i '' '/import io.micrometer.core.instrument.Tag;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/CloudWatchMetricsConfig.java
sed -i '' '/import java.util.Map;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/CloudWatchMetricsConfig.java && \
  echo "✅ Fixed CloudWatchMetricsConfig.java" && ((FIXED++))

# Fix EventProcessingConfig
sed -i '' '/import java.util.concurrent.Executor;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/EventProcessingConfig.java
sed -i '' '/import org.springframework.boot.actuate.metrics.MetricsEndpoint;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/EventProcessingConfig.java && \
  echo "✅ Fixed EventProcessingConfig.java" && ((FIXED++))

# Fix ProductionDatabaseConfiguration
sed -i '' '/import org.springframework.boot.jdbc.DataSourceBuilder;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/ProductionDatabaseConfiguration.java && \
  echo "✅ Fixed ProductionDatabaseConfiguration.java" && ((FIXED++))

# Fix RedisProperties
sed -i '' '/import jakarta.validation.constraints.NotEmpty;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/RedisProperties.java && \
  echo "✅ Fixed RedisProperties.java" && ((FIXED++))

# Fix XRayTracingConfig
sed -i '' '/import com.amazonaws.xray.strategy.ContextMissingStrategy;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/XRayTracingConfig.java
sed -i '' '/import org.springframework.web.servlet.config.annotation.InterceptorRegistry;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/config/XRayTracingConfig.java && \
  echo "✅ Fixed XRayTracingConfig.java" && ((FIXED++))

# Fix JPA Entity files
sed -i '' '/import java.time.LocalDateTime;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/inventory/persistence/entity/JpaInventoryEntity.java && \
  echo "✅ Fixed JpaInventoryEntity.java" && ((FIXED++))

sed -i '' '/import java.time.LocalDateTime;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/order/persistence/entity/JpaOrderEntity.java && \
  echo "✅ Fixed JpaOrderEntity.java" && ((FIXED++))

sed -i '' '/import java.time.LocalDateTime;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/payment/persistence/entity/JpaPaymentEntity.java && \
  echo "✅ Fixed JpaPaymentEntity.java" && ((FIXED++))

sed -i '' '/import java.time.LocalDateTime;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/shoppingcart/persistence/entity/JpaShoppingCartEntity.java && \
  echo "✅ Fixed JpaShoppingCartEntity.java" && ((FIXED++))

# Fix BusinessMetricsService
sed -i '' '/import java.util.concurrent.atomic.AtomicInteger;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/metrics/BusinessMetricsService.java && \
  echo "✅ Fixed BusinessMetricsService.java" && ((FIXED++))

# Fix ResilientServiceWrapper
sed -i '' '/import io.github.resilience4j.timelimiter.TimeLimiter;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/resilience/ResilientServiceWrapper.java
sed -i '' '/import java.util.concurrent.TimeoutException;/d' \
  app/src/main/java/solid/humank/genaidemo/infrastructure/resilience/ResilientServiceWrapper.java && \
  echo "✅ Fixed ResilientServiceWrapper.java" && ((FIXED++))

# Fix Test files
sed -i '' '/import org.mockito.Mockito.doThrow;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingTest.java
sed -i '' '/import org.mockito.Mockito.mock;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingTest.java
sed -i '' '/import java.util.concurrent.CountDownLatch;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingTest.java
sed -i '' '/import java.util.concurrent.ExecutorService;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingTest.java
sed -i '' '/import java.util.concurrent.Executors;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingTest.java && \
  echo "✅ Fixed AuroraOptimisticLockingTest.java" && ((FIXED++))

sed -i '' '/import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;/d' \
  app/src/test/java/solid/humank/genaidemo/infrastructure/config/TestMetricsConfiguration.java && \
  echo "✅ Fixed TestMetricsConfiguration.java" && ((FIXED++))

sed -i '' '/import java.time.Instant;/d' \
  app/src/test/java/solid/humank/genaidemo/testutils/builders/OptimizedTestDataBuilders.java && \
  echo "✅ Fixed OptimizedTestDataBuilders.java" && ((FIXED++))

echo -e "\n${GREEN}✅ Fixed $FIXED files${NC}"
echo -e "${YELLOW}Note: Some issues require manual review (unused fields, methods, etc.)${NC}"
