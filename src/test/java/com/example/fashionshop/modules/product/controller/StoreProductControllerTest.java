package com.example.fashionshop.modules.product.controller;

import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.StoreProductListLoadException;
import com.example.fashionshop.common.response.PaginationResponse;
import com.example.fashionshop.modules.product.dto.StoreProductSummaryResponse;
import com.example.fashionshop.modules.product.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StoreProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void browse_shouldReturnProductsForStorefront() throws Exception {
        StoreProductSummaryResponse product = StoreProductSummaryResponse.builder()
                .id(1)
                .name("Relaxed Linen Shirt")
                .price(new BigDecimal("39.99"))
                .imageUrl("https://cdn.example.com/p1.jpg")
                .categoryName("Shirts")
                .shortDescription("Breathable linen shirt")
                .inStock(true)
                .productDetailUrl("/products/1")
                .build();

        PaginationResponse<StoreProductSummaryResponse> page = PaginationResponse.<StoreProductSummaryResponse>builder()
                .items(List.of(product))
                .page(0)
                .size(12)
                .totalItems(1)
                .totalPages(1)
                .build();

        when(productService.getStoreProducts(0, 12)).thenReturn(page);

        mockMvc.perform(get("/api/store/products")
                        .param("page", "0")
                        .param("size", "12")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products fetched successfully"))
                .andExpect(jsonPath("$.data.items[0].name").value("Relaxed Linen Shirt"))
                .andExpect(jsonPath("$.data.items[0].productDetailUrl").value("/products/1"));
    }

    @Test
    void browse_shouldReturnNoProductsMessageWhenEmpty() throws Exception {
        PaginationResponse<StoreProductSummaryResponse> page = PaginationResponse.<StoreProductSummaryResponse>builder()
                .items(List.of())
                .page(0)
                .size(12)
                .totalItems(0)
                .totalPages(0)
                .build();

        when(productService.getStoreProducts(0, 12)).thenReturn(page);

        mockMvc.perform(get("/api/store/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No products available"));
    }

    @Test
    void browse_shouldReturnUnableToLoadProductsWhenServiceFails() throws Exception {
        when(productService.getStoreProducts(0, 12)).thenThrow(new StoreProductListLoadException());

        mockMvc.perform(get("/api/store/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unable to load products"));
    }

    @Test
    void browse_shouldReturnBadRequestWhenPaginationInvalid() throws Exception {
        when(productService.getStoreProducts(-1, 12)).thenThrow(new BadRequestException("Invalid pagination parameters"));

        mockMvc.perform(get("/api/store/products").param("page", "-1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameters"));
    }
}
