package solid.humank.genaidemo.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

class RecordValueObjectTest {

    @Test
    void testMoneyRecord() {
        Money money1 = Money.twd(100);
        Money money2 = Money.twd(100);
        Money money3 = Money.twd(200);

        // 測試相等性
        assertEquals(money1, money2);
        assertNotEquals(money1, money3);

        // 測試不可變性
        assertEquals(BigDecimal.valueOf(100), money1.amount());
        assertEquals(Currency.getInstance("TWD"), money1.currency());

        // 測試向後相容性
        assertEquals(BigDecimal.valueOf(100), money1.getAmount());
        assertEquals(Currency.getInstance("TWD"), money1.getCurrency());

        // 測試業務邏輯
        Money sum = money1.add(money2);
        assertEquals(Money.twd(200), sum);
    }

    @Test
    void testOrderIdRecord() {
        UUID uuid = UUID.randomUUID();
        OrderId orderId1 = OrderId.of(uuid);
        OrderId orderId2 = OrderId.of(uuid);
        OrderId orderId3 = OrderId.generate();

        // 測試相等性
        assertEquals(orderId1, orderId2);
        assertNotEquals(orderId1, orderId3);

        // 測試不可變性
        assertEquals(uuid, orderId1.value());

        // 測試向後相容性
        assertEquals(uuid, orderId1.getId());
        assertEquals(uuid.toString(), orderId1.getValue());
    }

    @Test
    void testCustomerIdRecord() {
        String id = "customer-123";
        CustomerId customerId1 = CustomerId.of(id);
        CustomerId customerId2 = CustomerId.of(id);
        CustomerId customerId3 = CustomerId.generate();

        // 測試相等性
        assertEquals(customerId1, customerId2);
        assertNotEquals(customerId1, customerId3);

        // 測試不可變性
        assertEquals(id, customerId1.value());

        // 測試向後相容性
        assertEquals(id, customerId1.getId());
        assertEquals(id, customerId1.getValue());
    }

    @Test
    void testEmailRecord() {
        String email = "test@example.com";
        Email email1 = new Email(email);
        Email email2 = new Email("TEST@EXAMPLE.COM"); // 應該正規化為小寫

        // 測試相等性（正規化後應該相等）
        assertEquals(email1, email2);

        // 測試不可變性和正規化
        assertEquals("test@example.com", email1.value());
        assertEquals("test@example.com", email2.value());

        // 測試向後相容性
        assertEquals("test@example.com", email1.getEmail());
    }

    @Test
    void testAddressRecord() {
        Address address1 = new Address("123 Main St", "Taipei", "10001", "Taiwan");
        Address address2 = new Address("123 Main St", "Taipei", "10001", "Taiwan");

        // 測試相等性
        assertEquals(address1, address2);

        // 測試不可變性
        assertEquals("123 Main St", address1.street());
        assertEquals("Taipei", address1.city());

        // 測試向後相容性
        assertEquals("123 Main St", address1.getStreet());
        assertEquals("Taipei", address1.getCity());

        // 測試業務邏輯
        String fullAddress = address1.getFullAddress();
        assertTrue(fullAddress.contains("123 Main St"));
        assertTrue(fullAddress.contains("Taipei"));
    }

    @Test
    void testOrderItemRecord() {
        Money price = Money.twd(100);
        OrderItem item1 = new OrderItem("PROD-001", "Test Product", 2, price);
        OrderItem item2 = new OrderItem("PROD-001", "Test Product", 2, price);

        // 測試相等性
        assertEquals(item1, item2);

        // 測試不可變性
        assertEquals("PROD-001", item1.productId());
        assertEquals("Test Product", item1.productName());
        assertEquals(2, item1.quantity());
        assertEquals(price, item1.price());

        // 測試向後相容性
        assertEquals("PROD-001", item1.getProductId());
        assertEquals("Test Product", item1.getProductName());
        assertEquals(2, item1.getQuantity());
        assertEquals(price, item1.getPrice());

        // 測試業務邏輯
        Money subtotal = item1.getSubtotal();
        assertEquals(Money.twd(200), subtotal);
    }

    @Test
    void testStockQuantityRecord() {
        StockQuantity stock1 = new StockQuantity(10);
        StockQuantity stock2 = new StockQuantity(10);
        StockQuantity stock3 = new StockQuantity(0);

        // 測試相等性
        assertEquals(stock1, stock2);
        assertNotEquals(stock1, stock3);

        // 測試不可變性
        assertEquals(10, stock1.value());

        // 測試向後相容性
        assertEquals(10, stock1.getValue());

        // 測試業務邏輯
        assertTrue(stock1.isAvailable());
        assertFalse(stock3.isAvailable());

        StockQuantity reduced = stock1.subtract(3);
        assertEquals(7, reduced.getValue());
        assertEquals(10, stock1.getValue()); // 原對象不變
    }

    @Test
    void testValidationInRecords() {
        // 測試 Money 驗證
        assertThrows(IllegalArgumentException.class,
                () -> new Money(BigDecimal.valueOf(-100), Currency.getInstance("TWD")));

        // 測試 Email 驗證
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));

        // 測試 OrderItem 驗證
        assertThrows(IllegalArgumentException.class, () -> new OrderItem("PROD-001", "Test", 0, Money.twd(100)));

        // 測試 StockQuantity 驗證
        assertThrows(IllegalArgumentException.class, () -> new StockQuantity(-1));
    }
}