package solid.humank.genaidemo.interfaces.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationManager;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationValidator;
import solid.humank.genaidemo.infrastructure.config.DatabaseHealthService;

/**
 * Database Health Controller
 * Provides REST endpoints for database health and configuration information
 */
@RestController
@RequestMapping("/api/database")
@Tag(name = "系統監控", description = "系統健康檢查和配置資訊相關的 API 操作")
public class DatabaseHealthController {
    
    private final DatabaseHealthService databaseHealthService;
    private final DatabaseConfigurationManager databaseConfigurationManager;
    private final DatabaseConfigurationValidator databaseConfigurationValidator;
    
    public DatabaseHealthController(DatabaseHealthService databaseHealthService,
                                  DatabaseConfigurationManager databaseConfigurationManager,
                                  DatabaseConfigurationValidator databaseConfigurationValidator) {
        this.databaseHealthService = databaseHealthService;
        this.databaseConfigurationManager = databaseConfigurationManager;
        this.databaseConfigurationValidator = databaseConfigurationValidator;
    }
    
    /**
     * Get database health status
     */
    @GetMapping("/health")
    @Operation(summary = "獲取資料庫健康狀態", description = "檢查資料庫連線狀態和基本健康資訊")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "資料庫狀態正常", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = DatabaseHealthService.DatabaseHealthStatus.class))),
        @ApiResponse(responseCode = "503", description = "資料庫服務不可用", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = DatabaseHealthService.DatabaseHealthStatus.class))),
        @ApiResponse(responseCode = "500", description = "內部伺服器錯誤", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = StandardErrorResponse.class)))
    })
    public ResponseEntity<DatabaseHealthService.DatabaseHealthStatus> getHealth() {
        DatabaseHealthService.DatabaseHealthStatus health = databaseHealthService.checkHealth();
        
        if ("UP".equals(health.status())) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }
    
    /**
     * Get database configuration information
     */
    @GetMapping("/config")
    @Operation(summary = "獲取資料庫配置資訊", description = "獲取當前資料庫配置和連線參數資訊")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取資料庫配置資訊", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = DatabaseConfigurationManager.DatabaseHealthInfo.class))),
        @ApiResponse(responseCode = "500", description = "內部伺服器錯誤", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = StandardErrorResponse.class)))
    })
    public ResponseEntity<DatabaseConfigurationManager.DatabaseHealthInfo> getConfiguration() {
        DatabaseConfigurationManager.DatabaseHealthInfo config = 
            databaseConfigurationManager.getDatabaseHealthInfo();
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Get database validation report
     */
    @GetMapping("/validation")
    @Operation(summary = "獲取資料庫驗證報告", description = "獲取資料庫結構和配置的完整驗證報告")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取資料庫驗證報告", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = DatabaseConfigurationValidator.DatabaseValidationReport.class))),
        @ApiResponse(responseCode = "500", description = "內部伺服器錯誤", 
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = StandardErrorResponse.class)))
    })
    public ResponseEntity<DatabaseConfigurationValidator.DatabaseValidationReport> getValidationReport() {
        DatabaseConfigurationValidator.DatabaseValidationReport report = 
            databaseConfigurationValidator.getValidationReport();
        
        return ResponseEntity.ok(report);
    }
}