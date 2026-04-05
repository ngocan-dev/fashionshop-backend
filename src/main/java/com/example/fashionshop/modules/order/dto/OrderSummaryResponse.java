package com.example.fashionshop.modules.order.dto;

import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentMethod;
import com.example.fashionshop.common.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderSummaryResponse {
    private Integer id;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String shippingStatus;
    private LocalDateTime updatedAt;
    private Integer orderId;
    private String orderCode;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
}
