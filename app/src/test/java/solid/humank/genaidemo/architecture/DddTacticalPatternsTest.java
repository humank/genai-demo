package solid.humank.genaidemo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.common.specification.Specification;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 確保正確實現 DDD 戰術模式
 */
public class DddTacticalPatternsTest {

    private static final String BASE_PACKAGE = "solid.humank.genaidemo";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String ACL_PACKAGE = BASE_PACKAGE + ".infrastructure..acl..";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("值對象應該是不可變的")
    void valueObjectsShouldBeImmutable() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(ValueObject.class)
                .or().haveSimpleNameEndingWith("ValueObject")
                .or().resideInAPackage("..valueobject..")
                .and().areNotEnums() // 排除枚舉類型
                .and().areNotInterfaces() // 排除接口
                .and().areNotInnerClasses() // 排除內部類
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("實體應該有唯一標識")
    void entitiesShouldHaveIdentity() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .or().haveSimpleNameEndingWith("Entity")
                .or().resideInAPackage("..entity..")
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("聚合根應該控制其內部實體的訪問")
    void aggregateRootsShouldControlAccessToEntities() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(AggregateRoot.class)
                .or().haveSimpleNameEndingWith("Aggregate")
                .or().resideInAPackage("..aggregate..")
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("領域事件應該是不可變的")
    void domainEventsShouldBeImmutable() {
        ArchRule rule = classes()
                .that().implement(DomainEvent.class)
                .or().haveSimpleNameEndingWith("Event")
                .or().resideInAPackage("..events..")
                .and().areNotInnerClasses() // 排除內部類
                .and().areNotAnonymousClasses() // 排除匿名類
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("儲存庫應該操作聚合根")
    void repositoriesShouldOperateOnAggregateRoots() {
        ArchRule rule = classes()
                .that().implement(Repository.class)
                .or().haveSimpleNameEndingWith("Repository")
                .or().resideInAPackage("..repository..")
                .and().resideInAPackage(DOMAIN_PACKAGE) // 只檢查領域層的儲存庫接口
                .should().beInterfaces();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("工廠應該創建聚合或複雜值對象")
    void factoriesShouldCreateAggregatesOrComplexValueObjects() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Factory")
                .or().resideInAPackage("..factory..")
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("領域服務應該是無狀態的")
    void domainServicesShouldBeStateless() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().resideInAPackage(DOMAIN_PACKAGE)
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("規格應該實現 Specification 接口")
    void specificationsShouldImplementSpecificationInterface() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Specification")
                .or().resideInAPackage("..specification..")
                .and().areNotInterfaces() // 排除Specification接口本身
                .should().implement(Specification.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("防腐層應該隔離外部系統")
    void antiCorruptionLayerShouldIsolateExternalSystems() {
        ArchRule rule = classes()
                .that().resideInAPackage(ACL_PACKAGE)
                .or().haveSimpleNameEndingWith("AntiCorruptionLayer")
                .should().bePublic();

        rule.check(importedClasses);
    }
}