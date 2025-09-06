package solid.humank.genaidemo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 架構測試：驗證 DDD Entity 重構的合規性
 * 
 * 此測試類別專門驗證重構後的 Entity 結構是否符合 DDD 戰術模式：
 * - 聚合根必須正確標註和實作介面
 * - Entity 必須正確標註並位於正確的套件中
 * - Value Object 必須是不可變的 Record
 * - 聚合邊界必須清晰
 */
@DisplayName("DDD Entity Refactoring Architecture Tests")
class DddEntityRefactoringArchitectureTest {

        private final JavaClasses classes = new ClassFileImporter()
                        .importPackages("solid.humank.genaidemo.domain");

        @Test
        @DisplayName("Aggregate roots should be properly annotated and implement AggregateRootInterface")
        void aggregateRootsShouldBeProperlyAnnotatedAndImplementInterface() {
                ArchRule rule = classes()
                                .that().areAnnotatedWith(AggregateRoot.class)
                                .should().implement(AggregateRootInterface.class)
                                .andShould().resideInAPackage("..domain.*.model.aggregate..")
                                .because("聚合根必須實作 AggregateRootInterface 並位於正確的套件中");

                rule.check(classes);
        }

        @Test
        @DisplayName("Entities should be properly annotated and located in entity packages")
        void entitiesShouldBeProperlyAnnotatedAndLocated() {
                ArchRule rule = classes()
                                .that().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.*.model.entity..")
                                .because("Entity 必須位於 entity 套件中");

                rule.check(classes);
        }

        @Test
        @DisplayName("Value objects should be records or enums and properly annotated")
        void valueObjectsShouldBeRecordsOrEnumsAndProperlyAnnotated() {
                ArchRule rule = classes()
                                .that().areAnnotatedWith(ValueObject.class)
                                .should().beRecords()
                                .orShould().beEnums()
                                .because("Value Object 必須是不可變的 Record 或 Enum");

                rule.check(classes);
        }

        @Test
        @DisplayName("Seller aggregate should contain required entities")
        void sellerAggregateShouldContainRequiredEntities() {
                // 驗證 Seller 聚合包含重構後的 Entity
                ArchRule sellerProfileEntityRule = classes()
                                .that().haveSimpleNameEndingWith("SellerProfile")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.seller.model.entity..")
                                .because("SellerProfile 應該是 Seller 聚合內的 Entity");

                ArchRule contactInfoEntityRule = classes()
                                .that().haveSimpleNameEndingWith("ContactInfo")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.seller.model.entity..")
                                .because("ContactInfo 應該是 Seller 聚合內的 Entity");

                ArchRule sellerRatingEntityRule = classes()
                                .that().haveSimpleNameEndingWith("SellerRating")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.seller.model.entity..")
                                .because("SellerRating 應該是 Seller 聚合內的 Entity");

                ArchRule sellerVerificationEntityRule = classes()
                                .that().haveSimpleNameEndingWith("SellerVerification")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.seller.model.entity..")
                                .because("SellerVerification 應該是 Seller 聚合內的 Entity");

                sellerProfileEntityRule.check(classes);
                contactInfoEntityRule.check(classes);
                sellerRatingEntityRule.check(classes);
                sellerVerificationEntityRule.check(classes);
        }

        @Test
        @DisplayName("ProductReview aggregate should contain required entities")
        void productReviewAggregateShouldContainRequiredEntities() {
                // 驗證 ProductReview 聚合包含重構後的 Entity
                ArchRule reviewImageEntityRule = classes()
                                .that().haveSimpleNameEndingWith("ReviewImage")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.review.model.entity..")
                                .because("ReviewImage 應該是 ProductReview 聚合內的 Entity");

                ArchRule moderationRecordEntityRule = classes()
                                .that().haveSimpleNameEndingWith("ModerationRecord")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.review.model.entity..")
                                .because("ModerationRecord 應該是 ProductReview 聚合內的 Entity");

                ArchRule reviewResponseEntityRule = classes()
                                .that().haveSimpleNameEndingWith("ReviewResponse")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.review.model.entity..")
                                .because("ReviewResponse 應該是 ProductReview 聚合內的 Entity");

                reviewImageEntityRule.check(classes);
                moderationRecordEntityRule.check(classes);
                reviewResponseEntityRule.check(classes);
        }

