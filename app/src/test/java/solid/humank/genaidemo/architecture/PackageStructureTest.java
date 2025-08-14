package solid.humank.genaidemo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PackageStructureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter().importPackages("solid.humank.genaidemo");
    }

    @Test
    @DisplayName("子領域模型結構應該符合DDD戰術設計")
    void subdomainModelStructureShouldFollowDDDTacticalDesign() {
        // 子領域的模型元素應該位於 model 包中
        ArchRule subdomainModelRule =
                classes()
                        .that()
                        .resideInAPackage("..domain.order.model..")
                        .or()
                        .resideInAPackage("..domain.payment.model..")
                        .should()
                        .resideInAPackage("..domain..model..");
        subdomainModelRule.check(importedClasses);

        // 子領域的聚合根應該位於 model.aggregate 包中
        ArchRule subdomainAggregateRule =
                classes()
                        .that()
                        .resideInAPackage("..domain.order..")
                        .or()
                        .resideInAPackage("..domain.payment..")
                        .and()
                        .haveSimpleNameEndingWith("Aggregate")
                        .or()
                        .areAnnotatedWith(
                                "solid.humank.genaidemo.domain.common.annotations.AggregateRoot")
                        .and()
                        .areNotAnnotations()
                        .should()
                        .resideInAPackage("..domain..model.aggregate..");
        subdomainAggregateRule.check(importedClasses);
    }

    @Test
    @DisplayName("應用層應該組織在正確的包結構中")
    void applicationLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 應用服務應該位於 application.service 包中
        ArchRule applicationServiceRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("ApplicationService")
                        .should()
                        .resideInAPackage("..application..service..");
        applicationServiceRule.check(importedClasses);

        // DTO 應該位於 application.dto 包中
        ArchRule dtoRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("DTO")
                        .or()
                        .haveSimpleNameEndingWith("Command")
                        .or()
                        .haveSimpleNameEndingWith("Query")
                        .or()
                        .haveSimpleNameEndingWith("Response")
                        .and()
                        .resideInAPackage("..application..")
                        .should()
                        .resideInAPackage("..application..dto..");
        dtoRule.check(importedClasses);

        // 端口應該位於 application.port 包中
        ArchRule portRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Port")
                        .or()
                        .haveSimpleNameEndingWith("UseCase")
                        .and()
                        .resideInAPackage("..application..")
                        .should()
                        .resideInAPackage("..application..port..");
        portRule.check(importedClasses);
    }

    @Test
    @DisplayName("基礎設施層應該組織在正確的包結構中")
    void infrastructureLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 儲存庫實現應該位於 infrastructure.persistence 包中
        ArchRule repositoryImplRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("RepositoryImpl")
                        .should()
                        .resideInAPackage("..infrastructure..persistence..");
        repositoryImplRule.check(importedClasses);

        // 防腐層應該位於 infrastructure.acl 包中
        ArchRule aclRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("AntiCorruptionLayer")
                        .should()
                        .resideInAPackage("..infrastructure..acl..");
        aclRule.check(importedClasses);

        // 外部系統適配器應該位於 infrastructure.external 或 infrastructure.*.external 包中
        ArchRule adapterRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Adapter")
                        .and()
                        .haveNameNotMatching(".*DomainEventPublisherAdapter")
                        .should()
                        .resideInAPackage("..infrastructure..external..")
                        .orShould()
                        .resideInAPackage("..infrastructure..persistence..");
        adapterRule.check(importedClasses);

        // Saga 應該位於 infrastructure.saga 包中
        ArchRule sagaRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Saga")
                        .should()
                        .resideInAPackage("..infrastructure..saga..");
        sagaRule.check(importedClasses);
    }

    @Test
    @DisplayName("介面層應該組織在正確的包結構中")
    void interfacesLayerShouldBeOrganizedInCorrectPackageStructure() {
        // 控制器應該位於 interfaces.web 包中
        ArchRule controllerRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Controller")
                        .should()
                        .resideInAPackage("..interfaces..web..");
        controllerRule.check(importedClasses);

        // 請求/響應 DTO 應該位於 interfaces.web.dto 包中
        ArchRule webDtoRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Request")
                        .or()
                        .haveSimpleNameEndingWith("Response")
                        .and()
                        .resideInAPackage("..interfaces..")
                        .and()
                        .haveSimpleNameNotEndingWith("ApiErrorResponse") // 排除ResponseFactory中的內部類
                        .should()
                        .resideInAPackage("..interfaces..web..dto..");
        webDtoRule.check(importedClasses);
    }
}
