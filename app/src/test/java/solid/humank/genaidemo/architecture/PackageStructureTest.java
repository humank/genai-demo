package solid.humank.genaidemo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 確保專案的包結構符合 DDD 最佳實踐
 * 
 * 包結構規則：
 * 1. solid.humank.genaidemo.domain.common - 通用和共用的物件
 * 2. solid.humank.genaidemo.domain.order 和 solid.humank.genaidemo.domain.payment - 子領域
 *    - 子領域下的 model 包含 DDD 戰術設計中的領域模型層元素
 */
public class PackageStructureTest {

    private static final String BASE_PACKAGE = "solid.humank.genaidemo";
    private static final String DOMAIN_COMMON_PACKAGE = "solid.humank.genaidemo.domain.common";
    private static JavaClasses importedClasses;
    
    // 允許特定類不遵循包結構規則的例外列表
    private static final List<String> COMMON_EXCEPTIONS = Arrays.asList(
        "solid.humank.genaidemo.domain.common.annotations.ValueObject",
        "solid.humank.genaidemo.domain.common.annotations.Entity",
        "solid.humank.genaidemo.domain.common.annotations.AggregateRoot",
        "solid.humank.genaidemo.domain.common.annotations.DomainService",
        "solid.humank.genaidemo.domain.common.entity.EntityAnnotation"
    );

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("領域模型應該組織在正確的包結構中")
    void domainModelShouldBeOrganizedInCorrectPackageStructure() {
        // 聚合根應該位於 model.aggregate 包中
        ArchRule aggregateRootRule = classes()
                .that().haveSimpleNameEndingWith("Aggregate")
                .or().areAnnotatedWith("solid.humank.genaidemo.domain.common.annotations.AggregateRoot")
                .and().areNotAnnotations()
                .and().resideOutsideOfPackage(DOMAIN_COMMON_PACKAGE)
                .should().resideInAPackage("..domain..model.aggregate..");
        aggregateRootRule.check(importedClasses);

        // 實體應該位於 model.entity 包中
        ArchRule entityRule = classes()
                .that().haveSimpleNameEndingWith("Entity")
                .or().areAnnotatedWith("solid.humank.genaidemo.domain.common.annotations.Entity")
                .and().areNotAnnotations()
                .and().resideOutsideOfPackage(DOMAIN_COMMON_PACKAGE)
                .and().doNotHaveFullyQualifiedName("solid.humank.genaidemo.domain.common.entity.EntityAnnotation")
                .should().resideInAPackage("..domain..model.entity..");
        entityRule.check(importedClasses);

        // 值對象應該位於 common.valueobject 或 model.valueobject 包中
        ArchRule valueObjectRule = classes()
                .that().haveSimpleNameEndingWith("ValueObject")
                .or().areAnnotatedWith("solid.humank.genaidemo.domain.common.annotations.ValueObject")
                .and().areNotAnnotations()
                .and().doNotHaveFullyQualifiedName("solid.humank.genaidemo.domain.common.annotations.ValueObject")
                .should().resideInAPackage("..domain..valueobject..")
                .orShould().resideInAPackage("..domain..model.valueobject..");
        valueObjectRule.check(importedClasses);

        // 領域事件應該位於 events 或 model.events 包中
        ArchRule domainEventRule = classes()
                .that().haveSimpleNameEndingWith("Event")
                .and().resideInAPackage("..domain..")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..events..")
                .orShould().resideInAPackage("..domain..model.events..");
        domainEventRule.check(importedClasses);

        // 領域服務應該位於 service 或 model.service 包中
        ArchRule domainServiceRule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().resideInAPackage("..domain..")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..service..")
                .orShould().resideInAPackage("..domain..model.service..");
        domainServiceRule.check(importedClasses);

        // 儲存庫接口應該位於 repository 包中
        ArchRule repositoryRule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().resideInAPackage("..domain..")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..repository..");
        repositoryRule.check(importedClasses);

        // 工廠應該位於 factory 或 model.factory 包中
        ArchRule factoryRule = classes()
                .that().haveSimpleNameEndingWith("Factory")
                .and().resideInAPackage("..domain..")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..factory..")
                .orShould().resideInAPackage("..domain..model.factory..");
        factoryRule.check(importedClasses);

        // 規格應該位於 specification 或 model.specification 包中
        ArchRule specificationRule = classes()
                .that().haveSimpleNameEndingWith("Specification")
                .and().resideInAPackage("..domain..")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..specification..")
                .orShould().resideInAPackage("..domain..model.specification..");
        specificationRule.check(importedClasses);
    }

