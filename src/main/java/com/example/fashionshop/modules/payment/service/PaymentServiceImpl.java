package com.example.fashionshop.modules.payment.service;

import com.example.fashionshop.common.enums.InvoicePaymentStatus;
import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.enums.PaymentStatus;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.invoice.entity.Invoice;
import com.example.fashionshop.modules.invoice.repository.InvoiceRepository;
import com.example.fashionshop.modules.order.entity.Order;
import com.example.fashionshop.modules.order.repository.OrderRepository;
import com.example.fashionshop.modules.payment.dto.PaymentRequest;
import com.example.fashionshop.modules.payment.dto.PaymentResponse;
import com.example.fashionshop.modules.payment.entity.Payment;
import com.example.fashionshop.modules.payment.repository.PaymentRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        try {
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
                    .build();
            Payment saved = paymentRepository.save(payment);

            Invoice invoice = invoiceRepository.findByOrder(order)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
            invoice.setPaymentStatus(status == PaymentStatus.PAID ? InvoicePaymentStatus.PAID : InvoicePaymentStatus.PENDING);
            invoiceRepository.save(invoice);

            if (request.getPaymentMethod() == PaymentMethod.COD || status == PaymentStatus.PAID) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }

            return toResponse(saved);
        } catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Payment processing failed. Please retry payment");
        }
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

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
