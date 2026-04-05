package com.example.fashionshop.modules.payment.controller;

import com.example.fashionshop.common.enums.CustomerPaymentState;
import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.exception.ForbiddenException;
import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.PaymentStatusLoadException;
import com.example.fashionshop.modules.payment.dto.CustomerPaymentStatusResponse;
import com.example.fashionshop.modules.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getCustomerPaymentStatus_shouldReturnSuccessMessageWhenPaymentExists() throws Exception {
        CustomerPaymentStatusResponse response = CustomerPaymentStatusResponse.builder()
                .orderId(1001)
                .orderCode("ORD-1001")
                .orderStatus(OrderStatus.CONFIRMED)
                .orderTotalAmount(new BigDecimal("320.00"))
                .paymentInfoAvailable(true)
                .paymentStatus(CustomerPaymentState.PAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentDateTime(LocalDateTime.of(2026, 4, 5, 10, 30))
                .transactionReference("VNPAY-ABCDEFG12345")
                .paidAmount(new BigDecimal("320.00"))
                .retryAllowed(false)
                .build();

        when(paymentService.getCustomerPaymentStatus(1001)).thenReturn(response);

        mockMvc.perform(get("/api/payments/orders/1001/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment status fetched successfully"))
                .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
                .andExpect(jsonPath("$.data.transactionReference").value("VNPAY-ABCDEFG12345"));
    }

    @Test
    void getCustomerPaymentStatus_shouldReturnNotAvailableMessageWhenNoRecordExists() throws Exception {
        CustomerPaymentStatusResponse response = CustomerPaymentStatusResponse.builder()
                .orderId(1001)
                .orderCode("ORD-1001")
                .orderStatus(OrderStatus.PENDING)
                .orderTotalAmount(new BigDecimal("320.00"))
                .paymentInfoAvailable(false)
                .retryAllowed(false)
                .build();

        when(paymentService.getCustomerPaymentStatus(1001)).thenReturn(response);

        mockMvc.perform(get("/api/payments/orders/1001/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment information not available"));
    }

    @Test
    void getCustomerPaymentStatus_shouldReturnForbiddenWhenOrderBelongsToAnotherCustomer() throws Exception {
        when(paymentService.getCustomerPaymentStatus(1002))
                .thenThrow(new ForbiddenException("You are not allowed to view this payment information"));

        mockMvc.perform(get("/api/payments/orders/1002/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getCustomerPaymentStatus_shouldReturnLoadErrorWhenUnexpectedFailureHappens() throws Exception {
        when(paymentService.getCustomerPaymentStatus(1001)).thenThrow(new PaymentStatusLoadException());

        mockMvc.perform(get("/api/payments/orders/1001/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unable to load payment status"));
    }
}
