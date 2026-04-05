package com.example.fashionshop.modules.payment.service;

import com.example.fashionshop.modules.payment.dto.CustomerPaymentStatusResponse;
import com.example.fashionshop.modules.payment.dto.PaymentRequest;
import com.example.fashionshop.modules.payment.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentStatus(Integer orderId);

    CustomerPaymentStatusResponse getCustomerPaymentStatus(Integer orderId);
}
