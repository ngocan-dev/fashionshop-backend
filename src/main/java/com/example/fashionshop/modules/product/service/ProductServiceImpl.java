package com.example.fashionshop.modules.product.service;

import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.ProductDeletionException;
import com.example.fashionshop.common.exception.ProductDetailLoadException;
import com.example.fashionshop.common.exception.ProductListLoadException;
import com.example.fashionshop.common.exception.ProductUpdateException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.exception.SearchResultLoadException;
import com.example.fashionshop.common.mapper.ProductMapper;
import com.example.fashionshop.common.response.PaginationResponse;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.category.entity.Category;
import com.example.fashionshop.modules.category.repository.CategoryRepository;
import com.example.fashionshop.modules.product.dto.ProductDetailResponse;
import com.example.fashionshop.modules.product.dto.ProductManageSummaryResponse;
import com.example.fashionshop.modules.product.dto.ProductManageUpdateRequest;
import com.example.fashionshop.modules.product.dto.ProductRequest;
import com.example.fashionshop.modules.product.dto.ProductResponse;
import com.example.fashionshop.modules.product.dto.ProductSearchResponse;
import com.example.fashionshop.modules.product.dto.ProductStatus;
import com.example.fashionshop.modules.product.entity.Product;
import com.example.fashionshop.modules.product.repository.ProductRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final int SEARCH_LIMIT = 5;
    private static final int DESCRIPTION_SNIPPET_MAX_LENGTH = 120;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        User creator = getCurrentUser();

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity())
                .isActive(true)
                .createdBy(creator)
                .updatedBy(creator)
                .build();

        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse update(Integer productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setUpdatedBy(getCurrentUser());

        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("Invalid product id");
        }

        try {
            Product product = productRepository.findByIdAndIsActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            deactivateProduct(product);
        } catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductDeletionException();
        }
    }

    @Override
    public ProductResponse getDetail(Integer productId) {
        Product product = productRepository.findByIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductMapper.toResponse(product);
    }

    @Override
    public ProductDetailResponse getManageDetail(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("Invalid product id");
        }

        try {
            Product product = productRepository.findByIdAndIsActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            return ProductMapper.toDetailResponse(product);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductDetailLoadException();
        }
    }

    @Override
    public ProductDetailResponse updateManageProduct(Integer productId, ProductManageUpdateRequest request) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("Invalid product id");
        }

        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            product.setName(request.getName().trim());
            product.setDescription(request.getDescription());
            product.setCategory(category);
            product.setPrice(request.getPrice());
            product.setStockQuantity(request.getStockQuantity());
            product.setIsActive(request.getStatus() == ProductStatus.ACTIVE);
            product.setImageUrl(serializeImageUrls(request.getImageUrls()));
            product.setUpdatedBy(getCurrentUser());

            Product updated = productRepository.save(product);
            return ProductMapper.toDetailResponse(updated);
        } catch (BadRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductUpdateException();
        }
    }

    @Override
    public PaginationResponse<ProductManageSummaryResponse> getManageProducts(int page, int size, String keyword) {
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters");
        }

        try {
            Page<Product> result = (keyword == null || keyword.isBlank())
                    ? productRepository.findByIsActiveTrue(PageRequest.of(page, size))
                    : productRepository.findByIsActiveTrueAndNameContainingIgnoreCase(keyword, PageRequest.of(page, size));

            return PaginationResponse.<ProductManageSummaryResponse>builder()
                    .items(result.getContent().stream().map(ProductMapper::toManageSummaryResponse).toList())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalItems(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductListLoadException();
        }
    }

    @Override
    public PaginationResponse<ProductResponse> getProducts(int page, int size, String keyword) {
        Page<Product> result = (keyword == null || keyword.isBlank())
                ? productRepository.findByIsActiveTrue(PageRequest.of(page, size))
                : productRepository.findByIsActiveTrueAndNameContainingIgnoreCase(keyword, PageRequest.of(page, size));

        return PaginationResponse.<ProductResponse>builder()
                .items(result.getContent().stream().map(ProductMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    public List<ProductSearchResponse> searchProducts(String keyword) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            throw new BadRequestException("Please enter a keyword");
        }

        try {
            Page<Product> page = productRepository.searchActiveProductsByKeyword(trimmedKeyword, PageRequest.of(0, SEARCH_LIMIT));
            return page.getContent().stream().map(this::toSearchResponse).toList();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SearchResultLoadException();
        }
    }

    private ProductSearchResponse toSearchResponse(Product product) {
        String description = product.getDescription() == null ? "" : product.getDescription().trim();
        String snippet = description.length() > DESCRIPTION_SNIPPET_MAX_LENGTH
                ? description.substring(0, DESCRIPTION_SNIPPET_MAX_LENGTH).trim() + "..."
                : description;
        boolean inStock = product.getStockQuantity() != null && product.getStockQuantity() > 0;

        return ProductSearchResponse.builder()
                .id(product.getId())
                .slug("product-" + product.getId())
                .name(product.getName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .price(product.getPrice())
                .thumbnailImageUrl(getPrimaryImageUrl(product.getImageUrl()))
                .descriptionSnippet(snippet)
                .inStock(inStock)
                .stockStatus(inStock ? "IN_STOCK" : "OUT_OF_STOCK")
                .productDetailUrl("/products/" + product.getId())
                .build();
    }

    private String getPrimaryImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        return java.util.Arrays.stream(imageUrl.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void deactivateProduct(Product product) {
        product.setIsActive(false);
        productRepository.save(product);
    }

    private String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return null;
        }

        List<String> sanitizedUrls = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .toList();

        if (sanitizedUrls.isEmpty()) {
            return null;
        }

        return String.join(",", sanitizedUrls);
    }
}
