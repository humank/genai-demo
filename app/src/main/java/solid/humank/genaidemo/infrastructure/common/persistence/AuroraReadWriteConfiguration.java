package solid.humank.genaidemo.infrastructure.common.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aurora 讀寫分離配置
 *
 * 提供 Aurora Global Database 的讀寫端點分離配置，包含：
 * 1. 寫入端點配置（主要端點）
 * 2. 讀取端點配置（只讀副本）
 * 3. 動態路由邏輯
 * 4. 事務感知的端點選擇
 *
 * 配置說明：
 * - 寫入操作自動路由到寫入端點
 * - 只讀操作自動路由到讀取端點
 * - 事務中的操作統一使用寫入端點
 * - 支援手動指定端點類型
 *
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 *
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(name = "aurora.read-write-separation.enabled", havingValue = "true", matchIfMissing = false)
public class AuroraReadWriteConfiguration {

    /**
     * 數據源類型枚舉
     */
    public enum DataSourceType {
        WRITE("write"),
        READ("read");

        private final String key;

        DataSourceType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * 寫入數據源配置
     * 連接到 Aurora 主要端點
     */
    @Bean
    @ConfigurationProperties(prefix = "aurora.datasource.write")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 讀取數據源配置
     * 連接到 Aurora 只讀副本端點
     */
    @Bean
    @ConfigurationProperties(prefix = "aurora.datasource.read")
    public DataSource readDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 動態路由數據源
     * 根據當前操作類型自動選擇合適的數據源
     */
    @Bean
    @Primary
    public DataSource routingDataSource(@Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        AuroraRoutingDataSource routingDataSource = new AuroraRoutingDataSource();

        // 設定目標數據源
        java.util.Map<Object, Object> targetDataSources = new java.util.HashMap<>();
        targetDataSources.put(DataSourceType.WRITE.getKey(), writeDataSource);
        targetDataSources.put(DataSourceType.READ.getKey(), readDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);

        // 設定默認數據源為寫入數據源
        // Spring framework API accepts DataSource as Object
        // writeDataSource is guaranteed non-null by Spring's @Bean contract
        routingDataSource.setDefaultTargetDataSource((Object) java.util.Objects.requireNonNull(writeDataSource));

        return routingDataSource;
    }

    /**
     * Aurora 動態路由數據源實現
     */
    public static class AuroraRoutingDataSource extends AbstractRoutingDataSource {

        private static final ThreadLocal<DataSourceType> currentDataSourceType = new ThreadLocal<>();

        /**
         * 設定當前線程使用的數據源類型
         *
         * @param dataSourceType 數據源類型
         */
        public static void setDataSourceType(DataSourceType dataSourceType) {
            currentDataSourceType.set(dataSourceType);
        }

        /**
         * 獲取當前線程使用的數據源類型
         *
         * @return 數據源類型
         */
        public static DataSourceType getDataSourceType() {
            return currentDataSourceType.get();
        }

        /**
         * 清除當前線程的數據源類型
         */
        public static void clearDataSourceType() {
            currentDataSourceType.remove();
        }

        /**
         * 確定當前查找鍵
         * 根據事務狀態和手動設定自動選擇數據源
         */
        @Override
        protected Object determineCurrentLookupKey() {
            // 1. 檢查是否手動設定了數據源類型
            DataSourceType manualType = getDataSourceType();
            if (manualType != null) {
                return manualType.getKey();
            }

            // 2. 檢查是否在事務中
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                // 事務中統一使用寫入數據源，確保一致性
                return DataSourceType.WRITE.getKey();
            }

            // 3. 檢查是否為只讀事務
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                return DataSourceType.READ.getKey();
            }

            // 4. 默認使用寫入數據源
            return DataSourceType.WRITE.getKey();
        }
    }

    /**
     * 數據源路由工具類
     */
    public static class DataSourceRouter {

        private DataSourceRouter() {
            // Private constructor to prevent instantiation
        }

        /**
         * 在指定的數據源類型下執行操作
         *
         * @param dataSourceType 數據源類型
         * @param operation      要執行的操作
         * @param <T>            返回類型
         * @return 操作結果
         */
        public static <T> T executeWithDataSource(DataSourceType dataSourceType,
                java.util.function.Supplier<T> operation) {
            DataSourceType previousType = AuroraRoutingDataSource.getDataSourceType();
            try {
                AuroraRoutingDataSource.setDataSourceType(dataSourceType);
                return operation.get();
            } finally {
                if (previousType != null) {
                    AuroraRoutingDataSource.setDataSourceType(previousType);
                } else {
                    AuroraRoutingDataSource.clearDataSourceType();
                }
            }
        }

        /**
         * 在讀取數據源下執行操作
         *
         * @param operation 要執行的操作
         * @param <T>       返回類型
         * @return 操作結果
         */
        public static <T> T executeWithReadDataSource(java.util.function.Supplier<T> operation) {
            return executeWithDataSource(DataSourceType.READ, operation);
        }

        /**
         * 在寫入數據源下執行操作
         *
         * @param operation 要執行的操作
         * @param <T>       返回類型
         * @return 操作結果
         */
        public static <T> T executeWithWriteDataSource(java.util.function.Supplier<T> operation) {
            return executeWithDataSource(DataSourceType.WRITE, operation);
        }

        /**
         * 在指定的數據源類型下執行無返回值操作
         *
         * @param dataSourceType 數據源類型
         * @param operation      要執行的操作
         */
        public static void executeWithDataSource(DataSourceType dataSourceType, Runnable operation) {
            executeWithDataSource(dataSourceType, () -> {
                operation.run();
                return null;
            });
        }

        /**
         * 在讀取數據源下執行無返回值操作
         *
         * @param operation 要執行的操作
         */
        public static void executeWithReadDataSource(Runnable operation) {
            executeWithDataSource(DataSourceType.READ, operation);
        }

        /**
         * 在寫入數據源下執行無返回值操作
         *
         * @param operation 要執行的操作
         */
        public static void executeWithWriteDataSource(Runnable operation) {
            executeWithDataSource(DataSourceType.WRITE, operation);
        }
    }
}
