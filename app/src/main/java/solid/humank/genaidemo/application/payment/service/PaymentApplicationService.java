package solid.humank.genaidemo.application.payment.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.common.service.DomainEventApplicationService;
import solid.humank.genaidemo.application.payment.dto.PaymentRequestDto;
import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;
import solid.humank.genaidemo.application.payment.dto.ProcessPaymentCommand;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentProcessingUseCase;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentGatewayPort;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

/** 支付應用服務 實現支付處理和管理用例 */
@Service
@Transactional
public class PaymentApplicationService
        implements PaymentProcessingUseCase, PaymentManagementUseCase {

    private final PaymentPersistencePort paymentPersistencePort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DomainEventApplicationService domainEventApplicationService;

    public PaymentApplicationService(
            PaymentPersistencePort paymentPersistencePort,
            PaymentGatewayPort paymentGatewayPort,
            DomainEventApplicationService domainEventApplicationService) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.domainEventApplicationService = domainEventApplicationService;
    }

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        // 創建支付
        Payment payment =
                new Payment(
                        UUID.fromString(requestDto.getOrderId()),
                        Money.of(requestDto.getAmount(), requestDto.getCurrency()),
                        PaymentMethod.valueOf(requestDto.getPaymentMethod()));

        // 保存支付
        paymentPersistencePort.save(payment);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(payment);

        try {
            // 處理支付
            String transactionId =
                    paymentGatewayPort.processPayment(
                            payment.getOrderIdAsUUID(),
                            payment.getAmount(),
                            payment.getPaymentMethod());

            // 完成支付
            payment.complete(transactionId);

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        } catch (Exception e) {
            // 支付失敗
            payment.fail(e.getMessage());

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        }

        return PaymentResponseDto.fromDomain(payment);
    }

    @Override
    public PaymentResponseDto processPayment(ProcessPaymentCommand command) {
        // 創建支付
        Payment payment =
                new Payment(command.getOrderId(), command.getAmount(), command.getPaymentMethod());

        // 保存支付
        paymentPersistencePort.save(payment);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(payment);

        try {
            // 處理支付
            String transactionId =
                    paymentGatewayPort.processPayment(
                            payment.getOrderIdAsUUID(),
                            payment.getAmount(),
                            payment.getPaymentMethod());

            // 完成支付
            payment.complete(transactionId);

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        } catch (Exception e) {
            // 支付失敗
            payment.fail(e.getMessage());

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        }

        return PaymentResponseDto.fromDomain(payment);
    }

    @Override
    public Payment processPayment(UUID orderId, Money amount) {
        // 創建支付
        Payment payment = new Payment(orderId, amount);

        // 保存支付
        paymentPersistencePort.save(payment);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(payment);

        try {
            // 處理支付
            String transactionId =
                    paymentGatewayPort.processPayment(
                            payment.getOrderIdAsUUID(),
                            payment.getAmount(),
                            payment.getPaymentMethod());

            // 完成支付
            payment.complete(transactionId);

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        } catch (Exception e) {
            // 支付失敗
            payment.fail(e.getMessage());

            // 更新支付
            paymentPersistencePort.update(payment);

            // 發布領域事件
            domainEventApplicationService.publishEventsFromAggregate(payment);
        }

        return payment;
    }

    @Override
    public Optional<Payment> getPayment(UUID paymentId) {
        return paymentPersistencePort.findById(paymentId);
    }

    @Override
    public PaymentResponseDto getPaymentDto(UUID paymentId) {
        return paymentPersistencePort
                .findById(paymentId)
                .map(PaymentResponseDto::fromDomain)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Payment not found with ID: " + paymentId));
    }

    @Override
    public Optional<Payment> getPaymentByOrderId(UUID orderId) {
        return paymentPersistencePort.findByOrderId(orderId);
    }

    @Override
    public PaymentResponseDto getPaymentDtoByOrderId(UUID orderId) {
        return paymentPersistencePort
                .findByOrderId(orderId)
                .map(PaymentResponseDto::fromDomain)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Payment not found for order: " + orderId));
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentPersistencePort.findAll();
    }

    @Override
    public List<PaymentResponseDto> getAllPaymentDtos() {
        return paymentPersistencePort.findAll().stream()
                .map(PaymentResponseDto::fromDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponseDto cancelPayment(UUID paymentId) {
        Optional<Payment> paymentOpt = paymentPersistencePort.findById(paymentId);

        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found with ID: " + paymentId);
        }

        Payment payment = paymentOpt.get();

        // 取消支付
        payment.fail("Payment cancelled by user");

        // 更新支付
        paymentPersistencePort.update(payment);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(payment);

        return PaymentResponseDto.fromDomain(payment);
    }

    @Override
    public PaymentResponseDto refundPayment(UUID paymentId) {
        Optional<Payment> paymentOpt = paymentPersistencePort.findById(paymentId);

        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found with ID: " + paymentId);
        }

        Payment payment = paymentOpt.get();

        // 退款
        payment.refund();

        // 更新支付
        paymentPersistencePort.update(payment);

        // 發布領域事件
        domainEventApplicationService.publishEventsFromAggregate(payment);

        return PaymentResponseDto.fromDomain(payment);
    }
}
