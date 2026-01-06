package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Promise;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payment processing service.
 * <p>
 * Handles credit card authorization, capture, and refunds.
 * In a real distributed system, this would be a separate @Slice module.
 */
public interface PaymentService {
    /**
     * Process a payment (authorize and capture).
     */
    Promise<PaymentResult> processPayment(ProcessPaymentRequest request);

    /**
     * Process a refund for a previous transaction.
     */
    Promise<RefundResult> processRefund(RefundRequest request);

    /**
     * Factory method.
     */
    static PaymentService paymentService() {
        return new PaymentServiceImpl();
    }
}

class PaymentServiceImpl implements PaymentService {
    private final Map<String, PaymentResult> transactions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Simulate failure rates for different scenarios
    private static final double DECLINE_RATE = 0.05;

    // 5% decline rate
    private static final double FRAUD_CHECK_DELAY_MS = 100;

    @Override
    public Promise<PaymentResult> processPayment(ProcessPaymentRequest request) {
        // Simulate fraud check delay
        return simulateFraudCheck()
                                 .flatMap(_ -> validatePaymentAmount(request.amount()))
                                 .flatMap(_ -> simulateAuthorization(request));
    }

    @Override
    public Promise<RefundResult> processRefund(RefundRequest request) {
        var original = transactions.get(request.transactionId());
        if (original == null) {
            return PaymentError.transactionNotFound(request.transactionId())
                               .promise();
        }
        var refundAmount = request.partialAmount()
                                  .or(original.amount());
        if (refundAmount.isGreaterThan(original.amount())) {
            return PaymentError.refundExceedsOriginal(refundAmount,
                                                      original.amount())
                               .promise();
        }
        var refund = RefundResult.refundResult(request.transactionId(), refundAmount);
        return Promise.success(refund);
    }

    private Promise<Void> simulateFraudCheck() {
        return Promise.lift(PaymentError.ProcessingFailed::new,
                            () -> {
                                Thread.sleep((long) FRAUD_CHECK_DELAY_MS);
                                return null;
                            });
    }

    private Promise<Money> validatePaymentAmount(Money amount) {
        if (amount.isZero()) {
            return PaymentError.invalidAmount("Amount cannot be zero")
                               .promise();
        }
        if (amount.amount()
                  .compareTo(BigDecimal.valueOf(50000)) > 0) {
            return PaymentError.invalidAmount("Amount exceeds maximum ($50,000)")
                               .promise();
        }
        return Promise.success(amount);
    }

    private Promise<PaymentResult> simulateAuthorization(ProcessPaymentRequest request) {
        // Simulate random decline
        if (random.nextDouble() < DECLINE_RATE) {
            return PaymentError.declined("Card declined by issuer")
                               .promise();
        }
        // Check for test card numbers that simulate specific errors
        var cardNumber = request.paymentMethod()
                                .cardNumber();
        if (cardNumber.endsWith("0000")) {
            return PaymentError.declined("Insufficient funds")
                               .promise();
        }
        if (cardNumber.endsWith("1111")) {
            return PaymentError.declined("Card expired")
                               .promise();
        }
        if (cardNumber.endsWith("2222")) {
            return PaymentError.fraudSuspected()
                               .promise();
        }
        // Success - authorize and capture
        var result = PaymentResult.authorized(request.orderId(),
                                              request.amount(),
                                              request.paymentMethod())
                                  .capture();
        transactions.put(result.transactionId(), result);
        return Promise.success(result);
    }
}
