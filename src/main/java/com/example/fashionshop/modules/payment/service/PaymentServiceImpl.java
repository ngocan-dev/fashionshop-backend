package com.example.fashionshop.modules.payment.service;

import com.example.fashionshop.common.enums.InvoicePaymentStatus;
import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.enums.PaymentStatus;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.PaymentCancelledException;
import com.example.fashionshop.common.exception.PaymentGatewayException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.invoice.entity.Invoice;
import com.example.fashionshop.modules.invoice.repository.InvoiceRepository;
import com.example.fashionshop.modules.order.entity.Order;
import com.example.fashionshop.modules.order.repository.OrderRepository;
import com.example.fashionshop.modules.payment.dto.PaymentRequest;
import com.example.fashionshop.modules.payment.dto.PaymentResponse;
import com.example.fashionshop.modules.payment.entity.Payment;
import com.example.fashionshop.modules.payment.gateway.GatewayPaymentRequest;
import com.example.fashionshop.modules.payment.gateway.GatewayPaymentResult;
import com.example.fashionshop.modules.payment.gateway.GatewayPaymentStatus;
import com.example.fashionshop.modules.payment.gateway.PaymentGateway;
import com.example.fashionshop.modules.payment.gateway.PaymentGatewayFactory;
import com.example.fashionshop.modules.payment.repository.PaymentRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final long ORDER_PAYMENT_TTL_MINUTES = 30;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;

    @Override
    @Transactional
    public PaymentResponse processPayment(Integer orderId, PaymentRequest request) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateOrderOwnership(user, order);
        validateOrderEligibility(order);

        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        Payment existingPayment = findIdempotentPayment(order, idempotencyKey);
        if (existingPayment != null) {
            return toResponse(existingPayment, order, resolveMessage(existingPayment), isRetryable(existingPayment), null);
        }

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            return processCashOnDelivery(order, idempotencyKey);
        }

        validateOnlinePaymentRequest(request);

        Payment processingPayment = paymentRepository.save(Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PROCESSING)
                .idempotencyKey(idempotencyKey)
                .build());

        PaymentGateway gateway = paymentGatewayFactory.getGateway(request.getPaymentMethod());
        GatewayPaymentResult gatewayResult = gateway.charge(buildGatewayRequest(order, user, request, idempotencyKey));

        if (gatewayResult.getStatus() == GatewayPaymentStatus.CANCELLED) {
            processingPayment.setPaymentStatus(PaymentStatus.CANCELLED);
            processingPayment.setFailureReason(gatewayResult.getMessage());
            paymentRepository.save(processingPayment);
            updateInvoiceStatus(order, InvoicePaymentStatus.PENDING);
            throw new PaymentCancelledException("Payment cancelled. Returned to checkout.");
        }

        if (gatewayResult.getStatus() == GatewayPaymentStatus.FAILED) {
            processingPayment.setPaymentStatus(PaymentStatus.FAILED);
            processingPayment.setFailureReason(gatewayResult.getMessage());
            paymentRepository.save(processingPayment);
            updateInvoiceStatus(order, InvoicePaymentStatus.FAILED);
            throw new PaymentGatewayException("Payment failed");
        }

        processingPayment.setPaymentStatus(PaymentStatus.PAID);
        processingPayment.setPaidAt(LocalDateTime.now());
        processingPayment.setGatewayTransactionId(gatewayResult.getTransactionId());
        processingPayment = paymentRepository.save(processingPayment);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        updateInvoiceStatus(order, InvoicePaymentStatus.PAID);

        return toResponse(processingPayment, order, "Payment successful", false, gatewayResult.getRedirectUrl());
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        throw new BadRequestException("Use /api/payments/orders/{orderId}/pay endpoint");
    }

    @Override
    public PaymentResponse getPaymentStatus(Integer orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateOrderOwnership(user, order);

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment, order, resolveMessage(payment), isRetryable(payment), null);
    }

    private PaymentResponse processCashOnDelivery(Order order, String idempotencyKey) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        payment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        updateInvoiceStatus(order, InvoicePaymentStatus.PENDING);

        return toResponse(payment, order, "Order confirmed with cash on delivery", false, null);
    }

    private void validateOnlinePaymentRequest(PaymentRequest request) {
        if (request.getPaymentMethod() == null) {
            throw new BadRequestException("Payment method is required");
        }

        if (request.isCancelledByUser()) {
            throw new PaymentCancelledException("Payment cancelled. Returned to checkout.");
        }

        if (isBlank(request.getCardHolderName())) {
            throw new BadRequestException("Card holder name is required");
        }
        if (isBlank(request.getCardNumber())) {
            throw new BadRequestException("Card number is required");
        }

        String normalizedCardNumber = request.getCardNumber().replaceAll("\\s", "");
        if (!normalizedCardNumber.matches("\\d{12,19}")) {
            throw new BadRequestException("Card number is invalid");
        }

        if (isBlank(request.getExpiryMonth()) || !request.getExpiryMonth().matches("^(0[1-9]|1[0-2])$")) {
            throw new BadRequestException("Expiry month is invalid");
        }
        if (isBlank(request.getExpiryYear()) || !request.getExpiryYear().matches("^\\d{2}$")) {
            throw new BadRequestException("Expiry year is invalid");
        }
        if (isBlank(request.getCvv()) || !request.getCvv().matches("^\\d{3,4}$")) {
            throw new BadRequestException("CVV is invalid");
        }
    }

    private void validateOrderOwnership(User user, Order order) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only pay your own order");
        }
    }

    private void validateOrderEligibility(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot be paid");
        }

        if (isOrderExpired(order)) {
            throw new BadRequestException("Checkout session has expired");
        }

        boolean alreadyPaid = paymentRepository.existsByOrderAndPaymentStatusIn(order,
                List.of(PaymentStatus.PAID));
        if (alreadyPaid) {
            throw new BadRequestException("Order already paid");
        }
    }

    private boolean isOrderExpired(Order order) {
        LocalDateTime createdAt = order.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        return order.getStatus() == OrderStatus.PENDING
                && createdAt.isBefore(LocalDateTime.now().minusMinutes(ORDER_PAYMENT_TTL_MINUTES));
    }

    private Payment findIdempotentPayment(Order order, String idempotencyKey) {
        if (isBlank(idempotencyKey)) {
            return null;
        }

        return paymentRepository.findTopByOrderAndIdempotencyKeyOrderByIdDesc(order, idempotencyKey)
                .orElse(null);
    }

    private GatewayPaymentRequest buildGatewayRequest(Order order, User user, PaymentRequest request, String idempotencyKey) {
        return GatewayPaymentRequest.builder()
                .orderId(order.getId())
                .customerId(user.getId())
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalPrice())
                .idempotencyKey(idempotencyKey)
                .cardHolderName(request.getCardHolderName())
                .cardNumber(request.getCardNumber())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cvv(request.getCvv())
                .returnUrl(request.getReturnUrl())
                .cancelledByUser(request.isCancelledByUser())
                .build();
    }

    private PaymentResponse toResponse(Payment payment,
                                       Order order,
                                       String message,
                                       boolean retryable,
                                       String redirectUrl) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .orderStatus(order.getStatus())
                .message(message)
                .retryable(retryable)
                .orderConfirmationPath("/account/orders/" + order.getId())
                .checkoutPath("/checkout")
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .idempotencyKey(payment.getIdempotencyKey())
                .redirectUrl(redirectUrl)
                .paidAt(payment.getPaidAt())
                .build();
    }

    private String resolveMessage(Payment payment) {
        return switch (payment.getPaymentStatus()) {
            case PAID -> "Payment successful";
            case CANCELLED -> "Payment cancelled";
            case FAILED -> "Payment failed";
            case PROCESSING -> "Payment is processing";
            case PENDING, UNPAID -> "Payment pending";
        };
    }

    private boolean isRetryable(Payment payment) {
        return payment.getPaymentStatus() == PaymentStatus.FAILED
                || payment.getPaymentStatus() == PaymentStatus.CANCELLED;
    }

    private void updateInvoiceStatus(Order order, InvoicePaymentStatus status) {
        Invoice invoice = invoiceRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoice.setPaymentStatus(status);
        invoiceRepository.save(invoice);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return idempotencyKey.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
