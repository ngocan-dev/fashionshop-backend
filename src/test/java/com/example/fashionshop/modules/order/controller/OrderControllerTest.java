package com.example.fashionshop.modules.order.controller;

import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.OrderDetailLoadException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.modules.order.dto.*;
import com.example.fashionshop.modules.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void orderDetailById_shouldReturnOrderDetailsForAdminOrStaff() throws Exception {
        OrderDetailResponse detailResponse = buildOrderDetailResponse();

        when(orderService.getOrderDetail(1001)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/orders/1001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order detail fetched successfully"))
                .andExpect(jsonPath("$.data.summary.orderId").value(1001))
                .andExpect(jsonPath("$.data.summary.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.customer.fullName").value("Jane Customer"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Classic Blazer"));
    }

    @Test
    void orderDetailById_shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderDetail(9999)).thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void orderDetailById_shouldReturnRetrieveFailureWhenServiceFails() throws Exception {
        when(orderService.getOrderDetail(1001)).thenThrow(new OrderDetailLoadException());

        mockMvc.perform(get("/api/orders/1001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to retrieve order details"));
    }

    @Test
    void orderDetailById_shouldReturnAccessDeniedWhenPermissionIsMissing() throws Exception {
        when(orderService.getOrderDetail(1001)).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/orders/1001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void orderDetailById_shouldReturnBadRequestWhenOrderIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/orders/0").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid order id"));
    }

    private OrderDetailResponse buildOrderDetailResponse() {
        return OrderDetailResponse.builder()
                .summary(OrderSummaryResponse.builder()
                        .orderId(1001)
                        .orderCode("INV-AB12CD34")
                        .orderDate(LocalDateTime.of(2026, 3, 20, 10, 30))
                        .orderStatus(OrderStatus.CONFIRMED)
                        .paymentStatus("PAID")
                        .paymentMethod("MOMO")
                        .totalAmount(new BigDecimal("320.00"))
                        .subtotal(new BigDecimal("320.00"))
                        .shippingFee(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .build())
                .customer(OrderCustomerInfoResponse.builder()
                        .fullName("Jane Customer")
                        .email("jane@example.com")
                        .phoneNumber("0900123456")
                        .shippingAddress("123 Main St, Springfield")
                        .billingAddress(null)
                        .build())
                .items(List.of(OrderDetailItemResponse.builder()
                        .productId(301)
                        .productImage("https://cdn.example.com/products/301.jpg")
                        .productName("Classic Blazer")
                        .sku(null)
                        .quantity(2)
                        .unitPrice(new BigDecimal("160.00"))
                        .lineTotal(new BigDecimal("320.00"))
                        .variant(null)
                        .build()))
                .additionalInfo(OrderAdditionalInfoResponse.builder()
                        .customerNote(null)
                        .deliveryMethod(null)
                        .internalNote("Invoice created automatically when placing order")
                        .lastUpdatedAt(LocalDateTime.of(2026, 3, 20, 10, 30))
                        .build())
                .build();
    }
}
