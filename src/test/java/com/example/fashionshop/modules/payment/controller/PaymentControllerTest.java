package com.example.fashionshop.modules.payment.controller;

import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.enums.PaymentStatus;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.PaymentGatewayException;
import com.example.fashionshop.modules.payment.dto.PaymentRequest;
import com.example.fashionshop.modules.payment.dto.PaymentResponse;
import com.example.fashionshop.modules.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void processPayment_shouldReturnSuccessWhenGatewayChargeIsSuccessful() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod(PaymentMethod.BANKING);
        request.setCardHolderName("Jane Customer");
        request.setCardNumber("4111111111111111");
        request.setExpiryMonth("12");
        request.setExpiryYear("29");
        request.setCvv("123");

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(8)
                .orderId(1001)
                .paymentMethod(PaymentMethod.BANKING)
                .paymentStatus(PaymentStatus.PAID)
                .orderStatus(OrderStatus.CONFIRMED)
                .message("Payment successful")
                .retryable(false)
                .paidAt(LocalDateTime.of(2026, 4, 5, 10, 20))
                .build();

        when(paymentService.processPayment(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/orders/1001/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.orderStatus").value("confirmed"))
                .andExpect(jsonPath("$.data.message").value("Payment successful"));
    }

    @Test
    void processPayment_shouldReturnValidationErrorForMissingMethod() throws Exception {
        PaymentRequest request = new PaymentRequest();

        mockMvc.perform(post("/api/payments/orders/1001/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void processPayment_shouldReturnGatewayFailure() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod(PaymentMethod.BANKING);

        when(paymentService.processPayment(any(), any())).thenThrow(new PaymentGatewayException("Payment failed"));

        mockMvc.perform(post("/api/payments/orders/1001/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment failed"));
    }

    @Test
    void processPayment_shouldReturnBadRequestForExpiredCheckoutSession() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod(PaymentMethod.COD);

        when(paymentService.processPayment(any(), any())).thenThrow(new BadRequestException("Checkout session has expired"));

        mockMvc.perform(post("/api/payments/orders/1001/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Checkout session has expired"));
    }
}
