package com.example.fashionshop.modules.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Integer cartId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private Integer distinctItemCount;
    private BigDecimal totalPrice;
}
