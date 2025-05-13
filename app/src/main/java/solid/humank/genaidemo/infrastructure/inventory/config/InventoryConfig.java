package solid.humank.genaidemo.infrastructure.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import solid.humank.genaidemo.application.inventory.port.outgoing.ExternalWarehousePort;
import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.application.inventory.service.InventoryApplicationService;
import solid.humank.genaidemo.infrastructure.inventory.external.ExternalWarehouseAdapter;
import solid.humank.genaidemo.infrastructure.inventory.persistence.adapter.InventoryRepositoryAdapter;

/**
 * 庫存配置類
 */
@Configuration
public class InventoryConfig {

    /**
     * 創建庫存應用服務
     */
    @Bean
    public InventoryApplicationService inventoryApplicationService(
            InventoryPersistencePort inventoryPersistencePort,
            ExternalWarehousePort externalWarehousePort) {
        return new InventoryApplicationService(inventoryPersistencePort, externalWarehousePort);
    }

    /**
     * 創建外部倉庫適配器
     */
    @Bean
    public ExternalWarehousePort externalWarehousePort() {
        return new ExternalWarehouseAdapter();
    }
}