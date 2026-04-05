package com.example.fashionshop.common.mapper;

import com.example.fashionshop.modules.product.dto.ProductDetailResponse;
import com.example.fashionshop.modules.product.dto.ProductResponse;
import com.example.fashionshop.modules.product.entity.Product;

import java.util.Collections;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .manageDetailUrl("/api/products/manage/" + product.getId())
                .build();
    }
    public static ProductDetailResponse toDetailResponse(Product product) {
        boolean inStock = product.getStockQuantity() != null && product.getStockQuantity() > 0;
        boolean active = Boolean.TRUE.equals(product.getIsActive());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .productCode("SKU-" + product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .inStock(inStock)
                .status(active ? (inStock ? "ACTIVE_IN_STOCK" : "ACTIVE_OUT_OF_STOCK") : "INACTIVE")
                .imageUrls(product.getImageUrl() == null || product.getImageUrl().isBlank()
                        ? Collections.emptyList()
                        : Collections.singletonList(product.getImageUrl()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

}
