package com.example.fashionshop.modules.product.controller;

import com.example.fashionshop.common.response.ApiResponse;
import com.example.fashionshop.modules.product.dto.ProductDetailResponse;
import com.example.fashionshop.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/manage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable Integer id) {
        return ApiResponse.success("Product detail fetched successfully", productService.getManageDetail(id));
    }
}
