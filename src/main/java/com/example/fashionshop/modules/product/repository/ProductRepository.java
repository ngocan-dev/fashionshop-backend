package com.example.fashionshop.modules.product.repository;

import com.example.fashionshop.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    List<Product> findTop8ByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    Optional<Product> findByIdAndIsActiveTrue(Integer id);
}
