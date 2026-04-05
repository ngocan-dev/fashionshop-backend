package com.example.fashionshop.modules.payment.service;

import com.example.fashionshop.common.enums.InvoicePaymentStatus;
import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.enums.PaymentStatus;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.ForbiddenException;
import com.example.fashionshop.common.exception.PaymentStatusLoadException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.invoice.entity.Invoice;
import com.example.fashionshop.modules.invoice.repository.InvoiceRepository;
import com.example.fashionshop.modules.order.entity.Order;
import com.example.fashionshop.modules.order.repository.OrderRepository;
import com.example.fashionshop.modules.payment.dto.CustomerPaymentStatusResponse;
import com.example.fashionshop.modules.payment.dto.PaymentRequest;
import com.example.fashionshop.modules.payment.dto.PaymentResponse;
import com.example.fashionshop.modules.payment.entity.Payment;
import com.example.fashionshop.modules.payment.mapper.PaymentStatusMapper;
import com.example.fashionshop.modules.payment.repository.PaymentRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only pay your own order");
        }

        PaymentStatus status = (request.getPaymentMethod() == PaymentMethod.COD)
                ? PaymentStatus.UNPAID : PaymentStatus.PAID;

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(status)
                .paidAt(status == PaymentStatus.PAID ? LocalDateTime.now() : null)
                .paidAmount(status == PaymentStatus.PAID ? order.getTotalPrice() : null)
                .gatewayProvider(request.getPaymentMethod() != null ? request.getPaymentMethod().name() : null)
                .transactionReference(status == PaymentStatus.PAID ? generateTransactionReference(request.getPaymentMethod()) : null)
                .build();
        Payment saved = paymentRepository.save(payment);

        Invoice invoice = invoiceRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoice.setPaymentStatus(status == PaymentStatus.PAID ? InvoicePaymentStatus.PAID : InvoicePaymentStatus.PENDING);
        invoiceRepository.save(invoice);
        if (status == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        return toResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentStatus(Integer orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only view payment of your own order");
        }

        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Override
    public CustomerPaymentStatusResponse getCustomerPaymentStatus(Integer orderId) {
        try {
            User user = getCurrentUser();
            Order order = getOwnedOrderOrThrow(orderId, user.getId());
            Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order).orElse(null);
            return PaymentStatusMapper.toCustomerResponse(order, payment);
        } catch (ResourceNotFoundException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentStatusLoadException(ex);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private Order getOwnedOrderOrThrow(Integer orderId, Integer userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseGet(() -> {
                    if (orderRepository.existsById(orderId)) {
                        throw new ForbiddenException("You are not allowed to view this payment information");
                    }
                    throw new ResourceNotFoundException("Order not found");
                });
    }

    private String generateTransactionReference(PaymentMethod method) {
        String prefix = method == null ? "PAY" : method.name();
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
