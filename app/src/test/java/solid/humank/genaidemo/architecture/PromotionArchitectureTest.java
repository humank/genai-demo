package solid.humank.genaidemo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.common.specification.Specification;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 確保促銷模塊遵循架構規範
 */
public class PromotionArchitectureTest {

    private static final String BASE_PACKAGE = "solid.humank.genaidemo";
    private static final String PROMOTION_PACKAGE = BASE_PACKAGE + ".domain.promotion..";
    private static final String PROMOTION_MODEL_PACKAGE = PROMOTION_PACKAGE + "model..";
    private static final String PROMOTION_SPECIFICATION_PACKAGE = PROMOTION_MODEL_PACKAGE + "specification..";
    private static final String PROMOTION_VALUEOBJECT_PACKAGE = PROMOTION_MODEL_PACKAGE + "valueobject..";
    private static final String PROMOTION_ENTITY_PACKAGE = PROMOTION_MODEL_PACKAGE + "entity..";
    private static final String PROMOTION_AGGREGATE_PACKAGE = PROMOTION_MODEL_PACKAGE + "aggregate..";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("促銷規格應該實現 Specification 接口")
    void promotionSpecificationsShouldImplementSpecificationInterface() {
        ArchRule rule = classes()
                .that().resideInAPackage(PROMOTION_SPECIFICATION_PACKAGE)
                .and().haveSimpleNameEndingWith("Specification")
                .and().areNotInterfaces()
                .should().implement(Specification.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("促銷值對象應該位於正確的包中")
    void promotionValueObjectsShouldResideInCorrectPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Rule")
                .or().haveSimpleNameEndingWith("Id")
                .or().haveSimpleNameEndingWith("Type")
                .and().resideInAPackage(PROMOTION_PACKAGE)
                .should().resideInAPackage(PROMOTION_VALUEOBJECT_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("促銷實體應該位於正確的包中")
    void promotionEntitiesShouldResideInCorrectPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("Voucher")
                .and().resideInAPackage(PROMOTION_PACKAGE)
                .should().resideInAPackage(PROMOTION_ENTITY_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("促銷聚合根應該位於正確的包中")
    void promotionAggregatesShouldResideInCorrectPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("Promotion")
                .and().resideInAPackage(PROMOTION_PACKAGE)
                .should().resideInAPackage(PROMOTION_AGGREGATE_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("促銷規格不應該依賴基礎設施層")
    void promotionSpecificationsShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(PROMOTION_SPECIFICATION_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(BASE_PACKAGE + ".infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("促銷工廠應該創建促銷聚合根")
    void promotionFactoryShouldCreatePromotionAggregates() {
        ArchRule rule = classes()
                .that().haveSimpleName("PromotionFactory")
                .should().dependOnClassesThat().haveSimpleName("Promotion");

        rule.check(importedClasses);
    }
}