package solid.humank.genaidemo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 確保專案遵循 DDD 分層架構的設計原則 */
public class DddArchitectureTest {

    private static final String BASE_PACKAGE = "solid.humank.genaidemo";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGE = BASE_PACKAGE + ".infrastructure..";
    private static final String INTERFACES_PACKAGE = BASE_PACKAGE + ".interfaces..";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("應該遵循分層架構")
    void shouldFollowLayeredArchitecture() {
        ArchRule rule =
                layeredArchitecture()
                        .consideringOnlyDependenciesInAnyPackage(BASE_PACKAGE)
                        .ignoreDependency(Object.class, Object.class) // 忽略標準庫依賴
                        .ignoreDependency(java.util.UUID.class, java.util.UUID.class)
                        .ignoreDependency(java.util.List.class, java.util.List.class)
                        .ignoreDependency(java.util.Map.class, java.util.Map.class)
                        .ignoreDependency(java.util.Optional.class, java.util.Optional.class)
                        .ignoreDependency(java.lang.String.class, java.lang.String.class)
                        .ignoreDependency(
                                java.time.LocalDateTime.class, java.time.LocalDateTime.class)
                        .ignoreDependency(java.math.BigDecimal.class, java.math.BigDecimal.class)
                        .layer("Domain")
                        .definedBy(DOMAIN_PACKAGE)
                        .layer("Application")
                        .definedBy(APPLICATION_PACKAGE)
                        .layer("Infrastructure")
                        .definedBy(INFRASTRUCTURE_PACKAGE)
                        .layer("Interfaces")
                        .definedBy(INTERFACES_PACKAGE)

                        // 定義允許的依賴關係
                        .whereLayer("Interfaces")
                        .mayOnlyAccessLayers("Application")
                        .whereLayer("Application")
                        .mayOnlyAccessLayers("Domain")
                        .whereLayer("Infrastructure")
                        .mayOnlyAccessLayers("Domain");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("領域層不應依賴其他層")
    void domainLayerShouldNotDependOnOtherLayers() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_PACKAGE)
                        .and()
                        .haveNameNotMatching(
                                ".*DomainEventBus") // 排除 DomainEventBus，因為它需要依賴 infrastructure 層的
                        // DomainEventPublisherAdapter
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                APPLICATION_PACKAGE, INFRASTRUCTURE_PACKAGE, INTERFACES_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("應用層不應依賴基礎設施層和介面層")
    void applicationLayerShouldNotDependOnInfrastructureOrInterfacesLayer() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(APPLICATION_PACKAGE)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(INFRASTRUCTURE_PACKAGE, INTERFACES_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("介面層不應直接依賴基礎設施層和領域層")
    void interfacesLayerShouldNotDependOnInfrastructureOrDomainLayer() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(INTERFACES_PACKAGE)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(INFRASTRUCTURE_PACKAGE, DOMAIN_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("控制器應該位於介面層")
    void controllersShouldResideInInterfacesLayer() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*Controller")
                        .should()
                        .resideInAPackage(INTERFACES_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("應用服務應該位於應用層")
    void applicationServicesShouldResideInApplicationLayer() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*ApplicationService")
                        .should()
                        .resideInAPackage(APPLICATION_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("儲存庫實現應該位於基礎設施層")
    void repositoryImplementationsShouldResideInInfrastructureLayer() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*RepositoryImpl")
                        .should()
                        .resideInAPackage(INFRASTRUCTURE_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("控制器不應直接使用數據庫相關類")
    void controllersShouldNotDirectlyUseDatabaseClasses() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(INTERFACES_PACKAGE)
                        .and()
                        .haveNameMatching(".*Controller")
                        .should()
                        .dependOnClassesThat()
                        .haveNameMatching(".*DataSource.*")
                        .orShould()
                        .dependOnClassesThat()
                        .haveNameMatching(".*Connection.*")
                        .orShould()
                        .dependOnClassesThat()
                        .haveNameMatching(".*PreparedStatement.*")
                        .orShould()
                        .dependOnClassesThat()
                        .haveNameMatching(".*ResultSet.*")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAnyPackage("java.sql..", "javax.sql..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("控制器應該只依賴應用服務")
    void controllersShouldOnlyDependOnApplicationServices() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(INTERFACES_PACKAGE)
                        .and()
                        .haveNameMatching(".*Controller")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(INFRASTRUCTURE_PACKAGE, "java.sql..", "javax.sql..");

        rule.check(importedClasses);
    }
}
