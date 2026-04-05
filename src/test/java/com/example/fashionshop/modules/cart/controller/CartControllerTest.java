package com.example.fashionshop.modules.cart.controller;

import com.example.fashionshop.common.exception.CartUpdateException;
import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.modules.cart.dto.CartItemResponse;
import com.example.fashionshop.modules.cart.dto.CartResponse;
import com.example.fashionshop.modules.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    void removeCartItem_shouldReturnUpdatedCartWithRecalculatedTotalsAndBadgeCount() throws Exception {
        CartResponse cartResponse = CartResponse.builder()
                .cartId(7)
                .items(List.of(CartItemResponse.builder()
                        .itemId(1002)
                        .productId(201)
                        .productName("Classic Coat")
                        .productImage("https://cdn.example.com/product-201.jpg")
                        .price(new BigDecimal("120.00"))
                        .quantity(1)
                        .lineTotal(new BigDecimal("120.00"))
                        .build()))
                .itemCount(1)
                .subtotal(new BigDecimal("120.00"))
                .totalPrice(new BigDecimal("120.00"))
                .empty(false)
                .build();

        when(cartService.removeCartItem(1001)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/items/1001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.itemCount").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(120.00))
                .andExpect(jsonPath("$.data.totalPrice").value(120.00))
                .andExpect(jsonPath("$.data.empty").value(false));
    }

    @Test
    void removeCartItem_shouldReturnNotFoundWhenItemDoesNotExist() throws Exception {
        when(cartService.removeCartItem(9999)).thenThrow(new ResourceNotFoundException("Item not found in cart"));

        mockMvc.perform(delete("/api/cart/items/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Item not found in cart"));
    }

    @Test
    void removeCartItem_shouldReturnUnableToUpdateCartWhenUpdateFails() throws Exception {
        when(cartService.removeCartItem(1001)).thenThrow(new CartUpdateException());

        mockMvc.perform(delete("/api/cart/items/1001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unable to update cart"));
    }
}