    @Test
    @DisplayName("子領域模型結構應該符合DDD戰術設計")
    void subdomainModelStructureShouldFollowDDDTacticalDesign() {
        // 子領域的模型元素應該位於 model 包中
        ArchRule subdomainModelRule = classes()
                .that().resideInAPackage("..domain.order.model..")
                .or().resideInAPackage("..domain.payment.model..")
                .should().resideInAPackage("..domain..model..");
        subdomainModelRule.check(importedClasses);
        
        // 子領域的聚合根應該位於 model.aggregate 包中
        ArchRule subdomainAggregateRule = classes()
                .that().resideInAPackage("..domain.order..")
                .or().resideInAPackage("..domain.payment..")
                .and().haveSimpleNameEndingWith("Aggregate")
                .or().areAnnotatedWith("solid.humank.genaidemo.domain.common.annotations.AggregateRoot")
                .and().areNotAnnotations()
                .should().resideInAPackage("..domain..model.aggregate..");
        subdomainAggregateRule.check(importedClasses);
    }

    @Test
    @DisplayName("應用層應該組織在正確的包結構中")
    void applicationLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 應用服務應該位於 application.service 包中
        ArchRule applicationServiceRule = classes()
                .that().haveSimpleNameEndingWith("ApplicationService")
                .should().resideInAPackage("..application..service..");
        applicationServiceRule.check(importedClasses);

        // DTO 應該位於 application.dto 包中
        ArchRule dtoRule = classes()
                .that().haveSimpleNameEndingWith("DTO")
                .or().haveSimpleNameEndingWith("Command")
                .or().haveSimpleNameEndingWith("Query")
                .or().haveSimpleNameEndingWith("Response")
                .and().resideInAPackage("..application..")
                .should().resideInAPackage("..application..dto..");
        dtoRule.check(importedClasses);

        // 端口應該位於 application.port 包中
        ArchRule portRule = classes()
                .that().haveSimpleNameEndingWith("Port")
                .or().haveSimpleNameEndingWith("UseCase")
                .and().resideInAPackage("..application..")
                .should().resideInAPackage("..application..port..");
        portRule.check(importedClasses);
    }

    @Test
    @DisplayName("基礎設施層應該組織在正確的包結構中")
    void infrastructureLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 儲存庫實現應該位於 infrastructure.persistence 包中
        ArchRule repositoryImplRule = classes()
                .that().haveSimpleNameEndingWith("RepositoryImpl")
                .should().resideInAPackage("..infrastructure..persistence..");
        repositoryImplRule.check(importedClasses);

        // 防腐層應該位於 infrastructure.acl 包中
        ArchRule aclRule = classes()
                .that().haveSimpleNameEndingWith("AntiCorruptionLayer")
                .should().resideInAPackage("..infrastructure..acl..");
        aclRule.check(importedClasses);

        // 外部系統適配器應該位於 infrastructure.external 或 infrastructure.*.external 包中
        ArchRule adapterRule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAPackage("..infrastructure..external..")
                .orShould().resideInAPackage("..infrastructure..persistence..");
        adapterRule.check(importedClasses);

        // Saga 應該位於 infrastructure.saga 包中
        ArchRule sagaRule = classes()
                .that().haveSimpleNameEndingWith("Saga")
                .should().resideInAPackage("..infrastructure..saga..");
        sagaRule.check(importedClasses);
    }

    @Test
    @DisplayName("介面層應該組織在正確的包結構中")
    void interfacesLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 控制器應該位於 interfaces.web 包中
        ArchRule controllerRule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..interfaces..web..");
        controllerRule.check(importedClasses);

        // 請求/響應 DTO 應該位於 interfaces.web.dto 包中
        ArchRule webDtoRule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .and().resideInAPackage("..interfaces..")
                .and().haveSimpleNameNotEndingWith("ApiErrorResponse") // 排除ResponseFactory中的內部類
                .should().resideInAPackage("..interfaces..web..dto..");
        webDtoRule.check(importedClasses);
    }
}