        @Test
        @DisplayName("Customer aggregate should contain required entities")
        void customerAggregateShouldContainRequiredEntities() {
                // 驗證 Customer 聚合包含重構後的 Entity
                ArchRule deliveryAddressEntityRule = classes()
                                .that().haveSimpleNameEndingWith("DeliveryAddress")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.customer.model.entity..")
                                .because("DeliveryAddress 應該是 Customer 聚合內的 Entity");

                ArchRule customerPreferencesEntityRule = classes()
                                .that().haveSimpleNameEndingWith("CustomerPreferences")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.customer.model.entity..")
                                .because("CustomerPreferences 應該是 Customer 聚合內的 Entity");

                ArchRule paymentMethodEntityRule = classes()
                                .that().haveSimpleNameEndingWith("PaymentMethod")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.customer.model.entity..")
                                .because("PaymentMethod 應該是 Customer 聚合內的 Entity（從聚合根降級）");

                deliveryAddressEntityRule.check(classes);
                customerPreferencesEntityRule.check(classes);
                paymentMethodEntityRule.check(classes);
        }

        @Test
        @DisplayName("Inventory aggregate should contain required entities")
        void inventoryAggregateShouldContainRequiredEntities() {
                // 驗證 Inventory 聚合包含重構後的 Entity
                ArchRule stockReservationEntityRule = classes()
                                .that().haveSimpleNameEndingWith("StockReservation")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.inventory.model.entity..")
                                .because("StockReservation 應該是 Inventory 聚合內的 Entity");

                ArchRule stockMovementEntityRule = classes()
                                .that().haveSimpleNameEndingWith("StockMovement")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.inventory.model.entity..")
                                .because("StockMovement 應該是 Inventory 聚合內的 Entity");

                ArchRule inventoryThresholdEntityRule = classes()
                                .that().haveSimpleNameEndingWith("InventoryThreshold")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.inventory.model.entity..")
                                .because("InventoryThreshold 應該是 Inventory 聚合內的 Entity");

                stockReservationEntityRule.check(classes);
                stockMovementEntityRule.check(classes);
                inventoryThresholdEntityRule.check(classes);
        }

        @Test
        @DisplayName("Entity ID value objects should be properly implemented")
        void entityIdValueObjectsShouldBeProperlyImplemented() {
                // 驗證所有 Entity ID Value Object 都是 Record
                ArchRule entityIdRule = classes()
                                .that().haveSimpleNameEndingWith("Id")
                                .and().areAnnotatedWith(ValueObject.class)
                                .and().resideInAPackage("..domain.*.model.valueobject..")
                                .should().beRecords()
                                .because("Entity ID Value Object 必須是不可變的 Record");

                entityIdRule.check(classes);
        }

        @Test
        @DisplayName("Status value objects should be enums")
        void statusValueObjectsShouldBeEnums() {
                // 驗證所有狀態 Value Object 都是 Enum
                ArchRule statusRule = classes()
                                .that().haveSimpleNameEndingWith("Status")
                                .and().areAnnotatedWith(ValueObject.class)
                                .should().beEnums()
                                .because("狀態 Value Object 必須是 Enum");

                statusRule.check(classes);
        }

        @Test
        @DisplayName("Entities should not depend on infrastructure")
        void entitiesShouldNotDependOnInfrastructure() {
                ArchRule rule = noClasses()
                                .that().areAnnotatedWith(Entity.class)
                                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                                .because("Entity 不應該依賴基礎設施層");

                rule.check(classes);
        }

        @Test
        @DisplayName("Aggregate roots should not depend on infrastructure")
        void aggregateRootsShouldNotDependOnInfrastructure() {
                ArchRule rule = noClasses()
                                .that().areAnnotatedWith(AggregateRoot.class)
                                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                                .because("聚合根不應該依賴基礎設施層");

                rule.check(classes);
        }

        @Test
        @DisplayName("Domain layer should not depend on application layer")
        void domainLayerShouldNotDependOnApplicationLayer() {
                ArchRule rule = noClasses()
                                .that().resideInAPackage("..domain..")
                                .should().dependOnClassesThat().resideInAPackage("..application..")
                                .because("領域層不應該依賴應用層");

                rule.check(classes);
        }

        @Test
        @DisplayName("NotificationTemplate aggregate should be enriched")
        void notificationTemplateAggregateShouldBeEnriched() {
                // 驗證 NotificationTemplate 聚合包含豐富的內部結構
                ArchRule templateUsageStatisticsEntityRule = classes()
                                .that().haveSimpleNameEndingWith("TemplateUsageStatistics")
                                .and().areAnnotatedWith(Entity.class)
                                .should().resideInAPackage("..domain.notification.model.entity..")
                                .because("TemplateUsageStatistics 應該是 NotificationTemplate 聚合內的 Entity");

                templateUsageStatisticsEntityRule.check(classes);
        }
}