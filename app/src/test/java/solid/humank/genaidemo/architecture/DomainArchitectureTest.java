package solid.humank.genaidemo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/** 領域層架構測試 */
public class DomainArchitectureTest {

    private final JavaClasses importedClasses =
            new ClassFileImporter().importPackages("solid.humank.genaidemo");

    @Test
    void domainLayerShouldNotDependOnApplicationLayer() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..application..");

        rule.check(importedClasses);
    }

    @Test
    void domainLayerShouldNotDependOnInfrastructureLayer() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    void aggregateRootsShouldBeAnnotated() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..domain..aggregate")
                        .should()
                        .beAnnotatedWith(
                                "solid.humank.genaidemo.domain.common.annotations.AggregateRoot");

        rule.check(importedClasses);
    }

    @Test
    void valueObjectsShouldBeAnnotated() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..domain..valueobject")
                        .and()
                        .areNotEnums()
                        .should()
                        .beAnnotatedWith(
                                "solid.humank.genaidemo.domain.common.annotations.ValueObject");

        rule.check(importedClasses);
    }

    @Test
    void domainServicesShouldBeAnnotated() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..domain..service")
                        .should()
                        .beAnnotatedWith(
                                "solid.humank.genaidemo.domain.common.annotations.DomainService");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldBeInterfaces() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..domain..port")
                        .and()
                        .haveSimpleNameEndingWith("Repository")
                        .should()
                        .beInterfaces();

        rule.check(importedClasses);
    }

    @Test
    void applicationServicesShouldNotDependOnInfrastructure() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }
}
