package solid.humank.genaidemo.infrastructure.payment.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentGatewayPort;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

import java.util.UUID;

/**
 * 支付網關適配器
 * 負責與外部支付網關的交互
 */
@Component
public class PaymentGatewayAdapter implements PaymentGatewayPort {

    /**
     * 處理信用卡支付
     * 
     * @param orderId 訂單ID
     * @param amount 金額
     * @param cardNumber 卡號
     * @param expiryDate 有效期
     * @param cvv 安全碼
     * @return 交易ID
     */
    public String processCreditCardPayment(UUID orderId, Money amount, String cardNumber, String expiryDate, String cvv) {
        // 模擬與外部支付網關的交互
        validateCreditCard(cardNumber, expiryDate, cvv);
        
        // 生成交易ID
        return "CC-TXN-" + System.currentTimeMillis();
    }
    
    /**
     * 處理銀行轉賬
     * 
     * @param orderId 訂單ID
     * @param amount 金額
     * @param accountNumber 賬號
     * @param bankCode 銀行代碼
     * @return 交易ID
     */
    public String processBankTransfer(UUID orderId, Money amount, String accountNumber, String bankCode) {
        // 模擬與外部銀行系統的交互
        validateBankAccount(accountNumber, bankCode);
        
        // 生成交易ID
        return "BT-TXN-" + System.currentTimeMillis();
    }
    
    /**
     * 處理支付
     * 
     * @param orderId 訂單ID
     * @param amount 金額
     * @param paymentMethod 支付方式
     * @return 交易ID
     */
    @Override
    public String processPayment(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        // 根據支付方式選擇不同的處理邏輯
        switch (paymentMethod) {
            case CREDIT_CARD:
                return processCreditCardPayment(orderId, amount, "4111111111111111", "12/2025", "123");
            case BANK_TRANSFER:
                return processBankTransfer(orderId, amount, "1234567890", "ABC");
            default:
                throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
        }
    }
    
    /**
     * 退款處理
     * 
     * @param transactionId 交易ID
     * @param amount 金額
     * @return 是否成功
     */
    @Override
    public boolean refund(String transactionId, Money amount) {
        // 模擬退款處理
        try {
            validateTransaction(transactionId);
            // 模擬退款成功
            return true;
        } catch (Exception e) {
            // 模擬退款失敗
            return false;
        }
    }
    
    /**
     * 取消支付
     * 
     * @param transactionId 交易ID
     * @return 是否成功
     */
    @Override
    public boolean cancel(String transactionId) {
        // 模擬取消支付
        try {
            validateTransaction(transactionId);
            // 模擬取消成功
            return true;
        } catch (Exception e) {
            // 模擬取消失敗
            return false;
        }
    }
    
    /**
     * 驗證信用卡
     */
    private void validateCreditCard(String cardNumber, String expiryDate, String cvv) {
        // 模擬信用卡驗證
        if (cardNumber == null || cardNumber.length() < 13) {
            throw new IllegalArgumentException("Invalid credit card number");
        }
        
        if (expiryDate == null || !expiryDate.matches("\\d{2}/\\d{4}")) {
            throw new IllegalArgumentException("Invalid expiry date format");
        }
        
        if (cvv == null || cvv.length() < 3) {
            throw new IllegalArgumentException("Invalid CVV");
        }
    }
    
    /**
     * 驗證銀行賬戶
     */
    private void validateBankAccount(String accountNumber, String bankCode) {
        // 模擬銀行賬戶驗證
        if (accountNumber == null || accountNumber.length() < 8) {
            throw new IllegalArgumentException("Invalid account number");
        }
        
        if (bankCode == null || bankCode.length() < 3) {
            throw new IllegalArgumentException("Invalid bank code");
        }
    }
    
    /**
     * 驗證交易
     */
    private void validateTransaction(String transactionId) {
        // 模擬交易驗證
        if (transactionId == null || (!transactionId.startsWith("CC-TXN-") && !transactionId.startsWith("BT-TXN-"))) {
            throw new IllegalArgumentException("Invalid transaction ID");
        }
    }
}