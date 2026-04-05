package com.example.fashionshop.modules.product.controller;

import com.example.fashionshop.common.exception.GlobalExceptionHandler;
import com.example.fashionshop.common.exception.ProductDetailLoadException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.modules.product.dto.ProductDetailResponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getProductDetail_shouldReturnProductDetailsForAdminOrStaff() throws Exception {
        ProductDetailResponse productDetail = ProductDetailResponse.builder()
                .id(101)
                .productCode("SKU-101")
                .name("Classic Blazer")
                .description("Formal blazer")
                .categoryId(8)
                .categoryName("Blazers")
                .price(new BigDecimal("199.99"))
                .stockQuantity(25)
                .isActive(true)
                .inStock(true)
                .status("ACTIVE_IN_STOCK")
                .imageUrls(List.of("https://cdn.example.com/products/101-main.jpg"))
                .createdAt(LocalDateTime.of(2025, 10, 12, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();

        when(productService.getManageDetail(101)).thenReturn(productDetail);

        mockMvc.perform(get("/api/products/manage/101").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product detail fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.productCode").value("SKU-101"))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://cdn.example.com/products/101-main.jpg"));
    }

    @Test
    void getProductDetail_shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getManageDetail(9999)).thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/manage/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void getProductDetail_shouldReturnUnableToLoadWhenRetrievalFails() throws Exception {
        when(productService.getManageDetail(202)).thenThrow(new ProductDetailLoadException());

        mockMvc.perform(get("/api/products/manage/202").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unable to load product details"));
    }
}
